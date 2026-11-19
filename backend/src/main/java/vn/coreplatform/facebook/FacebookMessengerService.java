package vn.coreplatform.facebook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import static vn.coreplatform.shared.ApiExceptionHandler.ApiProblem;

@Service
public class FacebookMessengerService {
  private final JdbcTemplate jdbc;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final String pageId;
  private final String pageAccessToken;
  private final String pageName;
  private final String graphVersion;

  public FacebookMessengerService(
      JdbcTemplate jdbc,
      ObjectMapper objectMapper,
      @Value("${facebook.page-id:${FACEBOOK_PAGE_ID:}}") String pageId,
      @Value("${facebook.page-access-token:${FACEBOOK_PAGE_ACCESS_TOKEN:}}") String pageAccessToken,
      @Value("${facebook.page-name:${FACEBOOK_PAGE_NAME:Facebook Page}}") String pageName,
      @Value("${facebook.graph-version:${FACEBOOK_GRAPH_VERSION:v27.0}}") String graphVersion) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
    this.pageId = pageId;
    this.pageAccessToken = pageAccessToken;
    this.pageName = pageName;
    this.graphVersion = graphVersion;
    this.httpClient = HttpClient.newBuilder().build();
  }

  public record Connection(boolean configured, String pageId, String pageName, String graphVersion) {}
  public record Summary(int openConversations, int unreadMessages, int sentToday) {}
  public record Conversation(String id, String customer, String psid, String preview, int unread, Instant updatedAt) {}
  public record Message(String id, String conversationId, String direction, String message, String status,
                        Instant sentAt, String attachmentType, String attachmentUrl) {}
  public record Workspace(Connection connection, Summary summary, List<Conversation> conversations) {}
  public record SyncResult(int conversations, int messages, Instant syncedAt) {}

  public Workspace workspace() {
    requireModuleEnabled();
    var conversations = jdbc.query("""
        select id, customer_name, customer_psid, last_message, unread_count, updated_at
        from facebook.messenger_conversation order by updated_at desc limit 100
        """, (rs, row) -> new Conversation(rs.getString("id"), rs.getString("customer_name"),
        rs.getString("customer_psid"), rs.getString("last_message"), rs.getInt("unread_count"),
        rs.getTimestamp("updated_at").toInstant()));
    var unread = conversations.stream().mapToInt(Conversation::unread).sum();
    var sentToday = jdbc.queryForObject("""
        select count(*) from facebook.messenger_message
        where direction='OUTBOUND' and sent_at >= current_date
        """, Integer.class);
    var configured = configured();
    return new Workspace(new Connection(configured, configured ? pageId : "", configured ? pageName : "Chưa cấu hình", graphVersion),
        new Summary(conversations.size(), unread, sentToday == null ? 0 : sentToday), conversations);
  }

  @Transactional
  public List<Message> messages(String conversationId) {
    requireModuleEnabled();
    jdbc.update("update facebook.messenger_conversation set unread_count=0 where id=?", conversationId);
    return jdbc.query("""
        select id, conversation_id, direction, message, status, sent_at, attachment_type, attachment_url
        from facebook.messenger_message where conversation_id=? order by sent_at asc limit 500
        """, (rs, row) -> new Message(rs.getString("id"), rs.getString("conversation_id"),
        rs.getString("direction"), rs.getString("message"), rs.getString("status"),
        rs.getTimestamp("sent_at").toInstant(), rs.getString("attachment_type"),
        rs.getString("attachment_url")), conversationId);
  }

  @Transactional
  public Message sendText(String conversationId, String text) {
    requireModuleEnabled();
    requireConfigured();
    var psid = jdbc.query("select customer_psid from facebook.messenger_conversation where id=?",
        rs -> rs.next() ? rs.getString(1) : null, conversationId);
    if (psid == null) throw new ApiProblem(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "Hội thoại Facebook không tồn tại");

    try {
      var payload = objectMapper.writeValueAsString(Map.of(
          "recipient", Map.of("id", psid),
          "messaging_type", "RESPONSE",
          "message", Map.of("text", text)));
      var uri = UriComponentsBuilder.fromUriString("https://graph.facebook.com")
          .pathSegment(graphVersion, pageId, "messages").build().toUri();
      var request = HttpRequest.newBuilder(uri)
          .header("Authorization", "Bearer " + pageAccessToken)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new ApiProblem(HttpStatus.BAD_GATEWAY, "FACEBOOK_SEND_FAILED", graphError(response.body()));
      }
      var body = objectMapper.readTree(response.body());
      var messageId = body.path("message_id").asText("local-" + UUID.randomUUID());
      var now = Instant.now();
      jdbc.update("""
          insert into facebook.messenger_message(id,conversation_id,direction,message,status,sent_at)
          values (?,?,'OUTBOUND',?,'SENT',?) on conflict (id) do nothing
          """, messageId, conversationId, text, java.sql.Timestamp.from(now));
      jdbc.update("update facebook.messenger_conversation set last_message=?,updated_at=? where id=?",
          text, java.sql.Timestamp.from(now), conversationId);
      return new Message(messageId, conversationId, "OUTBOUND", text, "SENT", now, null, null);
    } catch (ApiProblem problem) {
      throw problem;
    } catch (Exception ex) {
      throw new ApiProblem(HttpStatus.BAD_GATEWAY, "FACEBOOK_SEND_FAILED", "Không thể kết nối Facebook Graph API");
    }
  }

  /** Đồng bộ chủ động từ Graph API; dùng được khi local chưa có webhook HTTPS. */
  @Transactional
  public SyncResult syncFromFacebook() {
    requireModuleEnabled();
    requireConfigured();
    try {
      var uri = UriComponentsBuilder.fromUriString("https://graph.facebook.com")
          .pathSegment(graphVersion, pageId, "conversations")
          .queryParam("platform", "MESSENGER")
          .queryParam("fields", "id,updated_time,participants,messages.limit(50){id,message,from,to,created_time,attachments}")
          .queryParam("limit", 50).build().encode().toUri();
      var request = HttpRequest.newBuilder(uri)
          .header("Authorization", "Bearer " + pageAccessToken).GET().build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300)
        throw new ApiProblem(HttpStatus.BAD_GATEWAY, "FACEBOOK_SYNC_FAILED", graphError(response.body()));
      var root = objectMapper.readTree(response.body());
      var conversationCount = 0;
      var messageCount = 0;
      for (JsonNode graphConversation : root.path("data")) {
        var customer = customerParticipant(graphConversation.path("participants").path("data"));
        if (customer == null) continue;
        var psid = customer.path("id").asText();
        var customerName = customer.path("name").asText("Khách hàng " + psid.substring(Math.max(0, psid.length() - 6)));
        var conversationId = existingConversationId(pageId, psid, graphConversation.path("id").asText());
        var updatedAt = parseInstant(graphConversation.path("updated_time").asText(), Instant.now());
        jdbc.update("""
            insert into facebook.messenger_conversation(id,page_id,customer_psid,customer_name,last_message,unread_count,updated_at)
            values (?,?,?,?, '',0,?)
            on conflict (id) do update set customer_name=excluded.customer_name,updated_at=excluded.updated_at
            """, conversationId, pageId, psid, customerName, java.sql.Timestamp.from(updatedAt));
        conversationCount++;
        String latestText = "";
        Instant latestAt = Instant.EPOCH;
        for (JsonNode graphMessage : graphConversation.path("messages").path("data")) {
          var id = graphMessage.path("id").asText();
          if (id.isBlank()) continue;
          var fromId = graphMessage.path("from").path("id").asText();
          var direction = pageId.equals(fromId) ? "OUTBOUND" : "INBOUND";
          var sentAt = parseInstant(graphMessage.path("created_time").asText(), updatedAt);
          var text = graphMessage.path("message").asText(attachmentLabelFromGraph(graphMessage));
          var attachment = firstGraphAttachment(graphMessage);
          var inserted = jdbc.update("""
              insert into facebook.messenger_message(id,conversation_id,direction,message,status,attachment_type,attachment_url,sent_at)
              values (?,?,?,?,?,?,?,?) on conflict (id) do nothing
              """, id, conversationId, direction, text, "INBOUND".equals(direction) ? "RECEIVED" : "SENT",
              attachment[0], attachment[1], java.sql.Timestamp.from(sentAt));
          messageCount += inserted;
          if (inserted > 0 && "INBOUND".equals(direction))
            jdbc.update("update facebook.messenger_conversation set unread_count=unread_count+1 where id=?", conversationId);
          if (sentAt.isAfter(latestAt)) { latestAt = sentAt; latestText = text; }
        }
        if (!latestText.isBlank()) jdbc.update("update facebook.messenger_conversation set last_message=?,updated_at=? where id=?",
            latestText, java.sql.Timestamp.from(latestAt), conversationId);
      }
      return new SyncResult(conversationCount, messageCount, Instant.now());
    } catch (ApiProblem problem) {
      throw problem;
    } catch (Exception ex) {
      throw new ApiProblem(HttpStatus.BAD_GATEWAY, "FACEBOOK_SYNC_FAILED", "Không thể đồng bộ hội thoại từ Facebook");
    }
  }

  @Transactional
  public void acceptWebhook(JsonNode payload) {
    if (!"page".equals(payload.path("object").asText())) return;
    for (JsonNode entry : payload.path("entry")) {
      var eventPageId = entry.path("id").asText(pageId);
      for (JsonNode event : entry.path("messaging")) processEvent(eventPageId, event);
    }
  }

  private void processEvent(String eventPageId, JsonNode event) {
    var sender = event.path("sender").path("id").asText();
    var messageNode = event.path("message");
    if (messageNode.isMissingNode() || messageNode.path("is_echo").asBoolean(false)) return;
    if (sender.isBlank() || sender.equals(eventPageId)) return;

    var conversationId = existingConversationId(eventPageId, sender, eventPageId + ":" + sender);
    var messageId = messageNode.path("mid").asText("webhook-" + UUID.randomUUID());
    var text = messageNode.path("text").asText(attachmentLabel(messageNode));
    var sentAt = event.path("timestamp").asLong(0) > 0
        ? Instant.ofEpochMilli(event.path("timestamp").asLong()) : Instant.now();
    String attachmentType = null;
    String attachmentUrl = null;
    if (messageNode.path("attachments").isArray() && !messageNode.path("attachments").isEmpty()) {
      var attachment = messageNode.path("attachments").get(0);
      attachmentType = attachment.path("type").asText(null);
      attachmentUrl = attachment.path("payload").path("url").asText(null);
    }
    var customerName = "Khách hàng " + sender.substring(Math.max(0, sender.length() - 6));
    jdbc.update("""
        insert into facebook.messenger_conversation(id,page_id,customer_psid,customer_name,last_message,unread_count,updated_at)
        values (?,?,?,?,?,0,?)
        on conflict (id) do update set last_message=excluded.last_message,
          updated_at=excluded.updated_at
        """, conversationId, eventPageId, sender, customerName, text, java.sql.Timestamp.from(sentAt));
    var inserted = jdbc.update("""
        insert into facebook.messenger_message(id,conversation_id,direction,message,status,attachment_type,attachment_url,sent_at)
        values (?,?,'INBOUND',?,'RECEIVED',?,?,?) on conflict (id) do nothing
        """, messageId, conversationId, text, attachmentType, attachmentUrl, java.sql.Timestamp.from(sentAt));
    if (inserted > 0) jdbc.update("update facebook.messenger_conversation set unread_count=unread_count+1 where id=?", conversationId);
  }

  private String attachmentLabel(JsonNode message) {
    if (!message.path("attachments").isArray() || message.path("attachments").isEmpty()) return "";
    return "[" + message.path("attachments").get(0).path("type").asText("Tệp đính kèm") + "]";
  }

  private JsonNode customerParticipant(JsonNode participants) {
    if (!participants.isArray()) return null;
    for (JsonNode participant : participants) if (!pageId.equals(participant.path("id").asText())) return participant;
    return null;
  }

  private String existingConversationId(String eventPageId, String psid, String fallback) {
    var existing = jdbc.query("select id from facebook.messenger_conversation where page_id=? and customer_psid=?",
        rs -> rs.next() ? rs.getString(1) : null, eventPageId, psid);
    return existing == null ? fallback : existing;
  }

  private Instant parseInstant(String value, Instant fallback) {
    try { return Instant.parse(value); } catch (Exception ignored) { return fallback; }
  }

  private String attachmentLabelFromGraph(JsonNode message) {
    if (!message.path("attachments").path("data").isArray() || message.path("attachments").path("data").isEmpty()) return "";
    return "[Tệp đính kèm]";
  }

  private String[] firstGraphAttachment(JsonNode message) {
    var data = message.path("attachments").path("data");
    if (!data.isArray() || data.isEmpty()) return new String[] {null, null};
    var attachment = data.get(0);
    var type = attachment.path("mime_type").asText().startsWith("image/") ? "image" : attachment.path("type").asText(null);
    var url = attachment.path("image_data").path("url").asText(attachment.path("file_url").asText(null));
    return new String[] {type, url};
  }

  private String graphError(String raw) {
    try {
      var message = objectMapper.readTree(raw).path("error").path("message").asText();
      return message.isBlank() ? "Facebook từ chối gửi tin nhắn" : message;
    } catch (Exception ignored) {
      return "Facebook từ chối gửi tin nhắn";
    }
  }

  public boolean configured() { return !pageId.isBlank() && !pageAccessToken.isBlank(); }
  private void requireConfigured() {
    if (!configured()) throw new ApiProblem(HttpStatus.SERVICE_UNAVAILABLE, "FACEBOOK_NOT_CONFIGURED", "Chưa cấu hình Facebook Page ID và Page Access Token");
  }
  private void requireModuleEnabled() {
    var enabled = jdbc.queryForObject("select count(*) > 0 from platform.module where module_key='whatsapp' and status<>'DISABLED'", Boolean.class);
    if (!Boolean.TRUE.equals(enabled)) throw new ApiProblem(HttpStatus.CONFLICT, "MODULE_DISABLED", "Module WhatsApp đang tắt");
  }
}


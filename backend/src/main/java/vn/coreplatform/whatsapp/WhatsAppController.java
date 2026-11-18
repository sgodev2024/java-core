package vn.coreplatform.whatsapp;

import static vn.coreplatform.shared.ApiExceptionHandler.ApiProblem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Preview API. Không gọi Meta Graph API và không đọc/lưu access token. */
@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsAppController {
  private final JdbcTemplate jdbc;
  public WhatsAppController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public record Summary(int openConversations, int unreadMessages, int templates, int sentToday) {}
  public record Conversation(String id, String customer, String phone, String preview, String status, int unread, Instant updatedAt) {}
  public record Template(String id, String name, String category, String language, String status) {}
  public record Preview(boolean previewMode, boolean connected, String phoneNumber, String businessAccount,
                        Summary summary, List<Conversation> conversations, List<Template> templates) {}
  public record PreviewMessageRequest(@NotBlank @Size(max = 4096) String message) {}
  public record PreviewMessage(String id, String conversationId, String direction, String message,
                               String status, Instant sentAt, boolean preview) {}
  public record PreviewRecall(String id, String conversationId, String status, Instant recalledAt) {}

  @GetMapping("/preview")
  Preview preview() {
    requireEnabled();
    var now = Instant.now();
    return new Preview(true, false, "Chưa cấu hình", "Chưa kết nối", new Summary(3, 5, 3, 0),
        List.of(
            new Conversation("preview-1", "Nguyễn Minh Anh", "+84 912 345 678", "Mình muốn hỏi về trạng thái đơn hàng...", "WAITING", 2, now.minusSeconds(180)),
            new Conversation("preview-2", "Trần Quốc Huy", "+84 903 222 119", "Cảm ơn bạn, mình đã nhận được thông tin.", "OPEN", 0, now.minusSeconds(840)),
            new Conversation("preview-3", "Lê Thu Trang", "+84 987 654 321", "Bên mình có hỗ trợ giao hàng cuối tuần không?", "WAITING", 3, now.minusSeconds(2400))),
        List.of(
            new Template("template-1", "order_confirmation", "UTILITY", "vi", "DRAFT"),
            new Template("template-2", "shipping_update", "UTILITY", "vi", "DRAFT"),
            new Template("template-3", "welcome_customer", "MARKETING", "vi", "DRAFT")));
  }

  @PostMapping("/preview/conversations/{conversationId}/messages")
  PreviewMessage sendPreviewMessage(@org.springframework.web.bind.annotation.PathVariable String conversationId,
                                    @Valid @RequestBody PreviewMessageRequest request) {
    requireEnabled();
    if (!conversationId.matches("preview-[1-3]"))
      throw new ApiProblem(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "Hội thoại preview không tồn tại");
    return new PreviewMessage("preview-message-" + UUID.randomUUID(), conversationId, "OUTBOUND",
        request.message().trim(), "PREVIEW", Instant.now(), true);
  }

  @DeleteMapping("/preview/conversations/{conversationId}/messages/{messageId}")
  PreviewRecall recallPreviewMessage(@org.springframework.web.bind.annotation.PathVariable String conversationId,
                                     @org.springframework.web.bind.annotation.PathVariable String messageId) {
    requireEnabled();
    if (!conversationId.matches("preview-[1-3]") || !messageId.startsWith("preview-message-"))
      throw new ApiProblem(HttpStatus.NOT_FOUND, "MESSAGE_NOT_FOUND", "Tin nhắn preview không tồn tại");
    return new PreviewRecall(messageId, conversationId, "RECALLED", Instant.now());
  }

  private void requireEnabled() {
    var enabled = jdbc.queryForObject("select count(*) > 0 from platform.module where module_key='whatsapp' and status<>'DISABLED'", Boolean.class);
    if (!Boolean.TRUE.equals(enabled)) throw new ApiProblem(HttpStatus.CONFLICT, "MODULE_DISABLED", "Module WhatsApp đang tắt");
  }
}

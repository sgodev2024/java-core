package vn.coreplatform.facebook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint công khai để Meta xác minh và gửi sự kiện Messenger vào CRM. */
@RestController
@RequestMapping("/api/v1/facebook/webhook")
public class FacebookWebhookController {
  private static final String SIGNATURE_PREFIX = "sha256=";

  private final ObjectMapper objectMapper;
  private final FacebookMessengerService messengerService;
  private final String verifyToken;
  private final String appSecret;

  public FacebookWebhookController(
      ObjectMapper objectMapper,
      FacebookMessengerService messengerService,
      @Value("${facebook.webhook.verify-token:${FACEBOOK_WEBHOOK_VERIFY_TOKEN:}}") String verifyToken,
      @Value("${facebook.app-secret:${FACEBOOK_APP_SECRET:}}") String appSecret) {
    this.objectMapper = objectMapper;
    this.messengerService = messengerService;
    this.verifyToken = verifyToken;
    this.appSecret = appSecret;
  }

  /** Meta gọi endpoint này một lần khi cấu hình Callback URL. */
  @GetMapping
  ResponseEntity<String> verify(
      @RequestParam(name = "hub.mode", required = false) String mode,
      @RequestParam(name = "hub.verify_token", required = false) String receivedToken,
      @RequestParam(name = "hub.challenge", required = false) String challenge) {
    if (verifyToken.isBlank()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body("FACEBOOK_WEBHOOK_VERIFY_TOKEN is not configured");
    }
    if ("subscribe".equals(mode)
        && constantTimeEquals(verifyToken, receivedToken)
        && challenge != null) {
      return ResponseEntity.ok(challenge);
    }
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Webhook verification failed");
  }

  /** Meta gửi message, delivery, read và postback events vào endpoint này. */
  @PostMapping
  ResponseEntity<String> receive(
      @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
      @RequestBody byte[] rawBody) {
    if (appSecret.isBlank()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body("FACEBOOK_APP_SECRET is not configured");
    }
    if (!validSignature(rawBody, signature)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid webhook signature");
    }

    try {
      JsonNode payload = objectMapper.readTree(rawBody);
      if (!"page".equals(payload.path("object").asText())) {
        return ResponseEntity.badRequest().body("Unsupported webhook object");
      }
      messengerService.acceptWebhook(payload);
      return ResponseEntity.ok("EVENT_RECEIVED");
    } catch (Exception ex) {
      return ResponseEntity.badRequest().body("Invalid JSON payload");
    }
  }

  private boolean validSignature(byte[] body, String signature) {
    if (signature == null || !signature.startsWith(SIGNATURE_PREFIX)) return false;
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String expected = SIGNATURE_PREFIX + HexFormat.of().formatHex(mac.doFinal(body));
      return constantTimeEquals(expected, signature);
    } catch (Exception ex) {
      throw new IllegalStateException("Cannot validate Meta webhook signature", ex);
    }
  }

  private static boolean constantTimeEquals(String expected, String actual) {
    if (expected == null || actual == null) return false;
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
  }
}

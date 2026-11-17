package vn.coreplatform.whatsapp;

import static vn.coreplatform.shared.ApiExceptionHandler.ApiProblem;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
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

  @GetMapping("/preview")
  Preview preview() {
    var enabled = jdbc.queryForObject("select count(*) > 0 from platform.module where module_key='whatsapp' and status<>'DISABLED'", Boolean.class);
    if (!Boolean.TRUE.equals(enabled)) throw new ApiProblem(HttpStatus.CONFLICT, "MODULE_DISABLED", "Module WhatsApp đang tắt");
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
}

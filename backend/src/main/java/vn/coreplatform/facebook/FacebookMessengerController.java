package vn.coreplatform.facebook;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/facebook/messenger")
public class FacebookMessengerController {
  private final FacebookMessengerService service;
  public FacebookMessengerController(FacebookMessengerService service) { this.service = service; }

  public record SendRequest(@NotBlank @Size(max = 2000) String message) {}

  @GetMapping("/workspace")
  FacebookMessengerService.Workspace workspace() { return service.workspace(); }

  @PostMapping("/sync")
  FacebookMessengerService.SyncResult sync() { return service.syncFromFacebook(); }

  @GetMapping("/customers")
  List<FacebookMessengerService.CustomerInfo> customers() { return service.customers(); }

  @GetMapping("/conversations/{conversationId}/messages")
  List<FacebookMessengerService.Message> messages(@PathVariable String conversationId) {
    return service.messages(conversationId);
  }

  @PostMapping("/conversations/{conversationId}/messages")
  FacebookMessengerService.Message send(@PathVariable String conversationId,
                                        @Valid @RequestBody SendRequest request) {
    return service.sendText(conversationId, request.message().trim());
  }
}


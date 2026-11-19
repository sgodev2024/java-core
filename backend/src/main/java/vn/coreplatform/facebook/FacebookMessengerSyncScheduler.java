package vn.coreplatform.facebook;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Automatically imports Messenger conversations while the backend is running. */
@Component
public class FacebookMessengerSyncScheduler {
  private static final Logger log = LoggerFactory.getLogger(FacebookMessengerSyncScheduler.class);
  private final FacebookMessengerService service;
  private final AtomicBoolean connectedLogged = new AtomicBoolean(false);
  private final AtomicReference<String> lastError = new AtomicReference<>("");

  public FacebookMessengerSyncScheduler(FacebookMessengerService service) {
    this.service = service;
  }

  @Scheduled(initialDelay = 3000, fixedDelay = 5000)
  public void synchronize() {
    if (!service.configured()) {
      warnOnce("Facebook auto-sync chưa có Page ID hoặc Page Access Token");
      return;
    }
    try {
      var result = service.syncFromFacebook();
      lastError.set("");
      if (connectedLogged.compareAndSet(false, true)) {
        log.info("Facebook auto-sync connected; polling every 5 seconds");
      }
      if (result.messages() > 0) {
        log.info("Facebook auto-sync imported {} new message(s)", result.messages());
      }
    } catch (Exception exception) {
      warnOnce("Facebook auto-sync thất bại: " + exception.getMessage());
    }
  }

  private void warnOnce(String message) {
    if (!message.equals(lastError.getAndSet(message))) log.warn(message);
  }
}

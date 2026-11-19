package vn.coreplatform.facebook;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Nhận diện và chuẩn hóa số di động Việt Nam được khách hàng chủ động gửi. */
public final class VietnamesePhoneNumber {
  private static final Pattern CANDIDATE = Pattern.compile(
      "(?<!\\d)(?:\\+?84|0)(?:[ .-]?\\d){9}(?!\\d)");

  // Viettel, VinaPhone, MobiFone, Vietnamobile, Gmobile, iTel và Wintel.
  private static final Set<String> MOBILE_PREFIXES = Set.of(
      "032", "033", "034", "035", "036", "037", "038", "039", "086", "096", "097", "098",
      "081", "082", "083", "084", "085", "088", "091", "094",
      "070", "076", "077", "078", "079", "089", "090", "093",
      "052", "056", "058", "092",
      "059", "099",
      "087",
      "055");

  private VietnamesePhoneNumber() {}

  public static Optional<String> extractFirst(String message) {
    if (message == null || message.isBlank()) return Optional.empty();
    var matcher = CANDIDATE.matcher(message);
    while (matcher.find()) {
      var normalized = normalize(matcher.group());
      if (normalized.length() == 10 && MOBILE_PREFIXES.contains(normalized.substring(0, 3))) {
        return Optional.of(normalized);
      }
    }
    return Optional.empty();
  }

  private static String normalize(String candidate) {
    var digits = candidate.replaceAll("\\D", "");
    if (digits.startsWith("84")) return "0" + digits.substring(2);
    return digits;
  }
}

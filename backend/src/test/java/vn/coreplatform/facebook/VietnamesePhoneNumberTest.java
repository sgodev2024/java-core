package vn.coreplatform.facebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VietnamesePhoneNumberTest {
  @Test
  void extractsAndNormalizesVietnameseMobileNumbers() {
    assertEquals("0912345678", VietnamesePhoneNumber.extractFirst("SĐT của tôi: 0912 345 678").orElseThrow());
    assertEquals("0987654321", VietnamesePhoneNumber.extractFirst("+84-987-654-321").orElseThrow());
    assertEquals("0551234567", VietnamesePhoneNumber.extractFirst("Liên hệ 84551234567 nhé").orElseThrow());
  }

  @Test
  void rejectsUnknownPrefixesAndNonPhoneNumbers() {
    assertTrue(VietnamesePhoneNumber.extractFirst("0123456789").isEmpty());
    assertTrue(VietnamesePhoneNumber.extractFirst("Mã đơn hàng 1234567890").isEmpty());
    assertTrue(VietnamesePhoneNumber.extractFirst("09123456789").isEmpty());
  }
}

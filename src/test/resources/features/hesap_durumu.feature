Feature: Hesap durumunun değiştirilmesi
  Müşteri hesabını dondurabilir, yeniden etkinleştirebilir veya kapatabilir.

  Scenario: Aktif hesap dondurulur
    Given Ece adlı müşterinin STANDARD türünde ve 250 TL bakiyeli aktif hesabı var
    When Ece adlı müşteri hesabını dondurur
    Then işlem başarıyla tamamlanmalı
    And Ece adlı müşterinin hesabı dondurulmuş olmalı
    And Ece adlı müşterinin bakiyesi 250 TL olmalı

  Scenario: Dondurulmuş hesap yeniden etkinleştirilir
    Given Ece adlı müşterinin STANDARD türünde ve 250 TL bakiyeli aktif hesabı var
    And Ece adlı müşterinin hesabı dondurulmuş
    When Ece adlı müşteri hesabının dondurmasını kaldırır
    Then işlem başarıyla tamamlanmalı
    And Ece adlı müşterinin hesabı aktif olmalı
    And Ece adlı müşterinin bakiyesi 250 TL olmalı

  Scenario: Sıfır bakiyeli hesap kapatılır
    Given Ece adlı müşterinin STANDARD türünde ve 0 TL bakiyeli aktif hesabı var
    When Ece adlı müşteri hesabını kapatır
    Then işlem başarıyla tamamlanmalı
    And Ece adlı müşterinin hesabı kapalı olmalı
    And Ece adlı müşterinin bakiyesi 0 TL olmalı

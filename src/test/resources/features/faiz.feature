Feature: Aylık faizin uygulanması
  Müşteri hesap türüne göre hesaplanan faizi bakiyesinde ve işlem geçmişinde görmek ister.

  Scenario Outline: Aylık faiz kuruşa yuvarlanıp bakiyeye eklenir
    Given Ece adlı müşterinin <hesap_turu> türünde ve <ilk_bakiye> TL bakiyeli aktif hesabı var
    When Ece adlı müşterinin hesabına aylık faiz uygulanır
    Then işlem başarıyla tamamlanmalı
    And Ece adlı müşterinin bakiyesi <son_bakiye> TL olmalı
    And Ece adlı müşterinin işlem geçmişinde <faiz> TL tutarında başarılı faiz kaydı bulunmalı

    Examples: Tam lira tutarında faiz
      | hesap_turu | ilk_bakiye | faiz  | son_bakiye |
      | STANDARD   | 12000.00   | 30.00 | 12030.00   |
      | PREMIUM    | 12000.00   | 50.00 | 12050.00   |
      | STANDARD   | 0.00       | 0.00  | 0.00       |

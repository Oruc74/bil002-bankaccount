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

    # R-8.3, H-09
    @hata
    Examples: Kuruş hassasiyeti gerektiren faiz
      | hesap_turu | ilk_bakiye | faiz  | son_bakiye |
      | STANDARD   | 1000.00    | 2.50  | 1002.50    |
      | STANDARD   | 2.00       | 0.01  | 2.01       |
      | PREMIUM    | 2500.50    | 10.42 | 2510.92    |

  Scenario: Dondurulmuş hesaba faiz uygulanmaz
    Given Ece adlı müşterinin STANDARD türünde ve 1000 TL bakiyeli aktif hesabı var
    And Ece adlı müşterinin hesabı dondurulmuş
    When Ece adlı müşterinin hesabına aylık faiz uygulanır
    Then işlem dondurulmuş hesap nedeniyle reddedilmeli
    And Ece adlı müşterinin bakiyesi 1000 TL olmalı
    And Ece adlı müşterinin hesabı dondurulmuş kalmalı

  Scenario: Kapalı hesaba faiz uygulanmaz
    Given Ece adlı müşterinin STANDARD türünde ve 0 TL bakiyeli aktif hesabı var
    And Ece adlı müşterinin hesabı kapalı
    When Ece adlı müşterinin hesabına aylık faiz uygulanır
    Then işlem kapalı hesap nedeniyle reddedilmeli
    And Ece adlı müşterinin bakiyesi 0 TL olmalı
    And Ece adlı müşterinin hesabı kapalı olmalı

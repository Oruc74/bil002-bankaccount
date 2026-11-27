Feature: Hesaba para yatırma
  Müşteri yatırdığı tutarı bakiyesinde ve işlem geçmişinde görmek ister.

  Scenario Outline: Yatırılan tutar bakiyeye eklenir
    Given Ece adlı müşterinin STANDARD türünde ve <ilk_bakiye> TL bakiyeli aktif hesabı var
    When Ece adlı müşteri hesabına <tutar> TL yatırır
    Then işlem başarıyla tamamlanmalı
    And Ece adlı müşterinin bakiyesi <son_bakiye> TL olmalı
    And Ece adlı müşterinin işlem geçmişinde <tutar> TL tutarında başarılı para yatırma kaydı bulunmalı

    Examples:
      | ilk_bakiye | tutar    | son_bakiye |
      | 125.25     | 40.50    | 165.75     |
      | 250.00     | 10000.00 | 10250.00   |

  # R-2.2, H-03
  @hata
  Scenario: Sıfır lira yatırma reddedilir
    Given Ece adlı müşterinin STANDARD türünde ve 250 TL bakiyeli aktif hesabı var
    When Ece adlı müşteri hesabına 0 TL yatırır
    Then işlem geçersiz tutar nedeniyle reddedilmeli
    And Ece adlı müşterinin bakiyesi 250 TL olmalı

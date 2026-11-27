Feature: Hesaplar arası para transferi
  Müşteri başka bir hesaba para göndermek ister.

  Scenario: Gönderilen tutar iki hesabın bakiyesine yansır
    Given Ece adlı müşterinin STANDARD türünde ve 1000 TL bakiyeli aktif hesabı var
    And Mert adlı müşterinin PREMIUM türünde ve 200 TL bakiyeli aktif hesabı var
    When Ece adlı müşteri Mert adlı müşteriye 300 TL gönderir
    Then işlem başarıyla tamamlanmalı
    And Ece adlı müşterinin bakiyesi 700 TL olmalı
    And Mert adlı müşterinin bakiyesi 500 TL olmalı
    And Ece adlı müşterinin işlem geçmişinde 300 TL tutarında başarılı transfer kaydı bulunmalı
    And Mert adlı müşterinin işlem geçmişinde 300 TL tutarında başarılı transfer kaydı bulunmalı

  Scenario: Kaynak hesapta yeterli para yoksa transfer reddedilir
    Given Ece adlı müşterinin STANDARD türünde ve 300 TL bakiyeli aktif hesabı var
    And Mert adlı müşterinin STANDARD türünde ve 100 TL bakiyeli aktif hesabı var
    When Ece adlı müşteri Mert adlı müşteriye 500 TL gönderir
    Then işlem yetersiz bakiye nedeniyle reddedilmeli
    And Ece adlı müşterinin bakiyesi 300 TL olmalı
    And Mert adlı müşterinin bakiyesi 100 TL olmalı

  # R-4.3, H-07
  @hata
  Scenario: Dondurulmuş hesaba para gönderilemez
    Given Ece adlı müşterinin STANDARD türünde ve 1000 TL bakiyeli aktif hesabı var
    And Mert adlı müşterinin STANDARD türünde ve 200 TL bakiyeli aktif hesabı var
    And Mert adlı müşterinin hesabı dondurulmuş
    When Ece adlı müşteri Mert adlı müşteriye 300 TL gönderir
    Then işlem dondurulmuş hesap nedeniyle reddedilmeli
    And Ece adlı müşterinin bakiyesi 1000 TL olmalı
    And Mert adlı müşterinin bakiyesi 200 TL olmalı
    And Mert adlı müşterinin hesabı dondurulmuş kalmalı

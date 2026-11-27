Feature: Hesaptan para çekme
  Müşteri hesabındaki paranın bir kısmını çekmek ister.

  Scenario: Çekilen tutar bakiyeden düşülür
    Given Ece adlı müşterinin STANDARD türünde ve 1800 TL bakiyeli aktif hesabı var
    When Ece adlı müşteri hesabından 450 TL çeker
    Then işlem başarıyla tamamlanmalı
    And Ece adlı müşterinin bakiyesi 1350 TL olmalı
    And Ece adlı müşterinin işlem geçmişinde 450 TL tutarında başarılı para çekme kaydı bulunmalı

  Scenario: Bakiyeyi aşan çekim reddedilir
    Given Ece adlı müşterinin STANDARD türünde ve 300 TL bakiyeli aktif hesabı var
    When Ece adlı müşteri hesabından 500 TL çeker
    Then işlem yetersiz bakiye nedeniyle reddedilmeli
    And Ece adlı müşterinin bakiyesi 300 TL olmalı

  # R-3.3, H-02
  @hata
  Scenario: Dondurulmuş hesaptan para çekilemez
    Given Ece adlı müşterinin STANDARD türünde ve 1000 TL bakiyeli aktif hesabı var
    And Ece adlı müşterinin hesabı dondurulmuş
    When Ece adlı müşteri hesabından 200 TL çeker
    Then işlem dondurulmuş hesap nedeniyle reddedilmeli
    And Ece adlı müşterinin bakiyesi 1000 TL olmalı
    And Ece adlı müşterinin hesabı dondurulmuş kalmalı

package tr.edu.sakarya.bil002.blackbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import tr.edu.sakarya.bil002.exception.AccountNotFoundException;
import tr.edu.sakarya.bil002.exception.InsufficientBalanceException;
import tr.edu.sakarya.bil002.exception.InvalidAccountStateException;
import tr.edu.sakarya.bil002.exception.LimitExceededException;
import tr.edu.sakarya.bil002.model.Account;
import tr.edu.sakarya.bil002.model.AccountStatus;
import tr.edu.sakarya.bil002.model.Transaction;
import tr.edu.sakarya.bil002.model.TransactionStatus;
import tr.edu.sakarya.bil002.model.TransactionType;
import tr.edu.sakarya.bil002.service.AccountService;

/**
 * Kara kutu testleri: yalnızca docs/GEREKSINIMLER.md belgesine bakarak yazılmalıdır.
 * Kaynak kodu okumadan, girdi/çıktı davranışını test edin.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountServiceBlackBoxTest {

    @Autowired
    private AccountService accountService;

    // ===================== EP TESTLERİ — Denklik Sınıfı Bölümleme =====================

    // EP-01 (R-2.5, R-2.6): geçerli tutar yatırılır
    @Test
    void ep01_deposit_gecerliMiktar_basarili() {
        String no = hesapAc("STANDARD", 1000.0);

        accountService.deposit(no, 500.0);

        basariKontrol(no, TransactionType.DEPOSIT, 500.0, 1500.0);
    }

    // EP-02 (R-2.2, R-2.7): 0 TL yatırma reddedilmeli (PREMIUM hesapta)
    @Test
    @Tag("hata")
    void ep02_deposit_sifirMiktar_reddedilmeli() {
        // R-2.2: 0 TL yatırma reddedilmeli. Rapor: H-03
        String no = hesapAc("PREMIUM", 1000.0);

        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(no, 0.0));

        retKontrol(no, TransactionType.DEPOSIT, 0.0, 1000.0, AccountStatus.ACTIVE);
    }

    // EP-03 (R-2.2, R-2.7): negatif tutar yatırma reddedilmeli
    @Test
    @Tag("hata")
    void ep03_deposit_negatifMiktar_reddedilmeli() {
        // R-2.7: reddedilen yatırma için FAILED kaydı oluşmalı. Rapor: H-01
        String no = hesapAc("STANDARD", 1000.0);

        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(no, -100.0));

        retKontrol(no, TransactionType.DEPOSIT, -100.0, 1000.0, AccountStatus.ACTIVE);
    }

    // EP-04 (R-2.3, R-2.7): dondurulmuş hesaba yatırma reddedilmeli
    @Test
    @Tag("hata")
    void ep04_deposit_dondurulmusHesap_reddedilmeli() {
        // R-2.7: reddedilen yatırma için FAILED kaydı oluşmalı. Rapor: H-01
        String no = hesapAc("STANDARD", 1000.0);
        accountService.freezeAccount(no);
        assertEquals(AccountStatus.FROZEN, accountService.getAccount(no).getStatus());

        assertThrows(InvalidAccountStateException.class, () -> accountService.deposit(no, 500.0));

        retKontrol(no, TransactionType.DEPOSIT, 500.0, 1000.0, AccountStatus.FROZEN);
    }

    // EP-05 (R-2.3, R-2.7): kapalı hesaba yatırma reddedilmeli
    @Test
    @Tag("hata")
    void ep05_deposit_kapaliHesap_reddedilmeli() {
        // R-2.7: reddedilen yatırma için FAILED kaydı oluşmalı. Rapor: H-01
        String no = hesapAc("STANDARD", 0.0);
        accountService.closeAccount(no);
        assertEquals(AccountStatus.CLOSED, accountService.getAccount(no).getStatus());

        assertThrows(InvalidAccountStateException.class, () -> accountService.deposit(no, 500.0));

        retKontrol(no, TransactionType.DEPOSIT, 500.0, 0.0, AccountStatus.CLOSED);
    }

    // EP-06 (R-3.6, R-3.7): geçerli tutar çekilir
    @Test
    void ep06_withdraw_gecerliMiktar_basarili() {
        String no = hesapAc("STANDARD", 1000.0);

        accountService.withdraw(no, 200.0);

        basariKontrol(no, TransactionType.WITHDRAWAL, 200.0, 800.0);
    }

    // EP-07 (R-3.3, R-3.8): dondurulmuş hesaptan çekme reddedilmeli
    @Test
    @Tag("hata")
    void ep07_withdraw_dondurulmusHesap_reddedilmeli() {
        // R-3.3: dondurulmuş hesaptan çekim reddedilmeli. Rapor: H-02
        String no = hesapAc("STANDARD", 1000.0);
        accountService.freezeAccount(no);
        assertEquals(AccountStatus.FROZEN, accountService.getAccount(no).getStatus());

        assertThrows(InvalidAccountStateException.class, () -> accountService.withdraw(no, 100.0));

        retKontrol(no, TransactionType.WITHDRAWAL, 100.0, 1000.0, AccountStatus.FROZEN);
    }

    // EP-08 (R-1.1): hesap sahibi adı null, boş veya yalnız boşluk olamaz
    @Test
    void ep08_createAccount_bosAd_reddedilmeli() {
        int oncekiSayi = accountService.getAllAccounts().size();

        assertThrows(IllegalArgumentException.class,
                () -> accountService.createAccount(null, "STANDARD", 100.0));
        assertThrows(IllegalArgumentException.class,
                () -> accountService.createAccount("", "STANDARD", 100.0));
        assertThrows(IllegalArgumentException.class,
                () -> accountService.createAccount("   ", "STANDARD", 100.0));

        assertEquals(oncekiSayi, accountService.getAllAccounts().size(), "yeni hesap açılmamalı");
    }

    // EP-09 (R-1.2): hesap türü yalnız "STANDARD" veya "PREMIUM" olabilir (birebir)
    @Test
    void ep09_createAccount_gecersizTur_reddedilmeli() {
        int oncekiSayi = accountService.getAllAccounts().size();

        assertThrows(IllegalArgumentException.class,
                () -> accountService.createAccount("Ali", "GOLD", 100.0));
        assertThrows(IllegalArgumentException.class,
                () -> accountService.createAccount("Ali", "standard", 100.0));
        assertThrows(IllegalArgumentException.class,
                () -> accountService.createAccount("Ali", "STANDARD ", 100.0));
        assertThrows(IllegalArgumentException.class,
                () -> accountService.createAccount("Ali", "", 100.0));
        assertThrows(IllegalArgumentException.class,
                () -> accountService.createAccount("Ali", null, 100.0));

        assertEquals(oncekiSayi, accountService.getAllAccounts().size(), "yeni hesap açılmamalı");
    }

    // EP-10 (R-1.4): negatif başlangıç bakiyesi reddedilmeli
    @Test
    @Tag("hata")
    void ep10_createAccount_negatifBakiye_reddedilmeli() {
        // R-1.4: negatif başlangıç bakiyesi reddedilmeli. Rapor: H-06
        int oncekiSayi = accountService.getAllAccounts().size();

        assertThrows(IllegalArgumentException.class,
                () -> accountService.createAccount("Ali", "STANDARD", -100.0));

        assertEquals(oncekiSayi, accountService.getAllAccounts().size(), "yeni hesap açılmamalı");
    }

    // EP-11 (R-1.2, R-1.5, R-1.6, R-1.7): geçerli bilgilerle PREMIUM hesap açılır
    @Test
    void ep11_createAccount_gecerliBilgiler_basarili() {
        Account hesap = accountService.createAccount("Ali", "PREMIUM", 100.0);

        assertEquals("PREMIUM", hesap.getAccountType());
        assertEquals(100.0, hesap.getBalance(), 0.001);
        assertEquals(AccountStatus.ACTIVE, hesap.getStatus());
        // 10 haneli, yalnız rakam, ilk hane 0 değil
        assertTrue(hesap.getAccountNumber().matches("[1-9][0-9]{9}"), hesap.getAccountNumber());
        assertTrue(accountService.getTransactionHistory(hesap.getAccountNumber()).isEmpty());
    }

    // EP-12 (R-1.3): başlangıç bakiyesi verilmezse 0 olur
    @Test
    void ep12_createAccount_bakiyeVerilmezse_sifirOlur() {
        Account hesap = accountService.createAccount("Ali", "STANDARD", null);

        assertEquals(0.0, hesap.getBalance(), 0.001);
    }

    // EP-13 (R-2.1): olmayan hesaba yatırma (hesap numarası 0 ile başlayamaz)
    @Test
    void ep13_deposit_olmayanHesap_reddedilmeli() {
        assertThrows(AccountNotFoundException.class, () -> accountService.deposit("0000000000", 100.0));
    }

    // EP-14 (R-3.1): olmayan hesaptan çekme
    @Test
    void ep14_withdraw_olmayanHesap_reddedilmeli() {
        assertThrows(AccountNotFoundException.class, () -> accountService.withdraw("0000000000", 100.0));
    }

    // EP-15 (R-3.2, R-3.8): negatif tutar çekme reddedilmeli
    @Test
    @Tag("hata")
    void ep15_withdraw_negatifMiktar_reddedilmeli() {
        // R-3.8: reddedilen çekme için FAILED kaydı oluşmalı. Rapor: H-01
        String no = hesapAc("STANDARD", 1000.0);

        assertThrows(IllegalArgumentException.class, () -> accountService.withdraw(no, -100.0));

        retKontrol(no, TransactionType.WITHDRAWAL, -100.0, 1000.0, AccountStatus.ACTIVE);
    }

    // EP-16 (R-2.4): yatırma limiti her işlem için ayrıdır, toplam için değil
    @Test
    void ep16_deposit_limitHerIslemIcinAyri() {
        String no = hesapAc("STANDARD", 0.0);

        accountService.deposit(no, 6000.0);
        accountService.deposit(no, 6000.0);

        assertEquals(12000.0, accountService.getAccount(no).getBalance(), 0.001);
    }

    // EP-17 (R-3.5): reddedilen çekim günlük limite sayılmaz
    @Test
    void ep17_withdraw_basarisizDeneme_limiteSayilmaz() {
        String no = hesapAc("STANDARD", 10000.0);
        assertThrows(LimitExceededException.class, () -> accountService.withdraw(no, 6000.0));

        accountService.withdraw(no, 5000.0);

        assertEquals(5000.0, accountService.getAccount(no).getBalance(), 0.001);
    }

    // ===================== BVA TESTLERİ — Sınır Değer Analizi =====================
    // Her sınır için üç değer: sınırın altı, sınır, sınırın üstü

    // ----- Yatırma alt sınırı: 0 -----

    // BVA-01 (R-2.2, R-2.7): -0,01 reddedilmeli
    @Test
    @Tag("hata")
    void bva01_deposit_eksiBirKurus_reddedilmeli() {
        // R-2.7: reddedilen yatırma için FAILED kaydı oluşmalı. Rapor: H-01
        String no = hesapAc("STANDARD", 1000.0);

        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(no, -0.01));

        retKontrol(no, TransactionType.DEPOSIT, -0.01, 1000.0, AccountStatus.ACTIVE);
    }

    // BVA-02 (R-2.2, R-2.7): 0 TL reddedilmeli
    @Test
    @Tag("hata")
    void bva02_deposit_sifirTL_sinirDegeri() {
        // R-2.2: 0 TL yatırma reddedilmeli. Rapor: H-03
        String no = hesapAc("STANDARD", 1000.0);

        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(no, 0.0));

        retKontrol(no, TransactionType.DEPOSIT, 0.0, 1000.0, AccountStatus.ACTIVE);
    }

    // BVA-03 (G-1, R-2.2): 0,01 kabul edilmeli
    @Test
    void bva03_deposit_birKurus_minimumGecerli() {
        String no = hesapAc("STANDARD", 1000.0);

        accountService.deposit(no, 0.01);

        basariKontrol(no, TransactionType.DEPOSIT, 0.01, 1000.01);
    }

    // ----- STANDARD tek seferlik yatırma limiti: 10.000 -----

    // BVA-04 (R-2.4): 9.999,99 kabul edilmeli
    @Test
    void bva04_deposit_standartMaksimumunAlti_9999_99TL() {
        String no = hesapAc("STANDARD", 0.0);

        accountService.deposit(no, 9999.99);

        basariKontrol(no, TransactionType.DEPOSIT, 9999.99, 9999.99);
    }

    // BVA-05 (R-2.4): tam 10.000 kabul edilmeli
    @Test
    void bva05_deposit_standartMaksimum_10000TL() {
        String no = hesapAc("STANDARD", 0.0);

        accountService.deposit(no, 10000.0);

        basariKontrol(no, TransactionType.DEPOSIT, 10000.0, 10000.0);
    }

    // BVA-06 (R-2.4, R-2.7): 10.000,01 reddedilmeli
    @Test
    @Tag("hata")
    void bva06_deposit_standartMaksimumuAsan_10000_01TL() {
        // R-2.7: reddedilen yatırma için FAILED kaydı oluşmalı. Rapor: H-01
        String no = hesapAc("STANDARD", 0.0);

        assertThrows(LimitExceededException.class, () -> accountService.deposit(no, 10000.01));

        retKontrol(no, TransactionType.DEPOSIT, 10000.01, 0.0, AccountStatus.ACTIVE);
    }

    // ----- PREMIUM tek seferlik yatırma limiti: 50.000 -----

    // BVA-07 (R-2.4): 49.999,99 kabul edilmeli
    @Test
    void bva07_deposit_premiumMaksimumunAlti_49999_99TL() {
        String no = hesapAc("PREMIUM", 0.0);

        accountService.deposit(no, 49999.99);

        basariKontrol(no, TransactionType.DEPOSIT, 49999.99, 49999.99);
    }

    // BVA-08 (R-2.4): tam 50.000 kabul edilmeli
    @Test
    void bva08_deposit_premiumMaksimum_50000TL() {
        String no = hesapAc("PREMIUM", 0.0);

        accountService.deposit(no, 50000.0);

        basariKontrol(no, TransactionType.DEPOSIT, 50000.0, 50000.0);
    }

    // BVA-09 (R-2.4, R-2.7): 50.000,01 reddedilmeli
    @Test
    @Tag("hata")
    void bva09_deposit_premiumMaksimumuAsan_50000_01TL() {
        // R-2.7: reddedilen yatırma için FAILED kaydı oluşmalı. Rapor: H-01
        String no = hesapAc("PREMIUM", 0.0);

        assertThrows(LimitExceededException.class, () -> accountService.deposit(no, 50000.01));

        retKontrol(no, TransactionType.DEPOSIT, 50000.01, 0.0, AccountStatus.ACTIVE);
    }

    // ----- Çekme alt sınırı: 0 -----

    // BVA-10 (R-3.2, R-3.8): -0,01 reddedilmeli
    @Test
    @Tag("hata")
    void bva10_withdraw_eksiBirKurus_reddedilmeli() {
        // R-3.8: reddedilen çekme için FAILED kaydı oluşmalı. Rapor: H-01
        String no = hesapAc("STANDARD", 1000.0);

        assertThrows(IllegalArgumentException.class, () -> accountService.withdraw(no, -0.01));

        retKontrol(no, TransactionType.WITHDRAWAL, -0.01, 1000.0, AccountStatus.ACTIVE);
    }

    // BVA-11 (R-3.2, R-3.8): 0 TL reddedilmeli
    @Test
    @Tag("hata")
    void bva11_withdraw_sifirTL_reddedilmeli() {
        // R-3.8: reddedilen çekme için FAILED kaydı oluşmalı. Rapor: H-01
        String no = hesapAc("STANDARD", 1000.0);

        assertThrows(IllegalArgumentException.class, () -> accountService.withdraw(no, 0.0));

        retKontrol(no, TransactionType.WITHDRAWAL, 0.0, 1000.0, AccountStatus.ACTIVE);
    }

    // BVA-12 (G-1, R-3.2): 0,01 kabul edilmeli
    @Test
    void bva12_withdraw_birKurus_minimumGecerli() {
        String no = hesapAc("STANDARD", 1000.0);

        accountService.withdraw(no, 0.01);

        basariKontrol(no, TransactionType.WITHDRAWAL, 0.01, 999.99);
    }

    // ----- Bakiye sınırı: 1.000 TL bakiyeli hesap -----

    // BVA-13 (R-3.4): bakiyeden 1 kuruş az çekilebilmeli
    @Test
    void bva13_withdraw_bakiyedenAz_999_99TL() {
        String no = hesapAc("STANDARD", 1000.0);

        accountService.withdraw(no, 999.99);

        basariKontrol(no, TransactionType.WITHDRAWAL, 999.99, 0.01);
    }

    // BVA-14 (R-3.4): bakiyenin tamamı çekilebilmeli
    @Test
    @Tag("hata")
    void bva14_withdraw_tamBakiye_cekilebilmeli() {
        // R-3.4: bakiyenin tamamı çekilebilmeli. Rapor: H-04
        String no = hesapAc("STANDARD", 1000.0);

        accountService.withdraw(no, 1000.0);

        basariKontrol(no, TransactionType.WITHDRAWAL, 1000.0, 0.0);
    }

    // BVA-15 (R-3.4, R-3.8): bakiyeden 1 kuruş fazlası reddedilmeli
    @Test
    @Tag("hata")
    void bva15_withdraw_bakiyedenFazla_reddedilmeli() {
        // R-3.8: reddedilen çekme için FAILED kaydı oluşmalı. Rapor: H-01
        String no = hesapAc("STANDARD", 1000.0);

        assertThrows(InsufficientBalanceException.class, () -> accountService.withdraw(no, 1000.01));

        retKontrol(no, TransactionType.WITHDRAWAL, 1000.01, 1000.0, AccountStatus.ACTIVE);
    }

    // ----- STANDARD günlük çekim limiti: 5.000 -----

    // BVA-16 (R-3.5): günlük toplam 4.999,99 kabul edilmeli
    @Test
    void bva16_withdraw_standartGunlukToplam_4999_99TL() {
        String no = hesapAc("STANDARD", 10000.0);

        accountService.withdraw(no, 3000.0);
        accountService.withdraw(no, 1999.99);

        assertEquals(5000.01, accountService.getAccount(no).getBalance(), 0.001);
        assertEquals(2, accountService.getTransactionHistory(no).size());
    }

    // BVA-17 (R-3.5): günlük toplam tam 5.000 kabul edilmeli
    @Test
    void bva17_withdraw_standartGunlukToplam_5000TL() {
        String no = hesapAc("STANDARD", 10000.0);

        accountService.withdraw(no, 3000.0);
        accountService.withdraw(no, 2000.0);

        assertEquals(5000.0, accountService.getAccount(no).getBalance(), 0.001);
        assertEquals(2, accountService.getTransactionHistory(no).size());
    }

    // BVA-18 (R-3.5, R-3.8): günlük toplam 5.000,01 olunca reddedilmeli
    @Test
    @Tag("hata")
    void bva18_withdraw_standartGunlukToplamiAsan_5000_01TL() {
        // R-3.8: reddedilen çekme için FAILED kaydı oluşmalı. Rapor: H-01
        String no = hesapAc("STANDARD", 10000.0);
        accountService.withdraw(no, 3000.0);

        assertThrows(LimitExceededException.class, () -> accountService.withdraw(no, 2000.01));

        assertEquals(7000.0, accountService.getAccount(no).getBalance(), 0.001);
        // İlk çekimin SUCCESS kaydına ek olarak reddedilen denemenin FAILED kaydı olmalı
        List<Transaction> kayitlar = accountService.getTransactionHistory(no);
        assertEquals(2, kayitlar.size(), "işlem kaydı sayısı");
        assertTrue(kayitlar.stream().anyMatch(k -> k.getStatus() == TransactionStatus.FAILED));
    }

    // ----- PREMIUM günlük çekim limiti: 20.000 -----

    // BVA-19 (R-3.5): günlük toplam 19.999,99 kabul edilmeli
    @Test
    void bva19_withdraw_premiumGunlukToplam_19999_99TL() {
        String no = hesapAc("PREMIUM", 30000.0);

        accountService.withdraw(no, 12000.0);
        accountService.withdraw(no, 7999.99);

        assertEquals(10000.01, accountService.getAccount(no).getBalance(), 0.001);
        assertEquals(2, accountService.getTransactionHistory(no).size());
    }

    // BVA-20 (R-3.5): tam 20.000 kabul edilmeli
    @Test
    @Tag("hata")
    void bva20_withdraw_premiumGunlukLimit_20000TL() {
        // R-3.5: PREMIUM günlük limitin tamamı (20.000 TL) çekilebilmeli. Rapor: H-05
        String no = hesapAc("PREMIUM", 30000.0);

        accountService.withdraw(no, 20000.0);

        basariKontrol(no, TransactionType.WITHDRAWAL, 20000.0, 10000.0);
    }

    // BVA-21 (R-3.5, R-3.8): 20.000,01 reddedilmeli
    @Test
    @Tag("hata")
    void bva21_withdraw_premiumGunlukLimitiAsan_20000_01TL() {
        // R-3.8: reddedilen çekme için FAILED kaydı oluşmalı. Rapor: H-01
        String no = hesapAc("PREMIUM", 30000.0);

        assertThrows(LimitExceededException.class, () -> accountService.withdraw(no, 20000.01));

        retKontrol(no, TransactionType.WITHDRAWAL, 20000.01, 30000.0, AccountStatus.ACTIVE);
    }

    // ----- Başlangıç bakiyesi alt sınırı: 0 -----

    // BVA-22 (R-1.4): -0,01 reddedilmeli
    @Test
    @Tag("hata")
    void bva22_createAccount_eksiBirKurus_reddedilmeli() {
        // R-1.4: negatif başlangıç bakiyesi reddedilmeli. Rapor: H-06
        int oncekiSayi = accountService.getAllAccounts().size();

        assertThrows(IllegalArgumentException.class,
                () -> accountService.createAccount("Ali", "STANDARD", -0.01));

        assertEquals(oncekiSayi, accountService.getAllAccounts().size(), "yeni hesap açılmamalı");
    }

    // BVA-23 (R-1.4, R-1.6, R-1.7): 0 kabul edilmeli, hesap ACTIVE ve geçmiş boş olmalı
    @Test
    void bva23_createAccount_sifirBakiye_kabulEdilmeli() {
        String no = hesapAc("STANDARD", 0.0);

        assertEquals(0.0, accountService.getAccount(no).getBalance(), 0.001);
        assertEquals(AccountStatus.ACTIVE, accountService.getAccount(no).getStatus());
        assertTrue(accountService.getTransactionHistory(no).isEmpty());
    }

    // BVA-24 (R-1.4): 0,01 kabul edilmeli
    @Test
    void bva24_createAccount_birKurus_kabulEdilmeli() {
        String no = hesapAc("STANDARD", 0.01);

        assertEquals(0.01, accountService.getAccount(no).getBalance(), 0.001);
    }

    // ===================== DURUM GEÇİŞ TESTLERİ =====================

    @Test
    void hesap_aktiftenDonduruldu() {
        // TODO
    }

    @Test
    void hesap_donduruldutanAktife() {
        // TODO
    }

    @Test
    void hesap_aktiftenKapali() {
        // TODO
    }

    @Test
    void hesap_kapaliHesapYenidenAcilamaz() {
        // TODO
    }

    @Test
    void hesap_dondurulmusHesapKapatma_bakiyeSifirsa() {
        // TODO
    }

    // ===================== KARAR TABLOSU — Transfer kuralları =====================

    @Test
    void kt01_transfer_ikisiDeAktif_basarili() {
        String kaynak = hesapAc("STANDARD", 1000.0);
        String hedef = hesapAc("STANDARD", 200.0);

        accountService.transfer(kaynak, hedef, 250.0);

        basariKontrol(kaynak, TransactionType.TRANSFER, 250.0, 750.0);
        basariKontrol(hedef, TransactionType.TRANSFER, 250.0, 450.0);
    }

    @Test
    void kt02_transfer_kaynakDondurulmus_reddedilmeli() {
        String kaynak = hesapAc("STANDARD", 1000.0);
        String hedef = hesapAc("STANDARD", 200.0);
        accountService.freezeAccount(kaynak);
        assertEquals(AccountStatus.FROZEN, accountService.getAccount(kaynak).getStatus());

        assertThrows(InvalidAccountStateException.class, () -> accountService.transfer(kaynak, hedef, 250.0));

        transferRetKontrol(kaynak, 1000.0, AccountStatus.FROZEN);
        transferRetKontrol(hedef, 200.0, AccountStatus.ACTIVE);
    }

    @Test
    @Tag("hata")
    void kt03_transfer_hedefDondurulmus_reddedilmeli() {
        // R-4.3: Dondurulmuş hedefe transfer reddedilmeli. Rapor: H-07
        String kaynak = hesapAc("STANDARD", 1000.0);
        String hedef = hesapAc("STANDARD", 200.0);
        accountService.freezeAccount(hedef);
        assertEquals(AccountStatus.FROZEN, accountService.getAccount(hedef).getStatus());

        assertThrows(InvalidAccountStateException.class, () -> accountService.transfer(kaynak, hedef, 250.0));

        transferRetKontrol(kaynak, 1000.0, AccountStatus.ACTIVE);
        transferRetKontrol(hedef, 200.0, AccountStatus.FROZEN);
    }

    @Test
    @Tag("hata")
    void kt04_transfer_ayniHesap_reddedilmeli() {
        // R-4.4: Aynı hesaba transfer reddedilmeli. Rapor: H-08
        String no = hesapAc("STANDARD", 1000.0);

        assertThrows(IllegalArgumentException.class, () -> accountService.transfer(no, no, 250.0));

        transferRetKontrol(no, 1000.0, AccountStatus.ACTIVE);
    }

    @Test
    void kt05_transfer_kaynakKapali_reddedilmeli() {
        String kaynak = hesapAc("STANDARD", 0.0);
        String hedef = hesapAc("STANDARD", 200.0);
        accountService.closeAccount(kaynak);
        assertEquals(AccountStatus.CLOSED, accountService.getAccount(kaynak).getStatus());

        // Kapalı kaynağın bakiyesi sıfırdır. Durum ve bakiye hatalarının ikisi de kabul edilir.
        RuntimeException hata = assertThrows(RuntimeException.class, () -> accountService.transfer(kaynak, hedef, 250.0));
        assertTrue(hata instanceof InvalidAccountStateException || hata instanceof InsufficientBalanceException);

        transferRetKontrol(kaynak, 0.0, AccountStatus.CLOSED);
        transferRetKontrol(hedef, 200.0, AccountStatus.ACTIVE);
    }

    @Test
    @Tag("hata")
    void kt06_transfer_hedefKapali_reddedilmeli() {
        // R-4.3: Kapalı hedefe transfer reddedilmeli. Rapor: H-07
        String kaynak = hesapAc("STANDARD", 1000.0);
        String hedef = hesapAc("STANDARD", 0.0);
        accountService.closeAccount(hedef);
        assertEquals(AccountStatus.CLOSED, accountService.getAccount(hedef).getStatus());

        assertThrows(InvalidAccountStateException.class, () -> accountService.transfer(kaynak, hedef, 250.0));

        transferRetKontrol(kaynak, 1000.0, AccountStatus.ACTIVE);
        transferRetKontrol(hedef, 0.0, AccountStatus.CLOSED);
    }

    @Test
    void kt07_transfer_bakiyeYetersiz_reddedilmeli() {
        String kaynak = hesapAc("STANDARD", 100.0);
        String hedef = hesapAc("STANDARD", 200.0);

        assertThrows(InsufficientBalanceException.class, () -> accountService.transfer(kaynak, hedef, 100.01));

        transferRetKontrol(kaynak, 100.0, AccountStatus.ACTIVE);
        transferRetKontrol(hedef, 200.0, AccountStatus.ACTIVE);
    }

    @Test
    void kt08_transfer_kaynakYok_reddedilmeli() {
        String kaynak = "0000000000";
        String hedef = hesapAc("STANDARD", 200.0);

        assertThrows(AccountNotFoundException.class, () -> accountService.transfer(kaynak, hedef, 250.0));

        transferRetKontrol(hedef, 200.0, AccountStatus.ACTIVE);
    }

    @Test
    void kt09_transfer_hedefYok_reddedilmeli() {
        String kaynak = hesapAc("STANDARD", 1000.0);
        String hedef = "0000000000";

        assertThrows(AccountNotFoundException.class, () -> accountService.transfer(kaynak, hedef, 250.0));

        transferRetKontrol(kaynak, 1000.0, AccountStatus.ACTIVE);
    }

    @Test
    void kt10_transfer_sifirTutar_reddedilmeli() {
        String kaynak = hesapAc("STANDARD", 1000.0);
        String hedef = hesapAc("STANDARD", 200.0);

        assertThrows(IllegalArgumentException.class, () -> accountService.transfer(kaynak, hedef, 0.0));

        transferRetKontrol(kaynak, 1000.0, AccountStatus.ACTIVE);
        transferRetKontrol(hedef, 200.0, AccountStatus.ACTIVE);
    }

    @Test
    void kt11_transfer_negatifTutar_reddedilmeli() {
        String kaynak = hesapAc("STANDARD", 1000.0);
        String hedef = hesapAc("STANDARD", 200.0);

        assertThrows(IllegalArgumentException.class, () -> accountService.transfer(kaynak, hedef, -100.0));

        transferRetKontrol(kaynak, 1000.0, AccountStatus.ACTIVE);
        transferRetKontrol(hedef, 200.0, AccountStatus.ACTIVE);
    }

    @Test
    void kt12_transfer_tamBakiye_aktarilabilmeli() {
        String kaynak = hesapAc("STANDARD", 1000.0);
        String hedef = hesapAc("STANDARD", 200.0);

        accountService.transfer(kaynak, hedef, 1000.0);

        basariKontrol(kaynak, TransactionType.TRANSFER, 1000.0, 0.0);
        basariKontrol(hedef, TransactionType.TRANSFER, 1000.0, 1200.0);
    }

    @Test
    void kt13_transfer_ayriLimitiYok() {
        String kaynak = hesapAc("STANDARD", 70000.0);
        String hedef = hesapAc("STANDARD", 1000.0);

        // Tutar, belgelenen yatırma ve günlük çekim limitlerinin üstündedir.
        accountService.transfer(kaynak, hedef, 60000.0);

        basariKontrol(kaynak, TransactionType.TRANSFER, 60000.0, 10000.0);
        basariKontrol(hedef, TransactionType.TRANSFER, 60000.0, 61000.0);
    }

    @Test
    void kt14_transfer_gunlukCekimLimitineSayilmaz() {
        String kaynak = hesapAc("STANDARD", 10000.0);
        String hedef = hesapAc("STANDARD", 200.0);

        accountService.transfer(kaynak, hedef, 1000.0);
        basariKontrol(kaynak, TransactionType.TRANSFER, 1000.0, 9000.0);

        accountService.withdraw(kaynak, 5000.0);

        assertEquals(4000.0, accountService.getAccount(kaynak).getBalance(), 0.001);
    }

    // ===================== YARDIMCI METOTLAR =====================

    private String hesapAc(String tur, double bakiye) {
        return accountService.createAccount("Ali", tur, bakiye).getAccountNumber();
    }

    // Başarılı işlem: bakiye güncellenmeli ve bir SUCCESS kaydı oluşmalı (Bölüm 6)
    private void basariKontrol(String no, TransactionType tip, double tutar, double yeniBakiye) {
        assertEquals(yeniBakiye, accountService.getAccount(no).getBalance(), 0.001, "yeni bakiye");
        tekKayitKontrol(no, tip, TransactionStatus.SUCCESS, tutar, yeniBakiye);
    }

    // Reddedilen işlem: bakiye ve durum aynı kalmalı (G-4), bir FAILED kaydı oluşmalı (Bölüm 6)
    private void retKontrol(String no, TransactionType tip, double tutar, double bakiye, AccountStatus durum) {
        assertEquals(bakiye, accountService.getAccount(no).getBalance(), 0.001, "bakiye değişmemeli");
        assertEquals(durum, accountService.getAccount(no).getStatus(), "durum değişmemeli");
        tekKayitKontrol(no, tip, TransactionStatus.FAILED, tutar, bakiye);
    }

    private void transferRetKontrol(String no, double bakiye, AccountStatus durum) {
        Account hesap = accountService.getAccount(no);
        assertEquals(bakiye, hesap.getBalance(), 0.001, "bakiye değişmemeli");
        assertEquals(durum, hesap.getStatus(), "durum değişmemeli");
        assertTrue(accountService.getTransactionHistory(no).stream()
                .noneMatch(k -> k.getType() == TransactionType.TRANSFER
                        && k.getStatus() == TransactionStatus.SUCCESS),
                "başarılı transfer kaydı oluşmamalı");
    }

    // Hesap açılışı kayıt oluşturmaz (R-1.7), bu yüzden tek işlemden sonra tek kayıt beklenir
    private void tekKayitKontrol(String no, TransactionType tip, TransactionStatus durum,
            double tutar, double bakiyeSonrasi) {
        List<Transaction> kayitlar = accountService.getTransactionHistory(no);
        assertEquals(1, kayitlar.size(), "işlem kaydı sayısı");
        assertEquals(tip, kayitlar.get(0).getType());
        assertEquals(durum, kayitlar.get(0).getStatus());
        assertEquals(tutar, kayitlar.get(0).getAmount(), 0.001, "kayıt tutarı");
        assertEquals(bakiyeSonrasi, kayitlar.get(0).getBalanceAfter(), 0.001, "balanceAfter");
    }
}

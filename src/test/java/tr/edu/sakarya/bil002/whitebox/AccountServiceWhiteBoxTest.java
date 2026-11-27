package tr.edu.sakarya.bil002.whitebox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import tr.edu.sakarya.bil002.exception.LimitExceededException;
import tr.edu.sakarya.bil002.model.Account;
import tr.edu.sakarya.bil002.model.AccountStatus;
import tr.edu.sakarya.bil002.service.AccountService;

/**
 * Beyaz kutu testleri: kaynak kodu inceleyerek dal kapsama (branch coverage) hedefleyin.
 * Her if-else dalı için en az bir test olmalı. Kapsama raporu: mvn test → target/site/jacoco/index.html
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountServiceWhiteBoxTest {

    @Autowired
    private AccountService accountService;

    @Test
    @Tag("hata")
    void wb01_calculateInterest_standartHesap_dogruFaiz() {
        // R-8.3: 999 x 0,03 / 12 = 2,4975; kuruşa yuvarlanınca 2,50 TL. Rapor: H-09
        String no = hesapAc("STANDARD", 999.0);

        Account hesap = accountService.calculateInterest(no);

        assertEquals(1001.50, hesap.getBalance(), 0.001);
    }

    @Test
    @Tag("hata")
    void wb02_calculateInterest_premiumHesap_dogruFaiz() {
        // R-8.3: 239,99 x 0,05 / 12 = 0,99995...; kuruşa yuvarlanınca 1,00 TL. Rapor: H-09
        String no = hesapAc("PREMIUM", 239.99);

        Account hesap = accountService.calculateInterest(no);

        assertEquals(240.99, hesap.getBalance(), 0.001);
    }

    @Test
    void wb03_withdraw_gunlukLimitKontrol_standart() {
        String no = hesapAc("STANDARD", 10000.0);
        accountService.withdraw(no, 3400.0);

        // R-3.5: Bir kuruş fazlası reddedilir, tam limit kabul edilir.
        assertThrows(LimitExceededException.class, () -> accountService.withdraw(no, 1600.01));
        assertEquals(6600.0, accountService.getAccount(no).getBalance(), 0.001);
        Account hesap = accountService.withdraw(no, 1600.0);

        assertEquals(5000.0, hesap.getBalance(), 0.001);
    }

    @Test
    @Tag("hata")
    void wb04_withdraw_gunlukLimitKontrol_premium() {
        // R-3.5: 20.000 TL günlük limite dahildir. Rapor: H-05
        String no = hesapAc("PREMIUM", 30000.0);
        accountService.withdraw(no, 12500.0);

        Account hesap = accountService.withdraw(no, 7500.0);

        assertEquals(10000.0, hesap.getBalance(), 0.001);
    }

    @Test
    void wb05_deposit_nullTutarReddedilir() {
        String no = hesapAc("STANDARD", 100.0);

        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(no, null));

        hesapDegismedi(no, 100.0);
    }

    @Test
    void wb06_withdraw_nullTutarReddedilir() {
        String no = hesapAc("STANDARD", 100.0);

        assertThrows(IllegalArgumentException.class, () -> accountService.withdraw(no, null));

        hesapDegismedi(no, 100.0);
    }

    @Test
    void wb07_transfer_nullTutarReddedilir() {
        String kaynak = hesapAc("STANDARD", 100.0);
        String hedef = hesapAc("PREMIUM", 200.0);

        assertThrows(IllegalArgumentException.class, () -> accountService.transfer(kaynak, hedef, null));

        hesapDegismedi(kaynak, 100.0);
        hesapDegismedi(hedef, 200.0);
    }

    @Test
    void wb08_findAccount_hesapVarsaDoner() {
        String no = hesapAc("STANDARD", 100.0);

        Account hesap = accountService.findAccount(no).orElseThrow();

        assertEquals(no, hesap.getAccountNumber());
        assertEquals(100.0, hesap.getBalance(), 0.001);
    }

    @Test
    void wb09_findAccount_hesapYoksaBosDoner() {
        // Hesap numarası sıfırla başlamaz.
        assertTrue(accountService.findAccount("0000000000").isEmpty());
    }

    private String hesapAc(String tur, double bakiye) {
        return accountService.createAccount("Ali", tur, bakiye).getAccountNumber();
    }

    private void hesapDegismedi(String no, double bakiye) {
        Account hesap = accountService.getAccount(no);
        assertEquals(bakiye, hesap.getBalance(), 0.001);
        assertEquals(AccountStatus.ACTIVE, hesap.getStatus());
        // Bölüm 6: Null tutar için işlem kaydı oluşmaz.
        assertTrue(accountService.getTransactionHistory(no).isEmpty());
    }
}

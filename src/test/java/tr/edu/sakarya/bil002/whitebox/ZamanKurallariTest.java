package tr.edu.sakarya.bil002.whitebox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tr.edu.sakarya.bil002.exception.LimitExceededException;
import tr.edu.sakarya.bil002.model.Account;
import tr.edu.sakarya.bil002.model.Transaction;
import tr.edu.sakarya.bil002.model.TransactionStatus;
import tr.edu.sakarya.bil002.model.TransactionType;
import tr.edu.sakarya.bil002.repository.TransactionRepository;
import tr.edu.sakarya.bil002.service.AccountService;

/**
 * Zamana bağlı kurallar. Uygulama saati değiştirilemediği için işlem normal yoldan yapılır,
 * sonra kaydın zamanı TransactionRepository ile geçmişe alınır. src/main değişmez.
 */
@SpringBootTest
class ZamanKurallariTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @Tag("hata")
    void zm01_standartDunkuCekimBugunkuLimiteSayilmaz() {
        // R-3.5, G-6: Günlük limite yalnız bugünkü çekimler sayılır. Rapor: H-12
        String no = hesapAc("STANDARD", 15000.0);
        accountService.withdraw(no, 5000.0);
        zamanDegistir(no, TransactionType.WITHDRAWAL, LocalDate.now().minusDays(1).atTime(23, 59, 59));

        Account hesap = accountService.withdraw(no, 5000.0);

        assertEquals(5000.0, hesap.getBalance(), 0.001);
    }

    @Test
    @Tag("hata")
    void zm02_premiumDunkuCekimBugunkuLimiteSayilmaz() {
        // R-3.5, G-6: Günlük limite yalnız bugünkü çekimler sayılır. Rapor: H-12
        String no = hesapAc("PREMIUM", 35000.0);
        accountService.withdraw(no, 12000.0);
        zamanDegistir(no, TransactionType.WITHDRAWAL, LocalDate.now().minusDays(1).atTime(23, 59, 59));

        Account hesap = accountService.withdraw(no, 10000.0);

        assertEquals(13000.0, hesap.getBalance(), 0.001);
    }

    @Test
    @Tag("hata")
    void zm03_dunVeBugunKaristiksaYalnizBugunkuCekimSayilir() {
        // R-3.5, G-6: Günlük limite yalnız bugünkü çekimler sayılır. Rapor: H-12
        String no = hesapAc("STANDARD", 15000.0);
        accountService.withdraw(no, 2000.0);
        zamanDegistir(no, TransactionType.WITHDRAWAL, LocalDate.now().minusDays(1).atTime(23, 59, 59));
        accountService.withdraw(no, 2000.0);

        // Bugünkü 2.000 TL çekimden sonra limitte 3.000 TL kalır.
        assertThrows(LimitExceededException.class, () -> accountService.withdraw(no, 3000.01));
        Account hesap = accountService.withdraw(no, 3000.0);

        assertEquals(8000.0, hesap.getBalance(), 0.001);
    }

    @Test
    void zm04_gecmisZamanDamgasinaGoreYenidenEskiyeSiralanir() {
        String no = hesapAc("STANDARD", 100.0);
        accountService.deposit(no, 20.0);
        accountService.withdraw(no, 10.0);

        // İlk kaydın zamanını daha yeni yap.
        LocalDate dun = LocalDate.now().minusDays(1);
        LocalDateTime yeni = dun.atTime(15, 0);
        LocalDateTime eski = dun.atTime(9, 0);
        zamanDegistir(no, TransactionType.DEPOSIT, yeni);
        zamanDegistir(no, TransactionType.WITHDRAWAL, eski);

        List<Transaction> gecmis = accountService.getTransactionHistory(no);

        assertEquals(2, gecmis.size());
        assertEquals(TransactionType.DEPOSIT, gecmis.get(0).getType());
        assertEquals(yeni, gecmis.get(0).getTimestamp());
        assertEquals(TransactionType.WITHDRAWAL, gecmis.get(1).getType());
        assertEquals(eski, gecmis.get(1).getTimestamp());
    }

    private String hesapAc(String tur, double bakiye) {
        return accountService.createAccount("Ali", tur, bakiye).getAccountNumber();
    }

    private void zamanDegistir(String no, TransactionType tur, LocalDateTime zaman) {
        Transaction islem = transactionRepository
                .findByAccountNumberAndTypeAndStatus(no, tur, TransactionStatus.SUCCESS).get(0);
        islem.setTimestamp(zaman);
        transactionRepository.save(islem);
    }
}

package tr.edu.sakarya.bil002.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import tr.edu.sakarya.bil002.exception.InsufficientBalanceException;
import tr.edu.sakarya.bil002.exception.InvalidAccountStateException;
import tr.edu.sakarya.bil002.model.Account;
import tr.edu.sakarya.bil002.model.AccountStatus;
import tr.edu.sakarya.bil002.model.Transaction;
import tr.edu.sakarya.bil002.model.TransactionStatus;
import tr.edu.sakarya.bil002.model.TransactionType;
import tr.edu.sakarya.bil002.service.AccountService;

public class HesapIslemleriAdimlari {

    private static final double TOLERANS = 0.001;

    @Autowired
    private AccountService accountService;

    private final Map<String, String> hesapNumaralari = new HashMap<>();
    private RuntimeException islemHatasi;

    @Given("{word} adlı müşterinin {word} türünde ve {double} TL bakiyeli aktif hesabı var")
    public void aktifHesapOlustur(String musteri, String tur, double bakiye) {
        Account hesap = accountService.createAccount(musteri, tur, bakiye);
        hesapNumaralari.put(musteri, hesap.getAccountNumber());
    }

    @Given("{word} adlı müşterinin hesabı dondurulmuş")
    public void hesabiDondur(String musteri) {
        accountService.freezeAccount(hesapNo(musteri));
    }

    @When("{word} adlı müşteri hesabına {double} TL yatırır")
    public void paraYatir(String musteri, double tutar) {
        islemiDene(() -> accountService.deposit(hesapNo(musteri), tutar));
    }

    @When("{word} adlı müşteri hesabından {double} TL çeker")
    public void paraCek(String musteri, double tutar) {
        islemiDene(() -> accountService.withdraw(hesapNo(musteri), tutar));
    }

    @When("{word} adlı müşteri {word} adlı müşteriye {double} TL gönderir")
    public void paraGonder(String gonderen, String alici, double tutar) {
        islemiDene(() -> accountService.transfer(hesapNo(gonderen), hesapNo(alici), tutar));
    }

    @When("{word} adlı müşteri hesabını dondurur")
    public void hesapDondurmayiDene(String musteri) {
        islemiDene(() -> hesabiDondur(musteri));
    }

    @When("{word} adlı müşteri hesabının dondurmasını kaldırır")
    public void hesapDondurmasiniKaldir(String musteri) {
        islemiDene(() -> accountService.unfreezeAccount(hesapNo(musteri)));
    }

    @When("{word} adlı müşteri hesabını kapatır")
    public void hesapKapatmayiDene(String musteri) {
        islemiDene(() -> accountService.closeAccount(hesapNo(musteri)));
    }

    @Then("işlem başarıyla tamamlanmalı")
    public void islemBasariliOlmali() {
        assertNull(islemHatasi, () -> "İşlemin tamamlanması bekleniyordu: " + islemHatasi);
    }

    @Then("işlem geçersiz tutar nedeniyle reddedilmeli")
    public void gecersizTutarReddedilmeli() {
        assertInstanceOf(IllegalArgumentException.class, islemHatasi);
    }

    @Then("işlem yetersiz bakiye nedeniyle reddedilmeli")
    public void yetersizBakiyeReddedilmeli() {
        assertInstanceOf(InsufficientBalanceException.class, islemHatasi);
    }

    @Then("işlem dondurulmuş/bakiyeli hesap nedeniyle reddedilmeli")
    public void uygunOlmayanHesapReddedilmeli() {
        assertInstanceOf(InvalidAccountStateException.class, islemHatasi);
    }

    @Then("{word} adlı müşterinin bakiyesi {double} TL olmalı")
    public void bakiyeyiDogrula(String musteri, double beklenen) {
        double gercek = accountService.getAccount(hesapNo(musteri)).getBalance();
        assertEquals(beklenen, gercek, TOLERANS);
    }

    @Then("{word} adlı müşterinin hesabı dondurulmuş olmalı/kalmalı")
    public void hesapDondurulmusOlmali(String musteri) {
        hesapDurumunuDogrula(musteri, AccountStatus.FROZEN);
    }

    @Then("{word} adlı müşterinin hesabı aktif olmalı")
    public void hesapAktifOlmali(String musteri) {
        hesapDurumunuDogrula(musteri, AccountStatus.ACTIVE);
    }

    @Then("{word} adlı müşterinin hesabı kapalı olmalı")
    public void hesapKapaliOlmali(String musteri) {
        hesapDurumunuDogrula(musteri, AccountStatus.CLOSED);
    }

    @Then("{word} adlı müşterinin işlem geçmişinde {double} TL tutarında başarılı para yatırma kaydı bulunmalı")
    public void paraYatirmaKaydiniDogrula(String musteri, double tutar) {
        islemKaydiniDogrula(musteri, TransactionType.DEPOSIT, tutar);
    }

    @Then("{word} adlı müşterinin işlem geçmişinde {double} TL tutarında başarılı para çekme kaydı bulunmalı")
    public void paraCekmeKaydiniDogrula(String musteri, double tutar) {
        islemKaydiniDogrula(musteri, TransactionType.WITHDRAWAL, tutar);
    }

    @Then("{word} adlı müşterinin işlem geçmişinde {double} TL tutarında başarılı transfer kaydı bulunmalı")
    public void transferKaydiniDogrula(String musteri, double tutar) {
        islemKaydiniDogrula(musteri, TransactionType.TRANSFER, tutar);
    }

    private String hesapNo(String musteri) {
        String numara = hesapNumaralari.get(musteri);
        assertNotNull(numara, () -> musteri + " için senaryoda hesap açılmadı");
        return numara;
    }

    private void islemiDene(Runnable islem) {
        islemHatasi = null;
        try {
            islem.run();
        } catch (IllegalArgumentException | InsufficientBalanceException | InvalidAccountStateException hata) {
            islemHatasi = hata;
        }
    }

    private void hesapDurumunuDogrula(String musteri, AccountStatus durum) {
        Account hesap = accountService.getAccount(hesapNo(musteri));
        assertEquals(durum, hesap.getStatus());
    }

    private void islemKaydiniDogrula(String musteri, TransactionType tur, double tutar) {
        List<Transaction> gecmis = accountService.getTransactionHistory(hesapNo(musteri));
        assertEquals(1, gecmis.size(), "Hesabın işlem geçmişinde bir kayıt bekleniyordu");
        Transaction kayit = gecmis.get(0);
        assertEquals(tur, kayit.getType());
        assertEquals(TransactionStatus.SUCCESS, kayit.getStatus());
        assertEquals(tutar, kayit.getAmount(), TOLERANS);
    }
}

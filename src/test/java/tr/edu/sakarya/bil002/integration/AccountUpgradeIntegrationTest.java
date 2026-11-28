package tr.edu.sakarya.bil002.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tr.edu.sakarya.bil002.model.AccountStatus.ACTIVE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import tr.edu.sakarya.bil002.model.Account;
import tr.edu.sakarya.bil002.model.AccountStatus;
import tr.edu.sakarya.bil002.repository.AccountRepository;
import tr.edu.sakarya.bil002.service.AccountService;

@SpringBootTest
@AutoConfigureMockMvc
class AccountUpgradeIntegrationTest {

    private static final String HESAP_YOLU = "/api/accounts/{accountNumber}";
    private static final String YUKSELTME_YOLU = HESAP_YOLU + "/upgrade";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @ParameterizedTest
    @CsvSource({"1000.00", "1000.01"})
    void tdd01_upgrade_yeterliBakiyePremiumOlmali(double bakiye) throws Exception {
        String hesapNo = hesapKaydet("STANDARD", ACTIVE, bakiye);

        mockMvc.perform(post(YUKSELTME_YOLU, hesapNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(hesapNo))
                .andExpect(jsonPath("$.accountType").value("PREMIUM"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").value(bakiye));

        hesabiDogrula(hesapNo, "PREMIUM", ACTIVE, bakiye);
    }

    @Test
    void tdd02_upgrade_limitinBirKurusAltiReddedilmeli() throws Exception {
        String hesapNo = hesapKaydet("STANDARD", ACTIVE, 999.99);

        mockMvc.perform(post(YUKSELTME_YOLU, hesapNo))
                .andExpect(status().isUnprocessableEntity());

        hesabiDogrula(hesapNo, "STANDARD", ACTIVE, 999.99);
    }

    @ParameterizedTest
    @CsvSource({"FROZEN", "CLOSED"})
    void tdd03_upgrade_aktifOlmayanHesapReddedilmeli(AccountStatus durum) throws Exception {
        // Yeterli bakiye, hesap durumu kuralının tek başına sınanmasını sağlar.
        String hesapNo = hesapKaydet("STANDARD", durum, 1_000.00);

        mockMvc.perform(post(YUKSELTME_YOLU, hesapNo))
                .andExpect(status().isConflict());

        hesabiDogrula(hesapNo, "STANDARD", durum, 1_000.00);
    }

    @Test
    void tdd04_upgrade_zatenPremiumHesapReddedilmeli() throws Exception {
        String hesapNo = hesapKaydet("PREMIUM", ACTIVE, 1_000.00);

        mockMvc.perform(post(YUKSELTME_YOLU, hesapNo))
                .andExpect(status().isConflict());

        hesabiDogrula(hesapNo, "PREMIUM", ACTIVE, 1_000.00);
    }

    @Test
    void tdd05_upgrade_olmayanHesap404Donmeli() throws Exception {
        String hesapNo = "0000000000";

        mockMvc.perform(post(YUKSELTME_YOLU, hesapNo))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Hesap bulunamadı: " + hesapNo));
    }

    private String hesapKaydet(String hesapTuru, AccountStatus durum, double bakiye) {
        Account hesap = accountService.createAccount("Yükseltme Testi", hesapTuru, bakiye);
        if (durum != ACTIVE) {
            hesap.setStatus(durum);
            accountRepository.save(hesap);
        }
        return hesap.getAccountNumber();
    }

    private void hesabiDogrula(String hesapNo, String hesapTuru, AccountStatus durum, double bakiye) throws Exception {
        mockMvc.perform(get(HESAP_YOLU, hesapNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(hesapNo))
                .andExpect(jsonPath("$.accountType").value(hesapTuru))
                .andExpect(jsonPath("$.status").value(durum.name()))
                .andExpect(jsonPath("$.balance").value(bakiye));
    }
}

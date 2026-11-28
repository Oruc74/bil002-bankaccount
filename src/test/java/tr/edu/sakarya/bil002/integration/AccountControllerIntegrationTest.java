package tr.edu.sakarya.bil002.integration;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AccountControllerIntegrationTest {

    private static final String OLMAYAN_HESAP = "0000000000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Tag("hata")
    void it01_getAccount_bulunamayan_404Donmeli() throws Exception {
        // R-10.2: Olmayan hesap 404 döner. Rapor: H-13
        String yol = "/api/accounts/" + OLMAYAN_HESAP;

        mockMvc.perform(get(yol))
                .andExpect(hataYaniti(HttpStatus.NOT_FOUND, yol));
    }

    @Test
    void it02_createAccount_gecerliIstek_201Donmeli() throws Exception {
        // R-1.3, R-1.5, R-1.6: Bakiye verilmezse aktif ve sıfır bakiyeli hesap açılır.
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ownerName":"Ece Yılmaz","accountType":"STANDARD"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.accountNumber").value(matchesPattern("[1-9][0-9]{9}")))
                .andExpect(jsonPath("$.ownerName").value("Ece Yılmaz"))
                .andExpect(jsonPath("$.accountType").value("STANDARD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.updatedAt").isString());
    }

    @Test
    void it03_deposit_aktifHesap_200Donmeli() throws Exception {
        // R-2.5: Yatırılan tutar bakiyeye eklenir.
        String hesapNo = hesapAc("STANDARD", 100.0);

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":50}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountNumber").value(hesapNo))
                .andExpect(jsonPath("$.balance").value(150.0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @Tag("hata")
    void it04_createAccount_negatifBakiye_400Donmeli() throws Exception {
        // R-1.4: Negatif başlangıç bakiyesi reddedilir. Rapor: H-06
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ownerName":"Ece","accountType":"STANDARD","initialBalance":-0.01}
                        """))
                .andExpect(hataYaniti(HttpStatus.BAD_REQUEST, "/api/accounts"));
    }

    @Test
    void it05_getAccounts_hesaplariListelemeli() throws Exception {
        // API ve G-3: Liste JSON dizisi olarak açılan hesabı içerir.
        String hesapNo = hesapAc("STANDARD", 100.0);

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].accountNumber").value(hasItem(hesapNo)));
    }

    @Test
    void it06_getAccount_mevcutHesap_200Donmeli() throws Exception {
        // R-10.1: Hesap bilgisi döner.
        String hesapNo = hesapAc("STANDARD", 100.0);

        mockMvc.perform(get("/api/accounts/" + hesapNo))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountNumber").value(hesapNo))
                .andExpect(jsonPath("$.ownerName").value("API Testi"))
                .andExpect(jsonPath("$.balance").value(100.0))
                .andExpect(jsonPath("$.accountType").value("STANDARD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @Tag("hata")
    void it07_deposit_sifirTutar_400Donmeli() throws Exception {
        // R-2.2: Sıfır lira yatırma reddedilir. Rapor: H-03
        String hesapNo = hesapAc("STANDARD", 100.0);

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":0}"))
                .andExpect(hataYaniti(HttpStatus.BAD_REQUEST, "/api/accounts/" + hesapNo + "/deposit"));

        mockMvc.perform(get("/api/accounts/" + hesapNo))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountNumber").value(hesapNo))
                .andExpect(jsonPath("$.balance").value(100.0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @Tag("hata")
    void it08_withdraw_tamBakiye_200Donmeli() throws Exception {
        // R-3.4, R-3.6: Bakiyenin tamamı çekilebilir. Rapor: H-04
        String hesapNo = hesapAc("STANDARD", 100.0);

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":100}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountNumber").value(hesapNo))
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @Tag("hata")
    void it09_withdraw_dondurulmusHesap_409Donmeli() throws Exception {
        // R-3.3: Dondurulmuş hesaptan para çekilemez. Rapor: H-02
        String hesapNo = hesapAc("STANDARD", 100.0);

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/freeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":50}"))
                .andExpect(hataYaniti(HttpStatus.CONFLICT, "/api/accounts/" + hesapNo + "/withdraw"));

        mockMvc.perform(get("/api/accounts/" + hesapNo))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountNumber").value(hesapNo))
                .andExpect(jsonPath("$.balance").value(100.0))
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }

    @Test
    void it10_freeze_aktifHesap_200Donmeli() throws Exception {
        // R-5.2: Hesap dondurulur, bakiye korunur.
        String hesapNo = hesapAc("STANDARD", 100.0);

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/freeze"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountNumber").value(hesapNo))
                .andExpect(jsonPath("$.balance").value(100.0))
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }

    @Test
    @Tag("hata")
    void it11_freeze_zatenDondurulmus_409Donmeli() throws Exception {
        // R-5.1: Dondurulmuş hesap tekrar dondurulamaz. Rapor: H-11
        String hesapNo = hesapAc("STANDARD", 100.0);

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/freeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/freeze"))
                .andExpect(hataYaniti(HttpStatus.CONFLICT, "/api/accounts/" + hesapNo + "/freeze"));
    }

    @Test
    void it12_unfreeze_dondurulmusHesap_200Donmeli() throws Exception {
        // R-6.2: Hesap aktifleştirilir, bakiye korunur.
        String hesapNo = hesapAc("STANDARD", 100.0);

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/freeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/unfreeze"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountNumber").value(hesapNo))
                .andExpect(jsonPath("$.balance").value(100.0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void it13_unfreeze_aktifHesap_409Donmeli() throws Exception {
        // R-6.1: Aktif hesabın dondurması kaldırılamaz.
        String hesapNo = hesapAc("STANDARD", 100.0);

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/unfreeze"))
                .andExpect(hataYaniti(HttpStatus.CONFLICT, "/api/accounts/" + hesapNo + "/unfreeze"));
    }

    @Test
    void it14_close_sifirBakiyeliHesap_200Donmeli() throws Exception {
        // R-7.2, R-7.3: Sıfır bakiyeli hesap kapatılır.
        String hesapNo = hesapAc("STANDARD", 0.0);

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/close"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountNumber").value(hesapNo))
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @Tag("hata")
    void it15_close_bakiyeliHesap_409Donmeli() throws Exception {
        // R-7.2: Bir kuruş bakiyesi olan hesap kapatılamaz. Rapor: H-10
        String hesapNo = hesapAc("STANDARD", 0.01);

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/close"))
                .andExpect(hataYaniti(HttpStatus.CONFLICT, "/api/accounts/" + hesapNo + "/close"));

        mockMvc.perform(get("/api/accounts/" + hesapNo))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountNumber").value(hesapNo))
                .andExpect(jsonPath("$.balance").value(0.01))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void it16_interest_standartHesaba30TLFaizEklenmeli() throws Exception {
        // R-8.2, R-8.4: 12.000 TL STANDARD bakiyeye 30 TL aylık faiz eklenir.
        String hesapNo = hesapAc("STANDARD", 12000.0);

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/interest"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountNumber").value(hesapNo))
                .andExpect(jsonPath("$.balance").value(12030.0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void it17_interest_dondurulmusHesap_409Donmeli() throws Exception {
        // R-8.1: Dondurulmuş hesaba faiz uygulanmaz.
        String hesapNo = hesapAc("STANDARD", 100.0);

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/freeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/interest"))
                .andExpect(hataYaniti(HttpStatus.CONFLICT, "/api/accounts/" + hesapNo + "/interest"));
    }

    @Test
    void it18_getTransactions_islemGecmisiDonmeli() throws Exception {
        // R-2.6, R-9.2: Başarılı yatırma geçmişte görünür.
        String hesapNo = hesapAc("STANDARD", 100.0);

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":50}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/accounts/" + hesapNo + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accountNumber").value(hesapNo))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[0].amount").value(50.0))
                .andExpect(jsonPath("$[0].balanceAfter").value(150.0))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].timestamp").isString());
    }

    @Test
    void it19_getTransactions_olmayanHesap_404Donmeli() throws Exception {
        // R-9.1: Olmayan hesabın işlem geçmişi sorgulanamaz.
        String yol = "/api/accounts/" + OLMAYAN_HESAP + "/transactions";

        mockMvc.perform(get(yol))
                .andExpect(hataYaniti(HttpStatus.NOT_FOUND, yol));
    }

    @Test
    void it20_createAccount_bozukJson_400Donmeli() throws Exception {
        // Bölüm 7: Kapanış süslü parantezi eksik JSON reddedilir.
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerName\":\"Ece\",\"accountType\":\"STANDARD\""))
                .andExpect(hataYaniti(HttpStatus.BAD_REQUEST, "/api/accounts"));
    }

    @Test
    void it21_deposit_eksikTutar_400Donmeli() throws Exception {
        // Bölüm 7: Zorunlu amount alanı eksikse istek reddedilir.
        String hesapNo = hesapAc("STANDARD", 100.0);

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(hataYaniti(HttpStatus.BAD_REQUEST, "/api/accounts/" + hesapNo + "/deposit"));
    }

    @Test
    void it22_withdraw_bakiyeyiAsanTutar_422Donmeli() throws Exception {
        // R-3.4: Bakiyeyi bir kuruş aşan çekim reddedilir ve bakiye korunur.
        String hesapNo = hesapAc("STANDARD", 100.0);

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":100.01}"))
                .andExpect(hataYaniti(HttpStatus.UNPROCESSABLE_ENTITY, "/api/accounts/" + hesapNo + "/withdraw"));

        mockMvc.perform(get("/api/accounts/" + hesapNo))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.balance").value(100.0));
    }

    @Test
    void it23_withdraw_bakiyedenDusmeli_200Donmeli() throws Exception {
        // R-3.6: 100 TL bakiyeden 40 TL çekilince 60 TL kalır.
        String hesapNo = hesapAc("STANDARD", 100.0);

        mockMvc.perform(post("/api/accounts/" + hesapNo + "/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":40}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountNumber").value(hesapNo))
                .andExpect(jsonPath("$.balance").value(60.0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private String hesapAc(String hesapTuru, double bakiye) throws Exception {
        byte[] yanit = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"API Testi","accountType":"%s","initialBalance":%s}
                                """.formatted(hesapTuru, bakiye)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsByteArray();

        return objectMapper.readTree(yanit).get("accountNumber").asText();
    }

    private ResultMatcher hataYaniti(HttpStatus beklenen, String yol) {
        return sonuc -> {
            status().is(beklenen.value()).match(sonuc);
            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON).match(sonuc);
            jsonPath("$.timestamp").isString().match(sonuc);
            jsonPath("$.timestamp").isNotEmpty().match(sonuc);
            jsonPath("$.status").value(beklenen.value()).match(sonuc);
            jsonPath("$.error").value(beklenen.getReasonPhrase()).match(sonuc);
            jsonPath("$.message").isString().match(sonuc);
            jsonPath("$.message").isNotEmpty().match(sonuc);
            jsonPath("$.path").value(yol).match(sonuc);
        };
    }

    @Test
    void it30_hesapListesi_desteklenmeyenMetot_405Donmeli() throws Exception {
        // GET /api/accounts uç noktasının hatalı istek testi. HTTP standardında desteklenmeyen metot 405 döner.
        mockMvc.perform(delete("/api/accounts"))
                .andExpect(hataYaniti(HttpStatus.METHOD_NOT_ALLOWED, "/api/accounts"));
    }
}

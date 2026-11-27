package tr.edu.sakarya.bil002.integration;

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
class TransactionControllerIntegrationTest {

    private static final String OLMAYAN_HESAP = "0000000000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void it24_transfer_aktifHesaplar_200Donmeli() throws Exception {
        // R-4.6: Transfer tutarı kaynaktan düşer ve hedefe eklenir.
        String kaynakNo = hesapAc(1000.0);
        String hedefNo = hesapAc(200.0);

        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fromAccount":"%s","toAccount":"%s","amount":250}
                        """.formatted(kaynakNo, hedefNo)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Transfer başarılı"))
                .andExpect(jsonPath("$.fromAccount").value(kaynakNo))
                .andExpect(jsonPath("$.toAccount").value(hedefNo))
                .andExpect(jsonPath("$.amount").value(250.0));

        mockMvc.perform(get("/api/accounts/" + kaynakNo))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.balance").value(750.0));

        mockMvc.perform(get("/api/accounts/" + hedefNo))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.balance").value(450.0));
    }

    @Test
    @Tag("hata")
    void it25_transfer_dondurulmusHedef_409Donmeli() throws Exception {
        // R-4.3, G-5: Hedef dondurulmuşsa transfer reddedilir ve bakiyeler korunur. Rapor: H-07
        String kaynakNo = hesapAc(1000.0);
        String hedefNo = hesapAc(200.0);

        mockMvc.perform(post("/api/accounts/" + hedefNo + "/freeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));

        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fromAccount":"%s","toAccount":"%s","amount":250}
                        """.formatted(kaynakNo, hedefNo)))
                .andExpect(hataYaniti(HttpStatus.CONFLICT, "/api/transactions/transfer"));

        mockMvc.perform(get("/api/accounts/" + kaynakNo))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.balance").value(1000.0));

        mockMvc.perform(get("/api/accounts/" + hedefNo))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.balance").value(200.0))
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }

    @Test
    void it26_getHistory_yeniHesaptaBosDiziDonmeli() throws Exception {
        // R-1.7, R-9.4: Yeni hesabın işlem geçmişi boştur.
        String hesapNo = hesapAc(0.0);

        mockMvc.perform(get("/api/transactions/" + hesapNo))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void it27_getHistory_olmayanHesap_404Donmeli() throws Exception {
        // R-9.1: Alternatif yolda da olmayan hesap 404 döner.
        String yol = "/api/transactions/" + OLMAYAN_HESAP;

        mockMvc.perform(get(yol))
                .andExpect(hataYaniti(HttpStatus.NOT_FOUND, yol));
    }

    @Test
    @Tag("hata")
    void it28_transfer_ayniHesap_400Donmeli() throws Exception {
        // R-4.4: Aynı hesaba transfer reddedilir. Rapor: H-08
        String hesapNo = hesapAc(1000.0);

        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fromAccount":"%s","toAccount":"%s","amount":250}
                        """.formatted(hesapNo, hesapNo)))
                .andExpect(hataYaniti(HttpStatus.BAD_REQUEST, "/api/transactions/transfer"));

        mockMvc.perform(get("/api/accounts/" + hesapNo))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.balance").value(1000.0));
    }

    @Test
    void it29_transfer_yetersizBakiye_422Donmeli() throws Exception {
        // R-4.5, G-5: Kaynak bakiyeyi aşan transfer reddedilir ve iki bakiye de korunur.
        String kaynakNo = hesapAc(100.0);
        String hedefNo = hesapAc(50.0);

        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fromAccount":"%s","toAccount":"%s","amount":100.01}
                        """.formatted(kaynakNo, hedefNo)))
                .andExpect(hataYaniti(HttpStatus.UNPROCESSABLE_ENTITY, "/api/transactions/transfer"));

        mockMvc.perform(get("/api/accounts/" + kaynakNo))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.balance").value(100.0));

        mockMvc.perform(get("/api/accounts/" + hedefNo))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.balance").value(50.0));
    }

    private String hesapAc(double bakiye) throws Exception {
        byte[] yanit = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"API Testi","accountType":"STANDARD","initialBalance":%s}
                                """.formatted(bakiye)))
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
}

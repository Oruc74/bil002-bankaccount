# BIL 002 Dönem Projesi Test Raporu

**Öğrenci Adı:** _(Gün 19'da doldurulacak)_  
**Öğrenci No:** _(Gün 19'da doldurulacak)_  
**Tarih:** _(Gün 19'da doldurulacak)_  
**Kısım 1 Repo URL (BankAccount):** https://github.com/Oruc74/bil002-bankaccount  
**Kısım 2 Repo URL (Selenium + Cucumber):** _(Kısım 2 reposu açılınca doldurulacak)_

Bu rapor BYS üzerinden teslim edilir. Aşağıdaki Bölüm 1–10 Kısım 1'i, en sondaki Kısım 2 başlığı ise Selenium/Cucumber çalışmasını kapsar.

> **Çalışma notu:** Bu dosya bir iskelettir. Her bölümdeki "Durum" satırı bölümün hangi aşamada doldurulacağını gösterir. Teslimden önce bu satırlar ve `<!-- -->` yorumları silinecek, doğrulanmamış bilgi bırakılmayacak.

---

# KISIM 1 — BankAccount API Testleri

## 1. Statik Test Sonuçları
(SonarQube / Checkstyle bulguları)

> Durum: Gün 7'de doldurulacak.

| # | Araç | Dosya:Satır | Kural / Bulgu | Önem | Yorum |
|---|------|-------------|---------------|------|-------|
| 1 | | | | | |

---

## 2. Kara Kutu Test Vakaları

> Durum: 2.1 ve 2.2 Gün 4'te, 2.3 Gün 5'te, 2.4 Gün 6'da doldurulacak. Test ID ile test metodu adı eşleşecek.

### 2.1 Denklik Sınıfı Tablosu

| Test ID | Giriş | Beklenen | Gerçekleşen | Sonuç |
|---------|-------|----------|-------------|-------|
| EP-01 | | | | ✔ / ✘ |

### 2.2 Sınır Değer Analizi Tablosu

| Test ID | Değişken | Sınır | Test Değeri | Beklenen | Gerçekleşen | Sonuç |
|---------|----------|-------|-------------|----------|-------------|-------|
| BVA-01 | | | | | | ✔ / ✘ |

### 2.3 Karar Tablosu

| Koşul / Eylem | K1 | K2 | K3 | K4 | ... |
|---------------|----|----|----|----|-----|
| **Koşul:** | | | | | |
| **Eylem:** | | | | | |
| Test ID | | | | | |
| Sonuç | | | | | |

### 2.4 Durum Geçiş Tablosu

| Test ID | Başlangıç Durumu | Olay | Beklenen Durum | Gerçekleşen Durum | Sonuç |
|---------|------------------|------|----------------|-------------------|-------|
| ST-01 | | | | | ✔ / ✘ |

---

## 3. Beyaz Kutu Test Sonuçları
(JaCoCo ekran görüntüsü ve açıklama)

> Durum: Gün 7–8'de doldurulacak.

- Branch Coverage: %
- Statement Coverage: %

| Metot | Dal (koşul) | Test Metodu | Kapsandı mı? |
|-------|-------------|-------------|--------------|
| | | | |

---

## 4. TDD Uygulaması
(Hangi özellik için, nasıl uygulandı)

> Durum: Gün 9'da doldurulacak.

- **Özellik:**
- **Kırmızı (Red):** Yazılan başarısız test ve hata çıktısı
- **Yeşil (Green):** Testi geçiren en küçük kod değişikliği
- **Yeniden düzenleme (Refactor):** Yapılan iyileştirme
- **İlgili commit'ler:**

---

## 5. BDD Senaryoları
(Feature dosyalarının listesi ve açıklamaları)

> Durum: Gün 10–11'de doldurulacak.

| Feature Dosyası | Senaryo Sayısı | Açıklama | Sonuç |
|-----------------|----------------|----------|-------|
| | | | |

---

## 6. Entegrasyon ve API Testleri
(Postman koleksiyonu özeti)

> Durum: Gün 12–13'te doldurulacak.

| Test ID | Metot | Endpoint | Beklenen Kod | Gerçekleşen Kod | Sonuç |
|---------|-------|----------|--------------|-----------------|-------|
| API-01 | | | | | ✔ / ✘ |

---

## 7. Performans Testi Sonuçları
(JMeter raporu özeti, grafik)

> Durum: Gün 14–15'te doldurulacak.

| Senaryo | Kullanıcı Sayısı | Ort. Yanıt Süresi (ms) | 90p (ms) | Throughput (istek/sn) | Hata % |
|---------|------------------|------------------------|----------|-----------------------|--------|
| | | | | | |

---

## 8. CI/CD Pipeline
(GitHub Actions ekran görüntüsü)

> Durum: Gün 19'da doldurulacak (son commit'teki çalışmanın ekran görüntüsü ve Summary sayfasındaki @hata özeti).

---

## 9. Bulunan Hatalar

> Durum: Hata bulundukça doldurulacak (Gün 4'ten itibaren).

| Hata No | Açıklama | İhlal Edilen Kural | Yakalayan Test | Hangi Teknikle Bulundu | Etki Seviyesi |
|---------|----------|--------------------|----------------|------------------------|---------------|
| | | | | | |

> Her hata için: ilgili gereksinim kodu (örn. R-3.4), hatayı yakalayan test metodu ve
> beklenen / gerçekleşen davranışı belirtin.

<!--
Her hata için aşağıdaki kayıt kullanılacak (Hata No H-01, H-02 ... ve testteki "Rapor: H-xx" yorumuyla aynı):

### H-01 — <tek cümlelik açıklama>

| Alan | Değer |
|------|-------|
| İhlal edilen gereksinim | R-x.y / G-x |
| Yeniden üretme adımları | başlangıç durumu ve işlem sırası |
| Beklenen / Gerçekleşen | gereksinime göre ne olmalıydı / uygulama ne yaptı |
| Yakalayan test | SinifAdi.testMetoduAdi veya feature dosyası + senaryo adı |
| Hangi teknikle bulundu | EP, BVA, karar tablosu, durum geçişi, beyaz kutu, BDD, API, performans, statik analiz |
| Etki seviyesi | Kritik / Yüksek / Orta / Düşük |
-->

---

## 10. Sonuç ve Değerlendirme

> Durum: Gün 19'da doldurulacak. Sorular: Hangi teknik hangi tür hatayı buldu? Hangi hatayı yalnızca belirli bir teknikle bulabildiniz? Projeye yeniden başlasanız neyi farklı yapardınız?

---

# KISIM 2 — Selenium + Cucumber (Web Otomasyonu)

> Durum: Gün 16–18'de doldurulacak.

### K2.1 Seçilen Uygulama

- **Site / uygulama:**
- **Neden seçildi:**
- **Test edilen akışlar (en az 2):**

### K2.2 Senaryo Tablosu

| Feature Dosyası | Senaryo Sayısı | Pozitif/Negatif | Scenario Outline? | Kısa Açıklama |
|-----------------|----------------|-----------------|-------------------|---------------|
| | | | | |

### K2.3 Page Object Model Yapısı

| Page Sınıfı | Sorumluluğu (hangi sayfa/eleman) |
|-------------|----------------------------------|
| | |

### K2.4 Bekleme (Wait) Stratejisi
(Nerede WebDriverWait / ExpectedConditions kullanıldı?)

### K2.5 Karşılaşılan Sorunlar ve Çözümler
(Kırılgan locator, dinamik içerik, zamanlama vb.)

### K2.6 Çalıştırma Sonucu
(`mvn test` çıktısı veya Cucumber HTML raporu ekran görüntüsü)

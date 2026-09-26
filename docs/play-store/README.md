# Rasad'ı Google Play'de yayınlama

Bu klasör Play Console'a girilecek her şeyi içerir: imzalı paket, mağaza metinleri, görseller ve beyan formlarının cevapları.

## Hazır olanlar

| Ne | Nerede |
|---|---|
| İmzalı paket (AAB) | `app/build/outputs/bundle/release/app-release.aab`, yeniden üretmek için `./gradlew :app:bundleRelease` |
| Sürüm | 1.0.0, versionCode 1 (her yeni yüklemede versionCode artmalı) |
| R8 eşleme dosyası | `app/build/outputs/mapping/release/mapping.txt` (çökme raporlarını okunur yapar) |
| Mağaza metinleri | [listing-tr.md](listing-tr.md), [listing-en.md](listing-en.md) |
| Görseller | `assets/icon-512.png`, `assets/feature-graphic-tr.png`, `assets/feature-graphic-en.png`, `assets/screenshots-tr/` ve `assets/screenshots-en/` (8'er adet, 1080×1920); yeniden üretmek için `python3 tools/build_play_assets.py` |
| Gizlilik politikası | https://wienerlabs.xyz/rasad/gizlilik (İngilizcesi /rasad/privacy); uygulamada Hakkında panelinde bağlantısı var |

## Yükleme anahtarı

- Dosya: `~/.config/wienerlabs/rasad/rasad-upload.jks`, takma ad `rasad-upload`, RSA 4096, 2054'e kadar geçerli.
- Şifre: macOS Anahtar Zinciri'nde "Rasad Play upload key" kaydı. Gradle şifreyi derleme sırasında buradan okur; `keystore.properties` yalnızca dosya yolunu tutar ve git dışındadır.
- Sertifika SHA-256: `76:9D:96:C3:00:A9:51:4B:0A:03:3C:C1:32:63:C5:A2:D5:B8:4E:CA:89:FE:5E:55:AA:00:78:B6:4C:55:44:0F`
- Yedek: `.jks` dosyasını ve şifreyi parola yöneticine koy. Play App Signing açık olduğu için bu anahtar kaybolursa Play Console'dan yükleme anahtarı sıfırlaması istenebilir, ama uygulama güncellemeleri o süre boyunca durur.
- Başka bir makinede derlemek için `keystore.properties` dosyasını oluştur ya da şifreyi `RASAD_UPLOAD_PASSWORD` ortam değişkeniyle ver.

## 1. Geliştirici hesabı

Bu adımı sen yaparsın: hesap açmak ve ödeme yapmak senin kimliğinle olur.

1. https://play.google.com/console adresinden kaydol. 25 USD tek seferlik ücret var ve kimlik doğrulaması isteniyor.
2. Hesap türü:
   - **Kuruluş hesabı (Wiener Labs)**: D-U-N-S numarası gerekir (ücretsiz, birkaç gün ile birkaç hafta sürebilir). Kuruluş hesaplarında aşağıdaki kapalı test şartı yok.
   - **Kişisel hesap**: daha hızlı açılır, ama üretime çıkmadan önce kapalı testte en az 12 kişinin 14 gün boyunca katılımda kalması gerekir; ardından üretim erişimine başvurulur.

## 2. Uygulamayı oluştur

Uygulama oluştur: ad "Rasad: Gökyüzü, Hilal, Kıble", varsayılan dil Türkçe (tr-TR), tür Uygulama, Ücretsiz. Geliştirici programı politikaları ve ABD ihracat yasaları beyanlarını işaretle.

## 3. Uygulama içeriği (Politika > Uygulama içeriği)

- **Gizlilik politikası**: `https://wienerlabs.xyz/rasad/gizlilik`
- **Uygulama erişimi**: Tüm işlevler özel erişim gerekmeden kullanılabilir.
- **Reklamlar**: Uygulamada reklam yok.
- **İçerik derecelendirmesi (IARC)**: kategori "Referans, Haber veya Eğitim". Şiddet, cinsellik, küfür, uyuşturucu, alkol, tütün, kumar: hayır. Kullanıcılar arası iletişim ya da içerik paylaşımı: hayır. Kullanıcının konumunu başka kullanıcılarla paylaşma: hayır. Dijital satın alma: hayır. Beklenen sonuç: herkes için, 3+.
- **Hedef kitle**: 13-15, 16-17 ve 18 yaş ve üstü. Uygulama çocuklara yönelik değil; 13 yaş altını seçmek Aileler politikasını devreye sokar.
- **Haber uygulaması**: Hayır. **Devlet uygulaması**: Hayır. **Finans ve sağlık özellikleri**: Yok.
- **Konum izni**: yalnızca ön planda kullanılıyor; arka planda konum yok, ayrıca beyan gerekmiyor.

### Veri güvenliği formu

| Soru | Cevap |
|---|---|
| Uygulama kullanıcı verisi topluyor ya da paylaşıyor mu? | Evet |
| Toplanan veri türleri | Konum > Yaklaşık konum |
| Paylaşılıyor mu? | Hayır |
| Geçici olarak mı işleniyor? | Evet |
| Toplama zorunlu mu? | Hayır, kullanıcı konum iznini vermeyebilir |
| Amaç | Uygulama işlevselliği |
| Aktarım sırasında şifreleniyor mu? | Evet |
| Kullanıcı verisinin silinmesini isteme yolu var mı? | Hayır (hesap yok, uygulama veri saklamaz) |

Gerekçe: Uygulamanın internet izni yok ve bütün hesaplar cihazda. Cihazdan çıkan tek veri, yer adını göstermek için Android'in adres servisine (Google Play Hizmetleri) verilen, yaklaşık 2 kilometreye yuvarlanmış konum. Hassas konum yalnızca cihazda kullanılıyor, cihazdan çıkmıyor.

## 4. Mağaza girişi

- **Ana giriş (tr-TR)**: [listing-tr.md](listing-tr.md) metinleri, `icon-512.png`, `feature-graphic-tr.png`, `screenshots-tr/` klasöründeki 8 telefon ekran görüntüsü.
- **Çeviri ekle, English (en-US)**: [listing-en.md](listing-en.md), `feature-graphic-en.png`, `screenshots-en/`.
- **Kategori**: Uygulama > Eğitim.
- **İletişim bilgileri**: herkese açık e-posta adresi (zorunlu) ve web sitesi `https://wienerlabs.xyz`.

## 5. Sürüm

1. **Dahili test**: AAB'yi yükle ve Google'ın yönettiği uygulama imzalamayı (Play App Signing) kabul et. Kendini test kullanıcısı olarak ekle, uygulamayı Play'den kurup dene. Ön lansman raporu gerçek cihazlarda otomatik çalışır.
2. **Kapalı test** (kişisel hesapta zorunlu): en az 12 test kullanıcısı (e-posta listesi ya da Google Grubu), 14 gün kesintisiz katılım.
3. **Üretim**: ülkeleri seç, sürümü oluştur, sürüm notlarını ekle ve incelemeye gönder. Yeni hesaplarda inceleme birkaç gün sürebilir.

## Notlar

- Diyanet meali Tanzil üzerinden yalnızca ticari olmayan kullanım için yayımlanıyor. Uygulama ücretsiz ve reklamsız kaldıkça bu şarta uyuyor; ücretli sürüm ya da reklam eklenirse yayıncıdan izin alınmalı.
- Android 16'da dikey ekran kilidi tabletlerde ve katlanabilir cihazlarda uygulanmıyor; uygulama buralarda yatay açılır ve içerik ortalanmış 720 dp genişlikte gösterilir.

## Kontrol listesi

- [ ] Yükleme anahtarı ve şifresi parola yöneticisine yedeklendi
- [ ] Geliştirici hesabı açıldı ve doğrulandı
- [ ] Uygulama oluşturuldu, uygulama içeriği beyanları dolduruldu
- [ ] Türkçe ve İngilizce mağaza girişleri, görseller ve iletişim e-postası girildi
- [ ] AAB dahili teste yüklendi, Play'den kurulup denendi
- [ ] Gerekiyorsa kapalı test 14 gün tamamlandı
- [ ] Üretim sürümü incelemeye gönderildi

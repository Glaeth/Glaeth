# :)

Kopyalanan her metin veya linki telefonda saklayan ve aynı anda sana özel
bir sunucu adresine gönderen bir Android uygulaması.

WhatsApp sohbeti açılmaz. Kayıt, uygulama listesine düşer ve açık olan
internet gönderimiyle `https://ntfy.sh/<kanal>` adresine gider. O adresi
tarayıcıda veya ücretsiz ntfy uygulamasında açınca kopyaları görürsün.

## APK'yı indir ve kur

Hazır derlenmiş dosya: [`dist/clipsmile.apk`](dist/clipsmile.apk)

1. APK'yı telefona indir.
2. Dosyayı aç ve "yine de yükle" / bilinmeyen kaynaklara izin ver.
3. Uygulamayı aç. Sarı şerit çıkarsa ona dokunup **diğer uygulamaların üzerinde göster** iznini aç (arka planda kopyaları yakalamak için şart).
4. Bildirim iznini de kabul et. Üstteki bildirimi silme; silinirse arka plan izleme durur.
5. Ana ekrandaki adresi aç. Bu, kopyaların düştüğü sunucu sayfasıdır. Adresi Ayarlar'dan kopyalayıp başka telefonda da açabilirsin.
6. Bir link kopyala. Samsung klavyesi açık kalabilir. Metin listede görünür ve aynı adrese gider.

Aynı imzayla yeniden kurulursa uygulama güncellenir, veriler silinmez.

## Özellikler

- **Otomatik pano izleme** — Herhangi bir uygulamada bir metin kopyaladığınızda
  arka planda çalışan bir servis bunu yakalar ve yerel veritabanına kaydeder.
  Android 10 ve üzerinde işletim sistemi, pano içeriğini yalnızca o an odakta
  olan uygulamaya verir; bu yüzden uygulama hem arka planda çalışan bir
  bildirim servisi hem de uygulama ekranı açıkken devreye giren ikinci bir
  dinleyici kullanarak yakalamayı güvenilir hale getirir.
- **Alt alta, boşluklu liste** — Her kopyalanan metin kendi kartında, en
  yeniden en eskiye doğru sıralanmış şekilde listelenir. Dışa aktarılan/
  paylaşılan metinde kayıtlar arasına boş satır bırakılır.
- **Sunucuya otomatik gönderme** — İnternete gönderme varsayılan olarak
  açıktır. Her yeni kopya [ntfy.sh](https://ntfy.sh) üzerindeki sana özel
  kanala gider. Adresi ana ekranda görürsün; Ayarlar'dan kapatabilir,
  kanal adını değiştirebilir veya test mesajı gönderebilirsin.
- **E-postaya aktarma** — Ana ekrandaki "Export Data" düğmesi, kayıtları
  aralarında boş satır bırakarak `guray.vatanuk@gmail.com` adresine yazılmış
  bir e-posta taslağı açar. Mesaj, e-posta uygulamasında Gönder'e basılınca gider.
- **WhatsApp'a elle gönderme** — Tek bir kaydı ya da tüm listeyi "WhatsApp'a
  Gönder" düğmesiyle de paylaşabilirsiniz. WhatsApp kurulu değilse genel
  paylaşım menüsüne geri döner; çok uzun bir metin listesi paylaşılacaksa
  otomatik olarak bir `.txt` dosyası olarak eklenir.
- **Kayıt yönetimi** — Tek tek kopya/sil/paylaş, tümünü temizle, kaydırarak
  silme ve "geri al" bildirimi (Snackbar) desteklenir.
- **Pano izleme anahtarı** — Üst çubuktaki anahtar ile izlemeyi açıp
  kapatabilir, tercihiniz kalıcı olarak hatırlanır.
- **Pokémon temalı arayüz** — Pikachu sarısı ve Poké Topu kırmızı/beyazından
  oluşan bir renk paleti, Pokédex'i andıran bir üst çubuk, piksel tarzı
  başlıklar (açık kaynak "Press Start 2P" yazı tipi) ve uygulama ikonu olarak
  özgün bir Pikachu kafası illüstrasyonu kullanılır.

## Proje yapısı

```
app/
  src/main/java/com/clipsmile/app/
    ClipSmileApp.kt              Uygulama sınıfı, bildirim kanalını oluşturur
    data/                        Room veritabanı, DAO, repository ve internet
                                  senkronizasyon tercihleri/yöneticisi
    service/                     Panoyu izleyen ön planda servis
    ui/                          MainActivity, SettingsActivity, anlık pano
                                  okuma ekranı, RecyclerView adapter
    util/                        Elle WhatsApp paylaşımı, ntfy.sh istemcisi,
                                  tarih yardımcıları
  src/main/res/                  Layout, string, renk, yazı tipi ve ikon
                                  kaynakları
```

- **Dil / araçlar:** Kotlin, Android Gradle Plugin 8.5, Gradle 8.7
- **Mimari:** İki Activity (ana ekran + ayarlar) + Room + Kotlin
  Coroutines/Flow, ViewBinding
- **Minimum SDK:** 24 (Android 7.0) · **Hedef SDK:** 34

## Derleme

Android Studio ile projeyi açıp çalıştırabilir ya da komut satırından
derleyebilirsiniz:

```bash
./gradlew :app:assembleDebug
```

Üretilen APK: `app/build/outputs/apk/release/app-release.apk`
(Depodaki indirilebilir kopya: `dist/clipsmile.apk`)

## Notlar

- Android 10 ve sonrasında pano içeriğini yalnızca o an ekranda olan
  uygulama görebilir. Bu yüzden uygulama, arka plandayken kopyayı okuyabilmek
  için kısa bir an öne çıkar. Bunun çalışması için **diğer uygulamaların
  üzerinde göster** izni gerekir; izin yoksa ana ekranda sarı bir uyarı çıkar.
- Bildirim izni (Android 13+) ve pano izleme servisi, uygulama ilk açıldığında
  ve anahtar her açıldığında istenir/başlatılır.
- WhatsApp paylaşımı `com.whatsapp` paketini, yoksa `com.whatsapp.w4b`
  (WhatsApp Business) paketini dener; hiçbiri yoksa sistemin paylaşım
  menüsünü açar.
- İnternete gönderme özelliği, hesap/API anahtarı gerektirmeyen genel
  `ntfy.sh` servisini kullanır. Kanal adı, kimliği doğrulanmamış bu genel
  adresi koruyan tek "sır" olduğundan, uygulama içinde bu adresi yalnızca
  güvendiğiniz kişilerle paylaşmanız konusunda uyarır.
- Pikachu kafası ve Poké Topu simgeleri, bu proje için çizilmiş özgün vektör
  illüstrasyonlardır; resmi Nintendo/Game Freak görselleri kullanılmamıştır.
  "Press Start 2P" yazı tipi SIL Open Font License ile ücretsiz/özgür bir
  yazı tipidir.

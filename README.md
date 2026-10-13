# :)

Android uygulaması: cihazda kopyaladığınız her metni otomatik olarak algılar,
alt alta (aralarında boşluk bırakarak) kaydeder ve istediğiniz kaydı veya tüm
listeyi tek dokunuşla WhatsApp üzerinden başkasına gönderebilirsiniz.

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
- **WhatsApp'a gönderme** — Tek bir kaydı ya da tüm listeyi "WhatsApp'a
  Gönder" düğmesiyle paylaşabilirsiniz. WhatsApp kurulu değilse genel paylaşım
  menüsüne geri döner; çok uzun bir metin listesi paylaşılacaksa otomatik
  olarak bir `.txt` dosyası olarak eklenir.
- **Kayıt yönetimi** — Tek tek kopya/sil/paylaş, tümünü temizle, kaydırarak
  silme ve "geri al" bildirimi (Snackbar) desteklenir.
- **Pano izleme anahtarı** — Üst çubuktaki anahtar ile izlemeyi açıp
  kapatabilir, tercihiniz kalıcı olarak hatırlanır.

## Proje yapısı

```
app/
  src/main/java/com/clipsmile/app/
    ClipSmileApp.kt              Uygulama sınıfı, bildirim kanalını oluşturur
    data/                        Room veritabanı, DAO ve repository katmanı
    service/                     Panoyu izleyen ön planda (foreground) servis
    ui/                          MainActivity, RecyclerView adapter, kaydırarak silme
    util/                        WhatsApp paylaşım yardımcıları, tarih/biçimlendirme
  src/main/res/                  Layout, string, renk ve ikon kaynakları
```

- **Dil / araçlar:** Kotlin, Android Gradle Plugin 8.5, Gradle 8.7
- **Mimari:** Tek Activity + Room + Kotlin Coroutines/Flow, ViewBinding
- **Minimum SDK:** 24 (Android 7.0) · **Hedef SDK:** 34

## Derleme

Android Studio ile projeyi açıp çalıştırabilir ya da komut satırından
derleyebilirsiniz:

```bash
./gradlew :app:assembleDebug
```

Üretilen APK: `app/build/outputs/apk/debug/app-debug.apk`

## Notlar

- Bildirim izni (Android 13+) ve pano izleme servisi, uygulama ilk açıldığında
  ve anahtar her açıldığında istenir/başlatılır.
- WhatsApp paylaşımı `com.whatsapp` paketini, yoksa `com.whatsapp.w4b`
  (WhatsApp Business) paketini dener; hiçbiri yoksa sistemin paylaşım
  menüsünü açar.

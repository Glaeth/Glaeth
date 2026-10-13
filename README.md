# :)

Pokémon temalı bir Android uygulaması: cihazda kopyaladığınız her metni
otomatik olarak algılar, alt alta (aralarında boşluk bırakarak) kaydeder;
istediğiniz kaydı veya tüm listeyi WhatsApp üzerinden başkasına gönderebilir,
ayrıca her yeni kopyayı internet üzerinden anında ulaşabileceğiniz bir adrese
de gönderebilir.

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
- **İnternete anlık gönderme** — Ayarlar ekranından açıldığında, kopyaladığınız
  her şey [ntfy.sh](https://ntfy.sh) üzerinden kendi özel kanalınıza anında
  gönderilir. Bu kanalın adresini (`https://ntfy.sh/<kanal-adı>`) tarayıcınızda
  açarak ya da ücretsiz ntfy uygulamasıyla takip ederek başka bir cihazdan,
  her yerden ulaşabilirsiniz. Kanal adı değiştirilebilir/yeniden
  oluşturulabilir, bağlantı kopyalanabilir/paylaşılabilir ve bir test mesajı
  gönderilerek bağlantı doğrulanabilir.
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
    service/                     Panoyu izleyen ön planda (foreground) servis
    ui/                          MainActivity, SettingsActivity, RecyclerView
                                  adapter, kaydırarak silme
    util/                        WhatsApp paylaşımı, ntfy.sh istemcisi,
                                  tarih/biçimlendirme yardımcıları
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

Üretilen APK: `app/build/outputs/apk/debug/app-debug.apk`

## Notlar

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

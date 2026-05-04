# Glaeth

Modern Android gunluk takip uygulamasi. Kardesinizin uyku saatlerini,
ogunlerini, cilt fotograflarini ve odevlerini tek ekrandan takip etmek icin
Jetpack Compose ile tasarlandi.

## Ozellikler

- Uyku gunlugu: uyuma/uyanma saati, otomatik toplam sure, yas bazli yeterlilik
  yorumu ve daha detayli grafik.
- Ogun takibi: tarih bazli sabah, ogle, aksam ve ara ogun kayitlari; yasa gore
  beslenme onerileri.
- Cilt arsivi: tekli veya toplu fotograf secimi, kalici galeri izinleri,
  time-lapse seridi, gun numarasi, yuz bolgesi, urun notlari, fotograf onizleme
  ve saga/sola kaydirarak silme.
- Odev panosu: ders, odev adi, teslim tarihi, oncelik etiketi, ek notu ve silme.
- Ana sayfa: motivasyon karti, su takibi, yaklasan odev geri sayimi ve hizli
  aksiyonlar.
- Profil: sag ustten isim, yas, cinsiyet ve profil fotografi duzenleme.
- Ayarlar: siyah/grafit/lacivert arka plan secimi.
- SQLite tabanli lokal database, Material 3, siyah-gri glass tema ve cam efektli
  kartlar.

## Calistirma

Android Studio ile projeyi acip `:app` modulunu calistirin. Komut satirindan
derlemek icin Android SDK ve Gradle wrapper hazir oldugunda:

```bash
./gradlew assembleDebug
```

## APK

Hazir debug APK dosyasi `downloads/Glaeth-debug.apk` altindadir. Android telefonda
kurarken bilinmeyen kaynaklardan yuklemeye izin vermeniz gerekebilir.

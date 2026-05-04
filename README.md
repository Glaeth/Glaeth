# Glaeth

Modern Android günlük takip uygulaması. Kardeşinizin uyku saatlerini,
öğünlerini, cilt fotoğraflarını ve ödevlerini tek ekrandan takip etmek için
Jetpack Compose ile tasarlandı.

## Özellikler

- Uyku günlüğü: uyuma/uyanma saati, otomatik toplam süre, yaş bazlı yeterlilik
  yorumu ve daha detaylı grafik.
- Öğün takibi: tarih bazlı sabah, öğle, akşam ve ara öğün kayıtları; yaşa göre
  beslenme önerileri.
- Cilt arşivi: tekli veya toplu fotoğraf seçimi, kalıcı galeri izinleri,
  time-lapse şeridi, gün numarası, yüz bölgesi, ürün notları, fotoğraf önizleme
  ve sağa/sola kaydırarak silme.
- Ödev panosu: ders, ödev adı, teslim tarihi, öncelik etiketi, ek notu ve silme.
- Ana sayfa: motivasyon kartı, su takibi, yaklaşan ödev geri sayımı ve hızlı
  aksiyonlar.
- Profil: sağ üstten isim, yaş, cinsiyet ve profil fotoğrafı düzenleme.
- Ayarlar: siyah/grafit/lacivert arka plan seçimi.
- SQLite tabanlı lokal database, Material 3, siyah-gri glass tema ve cam efektli
  kartlar.

## Çalıştırma

Android Studio ile projeyi açıp `:app` modülünü çalıştırın. Komut satırından
derlemek için Android SDK ve Gradle wrapper hazır olduğunda:

```bash
./gradlew assembleDebug
```

## APK

Hazır debug APK dosyası `downloads/Glaeth-debug.apk` altındadır. Android telefonda
kurarken bilinmeyen kaynaklardan yüklemeye izin vermeniz gerekebilir.

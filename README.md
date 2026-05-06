# Glaeth

Modern Android günlük takip uygulaması. Uyku, öğün, cilt fotoğrafları, su,
ödev ve aile bütçesini tek ekrandan takip etmek için Jetpack Compose ile
tasarlandı.

## Modüller

- **Ana Sayfa**: Saate göre Türkçe selamlama, Duolingo tarzı cilt/su/öğün
  streak şeritleri, animasyonlu cilt skor halkası, hızlı istatistikler ve
  son güncellemeler akışı.
- **Uyku günlüğü**: Yatma, uyanma, otomatik süre, yaşa göre yeterlilik kararı
  ve haftalık grafik.
- **Öğün takibi**: Sabah, öğle, akşam ve ara öğün kayıtları; yaşa göre
  beslenme önerisi.
- **Su takibi**: Bardak/şişe ön ayarları, manuel ml girişi, günlük toplam,
  hedef ayarı ve geçmiş.
- **Cilt arşivi**: Tekli/toplu fotoğraf seçimi, kalıcı galeri izinleri,
  time-lapse, gün numarası, yüz bölgesi, ürün notu, tam ekran fotoğraf,
  sola kaydırarak silme.
- **Ödev panosu**: Ders, ad, teslim tarihi, öncelik etiketi, dosya/link notu,
  silme.
- **Bütçe paneli**: Çoklu Kişi (Kasa) desteği, banka/kart/nakit hesapları,
  kategoriler (Maaş, Kira, Elektrik, Su, Doğalgaz, Market, Abonelik vb.),
  toplam bakiye, aylık gider, pasta grafiği, bütçe sınırı uyarısı, para
  birimi seçimi (₺/$/€) ve sola kaydırarak silme.
- **Ayarlar**: Tema modu (Sistem / Açık / Koyu), beş renk paleti
  (Obsidyen, Aurora, Mocha, Sakura, Orman), profil düzenleme, para birimi,
  JSON yedek alma ve geri yükleme (Storage Access Framework).
- **Profil**: Sağ üstten isim, yaş, cinsiyet (Erkek/Kadın) ve profil
  fotoğrafı düzenleme.

## Mimari

- Tek modüllü Android uygulaması, Kotlin + Jetpack Compose Material 3.
- Veri saklama: SQLiteOpenHelper üzerinde JSON payload, Repository ile
  uygulama UI'ından soyutlanmış.
- Tema değiştirme, JSON yedek/geri yükleme ve modüller arası akış için
  `AppData` immutable durum nesnesi tek kaynak.

## Çalıştırma

Android Studio ile projeyi açıp `:app` modülünü çalıştırın. Komut satırından
derlemek için Android SDK ve Gradle wrapper hazır olduğunda:

```bash
./gradlew assembleDebug
```

## APK

Hazır debug APK dosyası `downloads/Glaeth-debug.apk` altındadır. Android
telefonda kurarken bilinmeyen kaynaklardan yüklemeye izin vermeniz
gerekebilir.

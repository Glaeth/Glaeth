package com.clipsmile.app.ui

import android.content.Context
import com.clipsmile.app.R
import com.clipsmile.app.data.Den

/**
 * Level 1 keeps the original pug. Each dog has its own take from a
 * public-domain bark, played at the recorded pitch.
 */
data class DogCompanion(
    val minLevel: Int,
    val nameRes: Int,
    val titleRes: Int,
    val foodNameRes: Int,
    val fedRes: Int,
    val portraitRes: Int,
    val foodRes: Int,
    val photo: Boolean,
    val model: String,
    val barkRes: Int,
    val id: String = "",
    val goldPrice: Int = 0,
    val gemPrice: Int = 0,
    val starter: Boolean = false,
    val label: String = "",
    val blurb: String = "",
    val rarity: String = "Yaygın",
    val portraitAsset: String = ""
) {
    fun stableId(): String = id.ifBlank { model }

    fun displayName(context: Context): String =
        label.ifBlank { context.getString(nameRes) }

    fun displayTitle(context: Context): String =
        blurb.ifBlank { context.getString(titleRes) }
}

object DogRoster {
    private fun pet(
        minLevel: Int,
        id: String,
        label: String,
        blurb: String,
        foodName: Int,
        fed: Int,
        foodRes: Int,
        bark: Int,
        gold: Int,
        gems: Int,
        rarity: String
    ) = DogCompanion(
        minLevel,
        R.string.dog_pug_name,
        R.string.dog_pug_title,
        foodName,
        fed,
        R.drawable.face_pug,
        foodRes,
        false,
        id,
        bark,
        id,
        gold,
        gems,
        false,
        label,
        blurb,
        rarity,
        "faces/$id.png"
    )

    val all = listOf(
        DogCompanion(1, R.string.dog_pug_name, R.string.dog_pug_title, R.string.food_durum, R.string.fed_durum, R.drawable.face_pug, R.drawable.ic_doner, true, "pug", R.raw.bark_pug, "pug", 480, 0, true, rarity = "Nadir", portraitAsset = "faces/pug.png"),
        DogCompanion(1, R.string.dog_cat_name, R.string.dog_cat_title, R.string.food_fish, R.string.fed_fish, R.drawable.face_cat, R.drawable.ic_food_fish, true, "cat", R.raw.bark_cat, "cat", 480, 0, true, rarity = "Nadir", portraitAsset = "faces/cat.png"),
        DogCompanion(1, R.string.dog_capy_name, R.string.dog_capy_title, R.string.food_grass, R.string.fed_grass, R.drawable.face_capy, R.drawable.ic_food_bread, false, "capy", R.raw.bark_capy, "capy", 480, 0, true, rarity = "Nadir", portraitAsset = "faces/capy.png"),
        pet(5, "kipir", "Kıpır", "Oynak kedi", R.string.food_fish, R.string.fed_fish, R.drawable.ic_food_fish, R.raw.bark_cat, 1100, 0, "Sıradışı"),
        pet(8, "tekir", "Tekir", "Çizgili kedi", R.string.food_fish, R.string.fed_fish, R.drawable.ic_food_fish, R.raw.bark_cat, 1200, 0, "Sıradışı"),
        pet(12, "baba", "Baba", "Bağıran kedi", R.string.food_fish, R.string.fed_fish, R.drawable.ic_food_fish, R.raw.bark_kahkaha, 2100, 0, "Nadir"),
        pet(6, "gece", "Gece", "Siyah kedi", R.string.food_fish, R.string.fed_fish, R.drawable.ic_food_fish, R.raw.bark_cat, 860, 0, "Yaygın"),
        pet(7, "cizgi", "Çizgi", "Karton kedi", R.string.food_fish, R.string.fed_fish, R.drawable.ic_food_fish, R.raw.bark_cat, 980, 0, "Yaygın"),
        pet(4, "pati", "Pati", "Küçük iskelet kedi", R.string.food_milk, R.string.fed_milk, R.drawable.ic_food_milk, R.raw.bark_bebek, 640, 0, "Yaygın"),
        pet(9, "adim", "Adım", "Yürüyen kedi", R.string.food_fish, R.string.fed_fish, R.drawable.ic_food_fish, R.raw.bark_cat, 720, 0, "Yaygın"),
        pet(18, "cadi", "Cadı", "Şapkalı kedi", R.string.food_berry, R.string.fed_berry, R.drawable.ic_food_berry, R.raw.bark_cat, 2500, 0, "Nadir"),
        pet(14, "sosis", "Sosis", "Uzun köpek", R.string.food_steak, R.string.fed_steak, R.drawable.ic_food_steak, R.raw.bark_somun, 1680, 0, "Sıradışı"),
        pet(11, "tuylu", "Tüylü", "Tüylü köpek", R.string.food_bread, R.string.fed_bread, R.drawable.ic_food_bread, R.raw.bark_pamuk, 1800, 0, "Sıradışı"),
        pet(13, "dost", "Dost", "Sokak köpeği", R.string.food_steak, R.string.fed_steak, R.drawable.ic_food_steak, R.raw.bark_kangal, 1900, 0, "Sıradışı"),
        pet(10, "yavru", "Yavru", "Tombul yavru", R.string.food_milk, R.string.fed_milk, R.drawable.ic_food_milk, R.raw.bark_bebek, 1500, 0, "Sıradışı"),
        pet(28, "sirtlan", "Sırtlan", "İri sırtlan", R.string.food_steak, R.string.fed_steak, R.drawable.ic_food_steak, R.raw.bark_kangal, 0, 80, "Destansı"),
        pet(30, "jake", "Jake", "Sarı macera köpeği", R.string.food_simit, R.string.fed_simit, R.drawable.ic_food_simit, R.raw.bark_kahkaha, 0, 95, "Efsane"),
        pet(22, "guccuk", "Güççük", "Boyun uzatan köpek", R.string.food_bone, R.string.fed_bone, R.drawable.ic_food_steak, R.raw.bark_pug, 0, 60, "Destansı"),
        pet(16, "maxwell", "Maxwell", "Dikenli kedi", R.string.food_fish, R.string.fed_fish, R.drawable.ic_food_fish, R.raw.bark_cat, 1400, 0, "Sıradışı"),
        pet(26, "munch", "Munch", "Kısa bacaklı kedi", R.string.food_fish, R.string.fed_fish, R.drawable.ic_food_fish, R.raw.bark_tombul, 0, 70, "Destansı"),
        pet(20, "oiia", "Oiia", "Dönen kedi", R.string.food_cheese, R.string.fed_cheese, R.drawable.ic_food_cheese, R.raw.bark_cat, 2800, 0, "Nadir"),
        pet(24, "shiba", "Shiba", "Boyalı shiba", R.string.food_rice, R.string.fed_rice, R.drawable.ic_food_rice, R.raw.bark_pamuk, 3200, 0, "Nadir"),
        pet(21, "meowth", "Meowth", "Altın kedi", R.string.food_fish, R.string.fed_fish, R.drawable.ic_food_fish, R.raw.bark_cat, 0, 45, "Destansı"),
        pet(3, "pop", "Pop", "Ağız açan kedi", R.string.food_fish, R.string.fed_fish, R.drawable.ic_food_fish, R.raw.bark_bebek, 420, 0, "Yaygın"),
        pet(17, "smurf", "Şirin", "Mavi kedi", R.string.food_berry, R.string.fed_berry, R.drawable.ic_food_berry, R.raw.bark_boncuk, 2300, 0, "Nadir"),
        pet(32, "dissiz", "Dişsiz", "Dişsiz ejder", R.string.food_steak, R.string.fed_steak, R.drawable.ic_food_steak, R.raw.bark_kangal, 0, 110, "Efsane"),
        pet(5, "ucat", "Ucat", "Tıknaz kedi", R.string.food_fish, R.string.fed_fish, R.drawable.ic_food_fish, R.raw.bark_tombul, 560, 0, "Yaygın"),
        pet(23, "kurt", "Kurt", "Küp kurt", R.string.food_steak, R.string.fed_steak, R.drawable.ic_food_steak, R.raw.bark_kangal, 0, 55, "Destansı")
    )

    fun forLevel(level: Int): DogCompanion =
        all.lastOrNull { level >= it.minLevel } ?: all.first()

    fun byId(id: String): DogCompanion =
        all.firstOrNull { it.stableId() == id } ?: all.first()

    /** The animal currently kept at home. */
    fun selected(context: Context): DogCompanion {
        val den = Den(context)
        return if (den.onboarded()) byId(den.activeId()) else all.first()
    }

    fun homeName(context: Context): String {
        val dog = selected(context)
        val fallback = dog.displayName(context)
        val den = Den(context)
        return if (den.onboarded()) den.displayName(dog.stableId(), fallback) else fallback
    }

    fun priceLabel(dog: DogCompanion): String =
        if (dog.gemPrice > 0) "${dog.gemPrice} kristal" else "${dog.goldPrice} altın"
}

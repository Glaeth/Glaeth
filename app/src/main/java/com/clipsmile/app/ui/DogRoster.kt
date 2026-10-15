package com.clipsmile.app.ui

import com.clipsmile.app.R

/**
 * Level 1 keeps the original pug. Later companions each eat something
 * that fits that dog. Barks are the same public-domain recording,
 * pitched so a tiny dog yips and the kangal woofs.
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
    val barkRate: Float
)

object DogRoster {
    val all = listOf(
        DogCompanion(1, R.string.dog_pug_name, R.string.dog_pug_title, R.string.food_durum, R.string.fed_durum, R.drawable.ic_pug_icon, R.drawable.ic_doner, true, "pug", R.raw.bark_small, 1.04f),
        DogCompanion(15, R.string.dog_pamuk_name, R.string.dog_pamuk_title, R.string.food_rice, R.string.fed_rice, R.drawable.ic_dog_pamuk, R.drawable.ic_food_rice, false, "pamuk", R.raw.bark_small, 0.96f),
        DogCompanion(30, R.string.dog_boncuk_name, R.string.dog_boncuk_title, R.string.food_cheese, R.string.fed_cheese, R.drawable.ic_dog_boncuk, R.drawable.ic_food_cheese, false, "boncuk", R.raw.bark_tiny, 1.06f),
        DogCompanion(45, R.string.dog_tombul_name, R.string.dog_tombul_title, R.string.food_simit, R.string.fed_simit, R.drawable.ic_dog_tombul, R.drawable.ic_food_simit, false, "tombul", R.raw.bark_small, 0.88f),
        DogCompanion(60, R.string.dog_bebek_name, R.string.dog_bebek_title, R.string.food_milk, R.string.fed_milk, R.drawable.ic_dog_bebek, R.drawable.ic_food_milk, false, "bebek", R.raw.bark_tiny, 0.98f),
        DogCompanion(90, R.string.dog_kalp_name, R.string.dog_kalp_title, R.string.food_berry, R.string.fed_berry, R.drawable.ic_dog_kalp, R.drawable.ic_food_berry, false, "kalp", R.raw.bark_tiny, 0.90f),
        DogCompanion(125, R.string.dog_somun_name, R.string.dog_somun_title, R.string.food_bread, R.string.fed_bread, R.drawable.ic_dog_somun, R.drawable.ic_food_bread, false, "somun", R.raw.bark_medium, 1.05f),
        DogCompanion(150, R.string.dog_kahkaha_name, R.string.dog_kahkaha_title, R.string.food_steak, R.string.fed_steak, R.drawable.ic_dog_kahkaha, R.drawable.ic_food_steak, false, "kahkaha", R.raw.bark_medium, 0.96f),
        DogCompanion(175, R.string.dog_goz_name, R.string.dog_goz_title, R.string.food_fish, R.string.fed_fish, R.drawable.ic_dog_goz, R.drawable.ic_food_fish, false, "goz", R.raw.bark_medium, 0.88f),
        DogCompanion(200, R.string.dog_ceket_name, R.string.dog_ceket_title, R.string.food_toast, R.string.fed_toast, R.drawable.ic_dog_ceket, R.drawable.ic_food_toast, false, "ceket", R.raw.bark_medium, 0.80f),
        DogCompanion(250, R.string.dog_kangal_name, R.string.dog_kangal_title, R.string.food_pot, R.string.fed_pot, R.drawable.ic_dog_kangal, R.drawable.ic_food_pot, false, "kangal", R.raw.bark_big, 0.92f)
    )

    fun forLevel(level: Int): DogCompanion =
        all.lastOrNull { level >= it.minLevel } ?: all.first()
}

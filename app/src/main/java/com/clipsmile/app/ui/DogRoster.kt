package com.clipsmile.app.ui

import com.clipsmile.app.R

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
    val barkRes: Int
)

object DogRoster {
    val all = listOf(
        DogCompanion(1, R.string.dog_pug_name, R.string.dog_pug_title, R.string.food_durum, R.string.fed_durum, R.drawable.ic_pug_icon, R.drawable.ic_doner, true, "pug", R.raw.bark_pug),
        DogCompanion(15, R.string.dog_pamuk_name, R.string.dog_pamuk_title, R.string.food_rice, R.string.fed_rice, R.drawable.ic_dog_pamuk, R.drawable.ic_food_rice, false, "pamuk", R.raw.bark_pamuk),
        DogCompanion(30, R.string.dog_boncuk_name, R.string.dog_boncuk_title, R.string.food_cheese, R.string.fed_cheese, R.drawable.ic_dog_boncuk, R.drawable.ic_food_cheese, false, "boncuk", R.raw.bark_boncuk),
        DogCompanion(45, R.string.dog_tombul_name, R.string.dog_tombul_title, R.string.food_simit, R.string.fed_simit, R.drawable.ic_dog_tombul, R.drawable.ic_food_simit, false, "tombul", R.raw.bark_tombul),
        DogCompanion(60, R.string.dog_bebek_name, R.string.dog_bebek_title, R.string.food_milk, R.string.fed_milk, R.drawable.ic_dog_bebek, R.drawable.ic_food_milk, false, "bebek", R.raw.bark_bebek),
        DogCompanion(90, R.string.dog_kalp_name, R.string.dog_kalp_title, R.string.food_berry, R.string.fed_berry, R.drawable.ic_dog_kalp, R.drawable.ic_food_berry, false, "kalp", R.raw.bark_kalp),
        DogCompanion(125, R.string.dog_somun_name, R.string.dog_somun_title, R.string.food_bread, R.string.fed_bread, R.drawable.ic_dog_somun, R.drawable.ic_food_bread, false, "somun", R.raw.bark_somun),
        DogCompanion(150, R.string.dog_kahkaha_name, R.string.dog_kahkaha_title, R.string.food_steak, R.string.fed_steak, R.drawable.ic_dog_kahkaha, R.drawable.ic_food_steak, false, "kahkaha", R.raw.bark_kahkaha),
        DogCompanion(175, R.string.dog_goz_name, R.string.dog_goz_title, R.string.food_fish, R.string.fed_fish, R.drawable.ic_dog_goz, R.drawable.ic_food_fish, false, "goz", R.raw.bark_goz),
        DogCompanion(200, R.string.dog_ceket_name, R.string.dog_ceket_title, R.string.food_toast, R.string.fed_toast, R.drawable.ic_dog_ceket, R.drawable.ic_food_toast, false, "ceket", R.raw.bark_ceket),
        DogCompanion(250, R.string.dog_kangal_name, R.string.dog_kangal_title, R.string.food_pot, R.string.fed_pot, R.drawable.ic_dog_kangal, R.drawable.ic_food_pot, false, "kangal", R.raw.bark_kangal)
    )

    fun forLevel(level: Int): DogCompanion =
        all.lastOrNull { level >= it.minLevel } ?: all.first()

    /** The dog currently kept at home, not merely the newest unlock. */
    fun selected(context: android.content.Context): DogCompanion {
        val level = com.clipsmile.app.data.PetProgress(context).selectedLevel()
        return all.firstOrNull { it.minLevel == level } ?: all.first()
    }

    fun homeName(context: android.content.Context): String {
        val dog = selected(context)
        return com.clipsmile.app.data.PetProgress(context)
            .displayName(dog.minLevel, context.getString(dog.nameRes))
    }
}

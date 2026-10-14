package com.clipsmile.app.ui

import com.clipsmile.app.R

/**
 * Companions unlocked as the daily level rises. The first is the real photo.
 * Later dogs are original drawings, each with its own food.
 */
data class DogCompanion(
    val minLevel: Int,
    val titleRes: Int,
    val foodNameRes: Int,
    val fedRes: Int,
    val portraitRes: Int,
    val foodRes: Int,
    val photo: Boolean
)

object DogRoster {
    val all = listOf(
        DogCompanion(
            minLevel = 1,
            titleRes = R.string.dog_pug_title,
            foodNameRes = R.string.food_durum,
            fedRes = R.string.fed_durum,
            portraitRes = R.drawable.ic_pug_icon,
            foodRes = R.drawable.ic_doner,
            photo = true
        ),
        DogCompanion(
            minLevel = 3,
            titleRes = R.string.dog_brown_title,
            foodNameRes = R.string.food_bone,
            fedRes = R.string.fed_bone,
            portraitRes = R.drawable.ic_dog_brown,
            foodRes = R.drawable.ic_food_bone,
            photo = false
        ),
        DogCompanion(
            minLevel = 6,
            titleRes = R.string.dog_spot_title,
            foodNameRes = R.string.food_meat,
            fedRes = R.string.fed_meat,
            portraitRes = R.drawable.ic_dog_spot,
            foodRes = R.drawable.ic_food_meat,
            photo = false
        ),
        DogCompanion(
            minLevel = 10,
            titleRes = R.string.dog_crown_title,
            foodNameRes = R.string.food_cookie,
            fedRes = R.string.fed_cookie,
            portraitRes = R.drawable.ic_dog_crown,
            foodRes = R.drawable.ic_food_cookie,
            photo = false
        )
    )

    fun forLevel(level: Int): DogCompanion =
        all.lastOrNull { level >= it.minLevel } ?: all.first()
}

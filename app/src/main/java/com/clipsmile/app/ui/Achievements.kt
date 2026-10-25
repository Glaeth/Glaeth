package com.clipsmile.app.ui

import android.content.Context
import android.widget.Toast
import com.clipsmile.app.R
import com.clipsmile.app.data.Den

object Achievements {
    const val DROP = "drop"
    const val FLAP = "flap"
    const val RUN = "run"
    const val SWORD = "sword"
    const val FARM = "farm"
    const val FARM_DAY = "farmday"
    const val FARM_LV = "farmlv"
    const val PET = "pet"
    const val MEAL = "meal"
    const val WASH = "wash"

    data class Badge(
        val id: String,
        val kind: String,
        val need: Int,
        val xp: Int,
        val titleRes: Int,
        val detailRes: Int
    )

    val all = listOf(
        Badge("drop_1", DROP, 1, 15, R.string.ach_drop_1_title, R.string.ach_drop_1_detail),
        Badge("drop_5", DROP, 5, 25, R.string.ach_drop_5_title, R.string.ach_drop_5_detail),
        Badge("drop_10", DROP, 10, 40, R.string.ach_drop_10_title, R.string.ach_drop_10_detail),
        Badge("drop_20", DROP, 20, 60, R.string.ach_drop_20_title, R.string.ach_drop_20_detail),
        Badge("drop_25", DROP, 25, 70, R.string.ach_drop_25_title, R.string.ach_drop_25_detail),
        Badge("drop_40", DROP, 40, 90, R.string.ach_drop_40_title, R.string.ach_drop_40_detail),
        Badge("flap_1", FLAP, 1, 15, R.string.ach_flap_1_title, R.string.ach_flap_1_detail),
        Badge("flap_3", FLAP, 3, 25, R.string.ach_flap_3_title, R.string.ach_flap_3_detail),
        Badge("flap_5", FLAP, 5, 35, R.string.ach_flap_5_title, R.string.ach_flap_5_detail),
        Badge("flap_8", FLAP, 8, 45, R.string.ach_flap_8_title, R.string.ach_flap_8_detail),
        Badge("flap_15", FLAP, 15, 70, R.string.ach_flap_15_title, R.string.ach_flap_15_detail),
        Badge("flap_30", FLAP, 30, 100, R.string.ach_flap_30_title, R.string.ach_flap_30_detail),
        Badge("run_1", RUN, 1, 15, R.string.ach_run_1_title, R.string.ach_run_1_detail),
        Badge("run_25", RUN, 25, 15, R.string.ach_run_25_title, R.string.ach_run_25_detail),
        Badge("run_80", RUN, 80, 30, R.string.ach_run_80_title, R.string.ach_run_80_detail),
        Badge("run_100", RUN, 100, 40, R.string.ach_run_100_title, R.string.ach_run_100_detail),
        Badge("run_200", RUN, 200, 50, R.string.ach_run_200_title, R.string.ach_run_200_detail),
        Badge("run_500", RUN, 500, 80, R.string.ach_run_500_title, R.string.ach_run_500_detail),
        Badge("run_1000", RUN, 1000, 120, R.string.ach_run_1000_title, R.string.ach_run_1000_detail),
        Badge("sword_1", SWORD, 1, 15, R.string.ach_sword_1_title, R.string.ach_sword_1_detail),
        Badge("sword_8", SWORD, 8, 30, R.string.ach_sword_8_title, R.string.ach_sword_8_detail),
        Badge("sword_20", SWORD, 20, 50, R.string.ach_sword_20_title, R.string.ach_sword_20_detail),
        Badge("sword_40", SWORD, 40, 80, R.string.ach_sword_40_title, R.string.ach_sword_40_detail),
        Badge("sword_80", SWORD, 80, 120, R.string.ach_sword_80_title, R.string.ach_sword_80_detail),
        Badge("farm_1", FARM, 1, 15, R.string.ach_farm_1_title, R.string.ach_farm_1_detail),
        Badge("farm_10", FARM, 10, 30, R.string.ach_farm_10_title, R.string.ach_farm_10_detail),
        Badge("farm_40", FARM, 40, 70, R.string.ach_farm_40_title, R.string.ach_farm_40_detail),
        Badge("farmday_3", FARM_DAY, 3, 20, R.string.ach_farm_day_3_title, R.string.ach_farm_day_3_detail),
        Badge("farmday_10", FARM_DAY, 10, 40, R.string.ach_farm_day_10_title, R.string.ach_farm_day_10_detail),
        Badge("farmlv_3", FARM_LV, 3, 25, R.string.ach_farm_lv_3_title, R.string.ach_farm_lv_3_detail),
        Badge("farmlv_8", FARM_LV, 8, 60, R.string.ach_farm_lv_8_title, R.string.ach_farm_lv_8_detail),
        Badge("pet_5", PET, 5, 20, R.string.ach_pet_5_title, R.string.ach_pet_5_detail),
        Badge("pet_15", PET, 15, 40, R.string.ach_pet_15_title, R.string.ach_pet_15_detail),
        Badge("pet_40", PET, 40, 70, R.string.ach_pet_40_title, R.string.ach_pet_40_detail),
        Badge("meal_5", MEAL, 5, 20, R.string.ach_meal_5_title, R.string.ach_meal_5_detail),
        Badge("meal_15", MEAL, 15, 40, R.string.ach_meal_15_title, R.string.ach_meal_15_detail),
        Badge("meal_40", MEAL, 40, 70, R.string.ach_meal_40_title, R.string.ach_meal_40_detail),
        Badge("wash_5", WASH, 5, 20, R.string.ach_wash_5_title, R.string.ach_wash_5_detail),
        Badge("wash_15", WASH, 15, 40, R.string.ach_wash_15_title, R.string.ach_wash_15_detail),
        Badge("wash_40", WASH, 40, 70, R.string.ach_wash_40_title, R.string.ach_wash_40_detail)
    )

    private const val PREFS = "clipsmile_pet"

    fun sectionTitle(kind: String): Int = when (kind) {
        DROP -> R.string.ach_section_drop
        FLAP -> R.string.ach_section_flap
        RUN -> R.string.ach_section_run
        SWORD -> R.string.ach_section_sword
        FARM, FARM_DAY, FARM_LV -> R.string.ach_section_farm
        PET, MEAL, WASH -> R.string.ach_section_care
        else -> R.string.ach_section_run
    }

    fun isUnlocked(context: Context, id: String): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(key(id), false)
    }

    fun unlockedCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return all.count { prefs.getBoolean(key(it.id), false) }
    }

    fun celebrate(context: Context, kind: String, score: Int) {
        val title = grant(context, kind, score) ?: return
        Toast.makeText(
            context,
            context.getString(R.string.achievement_unlocked_toast, context.getString(title.titleRes), title.xp),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun grant(context: Context, kind: String, score: Int): Badge? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        var best: Badge? = null
        var xp = 0
        for (badge in all) {
            if (badge.kind != kind || score < badge.need) continue
            if (prefs.getBoolean(key(badge.id), false)) continue
            editor.putBoolean(key(badge.id), true)
            xp += badge.xp
            if (best == null || badge.need > best.need) best = badge
        }
        if (best == null) return null
        editor.apply()
        Den(context).grantXp(xp, context.getString(best.titleRes))
        return best.copy(xp = xp)
    }

    private fun key(id: String) = "ach_$id"
}

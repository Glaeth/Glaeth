package com.clipsmile.app.data

import android.content.Context
import java.util.Calendar

/**
 * Per-animal den: the chosen pet, gold, premium gems, login streak, and the
 * daily/weekly tasks that are the only way to earn XP.
 * A fresh install (and anyone still on the old level save) starts at the
 * animal pick, because this uses its own preferences file.
 */
class Den(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    enum class Kind { FEED, BATH, NUZZLE, DROP, FLAP, RUN, SWORD }

    data class Task(
        val id: String,
        val animalId: String,
        val weekly: Boolean,
        val kind: Kind,
        val goal: Int,
        val xp: Int,
        val gold: Int,
        val title: String
    )

    data class TaskView(val task: Task, val progress: Int, val done: Boolean)

    fun onboarded(): Boolean = prefs.getBoolean(KEY_ON, false)

    fun tutorialDone(): Boolean = prefs.getBoolean(KEY_TUT, false)

    fun markTutorial() {
        prefs.edit().putBoolean(KEY_TUT, true).apply()
    }

    fun activeId(): String = prefs.getString(KEY_ACTIVE, "pug") ?: "pug"

    fun owns(id: String): Boolean = id in owned()

    fun owned(): Set<String> = (prefs.getString(KEY_OWNED, "") ?: "")
        .split(',')
        .filter { it.isNotBlank() }
        .toSet()

    fun gold(): Int = prefs.getInt(KEY_GOLD, 0)

    fun gems(): Int = prefs.getInt(KEY_GEMS, 0)

    fun streak(): Int = prefs.getInt(KEY_STREAK, 0)

    fun visitedToday(): Boolean = prefs.getString(KEY_DAY, null) == dayKey(0)

    fun xp(id: String): Int = prefs.getInt(xpKey(id), 0)

    fun displayName(id: String, fallback: String): String =
        prefs.getString(nameKey(id), null)?.takeIf { it.isNotBlank() } ?: fallback

    fun setName(id: String, value: String) {
        val cleaned = value.trim().take(24)
        if (cleaned.isEmpty()) return
        prefs.edit().putString(nameKey(id), cleaned).apply()
    }

    fun level(id: String): Int = split(xp(id)).first

    fun into(id: String): Int = split(xp(id)).second

    fun need(id: String): Int = split(xp(id)).third

    fun begin(id: String, name: String) {
        val cleaned = name.trim().take(24)
        if (cleaned.isEmpty()) return
        prefs.edit()
            .putBoolean(KEY_ON, true)
            .putString(KEY_ACTIVE, id)
            .putString(KEY_OWNED, id)
            .putInt(KEY_GOLD, START_GOLD)
            .putInt(KEY_GEMS, 0)
            .putString(nameKey(id), cleaned)
            .putInt(xpKey(id), 0)
            .apply()
    }

    /** Login streak plus a little gold. Opening the app does not raise a level. */
    fun recordVisit(): Int {
        if (!onboarded()) return 0
        val today = dayKey(0)
        if (prefs.getString(KEY_DAY, null) == today) return 0
        val last = prefs.getString(KEY_DAY, null)
        var streak = prefs.getInt(KEY_STREAK, 0)
        streak = if (last == dayKey(-1) && streak > 0) streak + 1 else 1
        val gain = 8 + minOf(streak, 10) * 2
        val editor = prefs.edit()
            .putString(KEY_DAY, today)
            .putInt(KEY_STREAK, streak)
            .putInt(KEY_GOLD, gold() + gain)
        if (prefs.getString(KEY_NOTE, null).isNullOrBlank()) {
            editor.putString(KEY_NOTE, "Giriş serisi $streak gün  +$gain altın")
        }
        editor.apply()
        return gain
    }

    fun setActive(id: String) {
        if (owns(id)) prefs.edit().putString(KEY_ACTIVE, id).apply()
    }

    /** @return ok, owned, gold or gems */
    fun buy(id: String, goldPrice: Int, gemPrice: Int, fallbackName: String): String {
        if (!onboarded() || owns(id)) return if (owns(id)) "owned" else "gold"
        val editor = prefs.edit()
        if (gemPrice > 0) {
            if (gems() < gemPrice) return "gems"
            editor.putInt(KEY_GEMS, gems() - gemPrice)
        } else {
            if (gold() < goldPrice) return "gold"
            editor.putInt(KEY_GOLD, gold() - goldPrice)
        }
        editor.putString(KEY_OWNED, (owned() + id).joinToString(","))
        if (prefs.getString(nameKey(id), null).isNullOrBlank()) {
            editor.putString(nameKey(id), fallbackName)
        }
        editor.apply()
        return "ok"
    }

    fun bump(kind: Kind, amount: Int) {
        if (!onboarded() || amount <= 0) return
        val id = activeId()
        var nextXp = xp(id)
        var nextGold = gold()
        val editor = prefs.edit()
        var changed = false
        var last = ""
        for (task in tasksFor(id)) {
            if (task.kind != kind) continue
            val key = progressKey(task)
            if (prefs.getBoolean(doneKey(key), false)) continue
            val next = (prefs.getInt(key, 0) + amount).coerceAtMost(task.goal)
            editor.putInt(key, next)
            changed = true
            if (next >= task.goal) {
                editor.putBoolean(doneKey(key), true)
                nextXp += task.xp
                nextGold += task.gold
                last = "${task.title}  +${task.xp} XP"
            }
        }
        if (!changed) return
        editor.putInt(xpKey(id), nextXp)
        editor.putInt(KEY_GOLD, nextGold)
        if (last.isNotEmpty()) editor.putString(KEY_NOTE, last)
        editor.apply()
    }

    /** Score rewards from a mini-game. XP and gold are separate. */
    fun grantPlay(xpAmount: Int, goldAmount: Int, title: String) {
        if (!onboarded() || (xpAmount <= 0 && goldAmount <= 0)) return
        val id = activeId()
        val bits = ArrayList<String>(2)
        if (xpAmount > 0) bits.add("+$xpAmount XP")
        if (goldAmount > 0) bits.add("+$goldAmount altın")
        val line = "$title  " + bits.joinToString("  ")
        val previous = prefs.getString(KEY_NOTE, null)
        val note = if (previous.isNullOrBlank()) line else "$previous · $line"
        prefs.edit()
            .putInt(xpKey(id), xp(id) + xpAmount.coerceAtLeast(0))
            .putInt(KEY_GOLD, gold() + goldAmount.coerceAtLeast(0))
            .putString(KEY_NOTE, note)
            .apply()
    }

    /** Bonus XP from a badge. A little gold comes with it, and the home screen reads the note. */
    fun grantXp(amount: Int, title: String) {
        if (!onboarded() || amount <= 0) return
        val id = activeId()
        val line = "$title  +$amount XP"
        val previous = prefs.getString(KEY_NOTE, null)
        val note = if (previous.isNullOrBlank()) line else "$previous · $line"
        prefs.edit()
            .putInt(xpKey(id), xp(id) + amount)
            .putInt(KEY_GOLD, gold() + amount / 5)
            .putString(KEY_NOTE, note)
            .apply()
    }

    fun takeNote(): String? {
        val value = prefs.getString(KEY_NOTE, null)?.takeIf { it.isNotBlank() }
        if (value != null) prefs.edit().remove(KEY_NOTE).apply()
        return value
    }

    fun taskViews(id: String): List<TaskView> = tasksFor(id).map { task ->
        val key = progressKey(task)
        TaskView(task, prefs.getInt(key, 0), prefs.getBoolean(doneKey(key), false))
    }

    private fun split(total: Int): Triple<Int, Int, Int> {
        var left = total
        var level = 1
        while (level < 99) {
            val cost = xpNeed(level)
            if (left < cost) return Triple(level, left, cost)
            left -= cost
            level++
        }
        return Triple(level, left, xpNeed(level))
    }

    private fun progressKey(task: Task): String {
        val period = if (task.weekly) weekKey() else dayKey(0)
        return period + "_" + task.id
    }

    private fun xpKey(id: String) = "xp_$id"

    private fun nameKey(id: String) = "name_$id"

    private fun doneKey(key: String) = "done_$key"

    private fun dayKey(offset: Int): String {
        val calendar = Calendar.getInstance()
        if (offset != 0) calendar.add(Calendar.DAY_OF_YEAR, offset)
        return "%04d-%02d-%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun weekKey(): String {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.minimalDaysInFirstWeek = 4
        return "%04d-W%02d".format(calendar.get(Calendar.YEAR), calendar.get(Calendar.WEEK_OF_YEAR))
    }

    companion object {
        const val START_GOLD = 40
        private const val PREFS = "clipsmile_den"
        private const val KEY_ON = "onboarded"
        private const val KEY_TUT = "tutorial"
        private const val KEY_ACTIVE = "active"
        private const val KEY_OWNED = "owned"
        private const val KEY_GOLD = "gold"
        private const val KEY_GEMS = "gems"
        private const val KEY_STREAK = "streak"
        private const val KEY_DAY = "last_day"
        private const val KEY_NOTE = "note"

        fun xpNeed(level: Int): Int = 50 + 40 * level

        fun tasksFor(id: String): List<Task> = tasks.filter { it.animalId == id }

        private fun t(
            animal: String,
            slug: String,
            weekly: Boolean,
            kind: Kind,
            goal: Int,
            xp: Int,
            gold: Int,
            title: String
        ) = Task("${animal}_$slug", animal, weekly, kind, goal, xp, gold, title)

        val tasks: List<Task> = listOf(
            t("pug", "d1", false, Kind.NUZZLE, 3, 24, 12, "Pati uzat"),
            t("pug", "d2", false, Kind.FEED, 2, 28, 14, "Döner paylaş"),
            t("pug", "d3", false, Kind.BATH, 1, 22, 10, "Yere yuvarlan"),
            t("pug", "w1", true, Kind.DROP, 15, 100, 40, "Takla skoru"),
            t("pug", "w2", true, Kind.NUZZLE, 12, 80, 32, "Pati antrenmanı"),
            t("cat", "d1", false, Kind.NUZZLE, 3, 26, 12, "Kulak arkası"),
            t("cat", "d2", false, Kind.FEED, 2, 28, 14, "Balık kasesi"),
            t("cat", "d3", false, Kind.FLAP, 4, 24, 12, "Raf atlayışı"),
            t("cat", "w1", true, Kind.BATH, 6, 90, 36, "Tüy bakımı"),
            t("cat", "w2", true, Kind.NUZZLE, 16, 100, 40, "Kucak saati"),
            t("capy", "d1", false, Kind.BATH, 2, 26, 12, "Su birikintisi"),
            t("capy", "d2", false, Kind.FEED, 3, 30, 14, "Ot öğünü"),
            t("capy", "d3", false, Kind.RUN, 80, 24, 12, "Nehir kenarı"),
            t("capy", "w1", true, Kind.BATH, 8, 100, 40, "Uzun yüzüş"),
            t("capy", "w2", true, Kind.FEED, 10, 90, 36, "Ot stoğu"),
            t("pamuk", "d1", false, Kind.FEED, 2, 24, 12, "Pirinç lokması"),
            t("pamuk", "d2", false, Kind.NUZZLE, 3, 22, 10, "Yumuşak pati"),
            t("pamuk", "d3", false, Kind.BATH, 1, 26, 12, "Köpük banyosu"),
            t("pamuk", "w1", true, Kind.DROP, 12, 90, 36, "Yumuşak yakalama"),
            t("pamuk", "w2", true, Kind.NUZZLE, 14, 80, 32, "Kucak haftası"),
            t("boncuk", "d1", false, Kind.NUZZLE, 3, 24, 12, "Boncuk göz"),
            t("boncuk", "d2", false, Kind.FEED, 2, 28, 14, "Peynir kırıntısı"),
            t("boncuk", "d3", false, Kind.FLAP, 5, 26, 12, "Zıpla say"),
            t("boncuk", "w1", true, Kind.FLAP, 16, 100, 40, "Boncuk parkuru"),
            t("boncuk", "w2", true, Kind.BATH, 7, 80, 32, "Parlak tüy"),
            t("tombul", "d1", false, Kind.FEED, 3, 30, 14, "Simit turu"),
            t("tombul", "d2", false, Kind.BATH, 2, 24, 12, "Göbek yıkama"),
            t("tombul", "d3", false, Kind.DROP, 6, 22, 10, "Düşen simit"),
            t("tombul", "w1", true, Kind.FEED, 12, 100, 40, "Haftalık ziyafet"),
            t("tombul", "w2", true, Kind.DROP, 20, 90, 36, "Simit şampiyonu"),
            t("bebek", "d1", false, Kind.FEED, 2, 24, 12, "Süt saati"),
            t("bebek", "d2", false, Kind.NUZZLE, 2, 22, 10, "Ninni"),
            t("bebek", "d3", false, Kind.BATH, 1, 26, 12, "Ilık su"),
            t("bebek", "w1", true, Kind.NUZZLE, 10, 80, 32, "Kucakta büyü"),
            t("bebek", "w2", true, Kind.BATH, 6, 90, 36, "Temiz bebek"),
            t("kalp", "d1", false, Kind.NUZZLE, 3, 26, 12, "Kalp patisi"),
            t("kalp", "d2", false, Kind.FEED, 2, 24, 12, "Meyve dilimi"),
            t("kalp", "d3", false, Kind.RUN, 60, 28, 14, "Sevgi koşusu"),
            t("kalp", "w1", true, Kind.NUZZLE, 18, 100, 40, "Haftalık sevgi"),
            t("kalp", "w2", true, Kind.RUN, 300, 90, 36, "Uzun yürüyüş"),
            t("somun", "d1", false, Kind.FEED, 2, 26, 12, "Ekmek kabuğu"),
            t("somun", "d2", false, Kind.DROP, 8, 28, 14, "Fırın oyunu"),
            t("somun", "d3", false, Kind.BATH, 1, 22, 10, "Un tozu"),
            t("somun", "w1", true, Kind.FEED, 10, 90, 36, "Fırın haftası"),
            t("somun", "w2", true, Kind.DROP, 24, 100, 40, "Usta fırıncı"),
            t("kahkaha", "d1", false, Kind.NUZZLE, 3, 24, 12, "Kahkaha"),
            t("kahkaha", "d2", false, Kind.FLAP, 4, 26, 12, "Zıp zıp"),
            t("kahkaha", "d3", false, Kind.FEED, 2, 28, 14, "Biftek şakası"),
            t("kahkaha", "w1", true, Kind.FLAP, 14, 100, 40, "Komik parkur"),
            t("kahkaha", "w2", true, Kind.NUZZLE, 15, 80, 32, "Gülme krizi"),
            t("goz", "d1", false, Kind.NUZZLE, 3, 26, 12, "Göz kırp"),
            t("goz", "d2", false, Kind.RUN, 100, 28, 14, "Sessiz takip"),
            t("goz", "d3", false, Kind.BATH, 2, 24, 12, "Berrak su"),
            t("goz", "w1", true, Kind.RUN, 400, 110, 44, "Nöbet haftası"),
            t("goz", "w2", true, Kind.FLAP, 12, 90, 36, "Keskin bakış"),
            t("ceket", "d1", false, Kind.BATH, 1, 24, 12, "Ceket silk"),
            t("ceket", "d2", false, Kind.FEED, 2, 26, 12, "Tost molası"),
            t("ceket", "d3", false, Kind.DROP, 8, 28, 14, "Cep oyunu"),
            t("ceket", "w1", true, Kind.DROP, 22, 100, 40, "Şık skor"),
            t("ceket", "w2", true, Kind.BATH, 8, 90, 36, "Ütü gibi"),
            t("kangal", "d1", false, Kind.RUN, 120, 30, 16, "Sürü devriyesi"),
            t("kangal", "d2", false, Kind.FEED, 3, 28, 14, "Çanak yemeği"),
            t("kangal", "d3", false, Kind.NUZZLE, 3, 24, 12, "Sadık pati"),
            t("kangal", "w1", true, Kind.RUN, 500, 120, 48, "Haftalık nöbet"),
            t("kangal", "w2", true, Kind.FEED, 12, 100, 40, "Sürü ziyafeti"),
            t("pug", "d4", false, Kind.SWORD, 4, 28, 14, "Kapı nöbeti"),
            t("pug", "w3", true, Kind.RUN, 200, 90, 36, "Mahalle turu"),
            t("cat", "d4", false, Kind.DROP, 5, 26, 12, "Düşen balık"),
            t("cat", "w3", true, Kind.SWORD, 10, 100, 40, "Çatı nöbeti"),
            t("capy", "d4", false, Kind.NUZZLE, 3, 24, 12, "Yanaktan okşa"),
            t("capy", "d5", false, Kind.SWORD, 4, 26, 12, "Su nöbeti"),
            t("capy", "w3", true, Kind.DROP, 18, 90, 36, "Düşen ot"),
            t("pamuk", "d4", false, Kind.RUN, 60, 24, 12, "Yumuşak tur"),
            t("pamuk", "w3", true, Kind.FLAP, 10, 90, 36, "Pamuk zıplaması"),
            t("boncuk", "d4", false, Kind.BATH, 1, 22, 10, "Parlak burun"),
            t("boncuk", "w3", true, Kind.DROP, 16, 90, 36, "Boncuk yağmuru"),
            t("tombul", "d4", false, Kind.NUZZLE, 3, 24, 12, "Göbek okşa"),
            t("tombul", "d5", false, Kind.RUN, 70, 26, 12, "Ağır adım"),
            t("tombul", "w3", true, Kind.FLAP, 12, 90, 36, "Zıplayan simit"),
            t("bebek", "d4", false, Kind.DROP, 5, 24, 12, "Yumuşak top"),
            t("bebek", "w3", true, Kind.RUN, 150, 90, 36, "Park turu"),
            t("kalp", "d4", false, Kind.BATH, 1, 22, 10, "Ilık pati"),
            t("kalp", "w3", true, Kind.DROP, 18, 90, 36, "Meyve yağmuru"),
            t("somun", "d4", false, Kind.NUZZLE, 3, 24, 12, "Unlu pati"),
            t("somun", "d5", false, Kind.FLAP, 3, 26, 12, "Fırın zıplayışı"),
            t("somun", "w3", true, Kind.RUN, 220, 90, 36, "Fırın yolu"),
            t("kahkaha", "d4", false, Kind.RUN, 70, 26, 12, "Kahkaha koşusu"),
            t("kahkaha", "w3", true, Kind.SWORD, 12, 100, 40, "Şaka kalkanı"),
            t("goz", "d4", false, Kind.DROP, 6, 26, 12, "Sessiz yakalama"),
            t("goz", "w3", true, Kind.SWORD, 12, 100, 40, "Gece nöbeti"),
            t("ceket", "d4", false, Kind.NUZZLE, 3, 24, 12, "Yaka düzelt"),
            t("ceket", "d5", false, Kind.RUN, 80, 26, 12, "Kaldırım"),
            t("ceket", "w3", true, Kind.FLAP, 12, 90, 36, "Şık zıplama"),
            t("kangal", "d4", false, Kind.BATH, 1, 22, 10, "Çamur silk"),
            t("kangal", "w3", true, Kind.SWORD, 15, 110, 44, "Sürü kalkanı")
        )
    }
}

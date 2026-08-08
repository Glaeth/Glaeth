package com.glaeth.tracker

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GlaethRoot() }
    }
}

// region Domain models

private enum class Section(val label: String, val icon: ImageVector, val short: String) {
    Dashboard("Ana Sayfa", Icons.Filled.Home, "Ana Sayfa"),
    Sleep("Uyku", Icons.Filled.DateRange, "Uyku"),
    Meals("Öğünler", Icons.Filled.LocalDining, "Öğün"),
    Water("Su", Icons.Filled.WaterDrop, "Su"),
    Skin("Cilt", Icons.Filled.Face, "Cilt"),
    Forest("Orman", Icons.Filled.Park, "Orman"),
    Homework("Ödev", Icons.Filled.School, "Ödev"),
    Budget("Bütçe", Icons.Filled.AttachMoney, "Bütçe"),
    History("Geçmiş", Icons.Filled.History, "Geçmiş"),
    Settings("Ayarlar", Icons.Filled.Settings, "Ayar"),
}

private enum class MealType(val label: String) {
    Morning("Sabah"),
    Lunch("Öğle"),
    Evening("Akşam"),
    Snack("Ara Öğün"),
}

private enum class Priority(val label: String, val color: Color) {
    Urgent("Acil", Color(0xFFEF4444)),
    Important("Önemli", Color(0xFFF59E0B)),
    Chill("Keyfi", Color(0xFF22C55E)),
}

private enum class ThemeMode(val label: String) {
    System("Sistem"), Light("Açık"), Dark("Koyu")
}

private enum class Palette(
    val label: String,
    val darkPrimary: Color,
    val darkSecondary: Color,
    val darkBackground: Color,
    val darkSurface: Color,
    val lightPrimary: Color,
    val lightSecondary: Color,
    val lightBackground: Color,
    val lightSurface: Color,
    val accentSoft: Color,
) {
    Obsidian("Obsidyen",
        darkPrimary = Color(0xFFFF8A3D), darkSecondary = Color(0xFF94A3B8),
        darkBackground = Color(0xFF06070A), darkSurface = Color(0xFF101218),
        lightPrimary = Color(0xFFD96A1B), lightSecondary = Color(0xFF1F2937),
        lightBackground = Color(0xFFF8FAFC), lightSurface = Color(0xFFFFFFFF),
        accentSoft = Color(0xFFFFB58A),
    ),
    Aurora("Aurora",
        darkPrimary = Color(0xFF7DD3FC), darkSecondary = Color(0xFF38BDF8),
        darkBackground = Color(0xFF050A18), darkSurface = Color(0xFF101A2C),
        lightPrimary = Color(0xFF0284C7), lightSecondary = Color(0xFF0F172A),
        lightBackground = Color(0xFFF1F5F9), lightSurface = Color(0xFFFFFFFF),
        accentSoft = Color(0xFF93C5FD),
    ),
    Mocha("Mocha",
        darkPrimary = Color(0xFFE8C39E), darkSecondary = Color(0xFFC0A98F),
        darkBackground = Color(0xFF120E0B), darkSurface = Color(0xFF1F1714),
        lightPrimary = Color(0xFF7C5836), lightSecondary = Color(0xFF3F2E1E),
        lightBackground = Color(0xFFFBF6F0), lightSurface = Color(0xFFFFFFFF),
        accentSoft = Color(0xFFD7BFA1),
    ),
    Sakura("Sakura",
        darkPrimary = Color(0xFFF472B6), darkSecondary = Color(0xFFC084FC),
        darkBackground = Color(0xFF0E0712), darkSurface = Color(0xFF1B1126),
        lightPrimary = Color(0xFFDB2777), lightSecondary = Color(0xFF7E22CE),
        lightBackground = Color(0xFFFFF1F8), lightSurface = Color(0xFFFFFFFF),
        accentSoft = Color(0xFFF9A8D4),
    ),
    Forest("Orman",
        darkPrimary = Color(0xFF4ADE80), darkSecondary = Color(0xFF34D399),
        darkBackground = Color(0xFF06120C), darkSurface = Color(0xFF0F1E15),
        lightPrimary = Color(0xFF15803D), lightSecondary = Color(0xFF065F46),
        lightBackground = Color(0xFFF0FDF4), lightSurface = Color(0xFFFFFFFF),
        accentSoft = Color(0xFF86EFAC),
    ),
}

private data class Profile(
    val name: String = "Kardeşim",
    val age: Int = 16,
    val gender: String = "Belirtilmedi",
    val photoUri: String = "",
)

private data class SleepEntry(val id: String, val date: String, val sleptAt: String, val wokeAt: String)
private data class MealEntry(val id: String, val date: String, val type: MealType, val foods: List<String>)
private data class SkinEntry(val id: String, val date: String, val photoUri: String, val products: String, val notes: String, val zones: Set<String>)
private data class HomeworkEntry(val id: String, val lesson: String, val title: String, val dueDate: String, val priority: Priority, val attachment: String)
private data class WaterEntry(val id: String, val date: String, val time: String, val amountMl: Int)

private enum class AccountType(val label: String, val icon: ImageVector) {
    Bank("Banka Hesabı", Icons.Filled.AccountBalance),
    Card("Kredi Kartı", Icons.Filled.CreditCard),
    Cash("Nakit", Icons.Filled.AttachMoney),
}

private data class BudgetPerson(val id: String, val name: String, val photoUri: String = "")

private data class BudgetAccount(
    val id: String,
    val personId: String,
    val name: String,
    val type: AccountType,
    val iconKey: String,
    val balance: Double = 0.0,
    val creditLimit: Double = 0.0,
    val dueAmount: Double = 0.0,
)

private enum class TxnCategory(val label: String, val isIncome: Boolean, val icon: ImageVector, val color: Color) {
    Salary("Maaş", true, Icons.Filled.AttachMoney, Color(0xFF22C55E)),
    Other("Diğer Gelir", true, Icons.Filled.Receipt, Color(0xFF34D399)),
    Rent("Kira", false, Icons.Filled.Home, Color(0xFFEF4444)),
    Electricity("Elektrik", false, Icons.Filled.Bolt, Color(0xFFFACC15)),
    Water("Su Faturası", false, Icons.Filled.WaterDrop, Color(0xFF38BDF8)),
    Gas("Doğalgaz", false, Icons.Filled.Bolt, Color(0xFFF97316)),
    Food("Market", false, Icons.Filled.LocalDining, Color(0xFFF472B6)),
    Subscription("Abonelik", false, Icons.Filled.MenuBook, Color(0xFF8B5CF6)),
    Misc("Diğer Gider", false, Icons.Filled.Receipt, Color(0xFF64748B)),
}

private enum class Currency(val label: String, val symbol: String) {
    TRY("Türk Lirası", "₺"),
    USD("Amerikan Doları", "$"),
    EUR("Euro", "€"),
    GBP("İngiliz Sterlini", "£"),
    JPY("Japon Yeni", "¥"),
    CAD("Kanada Doları", "C$"),
    AUD("Avustralya Doları", "A$"),
    CHF("İsviçre Frangı", "Fr"),
    CNY("Çin Yuanı", "¥"),
    INR("Hindistan Rupisi", "₹"),
    RUB("Rus Rublesi", "₽"),
    KRW("Kore Wonu", "₩"),
    MXN("Meksika Pesosu", "MX$"),
    BRL("Brezilya Reali", "R$"),
    SAR("Suudi Riyali", "﷼"),
    AED("BAE Dirhemi", "AED"),
    SEK("İsveç Kronu", "kr"),
    NOK("Norveç Kronu", "kr"),
    DKK("Danimarka Kronu", "kr"),
    NZD("Yeni Zelanda Doları", "NZ$"),
}

private enum class StoreCategory(val label: String) {
    Freeze("Seri Dondurma"),
    Pet("Evcil Hayvan"),
    Tree("Ağaç Tohumu"),
    MealSkin("Yemek Kâsesi"),
    WaterSkin("Bardak / Şişe"),
    Background("Arka Plan"),
}

private data class StoreItem(
    val key: String,
    val label: String,
    val cost: Int,
    val category: StoreCategory,
    val emoji: String,
    val isDefault: Boolean = false,
)

private val DefaultStoreCatalog: List<StoreItem> = listOf(
    StoreItem("freeze_pass", "Seri Dondurma (Buz)", 150, StoreCategory.Freeze, "🧊"),
    StoreItem("pet_dog", "Sadık Köpek", 0, StoreCategory.Pet, "🐶", isDefault = true),
    StoreItem("pet_cat", "Tatlı Kedi", 600, StoreCategory.Pet, "🐱"),
    StoreItem("pet_panda", "Nadir Panda", 1500, StoreCategory.Pet, "🐼"),
    StoreItem("pet_dragon", "Efsane Ejderha", 1500, StoreCategory.Pet, "🐉"),
    StoreItem("pet_bunny", "Sevimli Tavşan", 800, StoreCategory.Pet, "🐰"),
    StoreItem("tree_oak", "Meşe Tohumu", 500, StoreCategory.Tree, "🌳", isDefault = true),
    StoreItem("tree_sakura", "Sakura Tohumu", 750, StoreCategory.Tree, "🌸"),
    StoreItem("tree_sedir", "Sedir Tohumu", 900, StoreCategory.Tree, "🌲"),
    StoreItem("tree_palm", "Palmiye Tohumu", 700, StoreCategory.Tree, "🌴"),
    StoreItem("tree_glow", "Glowshroom", 1200, StoreCategory.Tree, "🍄"),
    StoreItem("bowl_classic", "Klasik Kâse", 0, StoreCategory.MealSkin, "🍚", isDefault = true),
    StoreItem("bowl_noodle", "Noodle & Chopstick", 750, StoreCategory.MealSkin, "🍜"),
    StoreItem("bowl_salad", "Sağlıklı Salata", 300, StoreCategory.MealSkin, "🥗"),
    StoreItem("bowl_ramen", "Premium Ramen", 750, StoreCategory.MealSkin, "🍲"),
    StoreItem("glass_basic", "Klasik Bardak", 0, StoreCategory.WaterSkin, "🥛", isDefault = true),
    StoreItem("glass_bottle", "Spor Matarası", 300, StoreCategory.WaterSkin, "🧃"),
    StoreItem("glass_crystal", "Kristal Kadeh", 750, StoreCategory.WaterSkin, "🍷"),
    StoreItem("glass_mug", "Sıcak Kupa", 300, StoreCategory.WaterSkin, "☕"),
    StoreItem("bg_aurora", "Aurora", 0, StoreCategory.Background, "🌅", isDefault = true),
    StoreItem("bg_ocean", "Okyanus", 300, StoreCategory.Background, "🌊"),
    StoreItem("bg_forest", "Orman", 300, StoreCategory.Background, "🌲"),
    StoreItem("bg_galaxy", "Galaksi", 750, StoreCategory.Background, "🌌"),
    StoreItem("bg_desert", "Çöl Günbatımı", 500, StoreCategory.Background, "🏜️"),
)

private data class TreeEntry(
    val id: String,
    val date: String,
    val durationMinutes: Int,
    val treeKey: String,
)

private enum class ChartKind(val label: String) { Bar("Bar"), Line("Line"), Candle("Candle") }

private data class TxnEntry(
    val id: String,
    val personId: String,
    val accountId: String,
    val category: TxnCategory,
    val amount: Double,
    val currency: Currency,
    val description: String,
    val date: String,
)

private data class AppData(
    val sleepEntries: List<SleepEntry>,
    val mealEntries: List<MealEntry>,
    val skinEntries: List<SkinEntry>,
    val homeworkEntries: List<HomeworkEntry>,
    val waterEntries: List<WaterEntry>,
    val treeEntries: List<TreeEntry>,
    val people: List<BudgetPerson>,
    val accounts: List<BudgetAccount>,
    val transactions: List<TxnEntry>,
    val waterTargetMl: Int,
    val budgetLimit: Double,
    val currency: Currency,
    val profile: Profile,
    val palette: Palette,
    val themeMode: ThemeMode,
    val currentPoints: Int,
    val freezePassCount: Int,
    val purchasedItems: Set<String>,
    val selectedPet: String,
    val selectedTree: String,
    val selectedBowl: String,
    val selectedGlass: String,
    val selectedBackground: String,
)

// endregion

// region Persistence

private class GlaethDatabase(context: Context) : SQLiteOpenHelper(context.applicationContext, "glaeth.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE app_state (state_key TEXT PRIMARY KEY, payload TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS app_state")
        onCreate(db)
    }
}

private class AppRepository(context: Context) {
    private val database = GlaethDatabase(context)

    fun load(): AppData = readableLoad() ?: demoData()
    private fun readableLoad(): AppData? {
        return database.readableDatabase.query("app_state", arrayOf("payload"), "state_key = ?", arrayOf("main"), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) runCatching { decode(JSONObject(cursor.getString(0))) }.getOrNull() else null
        }
    }
    fun save(data: AppData) {
        val values = ContentValues().apply {
            put("state_key", "main")
            put("payload", encode(data).toString())
        }
        database.writableDatabase.insertWithOnConflict("app_state", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun decode(root: JSONObject): AppData = AppData(
        sleepEntries = root.optJSONArray("sleep").mapJsonObjects {
            SleepEntry(it.optString("id", newId()), it.optString("date"), it.optString("sleptAt"), it.optString("wokeAt"))
        },
        mealEntries = root.optJSONArray("meals").mapJsonObjects {
            MealEntry(it.optString("id", newId()), it.optString("date"), enumValueOfOrDefault(it.optString("type"), MealType.Morning), it.optJSONArray("foods").mapStrings())
        },
        skinEntries = root.optJSONArray("skin").mapJsonObjects {
            SkinEntry(it.optString("id", newId()), it.optString("date"), it.optString("photoUri"), it.optString("products"), it.optString("notes"), it.optJSONArray("zones").mapStrings().toSet())
        },
        homeworkEntries = root.optJSONArray("homework").mapJsonObjects {
            HomeworkEntry(it.optString("id", newId()), it.optString("lesson"), it.optString("title"), it.optString("dueDate"), enumValueOfOrDefault(it.optString("priority"), Priority.Important), it.optString("attachment"))
        },
        waterEntries = root.optJSONArray("water").mapJsonObjects {
            WaterEntry(it.optString("id", newId()), it.optString("date"), it.optString("time"), it.optInt("amountMl", 250))
        },
        people = root.optJSONArray("people").mapJsonObjects {
            BudgetPerson(it.optString("id", newId()), it.optString("name", "Kişi"), it.optString("photoUri"))
        }.ifEmpty { listOf(BudgetPerson(newId(), "Ben")) },
        accounts = root.optJSONArray("accounts").mapJsonObjects {
            BudgetAccount(
                id = it.optString("id", newId()),
                personId = it.optString("personId"),
                name = it.optString("name", "Hesap"),
                type = enumValueOfOrDefault(it.optString("type"), AccountType.Bank),
                iconKey = it.optString("iconKey"),
                balance = it.optDouble("balance", 0.0),
                creditLimit = it.optDouble("creditLimit", 0.0),
                dueAmount = it.optDouble("dueAmount", 0.0),
            )
        },
        transactions = root.optJSONArray("transactions").mapJsonObjects {
            TxnEntry(
                it.optString("id", newId()),
                it.optString("personId"),
                it.optString("accountId"),
                enumValueOfOrDefault(it.optString("category"), TxnCategory.Misc),
                it.optDouble("amount", 0.0),
                enumValueOfOrDefault(it.optString("currency"), Currency.TRY),
                it.optString("description"),
                it.optString("date"),
            )
        },
        treeEntries = root.optJSONArray("trees").mapJsonObjects {
            TreeEntry(it.optString("id", newId()), it.optString("date"), it.optInt("durationMinutes", 25), it.optString("treeKey", "tree_oak"))
        },
        waterTargetMl = root.optInt("waterTargetMl", 2500),
        budgetLimit = root.optDouble("budgetLimit", 0.0),
        currency = enumValueOfOrDefault(root.optString("currency"), Currency.TRY),
        profile = root.optJSONObject("profile")?.let {
            Profile(it.optString("name", "Kardeşim"), it.optInt("age", 16), it.optString("gender", "Belirtilmedi"), it.optString("photoUri"))
        } ?: Profile(),
        palette = enumValueOfOrDefault(root.optString("palette"), Palette.Obsidian),
        themeMode = enumValueOfOrDefault(root.optString("themeMode"), ThemeMode.System),
        currentPoints = root.optInt("currentPoints", 0),
        freezePassCount = root.optInt("freezePassCount", 1),
        purchasedItems = root.optJSONArray("purchasedItems").mapStrings().toSet() + DefaultStoreCatalog.filter { it.isDefault }.map { it.key },
        selectedPet = root.optString("selectedPet", "pet_dog").ifBlank { "pet_dog" },
        selectedTree = root.optString("selectedTree", "tree_oak").ifBlank { "tree_oak" },
        selectedBowl = root.optString("selectedBowl", "bowl_classic").ifBlank { "bowl_classic" },
        selectedGlass = root.optString("selectedGlass", "glass_basic").ifBlank { "glass_basic" },
        selectedBackground = root.optString("selectedBackground", "bg_aurora").ifBlank { "bg_aurora" },
    )

    fun encode(data: AppData): JSONObject = JSONObject()
        .put("waterTargetMl", data.waterTargetMl)
        .put("budgetLimit", data.budgetLimit)
        .put("currency", data.currency.name)
        .put("palette", data.palette.name)
        .put("themeMode", data.themeMode.name)
        .put("profile", JSONObject().put("name", data.profile.name).put("age", data.profile.age).put("gender", data.profile.gender).put("photoUri", data.profile.photoUri))
        .put("sleep", JSONArray().apply { data.sleepEntries.forEach { put(JSONObject().put("id", it.id).put("date", it.date).put("sleptAt", it.sleptAt).put("wokeAt", it.wokeAt)) } })
        .put("meals", JSONArray().apply { data.mealEntries.forEach { put(JSONObject().put("id", it.id).put("date", it.date).put("type", it.type.name).put("foods", JSONArray(it.foods))) } })
        .put("skin", JSONArray().apply { data.skinEntries.forEach { put(JSONObject().put("id", it.id).put("date", it.date).put("photoUri", it.photoUri).put("products", it.products).put("notes", it.notes).put("zones", JSONArray(it.zones.toList()))) } })
        .put("homework", JSONArray().apply { data.homeworkEntries.forEach { put(JSONObject().put("id", it.id).put("lesson", it.lesson).put("title", it.title).put("dueDate", it.dueDate).put("priority", it.priority.name).put("attachment", it.attachment)) } })
        .put("water", JSONArray().apply { data.waterEntries.forEach { put(JSONObject().put("id", it.id).put("date", it.date).put("time", it.time).put("amountMl", it.amountMl)) } })
        .put("people", JSONArray().apply { data.people.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("photoUri", it.photoUri)) } })
        .put("accounts", JSONArray().apply {
            data.accounts.forEach {
                put(
                    JSONObject()
                        .put("id", it.id).put("personId", it.personId).put("name", it.name)
                        .put("type", it.type.name).put("iconKey", it.iconKey)
                        .put("balance", it.balance).put("creditLimit", it.creditLimit).put("dueAmount", it.dueAmount),
                )
            }
        })
        .put("transactions", JSONArray().apply { data.transactions.forEach { put(JSONObject().put("id", it.id).put("personId", it.personId).put("accountId", it.accountId).put("category", it.category.name).put("amount", it.amount).put("currency", it.currency.name).put("description", it.description).put("date", it.date)) } })
        .put("trees", JSONArray().apply { data.treeEntries.forEach { put(JSONObject().put("id", it.id).put("date", it.date).put("durationMinutes", it.durationMinutes).put("treeKey", it.treeKey)) } })
        .put("currentPoints", data.currentPoints)
        .put("freezePassCount", data.freezePassCount)
        .put("purchasedItems", JSONArray(data.purchasedItems.toList()))
        .put("selectedPet", data.selectedPet)
        .put("selectedTree", data.selectedTree)
        .put("selectedBowl", data.selectedBowl)
        .put("selectedGlass", data.selectedGlass)
        .put("selectedBackground", data.selectedBackground)
}

private fun demoData(): AppData {
    val today = LocalDate.now()
    val ben = BudgetPerson(newId(), "Ben")
    val bank = BudgetAccount(newId(), ben.id, "Ana Hesap", AccountType.Bank, "", balance = 18500.0)
    val card = BudgetAccount(newId(), ben.id, "Visa Kart", AccountType.Card, "", creditLimit = 15000.0, balance = 9700.0, dueAmount = 2800.0)
    return AppData(
        sleepEntries = listOf(
            SleepEntry(newId(), today.minusDays(2).toString(), "23:20", "07:10"),
            SleepEntry(newId(), today.minusDays(1).toString(), "22:55", "06:45"),
            SleepEntry(newId(), today.toString(), "23:05", "07:25"),
        ),
        mealEntries = listOf(
            MealEntry(newId(), today.toString(), MealType.Morning, listOf("Tost", "Çay", "Süt")),
            MealEntry(newId(), today.toString(), MealType.Lunch, listOf("Börek", "Ayran")),
            MealEntry(newId(), today.minusDays(1).toString(), MealType.Evening, listOf("Çorba", "Pilav")),
        ),
        skinEntries = emptyList(),
        homeworkEntries = listOf(
            HomeworkEntry(newId(), "Matematik", "Problemler testi", today.plusDays(1).toString(), Priority.Urgent, ""),
            HomeworkEntry(newId(), "Türkçe", "Kitap özeti", today.plusDays(3).toString(), Priority.Important, ""),
        ),
        waterEntries = listOf(
            WaterEntry(newId(), today.toString(), "09:30", 250),
            WaterEntry(newId(), today.toString(), "12:00", 500),
            WaterEntry(newId(), today.minusDays(1).toString(), "20:00", 250),
        ),
        treeEntries = emptyList(),
        people = listOf(ben),
        accounts = listOf(bank, card),
        transactions = listOf(
            TxnEntry(newId(), ben.id, bank.id, TxnCategory.Salary, 25000.0, Currency.TRY, "Maaş", today.toString()),
            TxnEntry(newId(), ben.id, bank.id, TxnCategory.Rent, 8000.0, Currency.TRY, "Ev kirası", today.minusDays(2).toString()),
            TxnEntry(newId(), ben.id, card.id, TxnCategory.Food, 1450.0, Currency.TRY, "Market", today.minusDays(1).toString()),
            TxnEntry(newId(), ben.id, card.id, TxnCategory.Electricity, 740.0, Currency.TRY, "Elektrik faturası", today.minusDays(3).toString()),
        ),
        waterTargetMl = 2500,
        budgetLimit = 12000.0,
        currency = Currency.TRY,
        profile = Profile(),
        palette = Palette.Obsidian,
        themeMode = ThemeMode.Dark,
        currentPoints = 250,
        freezePassCount = 1,
        purchasedItems = DefaultStoreCatalog.filter { it.isDefault }.map { it.key }.toSet(),
        selectedPet = "pet_dog",
        selectedTree = "tree_oak",
        selectedBowl = "bowl_classic",
        selectedGlass = "glass_basic",
        selectedBackground = "bg_aurora",
    )
}

// endregion

// region Theme

@Composable
private fun GlaethTheme(palette: Palette, themeMode: ThemeMode, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val scheme: ColorScheme = if (isDark) {
        darkColorScheme(
            primary = palette.darkPrimary,
            onPrimary = Color(0xFF0B0F14),
            secondary = palette.darkSecondary,
            tertiary = palette.accentSoft,
            background = palette.darkBackground,
            surface = palette.darkSurface,
            surfaceVariant = Color(0xFF1B1F27),
            onBackground = Color(0xFFF5F7FA),
            onSurface = Color(0xFFEAEEF4),
            primaryContainer = Color(0xFF1B1F27),
            onPrimaryContainer = Color(0xFFF8FAFC),
        )
    } else {
        lightColorScheme(
            primary = palette.lightPrimary,
            onPrimary = Color.White,
            secondary = palette.lightSecondary,
            tertiary = palette.accentSoft,
            background = palette.lightBackground,
            surface = palette.lightSurface,
            surfaceVariant = Color(0xFFE5E7EB),
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF111827),
            primaryContainer = Color(0xFFE2E8F0),
            onPrimaryContainer = Color(0xFF111827),
        )
    }
    androidx.compose.material3.MaterialTheme(colorScheme = scheme) {
        CompositionLocals(isDark = isDark, content = content)
    }
}

private val LocalIsDark = androidx.compose.runtime.staticCompositionLocalOf { true }

@Composable
private fun CompositionLocals(isDark: Boolean, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalIsDark provides isDark, content = content)
}

// endregion

// region Root

@Composable
private fun GlaethRoot() {
    val context = LocalContext.current
    val repository = remember { AppRepository(context) }
    var data by remember { mutableStateOf(repository.load()) }

    LaunchedEffect(data) {
        withContext(Dispatchers.IO) { repository.save(data) }
    }

    GlaethTheme(palette = data.palette, themeMode = data.themeMode) {
        GlaethApp(
            data = data,
            updateData = { data = it },
            repository = repository,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlaethApp(data: AppData, updateData: (AppData) -> Unit, repository: AppRepository) {
    val context = LocalContext.current
    var section by rememberSaveable { mutableStateOf(Section.Dashboard) }
    var sheet by remember { mutableStateOf<Section?>(null) }
    var profileSheet by remember { mutableStateOf(false) }
    var fullImage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(repository.encode(data).toString(2).toByteArray())
                }
            }.onSuccess {
                Toast.makeText(context, "Yedek kaydedildi", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Yedek alınamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.openInputStream(it)?.use { input ->
                    val text = BufferedReader(InputStreamReader(input)).readText()
                    val tempData = repository.let { repo ->
                        // reuse decode via writing to db then reading
                        runCatching {
                            val tempJson = JSONObject(text)
                            // delegate to repository's private decode through reflection-free approach: re-save then load
                            // simpler: temporarily save into prefs; here just write & reload
                            val helper = GlaethDatabase(context)
                            val cv = ContentValues().apply {
                                put("state_key", "main")
                                put("payload", tempJson.toString())
                            }
                            helper.writableDatabase.insertWithOnConflict("app_state", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                            repo.load()
                        }.getOrThrow()
                    }
                    updateData(tempData)
                }
            }.onSuccess {
                Toast.makeText(context, "Yedek geri yüklendi", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Yedek okunamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var storeSheet by remember { mutableStateOf(false) }
    var sleepEdit by remember { mutableStateOf<SleepEntry?>(null) }
    var historyFilter by remember { mutableStateOf<String?>(null) }
    var vaultPersonId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            if (section in listOf(Section.Sleep, Section.Meals, Section.Water, Section.Skin, Section.Homework, Section.Budget)) {
                FloatingActionButton(
                    onClick = { sheet = section },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) { Icon(Icons.Filled.Add, contentDescription = "Ekle") }
            }
        },
        bottomBar = { GlassBottomBar(selected = section, onSelect = { section = it }, currency = data.currency) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(appBackground(data.palette)).padding(padding)) {
            AnimatedContent(targetState = section, label = "section") { target ->
                when (target) {
                    Section.Dashboard -> DashboardScreen(
                        data = data,
                        onOpen = { section = it },
                        onProfile = { profileSheet = true },
                        onPetAction = { reward ->
                            updateData(data.copy(currentPoints = data.currentPoints + reward))
                            Toast.makeText(context, "+$reward puan", Toast.LENGTH_SHORT).show()
                        },
                        onSeeAll = {
                            historyFilter = null
                            section = Section.History
                        },
                        onOpenStore = { storeSheet = true },
                    )
                    Section.Sleep -> SleepScreen(
                        entries = data.sleepEntries,
                        profile = data.profile,
                        onDelete = { id -> updateData(data.copy(sleepEntries = data.sleepEntries.filterNot { it.id == id })) },
                        onEdit = { entry -> sleepEdit = entry },
                    )
                    Section.Meals -> MealScreen(
                        entries = data.mealEntries,
                        profile = data.profile,
                        bowlSkin = data.selectedBowl,
                        onDelete = { id -> updateData(data.copy(mealEntries = data.mealEntries.filterNot { it.id == id })) },
                    )
                    Section.Water -> WaterScreen(
                        entries = data.waterEntries,
                        targetMl = data.waterTargetMl,
                        glassSkin = data.selectedGlass,
                        onTargetChange = { updateData(data.copy(waterTargetMl = it.coerceIn(500, 5000))) },
                        onDelete = { id -> updateData(data.copy(waterEntries = data.waterEntries.filterNot { it.id == id })) },
                    )
                    Section.Skin -> SkinScreen(
                        entries = data.skinEntries,
                        onDelete = { id -> updateData(data.copy(skinEntries = data.skinEntries.filterNot { it.id == id })) },
                        onOpenPhoto = { fullImage = it },
                    )
                    Section.Forest -> ForestScreen(
                        data = data,
                        onComplete = { entry ->
                            updateData(
                                data.copy(
                                    treeEntries = data.treeEntries + entry,
                                    currentPoints = data.currentPoints + entry.durationMinutes,
                                ),
                            )
                            Toast.makeText(context, "+${entry.durationMinutes} puan kazandın", Toast.LENGTH_SHORT).show()
                        },
                        onSelectTree = { key -> updateData(data.copy(selectedTree = key)) },
                    )
                    Section.Homework -> HomeworkScreen(data.homeworkEntries) { id ->
                        updateData(data.copy(homeworkEntries = data.homeworkEntries.filterNot { it.id == id }))
                    }
                    Section.Budget -> BudgetScreen(
                        data = data,
                        onUpdate = updateData,
                        onOpenVault = { id -> vaultPersonId = id },
                    )
                    Section.History -> AllHistoryScreen(
                        data = data,
                        initialFilter = historyFilter,
                        onBack = { section = Section.Dashboard },
                    )
                    Section.Settings -> SettingsScreen(
                        data = data,
                        onPaletteChange = { updateData(data.copy(palette = it)) },
                        onThemeModeChange = { updateData(data.copy(themeMode = it)) },
                        onCurrencyChange = { updateData(data.copy(currency = it)) },
                        onBackgroundChange = { key -> updateData(data.copy(selectedBackground = key)) },
                        onProfileClick = { profileSheet = true },
                        onOpenStore = { storeSheet = true },
                        onExport = {
                            val ts = LocalDate.now().toString()
                            exportLauncher.launch("glaeth-yedek-$ts.json")
                        },
                        onImport = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) },
                    )
                }
            }
            TopProfileBar(
                profile = data.profile,
                points = data.currentPoints,
                onProfile = { profileSheet = true },
                onPoints = { storeSheet = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 12.dp, end = 16.dp),
            )
        }
    }

    if (storeSheet) {
        ModalBottomSheet(
            onDismissRequest = { storeSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            StoreScreen(
                data = data,
                onPurchase = { item ->
                    if (data.currentPoints >= item.cost && item.key !in data.purchasedItems) {
                        val updated = data.copy(
                            currentPoints = data.currentPoints - item.cost,
                            purchasedItems = data.purchasedItems + item.key,
                            freezePassCount = if (item.category == StoreCategory.Freeze) data.freezePassCount + 1 else data.freezePassCount,
                        )
                        updateData(updated)
                        Toast.makeText(context, "${item.label} alındı", Toast.LENGTH_SHORT).show()
                    } else if (item.key in data.purchasedItems && item.category != StoreCategory.Freeze) {
                        Toast.makeText(context, "Zaten envanterinde", Toast.LENGTH_SHORT).show()
                    } else if (item.category == StoreCategory.Freeze && data.currentPoints >= item.cost) {
                        val updated = data.copy(
                            currentPoints = data.currentPoints - item.cost,
                            freezePassCount = data.freezePassCount + 1,
                            purchasedItems = data.purchasedItems + item.key,
                        )
                        updateData(updated)
                    } else {
                        Toast.makeText(context, "Yeterli puanın yok", Toast.LENGTH_SHORT).show()
                    }
                },
                onSelect = { item ->
                    val key = item.key
                    val updated = when (item.category) {
                        StoreCategory.Pet -> data.copy(selectedPet = key)
                        StoreCategory.Tree -> data.copy(selectedTree = key)
                        StoreCategory.MealSkin -> data.copy(selectedBowl = key)
                        StoreCategory.WaterSkin -> data.copy(selectedGlass = key)
                        StoreCategory.Background -> data.copy(selectedBackground = key)
                        StoreCategory.Freeze -> data
                    }
                    updateData(updated)
                },
            )
        }
    }

    sleepEdit?.let { entry ->
        ModalBottomSheet(
            onDismissRequest = { sleepEdit = null },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            SleepEditForm(initial = entry) { updated ->
                val newList = data.sleepEntries.map { if (it.id == updated.id) updated else it }
                updateData(data.copy(sleepEntries = newList.sortedByDescending { it.date }))
                sleepEdit = null
            }
        }
    }

    vaultPersonId?.let { id ->
        val person = data.people.firstOrNull { it.id == id }
        if (person != null) {
            ModalBottomSheet(
                onDismissRequest = { vaultPersonId = null },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                VaultDetailScreen(
                    data = data,
                    person = person,
                )
            }
        } else {
            vaultPersonId = null
        }
    }

    sheet?.let { active ->
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            when (active) {
                Section.Sleep -> SleepForm { entry ->
                    updateData(data.copy(sleepEntries = (data.sleepEntries + entry).sortedByDescending { it.date }))
                    sheet = null
                }
                Section.Meals -> MealForm { entry ->
                    updateData(data.copy(mealEntries = (data.mealEntries + entry).sortedByDescending { it.date }))
                    sheet = null
                }
                Section.Water -> WaterForm { entry ->
                    updateData(data.copy(waterEntries = (data.waterEntries + entry).sortedByDescending { it.date + " " + it.time }))
                    sheet = null
                }
                Section.Skin -> SkinForm { entries ->
                    updateData(data.copy(skinEntries = (data.skinEntries + entries).sortedByDescending { it.date }))
                    sheet = null
                }
                Section.Homework -> HomeworkForm { entry ->
                    updateData(data.copy(homeworkEntries = (data.homeworkEntries + entry).sortedBy { it.dueDate }))
                    sheet = null
                }
                Section.Budget -> BudgetForm(data) { newData ->
                    updateData(newData)
                    sheet = null
                }
                Section.Dashboard, Section.Forest, Section.History, Section.Settings -> Unit
            }
        }
    }

    if (profileSheet) {
        ModalBottomSheet(
            onDismissRequest = { profileSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            ProfileForm(profile = data.profile) { newProfile ->
                updateData(data.copy(profile = newProfile))
                profileSheet = false
            }
        }
    }

    fullImage?.let { uri ->
        FullScreenPhoto(uri = uri, onDismiss = { fullImage = null })
    }
}

// endregion

// region Dashboard

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashboardScreen(
    data: AppData,
    onOpen: (Section) -> Unit,
    onProfile: () -> Unit,
    onPetAction: (Int) -> Unit,
    onSeeAll: () -> Unit,
    onOpenStore: () -> Unit,
) {
    val skinDates = data.skinEntries.map { it.date }
    val waterDates = data.waterEntries.map { it.date }.distinct()
    val mealDates = data.mealEntries.map { it.date }.distinct()
    val skinStreakInfo = streakWithFreeze(skinDates, data.freezePassCount)
    val waterStreakInfo = streakWithFreeze(waterDates, data.freezePassCount)
    val mealStreakInfo = streakWithFreeze(mealDates, data.freezePassCount)
    val today = LocalDate.now().toString()
    val todayWaterMl = data.waterEntries.filter { it.date == today }.sumOf { it.amountMl }
    val recent = recentUpdates(data).take(6)
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            ScenicHeader(
                profileName = data.profile.name.ifBlank { "Glaeth" },
                backgroundKey = data.selectedBackground,
                pagerState = pagerState,
                pageContent = { page ->
                    when (page) {
                        0 -> PetScorePage(data = data, onPetAction = onPetAction)
                        1 -> SkinScorePage(data = data, streak = skinStreakInfo, onAction = { onOpen(Section.Skin) })
                        2 -> WaterScorePage(data = data, todayMl = todayWaterMl, streak = waterStreakInfo, onAction = { onOpen(Section.Water) })
                        3 -> BudgetScorePage(data = data, onAction = { onOpen(Section.Budget) })
                        else -> Unit
                    }
                },
            )
            BodyArea(
                skinStreak = skinStreakInfo,
                waterStreak = waterStreakInfo,
                mealStreak = mealStreakInfo,
                todayWaterMl = todayWaterMl,
                waterTarget = data.waterTargetMl,
                updates = recent,
                onOpen = onOpen,
                onSeeAll = onSeeAll,
                onOpenStore = onOpenStore,
                points = data.currentPoints,
            )
            Spacer(Modifier.height(120.dp))
        }
    }
}

private data class StreakInfo(val days: Int, val isFrozen: Boolean)

private fun streakWithFreeze(dates: List<String>, freezePassCount: Int): StreakInfo {
    val parsed = dates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toHashSet()
    if (parsed.isEmpty()) return StreakInfo(0, false)
    val today = LocalDate.now()
    val frozen = !parsed.contains(today) && parsed.contains(today.minusDays(1)) && freezePassCount > 0
    var current = if (parsed.contains(today)) today else today.minusDays(1)
    var count = 0
    while (parsed.contains(current)) {
        count += 1
        current = current.minusDays(1)
    }
    return StreakInfo(count, frozen)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScenicHeader(
    profileName: String,
    backgroundKey: String,
    pagerState: androidx.compose.foundation.pager.PagerState,
    pageContent: @Composable (Int) -> Unit,
) {
    val isDark = LocalIsDark.current
    val skyColors = backgroundColors(backgroundKey, isDark)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(640.dp)
            .background(Brush.verticalGradient(skyColors)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.45f),
                        ),
                    ),
                ),
        )
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    val greet = timeBasedGreeting()
                    Text(greet.first, color = Color.White.copy(alpha = 0.92f), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(profileName, color = Color.White, fontWeight = FontWeight.Black, fontSize = 38.sp)
                }
                Spacer(Modifier.width(120.dp))
            }
            Spacer(Modifier.height(8.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 36.dp),
                pageSpacing = 12.dp,
            ) { page -> pageContent(page) }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                repeat(4) { index ->
                    val active = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (active) 9.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (active) Color.White else Color.White.copy(alpha = 0.45f)),
                    )
                }
            }
        }
    }
}

private fun backgroundColors(key: String, isDark: Boolean): List<Color> {
    val palettes = mapOf(
        "bg_aurora" to (listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF155E75), Color(0xFF0F3D45)) to listOf(Color(0xFF7C3AED), Color(0xFFEC4899), Color(0xFFF97316), Color(0xFF14B8A6))),
        "bg_ocean" to (listOf(Color(0xFF051937), Color(0xFF002b4f), Color(0xFF003c5e), Color(0xFF005e75)) to listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9), Color(0xFF0284C7), Color(0xFF0369A1))),
        "bg_forest" to (listOf(Color(0xFF052e16), Color(0xFF064e3b), Color(0xFF065f46), Color(0xFF0f766e)) to listOf(Color(0xFF34D399), Color(0xFF22C55E), Color(0xFF15803D), Color(0xFF065F46))),
        "bg_galaxy" to (listOf(Color(0xFF0B1020), Color(0xFF1E1B4B), Color(0xFF581C87), Color(0xFF7C3AED)) to listOf(Color(0xFF60A5FA), Color(0xFF7C3AED), Color(0xFFC026D3), Color(0xFFF472B6))),
        "bg_desert" to (listOf(Color(0xFF3E1F0E), Color(0xFF7C2D12), Color(0xFFEA580C), Color(0xFFFACC15)) to listOf(Color(0xFFFCA5A5), Color(0xFFF97316), Color(0xFFFACC15), Color(0xFFFEF3C7))),
    )
    val pair = palettes[key] ?: palettes["bg_aurora"]!!
    return if (isDark) pair.first else pair.second
}

@Composable
private fun GlassScoreCircle(
    accent: Color,
    fraction: Float,
    centerContent: @Composable () -> Unit,
) {
    val animated by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), label = "score")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(296.dp)) {
                val stroke = 14.dp.toPx()
                drawArc(
                    color = Color.White.copy(alpha = 0.18f),
                    startAngle = 135f, sweepAngle = 270f, useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = accent,
                    startAngle = 135f, sweepAngle = 270f * animated, useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            centerContent()
        }
    }
}

@Composable
private fun PetScorePage(data: AppData, onPetAction: (Int) -> Unit) {
    val petItem = DefaultStoreCatalog.firstOrNull { it.key == data.selectedPet } ?: DefaultStoreCatalog.first { it.category == StoreCategory.Pet && it.isDefault }
    val happiness = ((data.currentPoints % 100) / 100f).coerceIn(0.05f, 1f)
    GlassScoreCircle(accent = Color(0xFFF472B6), fraction = happiness) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(petItem.emoji, fontSize = 90.sp)
            Text(petItem.label, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PetActionButton("Sev") { onPetAction(5) }
                PetActionButton("Besle") { onPetAction(8) }
                PetActionButton("Temizle") { onPetAction(6) }
            }
        }
    }
}

@Composable
private fun PetActionButton(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = Color.White.copy(alpha = 0.20f),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(label, color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun SkinScorePage(data: AppData, streak: StreakInfo, onAction: () -> Unit) {
    val accent = Color(0xFFFB923C)
    val total = data.skinEntries.size
    GlassScoreCircle(accent = accent, fraction = (total / 100f).coerceIn(0f, 1f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(shape = RoundedCornerShape(99.dp), color = Color.Black.copy(alpha = 0.45f)) {
                Text(
                    if (total == 0) "Cilt yolculuğun başlasın" else "$total fotoğraflık arşiv",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color = Color.White,
                    fontSize = 12.sp,
                )
            }
            Text("${streak.days} Gün", color = Color.White, fontWeight = FontWeight.Black, fontSize = 64.sp)
            Text(if (streak.isFrozen) "🧊" else "🔥", fontSize = 36.sp)
            Spacer(Modifier.height(2.dp))
            CtaButton("Detayları Gör", accent, onAction)
        }
    }
}

@Composable
private fun WaterScorePage(data: AppData, todayMl: Int, streak: StreakInfo, onAction: () -> Unit) {
    val accent = Color(0xFF38BDF8)
    val target = data.waterTargetMl
    val remaining = (target - todayMl).coerceAtLeast(0)
    val message = when {
        target == 0 -> "Hedef ayarla"
        todayMl == 0 -> "Bugün başla 💧"
        remaining == 0 -> "Hedefe ulaştın 🎉"
        remaining <= 250 -> "Son $remaining ml!"
        todayMl < target / 2 -> "Hadi devam, $remaining ml var"
        else -> "Yarısını geçtin 🚰"
    }
    GlassScoreCircle(accent = accent, fraction = (todayMl.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(shape = RoundedCornerShape(99.dp), color = Color.Black.copy(alpha = 0.45f)) {
                Text("$todayMl / $target ml", modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), color = Color.White, fontSize = 12.sp)
            }
            Text(message, color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 18.dp))
            Text("${streak.days} gün ${if (streak.isFrozen) "🧊" else "🔥"}", color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(2.dp))
            CtaButton("Su Geçmişi", accent, onAction)
        }
    }
}

@Composable
private fun BudgetScorePage(data: AppData, onAction: () -> Unit) {
    val accent = Color(0xFF14B8A6)
    val expense = monthlyExpense(data)
    val income = data.transactions.filter { it.category.isIncome }.sumOf { it.amount }
    val limit = if (data.budgetLimit > 0) data.budgetLimit else income.coerceAtLeast(1.0)
    val ratio = (expense / limit).coerceIn(0.0, 1.5).toFloat()
    val card = data.accounts.firstOrNull { it.type == AccountType.Card && it.creditLimit > 0 }
    val cardRatio = if (card != null && card.creditLimit > 0) (card.balance / card.creditLimit).coerceIn(0.0, 1.0).toFloat() else 0f
    val message = when {
        ratio < 0.5f -> "Harika gidiyorsun, artısın! 🚀"
        ratio < 0.85f -> "Dengeli ilerliyor 👌"
        ratio < 1.05f -> "Bu ay limitleri zorluyorsun ⚠️"
        else -> "Limit aşıldı 🚨"
    }
    GlassScoreCircle(accent = accent, fraction = (1f - ratio.coerceAtMost(1f)).coerceAtLeast(0.05f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 22.dp)) {
            Surface(shape = RoundedCornerShape(99.dp), color = Color.Black.copy(alpha = 0.45f)) {
                Text(
                    "${data.currency.symbol}${expense.format()} / ${data.currency.symbol}${limit.format()}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color = Color.White, fontSize = 12.sp,
                )
            }
            Text(message, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp, textAlign = TextAlign.Center)
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MultiBar(label = "Gelir", value = income, max = (income + expense).coerceAtLeast(1.0), color = Color(0xFF22C55E))
                MultiBar(label = "Gider", value = expense, max = limit, color = Color(0xFFEF4444))
                if (card != null) MultiBar(label = "${card.name} limit", value = card.balance, max = card.creditLimit.coerceAtLeast(1.0), color = Color(0xFFF59E0B), forceRatio = cardRatio.toDouble())
            }
            Spacer(Modifier.height(2.dp))
            CtaButton("Bütçeye Git", accent, onAction)
        }
    }
}

@Composable
private fun MultiBar(label: String, value: Double, max: Double, color: Color, forceRatio: Double? = null) {
    val ratio = (forceRatio ?: (value / max.coerceAtLeast(1.0))).toFloat().coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, modifier = Modifier.width(100.dp))
        Box(modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha = 0.15f))) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(ratio).clip(RoundedCornerShape(99.dp)).background(color))
        }
    }
}

@Composable
private fun CtaButton(label: String, accent: Color, onAction: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = accent.copy(alpha = 0.92f),
        modifier = Modifier.clickable(onClick = onAction),
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black)
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.Black, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun BodyArea(
    skinStreak: StreakInfo,
    waterStreak: StreakInfo,
    mealStreak: StreakInfo,
    todayWaterMl: Int,
    waterTarget: Int,
    updates: List<FeedItem>,
    onOpen: (Section) -> Unit,
    onSeeAll: () -> Unit,
    onOpenStore: () -> Unit,
    points: Int,
) {
    val shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().offset(y = (-32).dp).clip(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(Alignment.CenterHorizontally)
                    .width(48.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)),
            )
            // Streak rozet satırı
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { StreakChip("Cilt", skinStreak, Color(0xFFFB923C)) }
                item { StreakChip("Su", waterStreak, Color(0xFF38BDF8)) }
                item { StreakChip("Öğün", mealStreak, Color(0xFF22C55E)) }
                item { PointBadge(points = points, onClick = onOpenStore) }
            }
            LatestUpdatesSection(updates = updates, onSeeAll = onSeeAll)
            QuickActions(onOpen)
            BodyWaterPill(todayWaterMl, waterTarget, onOpen)
        }
    }
}

@Composable
private fun StreakChip(label: String, info: StreakInfo, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(99.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(if (info.isFrozen) "🧊" else "🔥", fontSize = 16.sp)
        Text("${info.days}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontSize = 12.sp)
    }
}

@Composable
private fun PointBadge(points: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Text("$points puan", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 13.sp)
    }
}

@Composable
private fun BodyWaterPill(todayMl: Int, target: Int, onOpen: (Section) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(22.dp))
            .clickable { onOpen(Section.Water) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF38BDF8).copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = Color(0xFF38BDF8)) }
        Column(modifier = Modifier.weight(1f)) {
            Text("Bugün $todayMl / $target ml su", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Aç ve hızlıca bardak ekle", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

private data class FeedItem(val title: String, val subtitle: String, val icon: ImageVector, val tint: Color)

private fun recentUpdates(data: AppData): List<FeedItem> {
    val list = mutableListOf<Pair<String, FeedItem>>()
    data.skinEntries.forEach {
        list += it.date to FeedItem("Cilt fotoğrafı eklendi", "${it.date} – arşivde", Icons.Filled.Face, Color(0xFFFB923C))
    }
    data.waterEntries.forEach {
        list += "${it.date} ${it.time}" to FeedItem("${it.amountMl} ml su içildi", "${it.date} ${it.time}", Icons.Filled.WaterDrop, Color(0xFF38BDF8))
    }
    data.mealEntries.forEach {
        list += it.date to FeedItem("${it.type.label} kaydı", "${it.foods.take(3).joinToString(", ")} (${it.date})", Icons.Filled.LocalDining, Color(0xFF22C55E))
    }
    data.sleepEntries.forEach {
        list += it.date to FeedItem("Uyku ${it.sleptAt} → ${it.wokeAt}", "${it.date} – uyku kaydı", Icons.Filled.DateRange, Color(0xFFA855F7))
    }
    data.transactions.forEach {
        val sign = if (it.category.isIncome) "+" else "-"
        list += it.date to FeedItem("${it.category.label} $sign${it.amount.format()}${it.currency.symbol}", "${it.description} (${it.date})", Icons.Filled.Receipt, it.category.color)
    }
    data.homeworkEntries.forEach {
        list += it.dueDate to FeedItem("${it.lesson} – ${it.title}", "Teslim: ${it.dueDate}", Icons.Filled.School, it.priority.color)
    }
    return list.sortedByDescending { it.first }.map { it.second }
}

@Composable
private fun LatestUpdatesSection(updates: List<FeedItem>, onSeeAll: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Son güncellemeler", fontWeight = FontWeight.Black, fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground)
            TextButton(onClick = onSeeAll) { Text("Tümü", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        }
        if (updates.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = if (LocalIsDark.current) 0.78f else 1f),
            ) {
                Text(
                    "Henüz hareket yok. Yeni kayıt eklemek için aşağıdaki bölümleri kullan.",
                    modifier = Modifier.padding(18.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        } else {
            updates.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = if (LocalIsDark.current) 0.85f else 1f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(item.tint.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.icon, contentDescription = null, tint = item.tint)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(item.subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActions(onOpen: (Section) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Hızlı erişim", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(listOf(Section.Sleep, Section.Meals, Section.Water, Section.Skin, Section.Homework, Section.Budget, Section.Settings)) { section ->
                Column(
                    modifier = Modifier
                        .width(112.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = if (LocalIsDark.current) 0.85f else 1f))
                        .clickable { onOpen(section) }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(section.label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, maxLines = 1)
                    Text("Aç", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
        }
    }
}

// endregion

// region Sections (sleep/meal/skin/homework reused with cleaner styles)

@Composable
private fun SleepScreen(entries: List<SleepEntry>, profile: Profile, onDelete: (String) -> Unit, onEdit: (SleepEntry) -> Unit) {
    val average = entries.mapNotNull { it.duration() }.averageOrZero()
    var chartKind by rememberSaveable { mutableStateOf(ChartKind.Bar) }
    SectionList(
        title = "Uyku günlüğü",
        subtitle = "Yatma, uyanma saati ve toplam süreyi takip et. Sola kaydır: sil, sağa kaydır: düzenle.",
        header = {
            ChartKindSelector(chartKind) { chartKind = it }
            SleepChart(entries, profile.age, chartKind)
            InsightCard("Sana Özel Tavsiye 💡", sleepStatus(average, profile.age).detail)
        },
    ) {
        items(entries) { entry ->
            EditableDismissibleItem(
                onDelete = { onDelete(entry.id) },
                onEdit = { onEdit(entry) },
            ) {
                SleepEntryCard(entry = entry, age = profile.age)
            }
        }
    }
}

@Composable
private fun SleepChart(entries: List<SleepEntry>, age: Int, kind: ChartKind = ChartKind.Bar) {
    val target = sleepTarget(age).recommended
    GlassCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Detaylı uyku grafiği", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Hedef ${target.oneDecimal()}s", color = MaterialTheme.colorScheme.primary)
            }
            val data = entries.takeLast(7)
            when (kind) {
                ChartKind.Bar -> SleepBars(data, target)
                ChartKind.Line -> SleepLine(data, target)
                ChartKind.Candle -> SleepCandles(data, target)
            }
        }
    }
}

@Composable
private fun SleepBars(data: List<SleepEntry>, target: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().height(168.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        data.forEach { entry ->
            val hours = entry.duration() ?: 0.0
            val enough = hours >= target
            val fraction = (hours / (target + 2)).coerceIn(0.08, 1.0).toFloat()
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        if (enough) Color(0xFF22C55E) else Color(0xFFEF4444),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    ),
                                ),
                            ),
                    )
                }
                Text("${hours.oneDecimal()}s", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(entry.date.takeLast(5), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
        }
    }
}

@Composable
private fun SleepLine(data: List<SleepEntry>, target: Double) {
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    Canvas(modifier = Modifier.fillMaxWidth().height(168.dp)) {
        if (data.isEmpty()) return@Canvas
        val width = size.width
        val height = size.height
        val maxHours = (data.maxOf { it.duration() ?: 0.0 }).coerceAtLeast(target + 1)
        val step = if (data.size > 1) width / (data.size - 1) else 0f
        for (i in 0..3) {
            val y = height * i / 3f
            drawLine(track, Offset(0f, y), Offset(width, y), strokeWidth = 1f)
        }
        val points = data.mapIndexed { index, entry ->
            val hours = entry.duration() ?: 0.0
            val x = step * index
            val y = height - ((hours / maxHours).toFloat() * height)
            Offset(x, y)
        }
        for (i in 0 until points.size - 1) {
            drawLine(accent, points[i], points[i + 1], strokeWidth = 6f, cap = StrokeCap.Round)
        }
        points.forEach { p ->
            drawCircle(accent, radius = 8f, center = p)
        }
    }
}

@Composable
private fun SleepCandles(data: List<SleepEntry>, target: Double) {
    val up = Color(0xFF22C55E)
    val down = Color(0xFFEF4444)
    Canvas(modifier = Modifier.fillMaxWidth().height(168.dp)) {
        if (data.isEmpty()) return@Canvas
        val width = size.width
        val height = size.height
        val slot = width / data.size
        val maxRange = 14.0
        data.forEachIndexed { index, entry ->
            val x = slot * index + slot / 2f
            val hours = entry.duration() ?: 0.0
            val good = hours >= target
            val color = if (good) up else down
            val openY = height * (1 - (hours / maxRange).toFloat()).coerceIn(0.05f, 0.95f)
            val closeY = height * (1 - ((hours + 0.4) / maxRange).toFloat()).coerceIn(0f, 1f)
            val highY = height * (1 - ((hours + 1.0) / maxRange).toFloat()).coerceIn(0f, 1f)
            val lowY = height * (1 - ((hours - 0.5) / maxRange).toFloat()).coerceIn(0f, 1f)
            drawLine(color, Offset(x, highY), Offset(x, lowY), strokeWidth = 3f)
            val candleWidth = slot * 0.45f
            drawRect(
                color = color,
                topLeft = Offset(x - candleWidth / 2f, kotlin.math.min(openY, closeY)),
                size = Size(candleWidth, kotlin.math.abs(closeY - openY).coerceAtLeast(6f)),
            )
        }
    }
}

@Composable
private fun ChartKindSelector(selected: ChartKind, onSelected: (ChartKind) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        ChartKind.entries.forEach { kind ->
            val active = kind == selected
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .border(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), RoundedCornerShape(99.dp))
                    .clickable { onSelected(kind) },
            ) {
                Text(
                    kind.label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SleepEntryCard(entry: SleepEntry, age: Int) {
    val hours = entry.duration().orZero()
    val status = sleepStatus(hours, age)
    GlassCard {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = status.color)
            Column(modifier = Modifier.weight(1f)) {
                Text("${entry.sleptAt} – ${entry.wokeAt}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("${entry.date} • ${hours.oneDecimal()} saat • ${status.title}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun MealScreen(entries: List<MealEntry>, profile: Profile, bowlSkin: String, onDelete: (String) -> Unit) {
    val today = LocalDate.now().toString()
    val todayCount = entries.count { it.date == today }
    SectionList(
        title = "Öğünler",
        subtitle = "Yaşa göre besin önerileri ve günlük öğün kayıtları.",
        header = {
            BowlVisual(skinKey = bowlSkin, fillCount = todayCount)
            InsightCard("Besin önerisi", mealSuggestion(profile.age))
        },
    ) {
        items(entries.groupBy { it.date }.toList()) { (date, day) ->
            GlassCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Gün: $date", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    day.forEach { meal ->
                        DismissibleItem(onDelete = { onDelete(meal.id) }) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(meal.type.label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(meal.foods.joinToString(separator = "\n"), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f))
                            }
                        }
                        if (meal != day.last()) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SkinScreen(entries: List<SkinEntry>, onDelete: (String) -> Unit, onOpenPhoto: (String) -> Unit) {
    val indexed = entries.sortedBy { it.date }.mapIndexed { index, entry -> entry.id to index + 1 }.toMap()
    SectionList(
        title = "Cilt takip",
        subtitle = "Gün numarası, silme ve tam ekran fotoğraf görüntüleme.",
        header = {
            HeroBanner(
                title = "Toplu içe aktarma",
                subtitle = "Fotoğrafa dokun: büyüt. Sola kaydır: sil. Zaman yolculuğuna dokun: ilk fotoğrafı aç.",
                icon = Icons.Filled.Face,
            )
            if (entries.isNotEmpty()) TimeLapseStrip(entries = entries, dayNumbers = indexed, onOpenPhoto = onOpenPhoto)
        },
    ) {
        items(entries) { entry ->
            DismissibleItem(onDelete = { onDelete(entry.id) }) {
                SkinEntryCard(entry = entry, dayNumber = indexed[entry.id] ?: 1, onOpenPhoto = { onOpenPhoto(entry.photoUri) })
            }
        }
    }
}

@Composable
private fun TimeLapseStrip(entries: List<SkinEntry>, dayNumbers: Map<String, Int>, onOpenPhoto: (String) -> Unit) {
    GlassCard(modifier = Modifier.clickable { entries.sortedBy { it.date }.firstOrNull()?.let { onOpenPhoto(it.photoUri) } }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Zaman yolculuğu – dokun", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(entries.sortedBy { it.date }) { entry ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AsyncImage(
                            model = entry.photoUri,
                            contentDescription = "Gün ${dayNumbers[entry.id]}",
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                                .clickable { onOpenPhoto(entry.photoUri) },
                            contentScale = ContentScale.Crop,
                        )
                        Text("Gün ${dayNumbers[entry.id] ?: 1}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkinEntryCard(entry: SkinEntry, dayNumber: Int, onOpenPhoto: () -> Unit) {
    GlassCard {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AsyncImage(
                model = entry.photoUri,
                contentDescription = "Gün $dayNumber",
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                    .clickable { onOpenPhoto() },
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gün $dayNumber", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                if (entry.zones.isNotEmpty()) Text("Bölge: ${entry.zones.joinToString()}", color = MaterialTheme.colorScheme.primary)
                if (entry.products.isNotBlank()) Text("Ürün: ${entry.products}", maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                if (entry.notes.isNotBlank()) Text(entry.notes, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), maxLines = 2)
            }
        }
    }
}

@Composable
private fun HomeworkScreen(entries: List<HomeworkEntry>, onDelete: (String) -> Unit) {
    SectionList(
        title = "Ödev panosu",
        subtitle = "Sola kaydırarak veya bekleyerek silebilirsin.",
        header = { HeroBanner("Geri sayım aktif", "Ana sayfada en yakın ödev için bilgilendirme görünür.", Icons.Filled.School) },
    ) {
        items(entries) { entry ->
            DismissibleItem(onDelete = { onDelete(entry.id) }) {
                HomeworkCard(entry)
            }
        }
    }
}

@Composable
private fun HomeworkCard(entry: HomeworkEntry) {
    val days = runCatching { ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(entry.dueDate)) }.getOrDefault(0)
    GlassCard {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(entry.priority.color))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${entry.lesson} – ${entry.title}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Text(if (days >= 0) "Son $days gün" else "Tarih geçti", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
                if (entry.attachment.isNotBlank()) Text("Ek: ${entry.attachment}", color = MaterialTheme.colorScheme.secondary)
            }
            AssistChip(
                onClick = {},
                label = { Text(entry.priority.label) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = entry.priority.color.copy(alpha = 0.18f),
                    labelColor = entry.priority.color,
                ),
            )
        }
    }
}

// endregion

// region Water module

@Composable
private fun WaterScreen(entries: List<WaterEntry>, targetMl: Int, glassSkin: String, onTargetChange: (Int) -> Unit, onDelete: (String) -> Unit) {
    val today = LocalDate.now().toString()
    val todayMl = entries.filter { it.date == today }.sumOf { it.amountMl }
    val progress = (todayMl.toFloat() / targetMl).coerceIn(0f, 1f)

    SectionList(
        title = "Su takip",
        subtitle = "Bardak veya ml seçerek kayıt ekle, hedefini takip et.",
        header = {
            GlassVisual(skinKey = glassSkin, fillRatio = progress, todayMl = todayMl, targetMl = targetMl)
            GlassCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Bugün", fontWeight = FontWeight.Black, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("$todayMl / $targetMl ml", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(99.dp)),
                        color = Color(0xFF38BDF8),
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Günlük hedef", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onTargetChange(targetMl - 250) }) { Text("-250") }
                            Button(onClick = { onTargetChange(targetMl + 250) }) { Text("+250") }
                        }
                    }
                }
            }
            InsightCard("Hatırlatıcı", "Telefonu her elinize aldığınızda 1 bardak su için. Cilt sağlığı için günlük en az 2 litre öneriyoruz.")
        },
    ) {
        items(entries.groupBy { it.date }.toList()) { (date, day) ->
            GlassCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gün: $date", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Toplam ${day.sumOf { it.amountMl }} ml", color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    day.sortedBy { it.time }.forEach { entry ->
                        DismissibleItem(onDelete = { onDelete(entry.id) }) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = Color(0xFF38BDF8))
                                Text("${entry.amountMl} ml", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text(entry.time, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterForm(onAdd: (WaterEntry) -> Unit) {
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var time by rememberSaveable { mutableStateOf(LocalTime.now().withSecond(0).withNano(0).format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var amountMl by rememberSaveable { mutableIntStateOf(250) }
    val presets = listOf(150, 200, 250, 330, 500, 750, 1000)
    FormShell(title = "Su ekle") {
        DateField(date, { date = it })
        TimeField("Saat", time, { time = it })
        Text("Bardak / şişe", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        ChipSelector(presets, amountMl, { amountMl = it }) { "$it ml" }
        OutlinedTextField(
            value = amountMl.toString(),
            onValueChange = { amountMl = it.filter(Char::isDigit).take(4).toIntOrNull() ?: 0 },
            label = { Text("Manuel ml girişi") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Button(
            onClick = { onAdd(WaterEntry(newId(), date, time, amountMl.coerceAtLeast(50))) },
            modifier = Modifier.fillMaxWidth(),
            enabled = date.isValidDate() && time.isValidTime() && amountMl > 0,
        ) { Text("Kaydet") }
    }
}

// endregion

// region Budget module

@Composable
private fun BudgetScreen(data: AppData, onUpdate: (AppData) -> Unit, onOpenVault: (String) -> Unit) {
    val txnsByCategory = data.transactions.filter { !it.category.isIncome }.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }
    val income = data.transactions.filter { it.category.isIncome }.sumOf { it.amount }
    val expense = data.transactions.filter { !it.category.isIncome }.sumOf { it.amount }
    val balance = income - expense
    val limit = data.budgetLimit
    val ratio = if (limit > 0) (expense / limit).coerceIn(0.0, 1.5).toFloat() else 0f
    val overLimit = limit > 0 && expense > limit

    SectionList(
        title = "Bütçe paneli",
        subtitle = "Çoklu kişi (Kasa) desteği, hesaplar ve ay sonu görünümü.",
        header = {
            GlassCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Toplam bakiye", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("${data.currency.symbol}${balance.format()}", fontSize = 34.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        BudgetMetric("Gelir", "${data.currency.symbol}${income.format()}", Color(0xFF22C55E))
                        BudgetMetric("Gider", "${data.currency.symbol}${expense.format()}", Color(0xFFEF4444))
                    }
                    if (limit > 0) {
                        LinearProgressIndicator(
                            progress = { ratio.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(99.dp)),
                            color = if (overLimit) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        )
                        Text(
                            if (overLimit) "Bütçe sınırını aştın!" else "Bütçenin %${(ratio * 100).roundToInt()}'i kullanıldı",
                            color = if (overLimit) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            if (txnsByCategory.isNotEmpty()) BudgetPieCard(txnsByCategory)
            VaultCard(data, onOpenVault)
        },
    ) {
        items(data.transactions.sortedByDescending { it.date }) { txn ->
            DismissibleItem(onDelete = { onUpdate(data.copy(transactions = data.transactions.filterNot { it.id == txn.id })) }) {
                TransactionCard(txn, data.people, data.accounts)
            }
        }
    }
}

@Composable
private fun BudgetMetric(title: String, value: String, color: Color) {
    Column {
        Text(title, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
        Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp, color = color)
    }
}

@Composable
private fun BudgetPieCard(byCategory: Map<TxnCategory, Double>) {
    val total = byCategory.values.sum().takeIf { it > 0 } ?: 1.0
    GlassCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Kategori dağılımı", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 26.dp.toPx()
                        var angle = -90f
                        byCategory.forEach { (category, amount) ->
                            val sweep = (amount / total * 360.0).toFloat()
                            drawArc(
                                color = category.color,
                                startAngle = angle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(stroke / 2, stroke / 2),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(width = stroke, cap = StrokeCap.Butt),
                            )
                            angle += sweep
                        }
                    }
                    Text("${byCategory.size}\nkategori", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    byCategory.entries.sortedByDescending { it.value }.take(5).forEach { (category, amount) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(category.color))
                            Text(category.label, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Text(amount.format(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultCard(data: AppData, onOpenVault: (String) -> Unit) {
    if (data.people.isEmpty()) return
    val perPerson = data.people.map { person ->
        val txns = data.transactions.filter { it.personId == person.id }
        val income = txns.filter { it.category.isIncome }.sumOf { it.amount }
        val expense = txns.filter { !it.category.isIncome }.sumOf { it.amount }
        Triple(person, income, expense)
    }
    GlassCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Kasa görünümü 🔐", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("Bir kişiye dokun: kart limitleri, hesap bakiyeleri ve detayları aç.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            perPerson.forEach { (person, income, expense) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { onOpenVault(person.id) }.padding(8.dp),
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) { Text("✨", fontSize = 20.sp) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(person.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("💰 Gelir ${income.format()}${data.currency.symbol} · 💳 Gider ${expense.format()}${data.currency.symbol}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    val net = income - expense
                    Text(
                        "${if (net >= 0) "+" else ""}${net.format()}${data.currency.symbol}",
                        color = if (net >= 0) Color(0xFF22C55E) else Color(0xFFEF4444),
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(txn: TxnEntry, people: List<BudgetPerson>, accounts: List<BudgetAccount>) {
    val person = people.firstOrNull { it.id == txn.personId }
    val account = accounts.firstOrNull { it.id == txn.accountId }
    GlassCard {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(txn.category.color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(txn.category.icon, contentDescription = null, tint = txn.category.color) }
            Column(modifier = Modifier.weight(1f)) {
                Text(txn.description.ifBlank { txn.category.label }, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    listOfNotNull(person?.name, account?.name, txn.date).joinToString(" • "),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                )
            }
            Text(
                "${if (txn.category.isIncome) "+" else "-"}${txn.amount.format()}${txn.currency.symbol}",
                color = if (txn.category.isIncome) Color(0xFF22C55E) else Color(0xFFEF4444),
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun BudgetForm(data: AppData, onSave: (AppData) -> Unit) {
    val people = remember { mutableStateListOf<BudgetPerson>().apply { addAll(data.people) } }
    val accounts = remember { mutableStateListOf<BudgetAccount>().apply { addAll(data.accounts) } }
    var personName by rememberSaveable { mutableStateOf("") }
    var accountName by rememberSaveable { mutableStateOf("") }
    var accountType by rememberSaveable { mutableStateOf(AccountType.Bank) }
    var selectedPersonId by rememberSaveable { mutableStateOf(people.firstOrNull()?.id ?: "") }
    var selectedAccountId by rememberSaveable { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var category by rememberSaveable { mutableStateOf(TxnCategory.Salary) }
    var amount by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var budgetLimit by rememberSaveable { mutableStateOf(data.budgetLimit.takeIf { it > 0 }?.toString().orEmpty()) }

    // ChipSelector falls back visually to the person's first account, but selectedAccountId can
    // still point at another person's account after a person switch — keep them in sync.
    LaunchedEffect(selectedPersonId, accounts.map { it.id to it.personId }) {
        selectedAccountId = resolveAccountIdForPerson(
            accountIds = accounts.map { it.id },
            accountPersonIds = accounts.map { it.personId },
            personId = selectedPersonId,
            selectedAccountId = selectedAccountId,
        )
    }

    FormShell(title = "Bütçe ekle") {
        Text("Kişi seç (Kasa)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        ChipSelector(people.toList(), people.firstOrNull { it.id == selectedPersonId } ?: people.firstOrNull(), { p -> p?.let { selectedPersonId = it.id } }) { it?.name ?: "" }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(personName, { personName = it }, label = { Text("Yeni kişi") }, modifier = Modifier.weight(1f))
            Button(onClick = {
                if (personName.isNotBlank()) {
                    val p = BudgetPerson(newId(), personName.trim())
                    people += p
                    selectedPersonId = p.id
                    personName = ""
                }
            }) { Text("Ekle") }
        }

        Text("Hesap seç", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        val visibleAccounts = accounts.filter { it.personId == selectedPersonId }
        if (visibleAccounts.isNotEmpty()) {
            ChipSelector(visibleAccounts, visibleAccounts.firstOrNull { it.id == selectedAccountId } ?: visibleAccounts.first(), { a -> selectedAccountId = a.id }) { "${it.type.label}: ${it.name}" }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(accountName, { accountName = it }, label = { Text("Yeni hesap adı") }, modifier = Modifier.weight(1f))
            Button(onClick = {
                if (accountName.isNotBlank() && selectedPersonId.isNotBlank()) {
                    val a = BudgetAccount(newId(), selectedPersonId, accountName.trim(), accountType, "")
                    accounts += a
                    selectedAccountId = a.id
                    accountName = ""
                }
            }) { Text("Ekle") }
        }
        ChipSelector(AccountType.entries, accountType, { accountType = it }) { it.label }

        Text("Kategori", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        ChipSelector(TxnCategory.entries, category, { category = it }) { (if (it.isIncome) "+ " else "- ") + it.label }
        OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.replace(',', '.') }, label = { Text("Tutar") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        OutlinedTextField(description, { description = it }, label = { Text("Açıklama") }, modifier = Modifier.fillMaxWidth())
        DateField(date, { date = it })
        OutlinedTextField(budgetLimit, { budgetLimit = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.replace(',', '.') }, label = { Text("Aylık bütçe sınırı (opsiyonel)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))

        Button(
            onClick = {
                val parsed = amount.toDoubleOrNull()
                val accountId = resolveAccountIdForPerson(
                    accountIds = accounts.map { it.id },
                    accountPersonIds = accounts.map { it.personId },
                    personId = selectedPersonId,
                    selectedAccountId = selectedAccountId,
                )
                val newTxn = if (parsed != null && parsed > 0 && selectedPersonId.isNotBlank()) {
                    TxnEntry(
                        id = newId(),
                        personId = selectedPersonId,
                        accountId = accountId,
                        category = category,
                        amount = parsed,
                        currency = data.currency,
                        description = description.trim(),
                        date = date,
                    )
                } else null
                val newBudgetLimit = budgetLimit.toDoubleOrNull() ?: data.budgetLimit
                onSave(
                    data.copy(
                        people = people.toList(),
                        accounts = accounts.toList(),
                        transactions = if (newTxn != null) data.transactions + newTxn else data.transactions,
                        budgetLimit = newBudgetLimit,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedPersonId.isNotBlank(),
        ) { Text("Kaydet") }
    }
}

private fun monthlyExpense(data: AppData): Double {
    val now = LocalDate.now()
    return data.transactions
        .filter { !it.category.isIncome }
        .filter {
            runCatching { LocalDate.parse(it.date) }.getOrNull()?.let { d ->
                d.month == now.month && d.year == now.year
            } ?: false
        }
        .sumOf { it.amount }
}

// endregion

// region Settings

@Composable
private fun SettingsScreen(
    data: AppData,
    onPaletteChange: (Palette) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onCurrencyChange: (Currency) -> Unit,
    onBackgroundChange: (String) -> Unit,
    onProfileClick: () -> Unit,
    onOpenStore: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    val backgroundOptions = DefaultStoreCatalog.filter { it.category == StoreCategory.Background }
    SectionList(
        title = "Ayarlar",
        subtitle = "Tema, profil, para birimi, mağaza ve yedekleme.",
        header = {
            GlassCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Görünüm", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Tema modu", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 13.sp)
                    ChipSelector(ThemeMode.entries, data.themeMode, onThemeModeChange) { it.label }
                    Text("Renk paleti", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 13.sp)
                    ChipSelector(Palette.entries, data.palette, onPaletteChange) { it.label }
                    Text("Arka plan manzarası", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 13.sp)
                    ChipSelector(backgroundOptions, backgroundOptions.firstOrNull { it.key == data.selectedBackground } ?: backgroundOptions.first(), { onBackgroundChange(it.key) }) {
                        if (it.key in data.purchasedItems) "${it.emoji} ${it.label}" else "🔒 ${it.label} (${it.cost} puan)"
                    }
                }
            }
            GlassCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Profil", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Button(onClick = onProfileClick, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Person, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Profil bilgilerini düzenle")
                    }
                    OutlinedButton(onClick = onOpenStore, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.ShoppingBag, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Mağazayı aç (${data.currentPoints} puan)")
                    }
                }
            }
            GlassCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Bütçe", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Para birimi", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 13.sp)
                    ChipSelector(Currency.entries, data.currency, onCurrencyChange) { "${it.symbol} ${it.label}" }
                }
            }
            GlassCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Yedekleme", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Tüm verileri JSON dosyası olarak dışa aktarabilir veya geri yükleyebilirsin.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onExport, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Dışa aktar")
                        }
                        OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("İçe aktar")
                        }
                    }
                }
            }
        },
    ) {}
}

// endregion

// region Forms (sleep, meal, skin, homework, profile)

@Composable
private fun SleepForm(onAdd: (SleepEntry) -> Unit) {
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var sleptAt by rememberSaveable { mutableStateOf("23:00") }
    var wokeAt by rememberSaveable { mutableStateOf("07:00") }
    FormShell(title = "Uyku ekle") {
        DateField(date, { date = it })
        TimeField("Uyuma saati", sleptAt, { sleptAt = it })
        TimeField("Uyanma saati", wokeAt, { wokeAt = it })
        Button(
            onClick = { onAdd(SleepEntry(newId(), date, sleptAt, wokeAt)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = date.isValidDate() && sleptAt.isValidTime() && wokeAt.isValidTime(),
        ) { Text("Kaydet") }
    }
}

@Composable
private fun MealForm(onAdd: (MealEntry) -> Unit) {
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var mealType by rememberSaveable { mutableStateOf(MealType.Morning) }
    var foods by rememberSaveable { mutableStateOf("Tost\nÇay\nSüt") }
    FormShell(title = "Öğün ekle") {
        DateField(date, { date = it })
        ChipSelector(MealType.entries, mealType, { mealType = it }) { it.label }
        OutlinedTextField(
            value = foods,
            onValueChange = { foods = it },
            label = { Text("Yiyecekler (her satır bir öğe)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Button(
            onClick = { onAdd(MealEntry(newId(), date, mealType, foods.lines().map { it.trim() }.filter { it.isNotBlank() })) },
            modifier = Modifier.fillMaxWidth(),
            enabled = date.isValidDate() && foods.isNotBlank(),
        ) { Text("Kaydet") }
    }
}

@Composable
private fun SkinForm(onAddMany: (List<SkinEntry>) -> Unit) {
    val context = LocalContext.current
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var products by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    val selectedZones = remember { mutableStateListOf<String>() }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val persistUris: (List<Uri>) -> Unit = { uris ->
        uris.forEach { uri ->
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }
    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        persistUris(listOfNotNull(uri)); selectedUris = listOfNotNull(uri)
    }
    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        persistUris(uris); selectedUris = uris
    }
    FormShell(title = "Cilt fotoğrafı ekle") {
        DateField(date, { date = it })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { singlePicker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) { Text("Tek seç") }
            OutlinedButton(onClick = { multiPicker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) { Text("Toplu seç") }
        }
        if (selectedUris.isNotEmpty()) Text("${selectedUris.size} fotoğraf seçildi.", color = MaterialTheme.colorScheme.onSurface)
        Text("Yüz haritası", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        ZoneSelector(selectedZones)
        OutlinedTextField(products, { products = it }, label = { Text("Krem / ilaç / ürün") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notes, { notes = it }, label = { Text("Notlar") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        Button(
            onClick = {
                val entries = selectedUris.mapIndexed { index, uri ->
                    SkinEntry(
                        id = newId(),
                        date = LocalDate.parse(date).minusDays((selectedUris.lastIndex - index).toLong()).toString(),
                        photoUri = uri.toString(),
                        products = products,
                        notes = notes,
                        zones = selectedZones.toSet(),
                    )
                }
                onAddMany(entries)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = date.isValidDate() && selectedUris.isNotEmpty(),
        ) { Text("Arşive ekle") }
    }
}

@Composable
private fun HomeworkForm(onAdd: (HomeworkEntry) -> Unit) {
    var lesson by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var dueDate by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var priority by rememberSaveable { mutableStateOf(Priority.Important) }
    var attachment by rememberSaveable { mutableStateOf("") }
    FormShell(title = "Ödev ekle") {
        OutlinedTextField(lesson, { lesson = it }, label = { Text("Ders") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(title, { title = it }, label = { Text("Ödev adı") }, modifier = Modifier.fillMaxWidth())
        DateField(dueDate, { dueDate = it }, "Teslim tarihi")
        ChipSelector(Priority.entries, priority, { priority = it }) { it.label }
        OutlinedTextField(attachment, { attachment = it }, label = { Text("Dosya/fotoğraf notu veya link") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { onAdd(HomeworkEntry(newId(), lesson, title, dueDate, priority, attachment)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = lesson.isNotBlank() && title.isNotBlank() && dueDate.isValidDate(),
        ) { Text("Kaydet") }
    }
}

@Composable
private fun ProfileForm(profile: Profile, onSave: (Profile) -> Unit) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var age by rememberSaveable { mutableStateOf(profile.age.toString()) }
    var gender by rememberSaveable { mutableStateOf(profile.gender.takeIf { it != "Belirtilmedi" } ?: "Erkek") }
    var photoUri by rememberSaveable { mutableStateOf(profile.photoUri) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            photoUri = it.toString()
        }
    }
    FormShell(title = "Profil") {
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { picker.launch(arrayOf("image/*")) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (photoUri.isNotBlank()) AsyncImage(photoUri, contentDescription = "Profil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(44.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Profil fotoğrafı", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(if (photoUri.isBlank()) "Eklemek için dokun" else "Değiştirmek için dokun", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
                }
            }
        }
        OutlinedTextField(name, { name = it }, label = { Text("İsim") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(age, { age = it.filter(Char::isDigit).take(2) }, label = { Text("Yaş") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Text("Cinsiyet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        ChipSelector(listOf("Erkek", "Kadın"), gender, { gender = it }) { it }
        Button(
            onClick = { onSave(Profile(name.ifBlank { "Kardeşim" }, age.toIntOrNull() ?: 16, gender.ifBlank { "Belirtilmedi" }, photoUri)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Kaydet") }
    }
}

// endregion

// region Shared UI helpers

@Composable
private fun TopProfileBar(profile: Profile, points: Int, onProfile: () -> Unit, onPoints: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(99.dp))
                .clickable(onClick = onPoints)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Filled.ShoppingBag, contentDescription = "Mağaza", tint = Color(0xFFFACC15), modifier = Modifier.size(16.dp))
            Text("$points", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                .clickable(onClick = onProfile),
            contentAlignment = Alignment.Center,
        ) {
            if (profile.photoUri.isNotBlank()) {
                AsyncImage(profile.photoUri, contentDescription = "Profil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Filled.Person, contentDescription = "Profil", tint = Color.White)
            }
        }
    }
}

@Composable
private fun GlassBottomBar(selected: Section, onSelect: (Section) -> Unit, currency: Currency = Currency.TRY) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 0.dp, vertical = 0.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(Section.entries) { item ->
                val active = item == selected
                Column(
                    modifier = Modifier
                        .width(78.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelect(item) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (active) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.30f) else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (item == Section.Budget) {
                            Text(
                                currency.symbol,
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                            )
                        } else {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Text(
                        item.short,
                        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(20.dp, shape, ambientColor = Color.Black.copy(alpha = 0.18f))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (LocalIsDark.current) 0.85f else 1f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), shape),
    ) { content() }
}

@Composable
private fun HeroBanner(title: String, subtitle: String, icon: ImageVector) {
    GlassCard {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun InsightCard(title: String, body: String) {
    GlassCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(body, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun SectionList(
    title: String,
    subtitle: String,
    header: @Composable () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(end = 100.dp)) {
                Text(title, style = androidx.compose.material3.MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.onBackground)
                Text(subtitle, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f))
            }
        }
        item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = { header() }) }
        content()
    }
}

@Composable
private fun DismissibleItem(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val actionWidth = 110.dp
    val actionWidthPx = with(LocalDensity.current) { actionWidth.toPx() }
    var offsetPx by remember { mutableFloatStateOf(0f) }
    val revealFraction = (-offsetPx / actionWidthPx).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxWidth()) {
        if (revealFraction > 0.04f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    modifier = Modifier
                        .height(60.dp)
                        .width(actionWidth)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFFEF4444).copy(alpha = revealFraction))
                        .clickable(enabled = revealFraction > 0.7f) { onDelete() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (revealFraction > 0.45f) {
                        Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Sil", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { offsetPx = if (offsetPx < -actionWidthPx * 0.45f) -actionWidthPx else 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetPx = (offsetPx + dragAmount).coerceIn(-actionWidthPx, 0f)
                        },
                    )
                },
        ) { content() }
    }
}

@Composable
private fun FormShell(title: String, content: @Composable () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text(title, fontWeight = FontWeight.Black, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface) }
        item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = { content() }) }
    }
}

@Composable
private fun DateField(value: String, onChange: (String) -> Unit, label: String = "Tarih") {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("$label (YYYY-AA-GG)") },
        modifier = Modifier.fillMaxWidth(),
        isError = value.isNotBlank() && !value.isValidDate(),
    )
}

@Composable
private fun TimeField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("$label (SS:DD)") },
        modifier = Modifier.fillMaxWidth(),
        isError = value.isNotBlank() && !value.isValidTime(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipSelector(values: List<T>, selected: T?, onSelected: (T) -> Unit, label: (T) -> String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelected(value) },
                label = { Text(label(value)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZoneSelector(selectedZones: MutableList<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Alın", "Çene", "Sol yanak", "Sağ yanak", "Burun").forEach { zone ->
            FilterChip(
                selected = zone in selectedZones,
                onClick = { if (zone in selectedZones) selectedZones.remove(zone) else selectedZones.add(zone) },
                label = { Text(zone) },
            )
        }
    }
}

@Composable
private fun FullScreenPhoto(uri: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black).clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(model = uri, contentDescription = "Orijinal fotoğraf", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
private fun appBackground(palette: Palette): Brush {
    val isDark = LocalIsDark.current
    return if (isDark) {
        Brush.verticalGradient(
            listOf(
                palette.darkPrimary.copy(alpha = 0.10f),
                palette.darkBackground,
                Color.Black,
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                palette.lightPrimary.copy(alpha = 0.08f),
                palette.lightBackground,
                palette.lightSurface,
            ),
        )
    }
}

@Composable
private fun BowlVisual(skinKey: String, fillCount: Int) {
    val item = DefaultStoreCatalog.firstOrNull { it.key == skinKey } ?: DefaultStoreCatalog.first { it.category == StoreCategory.MealSkin && it.isDefault }
    val ratio = (fillCount / 4f).coerceIn(0f, 1f)
    GlassCard {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(ratio.coerceAtLeast(0.05f)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                )
                Text(item.emoji, fontSize = 56.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.label, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Text("Bugün $fillCount öğün eklendi", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), fontSize = 13.sp)
                Text("Mağazadan farklı kâseler açabilirsin.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun GlassVisual(skinKey: String, fillRatio: Float, todayMl: Int, targetMl: Int) {
    val item = DefaultStoreCatalog.firstOrNull { it.key == skinKey } ?: DefaultStoreCatalog.first { it.category == StoreCategory.WaterSkin && it.isDefault }
    GlassCard {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFF38BDF8).copy(alpha = 0.18f)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(fillRatio.coerceAtLeast(0.05f)).background(Color(0xFF38BDF8).copy(alpha = 0.45f)),
                )
                Text(item.emoji, fontSize = 56.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.label, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Text("$todayMl / $targetMl ml", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Mağazadan kristal/mat şişe gibi skinler açılır.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun EditableDismissibleItem(onDelete: () -> Unit, onEdit: () -> Unit, content: @Composable () -> Unit) {
    val actionWidth = 120.dp
    val actionWidthPx = with(LocalDensity.current) { actionWidth.toPx() }
    var offsetPx by remember { mutableFloatStateOf(0f) }
    val leftReveal = (-offsetPx / actionWidthPx).coerceIn(0f, 1f)
    val rightReveal = (offsetPx / actionWidthPx).coerceIn(0f, 1f)
    Box(modifier = Modifier.fillMaxWidth()) {
        if (leftReveal > 0.04f) {
            Box(modifier = Modifier.matchParentSize().padding(horizontal = 4.dp), contentAlignment = Alignment.CenterEnd) {
                Row(
                    modifier = Modifier
                        .height(60.dp).width(actionWidth)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFFEF4444).copy(alpha = leftReveal))
                        .clickable(enabled = leftReveal > 0.7f) { onDelete() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (leftReveal > 0.45f) {
                        Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Sil", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        if (rightReveal > 0.04f) {
            Box(modifier = Modifier.matchParentSize().padding(horizontal = 4.dp), contentAlignment = Alignment.CenterStart) {
                Row(
                    modifier = Modifier
                        .height(60.dp).width(actionWidth)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF2563EB).copy(alpha = rightReveal))
                        .clickable(enabled = rightReveal > 0.7f) { onEdit(); offsetPx = 0f },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (rightReveal > 0.45f) {
                        Icon(Icons.Filled.Edit, contentDescription = "Düzenle", tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Düzenle", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            offsetPx = when {
                                offsetPx < -actionWidthPx * 0.45f -> -actionWidthPx
                                offsetPx > actionWidthPx * 0.45f -> actionWidthPx
                                else -> 0f
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetPx = (offsetPx + dragAmount).coerceIn(-actionWidthPx, actionWidthPx)
                        },
                    )
                },
        ) { content() }
    }
}

@Composable
private fun SleepEditForm(initial: SleepEntry, onSave: (SleepEntry) -> Unit) {
    // Key by entry id so reopening the sheet for another row does not restore the previous edit.
    var date by rememberSaveable(initial.id) { mutableStateOf(initial.date) }
    var sleptAt by rememberSaveable(initial.id) { mutableStateOf(initial.sleptAt) }
    var wokeAt by rememberSaveable(initial.id) { mutableStateOf(initial.wokeAt) }
    FormShell(title = "Uyku düzenle ✏️") {
        DateField(date, { date = it })
        TimeField("Uyuma saati", sleptAt, { sleptAt = it })
        TimeField("Uyanma saati", wokeAt, { wokeAt = it })
        Button(
            onClick = { onSave(initial.copy(date = date, sleptAt = sleptAt, wokeAt = wokeAt)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = date.isValidDate() && sleptAt.isValidTime() && wokeAt.isValidTime(),
        ) { Text("Güncelle") }
    }
}

@Composable
private fun StoreScreen(data: AppData, onPurchase: (StoreItem) -> Unit, onSelect: (StoreItem) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Mağaza 🛍️", fontWeight = FontWeight.Black, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
            Surface(shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)) {
                Text("${data.currentPoints} puan", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            }
        }
        Text("Görevleri tamamlayarak puan kazan, kostümleri ve seri dondurma haklarını mağazadan al.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), fontSize = 13.sp)
        StoreCategory.entries.forEach { category ->
            val items = DefaultStoreCatalog.filter { it.category == category }
            Text(category.label, fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items) { item ->
                    val owned = item.key in data.purchasedItems || item.isDefault
                    val isSelected = when (item.category) {
                        StoreCategory.Pet -> data.selectedPet == item.key
                        StoreCategory.Tree -> data.selectedTree == item.key
                        StoreCategory.MealSkin -> data.selectedBowl == item.key
                        StoreCategory.WaterSkin -> data.selectedGlass == item.key
                        StoreCategory.Background -> data.selectedBackground == item.key
                        StoreCategory.Freeze -> false
                    }
                    StoreItemCard(
                        item = item,
                        owned = owned,
                        selected = isSelected,
                        onAction = {
                            if (!owned) onPurchase(item) else if (item.category != StoreCategory.Freeze) onSelect(item) else onPurchase(item)
                        },
                    )
                }
            }
        }
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Aktif seri dondurma hakkı: ${data.freezePassCount} 🧊", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Görev kaçırırsan otomatik kullanılır; alev yerine buz görseli görünür.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StoreItemCard(item: StoreItem, owned: Boolean, selected: Boolean, onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (LocalIsDark.current) 0.5f else 1f))
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                RoundedCornerShape(22.dp),
            )
            .clickable(onClick = onAction)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(item.emoji, fontSize = 34.sp)
        Text(item.label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, fontSize = 13.sp)
        if (item.cost == 0 && item.isDefault) {
            Text("Ücretsiz", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), fontSize = 11.sp)
        } else {
            Text("${item.cost} puan", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else if (owned) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        ) {
            Text(
                when {
                    selected -> "Aktif"
                    owned && item.category == StoreCategory.Freeze -> "+1 al"
                    owned -> "Seç"
                    else -> "Satın Al"
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = if (selected || owned) Color.Black else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ForestScreen(data: AppData, onComplete: (TreeEntry) -> Unit, onSelectTree: (String) -> Unit) {
    val treeOptions = DefaultStoreCatalog.filter { it.category == StoreCategory.Tree && (it.key in data.purchasedItems || it.isDefault) }
    val activeTree = treeOptions.firstOrNull { it.key == data.selectedTree } ?: treeOptions.first()
    val durations = listOf(15, 25, 45, 60)
    var duration by rememberSaveable { mutableIntStateOf(25) }
    var running by remember { mutableStateOf(false) }
    var remainingSec by remember { mutableIntStateOf(duration * 60) }

    LaunchedEffect(running, duration) {
        if (running) {
            remainingSec = duration * 60
            while (running && remainingSec > 0) {
                kotlinx.coroutines.delay(1000)
                remainingSec -= 1
            }
            if (running && remainingSec <= 0) {
                running = false
                onComplete(TreeEntry(newId(), LocalDate.now().toString(), duration, activeTree.key))
            }
        }
    }

    val totalSec = duration * 60
    val growth = if (running) 1f - (remainingSec.toFloat() / totalSec.coerceAtLeast(1)) else 0f
    SectionList(
        title = "Orman 🌳",
        subtitle = "Odaklan, fidanın büyüsün, puan kazan ve daha nadir tohumlar al.",
        header = {
            GlassCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(activeTree.emoji, fontSize = (24 + 60 * growth.coerceIn(0f, 1f)).sp)
                    Text(if (running) "${remainingSec / 60} dk ${remainingSec % 60} sn" else "Hazır", fontWeight = FontWeight.Black, fontSize = 32.sp, color = MaterialTheme.colorScheme.onSurface)
                    LinearProgressIndicator(
                        progress = { growth.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(99.dp)),
                        color = Color(0xFF22C55E),
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    )
                    ChipSelector(durations, duration, { duration = it }) { "$it dk" }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { running = !running; if (!running) remainingSec = duration * 60 }, modifier = Modifier.weight(1f)) {
                            Icon(if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (running) "Vazgeç" else "Başlat")
                        }
                        OutlinedButton(onClick = { running = false; remainingSec = duration * 60 }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.PauseCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Sıfırla")
                        }
                    }
                }
            }
            GlassCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Aktif tohum", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(treeOptions) { tree ->
                            val active = tree.key == activeTree.key
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .border(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), RoundedCornerShape(99.dp))
                                    .clickable { onSelectTree(tree.key) },
                            ) {
                                Text(
                                    "${tree.emoji} ${tree.label}",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    Text("Daha fazla tohum için mağazaya bak.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
            if (data.treeEntries.isNotEmpty()) {
                GlassCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Sanal Ormanın 🌲", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        FlowRowTrees(entries = data.treeEntries.takeLast(40))
                        Text("Toplam ${data.treeEntries.size} ağaç · ${data.treeEntries.sumOf { it.durationMinutes }} dk odak.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
            }
        },
    ) {}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowTrees(entries: List<TreeEntry>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        entries.forEach { entry ->
            val tree = DefaultStoreCatalog.firstOrNull { it.key == entry.treeKey } ?: DefaultStoreCatalog.first { it.category == StoreCategory.Tree }
            Text(tree.emoji, fontSize = 28.sp)
        }
    }
}

@Composable
private fun AllHistoryScreen(data: AppData, initialFilter: String?, onBack: () -> Unit) {
    val categories = listOf("Hepsi", "Cilt", "Su", "Uyku", "Öğün", "Bütçe", "Ödev")
    var selected by rememberSaveable { mutableStateOf(initialFilter ?: "Hepsi") }
    val items = recentUpdates(data)
    val filtered = items.filter {
        when (selected) {
            "Hepsi" -> true
            "Cilt" -> it.title.contains("Cilt") || it.title.contains("fotoğraf")
            "Su" -> it.title.contains("ml")
            "Uyku" -> it.title.contains("Uyku")
            "Öğün" -> it.title.contains("Sabah") || it.title.contains("Öğle") || it.title.contains("Akşam") || it.title.contains("Ara Öğün")
            "Bütçe" -> it.title.contains("Maaş") || it.title.contains("Kira") || it.title.contains("Elektrik") || it.title.contains("Su Faturası") || it.title.contains("Doğalgaz") || it.title.contains("Market") || it.title.contains("Abonelik") || it.title.contains("Diğer")
            "Ödev" -> it.subtitle.contains("Teslim:")
            else -> true
        }
    }
    SectionList(
        title = "Geçmiş",
        subtitle = "Tüm modüllerden gelen kayıtları kategoriye göre filtrele.",
        header = {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val active = cat == selected
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .border(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), RoundedCornerShape(99.dp))
                            .clickable { selected = cat },
                    ) {
                        Text(
                            cat,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        },
    ) {
        items(filtered) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(item.tint.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(item.icon, contentDescription = null, tint = item.tint) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(item.subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun VaultDetailScreen(data: AppData, person: BudgetPerson) {
    val txns = data.transactions.filter { it.personId == person.id }
    val income = txns.filter { it.category.isIncome }.sumOf { it.amount }
    val expense = txns.filter { !it.category.isIncome }.sumOf { it.amount }
    val cards = data.accounts.filter { it.personId == person.id && it.type == AccountType.Card }
    val banks = data.accounts.filter { it.personId == person.id && it.type == AccountType.Bank }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Text("✨", fontSize = 24.sp) }
            Column(modifier = Modifier.weight(1f)) {
                Text(person.name, fontWeight = FontWeight.Black, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Net ${(income - expense).format()}${data.currency.symbol}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
        Text("Kredi Kartları 💳", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        if (cards.isEmpty()) {
            Text("Bu kişide kredi kartı yok.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        } else {
            cards.forEach { card ->
                val remaining = (card.creditLimit - card.balance).coerceAtLeast(0.0)
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Filled.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(card.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Limit ${card.creditLimit.format()} · Kalan ${remaining.format()}${data.currency.symbol}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text("Bu ay borç ${card.dueAmount.format()}${data.currency.symbol}", fontSize = 12.sp, color = Color(0xFFEF4444))
                    }
                }
            }
        }
        Text("Banka Hesapları 🏦", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        if (banks.isEmpty()) {
            Text("Bu kişide banka hesabı yok.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        } else {
            banks.forEach { bank ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(bank.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Bakiye ${bank.balance.format()}${data.currency.symbol}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

// endregion

// region Helpers

private data class SleepReport(val title: String, val detail: String, val color: Color)
private data class SleepTarget(val recommended: Double, val rangeText: String)

private fun sleepTarget(age: Int): SleepTarget = when (age) {
    in 0..5 -> SleepTarget(11.0, "10-13 saat")
    in 6..12 -> SleepTarget(10.0, "9-12 saat")
    in 13..18 -> SleepTarget(9.0, "8-10 saat")
    in 19..64 -> SleepTarget(8.0, "7-9 saat")
    else -> SleepTarget(7.5, "7-9 saat")
}

private fun sleepStatus(hours: Double, age: Int): SleepReport {
    val target = sleepTarget(age)
    return when {
        hours <= 0.0 -> SleepReport("Veri yok", "Uyku kaydı eklenince yaşa göre otomatik yorumlanacak.", Color(0xFF9CA3AF))
        hours + 0.25 < target.recommended -> SleepReport("Yetersiz", "$age yaş için önerilen aralık ${target.rangeText}. Ortalama biraz düşük.", Color(0xFFEF4444))
        hours > target.recommended + 2 -> SleepReport("Fazla", "$age yaş için önerilen aralık ${target.rangeText}. Uyku süresi uzun görünüyor.", Color(0xFFF59E0B))
        else -> SleepReport("Yeterli", "$age yaş için önerilen aralık ${target.rangeText}. Uyku süresi iyi görünüyor.", Color(0xFF22C55E))
    }
}

private fun mealSuggestion(age: Int): String = when (age) {
    in 0..5 -> "Protein, yoğurt/süt, yumurta, meyve ve sebze ağırlıklı minik porsiyonlar iyi olur."
    in 6..12 -> "Kahvaltıda yumurta/peynir, öğlen protein + tahıl, akşam sebze + yoğurt dengesi önerilir."
    in 13..18 -> "Ergenlik dönemi için protein, kompleks karbonhidrat, yeşillik, su ve şekeri azaltma cilt için önemli."
    else -> "Protein, lifli sebze, tam tahıl ve yeterli su dengesi takip edilmeli."
}

private fun timeBasedGreeting(): Pair<String, String> {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 5..11 -> "Günaydın," to "Bugünü güzel başlat."
        in 12..17 -> "Merhaba," to "Günün ortasında bir mola."
        in 18..22 -> "İyi akşamlar," to "Bugünü kapatmaya hazır mısın?"
        else -> "İyi geceler," to "Sakin bir gece olsun."
    }
}

private fun computeStreak(dates: Iterable<String>): Int {
    val parsedDates = dates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toHashSet()
    if (parsedDates.isEmpty()) return 0
    var current = LocalDate.now()
    if (!parsedDates.contains(current)) current = current.minusDays(1)
    var streak = 0
    while (parsedDates.contains(current)) {
        streak += 1
        current = current.minusDays(1)
    }
    return streak
}

private fun SleepEntry.duration(): Double? {
    val start = sleptAt.parseTimeOrNull() ?: return null
    val end = wokeAt.parseTimeOrNull() ?: return null
    val raw = Duration.between(start, end).toMinutes()
    val minutes = if (raw <= 0) raw + Duration.ofDays(1).toMinutes() else raw
    return minutes / 60.0
}

private fun String.parseTimeOrNull(): LocalTime? = try {
    LocalTime.parse(this, DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT))
} catch (_: DateTimeParseException) { null }

private fun String.isValidTime(): Boolean = parseTimeOrNull() != null
private fun String.isValidDate(): Boolean = runCatching { LocalDate.parse(this) }.isSuccess
private fun Double?.orZero(): Double = this ?: 0.0
private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()
private fun Double.oneDecimal(): String = ((this * 10).roundToInt() / 10.0).toString()
private fun Double.format(): String = String.format(Locale("tr"), "%,.0f", this)

private inline fun <reified T : Enum<T>> enumValueOfOrDefault(name: String, default: T): T =
    runCatching { enumValueOf<T>(name) }.getOrDefault(default)

private fun JSONArray?.mapStrings(): List<String> = buildList {
    val arr = this@mapStrings ?: return@buildList
    for (i in 0 until arr.length()) add(arr.optString(i))
}

private fun <T> JSONArray?.mapJsonObjects(transform: (JSONObject) -> T): List<T> = buildList {
    val arr = this@mapJsonObjects ?: return@buildList
    for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(transform(it)) }
}

private fun newId(): String = UUID.randomUUID().toString()

// endregion

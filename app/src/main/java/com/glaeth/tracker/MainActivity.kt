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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
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
    Dashboard("Ana Sayfa", Icons.Filled.Home, "Ana"),
    Sleep("Uyku", Icons.Filled.DateRange, "Uyku"),
    Meals("Öğünler", Icons.Filled.LocalDining, "Öğün"),
    Water("Su", Icons.Filled.WaterDrop, "Su"),
    Skin("Cilt", Icons.Filled.Face, "Cilt"),
    Homework("Ödev", Icons.Filled.School, "Ödev"),
    Budget("Bütçe", Icons.Filled.AttachMoney, "Bütçe"),
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
    TRY("Türk Lirası", "₺"), USD("Dolar", "$"), EUR("Euro", "€")
}

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
    val people: List<BudgetPerson>,
    val accounts: List<BudgetAccount>,
    val transactions: List<TxnEntry>,
    val waterTargetMl: Int,
    val budgetLimit: Double,
    val currency: Currency,
    val profile: Profile,
    val palette: Palette,
    val themeMode: ThemeMode,
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
            BudgetAccount(it.optString("id", newId()), it.optString("personId"), it.optString("name", "Hesap"), enumValueOfOrDefault(it.optString("type"), AccountType.Bank), it.optString("iconKey"))
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
        waterTargetMl = root.optInt("waterTargetMl", 2500),
        budgetLimit = root.optDouble("budgetLimit", 0.0),
        currency = enumValueOfOrDefault(root.optString("currency"), Currency.TRY),
        profile = root.optJSONObject("profile")?.let {
            Profile(it.optString("name", "Kardeşim"), it.optInt("age", 16), it.optString("gender", "Belirtilmedi"), it.optString("photoUri"))
        } ?: Profile(),
        palette = enumValueOfOrDefault(root.optString("palette"), Palette.Obsidian),
        themeMode = enumValueOfOrDefault(root.optString("themeMode"), ThemeMode.System),
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
        .put("accounts", JSONArray().apply { data.accounts.forEach { put(JSONObject().put("id", it.id).put("personId", it.personId).put("name", it.name).put("type", it.type.name).put("iconKey", it.iconKey)) } })
        .put("transactions", JSONArray().apply { data.transactions.forEach { put(JSONObject().put("id", it.id).put("personId", it.personId).put("accountId", it.accountId).put("category", it.category.name).put("amount", it.amount).put("currency", it.currency.name).put("description", it.description).put("date", it.date)) } })
}

private fun demoData(): AppData {
    val today = LocalDate.now()
    val ben = BudgetPerson(newId(), "Ben")
    val account = BudgetAccount(newId(), ben.id, "Ana Hesap", AccountType.Bank, "")
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
        people = listOf(ben),
        accounts = listOf(account),
        transactions = listOf(
            TxnEntry(newId(), ben.id, account.id, TxnCategory.Salary, 25000.0, Currency.TRY, "Maaş", today.toString()),
            TxnEntry(newId(), ben.id, account.id, TxnCategory.Rent, 8000.0, Currency.TRY, "Ev kirası", today.minusDays(2).toString()),
            TxnEntry(newId(), ben.id, account.id, TxnCategory.Food, 1450.0, Currency.TRY, "Market", today.minusDays(1).toString()),
            TxnEntry(newId(), ben.id, account.id, TxnCategory.Electricity, 740.0, Currency.TRY, "Elektrik faturası", today.minusDays(3).toString()),
        ),
        waterTargetMl = 2500,
        budgetLimit = 12000.0,
        currency = Currency.TRY,
        profile = Profile(),
        palette = Palette.Obsidian,
        themeMode = ThemeMode.Dark,
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
        bottomBar = { GlassBottomBar(selected = section, onSelect = { section = it }) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(appBackground(data.palette)).padding(padding)) {
            AnimatedContent(targetState = section, label = "section") { target ->
                when (target) {
                    Section.Dashboard -> DashboardScreen(
                        data = data,
                        onOpen = { section = it },
                        onProfile = { profileSheet = true },
                    )
                    Section.Sleep -> SleepScreen(data.sleepEntries, data.profile) { id ->
                        updateData(data.copy(sleepEntries = data.sleepEntries.filterNot { it.id == id }))
                    }
                    Section.Meals -> MealScreen(data.mealEntries, data.profile) { id ->
                        updateData(data.copy(mealEntries = data.mealEntries.filterNot { it.id == id }))
                    }
                    Section.Water -> WaterScreen(
                        entries = data.waterEntries,
                        targetMl = data.waterTargetMl,
                        onTargetChange = { updateData(data.copy(waterTargetMl = it.coerceIn(500, 5000))) },
                        onDelete = { id -> updateData(data.copy(waterEntries = data.waterEntries.filterNot { it.id == id })) },
                    )
                    Section.Skin -> SkinScreen(
                        entries = data.skinEntries,
                        onDelete = { id -> updateData(data.copy(skinEntries = data.skinEntries.filterNot { it.id == id })) },
                        onOpenPhoto = { fullImage = it },
                    )
                    Section.Homework -> HomeworkScreen(data.homeworkEntries) { id ->
                        updateData(data.copy(homeworkEntries = data.homeworkEntries.filterNot { it.id == id }))
                    }
                    Section.Budget -> BudgetScreen(
                        data = data,
                        onUpdate = updateData,
                    )
                    Section.Settings -> SettingsScreen(
                        data = data,
                        onPaletteChange = { updateData(data.copy(palette = it)) },
                        onThemeModeChange = { updateData(data.copy(themeMode = it)) },
                        onCurrencyChange = { updateData(data.copy(currency = it)) },
                        onProfileClick = { profileSheet = true },
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
                onProfile = { profileSheet = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 12.dp, end = 16.dp),
            )
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
                Section.Dashboard, Section.Settings -> Unit
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
private fun DashboardScreen(data: AppData, onOpen: (Section) -> Unit, onProfile: () -> Unit) {
    val skinStreak = computeStreak(data.skinEntries.map { it.date })
    val waterStreak = computeStreak(data.waterEntries.map { it.date }.distinct())
    val mealStreak = computeStreak(data.mealEntries.map { it.date }.distinct())
    val today = LocalDate.now().toString()
    val todayWaterMl = data.waterEntries.filter { it.date == today }.sumOf { it.amountMl }
    val recent = recentUpdates(data).take(6)
    val pages = remember(data) { dashboardPages(data) }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            ScenicHeader(
                profileName = data.profile.name.ifBlank { "Glaeth" },
                pages = pages,
                pagerState = pagerState,
                onAction = onOpen,
            )
            BodyArea(
                skinStreak = skinStreak,
                waterStreak = waterStreak,
                mealStreak = mealStreak,
                todayWaterMl = todayWaterMl,
                waterTarget = data.waterTargetMl,
                updates = recent,
                onOpen = onOpen,
            )
            Spacer(Modifier.height(120.dp))
        }
    }
}

private data class DashboardPage(
    val title: String,
    val subtitle: String,
    val score: Int,
    val maxScore: Int,
    val accent: Color,
    val updateLabel: String,
    val ctaLabel: String,
    val targetSection: Section,
)

private fun dashboardPages(data: AppData): List<DashboardPage> {
    val today = LocalDate.now().toString()
    val skinDays = data.skinEntries.size
    val skinScore = skinDays.coerceAtMost(1000)
    val skinUpdate = data.skinEntries.maxByOrNull { it.date }?.date?.let { "Son fotoğraf $it" } ?: "Henüz fotoğraf yok"
    val todayMl = data.waterEntries.filter { it.date == today }.sumOf { it.amountMl }
    val waterScore = ((todayMl.toFloat() / data.waterTargetMl.coerceAtLeast(1)) * 1000).toInt().coerceIn(0, 1000)
    val waterUpdate = "$todayMl / ${data.waterTargetMl} ml bugün"
    val expense = monthlyExpense(data)
    val income = data.transactions.filter { it.category.isIncome }.sumOf { it.amount }
    val budgetMax = if (data.budgetLimit > 0) data.budgetLimit else (income.coerceAtLeast(1.0))
    val ratio = (1.0 - (expense / budgetMax).coerceIn(0.0, 1.0)).coerceIn(0.0, 1.0)
    val budgetScore = (ratio * 1000).toInt()
    val budgetUpdate = "${data.currency.symbol}${expense.format()} bu ay gider"

    return listOf(
        DashboardPage(
            title = "Cilt Skoru",
            subtitle = "Eklenen yüz fotoğrafı",
            score = skinScore,
            maxScore = 1000,
            accent = Color(0xFFFB923C),
            updateLabel = skinUpdate,
            ctaLabel = "Detayları Gör",
            targetSection = Section.Skin,
        ),
        DashboardPage(
            title = "Su Hedefi",
            subtitle = "Günlük hedefe ilerleme",
            score = waterScore,
            maxScore = 1000,
            accent = Color(0xFF38BDF8),
            updateLabel = waterUpdate,
            ctaLabel = "Su Geçmişi",
            targetSection = Section.Water,
        ),
        DashboardPage(
            title = "Bütçe Sağlığı",
            subtitle = if (data.budgetLimit > 0) "Aylık limit kullanımı" else "Aylık gider/gelir oranı",
            score = budgetScore,
            maxScore = 1000,
            accent = Color(0xFF14B8A6),
            updateLabel = budgetUpdate,
            ctaLabel = "Bütçeye Git",
            targetSection = Section.Budget,
        ),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScenicHeader(
    profileName: String,
    pages: List<DashboardPage>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onAction: (Section) -> Unit,
) {
    val isDark = LocalIsDark.current
    val skyColors = if (isDark) {
        listOf(
            Color(0xFF1E1B4B),
            Color(0xFF312E81),
            Color(0xFF155E75),
            Color(0xFF0F3D45),
        )
    } else {
        listOf(
            Color(0xFF7C3AED),
            Color(0xFFEC4899),
            Color(0xFFF97316),
            Color(0xFF14B8A6),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(610.dp)
            .background(Brush.verticalGradient(skyColors)),
    ) {
        // Subtle wave layer at the bottom for ocean feeling
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFF0F4C5C).copy(alpha = 0.55f),
                            Color(0xFF0E2939).copy(alpha = 0.85f),
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
                    Text(
                        greet.first,
                        color = Color.White.copy(alpha = 0.92f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    )
                    Text(
                        profileName,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 38.sp,
                    )
                }
                Spacer(Modifier.width(80.dp)) // reserved for the global TopProfileBar overlay
            }
            Spacer(Modifier.height(8.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 36.dp),
                pageSpacing = 12.dp,
            ) { page ->
                ScorePageCard(page = pages[page], onAction = { onAction(pages[page].targetSection) })
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pages.size) { index ->
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

@Composable
private fun ScorePageCard(page: DashboardPage, onAction: () -> Unit) {
    val animated by animateFloatAsState(
        targetValue = (page.score.toFloat() / page.maxScore.coerceAtLeast(1)).coerceIn(0f, 1f),
        label = "scoreArc",
    )
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
                .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(296.dp)) {
                val stroke = 14.dp.toPx()
                drawArc(
                    color = Color.White.copy(alpha = 0.18f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = page.accent,
                    startAngle = 135f,
                    sweepAngle = 270f * animated,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Color.Black.copy(alpha = 0.45f),
                ) {
                    Text(
                        page.updateLabel,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                    )
                }
                Text(
                    "${page.score}",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 64.sp,
                )
                Text(
                    "${page.title} · ${page.maxScore} üzerinden",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = page.accent.copy(alpha = 0.92f),
                    modifier = Modifier.clickable(onClick = onAction),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text(page.ctaLabel, color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun BodyArea(
    skinStreak: Int,
    waterStreak: Int,
    mealStreak: Int,
    todayWaterMl: Int,
    waterTarget: Int,
    updates: List<FeedItem>,
    onOpen: (Section) -> Unit,
) {
    val shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-32).dp)
            .clip(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(Alignment.CenterHorizontally)
                    .width(48.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)),
            )
            // Streak strip
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            ) {
                StreakChip("Cilt", skinStreak, Color(0xFFFB923C))
                StreakChip("Su", waterStreak, Color(0xFF38BDF8))
                StreakChip("Öğün", mealStreak, Color(0xFF22C55E))
            }
            // Latest updates
            LatestUpdatesSection(updates = updates)
            // Quick actions
            QuickActions(onOpen)
            // Bottom water mini-card
            BodyWaterPill(todayWaterMl, waterTarget, onOpen)
        }
    }
}

@Composable
private fun StreakChip(label: String, days: Int, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(99.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text("$days", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontSize = 12.sp)
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
private fun SleepScreen(entries: List<SleepEntry>, profile: Profile, onDelete: (String) -> Unit) {
    val average = entries.mapNotNull { it.duration() }.averageOrZero()
    SectionList(
        title = "Uyku günlüğü",
        subtitle = "Yatma, uyanma saati ve toplam süreyi takip et.",
        header = {
            SleepChart(entries, profile.age)
            InsightCard("Otomatik karar", sleepStatus(average, profile.age).detail)
        },
    ) {
        items(entries) { entry ->
            DismissibleItem(onDelete = { onDelete(entry.id) }) {
                SleepEntryCard(entry = entry, age = profile.age)
            }
        }
    }
}

@Composable
private fun SleepChart(entries: List<SleepEntry>, age: Int) {
    val target = sleepTarget(age).recommended
    GlassCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Detaylı uyku grafiği", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Hedef ${target.oneDecimal()}s", color = MaterialTheme.colorScheme.primary)
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(168.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                entries.takeLast(7).forEach { entry ->
                    val hours = entry.duration() ?: 0.0
                    val enough = hours >= target
                    val fraction = (hours / (target + 2)).coerceIn(0.08, 1.0).toFloat()
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(99.dp))
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
private fun MealScreen(entries: List<MealEntry>, profile: Profile, onDelete: (String) -> Unit) {
    SectionList(
        title = "Öğünler",
        subtitle = "Yaşa göre besin önerileri ve günlük öğün kayıtları.",
        header = { InsightCard("Besin önerisi", mealSuggestion(profile.age)) },
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
private fun WaterScreen(entries: List<WaterEntry>, targetMl: Int, onTargetChange: (Int) -> Unit, onDelete: (String) -> Unit) {
    val today = LocalDate.now().toString()
    val todayMl = entries.filter { it.date == today }.sumOf { it.amountMl }
    val progress = (todayMl.toFloat() / targetMl).coerceIn(0f, 1f)

    SectionList(
        title = "Su takip",
        subtitle = "Bardak veya ml seçerek kayıt ekle, hedefini takip et.",
        header = {
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
private fun BudgetScreen(data: AppData, onUpdate: (AppData) -> Unit) {
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
            VaultCard(data)
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
private fun VaultCard(data: AppData) {
    if (data.people.isEmpty()) return
    val perPerson = data.people.map { person ->
        val txns = data.transactions.filter { it.personId == person.id }
        val income = txns.filter { it.category.isIncome }.sumOf { it.amount }
        val expense = txns.filter { !it.category.isIncome }.sumOf { it.amount }
        Triple(person, income, expense)
    }
    GlassCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Kasa görünümü", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            perPerson.forEach { (person, income, expense) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(person.name.firstOrNull()?.toString() ?: "?", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(person.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Gelir ${income.format()} • Gider ${expense.format()}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
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
                val newTxn = if (parsed != null && parsed > 0 && selectedPersonId.isNotBlank()) {
                    TxnEntry(
                        id = newId(),
                        personId = selectedPersonId,
                        accountId = selectedAccountId,
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
    onProfileClick: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    SectionList(
        title = "Ayarlar",
        subtitle = "Tema, profil, para birimi ve yedekleme.",
        header = {
            GlassCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Görünüm", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Tema modu", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 13.sp)
                    ChipSelector(ThemeMode.entries, data.themeMode, onThemeModeChange) { it.label }
                    Text("Renk paleti", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 13.sp)
                    ChipSelector(Palette.entries, data.palette, onPaletteChange) { it.label }
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
private fun TopProfileBar(profile: Profile, onProfile: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = if (LocalIsDark.current) 0.85f else 1f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), CircleShape)
                .clickable(onClick = onProfile),
            contentAlignment = Alignment.Center,
        ) {
            if (profile.photoUri.isNotBlank()) {
                AsyncImage(profile.photoUri, contentDescription = "Profil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Filled.Person, contentDescription = "Profil", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = if (LocalIsDark.current) 0.85f else 1f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }
    }
}

@Composable
private fun GlassBottomBar(selected: Section, onSelect: (Section) -> Unit) {
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
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                            modifier = Modifier.size(20.dp),
                        )
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

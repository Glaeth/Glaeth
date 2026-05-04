package com.glaeth.tracker

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GlaethRoot() }
    }
}

private enum class Section(val label: String, val icon: ImageVector, val actionLabel: String) {
    Dashboard("Ana", Icons.Filled.Home, ""),
    Sleep("Uyku", Icons.Filled.DateRange, "Uyku ekle"),
    Meals("Öğün", Icons.Filled.Favorite, "Öğün ekle"),
    Skin("Cilt", Icons.Filled.Face, "Fotoğraf ekle"),
    Homework("Ödev", Icons.Filled.DateRange, "Ödev ekle"),
    Settings("Ayar", Icons.Filled.Settings, ""),
}

private enum class MealType(val label: String) {
    Morning("Sabah"),
    Lunch("Öğle"),
    Evening("Akşam"),
    Snack("Ara Öğün"),
}

private enum class Priority(val label: String, val color: Color) {
    Urgent("Acil", Color(0xFFFF5C5C)),
    Important("Önemli", Color(0xFFFFB84D)),
    Chill("Keyfi", Color(0xFF58D68D)),
}

private enum class BackgroundStyle(val label: String, val top: Color, val bottom: Color, val accent: Color) {
    Black("Siyah", Color(0xFF020204), Color(0xFF101014), Color(0xFF9CA3AF)),
    Graphite("Grafit", Color(0xFF07090D), Color(0xFF1D1F27), Color(0xFFB8BDC7)),
    Midnight("Lacivert", Color(0xFF020615), Color(0xFF111827), Color(0xFF7DD3FC)),
}

private data class Profile(
    val name: String = "Kardeşim",
    val age: Int = 16,
    val gender: String = "Belirtilmedi",
    val photoUri: String = "",
)

private data class SleepEntry(
    val id: String,
    val date: String,
    val sleptAt: String,
    val wokeAt: String,
)

private data class MealEntry(
    val id: String,
    val date: String,
    val type: MealType,
    val foods: List<String>,
)

private data class SkinEntry(
    val id: String,
    val date: String,
    val photoUri: String,
    val products: String,
    val notes: String,
    val zones: Set<String>,
)

private data class HomeworkEntry(
    val id: String,
    val lesson: String,
    val title: String,
    val dueDate: String,
    val priority: Priority,
    val attachment: String,
)

private data class AppData(
    val sleepEntries: List<SleepEntry>,
    val mealEntries: List<MealEntry>,
    val skinEntries: List<SkinEntry>,
    val homeworkEntries: List<HomeworkEntry>,
    val waterCups: Int,
    val profile: Profile,
    val backgroundStyle: BackgroundStyle,
)

private class AppDatabase(context: Context) : SQLiteOpenHelper(context, "glaeth.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE app_state (state_key TEXT PRIMARY KEY, payload TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS app_state")
        onCreate(db)
    }
}

private class AppRepository(context: Context) {
    private val database = AppDatabase(context.applicationContext)

    fun load(): AppData {
        database.readableDatabase.query(
            "app_state",
            arrayOf("payload"),
            "state_key = ?",
            arrayOf("main"),
            null,
            null,
            null,
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return runCatching { parse(JSONObject(cursor.getString(0))) }.getOrElse { demoData() }
            }
        }
        return demoData()
    }

    fun save(data: AppData) {
        val values = ContentValues().apply {
            put("state_key", "main")
            put("payload", data.toJson().toString())
        }
        database.writableDatabase.insertWithOnConflict("app_state", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun parse(root: JSONObject): AppData {
        return AppData(
            sleepEntries = root.optJSONArray("sleep")?.mapJsonObjects {
                SleepEntry(
                    id = it.optString("id", newId()),
                    date = it.optString("date"),
                    sleptAt = it.optString("sleptAt"),
                    wokeAt = it.optString("wokeAt"),
                )
            }.orEmpty(),
            mealEntries = root.optJSONArray("meals")?.mapJsonObjects {
                MealEntry(
                    id = it.optString("id", newId()),
                    date = it.optString("date"),
                    type = enumValueOfOrDefault(it.optString("type"), MealType.Morning),
                    foods = it.optJSONArray("foods")?.mapStrings().orEmpty(),
                )
            }.orEmpty(),
            skinEntries = root.optJSONArray("skin")?.mapJsonObjects {
                SkinEntry(
                    id = it.optString("id", newId()),
                    date = it.optString("date"),
                    photoUri = it.optString("photoUri"),
                    products = it.optString("products"),
                    notes = it.optString("notes"),
                    zones = it.optJSONArray("zones")?.mapStrings()?.toSet().orEmpty(),
                )
            }.orEmpty(),
            homeworkEntries = root.optJSONArray("homework")?.mapJsonObjects {
                HomeworkEntry(
                    id = it.optString("id", newId()),
                    lesson = it.optString("lesson"),
                    title = it.optString("title"),
                    dueDate = it.optString("dueDate"),
                    priority = enumValueOfOrDefault(it.optString("priority"), Priority.Important),
                    attachment = it.optString("attachment"),
                )
            }.orEmpty(),
            waterCups = root.optInt("waterCups", 4),
            profile = root.optJSONObject("profile")?.let {
                Profile(
                    name = it.optString("name", "Kardeşim"),
                    age = it.optInt("age", 16),
                    gender = it.optString("gender", "Belirtilmedi"),
                    photoUri = it.optString("photoUri"),
                )
            } ?: Profile(),
            backgroundStyle = enumValueOfOrDefault(root.optString("backgroundStyle"), BackgroundStyle.Black),
        )
    }
}

private fun AppData.toJson(): JSONObject = JSONObject()
    .put("waterCups", waterCups)
    .put("backgroundStyle", backgroundStyle.name)
    .put(
        "profile",
        JSONObject()
            .put("name", profile.name)
            .put("age", profile.age)
            .put("gender", profile.gender)
            .put("photoUri", profile.photoUri),
    )
    .put("sleep", JSONArray().also { array ->
        sleepEntries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("date", entry.date)
                    .put("sleptAt", entry.sleptAt)
                    .put("wokeAt", entry.wokeAt),
            )
        }
    })
    .put("meals", JSONArray().also { array ->
        mealEntries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("date", entry.date)
                    .put("type", entry.type.name)
                    .put("foods", JSONArray(entry.foods)),
            )
        }
    })
    .put("skin", JSONArray().also { array ->
        skinEntries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("date", entry.date)
                    .put("photoUri", entry.photoUri)
                    .put("products", entry.products)
                    .put("notes", entry.notes)
                    .put("zones", JSONArray(entry.zones.toList())),
            )
        }
    })
    .put("homework", JSONArray().also { array ->
        homeworkEntries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("lesson", entry.lesson)
                    .put("title", entry.title)
                    .put("dueDate", entry.dueDate)
                    .put("priority", entry.priority.name)
                    .put("attachment", entry.attachment),
            )
        }
    })

private fun demoData(): AppData {
    val today = LocalDate.now()
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
        waterCups = 4,
        profile = Profile(),
        backgroundStyle = BackgroundStyle.Black,
    )
}

@Composable
private fun GlaethRoot() {
    val context = LocalContext.current
    val repository = remember { AppRepository(context) }
    var data by remember { mutableStateOf(repository.load()) }

    LaunchedEffect(data) {
        withContext(Dispatchers.IO) { repository.save(data) }
    }

    GlaethTheme(data.backgroundStyle) {
        GlaethApp(
            data = data,
            updateData = { data = it },
        )
    }
}

@Composable
private fun GlaethTheme(style: BackgroundStyle, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = style.accent,
            secondary = Color(0xFFD1D5DB),
            tertiary = Color(0xFF22C55E),
            background = style.bottom,
            surface = Color(0xFF111318),
            surfaceVariant = Color(0xFF1F232B),
            onBackground = Color(0xFFF6F7F9),
            onSurface = Color(0xFFF1F5F9),
            primaryContainer = Color(0xFF20242C),
            onPrimaryContainer = Color(0xFFF8FAFC),
        ),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlaethApp(data: AppData, updateData: (AppData) -> Unit) {
    var section by rememberSaveable { mutableStateOf(Section.Dashboard) }
    var sheet by remember { mutableStateOf<Section?>(null) }
    var profileSheet by remember { mutableStateOf(false) }
    var fullImage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            if (section !in listOf(Section.Dashboard, Section.Settings)) {
                FloatingActionButton(
                    onClick = { sheet = section },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = section.actionLabel)
                }
            }
        },
        bottomBar = {
            GlassBottomBar(selected = section, onSelect = { section = it })
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackground(data.backgroundStyle))
                .padding(padding),
        ) {
            AnimatedContent(targetState = section, label = "seçtion") { target ->
                when (target) {
                    Section.Dashboard -> DashboardScreen(
                        data = data,
                        onWaterChange = { cups -> updateData(data.copy(waterCups = cups.coerceIn(0, 12))) },
                        onOpen = { section = it },
                    )
                    Section.Sleep -> SleepScreen(
                        entries = data.sleepEntries,
                        profile = data.profile,
                        onDelete = { id -> updateData(data.copy(sleepEntries = data.sleepEntries.filterNot { it.id == id })) },
                    )
                    Section.Meals -> MealScreen(
                        entries = data.mealEntries,
                        profile = data.profile,
                        onDelete = { id -> updateData(data.copy(mealEntries = data.mealEntries.filterNot { it.id == id })) },
                    )
                    Section.Skin -> SkinScreen(
                        entries = data.skinEntries,
                        onDelete = { id -> updateData(data.copy(skinEntries = data.skinEntries.filterNot { it.id == id })) },
                        onOpenPhoto = { fullImage = it },
                    )
                    Section.Homework -> HomeworkScreen(
                        entries = data.homeworkEntries,
                        onDelete = { id -> updateData(data.copy(homeworkEntries = data.homeworkEntries.filterNot { it.id == id })) },
                    )
                    Section.Settings -> SettingsScreen(
                        data = data,
                        onBackgroundChange = { updateData(data.copy(backgroundStyle = it)) },
                        onProfileClick = { profileSheet = true },
                    )
                }
            }
            ProfileButton(
                profile = data.profile,
                onClick = { profileSheet = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 12.dp, end = 18.dp),
            )
        }
    }

    sheet?.let { activeSheet ->
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            containerColor = Color(0xFF0D0F14),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            when (activeSheet) {
                Section.Sleep -> SleepForm {
                    updateData(data.copy(sleepEntries = (data.sleepEntries + it).sortedByDescending(SleepEntry::date)))
                    sheet = null
                }
                Section.Meals -> MealForm {
                    updateData(data.copy(mealEntries = (data.mealEntries + it).sortedByDescending(MealEntry::date)))
                    sheet = null
                }
                Section.Skin -> SkinForm { entries ->
                    updateData(data.copy(skinEntries = (data.skinEntries + entries).sortedByDescending(SkinEntry::date)))
                    sheet = null
                }
                Section.Homework -> HomeworkForm {
                    updateData(data.copy(homeworkEntries = (data.homeworkEntries + it).sortedBy(HomeworkEntry::dueDate)))
                    sheet = null
                }
                Section.Dashboard, Section.Settings -> Unit
            }
        }
    }

    if (profileSheet) {
        ModalBottomSheet(
            onDismissRequest = { profileSheet = false },
            containerColor = Color(0xFF0D0F14),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            ProfileForm(
                profile = data.profile,
                onSave = {
                    updateData(data.copy(profile = it))
                    profileSheet = false
                },
            )
        }
    }

    fullImage?.let { uri ->
        FullScreenPhoto(uri = uri, onDismiss = { fullImage = null })
    }
}

@Composable
private fun DashboardScreen(data: AppData, onWaterChange: (Int) -> Unit, onOpen: (Section) -> Unit) {
    val averageSleep = data.sleepEntries.mapNotNull { it.duration() }.averageOrZero()
    val nextHomework = data.homeworkEntries.minByOrNull { it.dueDate }
    val sleepReport = sleepStatus(averageSleep, data.profile.age)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(end = 72.dp)) {
                Text("Glaeth", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black))
                Text(
                    "${data.profile.name} için uyku, cilt, öğün ve ödev kontrol paneli.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f),
                )
            }
        }
        item {
            HeroCard(
                title = "${data.skinEntries.size.coerceAtLeast(441)} gündür devam",
                subtitle = "Siyah glass tema aktif. Fotoğrafları aç, sağa kaydırıp sil, ritmi koru.",
                icon = Icons.Filled.Favorite,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Ort. uyku", "${averageSleep.oneDecimal()} saat", Icons.Filled.DateRange, Modifier.weight(1f))
                StatCard("Uyku durumu", sleepReport.title, Icons.Filled.Face, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Cilt arşivi", "${data.skinEntries.size} gün", Icons.Filled.Face, Modifier.weight(1f))
                StatCard("Ödev", "${data.homeworkEntries.size}", Icons.Filled.DateRange, Modifier.weight(1f))
            }
        }
        item { InsightCard("Yaşa göre uyku", sleepReport.detail) }
        item { WaterCard(cups = data.waterCups, onChange = onWaterChange) }
        item { nextHomework?.let { CountdownCard(entry = it, onOpen = { onOpen(Section.Homework) }) } }
        item { QuickActions(onOpen) }
    }
}

@Composable
private fun HeroCard(title: String, subtitle: String, icon: ImageVector) {
    GlassCard {
        Row(
            modifier = Modifier.padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, fontWeight = FontWeight.Black, fontSize = 22.sp, maxLines = 1)
            Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun InsightCard(title: String, body: String) {
    GlassCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(body, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun WaterCard(cups: Int, onChange: (Int) -> Unit) {
    GlassCard {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Su ve cilt dengesi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            LinearProgressIndicator(
                progress = { (cups / 8f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = Color.White.copy(alpha = 0.12f),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("$cups / 8 bardak")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onChange(cups - 1) }) { Text("-") }
                    Button(onClick = { onChange(cups + 1) }) { Text("+") }
                }
            }
        }
    }
}

@Composable
private fun CountdownCard(entry: HomeworkEntry, onOpen: () -> Unit) {
    val days = runCatching { ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(entry.dueDate)) }.getOrDefault(0)
    GlassCard {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = entry.priority.color, modifier = Modifier.size(32.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${entry.lesson}: ${entry.title}", fontWeight = FontWeight.Bold)
                Text(if (days >= 0) "Son $days gün" else "Teslim tarihi geçti", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            }
            TextButton(onClick = onOpen) { Text("Aç") }
        }
    }
}

@Composable
private fun QuickActions(onOpen: (Section) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(listOf(Section.Sleep, Section.Meals, Section.Skin, Section.Homework, Section.Settings)) { action ->
            Button(
                onClick = { onOpen(action) },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.09f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            ) {
                Icon(action.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (action.actionLabel.isBlank()) action.label else action.actionLabel)
            }
        }
    }
}

@Composable
private fun SleepScreen(entries: List<SleepEntry>, profile: Profile, onDelete: (String) -> Unit) {
    val average = entries.mapNotNull { it.duration() }.averageOrZero()
    SectionList(
        title = "Uyku günlüğü",
        subtitle = "Grafiği, kalite yorumunu ve toplam süreyi takip et.",
        header = {
            SleepChart(entries, profile.age)
            InsightCard("Otomatik karar", sleepStatus(average, profile.age).detail)
        },
    ) {
        items(entries) { entry ->
            DismissibleItem(onDelete = { onDelete(entry.id) }) {
                SleepEntryCard(entry = entry, age = profile.age, onDelete = { onDelete(entry.id) })
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
                Text("Detaylı uyku grafiği", fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text("Hedef ${target.oneDecimal()}s", color = MaterialTheme.colorScheme.primary)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                entries.takeLast(7).forEach { entry ->
                    val hours = entry.duration() ?: 0.0
                    val enough = hours >= target
                    val fraction = (hours / (target + 2)).coerceIn(0.08, 1.0).toFloat()
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.64f)
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                if (enough) Color(0xFF22C55E) else Color(0xFFFF5C5C),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                                            ),
                                        ),
                                    ),
                            )
                        }
                        Text("${hours.oneDecimal()}s", fontSize = 11.sp)
                        Text(entry.date.takeLast(5), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepEntryCard(entry: SleepEntry, age: Int, onDelete: () -> Unit) {
    val hours = entry.duration().orZero()
    val status = sleepStatus(hours, age)
    GlassCard {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = status.color)
            Column(modifier = Modifier.weight(1f)) {
                Text("${entry.sleptAt} - ${entry.wokeAt}", fontWeight = FontWeight.Bold)
                Text("${entry.date} | ${hours.oneDecimal()} saat | ${status.title}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
            }
            DeleteButton(onDelete)
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
        items(entries.groupBy { it.date }.toList()) { (date, dayEntries) ->
            GlassCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Gün: $date", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    dayEntries.forEach { meal ->
                        DismissibleItem(onDelete = { onDelete(meal.id) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(meal.type.label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(meal.foods.joinToString(separator = "\n"), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f))
                                }
                                DeleteButton { onDelete(meal.id) }
                            }
                        }
                        if (meal != dayEntries.last()) HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
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
            HeroCard(
                title = "Toplu içe aktarma",
                subtitle = "Fotoğrafa dokun: büyüt. Sağa kaydır: sil. Zaman yolculuğuna dokun: ilk fotoğrafı aç.",
                icon = Icons.Filled.Face,
            )
            if (entries.isNotEmpty()) {
                TimeLapseStrip(
                    entries = entries,
                    dayNumbers = indexed,
                    onOpenPhoto = onOpenPhoto,
                )
            }
        },
    ) {
        items(entries) { entry ->
            DismissibleItem(onDelete = { onDelete(entry.id) }) {
                SkinEntryCard(
                    entry = entry,
                    dayNumber = indexed[entry.id] ?: 1,
                    onDelete = { onDelete(entry.id) },
                    onOpenPhoto = { onOpenPhoto(entry.photoUri) },
                )
            }
        }
    }
}

@Composable
private fun TimeLapseStrip(entries: List<SkinEntry>, dayNumbers: Map<String, Int>, onOpenPhoto: (String) -> Unit) {
    GlassCard(
        modifier = Modifier.clickable { entries.sortedBy { it.date }.firstOrNull()?.let { onOpenPhoto(it.photoUri) } },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Zaman yolculuğu - dokun", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(entries.sortedBy { it.date }) { entry ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AsyncImage(
                            model = entry.photoUri,
                            contentDescription = "Gün ${dayNumbers[entry.id]}",
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { onOpenPhoto(entry.photoUri) },
                            contentScale = ContentScale.Crop,
                        )
                        Text("Gün ${dayNumbers[entry.id] ?: 1}", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkinEntryCard(entry: SkinEntry, dayNumber: Int, onDelete: () -> Unit, onOpenPhoto: () -> Unit) {
    GlassCard {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AsyncImage(
                model = entry.photoUri,
                contentDescription = "Gün $dayNumber",
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onOpenPhoto() },
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gün $dayNumber", fontWeight = FontWeight.Black, fontSize = 20.sp)
                if (entry.zones.isNotEmpty()) Text("Bölge: ${entry.zones.joinToString()}", color = MaterialTheme.colorScheme.primary)
                if (entry.products.isNotBlank()) Text("Ürün: ${entry.products}", maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (entry.notes.isNotBlank()) Text(entry.notes, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), maxLines = 2)
            }
            DeleteButton(onDelete)
        }
    }
}

@Composable
private fun HomeworkScreen(entries: List<HomeworkEntry>, onDelete: (String) -> Unit) {
    SectionList(
        title = "Ödev panosu",
        subtitle = "Kartları sağa kaydırarak veya Sil tuşuyla temizle.",
        header = { HeroCard("Geri sayım aktif", "Ana sayfada en yakın ödev için geri sayım kartı görünür.", Icons.Filled.DateRange) },
    ) {
        items(entries) { entry ->
            DismissibleItem(onDelete = { onDelete(entry.id) }) {
                HomeworkCard(entry = entry, onDelete = { onDelete(entry.id) })
            }
        }
    }
}

@Composable
private fun HomeworkCard(entry: HomeworkEntry, onDelete: () -> Unit) {
    GlassCard {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(entry.priority.color),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${entry.lesson} - ${entry.title}", fontWeight = FontWeight.Black)
                Text("Teslim: ${entry.dueDate}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                if (entry.attachment.isNotBlank()) Text("Ek: ${entry.attachment}", color = MaterialTheme.colorScheme.secondary)
            }
            AssistChip(onClick = {}, label = { Text(entry.priority.label) })
            DeleteButton(onDelete)
        }
    }
}

@Composable
private fun SettingsScreen(data: AppData, onBackgroundChange: (BackgroundStyle) -> Unit, onProfileClick: () -> Unit) {
    SectionList(
        title = "Ayarlar",
        subtitle = "Arka plan rengini ve profil bilgilerini yönet.",
        header = {
            GlassCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Arka plan teması", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    ChipSelector(BackgroundStyle.entries, data.backgroundStyle, onBackgroundChange) { it.label }
                    Button(onClick = onProfileClick, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Person, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Profil bilgilerini düzenle")
                    }
                }
            }
        },
    ) {}
}

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
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        persistUris(listOfNotNull(uri))
        selectedUris = listOfNotNull(uri)
    }
    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        persistUris(uris)
        selectedUris = uris
    }

    FormShell(title = "Cilt fotoğrafı ekle") {
        DateField(date, { date = it })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { singlePicker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) { Text("Tek seç") }
            OutlinedButton(onClick = { multiPicker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) { Text("Toplu seç") }
        }
        if (selectedUris.isNotEmpty()) Text("${selectedUris.size} fotoğraf seçildi. İlk seçilen en eski, son seçilen bugün kabul edilir.")
        Text("Yüz haritası", fontWeight = FontWeight.Bold)
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
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            photoUri = it.toString()
        }
    }

    FormShell(title = "Profil") {
        GlassCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { picker.launch(arrayOf("image/*")) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (photoUri.isNotBlank()) {
                        AsyncImage(photoUri, contentDescription = "Profil fotoğrafı", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(44.dp))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Profil fotoğrafı", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(
                        if (photoUri.isBlank()) "Fotoğraf eklemek için dokun" else "Fotoğrafı değiştirmek için dokun",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                    )
                }
            }
        }
        OutlinedTextField(name, { name = it }, label = { Text("İsim") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(age, { age = it.filter(Char::isDigit).take(2) }, label = { Text("Yaş") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Text("Cinsiyet", fontWeight = FontWeight.Bold)
        ChipSelector(listOf("Erkek", "Kadın"), gender, { gender = it }) { it }
        Button(
            onClick = { onSave(Profile(name.ifBlank { "Kardeşim" }, age.toIntOrNull() ?: 16, gender.ifBlank { "Belirtilmedi" }, photoUri)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Kaydet") }
    }
}

@Composable
private fun FormShell(title: String, content: @Composable () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text(title, fontWeight = FontWeight.Black, fontSize = 24.sp) }
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
private fun <T> ChipSelector(values: List<T>, selected: T, onSelected: (T) -> Unit, label: (T) -> String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
            FilterChip(selected = value == selected, onClick = { onSelected(value) }, label = { Text(label(value)) })
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
private fun SectionList(
    title: String,
    subtitle: String,
    header: @Composable () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(end = 72.dp)) {
                Text(title, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black))
                Text(subtitle, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f))
            }
        }
        item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = { header() }) }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissibleItem(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .height(64.dp)
                        .width(104.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFFE5484D).copy(alpha = 0.94f)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Sil", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        },
        content = { content() },
    )
}

@Composable
private fun DeleteButton(onDelete: () -> Unit) {
    IconButton(onClick = onDelete) {
        Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = Color(0xFFFF7A7A))
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(22.dp, RoundedCornerShape(30.dp), ambientColor = Color.Black.copy(alpha = 0.38f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(30.dp)),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.075f)),
        content = { content() },
    )
}

@Composable
private fun GlassBottomBar(selected: Section, onSelect: (Section) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(30.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(30.dp)),
        tonalElevation = 0.dp,
        color = Color(0xFF0A0B0F).copy(alpha = 0.92f),
    ) {
        LazyRow(
            contentPadding = PaddingValues(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(Section.entries) { item ->
                val active = item == selected
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else Color.Transparent)
                        .clickable { onSelect(item) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = if (active) MaterialTheme.colorScheme.primary else Color(0xFF9CA3AF), modifier = Modifier.size(19.dp))
                    Text(item.label, color = if (active) MaterialTheme.colorScheme.primary else Color(0xFF9CA3AF), fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ProfileButton(profile: Profile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (profile.photoUri.isNotBlank()) {
            AsyncImage(profile.photoUri, contentDescription = "Profil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Filled.Person, contentDescription = "Profil", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun FullScreenPhoto(uri: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "Orijinal fotoğraf",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

private fun appBackground(style: BackgroundStyle): Brush =
    Brush.verticalGradient(listOf(style.top, style.bottom, Color.Black))

private data class SleepTarget(val recommended: Double, val rangeText: String)
private data class SleepReport(val title: String, val detail: String, val color: Color)

private fun sleepTarget(age: Int): SleepTarget = when (age) {
    in 0..2 -> SleepTarget(12.0, "11-14 saat")
    in 3..5 -> SleepTarget(11.0, "10-13 saat")
    in 6..12 -> SleepTarget(10.0, "9-12 saat")
    in 13..18 -> SleepTarget(8.5, "8-10 saat")
    else -> SleepTarget(7.5, "7-9 saat")
}

private fun sleepStatus(hours: Double, age: Int): SleepReport {
    val target = sleepTarget(age)
    return when {
        hours <= 0.0 -> SleepReport("Veri yok", "Uyku kaydı eklenince yaşa göre otomatik yorumlanacak.", Color(0xFF9CA3AF))
        hours + 0.25 < target.recommended -> SleepReport("Yetersiz", "$age yaş için önerilen aralık ${target.rangeText}. Ortalama biraz düşük.", Color(0xFFFF6B6B))
        hours > target.recommended + 2 -> SleepReport("Fazla", "$age yaş için önerilen aralık ${target.rangeText}. Uyku süresi uzun görünüyor.", Color(0xFFFFB84D))
        else -> SleepReport("Yeterli", "$age yaş için önerilen aralık ${target.rangeText}. Uyku süresi iyi görünüyor.", Color(0xFF22C55E))
    }
}

private fun mealSuggestion(age: Int): String = when (age) {
    in 0..5 -> "Protein, yoğurt/süt, yumurta, meyve ve sebze ağırlıklı minik porsiyonlar iyi olur."
    in 6..12 -> "Kahvaltıda yumurta/peynir, öğlen protein + tahıl, akşam sebze + yoğurt dengesi önerilir."
    in 13..18 -> "Ergenlik dönemi için protein, kompleks karbonhidrat, yeşillik, su ve şekeri azaltma cilt için önemli."
    else -> "Protein, lifli sebze, tam tahıl ve yeterli su dengesi takip edilmeli."
}

private fun SleepEntry.duration(): Double? {
    val start = sleptAt.parseTimeOrNull() ?: return null
    val end = wokeAt.parseTimeOrNull() ?: return null
    val rawMinutes = Duration.between(start, end).toMinutes()
    val minutes = if (rawMinutes <= 0) rawMinutes + Duration.ofDays(1).toMinutes() else rawMinutes
    return minutes / 60.0
}

private fun String.parseTimeOrNull(): LocalTime? = try {
    LocalTime.parse(this, DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT))
} catch (_: DateTimeParseException) {
    null
}

private fun String.isValidTime(): Boolean = parseTimeOrNull() != null
private fun String.isValidDate(): Boolean = runCatching { LocalDate.parse(this) }.isSuccess
private fun Double?.orZero(): Double = this ?: 0.0
private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()
private fun Double.oneDecimal(): String = ((this * 10).roundToInt() / 10.0).toString()
private fun newId(): String = "${System.currentTimeMillis()}-${(0..9999).random()}"

private inline fun <reified T : Enum<T>> enumValueOfOrDefault(name: String, default: T): T =
    runCatching { enumValueOf<T>(name) }.getOrDefault(default)

private fun JSONArray.mapStrings(): List<String> = buildList {
    for (index in 0 until length()) add(optString(index))
}

private fun <T> JSONArray.mapJsonObjects(transform: (JSONObject) -> T): List<T> = buildList {
    for (index in 0 until length()) {
        optJSONObject(index)?.let { add(transform(it)) }
    }
}

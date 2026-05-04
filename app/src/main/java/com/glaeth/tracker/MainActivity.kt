package com.glaeth.tracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
        setContent {
            GlaethTheme {
                GlaethApp()
            }
        }
    }
}

private enum class Section(
    val label: String,
    val icon: ImageVector,
    val actionLabel: String,
) {
    Dashboard("Ana Sayfa", Icons.Filled.Home, ""),
    Sleep("Uyku", Icons.Filled.Timer, "Uyku ekle"),
    Meals("Ogunler", Icons.Filled.Restaurant, "Ogun ekle"),
    Skin("Cilt", Icons.Filled.Face, "Fotograf ekle"),
    Homework("Odev", Icons.Filled.School, "Odev ekle"),
}

private enum class MealType(val label: String) {
    Morning("Sabah"),
    Lunch("Ogle"),
    Evening("Aksam"),
    Snack("Ara Ogun"),
}

private enum class Priority(val label: String, val color: Color) {
    Urgent("Acil", Color(0xFFFF5C8A)),
    Important("Onemli", Color(0xFFFFB74D)),
    Chill("Keyfi", Color(0xFF7CDA95)),
}

private data class SleepEntry(
    val date: String,
    val sleptAt: String,
    val wokeAt: String,
)

private data class MealEntry(
    val date: String,
    val type: MealType,
    val foods: List<String>,
)

private data class SkinEntry(
    val date: String,
    val photoUri: String,
    val products: String,
    val notes: String,
    val zones: Set<String>,
)

private data class HomeworkEntry(
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
)

private class AppRepository(context: Context) {
    private val prefs = context.getSharedPreferences("glaeth-data", Context.MODE_PRIVATE)

    fun load(): AppData {
        val json = prefs.getString("payload", null) ?: return demoData()
        return runCatching { parse(JSONObject(json)) }.getOrElse { demoData() }
    }

    fun save(data: AppData) {
        prefs.edit().putString("payload", data.toJson().toString()).apply()
    }

    private fun parse(root: JSONObject): AppData {
        return AppData(
            sleepEntries = root.optJSONArray("sleep")?.mapJsonObjects {
                SleepEntry(
                    date = it.optString("date"),
                    sleptAt = it.optString("sleptAt"),
                    wokeAt = it.optString("wokeAt"),
                )
            }.orEmpty(),
            mealEntries = root.optJSONArray("meals")?.mapJsonObjects {
                MealEntry(
                    date = it.optString("date"),
                    type = enumValueOfOrDefault(it.optString("type"), MealType.Morning),
                    foods = it.optJSONArray("foods")?.mapStrings().orEmpty(),
                )
            }.orEmpty(),
            skinEntries = root.optJSONArray("skin")?.mapJsonObjects {
                SkinEntry(
                    date = it.optString("date"),
                    photoUri = it.optString("photoUri"),
                    products = it.optString("products"),
                    notes = it.optString("notes"),
                    zones = it.optJSONArray("zones")?.mapStrings()?.toSet().orEmpty(),
                )
            }.orEmpty(),
            homeworkEntries = root.optJSONArray("homework")?.mapJsonObjects {
                HomeworkEntry(
                    lesson = it.optString("lesson"),
                    title = it.optString("title"),
                    dueDate = it.optString("dueDate"),
                    priority = enumValueOfOrDefault(it.optString("priority"), Priority.Important),
                    attachment = it.optString("attachment"),
                )
            }.orEmpty(),
            waterCups = root.optInt("waterCups", 4),
        )
    }
}

private fun AppData.toJson(): JSONObject = JSONObject()
    .put("waterCups", waterCups)
    .put("sleep", JSONArray().also { array ->
        sleepEntries.forEach { entry ->
            array.put(
                JSONObject()
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
            SleepEntry(today.minusDays(2).toString(), "23:20", "07:10"),
            SleepEntry(today.minusDays(1).toString(), "22:55", "06:45"),
            SleepEntry(today.toString(), "23:05", "07:25"),
        ),
        mealEntries = listOf(
            MealEntry(today.toString(), MealType.Morning, listOf("Tost", "Cay", "Sut")),
            MealEntry(today.toString(), MealType.Lunch, listOf("Borek", "Ayran")),
            MealEntry(today.minusDays(1).toString(), MealType.Evening, listOf("Corba", "Pilav")),
        ),
        skinEntries = emptyList(),
        homeworkEntries = listOf(
            HomeworkEntry("Matematik", "Problemler testi", today.plusDays(1).toString(), Priority.Urgent, ""),
            HomeworkEntry("Turkce", "Kitap ozeti", today.plusDays(3).toString(), Priority.Important, ""),
        ),
        waterCups = 4,
    )
}

@Composable
private fun GlaethTheme(content: @Composable () -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFFFF7DAF),
            secondary = Color(0xFFB7A7FF),
            tertiary = Color(0xFF72E6C4),
            background = Color(0xFF0C0B12),
            surface = Color(0xFF171421),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFE93C86),
            secondary = Color(0xFF7257D8),
            tertiary = Color(0xFF00A884),
            background = Color(0xFFFFF8FB),
            surface = Color(0xFFFFFFFF),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlaethApp() {
    val context = LocalContext.current
    val repository = remember { AppRepository(context) }
    var data by remember { mutableStateOf(repository.load()) }
    var section by rememberSaveable { mutableStateOf(Section.Dashboard) }
    var sheet by remember { mutableStateOf<Section?>(null) }

    LaunchedEffect(data) {
        withContext(Dispatchers.IO) {
            repository.save(data)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            if (section != Section.Dashboard) {
                FloatingActionButton(
                    onClick = { sheet = section },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = section.actionLabel)
                }
            }
        },
        bottomBar = {
            GlassBottomBar(
                selected = section,
                onSelect = { section = it },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                        ),
                    ),
                )
                .padding(padding),
        ) {
            AnimatedContent(
                targetState = section,
                label = "section",
            ) { target ->
                when (target) {
                    Section.Dashboard -> DashboardScreen(
                        data = data,
                        onWaterChange = { cups -> data = data.copy(waterCups = cups.coerceIn(0, 12)) },
                        onOpen = { section = it },
                    )
                    Section.Sleep -> SleepScreen(data.sleepEntries)
                    Section.Meals -> MealScreen(data.mealEntries)
                    Section.Skin -> SkinScreen(data.skinEntries)
                    Section.Homework -> HomeworkScreen(data.homeworkEntries)
                }
            }
        }
    }

    sheet?.let { activeSheet ->
        ModalBottomSheet(onDismissRequest = { sheet = null }) {
            when (activeSheet) {
                Section.Sleep -> SleepForm(
                    onAdd = {
                        data = data.copy(sleepEntries = (data.sleepEntries + it).sortedByDescending(SleepEntry::date))
                        sheet = null
                    },
                )
                Section.Meals -> MealForm(
                    onAdd = {
                        data = data.copy(mealEntries = (data.mealEntries + it).sortedByDescending(MealEntry::date))
                        sheet = null
                    },
                )
                Section.Skin -> SkinForm(
                    onAddMany = { entries ->
                        data = data.copy(skinEntries = (data.skinEntries + entries).sortedByDescending(SkinEntry::date))
                        sheet = null
                    },
                )
                Section.Homework -> HomeworkForm(
                    onAdd = {
                        data = data.copy(homeworkEntries = (data.homeworkEntries + it).sortedBy(HomeworkEntry::dueDate))
                        sheet = null
                    },
                )
                Section.Dashboard -> Unit
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    data: AppData,
    onWaterChange: (Int) -> Unit,
    onOpen: (Section) -> Unit,
) {
    val averageSleep = data.sleepEntries.mapNotNull { it.duration() }.averageOrZero()
    val skinDays = data.skinEntries.size
    val nextHomework = data.homeworkEntries.minByOrNull { it.dueDate }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Glaeth",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                )
                Text(
                    text = "Uyku, cilt, ogun ve odev takibi icin modern gunluk panel.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                )
            }
        }
        item {
            HeroCard(
                title = "441 gundur devam ediyorsun",
                subtitle = "Bugunun kaydini ekle, degisimi time-lapse gibi izle ve ritmini koru.",
                icon = Icons.Filled.Favorite,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Ort. uyku", "${averageSleep.oneDecimal()} saat", Icons.Filled.Timer, Modifier.weight(1f))
                StatCard("Cilt arsivi", "$skinDays gun", Icons.Filled.Face, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Ogun kaydi", "${data.mealEntries.size}", Icons.Filled.Restaurant, Modifier.weight(1f))
                StatCard("Odev", "${data.homeworkEntries.size}", Icons.Filled.School, Modifier.weight(1f))
            }
        }
        item {
            WaterCard(cups = data.waterCups, onChange = onWaterChange)
        }
        item {
            nextHomework?.let {
                CountdownCard(entry = it, onOpen = { onOpen(Section.Homework) })
            }
        }
        item {
            QuickActions(onOpen)
        }
    }
}

@Composable
private fun HeroCard(title: String, subtitle: String, icon: ImageVector) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun WaterCard(cups: Int, onChange: (Int) -> Unit) {
    GlassCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalDrink, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(10.dp))
                Text("Su ve cilt dengesi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            LinearProgressIndicator(
                progress = { (cups / 8f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = MaterialTheme.colorScheme.tertiary,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("$cups / 8 bardak")
                Row {
                    OutlinedButton(onClick = { onChange(cups - 1) }) { Text("-") }
                    Spacer(Modifier.width(8.dp))
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
                Text(if (days >= 0) "Son $days gun" else "Teslim tarihi gecti", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            }
            TextButton(onClick = onOpen) { Text("Ac") }
        }
    }
}

@Composable
private fun QuickActions(onOpen: (Section) -> Unit) {
    val actions = listOf(Section.Sleep, Section.Meals, Section.Skin, Section.Homework)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(actions) { action ->
            Button(
                onClick = { onOpen(action) },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), contentColor = MaterialTheme.colorScheme.onSurface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
            ) {
                Icon(action.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(action.actionLabel)
            }
        }
    }
}

@Composable
private fun SleepScreen(entries: List<SleepEntry>) {
    val average = entries.mapNotNull { it.duration() }.averageOrZero()
    SectionList(
        title = "Uyku gunlugu",
        subtitle = "Yatma, kalkma ve toplam sureyi detayli takip et.",
        header = {
            SleepChart(entries)
            StatCard("Ortalama uyku", "${average.oneDecimal()} saat", Icons.Filled.Timer, Modifier.fillMaxWidth())
        },
    ) {
        items(entries) { entry ->
            SleepEntryCard(entry)
        }
    }
}

@Composable
private fun SleepChart(entries: List<SleepEntry>) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            entries.takeLast(7).forEach { entry ->
                val hours = entry.duration() ?: 0.0
                val fraction = (hours / 10.0).coerceIn(0.08, 1.0).toFloat()
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.62f)
                                .height((102 * fraction).dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
                        )
                    }
                    Text(entry.date.takeLast(5), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                }
            }
        }
    }
}

@Composable
private fun SleepEntryCard(entry: SleepEntry) {
    GlassCard {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Filled.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.date, fontWeight = FontWeight.Bold)
                Text("${entry.sleptAt} uyudu - ${entry.wokeAt} uyandi", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            }
            Text("${entry.duration().orZero().oneDecimal()}s", fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
    }
}

@Composable
private fun MealScreen(entries: List<MealEntry>) {
    SectionList(
        title = "Ogunler",
        subtitle = "Sabah, ogle, aksam ve ara ogunleri gun gun kaydet.",
        header = {
            HeroCard("Bugun ne yedi?", "Tost, cay, sut gibi ogeleri satir satir ekleyebilirsin.", Icons.Filled.Restaurant)
        },
    ) {
        items(entries.groupBy { it.date }.toList()) { (date, dayEntries) ->
            GlassCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(date, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    dayEntries.forEach { meal ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(meal.type.label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(meal.foods.joinToString(separator = "\n"), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f))
                        }
                        if (meal != dayEntries.last()) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SkinScreen(entries: List<SkinEntry>) {
    SectionList(
        title = "Cilt takip",
        subtitle = "Fotograflari toplu yukle, bolge ve urun notlariyla degisimi izle.",
        header = {
            HeroCard(
                title = "Toplu ice aktarma hazir",
                subtitle = "Galeriden birden fazla fotograf sec; uygulama tarihleri otomatik siralar ve arsive ekler.",
                icon = Icons.Filled.Face,
            )
            if (entries.isNotEmpty()) TimeLapseStrip(entries)
        },
    ) {
        items(entries) { entry ->
            SkinEntryCard(entry)
        }
    }
}

@Composable
private fun TimeLapseStrip(entries: List<SkinEntry>) {
    GlassCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Zaman yolculugu", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(entries) { entry ->
                    AsyncImage(
                        model = entry.photoUri,
                        contentDescription = entry.date,
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}

@Composable
private fun SkinEntryCard(entry: SkinEntry) {
    GlassCard {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AsyncImage(
                model = entry.photoUri,
                contentDescription = entry.date,
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.date, fontWeight = FontWeight.Black)
                if (entry.zones.isNotEmpty()) Text("Bolge: ${entry.zones.joinToString()}", color = MaterialTheme.colorScheme.primary)
                if (entry.products.isNotBlank()) Text("Urun: ${entry.products}", maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (entry.notes.isNotBlank()) Text(entry.notes, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), maxLines = 2)
            }
        }
    }
}

@Composable
private fun HomeworkScreen(entries: List<HomeworkEntry>) {
    SectionList(
        title = "Odev panosu",
        subtitle = "Ders, odev adi, teslim tarihi, oncelik ve dosya notlarini ekle.",
        header = {
            HeroCard("Darlayan widget modu", "Ana sayfada en yakin odev icin geri sayim karti gorunur.", Icons.Filled.School)
        },
    ) {
        items(entries) { entry ->
            GlassCard {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
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
                }
            }
        }
    }
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
            onClick = { onAdd(SleepEntry(date, sleptAt, wokeAt)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = date.isValidDate() && sleptAt.isValidTime() && wokeAt.isValidTime(),
        ) {
            Text("Kaydet")
        }
    }
}

@Composable
private fun MealForm(onAdd: (MealEntry) -> Unit) {
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var mealType by rememberSaveable { mutableStateOf(MealType.Morning) }
    var foods by rememberSaveable { mutableStateOf("Tost\nCay\nSut") }
    FormShell(title = "Ogun ekle") {
        DateField(date, { date = it })
        ChipSelector(MealType.entries, mealType, { mealType = it }) { it.label }
        OutlinedTextField(
            value = foods,
            onValueChange = { foods = it },
            label = { Text("Yiyecekler (her satir bir oge)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Button(
            onClick = {
                onAdd(MealEntry(date, mealType, foods.lines().map { it.trim() }.filter { it.isNotBlank() }))
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = date.isValidDate() && foods.isNotBlank(),
        ) {
            Text("Kaydet")
        }
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

    FormShell(title = "Cilt fotografi ekle") {
        DateField(date, { date = it })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { singlePicker.launch(arrayOf("image/*")) },
                modifier = Modifier.weight(1f),
            ) { Text("Tek sec") }
            OutlinedButton(
                onClick = { multiPicker.launch(arrayOf("image/*")) },
                modifier = Modifier.weight(1f),
            ) { Text("Toplu sec") }
        }
        if (selectedUris.isNotEmpty()) Text("${selectedUris.size} fotograf secildi. Toplu aktarimda ilk secilen en eski, son secilen bugunun kaydi sayilir.")
        Text("Yuz haritasi", fontWeight = FontWeight.Bold)
        ZoneSelector(selectedZones)
        OutlinedTextField(
            value = products,
            onValueChange = { products = it },
            label = { Text("Krem / ilac / urun") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notlar") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        Button(
            onClick = {
                val entries = selectedUris.mapIndexed { index, uri ->
                    SkinEntry(
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
        ) {
            Text("Arsive ekle")
        }
    }
}

@Composable
private fun HomeworkForm(onAdd: (HomeworkEntry) -> Unit) {
    var lesson by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var dueDate by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var priority by rememberSaveable { mutableStateOf(Priority.Important) }
    var attachment by rememberSaveable { mutableStateOf("") }
    FormShell(title = "Odev ekle") {
        OutlinedTextField(lesson, { lesson = it }, label = { Text("Ders") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(title, { title = it }, label = { Text("Odev adi") }, modifier = Modifier.fillMaxWidth())
        DateField(dueDate, { dueDate = it }, "Teslim tarihi")
        ChipSelector(Priority.entries, priority, { priority = it }) { it.label }
        OutlinedTextField(attachment, { attachment = it }, label = { Text("Dosya/fotograf notu veya link") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { onAdd(HomeworkEntry(lesson, title, dueDate, priority, attachment)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = lesson.isNotBlank() && title.isNotBlank() && dueDate.isValidDate(),
        ) {
            Text("Kaydet")
        }
    }
}

@Composable
private fun FormShell(title: String, content: @Composable ColumnScopeCompat.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, fontWeight = FontWeight.Black, fontSize = 24.sp)
        ColumnScopeCompat(this).content()
    }
}

private class ColumnScopeCompat(private val column: ColumnScopeMarker)
private typealias ColumnScopeMarker = androidx.compose.foundation.layout.ColumnScope

@Composable
private fun DateField(value: String, onChange: (String) -> Unit, label: String = "Tarih") {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("$label (YYYY-AA-GG)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        modifier = Modifier.fillMaxWidth(),
        isError = value.isNotBlank() && !value.isValidDate(),
    )
}

@Composable
private fun TimeField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("$label (HH:MM)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        modifier = Modifier.fillMaxWidth(),
        isError = value.isNotBlank() && !value.isValidTime(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipSelector(
    values: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: (T) -> String,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelected(value) },
                label = { Text(label(value)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZoneSelector(selectedZones: MutableList<String>) {
    val zones = listOf("Alin", "Cene", "Sol yanak", "Sag yanak", "Burun")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        zones.forEach { zone ->
            FilterChip(
                selected = zone in selectedZones,
                onClick = {
                    if (zone in selectedZones) selectedZones.remove(zone) else selectedZones.add(zone)
                },
                label = { Text(zone) },
            )
        }
    }
}

@Composable
private fun SectionList(
    title: String,
    subtitle: String,
    header: @Composable ColumnScopeMarker.() -> Unit,
    content: LazyListScopeCompat.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black))
                Text(subtitle, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f))
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = header)
        }
        LazyListScopeCompat(this).content()
    }
}

private class LazyListScopeCompat(private val scope: androidx.compose.foundation.lazy.LazyListScope) {
    fun <T> items(items: List<T>, itemContent: @Composable (T) -> Unit) {
        scope.items(items) { item -> itemContent(item) }
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        content = { content() },
    )
}

@Composable
private fun GlassBottomBar(selected: Section, onSelect: (Section) -> Unit) {
    val sections = Section.entries
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(14.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), RoundedCornerShape(28.dp)),
        tonalElevation = 14.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sections.forEach { item ->
                val selectedColor by animateColorAsState(
                    targetValue = if (item == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                    animationSpec = spring(),
                    label = "tabColor",
                )
                IconButton(onClick = { onSelect(item) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(item.icon, contentDescription = item.label, tint = selectedColor)
                        Text(item.label, fontSize = 10.sp, color = selectedColor, maxLines = 1)
                    }
                }
            }
        }
    }
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

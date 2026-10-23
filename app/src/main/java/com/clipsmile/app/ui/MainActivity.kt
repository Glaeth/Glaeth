package com.clipsmile.app.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.clipsmile.app.AppVisibility
import com.clipsmile.app.R
import com.clipsmile.app.data.Den
import com.clipsmile.app.data.PetProgress
import com.clipsmile.app.databinding.ActivityMainBinding
import com.clipsmile.app.service.ClipboardWatcherService
import com.clipsmile.app.util.ClipCapturePipeline
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var petProgress: PetProgress
    private lateinit var den: Den

    private var glowPulse: ObjectAnimator? = null
    private var tab = HomeSections.Tab.HOME
    private var pickedStarter: String? = null
    private var tutorialStep = 0
    private var tutorialGen = 0
    private var feeding = false
    private var bathing = false
    private var playerLevel = 1
    private var companion = DogRoster.all.first()
    private var soundPool: SoundPool? = null
    private val barkIds = HashMap<Int, Int>()

    private val foregroundClipListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboardManager.primaryClip ?: return@OnPrimaryClipChangedListener
        if (clip.itemCount == 0) return@OnPrimaryClipChangedListener
        val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: return@OnPrimaryClipChangedListener
        ClipCapturePipeline.captureAsync(lifecycleScope, this, text)
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                ClipboardWatcherService.start(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        clipboardManager = getSystemService<ClipboardManager>()!!
        petProgress = PetProgress(this)
        den = Den(this)

        binding.homeBed.setImageBitmap(PixelArt.load(this, "home/bed.png"))
        binding.homeBowl.setImageBitmap(PixelArt.load(this, "home/bowl.png"))
        prepareBark()
        ensureMonitoring()
        setupActions()
        maybeAskBatteryExemption()
    }

    override fun onDestroy() {
        soundPool?.release()
        soundPool = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        binding.petCanvas.onResume()
        AppVisibility.hasVisibleActivity = true
        clipboardManager.addPrimaryClipChangedListener(foregroundClipListener)
        showPet(announce = true)
        startIdleMotion()
        captureCurrentClipboard()
    }

    override fun onPause() {
        binding.petCanvas.onPause()
        AppVisibility.hasVisibleActivity = false
        clipboardManager.removePrimaryClipChangedListener(foregroundClipListener)
        stopIdleMotion()
        super.onPause()
    }

    private fun setupActions() {
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.feedButton.setOnClickListener { feedDoner() }
        binding.bathButton.setOnClickListener { bathe() }
        binding.frameText.setOnClickListener {
            startActivity(Intent(this, FramesActivity::class.java))
        }
        binding.nameRow.setOnClickListener { renamePet() }
        binding.petCanvas.setOnClickListener { nuzzle() }
        binding.tabHome.setOnClickListener { showTab(HomeSections.Tab.HOME) }
        binding.tabStore.setOnClickListener { showTab(HomeSections.Tab.STORE) }
        binding.tabTasks.setOnClickListener { showTab(HomeSections.Tab.TASKS) }
        binding.tabCatalog.setOnClickListener { showTab(HomeSections.Tab.CATALOG) }
        binding.tabGames.setOnClickListener { showTab(HomeSections.Tab.GAMES) }
    }

    private fun prepareBark() {
        val pool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        for (res in DogRoster.all.map { it.barkRes }.distinct()) {
            barkIds[res] = pool.load(this, res, 1)
        }
        soundPool = pool
    }

    private fun bark() {
        val id = barkIds[companion.barkRes] ?: return
        // Recorded pitch. Stretching a real bark makes it sound like a scrape.
        soundPool?.play(id, 1f, 1f, 1, 0, 1f)
    }

    private fun openGame(activity: Class<*>) {
        startActivity(
            Intent(this, activity)
                .putExtra(FoodDropActivity.EXTRA_PORTRAIT, companion.portraitRes)
                .putExtra(FoodDropActivity.EXTRA_FOOD, companion.foodRes)
                .putExtra(FoodDropActivity.EXTRA_MODEL, companion.model)
        )
    }

    private fun ensureMonitoring() {
        if (!prefs.getBoolean(KEY_MONITORING_ENABLED, true)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ClipboardWatcherService.start(this)
        }
    }

    private val sections = object : HomeSections.Callbacks {
        override fun buy(dog: DogCompanion) {
            val result = den.buy(dog.stableId(), dog.goldPrice, dog.gemPrice, getString(dog.nameRes))
            val message = when (result) {
                "ok" -> getString(R.string.store_got, getString(dog.nameRes))
                "gems" -> getString(R.string.store_need_gems)
                "owned" -> getString(R.string.store_owned)
                else -> getString(R.string.store_need_gold)
            }
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            if (result == "ok") {
                bindHome()
                showTab(tab)
            }
        }

        override fun select(dog: DogCompanion) {
            den.setActive(dog.stableId())
            bindHome()
            Toast.makeText(
                this@MainActivity,
                getString(R.string.collection_selected, den.displayName(dog.stableId(), getString(dog.nameRes))),
                Toast.LENGTH_SHORT
            ).show()
            showTab(tab)
        }

        override fun play(activity: Class<*>) {
            openGame(activity)
        }

        override fun achievements() {
            startActivity(Intent(this@MainActivity, AchievementsActivity::class.java))
        }
    }

    private fun showPet(announce: Boolean) {
        if (!den.onboarded()) {
            showOnboarding()
            com.clipsmile.app.widget.PetWidgetProvider.updateAll(this)
            ClipboardWatcherService.refresh(this)
            return
        }
        binding.onboardHost.visibility = View.GONE
        binding.onboardHost.removeAllViews()
        den.recordVisit()
        bindHome()
        if (announce) {
            den.takeNote()?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        }
        if (tab == HomeSections.Tab.HOME) paintTabs() else showTab(tab)
        if (!den.tutorialDone() && binding.tutorialHost.visibility != View.VISIBLE) {
            showTutorial()
        }
        com.clipsmile.app.widget.PetWidgetProvider.updateAll(this)
        ClipboardWatcherService.refresh(this)
    }

    private fun bindHome() {
        companion = DogRoster.byId(den.activeId())
        val id = companion.stableId()
        playerLevel = den.level(id)
        val look = FrameLooks.forLevel(playerLevel)
        binding.streakText.text = den.streak().toString()
        binding.goldText.text = den.gold().toString()
        binding.gemText.text = den.gems().toString()
        binding.levelText.text = playerLevel.toString()
        binding.xpBar.max = den.need(id)
        binding.xpBar.progress = den.into(id)
        binding.xpText.text = getString(R.string.xp_line, den.into(id), den.need(id))
        binding.petNameText.text = den.displayName(id, getString(companion.nameRes))
        binding.dogTitle.setText(companion.titleRes)
        binding.frameText.text = getString(R.string.frames_chip, getString(look.nameRes))
        binding.frameText.setTextColor(look.ring)
        binding.fullnessBar.progress = petProgress.carePercent()
        binding.petMoodText.text = careLine()
        binding.feedButton.text = feedLabel()
        binding.bathButton.text = bathLabel()
        binding.feedButton.alpha = if (petProgress.charges("feed") == 0) 0.45f else 1f
        binding.bathButton.alpha = if (petProgress.charges("bath") == 0) 0.45f else 1f
        binding.feedDoner.setImageResource(companion.foodRes)
        binding.petCanvas.setModel(companion.model)
        binding.frameGlow.setFrame(look)
        val density = resources.displayMetrics.density
        binding.frameText.background = GradientDrawable().apply {
            cornerRadius = 16f * density
            setColor(0xFFFFFFFF.toInt())
            setStroke((look.widthDp * 0.55f * density).toInt().coerceAtLeast(3), look.ring)
        }
        if (playerLevel >= 22 && glowPulse?.isRunning != true) startIdleMotion()
    }

    private fun showTab(next: HomeSections.Tab) {
        if (!den.onboarded()) return
        tab = next
        paintTabs()
        val home = next == HomeSections.Tab.HOME
        binding.homeDock.visibility = if (home) View.VISIBLE else View.GONE
        binding.namePlate.visibility = if (home) View.VISIBLE else View.GONE
        binding.sectionHost.visibility = if (home) View.GONE else View.VISIBLE
        if (!home) HomeSections.show(binding.sectionHost, next, den, sections)
    }

    private fun paintTabs() {
        val on = 0xFF1C4F80.toInt()
        val off = 0xFF8A6844.toInt()
        binding.tabHome.setTextColor(if (tab == HomeSections.Tab.HOME) on else off)
        binding.tabStore.setTextColor(if (tab == HomeSections.Tab.STORE) on else off)
        binding.tabTasks.setTextColor(if (tab == HomeSections.Tab.TASKS) on else off)
        binding.tabCatalog.setTextColor(if (tab == HomeSections.Tab.CATALOG) on else off)
        binding.tabGames.setTextColor(if (tab == HomeSections.Tab.GAMES) on else off)
    }

    private fun showOnboarding() {
        binding.tutorialHost.visibility = View.GONE
        binding.onboardHost.visibility = View.VISIBLE
        val host = binding.onboardHost
        host.removeAllViews()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(36), dp(20), dp(28))
        }
        val scroll = ScrollView(this)
        scroll.addView(column)
        host.addView(scroll, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        val picked = pickedStarter?.let { DogRoster.byId(it) }
        if (picked == null) {
            column.addView(heading(R.string.pick_title))
            for (dog in DogRoster.all.filter { it.starter }) {
                column.addView(starterCard(dog))
            }
            return
        }
        column.addView(ImageView(this).apply {
            setImageResource(picked.portraitRes)
            layoutParams = LinearLayout.LayoutParams(dp(120), dp(120)).apply { bottomMargin = dp(12) }
        })
        column.addView(heading(R.string.pick_name_title))
        val input = EditText(this).apply {
            setTextColor(0xFF3B2918.toInt())
            setHintTextColor(0xFF8A6844.toInt())
            hint = getString(picked.nameRes)
            setBackgroundResource(R.drawable.bg_hud_pill)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setSingleLine()
            textSize = 18f
        }
        column.addView(input, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })
        column.addView(TextView(this).apply {
            setText(R.string.pick_go)
            gravity = Gravity.CENTER
            setTextColor(0xFF173307.toInt())
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setBackgroundResource(R.drawable.bg_candy_green)
            setPadding(dp(12), dp(14), dp(12), dp(14))
            setOnClickListener {
                val name = input.text?.toString().orEmpty().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this@MainActivity, R.string.pick_need_name, Toast.LENGTH_SHORT).show()
                } else {
                    den.begin(picked.stableId(), name)
                    pickedStarter = null
                    showPet(announce = true)
                }
            }
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(16) })
        column.addView(TextView(this).apply {
            text = "←"
            gravity = Gravity.CENTER
            setTextColor(0xFF1C4F80.toInt())
            textSize = 22f
            setPadding(dp(12), dp(16), dp(12), dp(8))
            setOnClickListener {
                pickedStarter = null
                showOnboarding()
            }
        })
    }

    private fun starterCard(dog: DogCompanion): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_collection_card)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            addView(ImageView(this@MainActivity).apply {
                setImageResource(dog.portraitRes)
                layoutParams = LinearLayout.LayoutParams(dp(72), dp(72))
            })
            val text = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
            }
            text.addView(TextView(this@MainActivity).apply {
                setText(dog.nameRes)
                setTextColor(0xFF3B2918.toInt())
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
            })
            text.addView(TextView(this@MainActivity).apply {
                setText(dog.titleRes)
                setTextColor(0xFF8A6844.toInt())
                textSize = 14f
            })
            addView(text)
            setOnClickListener {
                pickedStarter = dog.stableId()
                showOnboarding()
            }
        }
    }

    private fun heading(textRes: Int) = TextView(this).apply {
        setText(textRes)
        setTextColor(0xFF1C4F80.toInt())
        textSize = 28f
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER
    }

    private fun showTutorial() {
        tutorialStep = 0
        binding.tutorialHost.visibility = View.VISIBLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.contentRoot.setRenderEffect(
                RenderEffect.createBlurEffect(16f, 16f, Shader.TileMode.CLAMP)
            )
        }
        binding.tutorialHost.setOnClickListener { advanceTutorial() }
        renderTutorial()
    }

    private fun advanceTutorial() {
        tutorialStep += 1
        if (tutorialStep >= tutorialAnchors().size) finishTutorial() else renderTutorial()
    }

    private fun finishTutorial() {
        den.markTutorial()
        tutorialGen += 1
        binding.tutorialHost.setOnClickListener(null)
        binding.tutorialHost.removeAllViews()
        binding.tutorialHost.visibility = View.GONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.contentRoot.setRenderEffect(null)
        }
    }

    private fun tutorialAnchors(): List<Pair<View, Int>> = listOf(
        binding.petCanvas to R.string.tutorial_pet,
        binding.streakPill to R.string.tutorial_streak,
        binding.goldPill to R.string.tutorial_gold,
        binding.gemPill to R.string.tutorial_gem,
        binding.levelRow to R.string.tutorial_level,
        binding.tabBar to R.string.tutorial_tabs
    )

    private fun renderTutorial() {
        val host = binding.tutorialHost
        val gen = ++tutorialGen
        val (anchor, textRes) = tutorialAnchors()[tutorialStep]
        host.removeAllViews()
        host.post {
            if (gen != tutorialGen || host.visibility != View.VISIBLE) return@post
            val anchorLoc = IntArray(2)
            val hostLoc = IntArray(2)
            anchor.getLocationOnScreen(anchorLoc)
            host.getLocationOnScreen(hostLoc)
            val centerX = anchorLoc[0] - hostLoc[0] + anchor.width / 2
            val centerY = anchorLoc[1] - hostLoc[1] + anchor.height / 2
            val below = centerY < host.height * 0.55f
            val arrow = TextView(this).apply {
                text = if (below) "▼" else "▲"
                setTextColor(0xFFFFE08A.toInt())
                textSize = 28f
                gravity = Gravity.CENTER
            }
            val arrowSize = dp(36)
            val arrowTop = if (below) centerY + anchor.height / 4 else centerY - anchor.height / 4 - arrowSize
            host.addView(arrow, FrameLayout.LayoutParams(arrowSize, arrowSize).apply {
                leftMargin = (centerX - arrowSize / 2).coerceAtLeast(0)
                topMargin = arrowTop.coerceAtLeast(0)
            })
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_name_plate)
                setPadding(dp(16), dp(14), dp(16), dp(14))
                isClickable = false
            }
            card.addView(TextView(this).apply {
                setText(textRes)
                setTextColor(0xFF3B2918.toInt())
                textSize = 16f
            })
            card.addView(TextView(this).apply {
                setText(R.string.tutorial_next)
                setTextColor(0xFF1C4F80.toInt())
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.END
                setPadding(0, dp(8), 0, 0)
            })
            val cardTop = if (below) arrowTop + arrowSize + dp(4) else (arrowTop - dp(132)).coerceAtLeast(dp(16))
            host.addView(card, FrameLayout.LayoutParams(
                host.width - dp(48),
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dp(24)
                topMargin = cardTop
            })
        }
    }

    private fun grant(kind: Den.Kind, amount: Int) {
        if (!den.onboarded()) return
        val before = den.level(den.activeId())
        den.bump(kind, amount)
        bindHome()
        den.takeNote()?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        val after = den.level(den.activeId())
        if (after > before) {
            Toast.makeText(this, getString(R.string.level_up, after), Toast.LENGTH_SHORT).show()
        }
        if (tab != HomeSections.Tab.HOME) showTab(tab)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun careLine(): String {
        return getString(
            R.string.care_left,
            petProgress.charges("nuzzle"),
            petProgress.charges("feed"),
            petProgress.charges("bath")
        )
    }

    private fun feedLabel(): String {
        val left = petProgress.charges("feed")
        val food = getString(companion.foodNameRes)
        return if (left <= 0) getString(R.string.feed_done) else getString(R.string.action_feed_count, food, left)
    }

    private fun bathLabel(): String {
        val left = petProgress.charges("bath")
        return if (left <= 0) getString(R.string.bath_done) else getString(R.string.action_bath_count, left)
    }

    private fun renamePet() {
        if (!den.onboarded()) return
        val id = companion.stableId()
        val input = EditText(this).apply {
            setText(den.displayName(id, getString(companion.nameRes)))
            setSelection(text.length)
            setSingleLine()
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_rename_pet)
            .setView(input)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save_name) { _, _ ->
                den.setName(id, input.text?.toString().orEmpty())
                binding.petNameText.text = den.displayName(id, getString(companion.nameRes))
                com.clipsmile.app.widget.PetWidgetProvider.updateAll(this)
                ClipboardWatcherService.refresh(this)
            }
            .show()
    }

    private fun nuzzle() {
        if (feeding || bathing || !den.onboarded()) return
        val life = petProgress.tryNuzzle()
        bark()
        binding.hearts.burst()
        binding.petCanvas.playHappy()
        binding.petMoodText.setText(R.string.pet_loved)
        if (life < 0) return
        grant(Den.Kind.NUZZLE, 1)
        binding.petMoodText.setText(R.string.pet_loved)
        Achievements.celebrate(this, Achievements.PET, life)
    }

    private fun startIdleMotion() {
        if (glowPulse?.isRunning == true) return
        glowPulse?.cancel()
        if (playerLevel >= 22) {
            glowPulse = ObjectAnimator.ofFloat(binding.frameGlow, View.ALPHA, 0.55f, 1f).apply {
                duration = 900
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        } else {
            binding.frameGlow.alpha = 1f
        }
    }

    private fun stopIdleMotion() {
        glowPulse?.cancel()
        glowPulse = null
        binding.bubbles.stop()
        binding.hearts.stop()
        binding.feedDoner.animate().cancel()
        binding.feedDoner.visibility = View.INVISIBLE
        feeding = false
        bathing = false
    }

    private fun feedDoner() {
        if (feeding || bathing) return
        if (!petProgress.tryFeed()) {
            Toast.makeText(this, R.string.feed_done, Toast.LENGTH_SHORT).show()
            return
        }
        grant(Den.Kind.FEED, 1)
        feeding = true
        val doner = binding.feedDoner
        doner.animate().cancel()
        doner.visibility = View.VISIBLE
        doner.alpha = 1f
        doner.scaleX = 1f
        doner.scaleY = 1f
        doner.translationX = 48f
        doner.translationY = 36f
        doner.animate()
            .translationX(-6f)
            .translationY(-70f)
            .scaleX(0.4f)
            .scaleY(0.4f)
            .setDuration(620)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                binding.fullnessBar.progress = petProgress.carePercent()
                binding.petMoodText.setText(companion.fedRes)
                binding.feedButton.text = feedLabel()
                binding.feedButton.alpha = if (petProgress.charges("feed") == 0) 0.45f else 1f
                Achievements.celebrate(this@MainActivity, Achievements.MEAL, petProgress.lifetime("feed"))
                bark()
                binding.petCanvas.playHappy()
                doner.animate().alpha(0f).setDuration(160).withEndAction {
                    doner.visibility = View.INVISIBLE
                    doner.translationX = 0f
                    doner.translationY = 0f
                    doner.scaleX = 1f
                    doner.scaleY = 1f
                    feeding = false
                }.start()
            }
            .start()
    }

    private fun bathe() {
        if (feeding || bathing) return
        if (!petProgress.tryBath()) {
            Toast.makeText(this, R.string.bath_done, Toast.LENGTH_SHORT).show()
            return
        }
        grant(Den.Kind.BATH, 1)
        bathing = true
        bark()
        binding.petCanvas.playShake()
        binding.bubbles.burst {
            bathing = false
            binding.fullnessBar.progress = petProgress.carePercent()
            binding.petMoodText.setText(R.string.bath_fresh)
            binding.bathButton.text = bathLabel()
            binding.bathButton.alpha = if (petProgress.charges("bath") == 0) 0.45f else 1f
            Achievements.celebrate(this@MainActivity, Achievements.WASH, petProgress.lifetime("bath"))
        }
    }

    private fun captureCurrentClipboard() {
        val clip = clipboardManager.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: return
        ClipCapturePipeline.captureAsync(lifecycleScope, this, text)
    }

    private fun maybeAskBatteryExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (prefs.getBoolean(KEY_BATTERY_ASKED, false)) return
        val power = getSystemService(PowerManager::class.java)
        if (power.isIgnoringBatteryOptimizations(packageName)) return
        prefs.edit().putBoolean(KEY_BATTERY_ASKED, true).apply()
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = android.net.Uri.parse("package:$packageName")
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // The settings screen is missing on this device.
        }
    }

    companion object {
        private const val PREFS_NAME = "clipsmile_prefs"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        private const val KEY_BATTERY_ASKED = "battery_prompt_shown"
    }
}

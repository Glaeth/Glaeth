package com.clipsmile.app.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.clipsmile.app.AppVisibility
import com.clipsmile.app.R
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

    private var idleBob: ObjectAnimator? = null
    private var idleTilt: ObjectAnimator? = null
    private var idleYaw: ObjectAnimator? = null
    private var idlePitch: ObjectAnimator? = null
    private var glowPulse: ObjectAnimator? = null
    private var feeding = false
    private var bathing = false
    private var loving = false
    private var playerLevel = 1
    private var companion = DogRoster.all.first()
    private var soundPool: SoundPool? = null
    private var barkId = 0
    private var barkReady = false

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

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        clipboardManager = getSystemService<ClipboardManager>()!!
        petProgress = PetProgress(this)

        val distance = 9000f * resources.displayMetrics.density
        binding.petPivot.cameraDistance = distance
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
        AppVisibility.hasVisibleActivity = true
        clipboardManager.addPrimaryClipChangedListener(foregroundClipListener)
        showPet()
        startIdleMotion()
        captureCurrentClipboard()
    }

    override fun onPause() {
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
        binding.collectionButton.setOnClickListener {
            startActivity(Intent(this, CollectionActivity::class.java))
        }
        binding.nameRow.setOnClickListener { renamePet() }
        binding.petPivot.setOnClickListener { nuzzle() }
        binding.playDrop.setOnClickListener { openGame(FoodDropActivity::class.java) }
        binding.playFlappy.setOnClickListener { openGame(FlappyDogActivity::class.java) }
    }

    private fun prepareBark() {
        val pool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && sampleId == barkId) barkReady = true
        }
        barkId = pool.load(this, R.raw.dog_bark, 1)
        soundPool = pool
    }

    private fun bark(rate: Float) {
        if (!barkReady) return
        soundPool?.play(barkId, 0.85f, 0.85f, 1, 0, rate)
    }

    private fun openGame(activity: Class<*>) {
        startActivity(
            Intent(this, activity)
                .putExtra(FoodDropActivity.EXTRA_PORTRAIT, companion.portraitRes)
                .putExtra(FoodDropActivity.EXTRA_FOOD, companion.foodRes)
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

    private fun showPet() {
        val progress = petProgress.recordVisit()
        playerLevel = progress.level
        val gates = DogRoster.all.map { it.minLevel }
        petProgress.stampUnlocks(progress.level, gates)
        for (dog in DogRoster.all) {
            if (progress.level >= dog.minLevel) {
                petProgress.rememberName(dog.minLevel, getString(dog.nameRes))
            }
        }
        val newest = DogRoster.forLevel(progress.level)
        if (petProgress.claimFreshUnlock(newest.minLevel)) {
            petProgress.setSelectedLevel(newest.minLevel)
            Toast.makeText(
                this,
                getString(R.string.dog_unlocked, getString(newest.nameRes)),
                Toast.LENGTH_LONG
            ).show()
        }
        companion = currentDog(progress.level)
        val look = FrameLooks.forLevel(progress.level)
        binding.streakText.text = progress.streak.toString()
        binding.levelText.text = progress.level.toString()
        binding.petNameText.text = petProgress.displayName(companion.minLevel, getString(companion.nameRes))
        binding.dogTitle.setText(companion.titleRes)
        binding.frameText.setText(look.nameRes)
        binding.frameText.setTextColor(look.ring)
        binding.fullnessBar.progress = petProgress.carePercent()
        binding.petMoodText.text = careLine()
        binding.feedButton.text = feedLabel()
        binding.bathButton.text = bathLabel()
        binding.feedButton.alpha = if (petProgress.feedsToday() >= PetProgress.FEEDS_PER_DAY) 0.45f else 1f
        binding.bathButton.alpha = if (petProgress.bathsToday() >= PetProgress.BATHS_PER_DAY) 0.45f else 1f
        binding.feedDoner.setImageResource(companion.foodRes)
        binding.petImage.setImageResource(companion.portraitRes)
        binding.petImage.scaleType = if (companion.photo) {
            ImageView.ScaleType.CENTER_CROP
        } else {
            ImageView.ScaleType.FIT_CENTER
        }
        val density = resources.displayMetrics.density
        binding.petImage.strokeWidth = look.widthDp * density
        binding.petImage.strokeColor = ColorStateList.valueOf(look.ring)
        binding.frameGlow.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(look.glow)
            setStroke(((look.widthDp + 3f) * density).toInt().coerceAtLeast(2), look.ring)
        }
        binding.dropBest.text = getString(R.string.game_best, GameScores.best(this, GameScores.BEST_DROP))
        binding.flappyBest.text = getString(R.string.game_best, GameScores.best(this, GameScores.BEST_FLAP))
    }

    private fun currentDog(level: Int): DogCompanion {
        val unlocked = DogRoster.all.filter { level >= it.minLevel }
        val picked = petProgress.selectedLevel()
        return unlocked.find { it.minLevel == picked } ?: unlocked.last()
    }

    private fun careLine(): String {
        val feedsLeft = (PetProgress.FEEDS_PER_DAY - petProgress.feedsToday()).coerceAtLeast(0)
        val bathsLeft = (PetProgress.BATHS_PER_DAY - petProgress.bathsToday()).coerceAtLeast(0)
        return if (feedsLeft == 0 && bathsLeft == 0) {
            getString(R.string.care_done)
        } else {
            getString(R.string.care_left, feedsLeft, bathsLeft)
        }
    }

    private fun feedLabel(): String {
        val left = PetProgress.FEEDS_PER_DAY - petProgress.feedsToday()
        val food = getString(companion.foodNameRes)
        return if (left <= 0) getString(R.string.feed_done) else getString(R.string.action_feed_count, food, left)
    }

    private fun bathLabel(): String {
        val left = PetProgress.BATHS_PER_DAY - petProgress.bathsToday()
        return if (left <= 0) getString(R.string.bath_done) else getString(R.string.action_bath_count, left)
    }

    private fun renamePet() {
        val input = EditText(this).apply {
            setText(petProgress.displayName(companion.minLevel, getString(companion.nameRes)))
            setSelection(text.length)
            setSingleLine()
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_rename_pet)
            .setView(input)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save_name) { _, _ ->
                petProgress.setName(companion.minLevel, input.text?.toString().orEmpty())
                binding.petNameText.text = petProgress.displayName(companion.minLevel, getString(companion.nameRes))
            }
            .show()
    }

    private fun nuzzle() {
        if (feeding || bathing || loving) return
        loving = true
        bark(1f)
        pauseIdle()
        binding.hearts.burst()
        binding.petMoodText.setText(R.string.pet_loved)
        val pet = binding.petPivot
        pet.animate().cancel()
        pet.rotation = 0f
        pet.animate()
            .translationY(-46f)
            .rotationY(22f)
            .rotationX(-8f)
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(180)
            .withEndAction {
                syncShadow()
                pet.animate()
                    .rotationY(-22f)
                    .rotationX(10f)
                    .setDuration(160)
                    .withEndAction {
                        pet.animate()
                            .translationY(0f)
                            .rotationY(0f)
                            .rotationX(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(240)
                            .withEndAction {
                                loving = false
                                syncShadow()
                                resumeIdle()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun startIdleMotion() {
        if (idleBob?.isRunning == true) return
        val pet = binding.petPivot
        idleBob = ObjectAnimator.ofFloat(pet, View.TRANSLATION_Y, 0f, -16f).apply {
            duration = 900
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { syncShadow() }
            start()
        }
        idleTilt = ObjectAnimator.ofFloat(pet, View.ROTATION, -7f, 7f).apply {
            duration = 520
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        idleYaw = ObjectAnimator.ofFloat(pet, View.ROTATION_Y, -18f, 18f).apply {
            duration = 1700
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { syncShadow() }
            start()
        }
        idlePitch = ObjectAnimator.ofFloat(pet, View.ROTATION_X, 12f, -6f).apply {
            duration = 1400
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { syncShadow() }
            start()
        }
        glowPulse?.cancel()
        if (playerLevel >= 150) {
            glowPulse = ObjectAnimator.ofFloat(binding.frameGlow, View.ALPHA, 0.5f, 1f).apply {
                duration = 900
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        } else {
            binding.frameGlow.alpha = 1f
        }
    }

    private fun syncShadow() {
        val pet = binding.petPivot
        val lift = (-pet.translationY).coerceAtLeast(0f)
        binding.groundShadow.translationX = -pet.rotationY * 1.5f
        val scale = (1f - lift / 90f).coerceIn(0.62f, 1f)
        binding.groundShadow.scaleX = scale
        binding.groundShadow.scaleY = scale
        binding.groundShadow.alpha = (0.9f - lift / 140f).coerceIn(0.35f, 0.9f)
        binding.petSheen.translationX = pet.rotationY * 0.45f
        binding.petSheen.translationY = pet.rotationX * 0.35f
    }

    private fun pauseIdle() {
        idleBob?.cancel()
        idleTilt?.cancel()
        idleYaw?.cancel()
        idlePitch?.cancel()
        idleBob = null
        idleTilt = null
        idleYaw = null
        idlePitch = null
    }

    private fun resumeIdle() {
        if (isFinishing || isDestroyed) return
        startIdleMotion()
    }

    private fun stopIdleMotion() {
        pauseIdle()
        glowPulse?.cancel()
        glowPulse = null
        binding.bubbles.stop()
        binding.hearts.stop()
        binding.feedDoner.animate().cancel()
        binding.petPivot.animate().cancel()
        binding.petPivot.translationY = 0f
        binding.petPivot.rotation = 0f
        binding.petPivot.rotationX = 0f
        binding.petPivot.rotationY = 0f
        binding.petPivot.scaleX = 1f
        binding.petPivot.scaleY = 1f
        binding.petSheen.translationX = 0f
        binding.petSheen.translationY = 0f
        binding.groundShadow.translationX = 0f
        binding.groundShadow.scaleX = 1f
        binding.groundShadow.scaleY = 1f
        binding.groundShadow.alpha = 0.9f
        binding.feedDoner.visibility = View.INVISIBLE
        feeding = false
        bathing = false
        loving = false
    }

    private fun feedDoner() {
        if (feeding || bathing) return
        if (!petProgress.tryFeed()) {
            Toast.makeText(this, R.string.feed_done, Toast.LENGTH_SHORT).show()
            return
        }
        feeding = true
        pauseIdle()

        val pet = binding.petPivot
        pet.rotationX = 0f
        pet.rotationY = 0f
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
                binding.feedButton.alpha = if (petProgress.feedsToday() >= PetProgress.FEEDS_PER_DAY) 0.45f else 1f
                bark(0.86f)
                doner.animate().alpha(0f).setDuration(160).withEndAction {
                    doner.visibility = View.INVISIBLE
                    doner.translationX = 0f
                    doner.translationY = 0f
                    doner.scaleX = 1f
                    doner.scaleY = 1f
                }.start()
                pet.animate()
                    .scaleX(1.16f)
                    .scaleY(0.86f)
                    .setDuration(110)
                    .withEndAction {
                        pet.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .rotation(10f)
                            .setDuration(150)
                            .withEndAction {
                                pet.animate().rotation(0f).rotationY(0f).rotationX(0f).setDuration(160).withEndAction {
                                    feeding = false
                                    syncShadow()
                                    resumeIdle()
                                }.start()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun bathe() {
        if (feeding || bathing) return
        if (!petProgress.tryBath()) {
            Toast.makeText(this, R.string.bath_done, Toast.LENGTH_SHORT).show()
            return
        }
        bathing = true
        pauseIdle()
        bark(1.12f)
        val pet = binding.petPivot
        pet.rotationX = 0f
        pet.rotationY = 0f
        pet.animate().cancel()
        pet.animate().rotation(-8f).setDuration(120).withEndAction {
            pet.animate().rotation(8f).setDuration(140).withEndAction {
                pet.animate().rotation(-5f).setDuration(120).start()
            }.start()
        }.start()
        binding.bubbles.burst {
            pet.animate().rotation(0f).setDuration(180).withEndAction {
                bathing = false
                binding.fullnessBar.progress = petProgress.carePercent()
                binding.petMoodText.setText(R.string.bath_fresh)
                binding.bathButton.text = bathLabel()
                binding.bathButton.alpha = if (petProgress.bathsToday() >= PetProgress.BATHS_PER_DAY) 0.45f else 1f
                resumeIdle()
            }.start()
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

package com.clipsmile.app.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
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
    private var feeding = false
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
        binding.nameRow.setOnClickListener { renamePet() }
        binding.petImage.setOnClickListener { nuzzle() }
        binding.playDrop.setOnClickListener { openGame(FoodDropActivity::class.java) }
        binding.playFlappy.setOnClickListener { openGame(FlappyDogActivity::class.java) }
        listOf(binding.dog0, binding.dog1, binding.dog2, binding.dog3).forEachIndexed { index, view ->
            view.setOnClickListener { pickDog(index) }
        }
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

    private fun pickDog(index: Int) {
        val dog = DogRoster.all[index]
        val level = petProgress.level()
        if (level < dog.minLevel) {
            Toast.makeText(this, getString(R.string.dog_locked, dog.minLevel), Toast.LENGTH_SHORT).show()
            return
        }
        prefs.edit().putInt(KEY_DOG, dog.minLevel).apply()
        showPet()
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
        companion = currentDog(progress.level)
        binding.streakText.text = progress.streak.toString()
        binding.levelText.text = progress.level.toString()
        binding.petNameText.text = progress.name
        binding.dogTitle.setText(companion.titleRes)
        binding.fullnessBar.progress = progress.fullness
        binding.petMoodText.text = moodLine(progress.fullness)
        binding.feedButton.text = getString(R.string.action_feed_named, getString(companion.foodNameRes))
        binding.feedDoner.setImageResource(companion.foodRes)
        binding.petImage.setImageResource(companion.portraitRes)
        binding.petImage.scaleType = if (companion.photo) {
            ImageView.ScaleType.CENTER_CROP
        } else {
            ImageView.ScaleType.FIT_CENTER
        }
        binding.petImage.strokeWidth = if (companion.photo) 5f * resources.displayMetrics.density else 0f
        binding.dropBest.text = getString(R.string.game_best, GameScores.best(this, GameScores.BEST_DROP))
        binding.flappyBest.text = getString(R.string.game_best, GameScores.best(this, GameScores.BEST_FLAP))
        bindRoster(progress.level)
    }

    private fun currentDog(level: Int): DogCompanion {
        val unlocked = DogRoster.all.filter { level >= it.minLevel }
        val picked = prefs.getInt(KEY_DOG, unlocked.last().minLevel)
        return unlocked.find { it.minLevel == picked } ?: unlocked.last()
    }

    private fun bindRoster(level: Int) {
        val portraits = listOf(binding.dog0, binding.dog1, binding.dog2, binding.dog3)
        val locks = listOf(binding.lock0, binding.lock1, binding.lock2, binding.lock3)
        DogRoster.all.forEachIndexed { index, dog ->
            val open = level >= dog.minLevel
            val selected = dog.minLevel == companion.minLevel
            portraits[index].alpha = if (open) 1f else 0.35f
            portraits[index].setBackgroundResource(if (selected) R.drawable.bg_slot_on else 0)
            locks[index].text = if (open) getString(dog.titleRes) else getString(R.string.dog_level_short, dog.minLevel)
        }
    }

    private fun moodLine(fullness: Int): String {
        val food = getString(companion.foodNameRes)
        return when {
            fullness >= 80 -> getString(R.string.pet_mood_full, food)
            fullness >= 40 -> getString(R.string.pet_mood_ok)
            fullness >= 15 -> getString(R.string.pet_mood_hungry, food)
            else -> getString(R.string.pet_mood_starving, food)
        }
    }

    private fun renamePet() {
        val input = EditText(this).apply {
            setText(petProgress.petName)
            setSelection(text.length)
            setSingleLine()
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_rename_pet)
            .setView(input)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save_name) { _, _ ->
                petProgress.petName = input.text?.toString().orEmpty()
                binding.petNameText.text = petProgress.petName
            }
            .show()
    }

    private fun nuzzle() {
        if (feeding) return
        bark(1.08f)
        val pet = binding.petImage
        pet.animate().cancel()
        pet.animate()
            .scaleX(1.08f)
            .scaleY(0.94f)
            .setDuration(90)
            .withEndAction {
                pet.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
            }
            .start()
    }

    private fun startIdleMotion() {
        if (idleBob?.isRunning == true) return
        val pet = binding.petImage
        idleBob = ObjectAnimator.ofFloat(pet, View.TRANSLATION_Y, 0f, -18f).apply {
            duration = 780
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        idleTilt = ObjectAnimator.ofFloat(pet, View.ROTATION, -4f, 4f).apply {
            duration = 1100
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopIdleMotion() {
        idleBob?.cancel()
        idleTilt?.cancel()
        idleBob = null
        idleTilt = null
        binding.feedDoner.animate().cancel()
        binding.petImage.animate().cancel()
        binding.petImage.translationY = 0f
        binding.petImage.rotation = 0f
        binding.petImage.scaleX = 1f
        binding.petImage.scaleY = 1f
        binding.feedDoner.visibility = View.INVISIBLE
        feeding = false
    }

    private fun feedDoner() {
        if (feeding) return
        feeding = true
        idleBob?.pause()
        idleTilt?.pause()

        val pet = binding.petImage
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
                val fullness = petProgress.feed()
                binding.fullnessBar.progress = fullness
                binding.petMoodText.setText(companion.fedRes)
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
                                pet.animate().rotation(0f).setDuration(160).withEndAction {
                                    feeding = false
                                    idleBob?.resume()
                                    idleTilt?.resume()
                                }.start()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
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
        private const val KEY_DOG = "selected_dog_level"
    }
}

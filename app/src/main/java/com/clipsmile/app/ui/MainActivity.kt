package com.clipsmile.app.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clipsmile.app.AppVisibility
import com.clipsmile.app.R
import com.clipsmile.app.data.ClipEntry
import com.clipsmile.app.data.ClipRepository
import com.clipsmile.app.data.RemoteSyncPrefs
import com.clipsmile.app.databinding.ActivityMainBinding
import com.clipsmile.app.service.ClipboardWatcherService
import com.clipsmile.app.util.ClipCapturePipeline
import com.clipsmile.app.util.ClipFormatter
import com.clipsmile.app.util.EmailExportHelper
import com.clipsmile.app.util.WhatsAppShareHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: ClipRepository
    private lateinit var adapter: ClipAdapter
    private lateinit var prefs: SharedPreferences
    private lateinit var clipboardManager: ClipboardManager

    private var latestEntries: List<ClipEntry> = emptyList()

    /** Fallback listener: captures copies made while this screen is visible,
     * which also works reliably on Android 10+ where background clipboard
     * reads are restricted to the focused app. */
    private val foregroundClipListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboardManager.primaryClip ?: return@OnPrimaryClipChangedListener
        if (clip.itemCount == 0) return@OnPrimaryClipChangedListener
        val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: return@OnPrimaryClipChangedListener
        ClipCapturePipeline.captureAsync(lifecycleScope, this, text)
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                startMonitoringService()
            } else {
                binding.monitoringSwitch.isChecked = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = ClipRepository.getInstance(this)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        clipboardManager = getSystemService<ClipboardManager>()!!

        setupRecyclerView()
        setupMonitoringSwitch()
        setupActionButtons()
        observeEntries()
        maybeAskBatteryExemption()
    }

    override fun onResume() {
        super.onResume()
        AppVisibility.hasVisibleActivity = true
        clipboardManager.addPrimaryClipChangedListener(foregroundClipListener)
        updateOverlayHint()
        updateServerStatus()
        captureCurrentClipboard()
    }

    override fun onPause() {
        AppVisibility.hasVisibleActivity = false
        clipboardManager.removePrimaryClipChangedListener(foregroundClipListener)
        super.onPause()
    }

    private fun setupRecyclerView() {
        adapter = ClipAdapter(
            onCopyClick = ::copyEntryToClipboard,
            onShareClick = { entry -> WhatsAppShareHelper.shareText(this, entry.content) },
            onDeleteClick = { entry -> confirmDeleteItem(entry) }
        )
        binding.clipRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.clipRecyclerView.adapter = adapter

        ItemTouchHelper(SwipeToDeleteCallback { position ->
            if (position == RecyclerView.NO_POSITION) return@SwipeToDeleteCallback
            val entry = adapter.entryAt(position)
            deleteEntry(entry)
        }).attachToRecyclerView(binding.clipRecyclerView)
    }

    private fun setupMonitoringSwitch() {
        val shouldMonitor = prefs.getBoolean(KEY_MONITORING_ENABLED, true)
        binding.monitoringSwitch.isChecked = shouldMonitor
        updateMonitoringStatusText(shouldMonitor)
        if (shouldMonitor) {
            requestNotificationPermissionAndStart()
        }

        binding.monitoringSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_MONITORING_ENABLED, isChecked).apply()
            updateMonitoringStatusText(isChecked)
            if (isChecked) {
                requestNotificationPermissionAndStart()
            } else {
                ClipboardWatcherService.stop(this)
            }
        }
    }

    private fun updateMonitoringStatusText(isOn: Boolean) {
        binding.monitoringStatusText.text =
            getString(if (isOn) R.string.monitoring_on else R.string.monitoring_off)
        binding.monitoringDot.alpha = if (isOn) 1f else 0.35f
    }

    private fun requestNotificationPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startMonitoringService()
        }
    }

    private fun startMonitoringService() {
        ClipboardWatcherService.start(this)
    }

    private fun setupActionButtons() {
        binding.sendAllWhatsAppButton.setOnClickListener {
            val text = ClipFormatter.joinForExport(latestEntries)
            WhatsAppShareHelper.shareText(this, text)
        }

        binding.exportEmailButton.setOnClickListener {
            val text = ClipFormatter.joinForExport(latestEntries)
            EmailExportHelper.export(this, text)
        }

        binding.clearAllButton.setOnClickListener { confirmClearAll() }
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.overlayHintText.setOnClickListener { openOverlaySettings() }
        binding.autoSendStatusText.setOnClickListener {
            val url = RemoteSyncPrefs(this).channelUrl
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun updateServerStatus() {
        val sync = RemoteSyncPrefs(this)
        binding.autoSendStatusText.text = if (sync.isEnabled) {
            getString(R.string.server_status_on, sync.channelUrl)
        } else {
            getString(R.string.server_status_off)
        }
    }

    private fun captureCurrentClipboard() {
        val clip = clipboardManager.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: return
        ClipCapturePipeline.captureAsync(lifecycleScope, this, text)
    }

    private fun updateOverlayHint() {
        val needsOverlay = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !Settings.canDrawOverlays(this)
        binding.overlayHintText.visibility = if (needsOverlay) View.VISIBLE else View.GONE
    }

    private fun maybeAskBatteryExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (prefs.getBoolean(KEY_BATTERY_ASKED, false)) return
        val power = getSystemService(PowerManager::class.java)
        if (power.isIgnoringBatteryOptimizations(packageName)) return
        prefs.edit().putBoolean(KEY_BATTERY_ASKED, true).apply()
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // The settings screen is missing on this device.
        }
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun observeEntries() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeEntries().collect { entries ->
                    latestEntries = entries
                    val newestFirst = entries.asReversed()
                    adapter.submitList(newestFirst)
                    binding.emptyStateLayout.visibility =
                        if (entries.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun copyEntryToClipboard(entry: ClipEntry) {
        val clip = ClipData.newPlainText(getString(R.string.app_name), entry.content)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(this, R.string.toast_copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun confirmDeleteItem(entry: ClipEntry) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_item_confirm_title)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_confirm_delete) { _, _ -> deleteEntry(entry) }
            .show()
    }

    private fun deleteEntry(entry: ClipEntry) {
        lifecycleScope.launch {
            repository.delete(entry)
            showUndoSnackbar(entry)
        }
    }

    private fun showUndoSnackbar(deletedEntry: ClipEntry) {
        Snackbar.make(binding.root, R.string.toast_item_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_undo) {
                lifecycleScope.launch { repository.restore(deletedEntry) }
            }
            .setAnchorView(binding.sendAllWhatsAppButton)
            .show()
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_all_confirm_title)
            .setMessage(R.string.clear_all_confirm_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_confirm_delete) { _, _ ->
                lifecycleScope.launch {
                    repository.clearAll()
                    Toast.makeText(this@MainActivity, R.string.toast_all_cleared, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    companion object {
        private const val PREFS_NAME = "clipsmile_prefs"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        private const val KEY_BATTERY_ASKED = "battery_prompt_shown"
    }
}

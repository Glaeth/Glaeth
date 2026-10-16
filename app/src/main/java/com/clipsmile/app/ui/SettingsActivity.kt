package com.clipsmile.app.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.widget.doAfterTextChanged
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
import com.clipsmile.app.data.RemoteSyncManager
import com.clipsmile.app.data.RemoteSyncPrefs
import com.clipsmile.app.data.SyncStatus
import com.clipsmile.app.databinding.ActivitySettingsBinding
import com.clipsmile.app.service.ClipboardWatcherService
import com.clipsmile.app.util.ClipFormatter
import com.clipsmile.app.util.EmailExportHelper
import com.clipsmile.app.util.WhatsAppShareHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: RemoteSyncPrefs
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var repository: ClipRepository
    private lateinit var adapter: ClipAdapter

    private var latestEntries: List<ClipEntry> = emptyList()
    private var suppressChannelWatcher = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                ClipboardWatcherService.start(this)
            } else {
                binding.monitoringSwitch.isChecked = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = RemoteSyncPrefs(this)
        clipboardManager = getSystemService<ClipboardManager>()!!
        repository = ClipRepository.getInstance(this)

        setupClipsSection()
        setupSyncSection()
        observeSyncStatus()

        binding.backButton.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        AppVisibility.hasVisibleActivity = true
    }

    override fun onPause() {
        AppVisibility.hasVisibleActivity = false
        super.onPause()
    }

    private fun setupClipsSection() {
        adapter = ClipAdapter(
            onCopyClick = { entry ->
                val clip = ClipData.newPlainText(getString(R.string.app_name), entry.content)
                clipboardManager.setPrimaryClip(clip)
                Toast.makeText(this, R.string.toast_copied_to_clipboard, Toast.LENGTH_SHORT).show()
            },
            onShareClick = { entry -> WhatsAppShareHelper.shareText(this, entry.content) },
            onDeleteClick = { entry -> confirmDeleteItem(entry) }
        )
        binding.clipRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.clipRecyclerView.adapter = adapter
        ItemTouchHelper(SwipeToDeleteCallback { position ->
            if (position == RecyclerView.NO_POSITION) return@SwipeToDeleteCallback
            deleteEntry(adapter.entryAt(position))
        }).attachToRecyclerView(binding.clipRecyclerView)

        val monitoringOn = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MONITORING_ENABLED, true)
        binding.monitoringSwitch.isChecked = monitoringOn
        updateMonitoringStatus(monitoringOn)
        binding.monitoringSwitch.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_MONITORING_ENABLED, isChecked)
                .apply()
            updateMonitoringStatus(isChecked)
            if (isChecked) requestNotificationPermissionAndStart() else ClipboardWatcherService.stop(this)
        }
        binding.overlayButton.setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
        binding.sendAllWhatsAppButton.setOnClickListener {
            WhatsAppShareHelper.shareText(this, ClipFormatter.joinForExport(latestEntries))
        }
        binding.exportEmailButton.setOnClickListener {
            EmailExportHelper.export(this, ClipFormatter.joinForExport(latestEntries))
        }
        binding.clearAllButton.setOnClickListener { confirmClearAll() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeEntries().collect { entries ->
                    latestEntries = entries
                    adapter.submitList(entries.asReversed())
                    val empty = entries.isEmpty()
                    binding.emptyClipsText.visibility = if (empty) android.view.View.VISIBLE else android.view.View.GONE
                    binding.clipRecyclerView.visibility = if (empty) android.view.View.GONE else android.view.View.VISIBLE
                }
            }
        }
    }

    private fun updateMonitoringStatus(isOn: Boolean) {
        binding.monitoringStatusText.setText(if (isOn) R.string.monitoring_on else R.string.monitoring_off)
        binding.monitoringDot.alpha = if (isOn) 1f else 0.35f
    }

    private fun requestNotificationPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ClipboardWatcherService.start(this)
        }
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
            Snackbar.make(binding.root, R.string.toast_item_deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_undo) {
                    lifecycleScope.launch { repository.restore(entry) }
                }
                .show()
        }
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_all_confirm_title)
            .setMessage(R.string.clear_all_confirm_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_confirm_delete) { _, _ ->
                lifecycleScope.launch {
                    repository.clearAll()
                    Toast.makeText(this@SettingsActivity, R.string.toast_all_cleared, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun setupSyncSection() {
        binding.syncSwitch.isChecked = prefs.isEnabled
        setChannelText(prefs.channel)
        updateChannelUrlPreview(prefs.channel)

        binding.syncSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.isEnabled = isChecked
        }

        binding.channelEditText.doAfterTextChanged { text ->
            if (suppressChannelWatcher) return@doAfterTextChanged
            val raw = text?.toString().orEmpty()
            updateChannelUrlPreview(raw)
            if (isValidChannel(raw)) {
                binding.channelInputLayout.error = null
                prefs.channel = raw
            } else {
                binding.channelInputLayout.error = getString(R.string.toast_invalid_channel)
            }
        }

        binding.regenerateChannelButton.setOnClickListener {
            val newChannel = prefs.generateChannel()
            prefs.channel = newChannel
            setChannelText(newChannel)
            updateChannelUrlPreview(newChannel)
            Toast.makeText(this, R.string.toast_channel_regenerated, Toast.LENGTH_SHORT).show()
        }

        binding.copyLinkButton.setOnClickListener {
            val clip = ClipData.newPlainText(getString(R.string.settings_sync_link_label), prefs.channelUrl)
            clipboardManager.setPrimaryClip(clip)
            Toast.makeText(this, R.string.toast_link_copied, Toast.LENGTH_SHORT).show()
        }

        binding.shareLinkButton.setOnClickListener {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, prefs.channelUrl)
            }
            startActivity(Intent.createChooser(sendIntent, getString(R.string.settings_sync_share_link)))
        }

        binding.openLinkButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(prefs.channelUrl)))
        }

        binding.sendTestButton.setOnClickListener { sendTestMessage() }
    }

    private fun setChannelText(channel: String) {
        suppressChannelWatcher = true
        binding.channelEditText.setText(channel)
        binding.channelEditText.setSelection(channel.length)
        suppressChannelWatcher = false
    }

    private fun updateChannelUrlPreview(channel: String) {
        val safeChannel = channel.ifBlank { getString(R.string.settings_sync_channel_label) }
        binding.channelUrlText.text = getString(R.string.settings_sync_link_format, safeChannel)
    }

    private fun isValidChannel(channel: String): Boolean =
        channel.length >= 4 && channel.matches(Regex("^[a-zA-Z0-9][a-zA-Z0-9-]*$"))

    private fun sendTestMessage() {
        binding.sendTestButton.isEnabled = false
        lifecycleScope.launch {
            val success = RemoteSyncManager.sendTestMessage(this@SettingsActivity)
            binding.sendTestButton.isEnabled = true
            Toast.makeText(
                this@SettingsActivity,
                if (success) R.string.toast_test_sent else R.string.toast_test_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeSyncStatus() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                RemoteSyncManager.lastStatus.collect { status ->
                    binding.syncStatusText.text = when (status) {
                        is SyncStatus.Idle -> getString(R.string.settings_sync_last_status_idle)
                        is SyncStatus.Sending -> getString(R.string.settings_sync_last_status_sending)
                        is SyncStatus.Success -> getString(
                            R.string.settings_sync_last_status_success,
                            ClipFormatter.formatDate(status.atMillis)
                        )
                        is SyncStatus.Failed -> getString(
                            R.string.settings_sync_last_status_failed,
                            ClipFormatter.formatDate(status.atMillis)
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "clipsmile_prefs"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
    }
}

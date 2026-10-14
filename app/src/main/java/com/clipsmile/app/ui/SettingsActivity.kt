package com.clipsmile.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.clipsmile.app.AppVisibility
import com.clipsmile.app.R
import com.clipsmile.app.data.RemoteSyncManager
import com.clipsmile.app.data.RemoteSyncPrefs
import com.clipsmile.app.data.SyncStatus
import com.clipsmile.app.databinding.ActivitySettingsBinding
import com.clipsmile.app.util.ClipFormatter
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: RemoteSyncPrefs
    private lateinit var clipboardManager: ClipboardManager

    private var suppressChannelWatcher = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = RemoteSyncPrefs(this)
        clipboardManager = getSystemService<ClipboardManager>()!!

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
}

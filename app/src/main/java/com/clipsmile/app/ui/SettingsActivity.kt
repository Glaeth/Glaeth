package com.clipsmile.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.clipsmile.app.AppVisibility
import com.clipsmile.app.R
import com.clipsmile.app.data.RemoteSyncPrefs
import com.clipsmile.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        RemoteSyncPrefs(this).isEnabled = false

        setupDenSection()

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

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@registerForActivityResult
        val json = com.clipsmile.app.data.Den(this).exportJson()
        contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
        Toast.makeText(this, R.string.settings_export_ok, Toast.LENGTH_SHORT).show()
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
        val ok = com.clipsmile.app.data.Den(this).importJson(text)
        Toast.makeText(
            this,
            if (ok) R.string.settings_import_ok else R.string.settings_import_bad,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setupDenSection() {
        binding.musicSwitch.isChecked = PlayPrefs.music(this)
        binding.effectsSwitch.isChecked = PlayPrefs.effects(this)
        binding.musicSwitch.setOnCheckedChangeListener { _, checked -> PlayPrefs.setMusic(this, checked) }
        binding.effectsSwitch.setOnCheckedChangeListener { _, checked -> PlayPrefs.setEffects(this, checked) }
        binding.exportDenButton.setOnClickListener {
            exportLauncher.launch("cirkin-kopek.json")
        }
        binding.importDenButton.setOnClickListener {
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }
        binding.promoButton.setOnClickListener {
            val den = com.clipsmile.app.data.Den(this)
            val code = den.redeem(binding.promoEdit.text?.toString().orEmpty())
            val message = when (code) {
                "ok" -> R.string.settings_promo_ok
                "ok_gold" -> R.string.settings_promo_clrmnt
                "used" -> R.string.settings_promo_used
                else -> R.string.settings_promo_bad
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            if (code == "ok" || code == "ok_gold") binding.promoEdit.setText("")
        }
    }
}

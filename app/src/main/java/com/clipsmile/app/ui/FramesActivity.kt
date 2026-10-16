package com.clipsmile.app.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.clipsmile.app.R
import com.clipsmile.app.data.PetProgress
import com.clipsmile.app.databinding.ActivityFramesBinding
import com.clipsmile.app.databinding.ItemFrameBinding

class FramesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFramesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backButton.setOnClickListener { finish() }
        val level = PetProgress(this).level()
        val current = FrameLooks.forLevel(level)
        val openCount = FrameLooks.all.count { level >= it.minLevel }
        binding.progressLabel.text = getString(R.string.frames_progress, openCount, FrameLooks.all.size)
        val density = resources.displayMetrics.density
        for (look in FrameLooks.all) {
            val row = ItemFrameBinding.inflate(layoutInflater, binding.frameList, true)
            val open = level >= look.minLevel
            val active = look.minLevel == current.minLevel
            row.ring.setFrame(look)
            row.name.setText(look.nameRes)
            row.level.text = getString(R.string.collection_level_chip, look.minLevel)
            row.status.setText(
                when {
                    active -> R.string.frames_current
                    open -> R.string.frames_open
                    else -> R.string.frames_locked
                }
            )
            row.status.setTextColor(if (open) 0xFF3E8C12.toInt() else 0xFF8A6844.toInt())
            row.card.alpha = if (open) 1f else 0.55f
            row.card.setBackgroundResource(
                if (active) R.drawable.bg_collection_card_on else R.drawable.bg_collection_card
            )
            row.card.elevation = (if (active) 6f else 1.5f) * density
            row.homeBadge.visibility = if (active) View.VISIBLE else View.GONE
        }
    }
}

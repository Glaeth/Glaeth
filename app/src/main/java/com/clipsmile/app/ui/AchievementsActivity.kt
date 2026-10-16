package com.clipsmile.app.ui

import android.graphics.Typeface
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.ImageViewCompat
import com.clipsmile.app.R
import com.clipsmile.app.databinding.ActivityAchievementsBinding
import com.clipsmile.app.databinding.ItemAchievementBinding

class AchievementsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAchievementsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backButton.setOnClickListener { finish() }
        val unlocked = Achievements.unlockedCount(this)
        binding.progressLabel.text = getString(
            R.string.achievements_progress,
            unlocked,
            Achievements.all.size
        )
        showBadges()
    }

    private fun showBadges() {
        binding.badgeList.removeAllViews()
        var lastKind = ""
        for (badge in Achievements.all) {
            if (badge.kind != lastKind) {
                lastKind = badge.kind
                binding.badgeList.addView(section(getString(Achievements.sectionTitle(badge.kind))))
            }
            val row = ItemAchievementBinding.inflate(layoutInflater, binding.badgeList, true)
            val open = Achievements.isUnlocked(this, badge.id)
            row.title.setText(badge.titleRes)
            row.detail.setText(badge.detailRes)
            row.state.setText(if (open) R.string.achievement_unlocked else R.string.achievement_locked)
            row.state.setTextColor(if (open) 0xFF3E8C12.toInt() else 0xFF8A6844.toInt())
            row.card.alpha = if (open) 1f else 0.55f
            ImageViewCompat.setImageTintList(
                row.badge,
                android.content.res.ColorStateList.valueOf(if (open) 0xFFE2A322.toInt() else 0xFFC4B6A6.toInt())
            )
        }
    }

    private fun section(label: String): TextView {
        return TextView(this).apply {
            text = label
            setTextColor(0xFF8A6844.toInt())
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            letterSpacing = 0.08f
            val pad = (resources.displayMetrics.density * 4).toInt()
            setPadding(pad, pad * 3, pad, pad)
        }
    }
}

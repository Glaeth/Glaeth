package com.clipsmile.app.ui

import com.clipsmile.app.R

data class FrameLook(
    val minLevel: Int,
    val nameRes: Int,
    val ring: Int,
    val glow: Int,
    val widthDp: Float
)

object FrameLooks {
    val all = listOf(
        FrameLook(1, R.string.frame_1, 0xFF8A6844.toInt(), 0x33C4A574, 4f),
        FrameLook(15, R.string.frame_15, 0xFFC4622D.toInt(), 0x55E07A2F, 5f),
        FrameLook(30, R.string.frame_30, 0xFF8D6E63.toInt(), 0x558D6E63, 5f),
        FrameLook(45, R.string.frame_45, 0xFF7E8B99.toInt(), 0x667E8B99, 6f),
        FrameLook(60, R.string.frame_60, 0xFF3E8C12.toInt(), 0x553E8C12, 6f),
        FrameLook(90, R.string.frame_90, 0xFF2A75BB.toInt(), 0x552A75BB, 7f),
        FrameLook(125, R.string.frame_125, 0xFF7B4B9A.toInt(), 0x667B4B9A, 7f),
        FrameLook(150, R.string.frame_150, 0xFFC8960A.toInt(), 0x77E0A317, 8f),
        FrameLook(175, R.string.frame_175, 0xFFB4531A.toInt(), 0x77E07A2F, 8f),
        FrameLook(200, R.string.frame_200, 0xFFB8290A.toInt(), 0x77E3350D, 9f),
        FrameLook(250, R.string.frame_250, 0xFF8A6A00.toInt(), 0x99FFCB05.toInt(), 11f)
    )

    fun forLevel(level: Int): FrameLook = all.lastOrNull { level >= it.minLevel } ?: all.first()
}

package com.clipsmile.app.ui

import com.clipsmile.app.R

data class FrameLook(
    val minLevel: Int,
    val nameRes: Int,
    val ring: Int,
    val glow: Int,
    val widthDp: Float,
    val inner: Int,
    val tier: Int
)

object FrameLooks {
    val all = listOf(
        FrameLook(1, R.string.frame_1, 0xFF8B5A2B.toInt(), 0x338B5A2B, 5f, 0xFFC4A574.toInt(), 0),
        FrameLook(3, R.string.frame_15, 0xFFB87333.toInt(), 0x55E08A4F, 6f, 0xFFE8C4A0.toInt(), 0),
        FrameLook(5, R.string.frame_30, 0xFF8D6E63.toInt(), 0x558D6E63, 6f, 0xFFD7CCC8.toInt(), 1),
        FrameLook(8, R.string.frame_45, 0xFF8E9BA8.toInt(), 0x668E9BA8, 7f, 0xFFECEFF1.toInt(), 1),
        FrameLook(11, R.string.frame_60, 0xFF2F7D12.toInt(), 0x552F7D12, 7f, 0xFFB7E38A.toInt(), 1),
        FrameLook(14, R.string.frame_90, 0xFF1F5F9E.toInt(), 0x551F5F9E, 8f, 0xFFB9D7F2.toInt(), 1),
        FrameLook(18, R.string.frame_125, 0xFF6A3D86.toInt(), 0x666A3D86, 8f, 0xFFE1C4F0.toInt(), 2),
        FrameLook(22, R.string.frame_150, 0xFFC8960A.toInt(), 0x77E0B417, 9f, 0xFFFFE08A.toInt(), 2),
        FrameLook(26, R.string.frame_175, 0xFFC4622D.toInt(), 0x77E08A3A, 9f, 0xFFFFD0A6.toInt(), 2),
        FrameLook(30, R.string.frame_200, 0xFFB42318.toInt(), 0x77E24A3B, 10f, 0xFFFFB4A8.toInt(), 2),
        FrameLook(36, R.string.frame_250, 0xFF8A6A00.toInt(), 0x99FFCB05.toInt(), 12f, 0xFFFFF3C4.toInt(), 3)
    )

    fun forLevel(level: Int): FrameLook = all.lastOrNull { level >= it.minLevel } ?: all.first()
}

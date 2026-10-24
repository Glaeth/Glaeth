package com.clipsmile.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import com.clipsmile.app.R

/**
 * Each mini-game has its own loop and its own effects.
 */
class GameAudio(
    context: Context,
    private val musicRes: Int,
    jumpRes: Int,
    pointRes: Int,
    crashRes: Int,
    catchRes: Int,
    missRes: Int,
    hurtRes: Int = 0,
    rewardRes: Int = 0
) {

    private val app = context.applicationContext
    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val pool: SoundPool
    private val jumpId: Int
    private val pointId: Int
    private val crashId: Int
    private val catchId: Int
    private val missId: Int
    private val hurtId: Int
    private val rewardId: Int
    private var music: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var released = false

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attributes).build()
        jumpId = pool.load(app, jumpRes, 1)
        pointId = pool.load(app, pointRes, 1)
        crashId = pool.load(app, crashRes, 1)
        catchId = pool.load(app, catchRes, 1)
        missId = pool.load(app, missRes, 1)
        hurtId = if (hurtRes != 0) pool.load(app, hurtRes, 1) else 0
        rewardId = if (rewardRes != 0) pool.load(app, rewardRes, 1) else 0
    }

    fun start() {
        if (released || !PlayPrefs.music(app)) return
        requestFocus()
        if (music == null) music = buildPlayer()
        val player = music ?: return
        if (!player.isPlaying) player.start()
    }

    fun pause() {
        val player = music
        if (player != null && player.isPlaying) player.pause()
        abandonFocus()
    }

    fun release() {
        if (released) return
        released = true
        music?.release()
        music = null
        pool.release()
        abandonFocus()
    }

    fun jump() = shot(jumpId, 0.7f)

    fun point() = shot(pointId, 0.65f)

    fun crash() = shot(crashId, 0.8f)

    fun miss() = shot(missId, 0.55f)

    fun catchFood() = shot(catchId, 0.75f)

    fun hurt() = if (hurtId != 0) shot(hurtId, 0.85f) else crash()

    fun reward() = if (rewardId != 0) shot(rewardId, 0.8f) else point()

    private fun shot(id: Int, volume: Float) {
        if (released || id == 0 || !PlayPrefs.effects(app)) return
        pool.play(id, volume, volume, 1, 0, 1f)
    }

    private fun musicAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }

    private fun buildPlayer(): MediaPlayer? {
        return try {
            val player = MediaPlayer()
            player.setAudioAttributes(musicAttributes())
            app.resources.openRawResourceFd(musicRes).use { fd ->
                player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            player.isLooping = true
            player.setVolume(0.34f, 0.34f)
            player.prepare()
            player
        } catch (_: Exception) {
            null
        }
    }

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (focusRequest == null) {
                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(musicAttributes())
                    .build()
            }
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    companion object {
        fun drop(context: Context) = GameAudio(
            context,
            R.raw.music_drop,
            R.raw.sfx_drop_catch,
            R.raw.sfx_drop_catch,
            R.raw.sfx_drop_over,
            R.raw.sfx_drop_catch,
            R.raw.sfx_drop_miss,
            rewardRes = R.raw.sfx_reward
        )

        fun flap(context: Context) = GameAudio(
            context,
            R.raw.music_flap,
            R.raw.sfx_flap_jump,
            R.raw.sfx_flap_point,
            R.raw.sfx_flap_crash,
            R.raw.sfx_flap_point,
            R.raw.sfx_flap_crash,
            rewardRes = R.raw.sfx_reward
        )

        fun swords(context: Context) = GameAudio(
            context,
            R.raw.music_sword,
            R.raw.sfx_run_jump,
            R.raw.sfx_flap_point,
            R.raw.sfx_run_crash,
            R.raw.sfx_flap_point,
            R.raw.sfx_run_crash,
            R.raw.sfx_hit,
            R.raw.sfx_reward
        )

        fun run(context: Context) = GameAudio(
            context,
            R.raw.music_run,
            R.raw.sfx_run_jump,
            R.raw.sfx_run_point,
            R.raw.sfx_run_crash,
            R.raw.sfx_run_point,
            R.raw.sfx_run_crash,
            R.raw.sfx_hit,
            R.raw.sfx_reward
        )
    }
}

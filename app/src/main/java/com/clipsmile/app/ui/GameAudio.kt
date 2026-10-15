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
 * Looping original tune plus short effects for the mini-games.
 * Music ducks other apps and stops when the game leaves the screen.
 */
class GameAudio(context: Context) {

    private val app = context.applicationContext
    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val pool: SoundPool
    private val jumpId: Int
    private val pointId: Int
    private val crashId: Int
    private val catchId: Int
    private var music: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var released = false

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attributes).build()
        jumpId = pool.load(app, R.raw.sfx_jump, 1)
        pointId = pool.load(app, R.raw.sfx_point, 1)
        crashId = pool.load(app, R.raw.sfx_crash, 1)
        catchId = pool.load(app, R.raw.sfx_catch, 1)
    }

    fun start() {
        if (released) return
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

    fun jump() = shot(jumpId, 0.55f)

    fun point() = shot(pointId, 0.6f)

    fun crash() = shot(crashId, 0.75f)

    fun miss() = shot(crashId, 0.32f)

    fun catchFood() = shot(catchId, 0.62f)

    private fun shot(id: Int, volume: Float) {
        if (released || id == 0) return
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
            app.resources.openRawResourceFd(R.raw.game_music).use { fd ->
                player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            player.isLooping = true
            player.setVolume(0.3f, 0.3f)
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
}

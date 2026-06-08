package com.example.blockblast

import android.content.Context
import android.media.MediaPlayer

object SoundManager {
    private var backgroundPlayer: MediaPlayer? = null
    private var soundPlayer: MediaPlayer? = null

    private var currentMusicVolume = 0.5f
    private var currentSoundVolume = 0.5f
    private var currentMusicResId: Int? = null
    fun init(context: Context, musicVolume: Float, soundVolume: Float) {
        currentMusicVolume = musicVolume
        currentSoundVolume = soundVolume
    }

    fun startBackgroundMusic(context: Context, resId: Int, volume: Float) {
        if (backgroundPlayer != null && backgroundPlayer?.isPlaying == true && currentMusicResId == resId) {
            updateMusicVolume(volume)
            return
        }

        stopBackgroundMusic()
        currentMusicResId = resId
        backgroundPlayer = MediaPlayer.create(context, resId).apply {
            setVolume(volume, volume)
            isLooping = true
            start()
        }
    }

    fun stopBackgroundMusic() {
        backgroundPlayer?.stop()
        backgroundPlayer?.release()
        backgroundPlayer = null
        currentMusicResId = null
    }

    fun pauseBackgroundMusic() {
        if (backgroundPlayer?.isPlaying == true) {
            backgroundPlayer?.pause()
        }
    }

    fun resumeBackgroundMusic() {
        if (backgroundPlayer != null && backgroundPlayer?.isPlaying == false) {
            backgroundPlayer?.start()
        }
    }

    fun playSound(context: Context, resId: Int, volume: Float) {
        soundPlayer?.release()
        soundPlayer = MediaPlayer.create(context, resId).apply {
            setVolume(volume, volume)
            start()
            setOnCompletionListener {
                it.release()
                soundPlayer = null
            }
        }
    }

    fun updateMusicVolume(volume: Float) {
        currentMusicVolume = volume
        backgroundPlayer?.setVolume(volume, volume)
    }

    fun updateSoundVolume(volume: Float) {
        currentSoundVolume = volume
    }

    fun release() {
        backgroundPlayer?.release()
        soundPlayer?.release()
        backgroundPlayer = null
        soundPlayer = null
        currentMusicResId = null
    }
}
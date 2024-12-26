package com.example.yoga.Model

import MyTTS
import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class GlobalVariable: Application(), DefaultLifecycleObserver {
    lateinit var TTS:MyTTS
    lateinit var backgroundMusic:MyMediaPlayer

    companion object {//單例模式
        @Volatile
        private var instance: GlobalVariable? = null


        fun getInstance(): GlobalVariable {
            return instance ?: synchronized(this) {
                instance ?: GlobalVariable().also { instance = it }
            }
        }
    }

    override fun onCreate() {
        super<Application>.onCreate()
        instance = this

        TTS = MyTTS()
        TTS.init(applicationContext)

        backgroundMusic = MyMediaPlayer()
        backgroundMusic.init(applicationContext)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        // 當應用程式進入前景時開始播放音樂
        backgroundMusic.play()
        println("play music in GlobalVariable")
    }

    override fun onStop(owner: LifecycleOwner) {
        // 當應用程式進入背景時暫停音樂
        backgroundMusic.pause()
        TTS.stop()
        println("pause music in GlobalVariable")

    }

    override fun onTerminate() {
        super.onTerminate()
        backgroundMusic.stop()
        TTS.stop()

        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)

    }
}

package com.example.yoga.Model

import MyTTS
import android.app.Application
class GlobalVariable: Application() {
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
        super.onCreate()
        instance = this

        TTS = MyTTS()
        TTS.init(applicationContext)

        backgroundMusic = MyMediaPlayer()
        backgroundMusic.init(applicationContext)
    }

    override fun onTerminate() {
        super.onTerminate()
        backgroundMusic.stop()
        TTS.stop()
    }
}

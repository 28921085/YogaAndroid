package com.example.yoga.Model

import android.content.Context
import android.media.MediaPlayer
import com.example.yoga.R

class MyMediaPlayer {
    private lateinit var mediaPlayer: MediaPlayer
    fun init(context: Context){
        mediaPlayer = MediaPlayer.create(context, R.raw.background_music)
        mediaPlayer.isLooping = true // 設定音樂循環播放
        //mediaPlayer.start()
    }
    fun play(){
        if(!mediaPlayer.isPlaying)
            mediaPlayer.start()
    }
    fun pause(){
        if(mediaPlayer.isPlaying)
            mediaPlayer.pause()
    }
    fun stop(){mediaPlayer.stop()}

//    fun play() {
//        if (!isPlaying()) {
//            mediaPlayer.start()
//        }
//    }
//    fun pause() {
//        if (isPlaying()) {
//            mediaPlayer.pause()
//        }
//    }
//    fun stop(){
//        mediaPlayer.stop()
//    }
//    fun isPlaying(): Boolean {
//        return mediaPlayer.isPlaying
//    }
}
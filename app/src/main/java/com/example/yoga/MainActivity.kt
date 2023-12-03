package com.example.yoga

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.yoga.discover.BluetoothActivity
import android.media.MediaPlayer

class MainActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private  val CAMERA_PERMISSION_REQUEST_CODE = 1001  //據說是隨便設定就好
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // 隐藏title bar
        setContentView(R.layout.activity_main)

        val start = findViewById<Button>(R.id.start)
        start.setOnClickListener {
            // 頁面跳轉
            // val intent = Intent(this, CalibrationStage::class.java)
            val intent = Intent(this, BluetoothActivity::class.java)
            //val intent = Intent(this, Menu::class.java)
            startActivity(intent)
        }

        // 如果没有相机权限，请求相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }

        val playMusic = intent.getBooleanExtra("playMusic",true)

        // 初始化 MediaPlayer(背景音樂)
        if(playMusic){
            mediaPlayer = MediaPlayer.create(this, R.raw.background_music)
            mediaPlayer.isLooping = true // 設定音樂循環播放
            mediaPlayer.start()
        }
    }
}
package com.example.yoga.View

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.yoga.discover.BluetoothActivity
import com.example.yoga.Model.GlobalVariable
import com.example.yoga.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private  val CAMERA_PERMISSION_REQUEST_CODE = 1001  //據說是隨便設定就好
    private var global=GlobalVariable.getInstance()
    fun nextpage(){
        val intent = Intent(this, BluetoothActivity::class.java)
        startActivity(intent)
        finish()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // 隐藏title bar
        setContentView(R.layout.activity_main)

        val start = findViewById<Button>(R.id.start)
        start.setOnClickListener {
            nextpage()
        }
        // 如果没有相机权限，请求相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }

    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            delay(500)
//            global.backgroundMusic.play()
        }
    }

    override fun onPause() {
        super.onPause()
        global.backgroundMusic.pause()
        println("pause music in AllPose onPause")
        global.TTS.stop()
    }
    override fun onResume() {
        super.onResume()
        global.backgroundMusic.play()
        println("play music in AllPose onResume")

    }

    override fun onDestroy() {
        super.onDestroy()
//        global.backgroundMusic.pause()
    }

    override fun onStop() {
        super.onStop()
        global.backgroundMusic.pause()
    }
}
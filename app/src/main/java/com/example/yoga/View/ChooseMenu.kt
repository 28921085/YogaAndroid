package com.example.yoga.View

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.yoga.Model.GlobalVariable
import com.example.yoga.databinding.ActivityChooseMenuBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChooseMenu : AppCompatActivity() {
    private lateinit var chooseMenuBinding:ActivityChooseMenuBinding
    private var global = GlobalVariable.getInstance()

    fun lastpage() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chooseMenuBinding = ActivityChooseMenuBinding.inflate(layoutInflater)
        setContentView(chooseMenuBinding.root)
        supportActionBar?.hide()

        chooseMenuBinding.back.setOnClickListener{
            lastpage()
        }
        //之後再來包體感互動
        chooseMenuBinding.allPose.setOnClickListener{
            val intent = Intent(this, AllPoseMenu::class.java)
            startActivity(intent)
            finish()
        }
        chooseMenuBinding.trainingMenu.setOnClickListener{
            val intent = Intent(this, TrainingMenu::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            delay(800)
            global.backgroundMusic.play()
        }
    }
    override fun onPause() {
        super.onPause()
        global.backgroundMusic.pause()
    }
    override fun onResume() {
        super.onResume()
        global.backgroundMusic.play()
    }
    override fun onDestroy() {
        super.onDestroy()
        global.backgroundMusic.pause()
    }
}
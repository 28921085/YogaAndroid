package com.example.yoga.View
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.yoga.Model.KSecCountdownTimer
import com.example.yoga.ViewModel.TrainingMenuViewModel
//import com.example.yoga.Model.TrainingProcess
import com.example.yoga.databinding.ActivityRestIntervalBinding


class RestInterval : AppCompatActivity(), KSecCountdownTimer.TimerCallback {
    private lateinit var restIntervalBinding: ActivityRestIntervalBinding
    private var timer30S = KSecCountdownTimer(30)// 測試時設置為 5 秒
    var mode = ""
    var poseList = arrayOf<String>()
    var poseName=""
    var currentIndex = 0
    var menuTitle = ""
    var totalScore = 0.0
    var totalTime = 0.0

    override fun onTimerFinished() {
        Log.d("Rest menuTitle", "$menuTitle")
        Log.d("Rest 目前 index", "$currentIndex")
        Log.d("Rest 目前總時間", "$totalTime")
        Log.d("Rest 目前總分", "$totalScore")
        currentIndex = currentIndex+1
        poseName = poseList[currentIndex]
        val intent = Intent(this, VideoGuide::class.java).apply{
            putExtra("mode", mode)
            putExtra("menuTitle", menuTitle)
            putExtra("poseList", poseList)
            putExtra("poseName",poseName)
            putExtra("currentIndex", currentIndex)
            putExtra("totalScore",totalScore)
            putExtra("totalTime",totalTime)
        }
        startActivity(intent)
        finish()
    }

    // 更新倒計時條的顏色
    override fun updateColorBar(currentMs: Long, maxMS: Long) {
        val barColor = timer30S.getCurrentColor(currentMs)
        val layoutParams = restIntervalBinding.timeLeftBar.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.matchConstraintPercentWidth = currentMs / (1f * maxMS)
        restIntervalBinding.timeLeftBar.layoutParams = layoutParams
        restIntervalBinding.timeLeftBar.backgroundTintList = ColorStateList.valueOf(barColor)
    }

    override fun timerSpeak(str: String) {
        // 這裡可以加入語音提示邏輯
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restIntervalBinding = ActivityRestIntervalBinding.inflate(layoutInflater)
        setContentView(restIntervalBinding.root)
        supportActionBar?.hide() // 隱藏 title bar

        mode = intent.getStringExtra("mode").toString()
        poseList = intent.getStringArrayExtra("poseList")!!
        poseName = intent.getStringExtra("poseName").toString()
        currentIndex = intent.getIntExtra("currentIndex", -1)

        menuTitle = intent.getStringExtra("menuTitle").toString()
        totalScore = intent.getDoubleExtra("totalScore", 0.0)
//        time = intent.getIntExtra("finishTime", 0)
        totalTime = intent.getDoubleExtra("totalTime", 0.0)

        restIntervalBinding.skip.setOnClickListener {
            onTimerFinished()
        }
        timer30S.startTimer(this)
    }
}

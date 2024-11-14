package com.example.yoga.View

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.yoga.Model.GlobalVariable
import com.example.yoga.Model.fileNameGetter
import com.example.yoga.ViewModel.TrainingMenuViewModel
import com.example.yoga.databinding.ActivityVideoGuideBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VideoGuide : AppCompatActivity() {
    private lateinit var videoGuideBinding: ActivityVideoGuideBinding
    private var global=GlobalVariable.getInstance()
    private val trainingMenuViewModel: TrainingMenuViewModel by viewModels()
    var mode = ""
    var menuTitle = ""
    var poseList = arrayOf<String>()
    var poseName=""
    var currentIndex = 0
    var totalScore = 0.0
    var totalTime = 0.0

    // yogaMat nextPage
    private lateinit var python : Python
    private lateinit var yogaMat : PyObject
    private var yogaMatThread: Thread? = null
    private var threadFlag : Boolean = true

    private var fileGetter=fileNameGetter()

    fun nextpage(){
        Log.d("Video 訓練模式: ", "$mode")
        try {
            threadFlag = false // to stop thread
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        if (mode == "AllPose"){
            val intent = Intent(this, YogaMain::class.java).apply {
                putExtra("mode", mode)
                putExtra("poseName",poseName)
            }
            startActivity(intent)
            finish()
        }
        else if (mode == "TrainingProcess"){
//          else{
            Log.d("Video menuTitle", "$menuTitle")
            Log.d("Video 目前 index", "$currentIndex")
            Log.d("Video 目前總時間", "$totalTime")
            Log.d("Video 目前總分", "$totalScore")
            val intent = Intent(this, YogaMain::class.java).apply{
               putExtra("mode", mode)
               putExtra("menuTitle", menuTitle)
               putExtra("poseList", poseList)
               putExtra("poseName", poseList[currentIndex])
               putExtra("currentIndex", currentIndex)
               putExtra("totalScore", totalScore)
               putExtra("totalTime", totalTime)
            }
            startActivity(intent)
            finish()
        //            val resultIntent = Intent()
        //            setResult(Activity.RESULT_OK, resultIntent)
        //            finish()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // 隐藏title bar

        videoGuideBinding = ActivityVideoGuideBinding.inflate(layoutInflater)
        setContentView(videoGuideBinding.root)

        mode = intent.getStringExtra("mode").toString()
        poseName = intent.getStringExtra("poseName").toString()
        if(mode=="TrainingProcess"){
            menuTitle = intent.getStringExtra("menuTitle").toString()
            poseList = intent.getStringArrayExtra("poseList")!!
            currentIndex = intent.getIntExtra("currentIndex", -1)
            totalScore = intent.getDoubleExtra("totalScore", 0.0)
            totalTime = intent.getDoubleExtra("totalTime", 0.0)
        }
        videoGuideBinding.videoTitle.text = poseName

        //video player init
        val videoPath = "android.resource://" + packageName + "/" +  fileGetter.getfile(this, poseName)
        videoGuideBinding.videoPlayer.setVideoURI(Uri.parse(videoPath))
        videoGuideBinding.videoPlayer.start()
        // 设置循环播放
        videoGuideBinding.videoPlayer.setOnPreparedListener { mp -> // mp = mediaplayer
            mp.isLooping = true
        }

        videoGuideBinding.finish.setOnClickListener {
            nextpage()
        }

        //啟動python
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        python = Python.getInstance()

        // get yogaMat python module
        yogaMat = python.getModule("heatmap")

        // using yogaMat nextPage
        yogaMatThread = Thread {
            try {
                Thread.sleep(1000)
                while (!yogaMat.callAttr("checkReturn").toBoolean() and threadFlag) {
                    Thread.sleep(100)
                }
                if(threadFlag){
                    runOnUiThread {
                        nextpage()
                    }
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            println("!!! VideoGuide Done !!!")
        }

        yogaMatThread?.start()
    }
    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            delay(800)
            global.backgroundMusic.play()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        global.backgroundMusic.pause()
    }
    override fun onPause() {
        super.onPause()
        global.TTS.stop()
        global.backgroundMusic.pause()
    }

    override fun onResume() {
        super.onResume()
        global.backgroundMusic.play()
    }
}

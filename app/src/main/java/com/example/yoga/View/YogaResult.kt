package com.example.yoga.View

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.yoga.Model.GlobalVariable
import com.example.yoga.databinding.ActivityYogaResultBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class YogaResult : AppCompatActivity() {
    private lateinit var yogaResultBinding: ActivityYogaResultBinding
    private var global=GlobalVariable.getInstance()
    private lateinit var python : Python
    private lateinit var heatmapReturn : PyObject
    private var myThread: Thread? = null
    fun lastpage(){

        try {
            myThread?.interrupt()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        val intent = Intent(this, AllPoseMenu::class.java)
        startActivity(intent)
        finish()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // 隐藏title bar
        //初始化yogaResultBinding
        yogaResultBinding = ActivityYogaResultBinding.inflate(layoutInflater)
        setContentView(yogaResultBinding.root)

        //啟動python
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        python = Python.getInstance()

        heatmapReturn = python.getModule("heatmap")

        // yogamap return
        myThread = Thread {
            try {
                while (!heatmapReturn.callAttr("checkReturn").toBoolean()) {
                    Thread.sleep(100)
                    print("checkReturn")
                }
                runOnUiThread {
                    lastpage()
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        myThread?.start()


        val title = intent.getStringExtra("title")
        yogaResultBinding.title.text = title
        val finishTime = intent.getDoubleExtra("finishTime",0.0)
        yogaResultBinding.time.text = "完成時間:"+finishTime.toString()+"秒"
        val score = intent.getDoubleExtra("score",100.0)
        yogaResultBinding.score.text = "分數:${"%.2f".format(score)}"

        yogaResultBinding.back.text = "Back To Menu"
        yogaResultBinding.back.setOnClickListener {
            lastpage()
        }
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
        // 在Activity銷毀時結束thread
        myThread?.interrupt()
    }
    override fun onPause() {
        super.onPause()
        global.backgroundMusic.pause()
    }
    override fun onResume() {
        super.onResume()
        global.backgroundMusic.play()
    }
}
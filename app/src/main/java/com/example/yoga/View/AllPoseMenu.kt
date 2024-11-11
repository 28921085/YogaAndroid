package com.example.yoga.View

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.yoga.Model.GlobalVariable
import com.example.yoga.ViewModel.ButtonAdapter
import com.example.yoga.databinding.ActivityAllPoseMenuBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AllPoseMenu : AppCompatActivity() {
    private lateinit var menuBinding: ActivityAllPoseMenuBinding
    private var global = GlobalVariable.getInstance()
    private lateinit var currentSelect: Button

    // yogaMat function
    private lateinit var python : Python
    private lateinit var yogaMat : PyObject
    private var yogaMatFunctionThread: Thread? = null
    private var threadFlag : Boolean = true
    private var functionNumber: Int = 0

    private lateinit var recyclerView: RecyclerView
    private final var WIDTH = 2
    private final var HEIGHT = 10

    private val mode="AllPose"

    private val poseNames = arrayOf(
            "Tree Style", "Warrior2 Style", "Plank", "Reverse Plank", "Child's pose",
            "Seated Forward Bend", "Low Lunge", "Downward dog", "Pyramid pose", "Bridge pose",
            "Mountain pose", "Triangle pose", "Locust pose", "Cobra pose", "Half moon pose",
            "Boat pose", "Camel pose", "Pigeon pose", "Fish pose", "Chair pose"
    )

    private lateinit var buttonAdapter: ButtonAdapter

    fun lastpage() {
        threadFlag = false // to stop thread

        val intent = Intent(this, ChooseMenu::class.java)
        startActivity(intent)
        finish()
    }

    fun nextpage() {
        nextpage(poseNames[buttonAdapter.getIndexByButton(currentSelect)])
    }

    fun nextpage(posename: String) {
        threadFlag = false // to stop thread

        val intent = Intent(this, VideoGuide::class.java).apply {
            putExtra("mode", mode)
            putExtra("poseName", posename)
        }
        startActivity(intent)
        finish()
    }
    fun moveRollBarTo(index:Int){
        recyclerView.smoothScrollToPosition(index)
    }
    fun selectTo(index:Int){
        buttonAdapter.setSelectedIndex(index)
        moveRollBarTo(index)
        currentSelect = buttonAdapter.getButtonByIndex(index)
    }

    fun up() {
        recyclerView.post {
            var index = buttonAdapter.getIndexByButton(currentSelect)
            index = (index - WIDTH + poseNames.size) % poseNames.size
            selectTo(index)
        }
    }

    fun down() {
        recyclerView.post {
            var index = buttonAdapter.getIndexByButton(currentSelect)
            index = (index + WIDTH) % poseNames.size
            selectTo(index)
        }
    }

    fun left() {
        recyclerView.post {
            var index = buttonAdapter.getIndexByButton(currentSelect)
            var q = index/WIDTH
            var r = index%WIDTH
            r=(r-1+WIDTH)%WIDTH
            index = q*WIDTH+r
            selectTo(index)
        }
    }

    fun right() {
        recyclerView.post {
            var index = buttonAdapter.getIndexByButton(currentSelect)
            var q = index/WIDTH
            var r = index%WIDTH
            r=(r+1)%WIDTH
            index = q*WIDTH+r
            selectTo(index)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuBinding = ActivityAllPoseMenuBinding.inflate(layoutInflater)
        setContentView(menuBinding.root)
        supportActionBar?.hide()

        menuBinding.back.setOnClickListener {
            lastpage()
        }

        //init 按鈕們
        buttonAdapter = ButtonAdapter(this, poseNames) { posename ->
            nextpage(posename)
        }
        recyclerView = menuBinding.buttonContainer
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = buttonAdapter
        // 獲取索引為 0 的按鈕並設置 currentSelect
        recyclerView.post {
            val button = buttonAdapter.getButtonByIndex(0)
            if (button != null) {
                currentSelect = button
            }
        }

        // python start
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        python = Python.getInstance()

        // get yogaMat python module
        yogaMat = python.getModule("heatmap")

        // using yogaMat select and nextPage
        yogaMatFunctionThread = Thread {
            try {
                Thread.sleep(1000)
                while (threadFlag) {
                    functionNumber = yogaMat.callAttr("checkFunction").toInt()
                    runOnUiThread {
                        if (functionNumber == 1) {
                            right()
                        } else if (functionNumber == 2) {
                            up()
                        } else if (functionNumber == 3) {
                            left()
                        } else if (functionNumber == 4) {
                            down()
                        }
                    }
                    if (yogaMat.callAttr("checkReturn").toBoolean()) {
                        runOnUiThread{
                            nextpage()
                        }
                        break
                    }
                    Thread.sleep(750)
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            println("!!! Menu Done !!!")
        }

        yogaMatFunctionThread?.start()
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

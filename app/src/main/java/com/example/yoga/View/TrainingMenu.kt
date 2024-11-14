package com.example.yoga.View

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yoga.ViewModel.ButtonAdapter
import com.example.yoga.databinding.ActivityTrainingMenuBinding
//import com.example.yoga.Model.TrainingProcess;

import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.yoga.Model.GlobalVariable
import com.example.yoga.ViewModel.TrainingMenuViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TrainingMenu : AppCompatActivity() {
    private lateinit var trainingMenuBinding: ActivityTrainingMenuBinding
    private lateinit var recyclerView: RecyclerView
    private var global = GlobalVariable.getInstance()

    private val mode = "TrainingProcess"
    private var menuTitle = ""
    private var totalScore = 0.0
    private var totalTime = 0.0

//    private val trainingMenuViewModel: TrainingMenuViewModel by viewModels()

    private val poseNames = arrayOf(
        "早晨喚醒流", "全身強化訓練", "平衡與穩定練習", "中心強化流", "柔軟與伸展",
        "地獄核心訓練"
    )

    private val processListMap = mapOf(
        "早晨喚醒流" to arrayOf("Mountain pose", "Warrior2 Style", "Triangle pose", "Low Lunge", "Downward dog", "Child's pose"),
        "全身強化訓練" to arrayOf(
            "Tree Style",
            "Plank",
            "Cobra pose",
            "Boat pose",
            "Bridge pose",
            "Seated Forward Bend"
        ),
        "平衡與穩定練習" to arrayOf(
            "Locust pose",
            "Pigeon pose",
            "Half moon pose",
            "Reverse Plank",
            "Pyramid pose",
            "Chair pose"
        ),
        "中心強化流" to arrayOf(
            "Mountain pose",
            "Tree Style",
            "Warrior2 Style",
            "Triangle pose",
            "Downward dog",
            "Child's pose"
        ),
        "柔軟與伸展" to arrayOf(
            "Locust pose",
            "Cobra pose",
            "Camel pose",
            "Fish pose",
            "Low Lunge",
            "Bridge pose"
        ),
        "地獄核心訓練" to arrayOf(
            "Plank",
            "Boat pose",
            "Reverse Plank",
            "Low Lunge",
            "Chair pose",
            "Bridge pose"
        )
    )


    var poseList = arrayOf<String>()
    var currentIndex = 0

    private lateinit var buttonAdapter: ButtonAdapter

    // 上一頁
    fun lastpage() {
        val intent = Intent(this, ChooseMenu::class.java)
        startActivity(intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trainingMenuBinding = ActivityTrainingMenuBinding.inflate(layoutInflater)
        setContentView(trainingMenuBinding.root)
        supportActionBar?.hide()

        // 初始化按鈕
        buttonAdapter = ButtonAdapter(this, poseNames) { menuName ->
            poseList = processListMap[menuName] ?: arrayOf()
            menuTitle = menuName
            // 啟動 VideoGuide，並傳遞當前動作索引
            startTraining()
        }

        recyclerView = trainingMenuBinding.buttonContainer
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = buttonAdapter

        trainingMenuBinding.back.setOnClickListener {
            lastpage() // 返回上一頁
        }
    }

    private fun startTraining() {
        Log.d("Menu menuTitle", "$menuTitle")
        Log.d("Menu 目前總時間", "$totalTime")
        Log.d("Menu 目前總分", "$totalScore")
        val poseName = poseList[currentIndex]
        val intent = Intent(this, VideoGuide::class.java).apply {
            putExtra("mode", mode)
            putExtra("menuTitle", menuTitle)
            putExtra("poseList", poseList)
            putExtra("poseName", poseName)
            putExtra("currentIndex", currentIndex)
            putExtra("totalScore", totalScore)
            putExtra("totalTime", totalTime)
        }
        startActivity(intent)
        finish()
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
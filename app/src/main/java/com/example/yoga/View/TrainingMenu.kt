package com.example.yoga.View

import android.util.DisplayMetrics
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.util.Size
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yoga.ViewModel.ButtonAdapter
import com.example.yoga.databinding.ActivityTrainingMenuBinding
//import com.example.yoga.Model.TrainingProcess;

import androidx.activity.viewModels
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.yoga.Model.GlobalVariable
import com.example.yoga.ViewModel.TrainingMenuViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.yoga.Model.MainHandLandmarkViewModel
import com.example.yoga.Model.HandLandmarkerHelper
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TrainingMenu : AppCompatActivity(), HandLandmarkerHelper.LandmarkerListener {
    private lateinit var trainingMenuBinding: ActivityTrainingMenuBinding
    private lateinit var recyclerView: RecyclerView
    private var global = GlobalVariable.getInstance()
    private lateinit var currentSelect: Button

    // control UI by pose
    //拿mediapipe model
    private lateinit var handLandmarkerHelper: HandLandmarkerHelper
    private val handViewModel : MainHandLandmarkViewModel by viewModels()

    //分析圖片
    private var imageAnalyzer: ImageAnalysis? = null
    //前鏡頭
    private var camera: Camera? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT

    //開個thread
    private lateinit var backgroundExecutor: ExecutorService

    // yogaMat function
    private lateinit var python : Python
    private lateinit var yogaMat : PyObject
    private var yogaMatFunctionThread: Thread? = null
    private var threadFlag : Boolean = true
    private var functionNumber: Int = 0

    private val mode = "TrainingProcess"
    private var menuTitle = ""
    private var totalScore = 0.0
    private var totalTime = 0.0

    private var isExecuting = false
    val displayMetrics = DisplayMetrics()


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

    fun moveRollBarTo(index:Int){
        if (index in 0 until buttonAdapter.itemCount) {
            recyclerView.smoothScrollToPosition(index)
        } else {
            Log.e("moveRollBarTo", "Invalid index: $index")
        }
    }
    fun selectTo(index:Int){
        if (index in 0 until buttonAdapter.itemCount) {
            buttonAdapter.setSelectedIndex(index)
            currentSelect = buttonAdapter.getButtonByIndex(index)
            menuTitle = currentSelect.text.toString() // Update menuTitle
            poseList = processListMap[menuTitle] ?: arrayOf()
            Log.d("selectTo", "after buttonadapter setselectedindex ${currentSelect.text}")
            moveRollBarTo(index)
        } else {
            Log.e("selectTo", "Invalid index: $index")
        }
    }

    fun up() {
        if(!isExecuting) {
            isExecuting = true
            recyclerView.post {
                val currentPoseName = currentSelect.text.toString()
                val currentIndex = poseNames.indexOf(currentPoseName)
                val newIndex = if (currentIndex - 2 >= 0) currentIndex - 2 else currentIndex
                selectTo(newIndex)
                lifecycleScope.launch {
                    delay(1000)
                    isExecuting = false
                }
            }
        }
    }

    fun down() {
        if(!isExecuting){
            isExecuting = true
            recyclerView.post {
                val currentPoseName = currentSelect.text.toString()
                val currentIndex = poseNames.indexOf(currentPoseName)
                Log.d("down function", "${currentIndex} ${currentPoseName}")
                val newIndex = if (currentIndex+2 <= poseNames.size-1) currentIndex + 2 else currentIndex
                Log.d("down function", "${newIndex} ${buttonAdapter.getButtonByIndex(newIndex).text}")
                selectTo(newIndex)
                lifecycleScope.launch {
                    delay(1000)
                    isExecuting = false
                }
            }
        }
    }

    fun left() {
        if(!isExecuting) {
            isExecuting = true
            recyclerView.post {
                val currentPoseName = currentSelect.text.toString()
                val currentIndex = poseNames.indexOf(currentPoseName)
                val newIndex = if (currentIndex % 2 == 1) currentIndex-1 else currentIndex
                selectTo(newIndex)
                lifecycleScope.launch {
                    delay(1000)
                    isExecuting = false
                }
            }
        }
    }

    fun right() {
        if(!isExecuting) {
            isExecuting = true
            recyclerView.post {
                val currentPoseName = currentSelect.text.toString()
                val currentIndex = poseNames.indexOf(currentPoseName)
                val newIndex = if (currentIndex % 2 == 0) currentIndex+1 else currentIndex
                selectTo(newIndex)
                lifecycleScope.launch {
                    delay(1000)
                    isExecuting = false
                }
            }
        }
    }

    private fun startCamera() {
        lifecycleScope.launch {
            delay(800)
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // 获取 CameraProvider
            val cameraProvider : ProcessCameraProvider = cameraProviderFuture.get()
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraFacing = cameraManager.cameraIdList[0].toInt()

            // 配置预览
            val aspectRatio: Rational = Rational(4, 3) // 指定4:3的寬高比
            val size: Size = Size(aspectRatio.numerator, aspectRatio.denominator)

            // 配置相机选择器
            val cameraSelector : CameraSelector =
                CameraSelector.Builder().requireLensFacing(cameraFacing).build()

            // ImageAnalysis. Using RGBA 8888 to match how our models work
            imageAnalyzer =
                ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    //.setTargetRotation(CalibrationStageBinding.camera.display.rotation) // 模擬器需要指定旋轉
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    // The analyzer can then be assigned to the instance
                    .also {
                        it.setAnalyzer(backgroundExecutor) { image ->
                            detectPose(image)
                        }
                    }

            // 绑定相机和预览
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector , imageAnalyzer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun detectPose(imageProxy: ImageProxy) {
        if(this::handLandmarkerHelper.isInitialized) {
            handLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                //isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
                isFrontCamera = cameraFacing >= 0,
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trainingMenuBinding = ActivityTrainingMenuBinding.inflate(layoutInflater)
        setContentView(trainingMenuBinding.root)
        supportActionBar?.hide()

        trainingMenuBinding.back.setOnClickListener {
            lastpage() // 返回上一頁
        }

        backgroundExecutor = Executors.newSingleThreadExecutor()

        // 初始化 CameraX
        startCamera()
        //設定手部偵測的helper
        backgroundExecutor.execute {
            handLandmarkerHelper = HandLandmarkerHelper(
                context = this,
                runningMode = RunningMode.LIVE_STREAM,
                minHandDetectionConfidence = handViewModel.currentMinHandDetectionConfidence,
                minHandTrackingConfidence = handViewModel.currentMinHandTrackingConfidence,
                minHandPresenceConfidence = handViewModel.currentMinHandPresenceConfidence,
                currentDelegate = handViewModel.currentDelegate,
                handLandmarkerHelperListener = this
            )
        }

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

        // 獲取索引為 0 的按鈕並設置 currentSelect
        recyclerView.post {
            val button = buttonAdapter.getButtonByIndex(0)
            currentSelect = button
            buttonAdapter.setSelectedIndex(0) // Ensure the first button is selected by default
            menuTitle = currentSelect.text.toString() // Update menuTitle
            poseList = processListMap[menuTitle] ?: arrayOf()
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
                            startTraining()
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

    private fun startTraining() {
        threadFlag = false // to stop thread
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
            delay(500)
//            global.backgroundMusic.play()
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
        //關掉相機
        backgroundExecutor.shutdown()
//        global.backgroundMusic.pause()
    }

//    override fun onStop() {
//        super.onStop()
//        global.backgroundMusic.pause()
//    }

    override fun onError(error: String, errorCode: Int) {
        this.runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            if (errorCode == HandLandmarkerHelper.GPU_ERROR) {
                handViewModel.setDelegate(0)
            }
        }
    }

    override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
        this.runOnUiThread {

            // Pass the results to GestureOverlayView
            trainingMenuBinding.gestureOverlay.setResults(
                handLandmarkerResults = resultBundle.results.first(),
                imageHeight = displayMetrics.heightPixels,
                imageWidth = displayMetrics.widthPixels,
                runningMode = RunningMode.LIVE_STREAM
            )

            handlerDecider(resultBundle)
        }
    }

        private fun handlerDecider(resultBundle: HandLandmarkerHelper.ResultBundle){
        // if user face their palm to the camera
        val wholeFingerLandmark = resultBundle.results.first().landmarks().firstOrNull()

        //成功偵測到手部點位
        if (wholeFingerLandmark != null) {
            if (!handlePointingDirection(resultBundle)) {
                val wrist = wholeFingerLandmark[0]
                val thumbTIP = wholeFingerLandmark[4]
                val indexPIP = wholeFingerLandmark[6]
                val indexTIP = wholeFingerLandmark[8]
                val indexDIP = wholeFingerLandmark[7]
                val middlePIP = wholeFingerLandmark[10]
                val middleTIP = wholeFingerLandmark[12]
                val middleDIP = wholeFingerLandmark[11]
                val ringPIP = wholeFingerLandmark[14]
                val ringTIP = wholeFingerLandmark[16]
                val ringDIP = wholeFingerLandmark[15]
                val pinkyPIP = wholeFingerLandmark[18]
                val pinkyTIP = wholeFingerLandmark[20]
                val pinkyDIP = wholeFingerLandmark[19]

                when {
                    indexTIP.x() < indexDIP.x() && // 確認四隻手指皆為伸直狀態
                            middleTIP.x() < middleDIP.x() &&
                            ringTIP.x() < ringDIP.x() &&
                            pinkyTIP.x() < pinkyDIP.x() &&
                            indexDIP.x() < indexPIP.x() &&
                            middleDIP.x() < middlePIP.x() &&
                            ringDIP.x() < ringPIP.x() &&
                            pinkyDIP.x() < pinkyPIP.x() &&
                            indexTIP.x() < wrist.x() && // 四隻手指皆指向左側
                            middleTIP.x() < wrist.x() &&
                            ringTIP.x() < wrist.x() &&
                            pinkyTIP.x() < wrist.x() &&
                            thumbTIP.y() < wrist.y() && // 五隻手指皆位於手腕點位上方
                            indexTIP.y() < wrist.y() &&
                            middleTIP.y() < wrist.y() &&
                            ringTIP.y() < wrist.y() &&
                            pinkyTIP.y() < wrist.y() &&
                            thumbTIP.y() < indexTIP.y() -> { // 拇指的點位y離上方的距離較近，食指距離較遠
                        lifecycleScope.launch {
                            delay(1000)
                            Log.d("Detect Status", "上一頁")
                            if (threadFlag) {
                                runOnUiThread {
                                    lastpage()
                                }
                            }
                        }
                    }
                    indexTIP.x() > indexDIP.x() && // 確認四隻手指皆為伸直狀態
                            middleTIP.x() > middleDIP.x() &&
                            ringTIP.x() > ringDIP.x() &&
                            pinkyTIP.x() > pinkyDIP.x() &&
                            indexDIP.x() > indexPIP.x() &&
                            middleDIP.x() > middlePIP.x() &&
                            ringDIP.x() > ringPIP.x() &&
                            pinkyDIP.x() > pinkyPIP.x() &&
                            indexTIP.x() > wrist.x() && // 四隻手指皆指向右側
                            middleTIP.x() > wrist.x() &&
                            ringTIP.x() > wrist.x() &&
                            pinkyTIP.x() > wrist.x() &&
                            thumbTIP.y() < wrist.y() && // 五隻手指皆位於手腕點位上方
                            indexTIP.y() < wrist.y() &&
                            middleTIP.y() < wrist.y() &&
                            ringTIP.y() < wrist.y() &&
                            pinkyTIP.y() < wrist.y() &&
                            thumbTIP.y() < indexTIP.y() -> { // 拇指的點位y離上方的距離較近，食指距離較遠
                        lifecycleScope.launch {
                            delay(1000)
                            Log.d("Detect Status", "下一頁")
                            if (threadFlag) {
                                runOnUiThread {
                                    startTraining()
                                }
                            }
                        }
                    }
                    else -> {
                        Log.d("Detect Status", "no hand detected on the screen")
                    }
                }
            }
        } else {
            Log.d("Detect Status", "no hand detected on the screen")
        }
    }

    private fun handlePointingDirection(resultBundle: HandLandmarkerHelper.ResultBundle): Boolean {

        val wholeFingerLandmark = resultBundle.results.first().landmarks().firstOrNull()

        if (wholeFingerLandmark != null) {

            // Detect pointing direction using fingertips' landmarks
            // Get landmarks
            val wrist = wholeFingerLandmark[0]
            val thumbTip = wholeFingerLandmark[4]
            val indexTip = wholeFingerLandmark[8]
            val indexDIP = wholeFingerLandmark[7]

            val middleTip = wholeFingerLandmark[12]
            val middlePIP = wholeFingerLandmark[10]

            val ringTip = wholeFingerLandmark[16]
            val ringPIP = wholeFingerLandmark[14]

            val pinkyTip = wholeFingerLandmark[20]
            val pinkyPIP = wholeFingerLandmark[18]

            // Pointing Down
            if (thumbTip.y() < indexTip.y() &&
                indexDIP.y() < indexTip.y() &&
                indexTip.y() > wrist.y() &&
                middleTip.y() < middlePIP.y() &&
                ringTip.y() < ringPIP.y() &&
                pinkyTip.y() < pinkyPIP.y()) {

                Log.d("GestureDetection", "Pointing Down")
                Log.d("Detect Status", "向下指")
                down()
                return true
            }
            // Pointing Up
            else if (thumbTip.y() > indexTip.y() &&
                indexDIP.y() > indexTip.y() &&
                indexTip.y() < wrist.y() &&
                middleTip.y() > middlePIP.y() &&
                ringTip.y() > ringPIP.y() &&
                pinkyTip.y() > pinkyPIP.y()) {

                Log.d("GestureDetection", "Pointing Up")
                Log.d("Detect Status", "向上指")
                up()
                return true
            }
            // Pointing Left
            else if (indexTip.x() < thumbTip.x() &&
                indexDIP.x() > indexTip.x() &&
                indexTip.x() < wrist.x() &&
                middleTip.x() > middlePIP.x() &&
                ringTip.x() > ringPIP.x() &&
                pinkyTip.x() > pinkyPIP.x()) {

                Log.d("GestureDetection", "Pointing Left")
                Log.d("Detect Status", "向左指")
                left()
                return true
            }
            // Pointing Right
            else if (indexTip.x() > thumbTip.x() &&
                indexDIP.x() < indexTip.x() &&
                indexTip.x() > wrist.x() &&
                middleTip.x() < middlePIP.x() &&
                ringTip.x() < ringPIP.x() &&
                pinkyTip.x() < pinkyPIP.x()) {

                Log.d("GestureDetection", "Pointing Right")
                Log.d("Detect Status", "向右指")
                right()
                return true
            }
        }
        return false
    }
}
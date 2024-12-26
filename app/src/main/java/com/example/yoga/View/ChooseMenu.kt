package com.example.yoga.View

import android.util.DisplayMetrics
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.util.Size
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.yoga.Model.GlobalVariable
import com.example.yoga.Model.HandLandmarkerHelper
import com.example.yoga.Model.MainHandLandmarkViewModel
import com.example.yoga.databinding.ActivityChooseMenuBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ChooseMenu : AppCompatActivity(), HandLandmarkerHelper.LandmarkerListener {
    private lateinit var chooseMenuBinding:ActivityChooseMenuBinding
    private var global = GlobalVariable.getInstance()
    private lateinit var currentSelect: Button
    private lateinit var allPoseBtn: Button
    private lateinit var trainMenuBtn: Button

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

    private var isExecuting = false
    val displayMetrics = DisplayMetrics()


    fun lastpage() {
        threadFlag = false // to stop thread
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun nextpage() {
        Log.d("next page", "next page")
        threadFlag = false

        val intent: Intent = when (currentSelect) {
            allPoseBtn -> Intent(this, AllPoseMenu::class.java)
            trainMenuBtn -> Intent(this, TrainingMenu::class.java)
            else -> return // or handle the default case appropriately
        }
        startActivity(intent)
        finish()
    }

    fun left() {
        if(!isExecuting) {
            isExecuting = true
            selectButton(allPoseBtn)
            lifecycleScope.launch {
                delay(500)
                isExecuting = false
            }
        }
    }

    fun right() {
        if(!isExecuting) {
            isExecuting = true
            selectButton(trainMenuBtn)
            lifecycleScope.launch {
                delay(500)
                isExecuting = false
            }
        }
    }

    private fun startCamera() {
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
        chooseMenuBinding = ActivityChooseMenuBinding.inflate(layoutInflater)
        setContentView(chooseMenuBinding.root)
        supportActionBar?.hide()

        chooseMenuBinding.back.setOnClickListener{
            lastpage()
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

        // Initialize the "All Pose" and "Training Menu" button
        allPoseBtn = chooseMenuBinding.allPose
        trainMenuBtn = chooseMenuBinding.trainingMenu
        currentSelect = allPoseBtn
        updateButtonAppearance(currentSelect, true)

        // Set click listeners for buttons
        allPoseBtn.setOnClickListener {
            selectButton(allPoseBtn)
            val intent = Intent(this, AllPoseMenu::class.java)
            startActivity(intent)
            finish()
        }

        trainMenuBtn.setOnClickListener {
            selectButton(trainMenuBtn)
            val intent = Intent(this, TrainingMenu::class.java)
            startActivity(intent)
            finish()
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
                            //because no up
                            return@runOnUiThread
                        } else if (functionNumber == 3) {
                            left()
                        } else if (functionNumber == 4) {
                            //because no down
                            return@runOnUiThread
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

    private fun selectButton(button: Button) {
        // Deselect the current button
        updateButtonAppearance(currentSelect, false)
        // Select the new button
        currentSelect = button
        updateButtonAppearance(currentSelect, true)
    }

    private fun updateButtonAppearance(button: Button, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundColor(Color.rgb(10, 240, 5))
            button.setTextColor(Color.rgb(60, 60, 0))
            button.setShadowLayer(15f, 5f, 5f, Color.WHITE)
        } else {
            button.setBackgroundColor(Color.BLUE)
            button.setTextColor(Color.WHITE)
            button.setShadowLayer(0f, 0f, 0f, 0)
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
        //關掉相機
        backgroundExecutor.shutdown()
        global.backgroundMusic.pause()
    }

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
            chooseMenuBinding.gestureOverlay.setResults(
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
                                    nextpage()
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
                Log.d("Detect Status", "there‘s no pointing down function here")
            }
            // Pointing Up
            else if (thumbTip.y() > indexTip.y() &&
                indexDIP.y() > indexTip.y() &&
                indexTip.y() < wrist.y() &&
                middleTip.y() > middlePIP.y() &&
                ringTip.y() > ringPIP.y() &&
                pinkyTip.y() > pinkyPIP.y()) {

                Log.d("GestureDetection", "Pointing Up")
                Log.d("Detect Status", "there’s no pointing up function here")
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
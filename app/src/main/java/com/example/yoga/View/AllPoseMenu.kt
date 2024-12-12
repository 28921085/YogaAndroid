package com.example.yoga.View

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.util.Size
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
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
import com.example.yoga.Model.MainHandLandmarkViewModel
import com.example.yoga.Model.HandLandmarkerHelper
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AllPoseMenu : AppCompatActivity(), HandLandmarkerHelper.LandmarkerListener {
    private lateinit var menuBinding: ActivityAllPoseMenuBinding
    private var global = GlobalVariable.getInstance()
    private lateinit var currentSelect: Button

    private lateinit var recyclerView: RecyclerView

    private val mode="AllPose"

    private val poseNames = arrayOf(
            "Tree Style", "Warrior2 Style", "Plank", "Reverse Plank", "Child's pose",
            "Seated Forward Bend", "Low Lunge", "Downward dog", "Pyramid pose", "Bridge pose",
            "Mountain pose", "Triangle pose", "Locust Pose", "Cobra pose", "Half moon pose",
            "Boat pose", "Camel pose", "Pigeon pose", "Fish pose", "Chair pose"
    )

    private lateinit var buttonAdapter: ButtonAdapter

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

    // 手部偵測status描述
    private var angleshowtext:String = ""

    fun lastpage() {
        threadFlag = false // to stop thread

        val intent = Intent(this, ChooseMenu::class.java)
        startActivity(intent)
        finish()
    }

    fun nextpage() {
        threadFlag = false
        nextpage(currentSelect.text.toString())
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
        if (index in 0 until buttonAdapter.itemCount) {
            recyclerView.smoothScrollToPosition(index)
        } else {
            Log.e("moveRollBarTo", "Invalid index: $index")
        }
    }
    fun selectTo(index:Int){
        if (index in 0 until buttonAdapter.itemCount) {
            Log.d("selectTo", "Setting selected index to $index")
            buttonAdapter.setSelectedIndex(index)
            currentSelect = buttonAdapter.getButtonByIndex(index)
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
                val newIndex = if (currentIndex+2 <= poseNames.size-1) currentIndex + 2 else currentIndex
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
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // 获取 CameraProvider
            val cameraProvider : ProcessCameraProvider = cameraProviderFuture.get()
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraFacing = cameraManager.cameraIdList[0].toInt()

            // 配置预览
            val aspectRatio: Rational = Rational(4, 3) // 指定4:3的寬高比
            val size: Size = Size(aspectRatio.numerator, aspectRatio.denominator)

            val preview : Preview = Preview.Builder()
                .setTargetResolution(size)
                .build()
                .also {
                    it.setSurfaceProvider(menuBinding.camera.getSurfaceProvider())
                }

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
                    this, cameraSelector, preview , imageAnalyzer
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
        menuBinding = ActivityAllPoseMenuBinding.inflate(layoutInflater)
        setContentView(menuBinding.root)
        supportActionBar?.hide()

        menuBinding.back.setOnClickListener {
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
            currentSelect = button
            Log.d("OnCreate", "The initial selected button ${currentSelect.text} ${buttonAdapter.getIndexByButton(currentSelect)}")
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

        //縮小angle show 字體
        menuBinding.angleShow.textSize = 12.0f
        menuBinding.angleShow.postDelayed(updateRunnable,200)
    }

    private val updateRunnable =  object : Runnable {
        override fun run(){
            menuBinding.angleShow.text = angleshowtext
            menuBinding.angleShow.postDelayed(this, 200)
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
            menuBinding.gestureOverlay.setResults(
                handLandmarkerResults = resultBundle.results.first(),
                imageHeight = menuBinding.camera.height,
                imageWidth = menuBinding.camera.width,
                runningMode = RunningMode.LIVE_STREAM
            )

            handlerDecider(resultBundle)
        }
    }

    private fun handlerDecider(resultBundle: HandLandmarkerHelper.ResultBundle){
        // if user face their palm to the camera
        val wholeFingerLandmark = resultBundle.results.first().landmarks().firstOrNull()

        //成功偵測到手部點位
        if(wholeFingerLandmark != null){
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

            if( indexTIP.x() < indexDIP.x() && // 454~457: 確認四隻手指皆為伸直狀態
                middleTIP.x() < middleDIP.x() &&
                ringTIP.x() < ringDIP.x() &&
                pinkyTIP.x() < pinkyDIP.x() &&
                indexDIP.x() < indexPIP.x() &&
                middleDIP.x() < middlePIP.x() &&
                ringDIP.x() < ringPIP.x() &&
                pinkyDIP.x() < pinkyPIP.x() &&
                indexTIP.x() < wrist.x() && // 458~461: 四隻手指皆指向左側
                middleTIP.x() < wrist.x() &&
                ringTIP.x() < wrist.x() &&
                pinkyTIP.x() < wrist.x() &&
                thumbTIP.y() < wrist.y() && // 462~466: 五隻手指皆位於手腕點位上方
                indexTIP.y() < wrist.y() &&
                middleTIP.y() < wrist.y() &&
                ringTIP.y() < wrist.y() &&
                pinkyTIP.y() < wrist.y() &&
                thumbTIP.y() < indexTIP.y() // 467: 拇指的點位y離上方的距離較近，食指距離較遠
            ) {
                lifecycleScope.launch {
                    delay(1000)
                    menuBinding.angleShow.text = "上一頁"
                    if(threadFlag){
                        runOnUiThread {
                            lastpage()
                        }
                    }
                }
            }
            else if(indexTIP.x() > indexDIP.x() && // 479～482: 確認四隻手指皆為伸直狀態
                middleTIP.x() > middleDIP.x() &&
                ringTIP.x() > ringDIP.x() &&
                pinkyTIP.x() > pinkyDIP.x() &&
                indexDIP.x() > indexPIP.x() &&
                middleDIP.x() > middlePIP.x() &&
                ringDIP.x() > ringPIP.x() &&
                pinkyDIP.x() > pinkyPIP.x() &&
                indexTIP.x() > wrist.x() && // 483~486: 四隻手指皆指向右側
                middleTIP.x() > wrist.x() &&
                ringTIP.x() > wrist.x() &&
                pinkyTIP.x() > wrist.x() &&
                thumbTIP.y() < wrist.y() && // 487~491: 五隻手指皆位於手腕點位上方
                indexTIP.y() < wrist.y() &&
                middleTIP.y() < wrist.y() &&
                ringTIP.y() < wrist.y() &&
                pinkyTIP.y() < wrist.y() &&
                thumbTIP.y() < indexTIP.y() // 492: 拇指的點位y離上方的距離較近，食指距離較遠
            ){
                lifecycleScope.launch {
                    delay(1000)
                    menuBinding.angleShow.text = "下一頁"
                    if(threadFlag){
                        runOnUiThread {
                            Log.d("current select", currentSelect.text.toString())
                            nextpage(currentSelect.text.toString())
                        }
                    }
                }
            }else {
                handlePointingDirection(resultBundle)
            }
        }
        else {
            "no hand detected on the screen".also { menuBinding.angleShow.text = it }
        }
    }

    private fun handlePointingDirection(resultBundle: HandLandmarkerHelper.ResultBundle) {

        val wholeFingerLandmark = resultBundle.results.first().landmarks().firstOrNull()

        if (wholeFingerLandmark != null) {

            // Detect pointing direction using fingertips' landmarks
            // Get landmarks
            val wrist = wholeFingerLandmark.get(0)
            val thumbTip = wholeFingerLandmark.get(4)
            val indexTip = wholeFingerLandmark.get(8)
            val indexDIP = wholeFingerLandmark.get(7)

            val middleTip = wholeFingerLandmark.get(12)
            val middlePIP = wholeFingerLandmark.get(10)

            val ringTip = wholeFingerLandmark.get(16)
            val ringPIP = wholeFingerLandmark.get(14)

            val pinkyTip = wholeFingerLandmark.get(20)
            val pinkyPIP = wholeFingerLandmark.get(18)

            // Pointing Down
            if (thumbTip.y() < indexTip.y() &&
                indexDIP.y() < indexTip.y() &&
                indexTip.y() > wrist.y() &&
                middleTip.y() < middlePIP.y() &&
                ringTip.y() < ringPIP.y() &&
                pinkyTip.y() < pinkyPIP.y()) {

                Log.d("GestureDetection", "Pointing Down")
                menuBinding.angleShow.text = "向下指"
                down()
            }
            // Pointing Up
            else if (thumbTip.y() > indexTip.y() &&
                indexDIP.y() > indexTip.y() &&
                indexTip.y() < wrist.y() &&
                middleTip.y() > middlePIP.y() &&
                ringTip.y() > ringPIP.y() &&
                pinkyTip.y() > pinkyPIP.y()) {

                Log.d("GestureDetection", "Pointing Up")
                menuBinding.angleShow.text = "向上指"
                up()
            }
            // Pointing Left
            else if (indexTip.x() < thumbTip.x() &&
                indexDIP.x() > indexTip.x() &&
                indexTip.x() < wrist.x() &&
                middleTip.x() > middlePIP.x() &&
                ringTip.x() > ringPIP.x() &&
                pinkyTip.x() > pinkyPIP.x()) {

                Log.d("GestureDetection", "Pointing Left")
                menuBinding.angleShow.text = "向左指"
                left()
            }
            // Pointing Right
            else if (indexTip.x() > thumbTip.x() &&
                indexDIP.x() < indexTip.x() &&
                indexTip.x() > wrist.x() &&
                middleTip.x() < middlePIP.x() &&
                ringTip.x() < ringPIP.x() &&
                pinkyTip.x() < pinkyPIP.x()) {

                Log.d("GestureDetection", "Pointing Right")
                menuBinding.angleShow.text = "向右指"
                right()
            }
        }
    }

}

package com.example.yoga.View

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
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.yoga.Model.GlobalVariable
import com.example.yoga.Model.MainHandLandmarkViewModel
import com.example.yoga.Model.HandLandmarkerHelper
import com.example.yoga.databinding.ActivityMenuBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Menu : AppCompatActivity(), HandLandmarkerHelper.LandmarkerListener {
    private lateinit var menuBinding: ActivityMenuBinding
    private var global=GlobalVariable.getInstance()
    private lateinit var currentSelect:Button  //call by reference

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


    // for判斷左滑/右滑 紀錄手心底的x點位
    private var previousWristX: Float? = null
    private var firstWristX: Float? = null
    // 讓上下左右不會一直執行，執行完才會換下一個
    private var isExecuting = false

    // 手部偵測status描述
    private var angleshowtext:String = ""

    fun lastpage(){
        threadFlag = false // to stop thread

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("playMusic",false)
        }
        startActivity(intent)
        finish()
    }
    fun nextpage(){
        nextpage(currentSelect.text.toString())
    }
    fun nextpage(posename:String){
        threadFlag = false // to stop thread

        val intent = Intent(this, VideoGuide::class.java).apply {
            putExtra("poseName",posename)
        }
        startActivity(intent)
        finish()
    }
    fun select(){
        currentSelect.setBackgroundColor(Color.rgb(10,240,5))
        currentSelect.setTextColor(Color.rgb(60,60,0))
        currentSelect.setShadowLayer(15f,5f,5f,Color.WHITE)
    }
    fun unselect(){
        currentSelect.setBackgroundColor(Color.BLUE)
        currentSelect.setTextColor(Color.WHITE)
        currentSelect.setShadowLayer(0f,0f,0f,0)
    }
    fun selectTo(btn: Button){
        unselect()
        currentSelect = btn
        select()
    }
    fun up(){
        if(!isExecuting){
            isExecuting = true
            var next = when(currentSelect){
                menuBinding.button3 -> menuBinding.button1
                menuBinding.button4 -> menuBinding.button2
                menuBinding.button5 -> menuBinding.button3
                menuBinding.button6 -> menuBinding.button4
                menuBinding.button7 -> menuBinding.button5
                menuBinding.button8 -> menuBinding.button6
                menuBinding.button9 -> menuBinding.button7
                menuBinding.button10 -> menuBinding.button8
                else -> currentSelect
            }
            selectTo(next)
            lifecycleScope.launch {
                delay(1000)
                isExecuting = false
            }
        }
    }
    fun down(){
        if(!isExecuting){
            isExecuting = true
            var next = when(currentSelect){
                menuBinding.button1-> menuBinding.button3
                menuBinding.button2-> menuBinding.button4
                menuBinding.button3-> menuBinding.button5
                menuBinding.button4-> menuBinding.button6
                menuBinding.button5-> menuBinding.button7
                menuBinding.button6-> menuBinding.button8
                menuBinding.button7-> menuBinding.button9
                menuBinding.button8 ->menuBinding.button10
                else -> currentSelect
            }
            selectTo(next)
            lifecycleScope.launch {
                delay(1000)
                isExecuting = false
            }
        }
    }
    fun left(){
        if(!isExecuting){
            isExecuting = true
            var next = when(currentSelect){
                menuBinding.button2-> menuBinding.button1
                menuBinding.button4-> menuBinding.button3
                menuBinding.button6-> menuBinding.button5
                menuBinding.button8-> menuBinding.button7
                menuBinding.button10-> menuBinding.button9
                else -> currentSelect
            }
            selectTo(next)
            lifecycleScope.launch {
                delay(1000)
                isExecuting = false
            }
        }
    }
    fun right(){
        if(!isExecuting){
            isExecuting = true
            var next = when(currentSelect){
                menuBinding.button1-> menuBinding.button2
                menuBinding.button3-> menuBinding.button4
                menuBinding.button5-> menuBinding.button6
                menuBinding.button7-> menuBinding.button8
                menuBinding.button9-> menuBinding.button10
                else -> currentSelect
            }
            selectTo(next)
            lifecycleScope.launch {
                delay(1000)
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
        menuBinding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(menuBinding.root)
        supportActionBar?.hide() // 隐藏title bar

        currentSelect = menuBinding.button1

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

        menuBinding.button1.setBackgroundColor(Color.BLUE)
        menuBinding.button1.setOnClickListener {
            selectTo(menuBinding.button1)
            nextpage("Tree Style")
        }
        menuBinding.button2.setBackgroundColor(Color.BLUE)
        menuBinding.button2.setOnClickListener {
            selectTo(menuBinding.button2)
            nextpage("Warrior2 Style")
        }
        menuBinding.button3.setBackgroundColor(Color.BLUE)
        menuBinding.button3.setOnClickListener {
            selectTo(menuBinding.button3)
            nextpage("Plank")
        }
        menuBinding.button4.setBackgroundColor(Color.BLUE)
        menuBinding.button4.setOnClickListener {
            selectTo(menuBinding.button4)
            nextpage("Reverse Plank")
        }
        menuBinding.button5.setBackgroundColor(Color.BLUE)
        menuBinding.button5.setOnClickListener {
            selectTo(menuBinding.button5)
            nextpage("Child\'s pose")
        }
        menuBinding.button6.setBackgroundColor(Color.BLUE)
        menuBinding.button6.setOnClickListener {
            selectTo(menuBinding.button6)
            nextpage("Seated Forward Bend")
        }
        menuBinding.button7.setBackgroundColor(Color.BLUE)
        menuBinding.button7.setOnClickListener {
            selectTo(menuBinding.button7)
            nextpage("Low Lunge")
        }
        menuBinding.button8.setBackgroundColor(Color.BLUE)
        menuBinding.button8.setOnClickListener {
            selectTo(menuBinding.button8)
            nextpage("Downward dog")
        }
        menuBinding.button9.setBackgroundColor(Color.BLUE)
        menuBinding.button9.setOnClickListener {
            selectTo(menuBinding.button9)
            nextpage("Pyramid pose")
        }
        menuBinding.button10.setBackgroundColor(Color.BLUE)
        menuBinding.button10.setOnClickListener {
            selectTo(menuBinding.button10)
            nextpage("Bridge pose")
        }

        select()

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
            val wrist = wholeFingerLandmark.get(0)
            val thumbTIP = wholeFingerLandmark.get(4)
            val indexTIP = wholeFingerLandmark.get(8)
            val indexDIP = wholeFingerLandmark.get(7)
            val middleTIP = wholeFingerLandmark.get(12)
            val middleDIP = wholeFingerLandmark.get(11)
            val ringTIP = wholeFingerLandmark.get(16)
            val ringDIP = wholeFingerLandmark.get(15)
            val pinkyTIP = wholeFingerLandmark.get(20)
            val pinkyDIP = wholeFingerLandmark.get(19)

            if( indexTIP.x() < indexDIP.x() && // 454~457: 確認四隻手指皆為伸直狀態
                middleTIP.x() < middleDIP.x() &&
                ringTIP.x() < ringDIP.x() &&
                pinkyTIP.x() < pinkyDIP.x() &&
                indexTIP.x() < wrist.x() && // 458~461: 四隻手指皆指向右側
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

    private fun handleSwipePoseDetection(resultBundle: HandLandmarkerHelper.ResultBundle) {
        val wristLandmark = resultBundle.results.first().landmarks().firstOrNull()?.get(0)

        if (wristLandmark != null) {
            val currentWristX = wristLandmark.x()

            // 當手第一次在鏡頭裡被偵測到，紀錄下它的點位
            if (firstWristX == null) {
                firstWristX = currentWristX
            }

            firstWristX?.let { initialWristX ->
                previousWristX?.let { prevX ->
                    Log.d("value", "Value: $currentWristX and $prevX")
                    if (currentWristX > initialWristX + 0.2) {
                        Log.d("SwipeDetection", "Swiping right")
                        menuBinding.angleShow.text = "右滑"
                        if(threadFlag){
                            this.runOnUiThread {
                                nextpage(currentSelect.text.toString())
                            }
                        }
                    } else if (currentWristX < initialWristX - 0.2) {
                        Log.d("SwipeDetection", "Swiping left")
                        menuBinding.angleShow.text = "左滑"
                        if(threadFlag){
                            this.runOnUiThread {
                                lastpage()
                            }
                        }
                    } else {
                        Log.d("SwipeDetection", "Not Swiping")
                        firstWristX = currentWristX
                    }
                }
                previousWristX = currentWristX
            }
        }
        else {
            // Reset firstWrist if the hand is outside of the camera
            firstWristX = null
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
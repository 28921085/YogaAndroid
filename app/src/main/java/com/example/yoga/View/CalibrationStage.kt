package com.example.yoga.View

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Rational
import android.util.Size
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
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.yoga.Model.GlobalVariable
import com.example.yoga.Model.MainViewModel
import com.example.yoga.Model.PoseLandmarkerHelper
import com.example.yoga.R
import com.example.yoga.databinding.ActivityCalibrationStageBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CalibrationStage : AppCompatActivity() , PoseLandmarkerHelper.LandmarkerListener{
    //databinding 讓xml調用不綁R.id.xxx
    private lateinit var CalibrationStageBinding: ActivityCalibrationStageBinding
    //拿mediapipe model
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val viewModel : MainViewModel by viewModels()
    //分析圖片
    private var imageAnalyzer: ImageAnalysis? = null
    //前鏡頭
    private var camera: Camera? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT
    //開個thread
    private lateinit var backgroundExecutor: ExecutorService

    private var lastspeak:String = ""
    private var global=GlobalVariable.getInstance()
    private var delate_count : Int = 0

    // yogaMat nextPage
    private lateinit var python : Python
    private lateinit var yogaMat : PyObject
    private var yogaMatThread : Thread? = null
    private var threadFlag : Boolean = true

    fun nextpage(){

        threadFlag = false // to stop thread

        global.TTS.stop()
        //val intent = Intent(this, AllPoseMenu::class.java)
        val intent = Intent(this, ChooseMenu::class.java)
        startActivity(intent)
        finish()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // 隐藏title bar
        setContentView(R.layout.activity_calibration_stage)
        CalibrationStageBinding = ActivityCalibrationStageBinding.inflate(layoutInflater)

        global.TTS.speak(CalibrationStageBinding.guide.text.toString())

        setContentView(CalibrationStageBinding.root)
        CalibrationStageBinding.finish.setOnClickListener {
            nextpage()
        }
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // 初始化 CameraX
        startCamera()
        //設定PoseLandmarkerHelper
        backgroundExecutor.execute {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                    //context = requireContext(),
                    context = this,
                    runningMode = RunningMode.LIVE_STREAM,
                    minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                    minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                    minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                    currentDelegate = viewModel.currentDelegate,
                    poseLandmarkerHelperListener = this
            )
        }

        // python start
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
            println("!!! CalibrationStage Done !!!")
        }

        yogaMatThread?.start()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // 获取 CameraProvider
            val cameraProvider :ProcessCameraProvider = cameraProviderFuture.get()
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            // val cameraFacing = cameraManager.cameraIdList[0].toInt()
            // 配置预览
            val aspectRatio: Rational = Rational(4, 3) // 指定4:3的寬高比
            val size: Size = Size(aspectRatio.numerator, aspectRatio.denominator)

            val preview :Preview = Preview.Builder()
                .setTargetResolution(size)
                .build()
                .also {
                    it.setSurfaceProvider(CalibrationStageBinding.camera.getSurfaceProvider())
                }

            // 配置相机选择器
//            val cameraSelector : CameraSelector =
//                CameraSelector.Builder().requireLensFacing(cameraFacing).build()
            val cameraSelector : CameraSelector =
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
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
        if(this::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                    imageProxy = imageProxy,
                    //isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
                    isFrontCamera = cameraFacing >= 0
            )
        }
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            CalibrationStageBinding.camera.display.rotation
    }
    override fun onResults(
        resultBundle: PoseLandmarkerHelper.ResultBundle
    ){
        if(delate_count == 0){
            delate_count++
            this.runOnUiThread{
                if (resultBundle.results.first().landmarks().isNotEmpty()){
                    val count_node = resultBundle.results.first().landmarks()
                        .flatMap { nlandmarks ->
                            nlandmarks.map {
                                mutableListOf( it.visibility().orElse((-1.0).toFloat()).toFloat())
                            }
                        }
                        .count { it[0] > 0.5 }
                    println(count_node)
                    if (count_node < 16) //node 總數32, 先假設1/2沒偵測到,讓使用者調整人至鏡頭裡
                    {
                        lastspeak = CalibrationStageBinding.guide.text.toString()
                        CalibrationStageBinding.guide.text = "請將人調整至鏡頭裡以完成校正"
                        if (lastspeak != CalibrationStageBinding.guide.text) {
                            global.TTS.stop()
                            global.TTS.speak("請將人調整至鏡頭裡以完成校正")
                        }
                    }
                    else if (count_node > 16){
                        lastspeak = CalibrationStageBinding.guide.text.toString()
                        CalibrationStageBinding.guide.text = "完成校正"
                        if (lastspeak != CalibrationStageBinding.guide.text) {
                            global.TTS.stop()
                            global.TTS.speak("完成校正")
                        }
                    }
                }
            }
        }
        else if (delate_count == 5)
            delate_count=0
    }
    override fun onDestroy() {
        super.onDestroy()
        //關掉相機
        backgroundExecutor.shutdown()
        global.backgroundMusic.pause()
        global.TTS.stop()

    }
    override fun onError(error: String, errorCode: Int) {
        this.runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            if (errorCode == PoseLandmarkerHelper.GPU_ERROR) {
                viewModel.setDelegate(0)
            }
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
        global.TTS.stop()
        global.backgroundMusic.pause()
    }

    override fun onResume() {
        super.onResume()
        global.backgroundMusic.play()
    }

}
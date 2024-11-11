package com.example.yoga.View

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.util.Size
import android.widget.ImageView
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.yoga.Model.FinishTimer
import com.example.yoga.Model.MainViewModel
import com.example.yoga.Model.GlobalVariable
import com.example.yoga.Model.KSecCountdownTimer
import com.example.yoga.Model.PoseLandmarkerHelper
import com.example.yoga.Model.fileNameGetter
import com.example.yoga.R
import com.example.yoga.ViewModel.TrainingMenuViewModel
import com.example.yoga.databinding.ActivityYogaMainBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class YogaMain : AppCompatActivity() , PoseLandmarkerHelper.LandmarkerListener,KSecCountdownTimer.TimerCallback{

    var mode = ""
    var poseList = arrayOf<String>()
    var poseName=""
    var currentIndex = 0
    var menuTitle = ""
    var totalScore = 0.0
    var totalTime = 0.0

    //拿mediapipe model
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val viewModel : MainViewModel by viewModels()
    //分析圖片
    private var imageAnalyzer: ImageAnalysis? = null
    //databinding 讓xml調用不綁R.id.xxx
    private lateinit var yogamainBinding: ActivityYogaMainBinding
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT
    //開個thread
    private lateinit var backgroundExecutor: ExecutorService
    //前鏡頭
    private var camera: Camera? = null
    //python 物件
    private lateinit var python : Python
    private lateinit var pose     : PyObject
    private lateinit var heatmappy : PyObject
    private lateinit var yogamatProcessor : PyObject
    private lateinit var feetData : PyObject
    private lateinit var scoreCalculator : PyObject

    //判別文字是否更動用
    private var lastText="提示文字在這"
    private var TipsText=""
    //計時器
    private var timerCurrent = FinishTimer()
    private var timer30S = KSecCountdownTimer(7)
    //結算分數
    private var score = 99.0

    private var global=GlobalVariable.getInstance()
    //平滑化
    private var smoothedListQueue: MutableList<MutableList<MutableList<Float>> > = mutableListOf()
    private var len_of_landmark:Int = -1
    private var count_result : Int = 0

    // yogaMat nextPage
    private lateinit var yogaMat : PyObject
    private var yogaMatThread: Thread? = null
    private var heatMapThread: Thread? = null
    private var threadFlag : Boolean = true

    //file getter
    private var fileGetter=fileNameGetter()
    private val lock = Any()

    fun lastpage(){
        if(mode == "ALlPose"){
            threadFlag = false // to stop thread

            timer30S.stopTimer()
            timerCurrent.handlerStop()
            val intent = Intent(this, AllPoseMenu::class.java)
            startActivity(intent)
            finish()
        }
        else if(mode == "TrainingProcess"){
            timer30S.stopTimer()
            timerCurrent.handlerStop()
            val intent = Intent(this, TrainingMenu::class.java)
            startActivity(intent)
            finish()
        }

    }
    fun nextpage(){
        Log.d("訓練模式", "$mode")
        if(mode == "AllPose"){
            threadFlag = false // to stop thread

            val intent = Intent(this, YogaResult::class.java).apply {
                putExtra("title" ,yogamainBinding.title.text)
                putExtra("finishTime",timerCurrent.getTime())
                putExtra("score",score)
            }
            startActivity(intent)
            finish()
        }
        else if(mode == "TrainingProcess"){
//        else{
            totalTime = totalTime + timerCurrent.getTime()
            totalScore = totalScore + score
            Log.d("Main menuTitle", "$menuTitle")
            Log.d("Main 目前 index", "$currentIndex")
            Log.d("Main 目前總時間", "$totalTime")
            Log.d("Main 目前總分", "$totalScore")

            if(currentIndex < poseList.size-1) {
                val intent = Intent(this, RestInterval::class.java).apply {
                    putExtra("title", yogamainBinding.title.text)
//                putExtra("finishTime",timerCurrent.getTime())
//                    putExtra("score", score)
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
            }
            else{
                val intent = Intent(this, YogaResult::class.java).apply {
//                    putExtra("title" ,yogamainBinding.title.text)
                    putExtra("title" ,menuTitle)
                    putExtra("finishTime",totalTime)
                    putExtra("score",totalScore/poseList.size)
                }
                startActivity(intent)
                finish()
            }

        }
    }
    //30秒倒數結束
    override fun onTimerFinished() {
        timer30S.setRemainTimeStr("结束")
        timer30S.stopTimer()
        //停止计时
        timerCurrent.handlerStop()
        score = timerCurrent.getScore()
        nextpage()
    }
    //更新倒數條的顏色
    override fun updateColorBar(currentMs:Long,maxMS:Long) {
        val barColor = timer30S.getCurrentColor(currentMs)
        val layoutParams = yogamainBinding.timeLeftBar.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.matchConstraintPercentWidth = 0.47f*(currentMs/(1f*maxMS))
        yogamainBinding.timeLeftBar.layoutParams = layoutParams
        yogamainBinding.timeLeftBar.backgroundTintList = ColorStateList.valueOf(barColor)
    }
    //Timer TTS speak
    override fun timerSpeak(str: String) {
        if(str != "")
            TTSSpeak(str)
    }
    private fun setImage(imagePath: String?) {
        val picturePath = findViewById<ImageView>(R.id.guide_picture)
        var am: AssetManager? = null
        am = assets
        val pic = am.open(imagePath.toString())
        // Decode the input stream into a Drawable
        val drawable = Drawable.createFromStream(pic, null)
        // Set the drawable as the image source for the ImageView
        picturePath.setImageDrawable(drawable)
        // Close the input stream when you're done
        pic.close()
    }

    private fun TTSSpeak(str:String){
        global.TTS.stop()
        global.TTS.speak(str)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // 隐藏title bar

        //啟動python
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        python = Python.getInstance()

        //初始化yogamainBinding
        yogamainBinding = ActivityYogaMainBinding.inflate(layoutInflater)
        setContentView(yogamainBinding.root)

//        val poseName = intent.getStringExtra("poseName")

        mode = intent.getStringExtra("mode").toString()
        poseName = intent.getStringExtra("poseName").toString()

        if(mode=="TrainingProcess"){
            menuTitle = intent.getStringExtra("menuTitle").toString()
            poseList = intent.getStringArrayExtra("poseList")!!
            currentIndex = intent.getIntExtra("currentIndex", -1)
            totalScore = intent.getDoubleExtra("totalScore", 0.0)
            totalTime = intent.getDoubleExtra("totalTime", 0.0)
            yogamainBinding.title.text = menuTitle
        }
        else if (mode == "AllPose"){
            yogamainBinding.title.text = poseName
        }


        //開始計算完成時間
        timerCurrent.handlerStart()

        //啟動yogapose
        pose = python.getModule("yogaPoseDetect" ).callAttr("YogaPose",poseName)

        heatmappy = python.getModule("heatmap")

        yogamatProcessor = python.getModule("YogaMatProcessor").callAttr("YogaMatProcessor")

        yogamainBinding.title.text = poseName

        yogamainBinding.back.setOnClickListener { lastpage() }

        // 啟動分數計算器
        scoreCalculator = python.getModule("ScoreCalculator" ).callAttr("ScoreCalculator",poseName)

        //guide_picture init
        val picturePath = findViewById<ImageView>(R.id.guide_picture)
        var am: AssetManager? = null
        am = assets

        val pic = am.open("image/"+fileGetter.getDefaultPic(poseName)+".jpg")
        // Decode the input stream into a Drawable
        val drawable = Drawable.createFromStream(pic, null)
        // Set the drawable as the image source for the ImageView
        picturePath.setImageDrawable(drawable)
        // Close the input stream when you're done
        pic.close()

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
        // get yogaMat python module
        yogaMat = python.getModule("heatmap")

        // using yogaMat return
        yogaMatThread = Thread {
            try {
                Thread.sleep(3000)
                while (!yogaMat.callAttr("checkReturn").toBoolean() and threadFlag) {
                    Thread.sleep(100)
                }
                if(threadFlag){
                    runOnUiThread {
                        lastpage()
                    }
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            println("!!! YogaMain Done !!!")
        }

        yogaMatThread?.start()
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // 获取 CameraProvider
            val cameraProvider :ProcessCameraProvider = cameraProviderFuture.get()
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraFacing = cameraManager.cameraIdList[0].toInt()
            // 配置预览
            val aspectRatio: Rational = Rational(4, 3) // 指定4:3的寬高比
            val size: Size = Size(aspectRatio.numerator, aspectRatio.denominator)
            val preview :Preview = Preview.Builder()
                .setTargetResolution(size)
                .build()
                .also {
                    it.setSurfaceProvider(yogamainBinding.camera.getSurfaceProvider())
                }

            // 配置相机选择器
            val cameraSelector : CameraSelector =
                CameraSelector.Builder().requireLensFacing(cameraFacing).build()

            // ImageAnalysis. Using RGBA 8888 to match how our models work
            imageAnalyzer =
                ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    //.setTargetRotation(yogamainBinding.camera.display.rotation) // 模擬器需要指定旋轉
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
    private fun readBytesFromFile(filePath: String): ByteArray? {
        var fileBytes: ByteArray? = null
        try {
            val file = File(filePath)
            val fis = FileInputStream(file)
            fileBytes = fis.readBytes()
            fis.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return fileBytes
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            yogamainBinding.camera.display.rotation
    }
    // Update UI after pose have been detected. Extracts original
    // image height/width to scale and place the landmarks properly through
    // OverlayView
    override fun onResults(
        resultBundle: PoseLandmarkerHelper.ResultBundle
    ) {
        if (count_result == 0){
            count_result += 1
            this.runOnUiThread {

                var ArrowList: List<Float> = listOf()
                // heatmap 顯示 (目前沒用到)
                val heatmapexecutor: ExecutorService = Executors.newSingleThreadExecutor()
                /*thread {
                    heatmapexecutor.execute {
                        val fileName = "yourFile.txt"
                        val file = File(this.filesDir, fileName)
                        val filePath = file.path

                        val readByteArray = readBytesFromFile(filePath)

                        // 檢查 ByteArray 是否為空
                        if ((readByteArray != null) && readByteArray.isNotEmpty()) {
                            val bmp: Bitmap? =
                                BitmapFactory.decodeByteArray(readByteArray, 0, readByteArray.size)

                            // 檢查解碼的 Bitmap 是否為空
                            if (bmp != null) {
                                runOnUiThread {
                                    //yogamainBinding.imageView2.setImageBitmap(bmp)
                                }
                            } else {
                                // 處理解碼失敗的情況
                                Log.e("BitmapFactory", "Failed to decode ByteArray to Bitmap")
                            }
                        } else {
                            // 處理空的 ByteArray 情況
                            Log.e("BitmapFactory", "ByteArray is null or empty")
                        }
                    }
                }*/
                /*調換順序,先進行pose detect 得到各角度再丟到視圖上*/
                // pass result to Yogapose
                if (resultBundle.results.first().worldLandmarks().isNotEmpty()) {
                    val floatListList: List<MutableList<Any>> =
                        resultBundle.results.first().landmarks().flatMap { landmarks ->
                            landmarks.map { landmark ->
                                mutableListOf(landmark.x(), landmark.y(), landmark.z(), landmark.visibility().orElse((-1.0).toFloat()).toFloat())
                            }
                        }

                    val point2d: List<MutableList<Float>> =
                        resultBundle.results.first().landmarks().flatMap { nlandmarks ->
                            nlandmarks.map { landmark ->
                                mutableListOf(landmark.x(), landmark.y())
                            }
                        }

                    var center = heatmappy.callAttr("get_center")
                    // 取得腳在瑜珈墊上面的座標
                    var feet_data_str = yogamatProcessor.callAttr("generate_feet_data", point2d, floatListList)

                    // 如果沒有該腳的資料，會回傳 -999999
                    var left_x = yogamatProcessor.callAttr("get_left_foot_x").toFloat()
                    var left_y = yogamatProcessor.callAttr("get_left_foot_y").toFloat()
                    var right_x = yogamatProcessor.callAttr("get_right_foot_x").toFloat()
                    var right_y = yogamatProcessor.callAttr("get_right_foot_y").toFloat()

                    // 分數計算器
//                     var score = scoreCalculator.callAttr("calculate_score", floatListList, true)
                     println("score ${score}")
//                     yogamainBinding.score.text = "分數 ${score}"
//                    yogamainBinding.score.text = ""

                    yogamainBinding.yogaMat.setLeftFeetPosition(left_x, left_y);
                    yogamainBinding.yogaMat.setRightFeetPosition(right_x,right_y);

                    // Change pose tips
                    val detectlist = pose.callAttr("detect", floatListList , heatmappy.callAttr("get_rects") , center, feet_data_str).asList()

                    ArrowList = detectlist[2].asList().map{it.toFloat()}
                    println("ArrowList: $ArrowList")

                    val imagePath = detectlist[1].toString()
                    println("imagePath: $imagePath")
                    setImage(imagePath)

                    try {
                        TipsText = detectlist[0].toString()
                        println("TipsText: $TipsText")
                        if (lastText != TipsText)
                            TTSSpeak(TipsText)
                        lastText = TipsText

                    } catch (e: Exception) {
                        // Handle exceptions when accessing elements if the result doesn't behave like a list
                        println("Result does not have expected list behavior: ${e.message}")
                    }
                    yogamainBinding.guide.text = lastText
                    //30秒計時器
//                    if(true){//debug
                    if (lastText.contains("動作正確")) {
                        if (timer30S.isNotRunning())
                            timer30S.startTimer(this)
                    } else
                        timer30S.resetTimer()
                }
                if (resultBundle.results.first().landmarks().isNotEmpty()) {
                    val norfloatListList: List<MutableList<Float>> = resultBundle.results.first().landmarks().flatMap { nlandmarks ->
                        nlandmarks.map { landmark ->
                            mutableListOf( landmark.x(), landmark.y(), landmark.z() , landmark.visibility().orElse((-1.0).toFloat()).toFloat())
                        }
                    }

                    //val floatListList = resultBundle.results.first().landmarks()[0]
                    var smooth_floatListList: MutableList<MutableList<Float>> = mutableListOf()
                    //var middle_x_list: MutableList<Float> = mutableListOf()
                    //var middle_y_list: MutableList<Float> = mutableListOf()
                    //var middle_z_list: MutableList<Float> = mutableListOf()
                    //var middle_list: MutableList<MutableList<Float>> = mutableListOf()
                    if (norfloatListList.size != len_of_landmark && len_of_landmark != -1) {
                        smoothedListQueue.clear()
                        len_of_landmark = -1
                    }

                    len_of_landmark = norfloatListList.size
                    smoothedListQueue.add(norfloatListList.toMutableList())
                    if (smoothedListQueue.size > 3) {
                        smoothedListQueue.removeFirst()

                        smooth_floatListList = smoothedListQueue.first()
                        for (Fll in smoothedListQueue) {
                            if (Fll == smoothedListQueue.first())
                                continue
                            else {
                                for (i in Fll.indices) {
                                    smooth_floatListList[i][0] += Fll[i][0]
                                    smooth_floatListList[i][1] += Fll[i][1]
                                    smooth_floatListList[i][2] += Fll[i][2]
                                    smooth_floatListList[i][3] += Fll[i][3]
                                }
                            }
                        }
                        for (landmark in smooth_floatListList) {
                            landmark[0] = landmark[0] / smoothedListQueue.size
                            landmark[1] = landmark[1] / smoothedListQueue.size
                            landmark[2] = landmark[2] / smoothedListQueue.size
                            landmark[3] = landmark[3] / smoothedListQueue.size
                        }
                        // Pass necessary information to OverlayView for drawing on the canvas
                        yogamainBinding.overlay.setResults(
                                //resultBundle.results.first(),
                                smooth_floatListList,
                                resultBundle.inputImageWidth,
                                resultBundle.inputImageHeight,
                                RunningMode.LIVE_STREAM,
                                ArrowList
                        )
                    }

                }
                /*調換順序結尾*/
                // Force a redraw
                yogamainBinding.overlay.invalidate()
            }
        }else {
            count_result = 0
        }
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
    override fun onDestroy() {
        super.onDestroy()
        timer30S.stopTimer()
        timerCurrent.handlerStop()

        global.backgroundMusic.pause()
        //關掉相機
        backgroundExecutor.shutdown()
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
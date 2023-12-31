package com.example.yoga

import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.ImageButton
import com.example.yoga.databinding.ActivityMenuBinding
import com.example.yoga.databinding.ActivityYogaResultBinding

class Menu : AppCompatActivity() {
    private lateinit var menuBinding: ActivityMenuBinding
    private lateinit var mediaPlayer: MediaPlayer
    lateinit var global: GlobalVariable
    //data class menuReference(var currentSelect: Button)
    //private lateinit var currentSelect:menuReference  //call by reference
    private lateinit var currentSelect:Button  //call by reference
    fun lastpage(){
        global.currentMS = mediaPlayer.currentPosition
        mediaPlayer.stop()
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
        global.currentMS = mediaPlayer.currentPosition
        mediaPlayer.stop()
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
    }
    fun down(){
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
    }
    fun left(){
        var next = when(currentSelect){
            menuBinding.button2-> menuBinding.button1
            menuBinding.button4-> menuBinding.button3
            menuBinding.button6-> menuBinding.button5
            menuBinding.button8-> menuBinding.button7
            menuBinding.button10-> menuBinding.button9
            else -> currentSelect
        }
        selectTo(next)
    }
    fun right(){
        var next = when(currentSelect){
            menuBinding.button1-> menuBinding.button2
            menuBinding.button3-> menuBinding.button4
            menuBinding.button5-> menuBinding.button6
            menuBinding.button7-> menuBinding.button8
            menuBinding.button9-> menuBinding.button10
            else -> currentSelect
        }
        selectTo(next)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuBinding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(menuBinding.root)
        supportActionBar?.hide() // 隐藏title bar

        currentSelect = menuBinding.button1

        //controller debug
        //menuBinding.btnUp.setOnClickListener { up() }
        //menuBinding.btnDown.setOnClickListener { down() }
        //menuBinding.btnLeft.setOnClickListener { left() }
        //menuBinding.btnRight.setOnClickListener { right() }

        menuBinding.back.setOnClickListener {
            lastpage()
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
        global = application as GlobalVariable
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music)
        mediaPlayer.isLooping = true // 設定音樂循環播放
        mediaPlayer.seekTo(global.currentMS)
        mediaPlayer.start()

        select()
    }
    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }
}
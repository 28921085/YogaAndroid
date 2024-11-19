import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class MyTTS(){
    private var textToSpeech: TextToSpeech? = null
    fun init(context: Context) {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    // 初始化成功，可以在这里设置语言等参数
                    textToSpeech?.language = Locale.TAIWAN
                } else {
                    // 初始化失败，可以处理错误情况
                }
            }
        }
    }

    fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
    fun stop(){
        textToSpeech?.stop()
        Thread.sleep(50) // 短暫延遲，確保播放列隊清空
    }
}

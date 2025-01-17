package darylgorra.projectfinalgradius2016_20250110_kotlin

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer

class AudioManagerThread(private val context: Context, private val resourceId: Int, private val loop:Boolean) : Thread() {
    private lateinit var audioManager: AudioManager
    private var isRunning = true
    private var mediaPlayer: MediaPlayer? = null

    override fun run() {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        mediaPlayer = MediaPlayer.create(context, resourceId)
        mediaPlayer?.isLooping = loop // Set looping to true for continuous playback

        try {
            mediaPlayer?.start()
            while (isRunning) {
                // Keep the thread alive while music is playing
                sleep(100) // Adjust delay as needed
            }
        } catch (e: InterruptedException) {
            // Handle interruption (e.g., when the activity is paused or destroyed)
            mediaPlayer?.stop()
        } finally {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }


    fun stopMusic() {
        isRunning = false
        mediaPlayer?.stop()
    }
}
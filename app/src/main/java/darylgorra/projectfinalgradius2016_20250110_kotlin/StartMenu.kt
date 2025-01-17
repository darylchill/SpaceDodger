package darylgorra.projectfinalgradius2016_20250110_kotlin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class StartMenu : AppCompatActivity() {

    private lateinit var btnStart: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_start_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnStart = findViewById<View>(R.id.Start) as Button


        // Start background music
        val backgroundMusicThread = AudioManagerThread(this,R.raw.retor,true)
        backgroundMusicThread.start()


        btnStart.setOnClickListener { v: View ->
            if (v.id == R.id.Start) {
                startActivity(
                    Intent(
                        applicationContext,
                        GameEnviro::class.java
                    )
                )
                overridePendingTransition(R.anim.zoom_in, R.anim.fade_out) // Apply zoom in animation
            }
        }
    }
}
package darylgorra.projectfinalgradius2016_20250110_kotlin

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Display
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.lang.Thread.sleep
import java.util.Random


class GameEnviro : AppCompatActivity(), View.OnClickListener{

    private lateinit var spaceship: ImageView
    private lateinit var asteroidSmaller: ImageView
    private lateinit var asteroidSmall: ImageView
    private lateinit var asteroidBig: ImageView
    private lateinit var rootLayout: ViewGroup
    private var xShip = 0
    private var yShip = 0

    private var screenWidth = 0
    private var screenHeight = 0
    private var asteroidBigSpeed = 13
    private var asteroidSmallerSpeed = 18
    private var asteroidSmallSpeed = 20

    private lateinit var handler: android.os.Handler
    private lateinit var updateAsteroidPositionThread: Thread
    private var isGameRunning = false

    private lateinit var gameTime: TextView
    private lateinit var livesDisplay: TextView
    private lateinit var tryAgainButton: Button
    private lateinit var explosionAnimatedImage: ImageView

    private var explosionDuration: Int = 0
    private var startTime: Long = 0L
    private var timeInMilliseconds: Long = 0L
    private var timeSwapBuff: Long = 0L
    private var updatedTime: Long = 0L

    private var shipLives = 0

    private val random = Random()

    private lateinit var starFieldView: View
    private val backgroundCircles: MutableList<BackgroundCircle> = mutableListOf()
    private val paint = Paint()

    private var lastCollisionTime: Long = 0
    private val collisionDebounceTime: Long = 500 // 500 milliseconds debounce time


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game_enviro)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_game_enviro)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews() // Initialize all views and UI elements
        setupGameEnvironment() // Setup screen dimensions and initial game settings
        setupSpaceshipMovement() // Handle spaceship movement with touch controls
        createBackgroundCircles() // Create initial background circles

    }

    /**
     * Initialize views and set click listeners
     * Updated: 2025-01-10
     */
    private fun initializeViews() {
        handler = android.os.Handler()

        rootLayout = findViewById(R.id.activity_game_enviro)
        spaceship = findViewById(R.id.Spaceship)
        asteroidSmaller = findViewById(R.id.asteroidSmaller)
        asteroidSmall = findViewById(R.id.asteroidSmall)
        asteroidBig = findViewById(R.id.asteroid)


        explosionAnimatedImage = findViewById(R.id.explosionSprite)
        explosionAnimatedImage.setBackgroundResource(R.drawable.explosion_frames)

        // Initialize explosion animation
        val explosionAnimation = explosionAnimatedImage.background as AnimationDrawable
        explosionDuration = explosionAnimation.getDuration(0) * explosionAnimation.numberOfFrames

        tryAgainButton = findViewById(R.id.TryAgain)
        gameTime = findViewById(R.id.gameTimer)
        livesDisplay = findViewById(R.id.lives)

        tryAgainButton.setOnClickListener(this)
        tryAgainButton.visibility = View.INVISIBLE
        resetGame()
    }

    /**
     * Sets up the background animation to continuously scroll downwards.
     *
     * It uses a ValueAnimator to smoothly change the background image's bounds,
     * creating a scrolling effect. The animation repeats infinitely.
     */

    private fun applyLivesWarning(livesTextView: TextView, livesValue: Int) {
        livesDisplay.text = "$livesValue"
        var colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), Color.WHITE, Color.TRANSPARENT)
        if (livesValue < 30) {
            livesTextView.setTextColor(Color.RED)

            // Create a ValueAnimator for color blinking
            colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), Color.RED, Color.TRANSPARENT)
            colorAnimator.duration = 500 // Adjust blink duration as needed
            colorAnimator.repeatMode = ValueAnimator.REVERSE
            colorAnimator.repeatCount = ValueAnimator.INFINITE
            colorAnimator.addUpdateListener { animator ->
                livesTextView.setTextColor(animator.animatedValue as Int)
            }
            colorAnimator.start()
        }
        livesTextView.tag = colorAnimator // Store the animator for later cancellation
    }

    private fun setupGameEnvironment() {
        val window = windowManager
        val display: Display = window.defaultDisplay
        val size = Point()
        display.getSize(size)

        screenWidth = size.x
        screenHeight = size.y

        spaceship.x = screenWidth / 2f
        spaceship.y = screenHeight / 2f
        asteroidBig.visibility= View.INVISIBLE
        asteroidSmaller.visibility= View.INVISIBLE
        asteroidSmall.visibility= View.INVISIBLE



        // Adjust asteroid sizes based on screen dimensions
        val asteroidSizeFactor = (screenWidth + screenHeight) / 2000f // Adjust divisor for desired scaling
        asteroidBig.scaleX = asteroidSizeFactor * 0.75f // Big asteroid is now 75% of the original
        asteroidBig.scaleY = asteroidSizeFactor * 0.75f
        asteroidSmaller.scaleX = asteroidSizeFactor * 0.5f // Smaller asteroid is 50% of the original
        asteroidSmaller.scaleY = asteroidSizeFactor * 0.5f
        asteroidSmall.scaleX = asteroidSizeFactor * 0.35f // Smallest asteroid is 35% of the original
        asteroidSmall.scaleY = asteroidSizeFactor * 0.35f


        // Dynamically setting spaceship size based on screen width
        val spaceshipWidth = screenWidth / 5 // Example: 1/5th of the screen width
        val spaceshipHeight = spaceshipWidth // Keep the aspect ratio square

        val layoutParams = FrameLayout.LayoutParams(spaceshipWidth, spaceshipHeight)
        spaceship.layoutParams = layoutParams

        // Initialize starFieldView and add it to the root layout
        starFieldView = StarFieldView(this)
        rootLayout.addView(starFieldView, 0) // Add it as the first child (background)
    }

    private fun createBackgroundCircles() {
        // Create more circles for a denser background
        for (i in 0..30) {
            val x = random.nextInt(screenWidth).toFloat()
            val y = random.nextInt(screenHeight).toFloat()
            val color = Color.argb(random.nextInt(256), random.nextInt(256), random.nextInt(256), random.nextInt(256))
            val speed = random.nextInt(3) + 1 // Slower speeds for background
            backgroundCircles.add(BackgroundCircle(x, y, 5F, color, speed))
        }
    }


    private fun drawBackgroundCircles(canvas: Canvas) {
        for (circle in backgroundCircles) {
            paint.color = circle.color
            canvas.drawCircle(circle.x, circle.y, circle.radius, paint)
            circle.y += circle.speed
            if (circle.y > screenHeight + circle.radius) {
                circle.y = -circle.radius
            }
        }
        for (circle in backgroundCircles) { // Draw again to simulate depth
            paint.color = circle.color
            canvas.drawCircle(circle.x, circle.y, circle.radius, paint)

            // Update circle position for the next frame
            circle.y += circle.speed
            if (circle.y > screenHeight + circle.radius) {
                circle.y = -circle.radius // Reset to top when it goes off-screen
            }
        }
        // Request redraw to animate the circles
    }



    /**
     * Update game timer display
     * Updated: 2025-01-10
     */
    private val updateTimerThread: Runnable = object : Runnable {
        override fun run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime
            updatedTime = timeSwapBuff + timeInMilliseconds

            var secs = (updatedTime / 1000).toInt()
            val mins = secs / 60
            secs %= 60
            val milliseconds = (updatedTime % 1000).toInt()
            gameTime.text = String.format("%d:%02d:%03d", mins, secs, milliseconds)

            // Increase asteroid speed every 30 seconds by 5%
            if (secs % 30 == 0 && secs != 0) { // Every 30 seconds...
                asteroidBigSpeed = (asteroidBigSpeed * 1.05).toInt()
                asteroidSmallerSpeed = (asteroidSmallerSpeed * 1.05).toInt()
                asteroidSmallSpeed = (asteroidSmallSpeed * 1.05).toInt()

                // Cap the speed to a reasonable maximum if needed
            }
            handler.postDelayed(this, 50)
        }
    }


    /**
     * Update positions and sizes of asteroids
     * Updated: 2025-01-10
     */
    private fun updateAsteroidPositions() {
        updateAsteroidPositionThread = Thread {
            while (isGameRunning) {
                runOnUiThread {
                    moveAsteroid(asteroidSmaller, asteroidSmallerSpeed)
                    moveAsteroid(asteroidSmall, asteroidSmallSpeed)
                    moveAsteroid(asteroidBig, asteroidBigSpeed)

                    resizeAsteroid(asteroidSmall)

                    checkCollision(spaceship, asteroidSmaller)
                    checkCollision(spaceship, asteroidSmall)
                    checkCollision(spaceship, asteroidBig)
                }

                try {
                    sleep(20) // Adjust the delay as needed
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }

        updateAsteroidPositionThread.start()
    }


    /**
     * Move asteroid downward, reset position if it exits screen
     * Updated: 2025-01-10
     */
    private fun moveAsteroid(asteroid: ImageView, speed: Int) {
        asteroid.y += speed
        if (asteroid.y > screenHeight) {
            asteroid.y = -asteroid.height.toFloat()
            asteroid.x = random.nextInt(screenWidth - asteroid.width).toFloat()
        }
    }


    /**
     * Gradually resize asteroid to 3x its size
     * Updated: 2025-01-10
     */
    private fun resizeAsteroid(asteroid: ImageView) {
        val currentWidth = asteroid.width.toFloat()
        val currentHeight = asteroid.height.toFloat()
        if (currentWidth < 3 * 150) { // Check if the asteroid is already 3x its original size
            asteroid.layoutParams.width = (currentWidth * 1.005).toInt() // Gradually increase size by 0.5%
            asteroid.layoutParams.height = (currentHeight * 1.005).toInt() // Gradually increase size by 0.5%
            asteroid.requestLayout() // Apply the size changes
        }
    }

    /**
     * Check for collision between spaceship and asteroid
     * Updated: 2025-01-10
     */
    private fun checkCollision(ship: ImageView, asteroid: ImageView) {
        val shipX = ship.x
        val shipY = ship.y
        val shipWidth = ship.width.toFloat()
        val shipHeight = ship.height.toFloat()

        val asteroidX = asteroid.x
        val asteroidY = asteroid.y
        val asteroidWidth = asteroid.width.toFloat()
        val asteroidHeight = asteroid.height.toFloat()

        val shipCenterX = shipX + shipWidth / 2
        val collision = shipX < asteroidX + asteroidWidth && shipX + shipWidth > asteroidX &&
                shipY < asteroidY + asteroidHeight && shipY + shipHeight > asteroidY

        val currentTime = System.currentTimeMillis()

        // Check if enough time has passed since the last collision
        if (collision && currentTime - lastCollisionTime > collisionDebounceTime) {
            lastCollisionTime = currentTime // Update last collision time

            // Handle the collision
            val (explosionX, explosionY) = calculateExplosionPoint(ship, asteroid)
            showExplosion(explosionX, explosionY)
            handleCollision()


        }
    }


    /**
     * Calculates the point where the explosion should be displayed.
     *
     * This method takes the ship and asteroid as input and calculates the
     * approximate point of collision, which is used to position the explosion
     * animation.
     *
     * @param ship The ImageView representing the spaceship.
     * @param asteroid The ImageView representing the asteroid.
     * @return A Pair of Int values representing the x and y coordinates of
     * the explosion point.
     */
    private fun calculateExplosionPoint(ship: ImageView, asteroid: ImageView): Pair<Int, Int> {
        val collisionX = (ship.x + asteroid.x + asteroid.width / 2) / 2
        val collisionY = (ship.y + asteroid.y + asteroid.height / 2) / 2
        return Pair(collisionX.toInt(), collisionY.toInt())
    }


    /**
     * Handle collision: reduce lives, vibrate, and end game if needed
     * Updated: 2025-01-10
     */
    private fun handleCollision() {
        if (shipLives > 0) {
            vibrate(500)
            shipLives--
            applyLivesWarning(livesDisplay, shipLives)

        } else {
            // Stop the game immediately when lives reach 0
            spaceship.visibility = View.GONE

            endGame()
            return // Exit the function to prevent further execution
        }
    }

    /**
     * Shows explosion animation at the given coordinates
     */
    private fun showExplosion(x: Int, y: Int) {
        explosionAnimatedImage.visibility = View.VISIBLE

        // Adjust explosion position and size
        val explosionSize = spaceship.width // Example: Make explosion size equal to spaceship size -2
        explosionAnimatedImage.layoutParams.width = explosionSize
        explosionAnimatedImage.layoutParams.height = explosionSize
        explosionAnimatedImage.x = (x - explosionSize / 3).toFloat()
        explosionAnimatedImage.y = (y - explosionSize / 3).toFloat()

        // Start the explosion animation
        val explosionAnimation = explosionAnimatedImage.background as AnimationDrawable
        explosionAnimation.start()

        val soundEffectsThread = AudioManagerThread(this,R.raw.impact,false)
        soundEffectsThread.start()


        // Hide the explosion after the animation completes
        explosionAnimatedImage.postDelayed({
            explosionAnimatedImage.visibility = View.GONE
        }, explosionDuration.toLong()) // Use explosionDuration
    }


    /**
     * End game and navigate to save player screen
     * Updated: 2025-01-10
     */
    private fun endGame() {
        try {
            timeSwapBuff += timeInMilliseconds
            isGameRunning = false // Stop the game loop
            handler.removeCallbacks(updateTimerThread) // Stop timer updates
            val intent = Intent(this, SavePlayer::class.java)
            intent.putExtra("TIME", gameTime.text.toString())
            startActivity(intent)
        } catch (e: Exception) {
            // Handle exceptions, e.g., log the error
            e.printStackTrace()
        }
    }


    /**
     * Vibrate device to indicate collision
     * Updated: 2025-01-10
     */
    private fun vibrate(duration: Int) {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        duration.toLong(),
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                vibrator.vibrate(duration.toLong())
            }
        }
    }

    /**
     * Handle spaceship movement with touch listener
     * Updated: 2025-01-10
     */
    private fun setupSpaceshipMovement() {
        spaceship.setOnTouchListener { view: View, event: MotionEvent ->
            val x = event.rawX.toInt()
            val y = event.rawY.toInt()

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (livesDisplay.text == "PAUSE") {
                        // Resume the game
                        isGameRunning = true
                        startTime = SystemClock.uptimeMillis() - updatedTime // Adjust start time
                        handler.postDelayed(updateTimerThread, 0)
                        updateAsteroidPositions()
                        livesDisplay.text = shipLives.toString() // Restore lives display
                    } else {
                        // Start the game if not already running
                        if (startTime == 0L) {
                            xShip = x - (view.layoutParams as FrameLayout.LayoutParams).leftMargin
                            yShip = y - (view.layoutParams as FrameLayout.LayoutParams).topMargin

                            asteroidBig.visibility= View.VISIBLE
                            asteroidSmaller.visibility= View.VISIBLE
                            asteroidSmall.visibility= View.VISIBLE

                            startGameTimer()
                            startTime = SystemClock.uptimeMillis()

                            handler.postDelayed(updateTimerThread, 0)

                            isGameRunning = true
                            updateAsteroidPositions() // Start the game loop
                        }
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    val layoutParams = view.layoutParams as FrameLayout.LayoutParams
                    layoutParams.leftMargin = x - xShip
                    layoutParams.topMargin = y - yShip
                    view.layoutParams = layoutParams
                }
                MotionEvent.ACTION_UP -> {
                    // Pause the game
                    isGameRunning = false
                    handler.removeCallbacks(updateTimerThread)

                    // Update asteroid positions thread to pause movement
                    updateAsteroidPositionThread.interrupt()

                    livesDisplay.text = "PAUSE"
                }
            }
            rootLayout.invalidate()
            true
        }
    }



    /**
     * Reset game variables and restart timer
     * Updated: 2025-01-10
     */
    private fun resetGame() {
        shipLives = 100
        livesDisplay.text = String.format(shipLives.toString())
        livesDisplay.setTextColor(Color.WHITE)
        gameTime.text = "0.00"

        spaceship.visibility = View.VISIBLE
        asteroidBig.visibility = View.VISIBLE
        asteroidSmaller.visibility = View.VISIBLE
        asteroidSmall.visibility = View.VISIBLE
        explosionAnimatedImage.visibility = View.GONE // Ensure explosion is hidden
        isGameRunning = false // Stop any previous game loop
        tryAgainButton.visibility = View.INVISIBLE


    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.TryAgain) {
                resetGame()
            }
        }
    }

    private fun startGameTimer() {
        startTime = SystemClock.uptimeMillis()
        handler.postDelayed(updateTimerThread, 0)
    }


    /**
     * Represents a background circle with properties for position, size, color, and speed.
     */

    data class BackgroundCircle(
        var x: Float,
        var y: Float,
        val radius: Float,
        val color: Int,
        val speed: Int // Speed at which the circle moves downwards
    )

    /**
     * Custom View for drawing the star field background
     */
    inner class StarFieldView(context: android.content.Context) : View(context) {
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            drawBackgroundCircles(canvas)
            invalidate() // Request redraw for animation
        }
    }
}
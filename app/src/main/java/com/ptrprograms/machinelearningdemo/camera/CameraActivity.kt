package com.ptrprograms.machinelearningdemo.camera

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.view.WindowManager

import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata

import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

abstract class CameraActivity : Activity(), ImageReader.OnImageAvailableListener {

    private var mCameraHandler: CameraHandler? = null

    private var mBackgroundThread: HandlerThread? = null
    private var mBackgroundHandler: Handler? = null

    private var button: Button? = null
    private var exitButton: Button? = null

    private val mReady = AtomicBoolean(false)

    protected abstract val layoutResId: Int

    private val mInitializeOnBackground = Runnable {
        mCameraHandler = CameraHandler.instance
        mCameraHandler!!.initializeCamera(this@CameraActivity,
                PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT, mBackgroundHandler!!,
                this@CameraActivity)

        setReady(true)
    }

    private val mBackgroundClickHandler = Runnable { mCameraHandler!!.takePicture() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(layoutResId)

        init()
    }

    private fun init() {

        try {
            button = RainbowHat.openButtonA()
            button!!.setOnButtonEventListener { button, pressed ->
                Log.e("Test", "Button event")
                startImageCapture()
            }

            exitButton = RainbowHat.openButtonC()
            exitButton!!.setOnButtonEventListener { button, pressed ->
                cleanupActivity()
                finish()
            }
        } catch (e: IOException) {

        }

        mBackgroundThread = HandlerThread("BackgroundThread")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
        mBackgroundHandler!!.post(mInitializeOnBackground)
    }

    protected fun resetCamera() {
        mCameraHandler!!.shutDown()
        mBackgroundThread = HandlerThread("BackgroundThread")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
        mBackgroundHandler!!.post(mInitializeOnBackground)
    }

    @Throws(CameraAccessException::class)
    protected fun getRotationCompensation(activity: Activity): Int {

        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        var camIds: Array<String>? = null

        try {
            camIds = manager.cameraIdList
        } catch (e: CameraAccessException) {
            Log.w("Machine Learning", "Cannot get the list of available cameras", e)
        }

        if (camIds == null || camIds.size < 1) {
            Log.d("Machine Learning", "No cameras found")

            return 0
        }
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        val deviceRotation = activity.windowManager.defaultDisplay.rotation
        var rotationCompensation = ORIENTATIONS.get(deviceRotation)

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        val cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val sensorOrientation = cameraManager
                .getCameraCharacteristics(camIds[0])
                .get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        val result: Int
        when (rotationCompensation) {
            0 -> result = FirebaseVisionImageMetadata.ROTATION_0
            90 -> result = FirebaseVisionImageMetadata.ROTATION_90
            180 -> result = FirebaseVisionImageMetadata.ROTATION_180
            270 -> result = FirebaseVisionImageMetadata.ROTATION_270
            else -> {
                result = FirebaseVisionImageMetadata.ROTATION_0
                Log.e("Machine Learning", "Bad rotation value: $rotationCompensation")
            }
        }
        return result
    }

    protected fun convertImageToBitmap(image: Image): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    }

    private fun startImageCapture() {
        if (mReady.get()) {
            setReady(false)
            mBackgroundHandler!!.post(mBackgroundClickHandler)
        } else {

        }
    }

    protected fun setReady(ready: Boolean) {
        mReady.set(ready)
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupActivity()
    }

    protected fun cleanupActivity() {
        try {
            if (button != null) {
                button!!.close()
                button = null
            }
        } catch (e: IOException) {

        }

        try {
            if (exitButton != null) {
                exitButton!!.close()
                exitButton = null
            }
        } catch (e: IOException) {

        }

        try {
            if (mBackgroundThread != null) mBackgroundThread!!.quit()
        } catch (t: Throwable) {
            // close quietly
        }

        mBackgroundThread = null
        mBackgroundHandler = null

        try {
            if (mCameraHandler != null) mCameraHandler!!.shutDown()
        } catch (t: Throwable) {
            // close quietly
        }

    }

    abstract override fun onImageAvailable(reader: ImageReader)

    companion object {

        private val PREVIEW_IMAGE_WIDTH = 3280
        private val PREVIEW_IMAGE_HEIGHT = 2464

        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
    }
}

package com.ptrprograms.machinelearningdemo.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface

import java.util.Collections

class CameraHandler// Lazy-loaded singleton, so only one instance of the camera is created.
private constructor() {
    private var mCameraDevice: CameraDevice? = null
    private var mCaptureSession: CameraCaptureSession? = null
    private var initialized: Boolean = false

    /**
     * An [ImageReader] that handles still image capture.
     */
    private var mImageReader: ImageReader? = null


    /**
     * Callback handling device state changes
     */
    private val mStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            Log.d(TAG, "Opened camera.")
            mCameraDevice = cameraDevice
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            Log.d(TAG, "Camera disconnected, closing.")
            closeCaptureSession()
            cameraDevice.close()
        }

        override fun onError(cameraDevice: CameraDevice, i: Int) {
            Log.d(TAG, "Camera device error, closing.")
            closeCaptureSession()
            cameraDevice.close()
        }

        override fun onClosed(cameraDevice: CameraDevice) {
            Log.d(TAG, "Closed camera, releasing")
            mCameraDevice = null
        }
    }

    /**
     * Callback handling session state changes
     */
    private val mSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
            // The camera is already closed
            if (mCameraDevice == null) {
                Log.e("Test", "camera is already closed")
                return
            }
            // When the session is ready, we start capture.
            mCaptureSession = cameraCaptureSession
            triggerImageCapture()
        }

        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
            Log.w(TAG, "Failed to configure camera")
        }
    }

    /**
     * Callback handling capture session events
     */
    private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureProgressed(session: CameraCaptureSession,
                                         request: CaptureRequest,
                                         partialResult: CaptureResult) {
            Log.d(TAG, "Partial result")
        }

        override fun onCaptureCompleted(session: CameraCaptureSession,
                                        request: CaptureRequest,
                                        result: TotalCaptureResult) {

            Log.e("Test", "oncapturecomplete")
            session.close()
            mCaptureSession = null
            Log.d(TAG, "CaptureSession closed")
        }
    }

    private object InstanceHolder {
        val mCamera = CameraHandler()
    }

    /**
     * Initialize the camera device
     */
    @SuppressLint("MissingPermission")
    fun initializeCamera(context: Context, previewWidth: Int, previewHeight: Int,
                         backgroundHandler: Handler,
                         imageAvailableListener: ImageReader.OnImageAvailableListener) {
        if (initialized) {
            throw IllegalStateException(
                    "CameraHandler is already initialized or is initializing")
        }
        initialized = true

        // Discover the camera instance
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var camIds: Array<String>? = null
        try {
            camIds = manager.cameraIdList
        } catch (e: CameraAccessException) {
            Log.w(TAG, "Cannot get the list of available cameras", e)
        }

        if (camIds == null || camIds.size < 1) {
            Log.d(TAG, "No cameras found")
            return
        }
        Log.d(TAG, "Using camera id " + camIds[0])

        // Initialize the image processor
        mImageReader = ImageReader.newInstance(previewWidth, previewHeight, ImageFormat.JPEG,
                MAX_IMAGES)
        mImageReader!!.setOnImageAvailableListener(imageAvailableListener, backgroundHandler)

        // Open the camera resource
        try {
            manager.openCamera(camIds[0], mStateCallback, backgroundHandler)
        } catch (cae: CameraAccessException) {
            Log.d(TAG, "Camera access exception", cae)
        }

    }

    /**
     * Begin a still image capture
     */
    fun takePicture() {
        if (mCameraDevice == null) {
            Log.w(TAG, "Cannot capture image. Camera not initialized.")
            return
        }
        // Create a CameraCaptureSession for capturing still images.
        try {
            if (mImageReader == null) {
                Log.e("Test", "mimagereader is null")
            }

            mCameraDevice!!.createCaptureSession(
                    listOf(mImageReader!!.surface),
                    mSessionCallback, null)
        } catch (cae: CameraAccessException) {
            Log.e(TAG, "Cannot create camera capture session", cae)
        }

    }

    /**
     * Execute a new capture request within the active session
     */
    private fun triggerImageCapture() {
        try {
            val captureBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            Log.e("Test", "triggerimagecapture")
            captureBuilder.addTarget(mImageReader!!.surface)
            Log.e("Test", "after")
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            Log.d(TAG, "Capture request created.")
            mCaptureSession!!.capture(captureBuilder.build(), mCaptureCallback, null)
        } catch (cae: CameraAccessException) {
            Log.e(TAG, "Cannot trigger a capture request")
        }

    }

    private fun closeCaptureSession() {
        if (mCaptureSession != null) {
            try {
                mCaptureSession!!.close()
            } catch (ex: Exception) {
                Log.w(TAG, "Could not close capture session", ex)
            }

            mCaptureSession = null
        }
    }

    /**
     * Close the camera resources
     */
    fun shutDown() {
        try {
            closeCaptureSession()
            if (mCameraDevice != null) {
                mCameraDevice!!.close()
            }
            mImageReader!!.close()
        } finally {
            initialized = false
        }
    }

    companion object {
        private val TAG = CameraHandler::class.java.simpleName

        private val MAX_IMAGES = 1

        val instance: CameraHandler
            get() = InstanceHolder.mCamera

        /**
         * Helpful debugging method:  Dump all supported camera formats to log.  You don't need to run
         * this for normal operation, but it's very helpful when porting this code to different
         * hardware.
         */
        fun dumpFormatInfo(context: Context) {
            // Discover the camera instance
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            var camIds: Array<String>? = null
            try {
                camIds = manager.cameraIdList
            } catch (e: CameraAccessException) {
                Log.w(TAG, "Cannot get the list of available cameras", e)
            }

            if (camIds == null || camIds.size < 1) {
                Log.d(TAG, "No cameras found")
                return
            }
            Log.d(TAG, "Using camera id " + camIds[0])
            try {
                val characteristics = manager.getCameraCharacteristics(camIds[0])
                val configs = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                for (format in configs!!.outputFormats) {
                    Log.d(TAG, "Getting sizes for format: $format")
                    for (s in configs.getOutputSizes(format)) {
                        Log.d(TAG, "\t" + s.toString())
                    }
                }
                val effects = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS)
                for (effect in effects!!) {
                    Log.d(TAG, "Effect available: $effect")
                }
            } catch (e: CameraAccessException) {
                Log.d(TAG, "Cam access exception getting characteristics.")
            }

        }
    }

}

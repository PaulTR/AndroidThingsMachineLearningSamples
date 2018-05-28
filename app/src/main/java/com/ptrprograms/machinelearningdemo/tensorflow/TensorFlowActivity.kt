package com.ptrprograms.machinelearningdemo.tensorflow

import android.media.ImageReader
import com.ptrprograms.machinelearningdemo.R
import com.ptrprograms.machinelearningdemo.camera.CameraActivity
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.IOException

class TensorFlowActivity(override val layoutResId: Int = R.layout.activity_camera) : CameraActivity() {

    private val TF_INPUT_IMAGE_WIDTH = 224
    private val TF_INPUT_IMAGE_HEIGHT = 224

    private var mImagePreprocessor: ImagePreprocessor? = null
    private var tensorFlowImageClassifier: TensorFlowImageClassifier? = null

    override fun onImageAvailable(reader: ImageReader) {
        val image = reader.acquireNextImage()
        val bitmap = mImagePreprocessor!!.preprocessImage(image)

        runOnUiThread { camera_image.setImageBitmap(bitmap) }

        val result : String = tensorFlowImageClassifier!!.classifyFrame(bitmap)

        runOnUiThread {
            if( result.isEmpty() ) {
                camera_text.setText("Not sure")
            } else {
                camera_text.setText(result)
            }
        }

        setReady(true)
        reader.close()
        resetCamera()
    }

    override fun initCustomObjects() {
        super.initCustomObjects()
        try {
            tensorFlowImageClassifier = TensorFlowImageClassifier(this@TensorFlowActivity)
        } catch (e: IOException) {
            throw IllegalStateException("Cannot initialize TFLite Classifier", e)
        }

        mImagePreprocessor = ImagePreprocessor(CameraActivity.PREVIEW_IMAGE_WIDTH, CameraActivity.PREVIEW_IMAGE_HEIGHT,
                TF_INPUT_IMAGE_WIDTH, TF_INPUT_IMAGE_HEIGHT)
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            tensorFlowImageClassifier?.close()
        } catch (t: Throwable) {
            // close quietly
        }
    }
}

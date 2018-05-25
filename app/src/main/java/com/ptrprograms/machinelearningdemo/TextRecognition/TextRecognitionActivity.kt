package com.ptrprograms.machinelearningdemo.TextRecognition

import android.hardware.camera2.CameraAccessException
import android.media.ImageReader
import android.util.Log
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.ptrprograms.machinelearningdemo.R
import com.ptrprograms.machinelearningdemo.camera.CameraActivity
import kotlinx.android.synthetic.main.activity_camera.*

class TextRecognitionActivity(override val layoutResId: Int = R.layout.activity_camera) : CameraActivity() {

    override fun onImageAvailable(reader: ImageReader) {
        Log.e("Test", "image available")
        val image = reader.acquireNextImage()
        runOnUiThread { camera_image.setImageBitmap(convertImageToBitmap(image)) }
        var rotation = 0

        try {
            rotation = getRotationCompensation(this)
        } catch (e: CameraAccessException) {
            Log.e("Machine Learning", "camera access exception")
        }

        val firebaseVisionImage = FirebaseVisionImage.fromMediaImage(image, rotation)

        val detector = FirebaseVision.getInstance()
                .visionTextDetector

        val result = detector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener { firebaseVisionText ->
                    Log.e("Machine Learning", "success!")

                    var blocks = ArrayList<String>()
                    for (block in firebaseVisionText.blocks) {
                        Log.e("Machine Learning", block.text)
                        blocks.add(block.text)
                    }

                    camera_text.setText(blocks.joinToString())
                }
                .addOnFailureListener { e -> Log.e("Machine Learning", "failure :( " + e.message) }

        setReady(true)

        reader.close()

        resetCamera()
    }

}

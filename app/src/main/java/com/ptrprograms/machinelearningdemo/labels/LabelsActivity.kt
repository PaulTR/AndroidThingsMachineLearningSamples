package com.ptrprograms.machinelearningdemo.labels

import android.hardware.camera2.CameraAccessException
import android.media.ImageReader
import android.util.Log
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.ptrprograms.machinelearningdemo.R
import com.ptrprograms.machinelearningdemo.camera.CameraActivity
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions
import kotlinx.android.synthetic.main.activity_camera.*


class LabelsActivity(override val layoutResId: Int = R.layout.activity_camera) : CameraActivity() {

    override fun onImageAvailable(reader: ImageReader) {
        val image = reader.acquireNextImage()
        runOnUiThread { camera_image.setImageBitmap(convertImageToBitmap(image)) }
        var rotation = 0

        try {
            rotation = getRotationCompensation(this)
        } catch (e: CameraAccessException) {
            Log.e("Machine Learning", "camera access exception")
        }

        val firebaseVisionImage = FirebaseVisionImage.fromMediaImage(image, rotation)

        val options = FirebaseVisionLabelDetectorOptions.Builder()
                .setConfidenceThreshold(0.8f)
                .build()

        val detector = FirebaseVision.getInstance()
                .getVisionLabelDetector(options)

        val result = detector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener { firebaseVisionLabels ->
                    Log.e("Machine Learning", "success!")

                    var labels = ArrayList<String>()
                    for (label in firebaseVisionLabels) {
                        labels.add(label.label)
                    }

                    camera_text.setText(labels.joinToString())
                }
                .addOnFailureListener { e -> Log.e("Machine Learning", "failure :( " + e.message) }

        setReady(true)

        reader.close()

        resetCamera()
    }

}

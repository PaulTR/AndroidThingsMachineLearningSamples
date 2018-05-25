package com.ptrprograms.machinelearningdemo.landmarks

import android.hardware.camera2.CameraAccessException
import android.media.ImageReader
import android.util.Log
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.ptrprograms.machinelearningdemo.R
import com.ptrprograms.machinelearningdemo.camera.CameraActivity
import kotlinx.android.synthetic.main.activity_face_detection.*
import kotlinx.android.synthetic.main.activity_landmark.*
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions



class LandmarkDetectionActivity(override val layoutResId: Int = R.layout.activity_landmark) : CameraActivity() {

    override fun onImageAvailable(reader: ImageReader) {
        Log.e("Test", "image available")
        val image = reader.acquireNextImage()
        runOnUiThread { landmark_overlay.bitmap = convertImageToBitmap(image) }
        var rotation = 0

        try {
            rotation = getRotationCompensation(this)
        } catch (e: CameraAccessException) {
            Log.e("Machine Learning", "camera access exception")
        }

        val firebaseVisionImage = FirebaseVisionImage.fromMediaImage(image, rotation)

        val options = FirebaseVisionCloudDetectorOptions.Builder()
                .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                .setMaxResults(5)
                .build()

        val detector = FirebaseVision.getInstance()
                .getVisionCloudLandmarkDetector(options)

        val result = detector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener { landmarks ->
                    landmark_overlay.landmarks = landmarks
                    landmark_overlay.invalidate()
                }
                .addOnFailureListener { Log.e("Test", "onfailure") }

        setReady(true)
        reader.close()
        resetCamera()
    }

}

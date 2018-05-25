package com.ptrprograms.machinelearningdemo.facedetection

import android.hardware.camera2.CameraAccessException
import android.media.ImageReader
import android.util.Log
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.ptrprograms.machinelearningdemo.R
import com.ptrprograms.machinelearningdemo.camera.CameraActivity
import kotlinx.android.synthetic.main.activity_face_detection.*

class FaceDetectionActivity(override val layoutResId: Int = R.layout.activity_face_detection) : CameraActivity() {

    override fun onImageAvailable(reader: ImageReader) {
        Log.e("Test", "image available")
        val image = reader.acquireNextImage()
        runOnUiThread { face_overlay.bitmap = convertImageToBitmap(image) }
        var rotation = 0

        try {
            rotation = getRotationCompensation(this)
        } catch (e: CameraAccessException) {
            Log.e("Machine Learning", "camera access exception")
        }

        val firebaseVisionImage = FirebaseVisionImage.fromMediaImage(image, rotation)

        val options = FirebaseVisionFaceDetectorOptions.Builder()
                .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .setTrackingEnabled(true)
                .build()

        val detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options)

        val result = detector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener { faces ->
                    Log.e("Test", "onsuccess")
                    face_overlay.faces = faces
                    face_overlay.invalidate()
                }
                .addOnFailureListener { Log.e("Test", "onfailure") }

        setReady(true)
        reader.close()
        resetCamera()
    }

}

package com.ptrprograms.machinelearningdemo.barcodereader

import android.hardware.camera2.CameraAccessException
import android.media.ImageReader
import android.util.Log
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.ptrprograms.machinelearningdemo.R
import com.ptrprograms.machinelearningdemo.camera.CameraActivity
import kotlinx.android.synthetic.main.activity_barcode.*

class BarCodeReaderActivity : CameraActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_barcode
    }

    override fun onImageAvailable(reader: ImageReader) {
        val image = reader.acquireNextImage()
        runOnUiThread { barcode_overlay.bitmap = convertImageToBitmap(image) }
        var rotation = 0

        try {
            rotation = getRotationCompensation(this)
        } catch (e: CameraAccessException) {
            Log.e("Machine Learning", "camera access exception")
        }

        val firebaseVisionImage = FirebaseVisionImage.fromMediaImage(image, rotation)

        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(
                        FirebaseVisionBarcode.FORMAT_ALL_FORMATS)
                .build()

        val detector = FirebaseVision.getInstance()
                .getVisionBarcodeDetector(options)

        val result = detector
                .detectInImage(firebaseVisionImage)
                .addOnSuccessListener { firebaseVisionBarcodes ->
                    Log.e("Test", "barcodes found: " + firebaseVisionBarcodes.size)
                    barcode_overlay.barcodes = firebaseVisionBarcodes
                    barcode_overlay.invalidate()
                }
                .addOnFailureListener { Log.e("Test", "failed to find barcodes") }

        setReady(true)
        reader.close()
        resetCamera()
    }

}

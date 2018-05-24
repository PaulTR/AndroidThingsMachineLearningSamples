package com.ptrprograms.machinelearningdemo.barcodereader;

import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.ptrprograms.machinelearningdemo.R;
import com.ptrprograms.machinelearningdemo.barcodereader.BarcodeOverlayView;
import com.ptrprograms.machinelearningdemo.camera.CameraActivity;

import java.util.List;

public class BarCodeReaderActivity extends CameraActivity {


    protected BarcodeOverlayView barcodeOverlayView;

    @Override
    protected void initViews() {
        barcodeOverlayView = findViewById(R.id.barcode_overlay);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_barcode;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        final Image image = reader.acquireNextImage();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                barcodeOverlayView.setBitmap(convertImageToBitmap(image));
            }
        });
        int rotation = 0;

        try {
            rotation = getRotationCompensation(this);
        } catch( CameraAccessException e ) {
            Log.e("Machine Learning", "camera access exception");
        }

        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromMediaImage(image, rotation);

        FirebaseVisionBarcodeDetectorOptions options =
                new FirebaseVisionBarcodeDetectorOptions.Builder()
                        .setBarcodeFormats(
                                FirebaseVisionBarcode.FORMAT_ALL_FORMATS)
                        .build();

        FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance()
                .getVisionBarcodeDetector(options);

        Task<List<FirebaseVisionBarcode>> result =
                detector
                        .detectInImage(firebaseVisionImage)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {

                        Log.e("Test", "barcodes found: " + firebaseVisionBarcodes.size());
                        barcodeOverlayView.setBarcodeData(firebaseVisionBarcodes);
                        barcodeOverlayView.invalidate();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Test", "failed to find barcodes");
                    }
                });

        setReady(true);
        reader.close();
        resetCamera();
    }

}

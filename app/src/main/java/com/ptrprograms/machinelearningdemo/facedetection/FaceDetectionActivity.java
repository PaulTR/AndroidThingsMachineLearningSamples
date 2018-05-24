package com.ptrprograms.machinelearningdemo.facedetection;

import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.ptrprograms.machinelearningdemo.R;
import com.ptrprograms.machinelearningdemo.camera.CameraActivity;
import com.ptrprograms.machinelearningdemo.facedetection.FaceOverlayView;

import java.util.List;

public class FaceDetectionActivity extends CameraActivity {

    private FaceOverlayView faceOverlayView;

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_face_detection;
    }

    @Override
    protected void initViews() {
        faceOverlayView = findViewById(R.id.face_overlay);
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        Log.e("Test", "image available");
        final Image image = reader.acquireNextImage();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faceOverlayView.setBitmap(convertImageToBitmap(image));
            }
        });
        int rotation = 0;

        try {
            rotation = getRotationCompensation(this);
        } catch( CameraAccessException e ) {
            Log.e("Machine Learning", "camera access exception");
        }

        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromMediaImage(image, rotation);

        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                        .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .setTrackingEnabled(true)
                        .build();

        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);

        Task<List<FirebaseVisionFace>> result =
                detector.detectInImage(firebaseVisionImage)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionFace>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                        Log.e("Test", "onsuccess");
                                        faceOverlayView.setFaceData(faces);
                                        faceOverlayView.invalidate();
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e("Test", "onfailure");
                                    }
                                });

        setReady(true);
        reader.close();
        resetCamera();
    }

}

package com.ptrprograms.machinelearningdemo.TextRecognition;

import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;
import com.ptrprograms.machinelearningdemo.R;
import com.ptrprograms.machinelearningdemo.camera.CameraActivity;

public class TextRecognitionActivity extends CameraActivity {

    protected ImageView imageView;

    @Override
    protected void initViews() {
        imageView = findViewById(R.id.image);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_camera;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        Log.e("Test", "image available");
        final Image image = reader.acquireNextImage();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(convertImageToBitmap(image));
            }
        });
        int rotation = 0;

        try {
            rotation = getRotationCompensation(this);
        } catch( CameraAccessException e ) {
            Log.e("Machine Learning", "camera access exception");
        }

        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromMediaImage(image, rotation);

        FirebaseVisionTextDetector detector = FirebaseVision.getInstance()
                .getVisionTextDetector();

        Task<FirebaseVisionText> result =
                detector.detectInImage(firebaseVisionImage)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                Log.e("Machine Learning", "success!");

                                for( FirebaseVisionText.Block block : firebaseVisionText.getBlocks() ) {
                                    Log.e("Machine Learning", block.getText() );
                                }
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e("Machine Learning", "failure :( " + e.getMessage());
                                    }
                                });

        setReady(true);

        reader.close();

        resetCamera();
    }

}

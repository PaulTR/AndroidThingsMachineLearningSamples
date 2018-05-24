package com.ptrprograms.machinelearningdemo.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;

import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class CameraActivity extends Activity implements ImageReader.OnImageAvailableListener {

    private CameraHandler mCameraHandler;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private Button button;
    private Button exitButton;

    private static final int PREVIEW_IMAGE_WIDTH = 3280;
    private static final int PREVIEW_IMAGE_HEIGHT = 2464;

    private AtomicBoolean mReady = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(getLayoutResId());

        init();
    }

    protected abstract int getLayoutResId();

    private void init() {

        try {
            button = RainbowHat.openButtonA();
            button.setOnButtonEventListener(new Button.OnButtonEventListener() {
                @Override
                public void onButtonEvent(Button button, boolean pressed) {
                    Log.e("Test", "Button event");
                    startImageCapture();
                }
            });

            exitButton = RainbowHat.openButtonC();
            exitButton.setOnButtonEventListener(new Button.OnButtonEventListener() {
                @Override
                public void onButtonEvent(Button button, boolean pressed) {
                    cleanupActivity();
                    finish();
                }
            });
        } catch( IOException e ) {

        }

        mBackgroundThread = new HandlerThread("BackgroundThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mBackgroundHandler.post(mInitializeOnBackground);
    }

    protected void resetCamera() {
        mCameraHandler.shutDown();
        mBackgroundThread = new HandlerThread("BackgroundThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mBackgroundHandler.post(mInitializeOnBackground);
    }

    protected int getRotationCompensation(Activity activity)
            throws CameraAccessException {

        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        String[] camIds = null;

        try {
            camIds = manager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.w("Machine Learning", "Cannot get the list of available cameras", e);
        }
        if (camIds == null || camIds.length < 1) {
            Log.d("Machine Learning", "No cameras found");

            return 0;
        }
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        CameraManager cameraManager = (CameraManager) activity.getSystemService(CAMERA_SERVICE);
        int sensorOrientation = cameraManager
                .getCameraCharacteristics(camIds[0])
                .get(CameraCharacteristics.SENSOR_ORIENTATION);
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360;

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        int result;
        switch (rotationCompensation) {
            case 0:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                result = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                result = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                result = FirebaseVisionImageMetadata.ROTATION_270;
                break;
            default:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                Log.e("Machine Learning", "Bad rotation value: " + rotationCompensation);
        }
        return result;
    }

    protected Bitmap convertImageToBitmap(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
    }

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Runnable mInitializeOnBackground = new Runnable() {
        @Override
        public void run() {
            mCameraHandler = CameraHandler.getInstance();
            mCameraHandler.initializeCamera(CameraActivity.this,
                    PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT, mBackgroundHandler,
                    CameraActivity.this);

            setReady(true);

        }
    };

    private void startImageCapture() {
        if (mReady.get()) {
            setReady(false);
            mBackgroundHandler.post(mBackgroundClickHandler);
        } else {

        }
    }

    private Runnable mBackgroundClickHandler = new Runnable() {
        @Override
        public void run() {
            mCameraHandler.takePicture();
        }
    };

    protected void setReady(boolean ready) {
        mReady.set(ready);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupActivity();
    }

    protected void cleanupActivity() {
        try {
            if( button != null ) {
                button.close();
                button = null;
            }
        } catch( IOException e ) {

        }

        try {
            if( exitButton != null ) {
                exitButton.close();
                exitButton = null;
            }
        } catch( IOException e ) {

        }

        try {
            if (mBackgroundThread != null) mBackgroundThread.quit();
        } catch (Throwable t) {
            // close quietly
        }
        mBackgroundThread = null;
        mBackgroundHandler = null;

        try {
            if (mCameraHandler != null) mCameraHandler.shutDown();
        } catch (Throwable t) {
            // close quietly
        }
    }

    @Override
    public abstract void onImageAvailable(ImageReader reader);
}

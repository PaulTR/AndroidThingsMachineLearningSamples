package com.ptrprograms.machinelearningdemo.facedetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.google.android.gms.vision.face.Landmark;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.util.List;

public class FaceOverlayView extends View {

    private Bitmap mBitmap;
    private List<FirebaseVisionFace> mFaces;
    private Paint faceBoxPaint;
    private Paint faceLandmarkPaint;

    public FaceOverlayView(Context context) {
        this(context, null);
    }

    public FaceOverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FaceOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initPaint();
    }

    private void initPaint() {
        faceBoxPaint = new Paint();
        faceBoxPaint.setColor(Color.GREEN);
        faceBoxPaint.setStyle(Paint.Style.STROKE);
        faceBoxPaint.setStrokeWidth(5);

        faceLandmarkPaint = new Paint();
        faceLandmarkPaint.setColor( Color.GREEN );
        faceLandmarkPaint.setStyle( Paint.Style.STROKE );
        faceLandmarkPaint.setStrokeWidth( 5 );
    }

    public void setBitmap( Bitmap bitmap ) {
        mBitmap = bitmap;
    }

    public void setFaceData(List<FirebaseVisionFace> faces) {
        mFaces = faces;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if ((mBitmap != null) && (mFaces != null)) {
            double scale = drawBitmap(canvas);
            drawBitmap(canvas);
            drawFaceBox(canvas, scale);
            //drawFaceLandmarks(canvas, scale);
        }
    }

    private double drawBitmap(Canvas canvas) {
        double viewWidth = canvas.getWidth();
        double viewHeight = canvas.getHeight();
        double imageWidth = mBitmap.getWidth();
        double imageHeight = mBitmap.getHeight();
        double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);

        Rect destBounds = new Rect(0, 0, (int)(imageWidth * scale), (int)(imageHeight * scale));
        canvas.drawBitmap(mBitmap, null, destBounds, null);
        return scale;
    }

    private void drawFaceBox(Canvas canvas, double scale) {

        for( int i = 0; i < mFaces.size(); i++ ) {
            FirebaseVisionFace face = mFaces.get(i);

            float left = (float) (face.getBoundingBox().left * scale);
            float top = (float) (face.getBoundingBox().top * scale);
            float right = (float) (face.getBoundingBox().right * scale);
            float bottom = (float) (face.getBoundingBox().bottom * scale);

            canvas.drawRect( left, top, right, bottom, faceBoxPaint );
        }
    }

    private void drawFaceLandmarks( Canvas canvas, double scale ) {
        FirebaseVisionFaceLandmark landmark;
        for( int i = 0; i < mFaces.size(); i++ ) {
            FirebaseVisionFace face = mFaces.get(i);

            for( int j = 0; j <= Landmark.RIGHT_MOUTH; j++ ) {
                landmark = face.getLandmark(j);
                if( landmark != null ) {
                    int cx = (int) (landmark.getPosition().getX() * scale);
                    int cy = (int) (landmark.getPosition().getY() * scale);
                    canvas.drawCircle(cx, cy, 10, faceLandmarkPaint);
                }
            }

        }
    }

    private void logFaceData() {
        float smilingProbability;
        float leftEyeOpenProbability;
        float rightEyeOpenProbability;
        float eulerY;
        float eulerZ;
        for( int i = 0; i < mFaces.size(); i++ ) {
            FirebaseVisionFace face = mFaces.get(i);

            smilingProbability = face.getSmilingProbability();
            leftEyeOpenProbability = face.getLeftEyeOpenProbability();
            rightEyeOpenProbability = face.getRightEyeOpenProbability();
            eulerY = face.getHeadEulerAngleY();
            eulerZ = face.getHeadEulerAngleZ();

            Log.e( "Machine Learning", "Smiling: " + smilingProbability );
            Log.e( "Machine Learning", "Left eye open: " + leftEyeOpenProbability );
            Log.e( "Machine Learning", "Right eye open: " + rightEyeOpenProbability );
            Log.e( "Machine Learning", "Euler Y: " + eulerY );
            Log.e( "Machine Learning", "Euler Z: " + eulerZ );
        }
    }
}

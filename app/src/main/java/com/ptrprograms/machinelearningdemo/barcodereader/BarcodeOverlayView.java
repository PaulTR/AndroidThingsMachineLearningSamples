package com.ptrprograms.machinelearningdemo.barcodereader;

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
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.util.List;

public class BarcodeOverlayView extends View {

    private Bitmap mBitmap;
    private List<FirebaseVisionBarcode> mBarcodes;
    private Paint barcodeBoxPaint;
    private Paint textPaint;

    public BarcodeOverlayView(Context context) {
        this(context, null);
    }

    public BarcodeOverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BarcodeOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initPaint();
    }

    private void initPaint() {
        barcodeBoxPaint = new Paint();
        barcodeBoxPaint.setColor(Color.GREEN);
        barcodeBoxPaint.setStyle(Paint.Style.STROKE);
        barcodeBoxPaint.setStrokeWidth(5);

        textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(50);
    }

    public void setBitmap( Bitmap bitmap ) {
        mBitmap = bitmap;
    }

    public void setBarcodeData(List<FirebaseVisionBarcode> barcodes) {
        mBarcodes = barcodes;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if ((mBitmap != null) && (mBarcodes != null)) {
            double scale = drawBitmap(canvas);
            drawBitmap(canvas);
            drawBarcodeBoxes(canvas, scale);
            drawBarcodeData(canvas, scale);
        }
    }

    private void drawBarcodeData(Canvas canvas, double scale) {
        for( FirebaseVisionBarcode barcode : mBarcodes ) {
            Log.e("Test", barcode.getDisplayValue() );
            if( barcode.getDisplayValue() != null ) {
                canvas.drawText(barcode.getDisplayValue(),
                        (float) (barcode.getBoundingBox().left * scale),
                        (float) (barcode.getBoundingBox().bottom * scale),
                        textPaint);
            }
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

    private void drawBarcodeBoxes(Canvas canvas, double scale) {

        for( int i = 0; i < mBarcodes.size(); i++ ) {
            FirebaseVisionBarcode barcode = mBarcodes.get(i);

            float left = (float) (barcode.getBoundingBox().left * scale);
            float top = (float) (barcode.getBoundingBox().top * scale);
            float right = (float) (barcode.getBoundingBox().right * scale);
            float bottom = (float) (barcode.getBoundingBox().bottom * scale);
            canvas.drawRect( left, top, right, bottom, barcodeBoxPaint );
        }
    }
}

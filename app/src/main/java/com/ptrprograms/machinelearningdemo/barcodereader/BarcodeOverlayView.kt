package com.ptrprograms.machinelearningdemo.barcodereader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View

import com.google.android.gms.vision.face.Landmark
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark

class BarcodeOverlayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    var bitmap: Bitmap? = null
    var barcodes: List<FirebaseVisionBarcode>? = null

    lateinit private var barcodeBoxPaint: Paint
    lateinit var textPaint: Paint

    init {
        initPaint()
    }

    private fun initPaint() {
        barcodeBoxPaint = Paint()
        barcodeBoxPaint.color = Color.GREEN
        barcodeBoxPaint.style = Paint.Style.STROKE
        barcodeBoxPaint.strokeWidth = 5f

        textPaint = Paint()
        textPaint.color = Color.RED
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if( barcodes != null && bitmap != null ) {
            val scale = drawBitmap(canvas)
            drawBarcodeBoxes(canvas, scale)
            drawBarcodeData(canvas, scale)
        }

    }

    private fun drawBarcodeData(canvas: Canvas, scale: Double) {
        if( barcodes == null ) {
            return
        }

        for (barcode in barcodes!!) {
            Log.e("Test", barcode.displayValue)
            if (barcode.displayValue != null ) {
                canvas.drawText(barcode.displayValue!!,
                        (barcode.boundingBox!!.left * scale).toFloat(),
                        (barcode.boundingBox!!.bottom * scale).toFloat(),
                        textPaint)
            }
        }
    }

    private fun drawBitmap(canvas: Canvas): Double {
        val viewWidth = canvas.width.toDouble()
        val viewHeight = canvas.height.toDouble()
        val imageWidth = bitmap!!.width.toDouble()
        val imageHeight = bitmap!!.height.toDouble()
        val scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight)

        val destBounds = Rect(0, 0, (imageWidth * scale).toInt(), (imageHeight * scale).toInt())
        canvas.drawBitmap(bitmap, null, destBounds, null)
        return scale
    }

    private fun drawBarcodeBoxes(canvas: Canvas, scale: Double) {

        if( barcodes == null ) {
            return;
        }
        for (i in barcodes!!.indices) {
            val barcode = barcodes!![i]

            val left = (barcode.boundingBox!!.left * scale).toFloat()
            val top = (barcode.boundingBox!!.top * scale).toFloat()
            val right = (barcode.boundingBox!!.right * scale).toFloat()
            val bottom = (barcode.boundingBox!!.bottom * scale).toFloat()
            canvas.drawRect(left, top, right, bottom, barcodeBoxPaint!!)
        }
    }
}

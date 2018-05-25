package com.ptrprograms.machinelearningdemo.landmarks

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
import com.google.firebase.ml.vision.cloud.landmark.FirebaseVisionCloudLandmark
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark

class LandmarkOverlayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    var bitmap: Bitmap? = null
    var landmarks: List<FirebaseVisionCloudLandmark>? = null
    lateinit private var landmarkBoxPaint: Paint
    lateinit var textPaint: Paint

    init {
        initPaint()
    }

    private fun initPaint() {
        landmarkBoxPaint = Paint()
        landmarkBoxPaint.color = Color.GREEN
        landmarkBoxPaint.style = Paint.Style.STROKE
        landmarkBoxPaint.strokeWidth = 5f

        textPaint = Paint()
        textPaint.color = Color.RED
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (bitmap != null && landmarks != null) {
            val scale = drawBitmap(canvas)
            drawLandmarkBoxes(canvas, scale)
            drawLandmarkName(canvas, scale)
        }
    }

    private fun drawLandmarkName(canvas: Canvas, scale: Double) {
        if( landmarks == null ) {
            return
        }

        for( landmark in landmarks!! ) {
            if( landmark.landmark != null ) {
                canvas.drawText(landmark.landmark!!,
                        (landmark.boundingBox!!.left * scale).toFloat(),
                        (landmark.boundingBox!!.bottom * scale).toFloat(),
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

    private fun drawLandmarkBoxes(canvas: Canvas, scale: Double) {

        for (i in landmarks!!.indices) {
            val face = landmarks!![i]

            val left = (face.boundingBox!!.left * scale).toFloat()
            val top = (face.boundingBox!!.top * scale).toFloat()
            val right = (face.boundingBox!!.right * scale).toFloat()
            val bottom = (face.boundingBox!!.bottom * scale).toFloat()

            canvas.drawRect(left, top, right, bottom, landmarkBoxPaint!!)
        }
    }
}

package com.ptrprograms.machinelearningdemo.facedetection

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
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark

class FaceOverlayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    var bitmap: Bitmap? = null
    var faces: List<FirebaseVisionFace>? = null
    lateinit private var faceBoxPaint: Paint
    lateinit private var faceLandmarkPaint: Paint

    init {
        initPaint()
    }

    private fun initPaint() {
        faceBoxPaint = Paint()
        faceBoxPaint.color = Color.GREEN
        faceBoxPaint.style = Paint.Style.STROKE
        faceBoxPaint.strokeWidth = 5f

        faceLandmarkPaint = Paint()
        faceLandmarkPaint.color = Color.GREEN
        faceLandmarkPaint.style = Paint.Style.STROKE
        faceLandmarkPaint.strokeWidth = 5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (bitmap != null && faces != null) {
            val scale = drawBitmap(canvas)
            drawBitmap(canvas)
            drawFaceBox(canvas, scale)
            //drawFaceLandmarks(canvas, scale);
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

    private fun drawFaceBox(canvas: Canvas, scale: Double) {

        for (i in faces!!.indices) {
            val face = faces!![i]

            val left = (face.boundingBox.left * scale).toFloat()
            val top = (face.boundingBox.top * scale).toFloat()
            val right = (face.boundingBox.right * scale).toFloat()
            val bottom = (face.boundingBox.bottom * scale).toFloat()

            canvas.drawRect(left, top, right, bottom, faceBoxPaint!!)
        }
    }

    private fun drawFaceLandmarks(canvas: Canvas, scale: Double) {
        var landmark: FirebaseVisionFaceLandmark?
        for (i in faces!!.indices) {
            val face = faces!![i]

            for (j in 0..Landmark.RIGHT_MOUTH) {
                landmark = face.getLandmark(j)
                if (landmark != null) {
                    val cx = (landmark.position.x!! * scale).toInt()
                    val cy = (landmark.position.y!! * scale).toInt()
                    canvas.drawCircle(cx.toFloat(), cy.toFloat(), 10f, faceLandmarkPaint!!)
                }
            }

        }
    }
}

package com.ptrprograms.machinelearningdemo.tensorflow

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.Image
import android.os.Environment
import android.util.Log

import junit.framework.Assert

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

class ImagePreprocessor(previewWidth: Int, previewHeight: Int,
                        croppedwidth: Int, croppedHeight: Int) {

    private var rgbFrameBitmap: Bitmap? = null
    private val croppedBitmap: Bitmap?

    init {
        this.croppedBitmap = Bitmap.createBitmap(croppedwidth, croppedHeight, Config.ARGB_8888)
        this.rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888)
    }

    fun preprocessImage(image: Image?): Bitmap? {
        if (image == null) {
            return null
        }

        Assert.assertEquals("Invalid size width", rgbFrameBitmap!!.width, image.width)
        Assert.assertEquals("Invalid size height", rgbFrameBitmap!!.height, image.height)

        if (croppedBitmap != null && rgbFrameBitmap != null) {
            val bb = image.planes[0].buffer
            rgbFrameBitmap = BitmapFactory.decodeStream(ByteBufferBackedInputStream(bb))
            cropAndRescaleBitmap(rgbFrameBitmap!!, croppedBitmap, 0)
        }

        image.close()

        // For debugging
        if (SAVE_PREVIEW_BITMAP) {
            saveBitmap(croppedBitmap!!)
        }
        return croppedBitmap
    }

    private class ByteBufferBackedInputStream(internal var buf: ByteBuffer) : InputStream() {

        @Throws(IOException::class)
        override fun read(): Int {
            return if (!buf.hasRemaining()) {
                -1
            } else (buf.get().toInt() and 0xFF)
        }

        @Throws(IOException::class)
        override fun read(bytes: ByteArray, off: Int, len: Int): Int {
            var len = len
            if (!buf.hasRemaining()) {
                return -1
            }

            len = Math.min(len, buf.remaining())
            buf.get(bytes, off, len)
            return len
        }
    }

    companion object {
        private val SAVE_PREVIEW_BITMAP = false

        /**
         * Saves a Bitmap object to disk for analysis.
         *
         * @param bitmap The bitmap to save.
         */
        internal fun saveBitmap(bitmap: Bitmap) {
            val file = File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "tensorflow_preview.png")
            Log.d("ImageHelper", String.format("Saving %dx%d bitmap to %s.",
                    bitmap.width, bitmap.height, file.absolutePath))

            file.delete()

            try {
                FileOutputStream(file).use { fs -> BufferedOutputStream(fs).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 99, out) } }
            } catch (e: Exception) {
                Log.w("ImageHelper", "Could not save image for debugging", e)
            }

        }

        internal fun cropAndRescaleBitmap(src: Bitmap, dst: Bitmap,
                                          sensorOrientation: Int) {
            Assert.assertEquals(dst.width, dst.height)
            val minDim = Math.min(src.width, src.height).toFloat()

            val matrix = Matrix()

            // We only want the center square out of the original rectangle.
            val translateX = -Math.max(0f, (src.width - minDim) / 2)
            val translateY = -Math.max(0f, (src.height - minDim) / 2)
            matrix.preTranslate(translateX, translateY)

            val scaleFactor = dst.height / minDim
            matrix.postScale(scaleFactor, scaleFactor)

            // Rotate around the center if necessary.
            if (sensorOrientation != 0) {
                matrix.postTranslate(-dst.width / 2.0f, -dst.height / 2.0f)
                matrix.postRotate(sensorOrientation.toFloat())
                matrix.postTranslate(dst.width / 2.0f, dst.height / 2.0f)
            }

            val canvas = Canvas(dst)
            canvas.drawBitmap(src, matrix, null)
        }
    }
}
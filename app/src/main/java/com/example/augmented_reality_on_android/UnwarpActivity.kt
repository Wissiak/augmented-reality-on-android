package com.example.augmented_reality_on_android

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.Utils.bitmapToMat
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.getPerspectiveTransform
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt


class UnwarpActivity : AppCompatActivity() {
    private val imageView by lazy { findViewById<ImageView>(R.id.imageView) }
    private val infoText by lazy { findViewById<TextView>(R.id.infoText) }
    private val saveBtn by lazy { findViewById<Button>(R.id.save) }
    private val resetBtn by lazy { findViewById<Button>(R.id.reset) }
    private lateinit var warpedImg: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.unwarp_activity)

        init()
    }

    override fun onResume() {
        super.onResume()

        init()
    }

    fun init() {
        val imgUri = Uri.parse(intent.getStringExtra("image_uri"))

        var bm = uriToBitmap(imgUri)

        // Rotate bitmap correctly because there is an issue on Samsung devices
        val inputStream = contentResolver.openInputStream(imgUri)
        val exif = ExifInterface(inputStream!!)
        val orientation: Int =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        bm = rotateBitmap(bm!!, orientation)

        var width: Int
        var height: Int
        val ratio = bm!!.width.toDouble() / bm.height
        if (bm.width > bm.height) {
            // Is landscape image
            width = applicationContext.resources.displayMetrics.widthPixels
            height = (width / ratio).toInt()
        } else {
            // Is portrait image
            height = 960
            width = (height * ratio).toInt()
        }

        bm = Bitmap.createScaledBitmap(bm, width, height, false);

        imageView.setImageBitmap(bm)

        val pts = Array(4) { DoubleArray(2) }
        val posXY = IntArray(2)
        imageView.getLocationOnScreen(posXY)

        var numPoints = 0
        fun onPointClick(event: MotionEvent) {
            pts[numPoints] =
                doubleArrayOf((event.x - posXY[0]).toDouble(), (event.y - posXY[1]).toDouble())
            numPoints++

            if (numPoints == 4) {
                val mat = Mat()
                bitmapToMat(bm, mat)
                warpedImg = doWarp(mat, pts)
                imageView.setImageBitmap(warpedImg)
                infoText.text = "Do you want to save the unwarped image?"
                saveBtn.visibility = VISIBLE
                resetBtn.visibility = VISIBLE
            } else {
                infoText.text = "You have selected ${numPoints}/4 points"
            }
        }

        imageView.setOnTouchListener { v, event ->
            onPointClick(event)
            false
        }

        resetBtn.setOnClickListener {
            numPoints = 0
            imageView.setImageBitmap(bm)
            infoText.text = ""
            saveBtn.visibility = INVISIBLE
            resetBtn.visibility = INVISIBLE
        }

        saveBtn.setOnClickListener {
            val file = File(filesDir, UUID.randomUUID().toString() + ".png")
            val stream = FileOutputStream(file)
            warpedImg.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
            Toast.makeText(this, "Image has been added", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap? {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> return bitmap
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return try {
            val bmRotated =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            bmRotated
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            null
        }
    }

    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun fourPointTransform(image: Mat, rect: Array<DoubleArray>): Mat {
        assert(rect.size == 4)
        val (tl, tr, br, bl) = rect

        val widthA = sqrt((br[0] - bl[0]).pow(2) + (br[1] - bl[1]).pow(2))
        val widthB = sqrt((tr[0] - tl[0]).pow(2) + (tr[1] - tl[1]).pow(2))
        val maxWidth = max(widthA.toInt(), widthB.toInt())

        val heightA = sqrt((tr[0] - br[0]).pow(2) + (tr[1] - br[1]).pow(2))
        val heightB = sqrt((tl[0] - bl[0]).pow(2) + (tl[1] - bl[1]).pow(2))
        val maxHeight = max(heightA.toInt(), heightB.toInt())

        val dst = arrayOf(
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(maxWidth - 1.0, 0.0),
            doubleArrayOf(maxWidth - 1.0, maxHeight - 1.0),
            doubleArrayOf(0.0, maxHeight - 1.0)
        )

        val M = getPerspectiveTransform(doubleArrayToMat(rect), doubleArrayToMat(dst))
        val warped = Mat()
        Imgproc.warpPerspective(image, warped, M, Size(maxWidth.toDouble(), maxHeight.toDouble()))
        return warped
    }

    fun doubleArrayToMat(input: Array<DoubleArray>): Mat {
        val rows = input.size
        val cols = input[0].size
        val outputMat = Mat(rows, cols, CvType.CV_32F)

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                outputMat.put(i, j, input[i][j])
            }
        }

        return outputMat
    }

    fun doWarp(image: Mat, pts: Array<DoubleArray>): Bitmap {
        val warped = fourPointTransform(image, pts)

        val bitmap = Bitmap.createBitmap(warped.cols(), warped.rows(), Bitmap.Config.RGB_565)
        matToBitmap(warped, bitmap)

        return bitmap
    }
}
package com.example.augmented_reality_on_android

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
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

        val imgUri = Uri.parse(intent.getStringExtra("image_uri"))

        val bm = uriToBitmap(imgUri)

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
        }

        saveBtn.setOnClickListener {
            val file = File(filesDir, UUID.randomUUID().toString() + ".png")
            val stream = FileOutputStream(file)
            warpedImg.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
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

    fun orderPoints(pts: Array<DoubleArray>): Array<DoubleArray> {
        var topLeft = pts[0]
        var topRight = pts[0]
        var bottomRight = pts[0]
        var bottomLeft = pts[0]

        for (point in pts) {
            val sum = point[0] + point[1]
            val diff = point[0] - point[1]

            // Update top-left point
            if (sum < (topLeft[0] + topLeft[1])) {
                topLeft = point
            }

            // Update bottom-right point
            if (sum > (bottomRight[0] + bottomRight[1])) {
                bottomRight = point
            }

            // Update top-right point
            if (diff > (topRight[0] - topRight[1])) {
                topRight = point
            }

            // Update bottom-left point
            if (diff < (bottomLeft[0] - bottomLeft[1])) {
                bottomLeft = point
            }
        }
        return arrayOf(topLeft, topRight, bottomRight, bottomLeft)
    }

    fun fourPointTransform(image: Mat, pts: Array<DoubleArray>): Mat {
        assert(pts.size == 4)
        val rect = orderPoints(pts)
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
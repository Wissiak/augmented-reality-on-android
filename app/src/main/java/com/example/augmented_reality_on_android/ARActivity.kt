package com.example.augmented_reality_on_android

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.material.slider.RangeSlider
import org.opencv.android.CameraActivity
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.JavaCameraView
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.util.*


class ARActivity : CameraActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private val cameraView by lazy { findViewById<JavaCameraView>(R.id.cameraView) }
    private val sizeX by lazy { findViewById<RangeSlider>(R.id.size_x) }
    private val sizeY by lazy { findViewById<RangeSlider>(R.id.size_y) }
    private val sizeZ by lazy { findViewById<RangeSlider>(R.id.size_z) }
    private val imageView by lazy { findViewById<ImageView>(R.id.imageView) }
    private lateinit var imageMat: Mat
    private lateinit var arCore: ARCore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ar_activity)

        getPermission()

        cameraView.setCvCameraViewListener(this)
        cameraView.visibility = SurfaceView.VISIBLE

        val reference_image: Mat =
            org.opencv.android.Utils.loadResource(
                this,
                R.drawable.keyboard_reference,
                CvType.CV_8UC4
            )
        arCore = ARCore(reference_image)

        sizeX.addOnChangeListener { slider, value, fromUser ->
            arCore.changeX(value.toDouble())
        }
        sizeY.addOnChangeListener { slider, value, fromUser ->
            arCore.changeY(value.toDouble())
        }
        sizeZ.addOnChangeListener { slider, value, fromUser ->
            arCore.changeZ(value.toDouble())
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        imageMat = Mat(width, height, CvType.CV_8UC4)
        val ratio = resources.displayMetrics.widthPixels / height
        imageView.layoutParams = ViewGroup.LayoutParams(height * ratio, width * ratio)
    }

    override fun onCameraViewStopped() {
        imageMat.release()
    }
    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        imageMat = inputFrame!!.rgba()
        Core.flip(imageMat, imageMat, 1);
        imageMat = arCore.android_ar(imageMat)
        val imageMat2 = imageMat.t()
        Core.flip(imageMat2, imageMat2, -1);
        val bitmap =
            Bitmap.createBitmap(imageMat2.cols(), imageMat2.rows(), Bitmap.Config.RGB_565)
        matToBitmap(imageMat2, bitmap)
        runOnUiThread {
            imageView.setImageBitmap(bitmap)
        }
        return imageMat
    }

    override fun getCameraViewList(): MutableList<out CameraBridgeViewBase> {
        return Collections.singletonList(cameraView)
    }

    override fun onResume() {
        super.onResume()
        cameraView.enableView()
    }

    override fun onPause() {
        super.onPause()
        cameraView.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraView.disableView()
    }

    private fun getPermission() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 102)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 102 && grantResults.isNotEmpty()) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                getPermission()
            }
        }
    }
}
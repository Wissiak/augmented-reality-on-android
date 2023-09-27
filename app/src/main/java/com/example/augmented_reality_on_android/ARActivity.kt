package com.example.augmented_reality_on_android

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.api.rand
import org.opencv.android.*
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.io.IOException
import java.util.*


class ARActivity : CameraActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private val cameraView by lazy { findViewById<JavaCameraView>(R.id.cameraView) }
    private lateinit var imageMat: Mat
    private lateinit var arCore: ARCore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ar_activity)

        getPermission()

        cameraView.setCvCameraViewListener(this)
        val H_c_b = mk.ndarray(
            arrayOf(
                doubleArrayOf(1.0, 2.0, 3.0),
                doubleArrayOf(4.0, 5.0, 6.0),
                doubleArrayOf(7.0, 8.0, 9.0)
            )
        )

        var reference_image: Mat =
            org.opencv.android.Utils.loadResource(this, R.drawable.book1_reference, CvType.CV_8UC4)
        arCore = ARCore(reference_image)
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        imageMat = Mat(width, height, CvType.CV_8UC4)
        val ratio = resources.displayMetrics.widthPixels / height
        cameraView.layoutParams = ViewGroup.LayoutParams(height * ratio, width * ratio)
    }

    override fun onCameraViewStopped() {
        imageMat.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        imageMat = inputFrame!!.rgba()
        arCore.android_ar(imageMat)
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
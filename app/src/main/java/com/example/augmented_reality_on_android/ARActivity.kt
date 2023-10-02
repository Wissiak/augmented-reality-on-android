package com.example.augmented_reality_on_android

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.RangeSlider
import com.google.android.material.switchmaterial.SwitchMaterial
import org.opencv.android.CameraActivity
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.JavaCameraView
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.*


class ARActivity : CameraActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private val cameraView by lazy { findViewById<JavaCameraView>(R.id.cameraView) }
    private val sizeX by lazy { findViewById<RangeSlider>(R.id.size_x) }
    private val sizeY by lazy { findViewById<RangeSlider>(R.id.size_y) }
    private val sizeZ by lazy { findViewById<RangeSlider>(R.id.size_z) }
    private val imageView by lazy { findViewById<ImageView>(R.id.imageView) }
    private lateinit var imageMat: Mat
    private lateinit var arCore: ARCore
    private lateinit var reference_image: Mat
    private var isPause: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ar_activity)

        getPermission()

        cameraView.setCvCameraViewListener(this)
        cameraView.visibility = SurfaceView.VISIBLE

        val reference_image: Mat =
            org.opencv.android.Utils.loadResource(
                this,
                R.drawable.book1_reference,
                CvType.CV_8UC4
            )
        Core.flip(reference_image, reference_image, 1) // Must apply the same flip as for the camera feed
        arCore = ARCore(reference_image)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            if (isPause) {
                isPause = false
                cameraView.enableView()
                fab.setImageResource(R.drawable.ic_pause)
            } else {
                isPause = true
                cameraView.disableView()
                fab.setImageResource(R.drawable.ic_play)
            }
        }

        sizeX.addOnChangeListener { slider, value, fromUser ->
            arCore.changeX(min(reference_image.size(0), reference_image.size(1)) / 100.0 * value)
        }
        sizeY.addOnChangeListener { slider, value, fromUser ->
            arCore.changeY(max(reference_image.size(0), reference_image.size(1)) / 100.0 * value)
        }
        sizeZ.addOnChangeListener { slider, value, fromUser ->
            arCore.changeZ(value.toDouble()*5)
        }
        val colorToggle: SwitchMaterial = findViewById(R.id.toggle_color)
        colorToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                arCore.setEdgeColors(Scalar(255.0, 255.0, 255.0))
            } else {
                arCore.setEdgeColors(Scalar(0.0, 0.0, 0.0))
            }
        }
        val frameToggle: SwitchMaterial = findViewById(R.id.toggle_frame)
        frameToggle.setOnCheckedChangeListener { _, isChecked ->
            arCore.toggleFrame(isChecked)
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
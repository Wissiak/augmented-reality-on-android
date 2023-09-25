package com.example.augmented_reality_on_android

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.FileDescriptor
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var selectBtn: Button
    private lateinit var cameraBtn: Button
    private lateinit var changeViewBtn: Button
    private lateinit var imageView: AppCompatImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (OpenCVLoader.initDebug()) {
            Log.i("LOADED", "Successfully loaded OpenCV")
        } else {
            Log.e("LOADED", "Could not load OpenCV")
        }

        getPermission()

        selectBtn = findViewById(R.id.select)
        cameraBtn = findViewById<Button>(R.id.camera)
        imageView = findViewById(R.id.ImageView)
        changeViewBtn = findViewById(R.id.changeView)

        selectBtn.setOnClickListener {
            val pickImg = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            changeImage.launch(pickImg)
        }

        cameraBtn.setOnClickListener {
            val cam = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            capture.launch(cam)
        }
        changeViewBtn.setOnClickListener {
            val intent = Intent(this, ARActivity::class.java)
            startActivity(intent)
        }

    }

    private val capture =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                val data = it.data
                val bm = data?.extras?.get("data")
                if (bm is Bitmap) {
                    imageView.setImageBitmap(bm)
                }

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

    private val changeImage =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                val data = it.data
                val imgUri:Uri? = data?.data
                if (imgUri is Uri) {
                    val bm = uriToBitmap(imgUri)

                    val mat = Mat()
                    Utils.bitmapToMat(bm, mat)

                    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)
                    Utils.matToBitmap(mat, bm)

                    imageView.setImageBitmap(bm)
                }

            }
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
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                getPermission()
            }
        }
    }
}
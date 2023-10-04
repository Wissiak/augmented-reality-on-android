package com.example.augmented_reality_on_android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.FileDescriptor
import java.io.IOException

class UnwarpActivity: AppCompatActivity() {
    private val imageView by lazy { findViewById<ImageView>(R.id.imageView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.unwarp_activity)

        val imgUri = Uri.parse(intent.getStringExtra("image_uri"))

        val bm = uriToBitmap(imgUri)

        //val mat = Mat()
        //Utils.bitmapToMat(bm, mat)

        imageView.setImageBitmap(bm)
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
}
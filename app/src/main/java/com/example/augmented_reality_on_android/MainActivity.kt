package com.example.augmented_reality_on_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexboxLayout
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {
    private lateinit var changeViewBtn: Button
    private val referenceImages by lazy { findViewById<FlexboxLayout>(R.id.reference_images) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (OpenCVLoader.initDebug()) {
            Log.i("LOADED", "Successfully loaded OpenCV")
        } else {
            Log.e("LOADED", "Could not load OpenCV")
        }

        changeViewBtn = findViewById(R.id.changeView)

        changeViewBtn.setOnClickListener {
            val intent = Intent(this, ARActivity::class.java)
            startActivity(intent)
        }
        fun addRefImg(id: Int) {
            val view: View = LayoutInflater.from(this).inflate(R.layout.ref_img_layout, null)
            val button: ImageButton = view.findViewById(R.id.image_button) as ImageButton
            button.setImageResource(id)
            button.setOnClickListener {
                // TODO set ref img in permanent storage
            }
            referenceImages.addView(view)
        }
        addRefImg(R.drawable.book1_reference)
        addRefImg(R.drawable.keyboard_reference)
    }
}
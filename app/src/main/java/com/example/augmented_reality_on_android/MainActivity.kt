package com.example.augmented_reality_on_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexboxLayout
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {
    private lateinit var changeViewBtn: Button
    private val referenceImages by lazy { findViewById<FlexboxLayout>(R.id.reference_images) }
    private var selectedRefImg: Int = R.drawable.book1_reference

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
            intent.putExtra("reference_image", selectedRefImg)
            startActivity(intent)
        }


        val scale = applicationContext.resources.displayMetrics.density;
        fun addRefImg(id: Int, isAddButton: Boolean = false) {
            val view: View = LayoutInflater.from(this).inflate(R.layout.ref_img_layout, null)
            val button: ImageButton = view.findViewById(R.id.image_button) as ImageButton
            val checkButton: ImageButton = view.findViewById(R.id.ref_img_checked) as ImageButton
            button.setImageResource(id)
            if (!isAddButton) {
                button.setOnClickListener {
                    selectedRefImg = id
                    // Refresh views to show checkButton correctly
                    referenceImages.removeAllViews()
                    addRefImg(R.drawable.book1_reference)
                    addRefImg(R.drawable.keyboard_reference)
                    addRefImg(R.drawable.ic_add, true)
                }
            } else {
                val size = (120 * scale + 0.5f).toInt()
                button.layoutParams = LinearLayout.LayoutParams(size, size)
            }
            if (selectedRefImg == id) {
                checkButton.visibility = View.VISIBLE
            }
            referenceImages.addView(view)
        }
        addRefImg(R.drawable.book1_reference)
        addRefImg(R.drawable.keyboard_reference)
        addRefImg(R.drawable.ic_add, true)
    }
}
package com.example.augmented_reality_on_android

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexboxLayout
import org.opencv.android.OpenCVLoader
import java.io.File


class MainActivity : AppCompatActivity() {
    private val referenceImages by lazy { findViewById<FlexboxLayout>(R.id.reference_images) }
    private val changeViewBtn by lazy { findViewById<Button>(R.id.changeView) }
    private var selectedRefImg: Any = R.drawable.book1_reference
    private var dpScale: Float = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (OpenCVLoader.initDebug()) {
            Log.i("LOADED", "Successfully loaded OpenCV")
        } else {
            Log.e("LOADED", "Could not load OpenCV")
        }

        dpScale = applicationContext.resources.displayMetrics.density

        changeViewBtn.setOnClickListener {
            val intent = Intent(this, ARActivity::class.java)
            if (selectedRefImg is Int) {
                intent.putExtra("reference_image_int", selectedRefImg as Int)
            } else if (selectedRefImg is String) {
                intent.putExtra("reference_image_str", selectedRefImg as String)
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        refreshButtons()
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu.setHeaderTitle("Actions")
        menu.add(0, v.id, 0, "Delete").setOnMenuItemClickListener {
            val file = File(filesDir, v.tag as String)
            val deleted = file.delete()
            if (deleted) {
                Toast.makeText(this, "Image has been deleted", Toast.LENGTH_SHORT).show()
                refreshButtons()
            }
            true
        }
    }

    fun refreshButtons() {
        referenceImages.removeAllViews()
        addRefImg(R.drawable.book1_reference)
        addRefImg(R.drawable.keyboard_reference)
        loadCustomRefImages()
        addRefImg(R.drawable.ic_add, true)
    }

    fun addRefImg(id: Any, isAddButton: Boolean = false) {
        val view: View = LayoutInflater.from(this).inflate(R.layout.ref_img_layout, null)
        val button: ImageButton = view.findViewById(R.id.image_button) as ImageButton
        val checkButton: ImageButton = view.findViewById(R.id.ref_img_checked) as ImageButton

        if (id is Int) {
            button.setImageResource(id)
        } else if (id is String) {
            // Enable delete of image
            button.tag = id
            registerForContextMenu(button)

            val file = File(filesDir, id)
            if (file.exists()) {
                button.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
            }
        } else {
            return
        }
        if (!isAddButton) {
            button.setOnClickListener {
                selectedRefImg = id
                // Refresh views to show checkButton correctly
                refreshButtons()
            }
        } else {
            val size = (120 * dpScale + 0.5f).toInt()
            button.layoutParams = LinearLayout.LayoutParams(size, size)
            button.setOnClickListener {
                val pickImg =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                changeImage.launch(pickImg)
            }
        }
        if (selectedRefImg == id) {
            checkButton.visibility = View.VISIBLE
        }
        referenceImages.addView(view)
    }

    fun loadCustomRefImages() {
        for (file in filesDir.list()!!) {
            if (file.endsWith(".png")) {
                addRefImg(file)
            }
        }
    }

    private val changeImage =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                val data = it.data
                val imgUri: Uri? = data?.data

                val intent = Intent(this, UnwarpActivity::class.java)
                intent.putExtra("image_uri", imgUri.toString())
                startActivity(intent)

            }
        }
}
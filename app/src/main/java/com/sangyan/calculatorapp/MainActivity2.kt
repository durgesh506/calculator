package com.sangyan.calculatorapp


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.content.Intent

import android.widget.ImageView
import com.sangyan.calculatorapp.documents.DocumentActivity
import com.sangyan.calculatorapp.photos.PhotosActivity
import com.sangyan.calculatorapp.videos.VideosActivity


class MainActivity2 : AppCompatActivity() {

    lateinit var imageView: ImageView
    lateinit var imageView2: ImageView
    lateinit var imageView3:ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        imageView = findViewById(R.id.image_view1)
        imageView2 = findViewById(R.id.image_view20)
        imageView3=findViewById(R.id.image_view22)


        imageView.setOnClickListener {
            val intent = Intent(this, PhotosActivity::class.java)
            // start your next activity
            startActivity(intent)
        }
        imageView2.setOnClickListener {
            val intent = Intent(this, VideosActivity::class.java)
            // start your next activity
            startActivity(intent)
        }
        imageView3.setOnClickListener {
            val intent =Intent(this,DocumentActivity::class.java)
            startActivity(intent)
        }




    }

}
package com.dicoding.asclepius.view

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.dicoding.asclepius.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val result = intent.getStringExtra(EXTRA_RESULT)
        val image = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (image != null) {
            Log.d("nilai image", image)
        }
        val imageUri = Uri.parse(image)

        binding.resultText.setText(result)
        binding.resultImage.setImageURI(imageUri)
    }
    companion object {
        const val EXTRA_RESULT = "extra_result"
        const val EXTRA_IMAGE_URI = "extra_image"
    }
}
package com.dicoding.asclepius.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract

import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri

import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import com.yalantis.ucrop.UCrop

import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.File
import java.text.NumberFormat
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var imageClassifierHelper: ImageClassifierHelper

    private var currentImageUri: Uri? = null
    private val uCropImageContract = object : ActivityResultContract<List<Uri>, Uri>() {
        override fun createIntent(context: Context, input: List<Uri>): Intent {
            val inputUri = input.elementAt(0)
            val outputUri = input.elementAt(1)

            val ucrop = UCrop.of(inputUri, outputUri)
                .withMaxResultSize(1000, 1000)
            return ucrop.getIntent(context)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri {
            return if (intent != null) {
                val croppedUri = UCrop.getOutput(intent)
                croppedUri ?: run {
                    val resultIntent = Intent()
                    resultIntent.putExtra(EXTRA_CLOSE_UCROP, true)
                    setResult(Activity.RESULT_CANCELED, resultIntent)
                    finishActivity(UCrop.REQUEST_CROP)
                    return@run Uri.EMPTY
                }
            } else {
                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_CLOSE_UCROP, true)
                setResult(Activity.RESULT_CANCELED, resultIntent)
                finishActivity(UCrop.REQUEST_CROP)
                Uri.EMPTY
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.galleryButton.setOnClickListener {
            startGallery()
        }

        binding.analyzeButton.setOnClickListener {
            analyzeImage()
        }
    }

    private val launcherGallery =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                val uniqueImageId = getImageUniqueId()
                val sourceImageUri = uri
                val destinationImageUri = File(filesDir, "image_$uniqueImageId.jpg").toUri()

                val listUri = listOf(sourceImageUri, destinationImageUri)
                cropImage.launch(listUri)
            } else {
                Log.d("Photo Picker", "No media selected")
                Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show()
            }
        }

    private val cropImage = registerForActivityResult(uCropImageContract) { uri ->
        if (uri != null && uri != Uri.EMPTY) {
            currentImageUri = uri
            showImage()
        } else {
            Log.d("Cropping Image", "Cropping image failed.")
        }
    }

    private fun getImageUniqueId(): String {
        val uniqueId = UUID.randomUUID().toString()

        return uniqueId
    }

    private fun startGallery() {
        // TODO: Mendapatkan gambar dari Gallery.
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun showImage() {
        currentImageUri?.let {
            binding.previewImageView.setImageURI(it)
        }
    }

    private fun analyzeImage() {
        // TODO: Menganalisa gambar yang berhasil ditampilkan.
        binding.progressIndicator.visibility = View.VISIBLE
        currentImageUri?.let { uri ->
            imageClassifierHelper = ImageClassifierHelper(
                context = this,
                classifierListener = object : ImageClassifierHelper.ClassifierListener {
                    override fun onError(error: String) {
                        showToast("Classification not found.")
                    }

                    override fun onResults(results: List<Classifications>?) {
                        // TODO("Not yet implemented")
                        results?.let { it ->
                            if (it.isNotEmpty() && it[0].categories.isNotEmpty()) {

                                val category = it[0].categories.sortedByDescending {
                                    it?.score
                                }

                                val largestScore = category.elementAt(0)
                                val displayResult =
                                    "${largestScore.label} " + NumberFormat.getPercentInstance()
                                        .format(largestScore.score).trim()
                                moveToResult(displayResult)

                            } else {
                                showToast("Classification failed.")
                            }
                        }
                    }
                }
            )
            imageClassifierHelper.classifyStaticImage(uri)
        }
    }

    private fun moveToResult(displayResult: String) {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra(ResultActivity.EXTRA_RESULT, displayResult)
        intent.putExtra(ResultActivity.EXTRA_IMAGE_URI, currentImageUri.toString())
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_CLOSE_UCROP = "extra_close_activity"
    }
}
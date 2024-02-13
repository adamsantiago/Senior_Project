package com.example.textrecognizer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.textrecognizer.databinding.ActivityMainBinding
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.util.Linkify
import android.view.Menu
import android.view.MenuItem
import androidx.core.graphics.drawable.toBitmap
import com.github.dhaval2404.imagepicker.ImagePicker
import kotlinx.android.synthetic.main.activity_main.nextPage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var recognizer: TextRecognizer
    private val TAG = "Testing"
    private val SAVED_TEXT_TAG = "SavedText"

    //private val SAVED_IMAGE_BITMAP = "SavedImage"
    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    lateinit var camera: Camera
    var savedBitmap: Bitmap? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        if (savedInstanceState != null) {
            val savedText = savedInstanceState.getString(SAVED_TEXT_TAG)
            binding.apply {
                if (isTextValid(savedText)) {
                    textInImageLayout.visibility = View.VISIBLE
                    textInImage.text = savedInstanceState.getString(SAVED_TEXT_TAG)
                }
                if (savedBitmap != null) {
                    previewImage.visibility = View.VISIBLE
                    previewImage.setImageBitmap(savedBitmap)
                }
                //previewImage.setImageBitmap(savedInstanceState.getParcelable(SAVED_IMAGE_BITMAP))
            }
        }
        init()
        setContentView(binding.root)
    }

    private fun init() {

        cameraExecutor = Executors.newSingleThreadExecutor()

        recognizer = TextRecognition.getClient()

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        binding.apply {
            extractTextButton.setOnClickListener {
                when {
                    previewImage.visibility == View.VISIBLE -> {
                        savedBitmap = previewImage.drawable.toBitmap()
                        runTextRecognition(savedBitmap!!)
                    }

                    viewFinder.bitmap != null -> {
                        previewImage.visibility = View.VISIBLE
                        savedBitmap = viewFinder.bitmap
                        previewImage.setImageBitmap(viewFinder.bitmap!!)
                        runTextRecognition(savedBitmap!!)
                    }

                    else -> {
                        showToast(getString(R.string.camera_error_default_msg))
                    }
                }
            }


            share.setOnClickListener {
                val textToCopy = textInImage.text.toString()
                if (isTextValid(textToCopy)) {
                    shareText(textToCopy)
                } else {
                    showToast(getString(R.string.no_text_found))
                }
            }

            close.setOnClickListener {
                textInImageLayout.visibility = View.GONE
            }
            nextPage.setOnClickListener {
                moveToNextActivity()
            }
        }
    }

    private fun moveToNextActivity() {
        val intent = Intent(this, ManualInput::class.java)
        //New code for the transfer to manual input below by Santiago

        val textInImage = (binding.textInImage.text).toString()
        if (isTextValid(textInImage)) {
            intent.putExtra("savedText", textInImage)
        }
        startActivity(intent)
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                showToast(
                    getString(R.string.permission_denied_msg)
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.gallery) {
            binding.textInImageLayout.visibility = View.GONE
            ImagePicker.with(this)
                .galleryOnly()
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start()

            return true
        } else if (item.itemId == R.id.camera) {
            if (!allPermissionsGranted()) {
                requestPermissions()
            } else {
                binding.previewImage.visibility = View.GONE
                savedBitmap = null
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> {
                //Image Uri will not be null for RESULT_OK
                val uri: Uri = data?.data!!

                // Use Uri object instead of File to avoid storage permissions
                binding.previewImage.apply {
                    visibility = View.VISIBLE
                    setImageURI(uri)
                }
                //runTextRecognition(binding.previewImage.drawable.toBitmap())
            }

            ImagePicker.RESULT_ERROR -> {
                showToast(ImagePicker.getError(data))
            }

            else -> {
                showToast("No Image Selected")
            }
        }
    }

    private fun runTextRecognition(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        recognizer
            .process(inputImage)
            .addOnSuccessListener { text ->
                binding.textInImageLayout.visibility = View.VISIBLE
                processTextRecognitionResult(text)
            }.addOnFailureListener { e ->
                e.printStackTrace()
                showToast(e.localizedMessage ?: getString(R.string.error_default_msg))
            }
    }


    private fun processTextRecognitionResult(result: Text) {
        var finalText = ""
        val regexPattern = Regex("^(\\d\\s)?(.*)")

        var lineCount = 0 // Track line count
        for (block in result.textBlocks) {
            for (line in block.lines) {
                if (lineCount in 5..7) { // Check for the 5th, 6th, and 7th lines
                    val matchResult = regexPattern.find(line.text)
                    val modifiedLine = when (lineCount) {
                        5, 6 -> matchResult?.groupValues?.get(2)
                        7 -> matchResult?.groupValues?.get(2)?.replaceFirst("8\\s", "")
                        else -> line.text
                    }
                    modifiedLine?.let { finalText += it + " \n" }
                }
                lineCount++
            }
        }

        binding.textInImage.text = if (finalText.isNotEmpty()) {
            finalText
        } else {
            getString(R.string.no_text_found)
        }

        Linkify.addLinks(binding.textInImage, Linkify.ALL)
    }

    /*
    // this is blake's regex that I am trying to implement. I feel like it should work however it doesn't call anything.

    fun processTextRecognitionResult(text: Text): Triple<String, Any, Any> {
        val lastName = Regex("(?m)^1 (.+)^2 | $")   // regex for last name
        val firstName = Regex("(?m)^2 (.+)^8 | $ | [abc]{1} \z")  //regex for first name
        val addyCity = Regex("(?m)^8 (.+)^9 | $")  // regex for address+city

        val lastNameMatch = lastName.find(text.toString())?.groupValues?.get(1) ?: ""
        val firstNameMatch = firstName.find(text.toString())?.groupValues?.get(1) ?: ""
        val addyCityMatch = addyCity.find(text.toString())?.groupValues?.get(1) ?: ""

        val fullName = if (firstNameMatch.isNotBlank() && lastNameMatch.isNotBlank()) {
            "$firstNameMatch $lastNameMatch" //combines first name with last name
        } else {
            ""
        }

        val address = addyCityMatch.getOrNull(0) ?: ""  // Taking the first line as address
        val city = addyCityMatch.getOrNull(1) ?: ""     // Taking the second line as city

        return Triple(fullName, address, city)
    }
    */


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )


            } catch (exc: Exception) {
                showToast(getString(R.string.error_default_msg))
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun isTextValid(text: String?): Boolean {
        if (text == null)
            return false

        return text.isNotEmpty() and !text.equals(getString(R.string.no_text_found))
    }

    private fun shareText(text: String) {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_text_title)))
    }

    private fun copyToClipboard(text: CharSequence) {
        val clipboard =
            ContextCompat.getSystemService(applicationContext, ClipboardManager::class.java)
        val clip = ClipData.newPlainText("label", text)
        clipboard?.setPrimaryClip(clip)
        showToast(getString(R.string.clipboard_text))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.clear()
        val textInImage = (binding.textInImage.text).toString()
        if (isTextValid(textInImage)) {
            outState.putString(SAVED_TEXT_TAG, textInImage)
        }
        /*if (binding.previewImage.visibility == View.VISIBLE) {
            outState.putParcelable(SAVED_IMAGE_BITMAP, binding.previewImage.drawable.toBitmap())
        }*/
    }
}




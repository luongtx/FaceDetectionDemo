package com.example.facedetectiondemo

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), ImageAnalyzer.CallBackAnalyzer {
    lateinit var cameraExecutor: ExecutorService
    lateinit var detector: FaceDetector

    //    lateinit var tv_direct: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //request permission
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        cameraExecutor = Executors.newSingleThreadExecutor();

        //set firebase detector options
        val options = FaceDetectorOptions.Builder()
                .setClassificationMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .enableTracking()
                .build();
        detector = FaceDetection.getClient(options);

//        tv_direct = findViewById(R.id.tv_direct);
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        // bind the lifecycle of cameras to the lifecycle owner.
        // This eliminates the task of opening and closing the camera since CameraX is lifecycle-aware.
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
                    .also { it.setSurfaceProvider(viewFinder.createSurfaceProvider()) }

            val imageAnalyzer = ImageAnalyzer(detector);
            imageAnalyzer.setCallbacks(this)
            val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, imageAnalyzer)
                    }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))

    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults:
            IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                        this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA);
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown();
    }

    private fun changeDirect(text: String) {
        tv_direct.text = text
    }

    override fun onFaceAngleChange(text: String) {
        changeDirect(text);
    }

}

class ImageAnalyzer(var detector: FaceDetector) : ImageAnalysis.Analyzer {

    interface CallBackAnalyzer {
        fun onFaceAngleChange(text: String)
    }

    private lateinit var callBackAnalyzer: CallBackAnalyzer

    fun setCallbacks(callBackAnalyzer: CallBackAnalyzer) {
        this.callBackAnalyzer = callBackAnalyzer
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        var mediaImage = imageProxy.image
        if (mediaImage != null) {
            var image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            // Pass image to an ML Kit Vision API
            detector.process(image)
                    .addOnSuccessListener { faces ->
                        processListFace(faces)
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                    }
        }
    }


    private fun processListFace(faces: List<Face>) {
        for (face in faces) {
            var rotY = face.headEulerAngleY
            var rotX = face.headEulerAngleX
//                println("rotY: $rotY")
//                println("rotX: $rotX")
            when {
                rotY > 45 -> callBackAnalyzer.onFaceAngleChange("face is turn left")
                rotY < -45 -> callBackAnalyzer.onFaceAngleChange("face is turn right")
                else -> callBackAnalyzer.onFaceAngleChange("face is straight")
            }

        }
    }
}

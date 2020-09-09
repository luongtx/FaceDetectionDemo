package com.example.facedetectiondemo

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.Size
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
import kotlinx.android.synthetic.main.activity_face_detect.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), ImageAnalyzer.CallBackAnalyzer {
    lateinit var cameraExecutor: ExecutorService
    lateinit var faceDetector: FaceDetector
    lateinit var targetHeadRotations: ArrayList<Int>
    lateinit var tts: TextToSpeech

    //lateinit var tv_direct: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detect)

        //request permission
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        //set firebase detector options
        val options = FaceDetectorOptions.Builder()
                .setClassificationMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .enableTracking()
                .build();
        faceDetector = FaceDetection.getClient(options);

        targetHeadRotations = ArrayList(listOf(FaceRotation.LEFT, FaceRotation.RIGHT, FaceRotation.UP, FaceRotation.DOWN, FaceRotation.STRAIGHT))
        tv_direct.text = ("Please turn your face " + FaceRotation.valueOfs[targetHeadRotations.first()])

        tts = TextToSpeech(this) {
            tts.language = Locale.UK
            tts.speak(tv_direct.text.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
        }
        cameraExecutor = Executors.newSingleThreadExecutor();
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

            val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(Size(360, 480))
                    .build()
                    .also {
                        val imageAnalyzer = ImageAnalyzer(faceDetector);
                        imageAnalyzer.setCallbacks(this)
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

//    override fun onDestroy() {
//        super.onDestroy()
//    }

    private fun onDetectionCompleted() {
        faceDetector.close()
        cameraExecutor.shutdown()
        Thread.sleep(1000)
        val intent = Intent(this, DetectionResultsActivity::class.java)
        startActivity(intent)
    }


    override fun onFaceAngleChange(rotation: Int) {
        tv_rotation.text = ("Rotation: " + FaceRotation.valueOfs[rotation])
        if (rotation == targetHeadRotations.first()) {
            targetHeadRotations.remove(targetHeadRotations.first())
            if (targetHeadRotations.isEmpty()) {
                tv_direct.text = ("Authentication successfully!")
                onDetectionCompleted()
                return
            }
            //change direction
            tv_direct.text = if (targetHeadRotations.first() == FaceRotation.STRAIGHT) "Please keep your face straight"
            else "Please turn your face ${FaceRotation.valueOfs[rotation + 1]}"
            tts.speak(tv_direct.text.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

}


class FaceRotation {
    companion object {
        const val STRAIGHT = 0
        const val LEFT = 1
        const val RIGHT = 2
        const val UP = 3
        const val DOWN = 4
        val ANGLE = 40
        val valueOfs = mapOf(STRAIGHT to "straight", LEFT to "left", RIGHT to "right", UP to "up", DOWN to "down")
        val straightBoundary = 10.0
    }
}


class ImageAnalyzer(var detector: FaceDetector) : ImageAnalysis.Analyzer {


    interface CallBackAnalyzer {
        fun onFaceAngleChange(rotation: Int)
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
//            println("rotY: $rotY")
//            println("rotX: $rotX")
            when {
                rotY > FaceRotation.ANGLE -> callBackAnalyzer.onFaceAngleChange(FaceRotation.LEFT)
                rotY < -FaceRotation.ANGLE -> callBackAnalyzer.onFaceAngleChange(FaceRotation.RIGHT)
                else -> {
                    when {
                        rotX > FaceRotation.ANGLE -> callBackAnalyzer.onFaceAngleChange(FaceRotation.UP)
                        rotX < -FaceRotation.ANGLE -> callBackAnalyzer.onFaceAngleChange(FaceRotation.DOWN)
                        rotX in -FaceRotation.straightBoundary..FaceRotation.straightBoundary &&
                                rotY in -FaceRotation.straightBoundary..FaceRotation.straightBoundary
                        -> callBackAnalyzer.onFaceAngleChange(FaceRotation.STRAIGHT)
                    }
                }
            }
        }
    }
}

package com.example.ekycdemo

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ekycdemo.service.FaceAnalyzer
import com.example.ekycdemo.service.util.FaceRotation
import com.example.ekycdemo.utils.Constants
import com.example.ekycdemo.utils.Constants.Companion.REQUEST_CODE_PERMISSIONS
import com.example.ekycdemo.utils.Constants.Companion.REQUIRED_PERMISSIONS
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.android.synthetic.main.activity_face_detection.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class FaceDetectionActivity : AppCompatActivity(), FaceAnalyzer.CallBackAnalyzer {
    lateinit var cameraExecutor: ExecutorService
    lateinit var faceDetector: FaceDetector
    lateinit var targetFaceRotations: ArrayList<Int>
    lateinit var tts: TextToSpeech

    //lateinit var tv_direct: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detection)

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

        targetFaceRotations = ArrayList(listOf(FaceRotation.LEFT, FaceRotation.RIGHT, FaceRotation.UP, FaceRotation.DOWN, FaceRotation.STRAIGHT))
        tv_direct.text = ("Please turn your face " + FaceRotation.valueOfs[targetFaceRotations.first()])

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
                    .also { it.setSurfaceProvider(prv_face_detection.createSurfaceProvider()) }

            val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(Size(360, 480))
                    .build()
                    .also {
                        val imageAnalyzer = FaceAnalyzer(faceDetector);
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
                Log.e(Constants.TAG, "Use case binding failed", e)
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


    private fun onDetectionCompleted() {
        faceDetector.close()
        cameraExecutor.shutdown()
        Thread.sleep(1000)
        val intent = Intent(this, DetectionResultsActivity::class.java)
        startActivity(intent)
    }


    override fun onFaceAngleChange(rotation: Int) {
        tv_rotation.text = ("Rotation: " + FaceRotation.valueOfs[rotation])
        if (rotation == targetFaceRotations.first()) {
            targetFaceRotations.remove(targetFaceRotations.first())
            if (targetFaceRotations.isEmpty()) {
                tv_direct.text = ("Authentication successfully!")
                onDetectionCompleted()
                return
            }
            //change direction
            tv_direct.text = if (targetFaceRotations.first() == FaceRotation.STRAIGHT) "Please keep your face straight"
            else "Please turn your face ${FaceRotation.valueOfs[rotation + 1]}"
            tts.speak(tv_direct.text.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
}





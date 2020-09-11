package com.example.ekycdemo.service

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.ekycdemo.service.util.FaceRotation
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetector

class FaceAnalyzer(var detector: FaceDetector) : ImageAnalysis.Analyzer {


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
                        rotX in -FaceRotation.STRAIGHT_BOUNDARY..FaceRotation.STRAIGHT_BOUNDARY &&
                                rotY in -FaceRotation.STRAIGHT_BOUNDARY..FaceRotation.STRAIGHT_BOUNDARY
                        -> callBackAnalyzer.onFaceAngleChange(FaceRotation.STRAIGHT)
                    }
                }
            }
        }
    }
}
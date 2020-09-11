package com.example.ekycdemo.service

import android.graphics.Bitmap
import android.widget.Toast
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer

class TextProcessor() {
    private fun runTextRecognition(mSelectedImage: Bitmap) {
        val image = InputImage.fromBitmap(mSelectedImage, 0)
        val recognizer: TextRecognizer = TextRecognition.getClient()
        recognizer.process(image)
                .addOnSuccessListener(
                        OnSuccessListener<Text?> { texts ->
                            processTextRecognitionResult(texts)
                        })
                .addOnFailureListener(
                        OnFailureListener { e -> // Task failed with an exception
                            e.printStackTrace()
                        })
    }

    private fun processTextRecognitionResult(texts: Text) {
        val blocks: List<Text.TextBlock> = texts.textBlocks
        if (blocks.isEmpty()) {
            showToast("No text found")
            return
        }
        for (i in blocks.indices) {
            val lines: List<Text.Line> = blocks[i].lines
            for (j in lines.indices) {
                val elements: List<Text.Element> = lines[j].elements
                for (k in elements.indices) {
                    //
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(null, message, Toast.LENGTH_SHORT).show()
    }
}
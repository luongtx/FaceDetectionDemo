package com.example.ekycdemo.service.util

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

package com.example.ekycdemo.service.util

class FaceRotation {
    companion object {
        const val STRAIGHT = 0
        const val LEFT = 1
        const val RIGHT = 2
        const val UP = 3
        const val DOWN = 4
        val valueOfs = mapOf(STRAIGHT to "straight", LEFT to "left", RIGHT to "right", UP to "up", DOWN to "down")
        const val ANGLE = 40
        const val STRAIGHT_BOUNDARY = 10.0
    }
}

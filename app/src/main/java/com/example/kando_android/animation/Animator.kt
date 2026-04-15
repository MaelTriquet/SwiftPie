package com.example.kando_android.animation

import kotlin.math.*

class Animator
{
    // --- tuning ---
    private val durationFrames = 20   // how many frames the animation lasts

    // --- easing function: takes progress (0..1), returns coverage (0..1 or beyond) ---
    // swap this out to change animation feel
    var easingFunction: (Float) -> Float = Easing.overshootOut()

    // --- state ---
    var currentX: Float = 0f
    var currentY: Float = 0f

    var targetX: Float = 0f
    var targetY: Float = 0f

    private var startX: Float = 0f
    private var startY: Float = 0f
    private var frame: Int = durationFrames   // start settled

    fun teleport(x: Float, y: Float)
    {
        startX = x;   currentX = x;   targetX = x
        startY = y;   currentY = y;   targetY = y
        frame = durationFrames
    }

    fun moveTo(x: Float, y: Float)
    {
        // start interpolation from wherever we currently are
        startX = currentX
        startY = currentY
        targetX = x
        targetY = y
        frame = 0
    }

    // returns true if still animating
    fun update(): Boolean
    {
        if (frame >= durationFrames)
        {
            currentX = targetX
            currentY = targetY
            return false
        }

        val progress = frame.toFloat() / durationFrames.toFloat()
        val coverage = easingFunction(progress)

        currentX = startX + (targetX - startX) * coverage
        currentY = startY + (targetY - startY) * coverage

        frame++
        return true
    }
}

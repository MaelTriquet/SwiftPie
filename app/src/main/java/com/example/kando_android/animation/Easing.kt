package com.example.kando_android.animation

import kotlin.math.*

object Easing
{
    // smooth deceleration, no overshoot
    val easeOut: (Float) -> Float = { t ->
        1f - (1f - t) * (1f - t)
    }

    // smooth acceleration then deceleration
    val easeInOut: (Float) -> Float = { t ->
        if (t < 0.5f) 2f * t * t
        else 1f - (-2f * t + 2f).pow(2f) / 2f
    }

    // spring with overshoot — stiffness controls bounce amount, damping controls decay
    fun spring(stiffness: Float = 8f, damping: Float = 0.6f): (Float) -> Float = { t ->
        val envelope = 1f - exp(-damping * stiffness * t)
        val oscillation = cos(stiffness * t * (1f - damping))
        envelope + (1f - envelope) * (1f - oscillation) * 0.3f
    }

    // simple linear, no easing
    val linear: (Float) -> Float = { t -> t }

	fun overshootOut(overshoot: Float = 1.70158f): (Float) -> Float = { t ->
		val tMinus1 = t - 1f
		tMinus1 * tMinus1 * ((overshoot + 1f) * tMinus1 + overshoot) + 1f
	}
}

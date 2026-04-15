package com.example.kando_android.control

import com.example.kando_android.pie.PieManager
import com.example.kando_android.node.Node
import kotlin.math.*
import kotlinx.coroutines.*

class ActivationControl(
    private val pieManager: PieManager
)
{
    // --- tuning ---
    private val minStrokeLength = 100f   // px before corner detection activates
    private val minStrokeAngle  = 20f    // degrees — minimum angle to count as a corner
    private val jitterThreshold = 10f    // px — ignore tiny tip movements (noise)
    private val stillTimeout    = 400L   // ms — how long stationary before activation

    // --- stroke state ---
    private var strokeStart: Pair<Float, Float>? = null
    private var strokeEnd:   Pair<Float, Float>? = null

    // --- still detection ---
    private var stillJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    // --- public entry point, called each frame ---
    fun checkActivation(eventx: Float, eventy: Float)
    {
        val current = Pair(eventx, eventy)

        if (strokeStart == null)
        {
            strokeStart = current
            strokeEnd   = current
            return
        }

        val (sx, sy) = strokeStart!!
        val (ex, ey) = strokeEnd!!

        // S→E vector
        val strokeDx = ex - sx
        val strokeDy = ey - sy
        val strokeLength = length(strokeDx, strokeDy)


        if (strokeLength > minStrokeLength)
        {
            // E→M vector
            val tipDx = eventx - ex
            val tipDy = eventy - ey
            val tipLength = length(tipDx, tipDy)

            if (tipLength > jitterThreshold)
            {
                // finger is moving — cancel still timer
                cancelStillTimer()

                // angle between S→E and E→M
                val dot = (tipDx / tipLength) * (strokeDx / strokeLength) +
                          (tipDy / tipLength) * (strokeDy / strokeLength)
                val angleDeg = Math.toDegrees(acos(dot.toDouble().coerceIn(-1.0, 1.0))).toFloat()

                if (angleDeg > minStrokeAngle)
                {
                    // sharp enough turn — activate at E, reset stroke to E
                    if (pieManager.SelectedNode != null)
                        activateSelected()
                    strokeStart = strokeEnd
                    strokeEnd   = current
                    return
                }

                // no corner yet — advance E to M
                strokeEnd = current
            }

            // stroke is long enough — arm the still timer if not already running
            armStillTimer(eventx, eventy)

        } else {
            // stroke too short — just advance E
            strokeEnd = current
        }
    }

    fun reset()
    {
        strokeStart = null
        strokeEnd   = null
        cancelStillTimer()
    }

    // --- activation ---
	private fun activateSelected()
	{
		if (pieManager.SelectedNode?.toNodeMenu() != null)
		{
			pieManager.pie.current.radius = 50f
			pieManager.pie.current = pieManager.SelectedNode!!.toNodeMenu()!!
			pieManager.pie.current.setChidrenPos()
			pieManager.pie.current.radius = 100f
			pieManager.SelectedNode?.isSelected = false
			pieManager.SelectedNode = null
		} else {
			// pieManager.SelectedNode?.activate()
			// don't do anything
		}
		reset()
	}

    // --- still timer ---
	private fun armStillTimer(x: Float, y: Float)
	{
		if (stillJob?.isActive == true) return
		stillJob = scope.launch {
			delay(stillTimeout)
			if (pieManager.SelectedNode != null)
				activateSelected()
		}
	}

    private fun cancelStillTimer()
    {
        stillJob?.cancel()
        stillJob = null
    }

    // --- math ---
    private fun length(dx: Float, dy: Float) = sqrt(dx * dx + dy * dy)
}

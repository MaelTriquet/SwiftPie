package com.example.kando_android

import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import android.view.MotionEvent
import com.example.kando_android.pie.Pie
import com.example.kando_android.pie.PieManager
import com.example.kando_android.pie.PieLoader
import com.example.kando_android.node.NodeMenu
import com.example.kando_android.node.NodeLeaf
import com.example.kando_android.control.ActivationControl
import android.content.Intent
import android.graphics.Color

class MainActivity : ComponentActivity() {

    private lateinit var pie: Pie
    private lateinit var pieManager: PieManager
	private lateinit var activationControl: ActivationControl
	var x: Float = -1f
	var y: Float = -1f

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		pie = PieLoader.load(this)
		pieManager = PieManager(pie)
		activationControl = ActivationControl(pieManager)
		pie.isShown = false

		val renderView = RenderView(this)
		renderView.setBackgroundColor(Color.TRANSPARENT)
		setContentView(renderView)
	}

	private fun openAppDrawer()
	{
		val intent = Intent("com.android.launcher.action.APP_DRAWER")
		startActivity(intent)
	}

    inner class RenderView(context: Context) : View(context) {

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            // Call pie every frame
            pie.show(canvas)

            // Request next frame
            invalidate()
        }
    }

	private var lastTapTime = 0L
	private val doubleTapInterval = 300L

	override fun onTouchEvent(event: MotionEvent): Boolean {
		x = event.x
		y = event.y

		when (event.action) {
			MotionEvent.ACTION_DOWN -> {
				val now = System.currentTimeMillis()
				if (now - lastTapTime < doubleTapInterval)
					openAppDrawer()
				lastTapTime = now
				pie.current = pie.origin
				pie.current.radius = 100f
				pie.isShown = true
				pie.origin.teleport(x, y)      // ← was: pie.origin.posx = x / posy = y
				pie.origin.setChidrenPos()
			}

			MotionEvent.ACTION_MOVE -> {
				pieManager.act(x, y)
				activationControl.checkActivation(x, y)
			}

			MotionEvent.ACTION_UP -> {
				if (pieManager.SelectedNode != null && pieManager.SelectedNode?.toNodeMenu() == null)
					pieManager.SelectedNode!!.activate()
				pie.isShown = false
				pie.current.radius = 50f
				activationControl.reset()
				x = -1f
				y = -1f
			}
		}

		return true
	}
}

package com.example.kando_android.node

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.widget.Toast
import com.example.kando_android.config.ConfigActivity
import android.content.Intent

class NodeLeaf(
    private val context: android.content.Context,
    val packageName: String = ""
) : Node()
{
	private val icon: android.graphics.drawable.Drawable? by lazy {
        try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            null
        }
    }

	override fun show(canvas: android.graphics.Canvas, root: Boolean)
    {
        super.show(canvas, false)

        if (icon == null) return

        val x = animator.currentX
        val y = animator.currentY
        val size = (radius * 2.1f).toInt()

        icon!!.setBounds(
            (x - size / 2).toInt(),
            (y - size / 2).toInt(),
            (x + size / 2).toInt(),
            (y + size / 2).toInt()
        )

        // clip drawing to the circle
        canvas.save()
        val path = android.graphics.Path().apply {
            addCircle(x, y, radius, android.graphics.Path.Direction.CW)
        }
        canvas.clipPath(path)
        icon!!.draw(canvas)
        canvas.restore()
    }

    override fun activate()
    {
        if (packageName.isEmpty()) return
		if (packageName == "config")
		{
			val intent = Intent(context, ConfigActivity::class.java)
			intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
			context.startActivity(intent)
			return
		}
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null)
            context.startActivity(intent)
        else
            android.widget.Toast.makeText(context, "App not found: $packageName", android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun toNodeMenu(): NodeMenu?
    {
        return null
    }
}

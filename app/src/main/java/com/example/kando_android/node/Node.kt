package com.example.kando_android.node

import android.graphics.*
import com.example.kando_android.animation.Animator

abstract class Node()
{
    // vars
    var radius: Float = 50f
    var posx: Float                         // logical position (target)
        get() = animator.targetX
        set(v) { animator.moveTo(v, animator.targetY) }
    var posy: Float
        get() = animator.targetY
        set(v) { animator.moveTo(animator.targetX, v) }
    var angle: Float = 0f
    var distToCenter: Float = 200f
    var isShown: Boolean = true
    var isSelected: Boolean = false
    var parent: Node? = null
    var initPosx: Float = 0f
    var initPosy: Float = 0f

    val animator = Animator()

    // Call this when a node should appear at a position with no animation
    fun teleport(x: Float, y: Float)
    {
        animator.teleport(x, y)
        initPosx = x
        initPosy = y
    }

    open fun show(canvas: Canvas, root: Boolean = false)
    {
        animator.update()
        val x = animator.currentX
        val y = animator.currentY

        if (!isShown) return

        if (parent != null)
        {
            val centerx = parent!!.animator.currentX
            val centery = parent!!.animator.currentY
            canvas.drawLine(centerx, centery, x, y, Paint().apply {
                color = Color.argb(255, 255, 0, 0)
                style = Paint.Style.STROKE
                strokeWidth = 8f
            })
            canvas.drawCircle(centerx, centery, parent!!.radius, Paint().apply {
                color = Color.argb(255, 0, 0, 255)
                style = Paint.Style.FILL
            })
        }

        canvas.drawCircle(x, y, radius, Paint().apply {
            color = Color.argb(255, 0, 0, 255)
            style = Paint.Style.FILL
        })
    }

    open fun activate() {}
    abstract fun toNodeMenu(): NodeMenu?
}

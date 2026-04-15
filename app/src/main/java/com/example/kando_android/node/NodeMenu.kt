package com.example.kando_android.node

import android.graphics.Canvas
import kotlin.math.*

class NodeMenu : Node()
{
	var children: MutableList<Node> = mutableListOf()

	override fun show(canvas: Canvas, root: Boolean)
	{
		if (root)
		{
			for (child in children)
				child.show(canvas)
		}
		super.show(canvas, false)
	}

	fun setChidrenPos()
	{
		var base_angle = 0f
		if (parent != null)
			base_angle = angle - 180f
		for (i in children.indices)
		{
			val child = children[i]
			if (parent != null)
				child.angle = (i+1).toFloat() * 360f / (children.size+1) + base_angle
			else
				child.angle = i.toFloat() * 360f / children.size

			val targetX = posx + distToCenter * cos(Math.toRadians(child.angle.toDouble())).toFloat()
			val targetY = posy + distToCenter * sin(Math.toRadians(child.angle.toDouble())).toFloat()

			// teleport to parent center first so animation starts from here
			child.animator.teleport(posx, posy)

			// then set target — animator will slide outward from center
			child.posx = targetX
			child.posy = targetY
			child.initPosx = targetX
			child.initPosy = targetY
		}
	}

	fun addChild(child: Node)
	{
		children.add(child)
		child.parent = this
		setChidrenPos()
	}

	override fun toNodeMenu(): NodeMenu?
	{
		return this
	}
}

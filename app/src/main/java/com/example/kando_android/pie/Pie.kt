package com.example.kando_android.pie

import com.example.kando_android.node.Node
import com.example.kando_android.node.NodeMenu
import android.content.Context
import android.graphics.Canvas

class Pie(
	val origin: NodeMenu,
	context: Context
)
{
	// vals
	// vars
	var isShown: Boolean = true
	var current: NodeMenu = origin
	// methods

	fun show(canvas: Canvas)
	{
		if (!isShown) return
		current.parent?.radius = current.radius
		current.show(canvas, true)
	}
}

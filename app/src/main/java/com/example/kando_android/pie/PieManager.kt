package com.example.kando_android.pie

import com.example.kando_android.node.Node
import kotlin.math.*

class PieManager(
	var pie: Pie
)
{
	// vals
	// vars
	var SelectedNode: Node? = null
	// methods
	fun act(eventx: Float, eventy: Float)
	{

		if (pie.current.parent != null && distance(eventx, eventy, pie.current.parent!!.posx, pie.current.parent!!.posy) < pie.current.parent!!.radius)
		{
			if (SelectedNode != pie.current.parent)
			{
				SelectedNode?.isSelected = false
				SelectedNode?.posx = SelectedNode!!.initPosx
				SelectedNode?.posy = SelectedNode!!.initPosy
				SelectedNode = pie.current.parent
				SelectedNode?.isSelected = true
			}
			return
		}
		if (distance(pie.current.posx, pie.current.posy, eventx, eventy) > pie.current.radius)
		{
			var minDist = Float.MAX_VALUE
			var minChild: Node? = null
			for (child in pie.current.children)
			{
				val dist = distance(child.initPosx, child.initPosy, eventx, eventy)
				if (dist < minDist)
				{
					minDist = dist
					minChild = child
				}
			}
			if (minChild != SelectedNode)
			{
				SelectedNode?.isSelected = false
				if (SelectedNode != pie.current.parent)
				{
					SelectedNode?.posx = SelectedNode!!.initPosx
					SelectedNode?.posy = SelectedNode!!.initPosy
				}
				SelectedNode = minChild
				SelectedNode?.isSelected = true
			}
			minChild?.posx = eventx
			minChild?.posy = eventy
			minChild!!.animator.teleport(eventx, eventy)
		} else if (SelectedNode != null)
		{
			SelectedNode?.isSelected = false
			SelectedNode?.posx = SelectedNode!!.initPosx
			SelectedNode?.posy = SelectedNode!!.initPosy
			SelectedNode = null
		}
	}

	fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float
	{
		val dx = x2 - x1
		val dy = y2 - y1
		return sqrt(dx * dx + dy * dy)
	}
}

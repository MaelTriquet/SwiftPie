package com.example.kando_android.config

import android.content.Context
import android.graphics.*
import android.view.*
import com.example.kando_android.node.*
import com.example.kando_android.pie.Pie
import kotlin.math.*

class ConfigPieView(
    context: Context,
    private val pie: Pie,
    private val onChanged: () -> Unit
) : View(context)
{
    // --- state ---
    private var currentMenu: NodeMenu = pie.origin
    private var dragState: DragState? = null
    private val breadcrumb: MutableList<NodeMenu> = mutableListOf()

    // --- paint ---
    private val circlePaint = Paint().apply { color = Color.argb(255, 0, 0, 200) }
    private val selectedPaint = Paint().apply { color = Color.argb(255, 0, 180, 0) }
    private val linePaint = Paint().apply {
        color = Color.RED; style = Paint.Style.STROKE; strokeWidth = 4f
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE; textSize = 28f; textAlign = Paint.Align.CENTER
    }
    private val ghostPaint = Paint().apply { alpha = 180 }
    private val dropHighlightPaint = Paint().apply {
        color = Color.argb(120, 255, 255, 0); style = Paint.Style.FILL
    }

    // --- layout constants ---
    private val centerRadius = 80f
    private val childRadius  = 55f
    private val orbitRadius  = 250f
    private val deleteZoneHeight = 120f  // px from bottom of this view = delete zone

    // --- computed positions ---
    private var centerX = 0f
    private var centerY = 0f
    private data class ChildSlot(val x: Float, val y: Float, val index: Int)
    private var slots: List<ChildSlot> = emptyList()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int)
    {
        centerX = w / 2f
        centerY = h / 2f
        recomputeSlots()
    }

    private fun recomputeSlots()
    {
        val count = currentMenu.children.size
        slots = (0 until count).map { i ->
            val angle = i.toFloat() * 360f / count
            ChildSlot(
                centerX + orbitRadius * cos(Math.toRadians(angle.toDouble())).toFloat(),
                centerY + orbitRadius * sin(Math.toRadians(angle.toDouble())).toFloat(),
                i
            )
        }
    }

    // --- called from outside to start a drag ---
    fun startDrag(payload: DragPayload, x: Float, y: Float)
    {
        // convert screen coords to view coords
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        dragState = DragState(payload, x - loc[0], y - loc[1])
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean
    {
        val loc = IntArray(2)
        getLocationOnScreen(loc)

        when (event.action)
        {
            MotionEvent.ACTION_MOVE ->
            {
                dragState?.let {
                    it.x = event.rawX - loc[0]
                    it.y = event.rawY - loc[1]
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP ->
            {
                dragState?.let { ds ->
                    handleDrop(ds, event.rawX - loc[0], event.rawY - loc[1])
                }
                dragState = null
                invalidate()
            }

            MotionEvent.ACTION_DOWN ->
            {
                // if no drag in progress, check if tapping a NodeMenu child to dive in
                if (dragState == null)
                    handleTap(event.x, event.y)
            }
        }
        return true
    }

    private fun handleTap(x: Float, y: Float)
    {
        // check back arrow (center tap when not at root)
        if (currentMenu != pie.origin)
        {
            val dist = sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
            if (dist < centerRadius)
            {
                currentMenu = breadcrumb.removeLastOrNull() ?: pie.origin
                recomputeSlots()
                invalidate()
                return
            }
        }

        // check child taps
        slots.forEachIndexed { i, slot ->
            val dist = sqrt((x - slot.x).pow(2) + (y - slot.y).pow(2))
            if (dist < childRadius)
            {
                val child = currentMenu.children[i]
                if (child is NodeMenu)
                {
                    breadcrumb.add(currentMenu)
                    currentMenu = child
                    recomputeSlots()
                    invalidate()
                }
                return
            }
        }
    }

    private fun handleDrop(ds: DragState, x: Float, y: Float)
    {
        // drop in delete zone (bottom of view)
        if (y > height - deleteZoneHeight)
        {
            if (ds.payload is DragPayload.ExistingNodePayload)
            {
                val p = ds.payload
                p.sourceParent.children.removeAt(p.sourceIndex)
                recomputeSlots()
                onChanged()
            }
            return
        }

        // find closest slot
        val insertIndex = findInsertIndex(x, y)

        when (val payload = ds.payload)
        {
            is DragPayload.AppPayload ->
            {
                val leaf = NodeLeaf(context, payload.packageName)
                currentMenu.children.add(insertIndex, leaf)
                leaf.parent = currentMenu
            }
            is DragPayload.NewMenuPayload ->
            {
                val menu = NodeMenu()
                currentMenu.children.add(insertIndex, menu)
                menu.parent = currentMenu
            }
            is DragPayload.ExistingNodePayload ->
            {
                // remove from source first
                val node = payload.sourceParent.children.removeAt(payload.sourceIndex)
                // adjust index if same parent and source was before insert
                val adjusted = if (payload.sourceParent == currentMenu && payload.sourceIndex < insertIndex)
                    insertIndex - 1 else insertIndex
                currentMenu.children.add(adjusted.coerceIn(0, currentMenu.children.size), node)
                node.parent = currentMenu
            }
        }

        recomputeSlots()
        onChanged()
        invalidate()
    }

    // find which index to insert at based on drop position
    private fun findInsertIndex(x: Float, y: Float): Int
    {
        if (slots.isEmpty()) return 0
        // find closest slot and insert before or after based on angle
        var minDist = Float.MAX_VALUE
        var closest = 0
        slots.forEachIndexed { i, slot ->
            val d = sqrt((x - slot.x).pow(2) + (y - slot.y).pow(2))
            if (d < minDist) { minDist = d; closest = i }
        }
        return (closest + 1).coerceAtMost(currentMenu.children.size)
    }

    // wire up dragging existing nodes from the pie
    private fun startExistingDrag(index: Int)
    {
        val child = currentMenu.children[index]
        val slot = slots[index]
        val icon = if (child is NodeLeaf)
            runCatching { context.packageManager.getApplicationIcon(child.packageName) }.getOrNull()
        else null

        dragState = DragState(
            DragPayload.ExistingNodePayload(
                packageName = if (child is NodeLeaf) child.packageName else null,
                isMenu = child is NodeMenu,
                icon = icon,
                sourceParent = currentMenu,
                sourceIndex = index
            ),
            slot.x, slot.y
        )
        invalidate()
    }

    override fun onDraw(canvas: Canvas)
    {
        super.onDraw(canvas)
        setBackgroundColor(Color.BLACK)

        // breadcrumb
        val crumbText = (breadcrumb.map { "Root" } + listOf("Current")).joinToString(" > ")
        canvas.drawText(crumbText, centerX, 40f, textPaint)

        // draw lines to children
        slots.forEach { slot ->
            canvas.drawLine(centerX, centerY, slot.x, slot.y, linePaint)
        }

        // draw center node
        canvas.drawCircle(centerX, centerY, centerRadius, circlePaint)
        if (currentMenu != pie.origin)
            canvas.drawText("↑", centerX, centerY + 10f, textPaint)

        // draw children
        slots.forEachIndexed { i, slot ->
            val child = currentMenu.children[i]
            val isDropTarget = dragState != null && isNearSlot(dragState!!.x, dragState!!.y, slot)
            canvas.drawCircle(slot.x, slot.y, childRadius, if (isDropTarget) selectedPaint else circlePaint)

            when (child)
            {
                is NodeLeaf -> {
                    val icon = runCatching {
                        context.packageManager.getApplicationIcon(child.packageName)
                    }.getOrNull()
                    if (icon != null)
                    {
                        val s = childRadius.toInt()
                        icon.setBounds(
                            (slot.x - s * 0.7f).toInt(), (slot.y - s * 0.7f).toInt(),
                            (slot.x + s * 0.7f).toInt(), (slot.y + s * 0.7f).toInt()
                        )
                        canvas.save()
                        canvas.clipPath(Path().apply {
                            addCircle(slot.x, slot.y, childRadius, Path.Direction.CW)
                        })
                        icon.draw(canvas)
                        canvas.restore()
                    }
                }
                is NodeMenu -> canvas.drawText("☰", slot.x, slot.y + 10f, textPaint)
                else -> {}
            }
        }

        // delete zone indicator
        val deleteZoneTop = height - deleteZoneHeight
        canvas.drawRect(0f, deleteZoneTop, width.toFloat(), height.toFloat(), dropHighlightPaint)
        canvas.drawText("drag here to remove", centerX, deleteZoneTop + deleteZoneHeight / 2, textPaint)

        // draw ghost under finger
        dragState?.let { ds ->
            val icon = when (val p = ds.payload)
            {
                is DragPayload.AppPayload -> p.icon
                is DragPayload.ExistingNodePayload -> p.icon
                else -> null
            }
            if (icon != null)
            {
                val s = 60
                icon.setBounds(
                    (ds.x - s).toInt(), (ds.y - s).toInt(),
                    (ds.x + s).toInt(), (ds.y + s).toInt()
                )
                icon.draw(canvas)
            } else {
                canvas.drawCircle(ds.x, ds.y, 40f, ghostPaint)
                canvas.drawText("☰", ds.x, ds.y + 10f, textPaint)
            }
        }

        invalidate()
    }

    private fun isNearSlot(x: Float, y: Float, slot: ChildSlot): Boolean
    {
        return sqrt((x - slot.x).pow(2) + (y - slot.y).pow(2)) < childRadius * 1.5f
    }
}

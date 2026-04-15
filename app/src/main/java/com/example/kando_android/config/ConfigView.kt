package com.example.kando_android.config

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.view.*
import com.example.kando_android.node.*
import com.example.kando_android.pie.Pie
import com.example.kando_android.pie.PieSerializer
import com.example.kando_android.pie.PieStorage
import kotlin.math.*
import android.util.Log
import android.graphics.Color
import com.example.kando_android.animation.Animator

class ConfigView(
    context: Context,
    private val pie: Pie
) : View(context)
{
    // --- layout ---
    private val splitRatio   = 0.6f
    private val dragThreshold = 20f
    private val appColumns   = 4
    private val appCellSize  = 180f
    private val appIconSize  = 100f
    private val pieOrbit     = 280f
    private val pieCenter    = 90f
    private val pieChild     = 60f

    // --- pie navigation ---
    private var currentMenu: NodeMenu = pie.origin
    private val breadcrumb: MutableList<NodeMenu> = mutableListOf()

    // --- app list ---
    private var appItems: List<AppItem> = emptyList()
    private var scrollOffsetY = 0f
    private var maxScrollY    = 0f

    // --- touch tracking ---
    private var downX         = 0f
    private var downY         = 0f
	private var upX           = 0f
	private var upY           = 0f
    private var downTime      = 0L
    private var isDragging    = false
    private var isScrolling   = false
    private var lastTapTime   = 0L
    private val doubleTapMs   = 300L

    // --- drag ---
    private var dragState: DragState? = null

    // --- paint ---
    private val bgPaint        = Paint().apply { color = Color.BLACK }
    private val circlePaint    = Paint().apply { color = Color.argb(255, 30, 30, 180) }
    private val highlightPaint = Paint().apply { color = Color.argb(255, 30, 180, 30) }
    private val deletePaint    = Paint().apply { color = Color.argb(80, 255, 0, 0) }
    private val linePaint      = Paint().apply {
        color = Color.argb(180, 255, 60, 60)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val textPaint      = Paint().apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }
    private val smallTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }
    private val bigTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 70f
        textAlign = Paint.Align.CENTER
    }
    private val ghostPaint     = Paint().apply { alpha = 180 }
    private val dividerPaint   = Paint().apply { color = Color.DKGRAY }

    // --- geometry (computed in onSizeChanged) ---
    private var pieCenterX = 0f
    private var pieCenterY = 0f
    private var splitY     = 0f

    data class SlotInfo(val x: Float, val y: Float, val index: Int, val animator: Animator = Animator())
    private var slots: List<SlotInfo> = emptyList()
	val btnRadius = 50f
	var btnX = width - btnRadius - 16f
	var btnY = splitY - btnRadius - 16f

    // -------------------------------------------------------------------------
    // setup
    // -------------------------------------------------------------------------

    fun loadApps()
    {
        val pm = context.packageManager
        appItems = pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { pm.getApplicationLabel(it).toString() }
            .map { AppItem(
                it.packageName,
                pm.getApplicationLabel(it).toString(),
                pm.getApplicationIcon(it.packageName)
            )}
        // compute max scroll
        val rows = ceil(appItems.size.toFloat() / appColumns).toInt()
        maxScrollY = max(0f, rows * appCellSize - (height * (1f - splitRatio)))
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int)
    {
        splitY     = h * splitRatio
        pieCenterX = w / 2f
        pieCenterY = splitY / 2f
        recomputeSlots()
        val rows = ceil(appItems.size.toFloat() / appColumns).toInt()
        maxScrollY = max(0f, rows * appCellSize - (h * (1f - splitRatio)))
		btnX = width - btnRadius - 16f
		btnY = splitY - btnRadius - 16f
    }

    private fun recomputeSlots()
    {
		val count: Int
		val baseAngle: Float
		if (currentMenu.parent != null)
		{
			count = currentMenu.children.size + 1
			baseAngle = currentMenu.parent!!.angle - 180f + 360f / count
		}
		else
		{
			count = currentMenu.children.size
			baseAngle = 0f
		}
        if (count == 0) { slots = emptyList(); return }
        slots = (0 until currentMenu.children.size).map { i ->
            val angle = i.toFloat() * 360f / count + baseAngle
			val targetX = pieCenterX + pieOrbit * cos(Math.toRadians(angle.toDouble())).toFloat()
			val targetY = pieCenterY + pieOrbit * sin(Math.toRadians(angle.toDouble())).toFloat()

			val existing = slots.getOrNull(i)
			val animator: Animator
			if (existing == null)
				animator = Animator().also {
					// new slot — spawn from center
					it.teleport(pieCenterX, pieCenterY)
				}
			else
			{
				if (existing!!.x < 0f)
					animator = Animator().also {
						// new slot — spawn from center
						it.teleport(upX, upY)
					}
				else
					animator = existing.animator
			}
			animator.moveTo(targetX, targetY)

            SlotInfo(targetX, targetY, i, animator)
        }
    }

    // -------------------------------------------------------------------------
    // touch
    // -------------------------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean
    {
        val x = event.x
        val y = event.y

        when (event.action)
        {
            MotionEvent.ACTION_DOWN ->
            {
                downX   = x
                downY   = y
                downTime = System.currentTimeMillis()
                isDragging  = false
                isScrolling = false
            }

            MotionEvent.ACTION_MOVE ->
            {
                val moved = sqrt((x - downX).pow(2) + (y - downY).pow(2))

                if (!isDragging && !isScrolling && moved > dragThreshold)
                {
                    if (downY > splitY)
                    {
                        // started below splitY
                        val tapped = appItemAt(downX, downY)
                        if (tapped != null)
                        {
                            // drag from app list
                            isDragging = true
                            dragState  = DragState(
                                DragPayload.AppPayload(tapped.packageName, tapped.icon),
                                x, y
                            )
                        } else {
                            // scroll
                            isScrolling = true
                        }
                    } else {
                        // started above splitY — drag existing node if one was touched
                        val slotIndex = slotAt(downX, downY)
                        if (slotIndex != null)
                        {
                            isDragging = true
                            dragState  = buildExistingDrag(slotIndex, x, y)
                            // remove from pie immediately so slot disappears while dragging
                            currentMenu.children.removeAt(slotIndex)
							var passedChange = false
							slots = (0 until currentMenu.children.size).map { i ->
								if (i == slotIndex)
									passedChange = true
								if (passedChange)
									slots.getOrNull(i+1) ?: SlotInfo(0f, 0f, i, Animator())
								else
									slots.getOrNull(i) ?: SlotInfo(0f, 0f, i, Animator())
							}
                            recomputeSlots()
                        }
                        // tapped center = handled on UP
                    }
                }

                if (isDragging)
                {
                    dragState?.x = x
                    dragState?.y = y
                    invalidate()
                }

                if (isScrolling)
                {
                    val dy = downY - y
                    scrollOffsetY = (scrollOffsetY + dy).coerceIn(0f, maxScrollY)
                    downX = x
                    downY = y
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP ->
            {
				upX = x
				upY = y
                if (isDragging)
                {
                    handleDrop(x, y)
                    dragState  = null
                    isDragging = false
                    recomputeSlots()
                    save()
                    invalidate()
                } else {
                    // it was a tap — check double tap first
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < doubleTapMs)
                    {
                        // double tap — exit config
                        (context as? android.app.Activity)?.finish()
                        return true
                    }
                    lastTapTime = now

                    // single tap
                    if (downY < splitY)
                        handlePieTap(downX, downY)
                    // taps below splitY on app icons do nothing on UP without drag
                }
                isScrolling = false
            }
        }
        return true
    }

    // -------------------------------------------------------------------------
    // tap handling
    // -------------------------------------------------------------------------

    private fun handlePieTap(x: Float, y: Float)
    {
		// check for new menu button
		val distBtn = sqrt((x - btnX).pow(2) + (y - btnY).pow(2))
		if (distBtn < btnRadius)
		{
			currentMenu.children.add(NodeMenu())
			recomputeSlots()
			invalidate()
			return
		}
        // tap center = go up
        val distCenter = sqrt((x - pieCenterX).pow(2) + (y - pieCenterY).pow(2))
        if (distCenter < pieCenter)
        {
            if (currentMenu != pie.origin)
            {
                currentMenu = breadcrumb.removeLastOrNull() ?: pie.origin
				slots = emptyList()
                recomputeSlots()
                invalidate()
            }
            return
        }

        // tap a child node
        val index = slotAt(x, y) ?: return
        val child = currentMenu.children[index]
        if (child is NodeMenu)
        {
            breadcrumb.add(currentMenu)
            currentMenu = child
			slots = emptyList()
            recomputeSlots()
            invalidate()
        }
    }

    // -------------------------------------------------------------------------
    // drop handling
    // -------------------------------------------------------------------------

    private fun handleDrop(x: Float, y: Float)
    {
        val ds = dragState ?: return

        if (y > splitY)
        {
            // dropped in app zone — delete if it was an existing node, ignore otherwise
            // (node was already removed from pie when drag started if ExistingNodePayload)
            return
        }

        // dropped in pie zone — insert
        val index = insertIndexAt(x, y)
        val node: com.example.kando_android.node.Node = when (val p = ds.payload)
        {
            is DragPayload.AppPayload      -> NodeLeaf(context, p.packageName)
            is DragPayload.NewMenuPayload  -> NodeMenu()
            is DragPayload.ExistingNodePayload -> {
                // rebuild node from payload
                if (p.isMenu) NodeMenu()
                else NodeLeaf(context, p.packageName ?: "")
            }
        }
        node.parent = currentMenu
        currentMenu.children.add(index, node)
		var passedChange = false
		slots = (0 until currentMenu.children.size).map { i ->
			if (i == index)
			{
				passedChange = true
				SlotInfo(-1f, -1f, i, Animator())
			}
			else if (passedChange)
				slots.getOrNull(i-1) ?: SlotInfo(0f, 0f, i, Animator())
			else
				slots.getOrNull(i) ?: SlotInfo(0f, 0f, i, Animator())
		}
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private fun slotAt(x: Float, y: Float): Int?
    {
        slots.forEachIndexed { i, slot ->
            if (sqrt((x - slot.animator.currentX).pow(2) + (y - slot.animator.currentY).pow(2)) < pieChild * 1.4f)
                return i
        }
        return null
    }

    private fun appItemAt(x: Float, y: Float): AppItem?
    {
        val listY = y - splitY + scrollOffsetY
        val col   = (x / (width.toFloat() / appColumns)).toInt().coerceIn(0, appColumns - 1)
        val row   = (listY / appCellSize).toInt()
        val index = row * appColumns + col
		val dx = x - (col * (width.toFloat() / appColumns) + appCellSize / 2f + appIconSize / 2f)
		val dy = y - (row * appCellSize + appCellSize / 2f + splitY + appIconSize / 2f)
		val dist = sqrt(dx * dx + dy * dy)

		Log.d("ConfigView", "mouse: $x, $y")
		Log.d("ConfigView", "dx: $dx, dy: $dy")

		if (dist > appIconSize) return null

        return if (index in appItems.indices) appItems[index] else null
    }

    private fun insertIndexAt(x: Float, y: Float): Int
    {
        if (slots.isEmpty()) return 0
        // var minDist = Float.MAX_VALUE
        // var closest = 0
        // slots.forEachIndexed { i, slot ->
        //     val d = sqrt((x - slot.x).pow(2) + (y - slot.y).pow(2))
        //     if (d < minDist) { minDist = d; closest = i }
        // }
        // // insert after closest
        // return (closest + 1).coerceAtMost(currentMenu.children.size)
		if (currentMenu.parent != null)
		{
			val count = currentMenu.children.size + 1
			val baseAngle = currentMenu.parent!!.angle - 180f
			val minAngle = 360f / count

			val angles = (0 until count).map {i -> i.toFloat() * minAngle + baseAngle}.toFloatArray()
			for (i in angles.indices)
				if (angles[i] < 0) angles[i] += 360f
			var angle = getAngle(x, y, pieCenterX, pieCenterY)
			if (angle < 0) angle += 360f
			Log.d("ConfigView", "angle: $angle")
			for (i in angles.indices)
				Log.d("ConfigView", "angles[$i]: ${angles[i]}")
				
			for (i in angles.indices)
			{
				if (angles[i] <= angle && angles[(i+1)%(count)] > angle)
					return i
				if (angles[i] > angles[(i+1)%(count)])
				{
					if (angles[i] <= angle && angles[(i+1)%(count)] + 360f > angle)
						return i
					if (angles[i] - 360f <= angle && angles[(i+1)%(count)] > angle)
						return i
				}

			}
			Log.d("ConfigView", "FUCK")
			return 0
		}
		else
		{
			val count = slots.size
			val minAngle = 360f / count
			val angles = (0 until count).map {i -> i.toFloat() * minAngle}.toFloatArray()
			for (i in angles.indices)
				if (angles[i] < 0) angles[i] += 360f
			for (i in angles.indices)
				if (angles[i] > 360) angles[i] -= 360f
			var angle = getAngle(x, y, pieCenterX, pieCenterY)
			if (angle < 0) angle += 360f
			Log.d("ConfigView", "angle: $angle")
			for (i in angles.indices)
				Log.d("ConfigView", "angles[$i]: ${angles[i]}")
			if (currentMenu.parent == null)
			{
				for (i in angles.indices)
				{
					if (angles[i] > angle)
						return i
				}
				return count
			}	
		}
		return 0
    }

    private fun buildExistingDrag(index: Int, x: Float, y: Float): DragState
    {
        val child = currentMenu.children[index]
        val icon  = if (child is NodeLeaf)
            runCatching { context.packageManager.getApplicationIcon(child.packageName) }.getOrNull()
        else null
        return DragState(
            DragPayload.ExistingNodePayload(
                packageName  = if (child is NodeLeaf) child.packageName else null,
                isMenu       = child is NodeMenu,
                icon         = icon,
                sourceParent = currentMenu,
                sourceIndex  = index
            ),
            x, y
        )
    }

    private fun save()
    {
        val json = PieSerializer.serialize(pie.origin)
        PieStorage.save(context, json.toString(2))
    }

    // -------------------------------------------------------------------------
    // drawing
    // -------------------------------------------------------------------------

    override fun onDraw(canvas: Canvas)
    {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        drawAppZone(canvas)
        drawDivider(canvas)
        drawPieZone(canvas)
        drawGhost(canvas)

        invalidate()
    }

	private fun getAngle(x1: Float, y1: Float, x2: Float, y2: Float): Float
	{
		val dx = x1 - x2
		val dy = y1 - y2
		return Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
	}

    private fun drawPieZone(canvas: Canvas)
    {
        // delete hint when dragging existing node
        if (dragState?.payload is DragPayload.ExistingNodePayload)
            canvas.drawRect(0f, splitY - 60f, width.toFloat(), splitY, deletePaint)

        // lines to children
        slots.forEach { slot ->
            canvas.drawLine(pieCenterX, pieCenterY, slot.animator.currentX, slot.animator.currentY, linePaint)
        }
		if (dragState != null && dragState!!.y < splitY)
		{
			canvas.drawLine(pieCenterX, pieCenterY, dragState!!.x, dragState!!.y, linePaint)
		}

        // center node
        canvas.drawCircle(pieCenterX, pieCenterY, pieCenter, circlePaint)
        if (currentMenu != pie.origin)
            canvas.drawText("↑", pieCenterX, pieCenterY + 10f, textPaint)

        // child nodes
        slots.forEachIndexed { i, slot ->
			slot.animator.update()
            val child   = currentMenu.children[i]
            val isTarget = dragState != null &&
                dragState!!.y < splitY &&
                sqrt((dragState!!.x - slot.animator.currentX).pow(2) + (dragState!!.y - slot.animator.currentY).pow(2)) < pieChild * 1.5f

            canvas.drawCircle(slot.animator.currentX, slot.animator.currentY, pieChild, if (isTarget) highlightPaint else circlePaint)

            when (child)
            {
                is NodeLeaf ->
                {
                    val icon = runCatching {
                        context.packageManager.getApplicationIcon(child.packageName)
                    }.getOrNull()
                    drawIconInCircle(canvas, icon, slot.animator.currentX, slot.animator.currentY, pieChild)
                }
                is NodeMenu -> canvas.drawText("☰", slot.animator.currentX, slot.animator.currentY + 10f, textPaint)
                else        -> {}
            }
        }

        // breadcrumb
        canvas.drawText(
            (listOf("Root") + breadcrumb.indices.map { "▸" } + if (currentMenu != pie.origin) listOf("▸ here") else emptyList()).joinToString(" "),
            pieCenterX, 36f, smallTextPaint
        )
    }

    private fun drawDivider(canvas: Canvas)
    {
		canvas.drawRect(0f, 0f, width.toFloat(), splitY, bgPaint)

        canvas.drawRect(0f, splitY, width.toFloat(), splitY + 2f, dividerPaint)

        // "new folder" button on the divider
        // val btnW = 220f; val btnH = 50f
        // val btnX = width - btnW - 16f
        // val btnY = splitY - btnH / 2
        // canvas.drawRoundRect(RectF(btnX, btnY, btnX + btnW, btnY + btnH), 12f, 12f, highlightPaint)
        // canvas.drawText("+ New Folder", btnX + btnW / 2, btnY + 33f, smallTextPaint)
		canvas.drawCircle(btnX, btnY, btnRadius, highlightPaint)
		canvas.drawText("+", btnX, btnY + 25f, bigTextPaint)
    }

    private fun drawAppZone(canvas: Canvas)
    {
        val cellW = width.toFloat() / appColumns

        appItems.forEachIndexed { index, app ->
            val col  = index % appColumns
            val row  = index / appColumns
            val cx   = col * cellW + cellW / 2f
            val cy   = splitY + row * appCellSize - scrollOffsetY + appCellSize / 2f

            if (cy + appCellSize < splitY || cy - appCellSize > height) return@forEachIndexed

            val iconTop  = cy - appIconSize / 2f
            val iconBot  = cy + appIconSize / 2f - 24f
            app.icon.setBounds(
                (cx - appIconSize / 2f).toInt(), iconTop.toInt(),
                (cx + appIconSize / 2f).toInt(), iconBot.toInt()
            )
            app.icon.draw(canvas)
            canvas.drawText(app.label, cx, iconBot + 22f, smallTextPaint)
        }
    }

    private fun drawGhost(canvas: Canvas)
    {
        val ds = dragState ?: return
        val icon = when (val p = ds.payload)
        {
            is DragPayload.AppPayload          -> p.icon
            is DragPayload.ExistingNodePayload -> p.icon
            else                               -> null
        }
        drawIconInCircle(canvas, icon, ds.x, ds.y, 50f)
    }

    private fun drawIconInCircle(canvas: Canvas, icon: android.graphics.drawable.Drawable?, cx: Float, cy: Float, r: Float)
    {
        if (icon == null) return
        val s = (r * 0.85f).toInt()
        icon.setBounds((cx - s).toInt(), (cy - s).toInt(), (cx + s).toInt(), (cy + s).toInt())
        canvas.save()
        canvas.clipPath(Path().apply { addCircle(cx, cy, r, Path.Direction.CW) })
        icon.draw(canvas)
        canvas.restore()
    }
}

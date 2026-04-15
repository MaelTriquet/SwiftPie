package com.example.kando_android.config

import android.content.Context
import android.graphics.Color
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val context: Context,
    private val onDragStart: (DragPayload.AppPayload, Float, Float) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>()
{
    private var items: List<AppItem> = emptyList()

    fun submitList(list: List<AppItem>) { items = list; notifyDataSetChanged() }

    inner class ViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(8, 8, 8, 8)
        }
        val icon = android.widget.ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(80, 80)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }
        val label = TextView(context).apply {
            textSize = 10f
            gravity = android.view.Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(Color.WHITE)
        }
        layout.addView(icon)
        layout.addView(label)
        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int)
    {
        val item = items[position]
        val icon = holder.layout.getChildAt(0) as android.widget.ImageView
        val label = holder.layout.getChildAt(1) as TextView
        icon.setImageDrawable(item.icon)
        label.text = item.label

        holder.layout.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN)
            {
                val loc = IntArray(2)
                v.getLocationOnScreen(loc)
                onDragStart(
                    DragPayload.AppPayload(item.packageName, item.icon),
                    loc[0] + event.x,
                    loc[1] + event.y
                )
            }
            true
        }
    }

    override fun getItemCount() = items.size
}

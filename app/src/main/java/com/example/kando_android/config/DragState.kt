package com.example.kando_android.config

import android.graphics.drawable.Drawable

sealed class DragPayload
{
    // dragging an app from the list
    data class AppPayload(val packageName: String, val icon: Drawable) : DragPayload()
    // dragging a new empty menu from the palette
    object NewMenuPayload : DragPayload()
    // dragging an existing node within the pie
    data class ExistingNodePayload(
        val packageName: String?,   // null if it's a menu
        val isMenu: Boolean,
        val icon: Drawable?,
        val sourceParent: com.example.kando_android.node.NodeMenu,
        val sourceIndex: Int
    ) : DragPayload()
}

data class DragState(
    val payload: DragPayload,
    var x: Float,
    var y: Float
)

package com.example.kando_android.pie

import com.example.kando_android.node.NodeLeaf
import com.example.kando_android.node.NodeMenu
import org.json.JSONArray
import org.json.JSONObject

object PieSerializer
{
    fun serialize(menu: NodeMenu): JSONObject
    {
        val obj = JSONObject()
        obj.put("type", "menu")
        val children = JSONArray()
        for (child in menu.children)
        {
            when (child)
            {
                is NodeMenu -> children.put(serialize(child))
                is NodeLeaf -> {
                    val leaf = JSONObject()
                    leaf.put("type", "leaf")
                    leaf.put("package", child.packageName)
                    children.put(leaf)
                }
            }
        }
        obj.put("children", children)
        return obj
    }
}

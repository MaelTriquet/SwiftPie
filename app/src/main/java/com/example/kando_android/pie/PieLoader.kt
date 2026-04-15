package com.example.kando_android.pie

import android.content.Context
import android.content.Intent
import com.example.kando_android.node.NodeLeaf
import com.example.kando_android.node.NodeMenu
import org.json.JSONObject

object PieLoader
{
	fun load(context: Context): Pie
    {
        val json = PieStorage.getOrCreate(context)
        val root = JSONObject(json)
        val rootMenu = parseNode(context, root) as? NodeMenu
            ?: throw IllegalArgumentException("Root node must be of type menu")
        return Pie(rootMenu, context)
    }

    private fun parseNode(context: Context, json: JSONObject): com.example.kando_android.node.Node
    {
        val type = json.getString("type")

        return when (type)
        {
            "menu" -> {
                val menu = NodeMenu()
                val children = json.optJSONArray("children")
                if (children != null)
                    for (i in 0 until children.length())
                        menu.addChild(parseNode(context, children.getJSONObject(i)))
                menu
            }
            "leaf" -> {
                val packageName = json.getString("package")
                NodeLeaf(context, packageName)
            }
            else -> throw IllegalArgumentException("Unknown node type: $type")
        }
    }
}

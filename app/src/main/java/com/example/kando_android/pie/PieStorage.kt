package com.example.kando_android.pie

import android.content.Context
import java.io.File

object PieStorage
{
    private const val FILENAME = "pie.json"

    fun getOrCreate(context: Context): String
    {
        val file = getFile(context)
        if (!file.exists())
        {
            // first launch — copy default from assets
            val default = context.assets.open(FILENAME).bufferedReader().use { it.readText() }
            file.writeText(default)
        }
        return file.readText()
    }

    fun save(context: Context, json: String)
    {
        getFile(context).writeText(json)
    }

    fun getFile(context: Context): File
    {
        return File(context.filesDir, FILENAME)
    }
}

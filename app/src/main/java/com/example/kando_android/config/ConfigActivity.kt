package com.example.kando_android.config

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.kando_android.pie.PieLoader

class ConfigActivity : ComponentActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val pie = PieLoader.load(this)
        val view = ConfigView(this, pie)
        setContentView(view)
        view.loadApps()
    }
}

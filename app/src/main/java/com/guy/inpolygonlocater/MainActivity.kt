package com.guy.inpolygonlocater

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guy.polygonlocaterlibrary.PolygonLocater

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        PolygonLocater.init(this)


        val lat1 = 31.96932735216634
        val lon1 = 34.790859063540395
        val lat2 = 30.756775776235912
        val lon2 = 36.33266494451018
        val lat3 = -21.844783459805893
        val lon3 = 136.86328204031264

        val response1 = PolygonLocater.isInCountry(lat1, lon1, "IL")
        val response2 = PolygonLocater.isInCountry(lat2, lon2, "IL")
        val response3 = PolygonLocater.whichCountry(lat1, lon1)
        val response4 = PolygonLocater.whichCountry(lat3, lon3)
        val response5 = PolygonLocater.whichCountry(50.0, 50.0)
        val response6 = PolygonLocater.whichCountryInfo(lat1, lon1)

        Log.d("pttt", "onCreate: $response1")
        Log.d("pttt", "onCreate: $response2")
        Log.d("pttt", "onCreate: $response3")
        Log.d("pttt", "onCreate: $response4")
        Log.d("pttt", "onCreate: $response5")
        Log.d("pttt", "onCreate: $response6")
    }
}


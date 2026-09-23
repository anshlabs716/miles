package com.example.miles.auto

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class MilesCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return MilesCarNavigationScreen(carContext)
    }
}

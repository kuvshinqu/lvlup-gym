package com.levelup.gym

import android.app.Application
import com.levelup.gym.data.GymDatabase
import com.levelup.gym.data.Settings

class GymApp : Application() {
    val db: GymDatabase by lazy { GymDatabase.create(this) }
    val settings: Settings by lazy { Settings(this) }
}

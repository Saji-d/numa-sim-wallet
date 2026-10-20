package com.cloneapp.numa

import android.app.Application
import java.io.File

/** Process-wide store, created once in [NumaApp.onCreate]. */
lateinit var store: AppStore
    private set

class NumaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        store = AppStore(FileStateStorage(File(filesDir, "numa-state.json")))
    }
}

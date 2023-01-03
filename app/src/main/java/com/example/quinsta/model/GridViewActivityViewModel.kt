package com.example.quinsta.model

import android.app.Application
import android.graphics.Bitmap
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel

class GridViewActivityViewModel(application: Application) : AndroidViewModel(application) {

    var image: ArrayList<Bitmap> = arrayListOf()
}

object AppViewModel {
    private lateinit var instance: GridViewActivityViewModel

    @MainThread
    fun getInstance(application: Application): GridViewActivityViewModel {
        instance =
            if (::instance.isInitialized) instance else GridViewActivityViewModel(application)
        return instance
    }
}
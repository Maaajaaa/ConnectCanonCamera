package com.github.sparkycola.connectcanoncamera.ui.main

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel

class PageViewModel : ViewModel() {

    private val _index = MutableLiveData<Int>()
    val bitmap = MutableLiveData<Bitmap>()
    val text = MutableLiveData<String>()

    fun setIndex(index: Int) {
        _index.value = index
    }
}
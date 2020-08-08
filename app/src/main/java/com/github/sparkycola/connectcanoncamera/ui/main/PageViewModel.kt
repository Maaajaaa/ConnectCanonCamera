package com.github.sparkycola.connectcanoncamera.ui.main

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PageViewModel : ViewModel() {

    private val _index = MutableLiveData<Int>()
    val text = MutableLiveData<String>()
    val itemLength = MutableLiveData<Int>()
    val galleryObject = MutableLiveData<GalleryObject>()

    fun setIndex(index: Int) {
        _index.value = index
    }
}
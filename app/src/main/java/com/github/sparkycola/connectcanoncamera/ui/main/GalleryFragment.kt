package com.github.sparkycola.connectcanoncamera.ui.main

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.github.sparkycola.connectcanoncamera.R


/**
 * A placeholder fragment containing a simple view.
 */
class GalleryFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //view model in activity scope
        activity?.let{
            pageViewModel = ViewModelProviders.of(it).get(PageViewModel::class.java)
        }

        pageViewModel.bitmap.observe(this, Observer<Bitmap>{
            Log.d("Fragment", "got bitmap with ${it.height}x${it.width}px")
        })
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =  inflater.inflate(R.layout.gallery_fragment, container, false)
        val gridView: GridView = root.findViewById(R.id.gridView)
        val allItems = getAllItems()
        gridView.adapter = this.context?.let { GalleryAdapter(inflater,allItems, it) }
        gridView.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            Log.d("GalleryFragment", "Position: $position id: $id")
        }
        return root
    }

    private fun getAllItems(): List<GalleryObject> {
        val img = BitmapFactory.decodeResource(
            resources, R.drawable.sample_thumb
        )
        return mutableListOf(GalleryObject(img, "test"),
            GalleryObject(img, "test2"), GalleryObject(img, "test3"),
            GalleryObject(img,"test4"), GalleryObject(img, "test5"),
            GalleryObject(img,"test6"), GalleryObject(img,"test7"),
            GalleryObject(img,"test6"), GalleryObject(img,"test7"),
            GalleryObject(img,"test6"), GalleryObject(img,"test7"),
            GalleryObject(img,"test6"), GalleryObject(img,"test7"),
            GalleryObject(img,"test6"), GalleryObject(img,"test7"),
            GalleryObject(img,"test6"), GalleryObject(img,"test7"),
            GalleryObject(img,"test6"), GalleryObject(img,"test7"),
            GalleryObject(img,"test6"), GalleryObject(img,"test7"),
            GalleryObject(img,"test6"), GalleryObject(img,"test7"),
            GalleryObject(img,"test6"), GalleryObject(img,"test7")
        )
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): GalleryFragment {
            return GalleryFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}
package com.github.sparkycola.connectcanoncamera.ui.main

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.github.sparkycola.connectcanoncamera.R


/**
 * A placeholder fragment containing a simple view.
 */
class GalleryFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    private lateinit var gridView: GridView
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //view model in activity scope
        activity?.let{
            pageViewModel = ViewModelProviders.of(it).get(PageViewModel::class.java)
        }
        pageViewModel.itemLength.observe(this, Observer<Int> {
            val galAdapter = (gridView.adapter as GalleryAdapter)
            Log.d(tag, "got new number of Items: $it")
            galAdapter.listStorage = createListOfEmptyGalleryObjects(it)
            galAdapter.notifyDataSetChanged()
        })
        pageViewModel.galleryObject.observe(this, Observer<GalleryObject> {
            Log.d(tag, "got new thumbnail object with id${it.id}")
            (gridView.adapter as GalleryAdapter).updateTile(it)
        })
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =  inflater.inflate(R.layout.gallery_fragment, container, false)
        gridView = root.findViewById(R.id.gridView)
        gridView.adapter = this.context?.let { GalleryAdapter(inflater,createListOfEmptyGalleryObjects(25), it) }
        gridView.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            Log.d(tag, "Position: $position id: $id")
        }

        return root
    }

    private fun createListOfEmptyGalleryObjects(size: Int): MutableList<GalleryObject>{
        val galObjList = mutableListOf<GalleryObject>(GalleryObject(null,"loading"))
        for(i in 1..size){
            galObjList.add(GalleryObject(null,"loading"))
        }
        return galObjList
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
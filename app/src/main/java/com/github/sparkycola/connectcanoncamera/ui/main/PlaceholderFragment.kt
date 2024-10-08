package com.github.sparkycola.connectcanoncamera.ui.main

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.github.sparkycola.connectcanoncamera.databinding.FragmentMainBinding
import com.github.sparkycola.connectcanoncamera.libimink.IminkActionSet


/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    private lateinit var textView: TextView
    private lateinit var imageView: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //view model in activity scope
        activity?.let{
            pageViewModel = ViewModelProviders.of(it).get(PageViewModel::class.java)
        }
        pageViewModel.text.observe(this, Observer<String> {
            Log.d("Fragment", "got $it")
            textView.text = it
        })
    }


    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentMainBinding = FragmentMainBinding.inflate(inflater, container, false)
        textView = binding.sectionLabel
        return binding.root
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
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}
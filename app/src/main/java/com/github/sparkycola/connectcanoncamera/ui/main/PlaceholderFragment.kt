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
import com.github.sparkycola.connectcanoncamera.R
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

        pageViewModel.bitmap.observe(this, Observer<Bitmap>{
            Log.d("Fragment", "got bitmap with ${it.height}x${it.width}px")
            imageView.setImageBitmap(it)
        })
    }


    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_main, container, false)
        textView = root.findViewById(R.id.section_label)
        imageView = root.findViewById(R.id.imageView)

        //just a little test of the library
        /*val actionSet: IminkActionSet = IminkActionSet(setOf<KnownIminkAction>(
            KnownIminkAction.GetGPSCaptureTimeList,
            KnownIminkAction.GetGPSTime))*/
        //var hasGPSTime = actionSet.contains(KnownAction.GetGPSTime)
        //var hasSomethingitHasnt = actionSet.contains(KnownAction.GetMovieExtProperty)
        //Log.d(TAG, "actionset: hasGPSTime:$hasGPSTime but GetMovieProperty is $hasSomethingitHasnt")

        val actionSet: IminkActionSet = IminkActionSet("<?xml version=\"1.0\"?>\n" +
                "<scpd xmlns=\"urn:schemas-upnp-org:service-1-0\">\n" +
                " <specVersion>\n" +
                "  <major>1</major>\n" +
                "  <minor>0</minor>\n" +
                " </specVersion>\n" +
                " <actionList>\n" +
                "  <action>\n" +
                "   <name>SetUsecaseStatus</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Set</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">UsecaseStatus</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>GetObjRecvCapability</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Get</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">ObjRecvCapability</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>SetSendObjInfo</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Set</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">SendObjInfo</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>SetMovieExtProperty</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Set</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">MovieExtProperty</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>SetObjData</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Set</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">ObjData</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>GetResizeProperty</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Get</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">ResizeProperty</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>GetObjProperty</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Get</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">ObjProperty</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>GetMovieExtProperty</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Get</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">MovieExtProperty</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>GetObjCount</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Get</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">ObjCount</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>GetObjIDList</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Get</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">ObjIDList</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>GetThumbDataList</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Get</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">ThumbDataList</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>GetObjData</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Get</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">ObjData</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>GetGPSTime</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Get</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">GPSTime</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>GetGPSCaptureTimeList</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Get</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">GPSCaptureTimeList</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>GetGPSListRecvCapability</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Get</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">GPSListRecvCapability</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>SetGPSList</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Set</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">GPSList</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>SetGPSClearList</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Set</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">GPSClearList</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>SetDisconnectStatus</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Set</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">DisconnectStatus</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                "  <action>\n" +
                "   <name>GetGroupedObjIDList</name>\n" +
                "   <pnpx:X_actKind xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">Get</pnpx:X_actKind>\n" +
                "   <pnpx:X_resourceName xmlns:pnpx=\"urn:schemas-canon-com:schema-imink\">GroupedObjIDList</pnpx:X_resourceName>\n" +
                "  </action>\n" +
                " </actionList>\n" +
                "</scpd>")
        //textView.text = actionSet.toIminkDescriptorXMLString()
        return root
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
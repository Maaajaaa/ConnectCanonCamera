package com.github.sparkycola.connectcanoncamera.ui.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView


class GalleryAdapter(
    private val layoutInflater: LayoutInflater,
    var listStorage: MutableList<GalleryObject>,
    private val context: Context
) :BaseAdapter() {
    override fun getView(position: Int, view: View?, parent: ViewGroup?): View {
        val listViewHolder: ViewHolder
        var convertView = view
        if (convertView == null) {
            listViewHolder = ViewHolder()
            convertView = layoutInflater.inflate(com.github.sparkycola.connectcanoncamera.R.layout.listview_with_text_image, parent, false)
            listViewHolder.textInListView = convertView?.findViewById(com.github.sparkycola.connectcanoncamera.R.id.textView) as TextView
            listViewHolder.imageInListView = convertView.findViewById(com.github.sparkycola.connectcanoncamera.R.id.imageView) as ImageView
            convertView.tag = listViewHolder
        } else {
            listViewHolder = convertView.tag as ViewHolder
        }

        listViewHolder.textInListView?.text = listStorage[position].description
        if(listStorage[position].image == null){
            //blank image if bitmap is null
            listViewHolder.imageInListView?.setImageResource(android.R.color.transparent)
        }else{
            listViewHolder.imageInListView?.setImageBitmap(listStorage[position].image)
        }
        listViewHolder.imageInListView?.contentDescription = listStorage[position].description

        return convertView
    }
    internal class ViewHolder {
        var textInListView: TextView? = null
        var imageInListView: ImageView? = null
    }

    override fun getItem(p0: Int): Any {
        return listStorage[p0]
    }

    override fun getItemId(p0: Int): Long {
        return listStorage[p0].id.toLong()
    }

    override fun getCount(): Int {
        return listStorage.size
    }

    fun updateTile(galobj: GalleryObject){
        listStorage[galobj.id] = galobj
        //make changes visible
        this.notifyDataSetChanged()
    }
}
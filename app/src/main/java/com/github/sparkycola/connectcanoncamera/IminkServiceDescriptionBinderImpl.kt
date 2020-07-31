package com.github.sparkycola.connectcanoncamera

import android.util.Log
import org.fourthline.cling.binding.staging.MutableDevice
import org.fourthline.cling.binding.xml.UDA10ServiceDescriptorBinderImpl
import org.w3c.dom.Element
import org.w3c.dom.NodeList

class IminkServiceDescriptionBinderImpl: UDA10ServiceDescriptorBinderImpl() {
    private val TAG = "IminkSrvcDescBindImpl"
    /*override fun hydrateRoot(descriptor: MutableDevice?, rootElement: Element?) {
        if (rootElement.nodeName == "scpd")
        val nodes: NodeList = rootElement?.getElementsByTagName("serviceList")!!.item(0).childNodes.item(1).childNodes
        Log.d(TAG, "root ${rootElement.nodeName}")
        /*for(i in 0..nodes.length){
            Log.d(TAG, "Node$i: ${nodes.item(i).nodeName}=${nodes.item(i).textContent}")
        }*/
        super.hydrateRoot(descriptor, rootElement)
    }*/
}
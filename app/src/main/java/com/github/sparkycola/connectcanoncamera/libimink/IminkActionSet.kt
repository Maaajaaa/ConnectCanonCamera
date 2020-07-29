package com.github.sparkycola.connectcanoncamera.libimink

import android.util.Xml
import org.xmlpull.v1.XmlSerializer
import java.io.StringWriter

class IminkActionSet {
    private lateinit var set: Set<KnownIminkAction>

    constructor(iminkDescriptorXmlString: String) {
        //Todo: Implement this
    }

    constructor(of: Set<KnownIminkAction>){
        this.set = of
    }

    fun contains(element: KnownIminkAction): Boolean{
        return set.contains(element)
    }

    //create imink device descriptor string
    @Throws(IllegalArgumentException::class , IllegalStateException::class)
    fun toIminkDescriptorXMLString(): String? {
        var stringWriter = StringWriter()
        var xmlSerializer: XmlSerializer = Xml.newSerializer()
        val ns_imink = "urn:schemas-canon-com:schema-imink"
        val ns_upnp = "urn:schemas-upnp-org:service-1-0"
        //<?xml tag
        xmlSerializer.setOutput(stringWriter)
        xmlSerializer.startDocument("UTF-8",true)
        //<scpd
        xmlSerializer.startTag(ns_upnp,"scpd")
        //<specVersion
        xmlSerializer.startTag("","specVersion")
        xmlSerializer.startTag("","major")
        xmlSerializer.text("1")
        xmlSerializer.endTag("","major")
        xmlSerializer.startTag("","minor")
        xmlSerializer.text("0")
        xmlSerializer.endTag("","minor")
        xmlSerializer.endTag("","specVersion")
        //<actionList
        xmlSerializer.startTag(ns_imink,"actionList")
        set.forEach {
            xmlSerializer.startTag(ns_imink,"action")
            xmlSerializer.startTag("","name")
            xmlSerializer.text(it.name)
            xmlSerializer.endTag("","name")
            xmlSerializer.startTag(ns_imink,"X_actKind")
            xmlSerializer.text(it.kind.name)
            xmlSerializer.endTag(ns_imink,"X_actKind")
            xmlSerializer.startTag(ns_imink,"X_resourceName")
            xmlSerializer.text(it.resourceName)
            xmlSerializer.endTag(ns_imink,"X_resourceName")
            xmlSerializer.endTag(ns_imink,"action")
        }
        xmlSerializer.endTag(ns_imink,"actionList")
        //end <scpd and document
        xmlSerializer.endTag(ns_upnp,"scpd")
        xmlSerializer.endDocument()
        return stringWriter.toString()
    }
}

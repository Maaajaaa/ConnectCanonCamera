package com.github.sparkycola.connectcanoncamera.libimink

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlSerializer
import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import java.io.StringWriter

class IminkActionSet {
    private var set: Set<KnownIminkAction>

    //namespace constants
    //Todo make those nicer and project-wide
    private val nsImink = "urn:schemas-canon-com:schema-imink"
    private val nsUpnp = "urn:schemas-upnp-org:service-1-0"

    @Throws(XmlPullParserException::class, IOException::class)
    constructor(iminkDescriptorXmlString: String) {
        val supportedActions = mutableSetOf<KnownIminkAction>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setInput(StringReader(iminkDescriptorXmlString))
        while (parser.next() != XmlPullParser.END_DOCUMENT){
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            //look for the action names in the list and ignore everything else
            if (parser.name == "action") {
                while(!(parser.next() == XmlPullParser.END_TAG && parser.name == "action")){
                    if (parser.name == "name" && parser.eventType == XmlPullParser.START_TAG){
                        parser.next()
                        if(parser.text != null){
                            KnownIminkAction.valueOf(parser.text)?.let{supportedActions.add(it)}
                        }
                    }
                }
            }
        }
        this.set = supportedActions
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
        val stringWriter = StringWriter()
        val xmlSerializer: XmlSerializer = Xml.newSerializer()
        //<?xml tag
        xmlSerializer.setOutput(stringWriter)
        xmlSerializer.startDocument("UTF-8",true)
        //<scpd
        xmlSerializer.startTag(nsUpnp,"scpd")
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
        xmlSerializer.startTag(nsImink,"actionList")
        set.forEach {
            xmlSerializer.startTag(nsImink,"action")
            xmlSerializer.startTag("","name")
            xmlSerializer.text(it.name)
            xmlSerializer.endTag("","name")
            xmlSerializer.startTag(nsImink,"X_actKind")
            xmlSerializer.text(it.kind.name)
            xmlSerializer.endTag(nsImink,"X_actKind")
            xmlSerializer.startTag(nsImink,"X_resourceName")
            xmlSerializer.text(it.resourceName)
            xmlSerializer.endTag(nsImink,"X_resourceName")
            xmlSerializer.endTag(nsImink,"action")
        }
        xmlSerializer.endTag(nsImink,"actionList")
        //end <scpd and document
        xmlSerializer.endTag(nsUpnp,"scpd")
        xmlSerializer.endDocument()
        return stringWriter.toString()
    }
}

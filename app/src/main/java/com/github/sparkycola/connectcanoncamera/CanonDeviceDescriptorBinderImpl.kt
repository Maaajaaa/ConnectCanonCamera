package com.github.sparkycola.connectcanoncamera

import android.util.Log
import org.fourthline.cling.binding.xml.DescriptorBindingException
import org.fourthline.cling.binding.xml.UDA10DeviceDescriptorBinderImpl
import org.fourthline.cling.model.ValidationException
import org.fourthline.cling.model.meta.Device
import org.seamless.util.Exceptions
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.StringReader


class CanonDeviceDescriptorBinderImpl: UDA10DeviceDescriptorBinderImpl() {
    private val TAG: String = "CanonDevDescBinderImpl"

    @Throws(
        DescriptorBindingException::class,
        ValidationException::class
    )
    override fun <D : Device<*, *, *>?> describe(undescribedDevice: D, descriptorXml: String?): D? {
        Log.d(TAG,"describing device")
        var descriptorXml = descriptorXml
        var device: D? = null
        val originalException: DescriptorBindingException
        try {
            try {
                if (descriptorXml != null) descriptorXml =
                    descriptorXml.trim { it <= ' ' } // Always trim whitespace
                device = super.describe(undescribedDevice, descriptorXml)
                return device
            } catch (ex: DescriptorBindingException) {
                Log.e(TAG, "Regular parsing failed:${Exceptions.unwrap(ex).message}")
                originalException = ex
            }
        } catch (ex: ValidationException) {
            Log.e(TAG,"ValidationException:${Exceptions.unwrap(ex).message}")
        }
        throw IllegalStateException("No device produced, did you swallow exceptions in your subclass?")
    }
    //https://stackoverflow.com/questions/34875409/is-there-a-short-way-to-parse-xml-string-in-android
    fun parseXml(xml: String?) {
        try {
            val factory =
                XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val xpp = factory.newPullParser()
            xpp.setInput(StringReader(xml)) // pass input whatever xml you have
            var eventType = xpp.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                xpp.name
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    Log.d(TAG, "Start document")
                } else if (eventType == XmlPullParser.START_TAG) {
                    Log.d(TAG, "Start tag " + xpp.name)
                } else if (eventType == XmlPullParser.END_TAG) {
                    Log.d(TAG, "End tag " + xpp.name)

                } else if (eventType == XmlPullParser.TEXT) {
                    Log.d(
                        TAG,
                        "Text " + xpp.text
                    ) // here you get the text from xml
                }
                eventType = xpp.next()
            }
            Log.d(TAG, "End document")
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
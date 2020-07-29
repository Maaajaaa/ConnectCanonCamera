package com.github.sparkycola.connectcanoncamera

import android.util.Log
import org.fourthline.cling.binding.xml.Descriptor
import org.fourthline.cling.binding.xml.DescriptorBindingException
import org.fourthline.cling.binding.xml.UDA10DeviceDescriptorBinderImpl
import org.fourthline.cling.model.Namespace
import org.fourthline.cling.model.ValidationException
import org.fourthline.cling.model.XMLUtil.appendNewElementIfNotNull
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.model.profile.RemoteClientInfo
import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory


class CanonDeviceDescriptorBinderImpl: UDA10DeviceDescriptorBinderImpl() {
    private val TAG: String = "CanonDevDescBinderImpl"

    @Throws(
        DescriptorBindingException::class,
        ValidationException::class
    )

    override fun generate(
        deviceModel: Device<*, out Device<*, *, *>, out Service<*, *>>?,
        info: RemoteClientInfo?,
        namespace: Namespace?
    ): String {
        val orignalXMLString = super.generate(deviceModel, info, namespace)
        val XMLWithImink = orignalXMLString.replace("</service>","<ns:X_SCPDURL xmlns:ns=\"urn:schemas-canon-com:schema-imink\">desc_iml/CameraConnectedMobile.xml</ns:X_SCPDURL> \n" +
                "<ns:X_ExtActionVer xmlns:ns=\"urn:schemas-canon-com:schema-imink\">1.0</ns:X_ExtActionVer> \n" +
                "<ns:X_VendorExtVer xmlns:ns=\"urn:schemas-canon-com:schema-imink\">1-1502.0.0.0</ns:X_VendorExtVer></service>")
        return XMLWithImink
    }
    override fun generateRoot(
        namespace: Namespace?,
        deviceModel: Device<*, *, *>?,
        descriptor: Document,
        info: RemoteClientInfo?
    ) {
        val rootElement = descriptor.createElementNS(
            Descriptor.Device.NAMESPACE_URI,
            Descriptor.Device.ELEMENT.root.toString()
        )
        descriptor.appendChild(rootElement)
        generateSpecVersion(namespace, deviceModel, descriptor, rootElement)

        //UDA 1.1 spec says: Don't use URLBase anymore BUT Canon uses UDA1.0!!
        //if (deviceModel.getBaseURL() != null) {
            appendNewElementIfNotNull(descriptor, rootElement, "URLBase", "http://10.42.0.129:49152/")
        //}
        generateDevice(namespace, deviceModel, descriptor, rootElement, info)
    }

    @Throws(DescriptorBindingException::class)
    override fun buildDOM(deviceModel: Device<*, *, *>, info: RemoteClientInfo?, namespace: Namespace?): Document? {
        return try {
            Log.d(TAG,"Generating DOM from device model: $deviceModel")
            val factory =
                DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val d = factory.newDocumentBuilder().newDocument()
            generateRoot(namespace, deviceModel, d, info)
            //d.getElementsByTagName("UDN").forEach { it.textContent= "uuid:2188B849-F71E-4B2D-AAF3-EE57761A9975"}
            //d.getElementsByTagName("root").forEach { it.insertBefore(it.childNodes.item(1), Node.) }
            d
        } catch (ex: Exception) {
            throw DescriptorBindingException(
                "Could not generate device descriptor: " + ex.message,
                ex
            )
        }
    }
}
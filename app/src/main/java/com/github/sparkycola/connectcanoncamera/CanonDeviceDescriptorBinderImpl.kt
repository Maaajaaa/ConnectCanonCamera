package com.github.sparkycola.connectcanoncamera

import android.util.Log
import org.fourthline.cling.binding.staging.MutableDevice
import org.fourthline.cling.binding.staging.MutableService
import org.fourthline.cling.binding.xml.Descriptor
import org.fourthline.cling.binding.xml.DescriptorBindingException
import org.fourthline.cling.binding.xml.UDA10DeviceDescriptorBinderImpl
import org.fourthline.cling.model.Namespace
import org.fourthline.cling.model.ValidationException
import org.fourthline.cling.model.XMLUtil
import org.fourthline.cling.model.XMLUtil.appendNewElementIfNotNull
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.model.profile.RemoteClientInfo
import org.fourthline.cling.model.types.InvalidValueException
import org.fourthline.cling.model.types.ServiceId
import org.fourthline.cling.model.types.ServiceType
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URI

class CanonDeviceDescriptorBinderImpl : UDA10DeviceDescriptorBinderImpl() {
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
        //Todo soft-code this
        val XMLWithImink = orignalXMLString.replace(
            "</service>",
            "<ns:X_SCPDURL xmlns:ns=\"urn:schemas-canon-com:schema-imink\">desc_iml/CameraConnectedMobile.xml</ns:X_SCPDURL>" +
                    "<ns:X_ExtActionVer xmlns:ns=\"urn:schemas-canon-com:schema-imink\">1.0</ns:X_ExtActionVer>" +
                    "<ns:X_VendorExtVer xmlns:ns=\"urn:schemas-canon-com:schema-imink\">1-1502.0.0.0</ns:X_VendorExtVer></service>"
        )
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
        appendNewElementIfNotNull(
            descriptor,
            rootElement,
            "URLBase",
            "http://${getIpv4HostAddress()}:$hostPort/"
        )
        //}
        generateDevice(namespace, deviceModel, descriptor, rootElement, info)
    }

    @Throws(DescriptorBindingException::class)
    override fun hydrateServiceList(
        descriptor: MutableDevice,
        serviceListNode: Node
    ) {
        val serviceListNodeChildren = serviceListNode.childNodes
        for (i in 0 until serviceListNodeChildren.length) {
            val serviceListNodeChild = serviceListNodeChildren.item(i)
            if (serviceListNodeChild.nodeType != Node.ELEMENT_NODE) continue
            if (Descriptor.Device.ELEMENT.service.equals(
                    serviceListNodeChild
                )
            ) {
                val serviceChildren = serviceListNodeChild.childNodes
                try {
                    val service = MutableService()
                    for (x in 0 until serviceChildren.length) {
                        val serviceChild = serviceChildren.item(x)
                        Log.d(
                            TAG,
                            "serviceChild: ${serviceChild.nodeName} ns: ${serviceChild.namespaceURI} localname: ${serviceChild.localName}"
                        )
                        var iminkSCPDURL: URI? = null
                        if (serviceChild.nodeType != Node.ELEMENT_NODE) continue
                        when {
                            Descriptor.Device.ELEMENT.serviceType.equals(serviceChild) -> {
                                service.serviceType =
                                    ServiceType.valueOf(XMLUtil.getTextContent(serviceChild))
                            }
                            Descriptor.Device.ELEMENT.serviceId.equals(serviceChild) -> {
                                service.serviceId = ServiceId.valueOf(
                                    XMLUtil.getTextContent(serviceChild)
                                )
                            }
                            Descriptor.Device.ELEMENT.SCPDURL.equals(serviceChild) -> {
                                service.descriptorURI = parseURI(
                                    XMLUtil.getTextContent(serviceChild)
                                )
                            }
                            Descriptor.Device.ELEMENT.controlURL.equals(serviceChild) -> {
                                service.controlURI = parseURI(XMLUtil.getTextContent(serviceChild))
                            }
                            Descriptor.Device.ELEMENT.eventSubURL.equals(serviceChild) -> {
                                service.eventSubscriptionURI =
                                    parseURI(XMLUtil.getTextContent(serviceChild))
                            }
                        }
                        if (serviceChild.localName == "X_SCPDURL" && serviceChild.namespaceURI == "urn:schemas-canon-com:schema-imink") {
                            iminkSCPDURL = parseURI(XMLUtil.getTextContent(serviceChild))
                            Log.d(
                                TAG,
                                "found iminkSCPDURL: ${serviceChild.textContent} parsed as: $iminkSCPDURL"
                            )
                        }
                        /* custom code: override the SCPDURL/service descriptor URI if a IMINK X_SCPDURL was found*/
                        if (iminkSCPDURL != null) {
                            service.descriptorURI = iminkSCPDURL
                        }
                    }


                    descriptor.services.add(service)
                } catch (ex: InvalidValueException) {
                    Log.w(
                        TAG,
                        "UPnP specification violation, skipping invalid service declaration. "
                                + ex.message
                    )
                }
            }
        }
    }

    override fun hydrateRoot(descriptor: MutableDevice?, rootElement: Element?) {
        val nodes: NodeList =
            rootElement?.getElementsByTagName("serviceList")!!.item(0).childNodes.item(1).childNodes
        super.hydrateRoot(descriptor, rootElement)
    }

    fun getIpv4HostAddress(): String {
        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
            networkInterface.inetAddresses?.toList()?.find {
                !it.isLoopbackAddress && it is Inet4Address
            }?.let { return it.hostAddress }
        }
        return ""
    }
}
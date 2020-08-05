package com.github.sparkycola.connectcanoncamera

import android.util.Log
import org.fourthline.cling.binding.staging.MutableDevice
import org.fourthline.cling.binding.staging.MutableService
import org.fourthline.cling.binding.xml.Descriptor
import org.fourthline.cling.binding.xml.Descriptor.Device.ELEMENT
import org.fourthline.cling.binding.xml.DescriptorBindingException
import org.fourthline.cling.binding.xml.UDA10DeviceDescriptorBinderImpl
import org.fourthline.cling.model.Namespace
import org.fourthline.cling.model.ValidationException
import org.fourthline.cling.model.XMLUtil
import org.fourthline.cling.model.XMLUtil.appendNewElementIfNotNull
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.LocalService
import org.fourthline.cling.model.meta.RemoteService
import org.fourthline.cling.model.profile.RemoteClientInfo
import org.fourthline.cling.model.types.InvalidValueException
import org.fourthline.cling.model.types.ServiceId
import org.fourthline.cling.model.types.ServiceType
import org.fourthline.cling.model.types.UDN
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.net.URI

class CanonDeviceDescriptorBinderImpl : UDA10DeviceDescriptorBinderImpl() {
    private val TAG: String = "CanonDevDescBinderImpl"

    @Throws(
        DescriptorBindingException::class,
        ValidationException::class
    )

    override fun generateRoot(
        namespace: Namespace?,
        deviceModel: Device<*, *, *>?,
        descriptor: Document,
        info: RemoteClientInfo?
    ) {
        val rootElement = descriptor.createElementNS(
            Descriptor.Device.NAMESPACE_URI,
            ELEMENT.root.toString()
        )
        descriptor.appendChild(rootElement)
        generateSpecVersion(namespace, deviceModel, descriptor, rootElement)

        //UDA 1.1 spec says: Don't use URLBase anymore BUT Canon uses UDA1.0!!
        //if (deviceModel.getBaseURL() != null) {
        appendNewElementIfNotNull(
            descriptor,
            rootElement,
            "URLBase",
            "http://$hostAddress:$hostPort/"
        )
        //}
        generateDevice(namespace, deviceModel, descriptor, rootElement, info)
    }

    override fun generateServiceList(
        namespace: Namespace,
        deviceModel: Device<*, *, *>,
        descriptor: Document?,
        deviceElement: Element?
    ) {
        if (!deviceModel.hasServices()) return
        val serviceListElement = XMLUtil.appendNewElement(descriptor, deviceElement, ELEMENT.serviceList)
        for (service in deviceModel.services) {

            val serviceElement = XMLUtil.appendNewElement(descriptor, serviceListElement, ELEMENT.service)
            appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.serviceType, service.serviceType)
            appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.serviceId, service.serviceId)

            if (service is RemoteService) {
                appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.SCPDURL, service.descriptorURI)
                appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.controlURL, service.controlURI)
                appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.eventSubURL, service.eventSubscriptionURI)
            } else if (service is LocalService<*>) {
                appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.SCPDURL, namespace.getDescriptorPath(
                    service
                ))
                appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.controlURL, namespace.getControlPath(
                    service
                ))
                appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.eventSubURL, namespace.getEventSubscriptionPath(
                    service
                ))
                //if we're providing the CameraConnectedMobile service we need to provide imink too
                if (service.serviceType == CCM_SERVICE_TYPE){
                    Log.d(TAG,"found CCM local service to append the imink tags on")
                    //append imink tags
                    appendNewElementIfNotNull(descriptor, serviceElement, "X_SCPDURL",
                        namespace.getDescriptorPath(service), IMINK_NAMESPACE)
                    appendNewElementIfNotNull(descriptor, serviceElement, "X_ExtActionVer",
                        "1.0", IMINK_NAMESPACE)
                    appendNewElementIfNotNull(descriptor, serviceElement, "X_VendorExtVer",
                        "1-1502.0.0.0", IMINK_NAMESPACE)
                }
            }
        }
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
            if (ELEMENT.service.equals(
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
                            ELEMENT.serviceType.equals(serviceChild) -> {
                                service.serviceType =
                                    ServiceType.valueOf(XMLUtil.getTextContent(serviceChild))
                            }
                            ELEMENT.serviceId.equals(serviceChild) -> {
                                service.serviceId = ServiceId.valueOf(
                                    XMLUtil.getTextContent(serviceChild)
                                )
                            }
                            ELEMENT.SCPDURL.equals(serviceChild) -> {
                                service.descriptorURI = parseURI(
                                    XMLUtil.getTextContent(serviceChild)
                                )
                            }
                            ELEMENT.controlURL.equals(serviceChild) -> {
                                service.controlURI = parseURI(XMLUtil.getTextContent(serviceChild))
                            }
                            ELEMENT.eventSubURL.equals(serviceChild) -> {
                                service.eventSubscriptionURI =
                                    parseURI(XMLUtil.getTextContent(serviceChild))
                            }
                        }
                        if (serviceChild.localName == "X_SCPDURL" && serviceChild.namespaceURI == "urn:schemas-canon-com:schema-imink") {
                            iminkSCPDURL = parseURI("${XMLUtil.getTextContent(serviceChild)}?uuid=$UDN_STRING")
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
        //
        /*val nodes: NodeList =
            rootElement?.getElementsByTagName("serviceList")!!.item(0).childNodes.item(1).childNodes*/
        super.hydrateRoot(descriptor, rootElement)
    }
}
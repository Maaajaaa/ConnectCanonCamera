package com.github.sparkycola.connectcanoncamera

import android.util.Log
import org.fourthline.cling.binding.xml.DescriptorBindingException
import org.fourthline.cling.binding.xml.UDA10DeviceDescriptorBinderImpl
import org.fourthline.cling.model.Namespace
import org.fourthline.cling.model.ValidationException
import org.fourthline.cling.model.XMLUtil
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.model.profile.RemoteClientInfo
import org.seamless.util.Exceptions
import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory


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
                descriptorXml = descriptorXml?.replace("</service>","<ns:X_SCPDURL xmlns:ns=\"urn:schemas-canon-com:schema-imink\">desc_iml/CameraConnectedMobile.xml</ns:X_SCPDURL> \n" +
                        "<ns:X_ExtActionVer xmlns:ns=\"urn:schemas-canon-com:schema-imink\">1.0</ns:X_ExtActionVer> \n" +
                        "<ns:X_VendorExtVer xmlns:ns=\"urn:schemas-canon-com:schema-imink\">1-1502.0.0.0</ns:X_VendorExtVer></service>")
                Log.d(TAG,"descriptorXML: $descriptorXml")
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
    /*
    @Throws(ValidationException::class)
    override fun <D : Device<*, *, *>?> buildInstance(undescribedDevice: D, descriptor: MutableDevice): D {
        var device: D = descriptor.build(undescribedDevice) as D
        Log.d(TAG,"device${device}")
        return device
    }

    @Throws(DescriptorBindingException::class)
    override fun hydrateRoot(
        descriptor: MutableDevice,
        rootElement: Element
    ) {
        Log.d(TAG, "hydrating root")
        Log.d(TAG, "root device: ${descriptor.toString()}")
        super.hydrateRoot(descriptor, rootElement)
    }
*/

    override fun generate(
        deviceModel: Device<*, out Device<*, *, *>, out Service<*, *>>?,
        info: RemoteClientInfo?,
        namespace: Namespace?
    ): String {
        var orignalXMLString = super.generate(deviceModel, info, namespace)
        var XMLWithImink = orignalXMLString.replace("</service>","<ns:X_SCPDURL xmlns:ns=\"urn:schemas-canon-com:schema-imink\">desc_iml/CameraConnectedMobile.xml</ns:X_SCPDURL> \n" +
                "<ns:X_ExtActionVer xmlns:ns=\"urn:schemas-canon-com:schema-imink\">1.0</ns:X_ExtActionVer> \n" +
                "<ns:X_VendorExtVer xmlns:ns=\"urn:schemas-canon-com:schema-imink\">1-1502.0.0.0</ns:X_VendorExtVer></service>")

        return "<?xml version=\"1.0\"?> \n" +
                "<root xmlns=\"urn:schemas-upnp-org:device-1-0\"> \n" +
                "<specVersion> \n" +
                "<major>1</major> \n" +
                "<minor>0</minor> \n" +
                "</specVersion> \n" +
                "<URLBase>http://10.42.0.129:49152/</URLBase> \n" +
                "<device> \n" +
                "<deviceType>urn:schemas-upnp-org:device:Basic:1</deviceType> \n" +
                "<friendlyName>Redmi Note 8</friendlyName> \n" +
                "<manufacturer>CANON INC.</manufacturer> \n" +
                "<manufacturerURL>http://www.canon.com/</manufacturerURL> \n" +
                "<modelDescription>Canon Mobile Simulator</modelDescription> \n" +
                "<modelName>Android 9/Redmi Note 8</modelName> \n" +
                "<UDN>uuid:2188B849-F71E-4B2D-AAF3-EE57761A9975</UDN> \n" +
                "<serviceList> \n" +
                "<service> \n" +
                "<serviceType>urn:schemas-canon-com:service:CameraConnectedMobileService:1</serviceType> \n" +
                "<serviceId>urn:schemas-canon-com:serviceId:CameraConnectedMobile</serviceId> \n" +
                "<SCPDURL>desc/CameraConnectedMobile.xml</SCPDURL> \n" +
                "<controlURL>CameraConnectedMobile/</controlURL> \n" +
                "<eventSubURL> </eventSubURL> \n" +
                "<ns:X_SCPDURL xmlns:ns=\"urn:schemas-canon-com:schema-imink\">desc_iml/CameraConnectedMobile.xml</ns:X_SCPDURL> \n" +
                "<ns:X_ExtActionVer xmlns:ns=\"urn:schemas-canon-com:schema-imink\">1.0</ns:X_ExtActionVer> \n" +
                "<ns:X_VendorExtVer xmlns:ns=\"urn:schemas-canon-com:schema-imink\">1-1502.0.0.0</ns:X_VendorExtVer> \n" +
                "</service> \n" +
                "</serviceList> \n" +
                "<presentationURL>/</presentationURL> \n" +
                "</device> \n" +
                "</root> "
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
            val dAsString: String = XMLUtil.documentToString(d)
            Log.d(TAG, "Dom built: $dAsString")
            d
        } catch (ex: Exception) {
            throw DescriptorBindingException(
                "Could not generate device descriptor: " + ex.message,
                ex
            )
        }
    }
}
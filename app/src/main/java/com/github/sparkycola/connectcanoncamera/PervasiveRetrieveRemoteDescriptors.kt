package com.github.sparkycola.connectcanoncamera

import android.util.Log
import org.fourthline.cling.UpnpService
import org.fourthline.cling.binding.xml.DescriptorBindingException
import org.fourthline.cling.model.ValidationException
import org.fourthline.cling.model.message.StreamRequestMessage
import org.fourthline.cling.model.message.UpnpRequest
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.RemoteService
import org.fourthline.cling.protocol.RetrieveRemoteDescriptors
import org.fourthline.cling.transport.RouterException
import java.net.URL

class PervasiveRetrieveRemoteDescriptors(upnpService: UpnpService?, rd: RemoteDevice?) :
    RetrieveRemoteDescriptors(upnpService, rd) {

    private val tag = "PerRetRemDesc"
    @Throws(
        RouterException::class,
        DescriptorBindingException::class,
        ValidationException::class
    )
    override fun describeService(service: RemoteService): RemoteService? {
        val descriptorURL: URL = try {
            service.device.normalizeURI(service.descriptorURI)
        } catch (e: IllegalArgumentException) {
            Log.w(tag,"Could not normalize service descriptor URL: " + service.descriptorURI)
            return null
        }
        val serviceDescRetrievalMsg =
            StreamRequestMessage(UpnpRequest.Method.GET, descriptorURL)

        // Extra headers
        val headers = upnpService.configuration
            .getDescriptorRetrievalHeaders(service.device.identity)
        if (headers != null) serviceDescRetrievalMsg.headers.putAll(headers)
        Log.d(tag,"Sending service descriptor retrieval message: $serviceDescRetrievalMsg")
        val serviceDescMsg =
            upnpService.router.send(serviceDescRetrievalMsg)
        if (serviceDescMsg == null) {
            Log.w(tag,"Could not retrieve service descriptor, no response: $service")
            return null
        }
        if (serviceDescMsg.operation.isFailed) {
            Log.w(tag,
                "Service descriptor retrieval failed: "
                        + descriptorURL
                        + ", "
                        + serviceDescMsg.operation.responseDetails
            )
            Log.w(tag,"trying to retrieve service descriptor again")
            //sleep 0.5s to not spam too much
            Thread.sleep(500)
            return describeService(service)
        }
        if (!serviceDescMsg.isContentTypeTextUDA) {
            Log.d(tag,"Received service descriptor without or with invalid Content-Type: $descriptorURL")
            // We continue despite the invalid UPnP message because we can still hope to convert the content
        }
        val descriptorContent = serviceDescMsg.bodyString
        if (descriptorContent == null || descriptorContent.length == 0) {
            Log.w(tag,"Received empty service descriptor:$descriptorURL")
            return null
        }
        Log.d(tag,"Received service descriptor, hydrating service model: $serviceDescMsg")
        val serviceDescriptorBinder =
            upnpService.configuration.serviceDescriptorBinderUDA10
        return serviceDescriptorBinder.describe(service, descriptorContent)
    }
}
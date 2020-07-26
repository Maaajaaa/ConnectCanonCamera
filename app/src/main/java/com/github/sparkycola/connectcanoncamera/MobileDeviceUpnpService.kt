package com.github.sparkycola.connectcanoncamera

import android.util.Log
import org.fourthline.cling.UpnpService
import org.fourthline.cling.UpnpServiceConfiguration
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration
import org.fourthline.cling.android.AndroidUpnpServiceImpl
import org.fourthline.cling.controlpoint.ControlPoint
import org.fourthline.cling.model.message.UpnpHeaders
import org.fourthline.cling.model.message.header.UpnpHeader
import org.fourthline.cling.model.meta.RemoteDeviceIdentity
import org.fourthline.cling.model.meta.RemoteService
import org.fourthline.cling.model.types.ServiceType
import org.fourthline.cling.model.types.UDAServiceType
import org.fourthline.cling.registry.Registry

class MobileDeviceUpnpService : AndroidUpnpServiceImpl() {
    override fun createConfiguration(): UpnpServiceConfiguration {
        return object : AndroidUpnpServiceConfiguration() {

            override fun getRegistryMaintenanceIntervalMillis(): Int {
                return 7000
            }

            //only search for CanonCameras
            /*override fun getExclusiveServiceTypes(): Array<ServiceType> {
                return arrayOf(
                    UDAServiceType(SERVICE_NAME)
                )
            }*/

            override fun getDescriptorRetrievalHeaders(identity: RemoteDeviceIdentity?): UpnpHeaders {

                val headers: UpnpHeaders = super.getDescriptorRetrievalHeaders(identity)
                Log.d("Headers", "Host: ${headers.get(UpnpHeader.Type.HOST)}")
                return headers
            }
            override fun getEventSubscriptionHeaders(service: RemoteService?): UpnpHeaders {
                val headers: UpnpHeaders = super.getEventSubscriptionHeaders(service)
                Log.d("Headers", "Host: ${headers.get(UpnpHeader.Type.HOST)}")
                return headers
            }

        }
    }


}
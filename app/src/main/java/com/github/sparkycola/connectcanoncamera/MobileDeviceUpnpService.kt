package com.github.sparkycola.connectcanoncamera

import org.fourthline.cling.UpnpServiceConfiguration
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration
import org.fourthline.cling.android.AndroidUpnpServiceImpl
import org.fourthline.cling.binding.xml.DeviceDescriptorBinder
import org.fourthline.cling.binding.xml.ServiceDescriptorBinder
import org.fourthline.cling.binding.xml.UDA10ServiceDescriptorBinderImpl


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

            override fun getDeviceDescriptorBinderUDA10(): DeviceDescriptorBinder? {
                // Recommended for best interoperability with broken UPnP stacks!
                return CanonDeviceDescriptorBinderImpl()
            }
            override fun getServiceDescriptorBinderUDA10(): ServiceDescriptorBinder? {
                return UDA10ServiceDescriptorBinderImpl()
            }


        }
    }


}
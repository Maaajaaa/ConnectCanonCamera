package com.github.sparkycola.connectcanoncamera.ui.main

import org.fourthline.cling.binding.annotations.*


@UpnpService(
    serviceId = UpnpServiceId("CameraConnectedMobile"),
    serviceType = UpnpServiceType(value = "CameraConnectedMobile", version = 1)
)
class CameraConnectedMobileService {
    @UpnpStateVariable(defaultValue = "0", sendEvents = false)
    private var target = false

    @UpnpStateVariable(defaultValue = "0")
    private var status = false

    @UpnpAction
    fun setTarget(@UpnpInputArgument(name = "NewTargetValue") newTargetValue: Boolean) {
        target = newTargetValue
        status = newTargetValue
        println("Switch is: $status")
    }

    @UpnpAction(out = [UpnpOutputArgument(name = "RetTargetValue")])
    fun getTarget(): Boolean {
        return target
    }

    @UpnpAction(out = [UpnpOutputArgument(name = "ResultStatus")])
    fun getStatus(): Boolean {
        // If you want to pass extra UPnP information on error:
        // throw new ActionException(ErrorCode.ACTION_NOT_AUTHORIZED);
        return status
    }
}
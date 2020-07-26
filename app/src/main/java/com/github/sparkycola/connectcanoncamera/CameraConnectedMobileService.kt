package com.github.sparkycola.connectcanoncamera

import org.fourthline.cling.binding.annotations.*
import org.fourthline.cling.model.action.ActionException
import org.fourthline.cling.model.types.ErrorCode


@UpnpService(
    serviceId = UpnpServiceId(SERVICE_NAME),
    serviceType = UpnpServiceType(value = SERVICE_NAME, version = 1)
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
    }

    @UpnpAction(out = [UpnpOutputArgument(name = "RetTargetValue")])
    fun getTarget(): Boolean {
        return target
    }

    @UpnpAction(out = [UpnpOutputArgument(name = "ResultStatus")])
    fun getStatus(): Boolean {
        // If you want to pass extra UPnP information on error:
        throw ActionException(ErrorCode.ACTION_NOT_AUTHORIZED);
        return status
    }
}
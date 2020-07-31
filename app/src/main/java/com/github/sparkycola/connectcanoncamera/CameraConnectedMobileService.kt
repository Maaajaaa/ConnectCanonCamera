package com.github.sparkycola.connectcanoncamera

import android.util.Log
import org.fourthline.cling.binding.annotations.*
import org.fourthline.cling.model.action.ActionException
import org.fourthline.cling.model.profile.RemoteClientInfo
import org.fourthline.cling.model.types.ErrorCode


@UpnpService(
    serviceId = UpnpServiceId(namespace = CANON_NAMESPACE, value = CCM_SERVICE_ID),
    serviceType = UpnpServiceType(namespace = CANON_NAMESPACE, value = CCM_SERVICE, version = 1)
)
class CameraConnectedMobileService {
    @UpnpStateVariable(defaultValue = "0", sendEvents = false)
    private var target = false

    @UpnpStateVariable(defaultValue = "0")
    private var status = false

    @UpnpAction
    fun setTarget(
        @UpnpInputArgument(name = "NewTargetValue") newTargetValue: Boolean,
        clientInfo: RemoteClientInfo?
    ) {
        if (clientInfo != null) {
            Log.d(
                "CCMService",
                "Client's address is: " + clientInfo.remoteAddress
            )
            Log.d(
                "CCMService",
                "Received message on: " + clientInfo.localAddress
            )
            Log.d(
                "CCMService",
                "Client's user agent is: " + clientInfo.requestUserAgent
            )
            Log.d(
                "CCMService",
                "Client's custom header is: " +
                        clientInfo.requestHeaders.toString()
            )
        }
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
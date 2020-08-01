package com.github.sparkycola.connectcanoncamera

import org.fourthline.cling.binding.annotations.*
import org.fourthline.cling.model.profile.RemoteClientInfo


@UpnpService(
    serviceId = UpnpServiceId(namespace = CANON_NAMESPACE, value = CCM_SERVICE_ID),
    serviceType = UpnpServiceType(namespace = CANON_NAMESPACE, value = CCM_SERVICE, version = 1)
)
class CameraConnectedMobileService {

    //usecaseStatus indicates whether the device is running or stopped (to be shutdown)
    @UpnpStateVariable(defaultValue = "")
    private var usecaseStatus = ""

    @UpnpStateVariable(defaultValue = "")
    private var objRecvCapability = ""

    @UpnpStateVariable(defaultValue = "")
    private var objInfo = ""

    @UpnpStateVariable(defaultValue = "")
    private var objData = ""

    @UpnpStateVariable(defaultValue = "")
    private var movieExtProperty = ""


    @UpnpAction(out = [UpnpOutputArgument(name = "ObjRecvCapability")])
    fun getObjRecvCapability(): String {
        return objRecvCapability
    }

    @UpnpAction
    fun setUsecaseStatus(
        @UpnpInputArgument(name = "UsecaseStatus") newUsecaseStatus: String,
        clientInfo: RemoteClientInfo?
    ) {

    }

    @UpnpAction
    fun setSendObjInfo(
        @UpnpInputArgument(name = "ObjInfo") newUsecaseStatus: String,
        clientInfo: RemoteClientInfo?
    ) {

    }

    @UpnpAction
    fun setObjData(
        @UpnpInputArgument(name = "ObjData") newUsecaseStatus: String,
        clientInfo: RemoteClientInfo?
    ) {

    }

    @UpnpAction
    fun setMovieExtProperty(
        @UpnpInputArgument(name = "MovieExtProperty") newUsecaseStatus: String,
        clientInfo: RemoteClientInfo?
    ) {

    }
}
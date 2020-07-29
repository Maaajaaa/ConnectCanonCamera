package com.github.sparkycola.connectcanoncamera.libimink

interface Device {
    //actions a remote device supports/local device can handle
    val supportedActions: ActionSet

    fun supports(action: KnownAction): Boolean{
        return supportedActions.contains(action)
    }


}
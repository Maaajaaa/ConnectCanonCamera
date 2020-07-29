package com.github.sparkycola.connectcanoncamera.libimink

interface Device {
    //actions a remote device supports/local device can handle
    val supportedActions: IminkActionSet

    fun supports(iminkAction: KnownIminkAction): Boolean{
        return supportedActions.contains(iminkAction)
    }


}
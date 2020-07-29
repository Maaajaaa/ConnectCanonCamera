package com.github.sparkycola.connectcanoncamera.libimink

enum class KnownAction(val kind: Kind, val resourceName: String) {
    //found in G7X's DeviceDescription XML, no additions from Canon's Camera Connect Android app v2.6.30.21
    SetUsecaseStatus(Kind.Set,"UsecaseStatus"),
    GetObjRecvCapability(Kind.Get,"ObjRecvCapability"),
    SetSendObjInfo(Kind.Set,"SendObjInfo"),
    SetMovieExtProperty(Kind.Set,"MovieExtProperty"),
    SetObjData(Kind.Set,"ObjData"),
    GetResizeProperty(Kind.Get,"ResizeProperty"),
    GetObjProperty(Kind.Get,"ObjProperty"),
    GetMovieExtProperty(Kind.Get,"MovieExtProperty"),
    GetObjCount(Kind.Get,"ObjCount"),
    GetObjIDList(Kind.Get,"ObjIDList"),
    GetThumbDataList(Kind.Get,"ThumbDataList"),
    GetObjData(Kind.Get,"ObjData"),
    GetGPSTime(Kind.Get,"GPSTime"),
    GetGPSCaptureTimeList(Kind.Get,"GPSCaptureTimeList"),
    GetGPSListRecvCapability(Kind.Get,"GPSListRecvCapability"),
    SetGPSList(Kind.Set,"GPSList"),
    SetGPSClearList(Kind.Set,"GPSClearList"),
    SetDisconnectStatus(Kind.Set,"DisconnectStatus"),
    GetGroupedObjIDList(Kind.Get,"GroupedObjIDList")
}

enum class Kind {
    Get,Set
}
package com.github.sparkycola.connectcanoncamera.libimink

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

class IminkHTTPD : NanoHTTPD(8615) {
    private val tag = "IminkHTTPD"
    override fun serve(session: IHTTPSession): Response {

        val files: Map<String, String>  = HashMap<String, String>();
        val method = session.method
        if(Method.POST == method){
            Log.d(tag, "received POST request")
            try {
                session.parseBody(files);
            } catch (ioe: IOException) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.message);
            } catch (re: ResponseException) {
                return newFixedLengthResponse(re.status, MIME_PLAINTEXT, re.message);
            }
            Log.d(tag, "received POSTbody: ${session.queryParameterString}")
            Log.d(tag, "requested URI: ${session.uri}")
            val nameParam: String? = session.parameters["Name"].toString()
            if(nameParam != null) {
                Log.d(tag,"requested Name parameter; $nameParam")
            }
        }

        when(session.uri.replace("/MobileConnectedCamera/","")){
            "UsecaseStatus" -> {
                //files.keys.forEach {Log.d(tag, "keyInList: $it value: ${files[it]}")}
                if(files["postData"]?.contains("<Status>Run</Status>")!!){
                    return newFixedLengthResponse(Response.Status.OK,"text/xml","<?xml version=\"1.0\"?>\n" +
                            "<ResultSet xmlns=\"urn:schemas-canon-com:service:CameraConnectedMobileService:1\">\n" +
                            "  <Status>Run</Status>\n" +
                            "  <MajorVersion>0</MajorVersion>\n" +
                            "  <MinorVersion>1</MinorVersion>\n" +
                            "</ResultSet>")
                }
                if(files["postData"]?.contains("<Status>Stop</Status>")!!) {
                    Log.d(tag, "WE'RE IN")
                    return newFixedLengthResponse(Response.Status.OK,"text/xml","<?xml version=\"1.0\"?>\n" +
                            "<ResultSet xmlns=\"urn:schemas-canon-com:service:CameraConnectedMobileService:1\">\n" +
                            "  <Status>Stop</Status>\n" +
                            "  <MajorVersion>0</MajorVersion>\n" +
                            "  <MinorVersion>1</MinorVersion>\n" +
                            "</ResultSet>")
                }
                Log.e(tag, "undefined UsecaseStatus")

            }
            "NFCData", "CameraInfo", "CapabilityInfo"->{
                //just acknowledge those for now
                //Todo: parse details and use them for whatever
                return newFixedLengthResponse(null)
            }
            else -> {
                Log.d(tag, "unkown request uri: ${session.queryParameterString.replace("/MobileConnectedCamera/","")}")

            }
        }
        //else that also catches non-defined case of other UseCaseStatus
        return newFixedLengthResponse("")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                IminkHTTPD()
            } catch (ioe: IOException) {
                System.err.println("Couldn't start server:\n$ioe")
            }
        }
    }

    init {
        start(SOCKET_READ_TIMEOUT, false)
    }
}
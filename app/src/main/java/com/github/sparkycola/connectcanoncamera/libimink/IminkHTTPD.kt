package com.github.sparkycola.connectcanoncamera.libimink

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import android.os.Handler
import android.os.Message
import com.github.sparkycola.connectcanoncamera.IMINK_PORT
import com.github.sparkycola.connectcanoncamera.iminkIsReady

class IminkHTTPD(private val handler: Handler) : NanoHTTPD(IMINK_PORT) {
    private val tag = "IminkHTTPD"
    private var connectedToCamera = false
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
                    connectedToCamera = true
                    //give the response some time to get through
                    handler.sendEmptyMessageDelayed(iminkIsReady,200)
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

    init {
        start(SOCKET_READ_TIMEOUT, false)
    }
}
package com.github.sparkycola.connectcanoncamera.libimink

import android.util.Log
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class IminkController: HttpServlet() {
    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
        Log.d("IminkController", "got POST request: ${req?.requestURL}")
        when{
        }
    }
}
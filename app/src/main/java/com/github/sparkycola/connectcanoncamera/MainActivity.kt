/*
 * GPLv3 2020 SparkyCola
 *
 * ...
 */

package com.github.sparkycola.connectcanoncamera

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.github.sparkycola.connectcanoncamera.libimink.IminkHTTPD
import com.github.sparkycola.connectcanoncamera.ui.main.SectionsPagerAdapter
import com.google.android.material.tabs.TabLayout
import org.fourthline.cling.UpnpService
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.binding.LocalServiceBindingException
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder
import org.fourthline.cling.model.DefaultServiceManager
import org.fourthline.cling.model.ValidationException
import org.fourthline.cling.model.message.header.ServiceTypeHeader
import org.fourthline.cling.model.meta.*
import org.fourthline.cling.model.types.DeviceType
import org.fourthline.cling.model.types.ServiceType
import org.fourthline.cling.model.types.UDADeviceType
import org.fourthline.cling.model.types.UDN
import org.fourthline.cling.protocol.RetrieveRemoteDescriptors
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import java.net.ServerSocket
import java.net.URI
import java.net.URL
import java.util.*

//MobileConnectedCamera refers to the camera, a mobile-connected camera
const val MCC_SERVICE: String = "MobileConnectedCameraService"

//CameraConnectedMobile refers to the mobile(phone), a camera-connected mobile
const val CCM_SERVICE: String = "CameraConnectedMobileService"
const val CCM_SERVICE_ID: String = "CameraConnectedMobile"
const val CANON_NAMESPACE: String = "schemas-canon-com"
const val IMINK_NAMESPACE: String = "urn:schemas-canon-com:schema-imink"

val CCM_SERVICE_TYPE: ServiceType = ServiceType(CANON_NAMESPACE, CCM_SERVICE, 1)
val MCC_SERVICE_TYPE: ServiceType = ServiceType(CANON_NAMESPACE, MCC_SERVICE, 1)

val iminkIsReady: Int = 400

//port for IMINK
const val IMINK_PORT = 8615

//legacy service, probably for older EOS cameras
//const val ICPO_SERVICE: String = "ICPO-SmartPhoneEOSSystemService"

//the interval in which the notify and search requests are sent
const val NOTIFY_INTERVAL: Int = 10
var hostPort: Int = 0
var hostAddress = ""

const val UDN_STRING = "2188B849-F71E-4B2D-AAF3-EE57761A9975"

var cameraBaseURL: URL? = null
var cameraControlURI: URI? = null

class MainActivity : AppCompatActivity() {
    private var upnpService: AndroidUpnpService? = null
    private lateinit var serverSocket: ServerSocket

    // TODO: Generate and store
    private val udn: UDN = UDN(UUID.fromString("2188B849-F71E-4B2D-AAF3-EE57761A9975"))
    private val TAG = "MainActivity"
    private val cameraRegistryListener: CameraRegistryListener = CameraRegistryListener()

    private lateinit var iminkHTTPD: IminkHTTPD
    private lateinit var queue : RequestQueue


    private val serviceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {

            upnpService = service as AndroidUpnpService
            var cameraConnectService = getCameraConnectService()

            // Get ready for future device advertisements
            upnpService!!.registry.addListener(cameraRegistryListener)

            // Now add all devices to the list we already know about
            for (device in upnpService!!.registry.devices) {
                cameraRegistryListener.deviceAdded(device)
            }

            // Register the device when this activity binds to the service for the first time
            if (cameraConnectService == null) {
                try {
                    val cameraConnectDevice: LocalDevice? = createDevice()
                    Log.v(TAG, "Registering CameraConnectDevice")
                    upnpService!!.registry.addDevice(cameraConnectDevice)
                    //cameraConnectService = getCameraConnectService()
                } catch (ex: Exception) {
                    Log.w(TAG, "Creating CameraConnectDevice device failed $ex")
                    return
                }
            }

            val activeStreamServers = upnpService!!.get().router.getActiveStreamServers(null)
            for (streamServer in activeStreamServers) {
                Log.d(TAG, "stream server at: ${streamServer.address}:${streamServer.port}")
                hostPort = streamServer.port
                hostAddress = streamServer.address.hostAddress
            }
            //find camera(s)
            upnpService!!.controlPoint.search(ServiceTypeHeader(MCC_SERVICE_TYPE), NOTIFY_INTERVAL)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            upnpService = null
        }
    }

    public fun refreshDeviceConfiguration(rd: RemoteDevice) {
        upnpService?.configuration?.asyncProtocolExecutor?.execute(
            RetrieveRemoteDescriptors(
                upnpService as UpnpService,
                rd
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
        (tabs.getChildAt(0) as ViewGroup).getChildAt(1).isEnabled = false

        //bind upnp service
        applicationContext.bindService(
            Intent(this, MobileDeviceUpnpServiceImpl::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        //Todo: make proper handler that is not leaking
        @SuppressLint("HandlerLeak")
        val mHandler: Handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    iminkIsReady -> startObjectPullMode()
                }
                super.handleMessage(msg)
            }
        }
        iminkHTTPD = IminkHTTPD(mHandler)
        queue = Volley.newRequestQueue(this)
    }

    private fun startObjectPullMode() {
        iminkHTTPD.stop()
        val statusRunString = "<?xml version=\"1.0\"?>\n" +
                "<ParamSet xmlns=\"urn:schemas-canon-com:service:MobileConnectedCameraService:1\">\n" +
                "  <Status>Run</Status>\n" +
                "</ParamSet>"
        Log.d(TAG, "We're in like Flinn!")
        //get camera into object pull mode
        //generate URL
        val url = URL(
            cameraBaseURL?.protocol,
            cameraBaseURL?.host,
            IMINK_PORT,
            cameraControlURI.toString() + "UsecaseStatus?Name=ObjectPull&MajorVersion=1&MinorVersion=0"
        ).toString()

        //String Request initialized
        val stringRequest: StringRequest =
            object : StringRequest(Request.Method.POST, url, Response.Listener { response ->

                // Display the first 500 characters of the response string.
                Log.d(TAG, "pull mode Response is: ${response}")
                getObjectList()

            }, Response.ErrorListener { error ->
                Log.d(TAG, "Requesting $url failed: $error")
            }) {
                override fun getBodyContentType(): String {
                    return "text/xml ; charset=utf-8"
                }

                @Throws(AuthFailureError::class)
                override fun getBody(): ByteArray {
                    return statusRunString.toByteArray()
                }
            }
        // Add the request to the RequestQueue.
        queue.add(stringRequest)

    }

    private fun getObjectList() {
        //generate URL
        val url = URL(
            cameraBaseURL?.protocol,
            cameraBaseURL?.host,
            IMINK_PORT,
            cameraControlURI.toString() + "ObjIDList?StartIndex=1&MaxNum=1&ObjType=ALL"
        )
        // Add the request to the RequestQueue.
        queue.add(simpleGetRequest(url))
    }

    private fun simpleGetRequest(url: URL):StringRequest{
        return StringRequest(Request.Method.GET, url.toString(), Response.Listener { response ->

            // Display the first 500 characters of the response string.
            Log.d(TAG, "object list Response is: $response")

        }, Response.ErrorListener { error ->
            Log.d(TAG, "Requesting object list at: $url failed: $error")
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (upnpService != null) {
            upnpService!!.registry.removeListener(cameraRegistryListener)
        }
        // This will stop the UPnP service if nobody else is bound to it
        applicationContext.unbindService(serviceConnection)
    }

    private fun getCameraConnectService(): LocalService<CameraConnectedMobileService?>? {
        if (upnpService == null) return null
        val cameraConnectDevice: LocalDevice? = upnpService!!.registry.getLocalDevice(udn, true)
        return if (cameraConnectDevice == null) {
            null
        } else cameraConnectDevice.findService(
            CCM_SERVICE_TYPE
        ) as LocalService<CameraConnectedMobileService?>?
    }

    @Throws(ValidationException::class, LocalServiceBindingException::class)
    protected fun createDevice(): LocalDevice? {
        val deviceType: DeviceType = UDADeviceType("Basic", 1)
        //Todo: make this non-hardcoded, consider fixing the manufacturer
        val deviceDetails = DeviceDetails(
            "Redmi Note 8",
            ManufacturerDetails("CANON INC.", "http://www.canon.com/"),
            ModelDetails("Android 9/Redmi Note 8", "Canon Mobile Simulator")
        )
        val service = AnnotationLocalServiceBinder().read(CameraConnectedMobileService::class.java)
        service.setManager(
            DefaultServiceManager<CameraConnectedMobileService>(
                service as LocalService<CameraConnectedMobileService>?,
                CameraConnectedMobileService::class.java
            )
        )
        return LocalDevice(
            DeviceIdentity(udn),
            deviceType,
            deviceDetails,
            service
        )
    }

    class CameraRegistryListener : DefaultRegistryListener() {
        /* Discovery performance optimization for very slow Android devices! */
        override fun remoteDeviceDiscoveryStarted(registry: Registry?, device: RemoteDevice?) {
            Log.v("CameraRegistyListener", "(slow mode) Device added: $device")
        }

        override fun remoteDeviceDiscoveryFailed(
            registry: Registry?,
            device: RemoteDevice,
            ex: Exception?
        ) {
            Log.e(
                "CameraRegistryListener",
                "(slow mode) Discovery failed of '" + device.displayString + "': "
                        + (ex?.toString() ?: "Couldn't retrieve device/service descriptors")
            )
        }
        /* End of optimization, you can remove the whole block if your Android handset is fast (>= 600 Mhz) */

        override fun remoteDeviceAdded(registry: Registry?, device: RemoteDevice?) {


            Log.v("CameraRegistyListener", "Device added:  ${device?.details?.friendlyName}")

            Log.v("CaemraRegistryListener", "URL: ${device?.identity?.descriptorURL}")
            Log.v("CaemraRegistryListener", "udn : ${device?.identity?.udn}")
            if (device?.services?.size == 0 && device.details.friendlyName == "G7X") {
                //remove device from registry so we can scan for services again
                //registry?.remoteDevices?.minus(device)
                registry?.removeDevice(device.identity.udn)
                registry?.upnpService?.configuration?.asyncProtocolExecutor?.execute(
                    PervasiveRetrieveRemoteDescriptors(registry.upnpService as UpnpService, device)
                )
            }
            if (device?.services?.size != 0) {
                cameraBaseURL = device?.details?.baseURL
                cameraControlURI = device?.services!![0].controlURI
                Log.v("remote dev added", "service list not null, baseURL is: $cameraBaseURL")
                Log.v("CaemraRegistryListener", "number of services : ${device?.services!!.size}")
                Log.v(
                    "CaemraRegistryListener",
                    "descriptorURI : ${device?.services!![0].descriptorURI}"
                )
                Log.v("CaemraRegistryListener", "controlURI : ${device?.services!![0].controlURI}")
                Log.v(
                    "CaemraRegistryListener",
                    "eventsubURI : ${device?.services!![0].eventSubscriptionURI}"
                )
                Log.v(
                    "CaemraRegistryListener",
                    "nb of actions : ${device?.services!![0].actions.size}"
                )
                Log.v(
                    "CaemraRegistryListener",
                    "nb of state vars : ${device?.services!![0].stateVariables.size}"
                )
            }
        }

        override fun remoteDeviceRemoved(registry: Registry?, device: RemoteDevice?) {
            Log.v("CameraRegistyListener", "Device REMOVED:  ${device?.details?.friendlyName}")
        }

        override fun localDeviceAdded(registry: Registry?, device: LocalDevice?) {
            Log.v("CameraRegistyListener", "local device added: ${device?.details?.friendlyName}")

        }

        override fun localDeviceRemoved(registry: Registry?, device: LocalDevice?) {
            Log.v("CameraRegistyListener", "local device REMOVED: ${device?.details?.friendlyName}")
        }

        fun deviceAdded(device: Device<DeviceIdentity, Device<*, *, *>, Service<*, *>>?) {
            Log.v(
                "CameraRegistyListener",
                "already connected device added:  ${device?.details?.friendlyName}"
            )
        }
    }
}
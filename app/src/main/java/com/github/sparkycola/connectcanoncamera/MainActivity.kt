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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.android.volley.AuthFailureError
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.github.sparkycola.connectcanoncamera.libimink.IminkHTTPD
import com.github.sparkycola.connectcanoncamera.ui.main.GalleryObject
import com.github.sparkycola.connectcanoncamera.ui.main.PageViewModel
import com.github.sparkycola.connectcanoncamera.ui.main.SectionsPagerAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.w3c.dom.Document
import java.io.*
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.*
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.ArrayList

//MobileConnectedCamera refers to the camera, a mobile-connected camera
const val MCC_SERVICE: String = "MobileConnectedCameraService"

//CameraConnectedMobile refers to the mobile(phone), a camera-connected mobile
const val CCM_SERVICE: String = "CameraConnectedMobileService"
const val CCM_SERVICE_ID: String = "CameraConnectedMobile"
const val CANON_NAMESPACE: String = "schemas-canon-com"
const val IMINK_NAMESPACE: String = "urn:schemas-canon-com:schema-imink"

val CCM_SERVICE_TYPE: ServiceType = ServiceType(CANON_NAMESPACE, CCM_SERVICE, 1)
val MCC_SERVICE_TYPE: ServiceType = ServiceType(CANON_NAMESPACE, MCC_SERVICE, 1)

const val iminkIsReady: Int = 400

//port for IMINK
const val IMINK_PORT = 8615


//legacy service, probably for older EOS cameras
//const val ICPO_SERVICE: String = "ICPO-SmartPhoneEOSSystemService"

//the interval (in s( in which the notify and search requests are sent
const val NOTIFY_INTERVAL: Int = 10
var hostPort: Int = 0
var hostAddress = ""

// TODO: Generate and store
const val UDN_STRING = "2188B849-F71E-4B2D-AAF3-EE57761A9975"

var cameraBaseURL: URL? = null
var cameraControlURI: URI? = null

class MainActivity : AppCompatActivity() {
    /*cameraObjects is a list, starting at the oldest object, of dictionaries
        containing objID: unique identifier of each picture given to it by the
        camera, required for loading the EXIF header and downloading the image
        objType: type of picture/video, can be JPEG, CR2, JPEG+CR2 or MP4?
        groupNbr: number of pictures in a group of pictures taken in
        CreativeShot mode, all of them seem to be referenced by the same ID*/

    private lateinit var cameraObjects: MutableMap<Int, List<String?>>
    private lateinit var viewModel: PageViewModel

    private var upnpService: AndroidUpnpService? = null

    private val udn: UDN = UDN(UUID.fromString(UDN_STRING))
    private val TAG = "MainActivity"
    private val cameraRegistryListener: CameraRegistryListener = CameraRegistryListener()

    private lateinit var iminkHTTPD: IminkHTTPD
    private lateinit var queue: RequestQueue

    private lateinit var errorThrowingScope: CoroutineScope

    private val serviceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {

            upnpService = service as AndroidUpnpService
            val cameraConnectService = getCameraConnectService()

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
                viewModel.text.postValue("stream server at: ${streamServer.address}:${streamServer.port}")
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

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
        //(tabs.getChildAt(0) as ViewGroup).getChildAt(1).isEnabled = false

        viewModel = ViewModelProviders.of(this).get(PageViewModel::class.java)

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

        val errorHandler = CoroutineExceptionHandler { context, error ->
            val sw = StringWriter()
            error.printStackTrace(PrintWriter(sw))
            Log.e(TAG, "error in coroutine: $sw")
        }
        errorThrowingScope = CoroutineScope(errorHandler)
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
            object : StringRequest(Method.POST, url, Response.Listener { response ->

                Log.d(TAG, "pull mode Response is: $response")
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

    private fun objectIDURL(startIndex: Int, maxNum: Int, groupType: Int? = null): String {
        return if (groupType == null) {
            URL(
                cameraBaseURL?.protocol,
                cameraBaseURL?.host,
                IMINK_PORT,
                cameraControlURI.toString() + "ObjIDList?StartIndex=$startIndex&MaxNum=$maxNum&ObjType=ALL"
            ).toString()
        } else {
            URL(
                cameraBaseURL?.protocol,
                cameraBaseURL?.host,
                IMINK_PORT,
                cameraControlURI.toString() + "ObjIDList?StartIndex=$startIndex&MaxNum=$maxNum&ObjType=ALL&GroupType=$groupType"
            ).toString()
        }
    }

    private fun getObjectList() {
        val dbFactory =
            DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()

        errorThrowingScope.launch {
            //the retrieval is 1-based
            val doc: Document = dBuilder.parse(objectIDURL(1, 1))
            val totalNumber =
                Integer.valueOf(doc.getElementsByTagName("TotalNum").item(0).textContent)
            val initialObjectID = doc.getElementsByTagName("ObjIDList-1").item(0).textContent
            Log.d(TAG, "Total Number of Elements: $totalNumber")

            //initialize cameraObjects
            cameraObjects = mutableMapOf()

            //retrieve all objectIDs, G7X sends a maximum of 99 Objects per packet, so if we have more pictures we need to iterate longer
            while (cameraObjects.size < totalNumber) {
                // get images starting with the last we added (camera counts 1-based) and as many as we're still missing
                // the camera won't bitch if we request too many but still only send 99 per packet (G7X, other cameras could differ but probably not)
                val url = objectIDURL(cameraObjects.size + 1, totalNumber - cameraObjects.size, 1)
                val doc: Document = dBuilder.parse(url)
                //iterate over the items in the retrieved list, again it's 1-based so the iteration goes 1 to size + 1 instead of the 0 to size for 0-based
                for (listID in 1 until doc.getElementsByTagName("ListCount")
                    .item(0).textContent.toInt() + 1) {
                    val objectID =
                        doc.getElementsByTagName("ObjIDList-$listID").item(0)?.textContent?.toInt()
                    val objType =
                        doc.getElementsByTagName("ObjTypeList-$listID").item(0)?.textContent
                    val groupNum =
                        doc.getElementsByTagName("GroupedNumList-$listID").item(0)?.textContent
                    //assign object ID, group num can be null
                    if (objectID == null || objType.isNullOrEmpty()) {
                        Log.e(
                            TAG,
                            "invalid listID $listID at number ${cameraObjects.size}, totalNumber is: $totalNumber"
                        )
                        Log.e(
                            TAG,
                            "CHECK FOR FAILS: objectID: $objectID\tobjectType: $objType\tgroupNum: $groupNum"
                        )
                    } else {
                        cameraObjects[objectID] = listOf(objType, groupNum)
                        Log.d(TAG, "objectID: $objectID\tobjectType: $objType\tgroupNum: $groupNum")
                    }
                }
            }
            Log.d(TAG, "got ${cameraObjects.size} objects")
            viewModel.itemLength.postValue(cameraObjects.size)
            //maps keep the order of objects but don't provide a key that is starting at 0 for the first so we need to track it manually
            var orderKey = 0
            cameraObjects.forEach {
                //get thumb
                Log.d(TAG, "getting header bytes of: ${it.key}")
                val headerBytes = getHeaderBytesOfObject(it.key)
                if (headerBytes != null) {
                    Log.d(TAG, "posting thumb${it.key} / orderKey: $orderKey")
                    //process and displayImage with type as description and order key as ID
                    extractAndPostThumb(headerBytes, "${it.value[0]}", orderKey)
                } else {
                    Log.e(TAG, "getting thumb ${it.key} returned null")
                }
                orderKey++
            }
        }
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

    private fun getHeaderBytesOfObject(objectID: Int): ByteArray? {
        //generate URL
        val thumbUrl = URL(
            cameraBaseURL?.protocol,
            cameraBaseURL?.host,
            IMINK_PORT,
            cameraControlURI.toString() + "ObjParsingExifHeaderList?ListNum=1&ObjIDList-1=$objectID"
        )
        return getHeaderBytesFromURL(thumbUrl)
    }

    private fun getHeaderBytesFromURL(src: URL): ByteArray? {
        val connection: HttpURLConnection = src
            .openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        return connection.inputStream.readBytes()
    }

    private fun extractAndPostThumb(headerBytes: ByteArray, description: String, imageId: Int) {
        val ds = ByteArrayDataSource(headerBytes, "multipart/mixed")
        val multipart = MimeMultipart(ds)
        for (i in 0 until multipart.count) {
            if (multipart.getBodyPart(i).contentType == "application/octet-stream ;Object-ID=objid1") {
                val bytes = multipart.getBodyPart(i).content as ByteArrayInputStream
                val byteArray = bytes.readBytes()
                val ffIndexes = ArrayList<Int>()
                val f9Indexes = ArrayList<Int>()
                //Todo: streamline this parsing, many optimisations should be possible
                byteArray.forEachIndexed { index, byte ->
                    //last 8 bytes need to be FF D8 FF DB (something) and FF D9
                    //find possible jpeg header
                    //Todo: the possible headers can be found much faster by reading only every 7th byte
                    if (index + 8 < byteArray.size - 5 && byte == 0xFF.toByte() && byteArray[index + 1] == 0xD8.toByte() && byteArray[index + 2] == 0xFF.toByte() && byteArray[index + 3] == 0xDB.toByte()) {
                        ffIndexes.add(index)
                    }
                    //Todo: this can be found quicker in reverse, we only need the last
                    //find possible jpeg file ending
                    if (index + 1 < byteArray.size && byte == 0xFF.toByte() && byteArray[index + 1] == 0xD9.toByte()) {
                        f9Indexes.add(index)
                    }
                }
                var parsedImage: ByteArray? = null
                ffIndexes.forEach {
                    if (f9Indexes.last() - it + 3 > 0) {
                        parsedImage =
                            byteArray.sliceArray(IntRange(it, f9Indexes.last() + 1))
                        viewModel.galleryObject.postValue(GalleryObject(BitmapFactory.decodeByteArray(parsedImage,0,
                            parsedImage!!.size),description, imageId))
                    }
                }
                if(parsedImage == null){
                    Log.w(TAG, "unable to parse headerBytes, saving to Downloads Folder")
                    ffIndexes.forEach { Log.e(TAG, "ff d8 ff db (jpeg magic number) found at: $it") }
                    f9Indexes.forEach { Log.e(TAG, "ff d9 (jpeg file end) found at: $it") }
                    //Todo: fix this to a method that is not deprecated
                    //saving for debugging of failures without the need to grab the bytes with a network analyzer
                    try {
                        FileOutputStream(
                            File(
                                Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS
                                ), "ConnectCanonCamera_failedHeaderBytes.dat"
                            )
                        ).write(headerBytes)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

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
                Log.v("CaemraRegistryListener", "number of services : ${device.services!!.size}")
                Log.v(
                    "CaemraRegistryListener",
                    "descriptorURI : ${device.services!![0].descriptorURI}"
                )
                Log.v("CaemraRegistryListener", "controlURI : ${device.services!![0].controlURI}")
                Log.v(
                    "CaemraRegistryListener",
                    "eventsubURI : ${device.services!![0].eventSubscriptionURI}"
                )
                Log.v(
                    "CaemraRegistryListener",
                    "nb of actions : ${device.services!![0].actions.size}"
                )
                Log.v(
                    "CaemraRegistryListener",
                    "nb of state vars : ${device.services!![0].stateVariables.size}"
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
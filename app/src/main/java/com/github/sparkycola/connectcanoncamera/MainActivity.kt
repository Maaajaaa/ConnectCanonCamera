/*
 * GPLv3 2020 SparkyCola
 *
 * ...
 */

package com.github.sparkycola.connectcanoncamera

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.github.sparkycola.connectcanoncamera.ui.main.SectionsPagerAdapter
import com.google.android.material.tabs.TabLayout
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.binding.LocalServiceBindingException
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder
import org.fourthline.cling.model.DefaultServiceManager
import org.fourthline.cling.model.ValidationException
import org.fourthline.cling.model.message.UpnpHeaders
import org.fourthline.cling.model.message.header.STAllHeader
import org.fourthline.cling.model.message.header.UDADeviceTypeHeader
import org.fourthline.cling.model.message.header.UpnpHeader
import org.fourthline.cling.model.meta.*
import org.fourthline.cling.model.types.DeviceType
import org.fourthline.cling.model.types.UDADeviceType
import org.fourthline.cling.model.types.UDAServiceType
import org.fourthline.cling.model.types.UDN
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import java.util.*

public const val SERVICE_NAME: String = "MobileConnectedCameraService"
public const val LEGACY_SERVICE_NAME: String = "ICPO-SmartPhoneEOSSystemService"

class MainActivity : AppCompatActivity() {

    private var upnpService: AndroidUpnpService? = null
    // TODO: Generate and store
    private val udn: UDN = UDN(UUID.fromString("2188B849-F71E-4B2D-AAF3-EE57761A9975"))
    private val tag = "MainActivity"
    private val cameraRegistryListener: CameraRegistryListener = CameraRegistryListener()


    private val serviceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {

            upnpService = service as AndroidUpnpService
            var cameraConnectService: LocalService<CameraConnectedMobileService?>? = getCameraConnectService()

            // Get ready for future device advertisements
            upnpService!!.registry.addListener(cameraRegistryListener)

            // Now add all devices to the list we already know about

            // Now add all devices to the list we already know about
            for (device in upnpService!!.registry.devices) {
                cameraRegistryListener.deviceAdded(device)
            }

            // Register the device when this activity binds to the service for the first time
            if (cameraConnectService == null) {
                try {
                    val cameraConnectDevice: LocalDevice? = createDevice()
                    Log.v(tag,"Registering CameraConnectDevice")
                    upnpService!!.registry.addDevice(cameraConnectDevice)
                    cameraConnectService = getCameraConnectService()
                } catch (ex: Exception) {
                    Log.w(tag,"Creating CameraConnectDevice device failed $ex")
                    return
                }
            }

            //find camera(s)
            val headerValue = "urn:schemas-canon-com:service:MobileConnectedCameraService:1"
            upnpService!!.controlPoint.search(UpnpHeader.newInstance(UpnpHeader.Type.ST,
                headerValue))
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
        (tabs.getChildAt(0) as ViewGroup).getChildAt(1).isEnabled = false

        //bind upnp service
        applicationContext.bindService(
            Intent(this, MobileDeviceUpnpService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )

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
        var cameraConnectDevice: LocalDevice? = upnpService!!.registry.getLocalDevice(udn, true)
        return if (cameraConnectDevice == null)
                /*.also { cameraConnectDevice = it }*/
         {
            null
        } else cameraConnectDevice.findService(
            UDAServiceType(
                "CameraConnectedMobileService",
                1
            )
        ) as LocalService<CameraConnectedMobileService?>?
    }

    @Throws(ValidationException::class, LocalServiceBindingException::class)
    protected fun createDevice(): LocalDevice? {
        val type: DeviceType = UDADeviceType("Basic", 1)
        //Todo: make this non-hardcoded, consider fixing the manufacturer
        val details = DeviceDetails(
            "Redmi Note 8",
            ManufacturerDetails("CANON INC.","http://www.canon.com/"),
            ModelDetails("Android 9/Redmi Note 8")
        )
        val service = AnnotationLocalServiceBinder().read(CameraConnectedMobileService::class.java)
        service.setManager(
            DefaultServiceManager<CameraConnectedMobileService>(service as LocalService<CameraConnectedMobileService>?, CameraConnectedMobileService::class.java)
        )
        return LocalDevice(
            DeviceIdentity(udn),
            type,
            details,
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
            Log.e("CameraRegistryListener","(slow mode) Discovery failed of '" + device.displayString + "': "
                    + (ex?.toString() ?: "Couldn't retrieve device/service descriptors"))
        }
        /* End of optimization, you can remove the whole block if your Android handset is fast (>= 600 Mhz) */

        override fun remoteDeviceAdded(registry: Registry?, device: RemoteDevice?) {
            Log.v("CameraRegistyListener", "Device added:  ${device?.getDetails()?.friendlyName}")
        }

        override fun remoteDeviceRemoved(registry: Registry?, device: RemoteDevice?) {
            Log.v("CameraRegistyListener", "Device REMOVED:  ${device?.getDetails()?.friendlyName}")
        }

        override fun localDeviceAdded(registry: Registry?, device: LocalDevice?) {
            Log.v("CameraRegistyListener", "local device added: ${device?.getDetails()?.friendlyName}")

        }

        override fun localDeviceRemoved(registry: Registry?, device: LocalDevice?) {
            Log.v("CameraRegistyListener", "local device REMOVED: ${device?.getDetails()?.friendlyName}")
        }

        fun deviceAdded(device: Device<DeviceIdentity, Device<*, *, *>, Service<*, *>>?) {
            Log.v("CameraRegistyListener", "already connected device added:  ${device?.getDetails()?.friendlyName}")
        }
    }
}
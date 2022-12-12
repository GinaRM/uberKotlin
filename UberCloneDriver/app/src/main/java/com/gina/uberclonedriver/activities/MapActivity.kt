package com.gina.uberclonedriver.activities


import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.gina.uberclonedriver.R
import com.gina.uberclonedriver.databinding.ActivityMapBinding
import com.gina.uberclonedriver.providers.AuthProvider
import com.gina.uberclonedriver.providers.GeoProvider
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*


class MapActivity : AppCompatActivity(), OnMapReadyCallback, Listener {
    private lateinit var binding: ActivityMapBinding
    private var googleMap: GoogleMap? = null
    private var easyWayLocation: EasyWayLocation? = null
    private var myLocationLatLong: LatLng? = null
    private var markerDriver: Marker? = null
    private val geoProvider = GeoProvider()
    private  val authProvider = AuthProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }
        easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

        locationPermissions.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ))

        binding.btnConnect.setOnClickListener { connectDriver() }
        binding.btnDisconnect.setOnClickListener { disconnectDriver() }
    }

    val locationPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permission ->
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            when {
                permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    Log.d("LOCALIZATION", "Permission Granted")
    //                easyWayLocation?.startLocation()
                    checkIfDriverIsConnected()
                }
                permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    Log.d("LOCALIZATION", "Permission Granted With Limitation")
   //                 easyWayLocation?.startLocation()
                    checkIfDriverIsConnected()
                }
                else -> {
                    Log.d("LOCALIZATION", "Permission Denied")
                }
            }
        }
    }

    private fun checkIfDriverIsConnected() {
        geoProvider.getLocation(authProvider.getId()).addOnSuccessListener { document ->
            if(document.exists()) {
                if(document.contains("l")) { //l contains latitude and longitude
                    connectDriver()
                }
                else {
                    showButtonConnect()
                }
            }
            else {
                showButtonConnect()
            }
        }
    }

    private fun saveLocation() {
        if(myLocationLatLong != null) {
            geoProvider.saveLocation(authProvider.getId(), myLocationLatLong!!)
        }
    }

    private fun disconnectDriver() {
        easyWayLocation?.endUpdates()
        if (myLocationLatLong != null) {
            geoProvider.removeLocation(authProvider.getId())
            showButtonConnect()
        }
    }

    private fun connectDriver() {
        easyWayLocation?.endUpdates()
        easyWayLocation?.startLocation()
        showButtonDisconnect()
    }

    private fun showButtonConnect() {
        binding.btnDisconnect.visibility = View.GONE
        binding.btnConnect.visibility = View.VISIBLE
    }

    private fun showButtonDisconnect() {
        binding.btnDisconnect.visibility = View.VISIBLE
        binding.btnConnect.visibility = View.GONE
    }

    private fun addMarker() {
        val drawable = ContextCompat.getDrawable(applicationContext, R.drawable.uber_car)
        val markerIcon = getMarkerFromDrawable(drawable!!)
        if (markerDriver != null) {
            markerDriver?.remove() //do not redraw the icon
        }
        if (myLocationLatLong != null) {
            markerDriver = googleMap?.addMarker(
                MarkerOptions()
                    .position(myLocationLatLong!!)
                    .anchor(05f, 0.5f)
                    .flat(true)
                    .icon(markerIcon)
            )
        }
    }

    private fun getMarkerFromDrawable(drawable: Drawable): BitmapDescriptor {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            70,
            150,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0,0,70,150)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onDestroy() { //when app close or we pass another activity
        super.onDestroy()
        easyWayLocation?.endUpdates()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
     //   easyWayLocation?.startLocation()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        googleMap?.isMyLocationEnabled = false

        try {
            val success = googleMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.style)
            )
            if(!success!!) {
                Log.d("MAPS", "Map style could not be found")
            }

        } catch (e: Resources.NotFoundException) {
            Log.d("MAPS","Error: ${e.toString()}")
        }
    }

    override fun locationOn() {

    }

    override fun currentLocation(location: Location) {
        myLocationLatLong = LatLng(location.latitude, location.longitude) //actual position lat and long

        googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(
            CameraPosition.builder().target(myLocationLatLong!!).zoom(17f).build()
        ))

        addMarker()
        saveLocation()
    }

    override fun locationCancelled() {

    }


}
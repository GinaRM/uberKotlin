package com.gina.uberclone.activities

import android.content.Intent
import android.content.res.Resources
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.example.easywaylocation.draw_path.DirectionUtil
import com.example.easywaylocation.draw_path.PolyLineDataBean
import com.gina.uberclone.R
import com.gina.uberclone.databinding.ActivityMapBinding
import com.gina.uberclone.databinding.ActivityTripInfoBinding
import com.gina.uberclone.models.Price
import com.gina.uberclone.providers.ConfigProvider
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class TripInfoActivity : AppCompatActivity(), OnMapReadyCallback, Listener,  DirectionUtil.DirectionCallBack{
    private lateinit var  binding: ActivityTripInfoBinding
    private var googleMap: GoogleMap? = null
    private var easyWayLocation: EasyWayLocation? = null

    private var extraOriginName = ""
    private var extraDestinationnName = ""
    private var extraOriginLat = 0.0
    private var extraOriginLng = 0.0
    private var extraDestinationLat = 0.0
    private var extraDestinationLng = 0.0

    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null

    private var wayPoints: ArrayList<LatLng> = ArrayList()
    private val WAY_POINT_TAG = "way_point_tag"
    private lateinit var directionUtil: DirectionUtil

    private var markerOrigin: Marker? = null
    private var markerDestination: Marker? = null

    private var configProvider = ConfigProvider()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTripInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        //extras
        extraOriginName = intent.getStringExtra("origin")!!
        extraDestinationnName = intent.getStringExtra("destination")!!
        extraOriginLat = intent.getDoubleExtra("origin_lat", 0.0)
        extraOriginLng = intent.getDoubleExtra("origin_lng", 0.0)
        extraDestinationLat = intent.getDoubleExtra("destination_lat", 0.0)
        extraDestinationLng = intent.getDoubleExtra("destination_lng", 0.0)

        originLatLng = LatLng(extraOriginLat, extraOriginLng)
        destinationLatLng = LatLng(extraDestinationLat, extraDestinationLng)


        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }

        easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

        binding.textViewOrigin.text = extraOriginName
        binding.textViewDestination.text = extraDestinationnName

        Log.d("LOCATION", "Origin lat: ${originLatLng?.latitude}")
        Log.d("LOCATION", "Origin lng: ${originLatLng?.longitude}")
        Log.d("LOCATION", "Origin lat: ${destinationLatLng?.latitude}")
        Log.d("LOCATION", "Origin lng: ${destinationLatLng?.longitude}")

        binding.imageViewBack.setOnClickListener { finish()}
        binding.btnConfirmRequest.setOnClickListener { gotToSearchDriver() }

    }

    private fun gotToSearchDriver() {

        if(originLatLng != null && destinationLatLng != null) {
            val i = Intent(this, SearchActivity::class.java)
            i.putExtra("origin", extraOriginName)
            i.putExtra("destination", extraDestinationnName)
            i.putExtra("origin_lat", originLatLng?.latitude)
            i.putExtra("origin_lng", originLatLng?.longitude)
            i.putExtra("destination_lat", destinationLatLng?.latitude)
            i.putExtra("destination_lng", destinationLatLng?.longitude)
            startActivity(i)
        } else {
            Toast.makeText(this, "You must enter the Pickup location and Destination", Toast.LENGTH_LONG).show()
        }
    }

    private fun getPrices(distance: Double, time: Double) {
        configProvider.getPrices().addOnSuccessListener { document ->
            if (document.exists()) {
                val prices = document.toObject(Price.Price::class.java)

                val totalDistance = distance * prices?.km!!
                val totalTime = time * prices.min!!
                var total = totalDistance + totalTime

                total = if(total < 5000.0) prices.minValue!! else total

                var minTotal = total - prices.difference!!
                var maxTotal = total + prices.difference!!

                val minTotalString = String.format("%.0f", minTotal)
                val maxTotalString = String.format("%.0f", maxTotal)
                binding.textViewPrice.text = "$minTotalString - $maxTotalString"
            }
        }

    }


    private fun addOriginMarker() {
        markerOrigin = googleMap?.addMarker(MarkerOptions().position(originLatLng!!).title("My position")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_location_person)))
    }

    private fun addODestinationMarker() {
        markerDestination = googleMap?.addMarker(MarkerOptions().position(destinationLatLng!!).title("Destination")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_pin)))
    }

    private fun easyDrawRoute() {
        wayPoints.add(originLatLng!!)
        wayPoints.add(destinationLatLng!!)
        directionUtil = DirectionUtil.Builder()
            .setDirectionKey(resources.getString(R.string.google_maps_key))
            .setOrigin(originLatLng!!)
            .setWayPoints(wayPoints)
            .setGoogleMap(googleMap!!)
            .setPolyLinePrimaryColor(R.color.black)
            .setPolyLineWidth(10)
            .setPathAnimation(true)
            .setCallback(this)
            .setDestination(destinationLatLng!!)
            .build()
        directionUtil.initPath()

    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true

        googleMap?.moveCamera(
            CameraUpdateFactory.newCameraPosition(
            CameraPosition.builder().target(originLatLng!!).zoom(13f).build()
        ))
        easyDrawRoute()
        addOriginMarker()
        addODestinationMarker()

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

    override fun currentLocation(location: Location?) {

    }

    override fun locationCancelled() {

    }

    override fun onDestroy() { //when app close or we pass another activity
        super.onDestroy()
        easyWayLocation?.endUpdates()
    }

    override fun pathFindFinish(
        polyLineDetailsMap: HashMap<String, PolyLineDataBean>,
        polyLineDetailsArray: ArrayList<PolyLineDataBean>
    ) {
        var distance = polyLineDetailsArray[1].distance.toDouble() //meters
        var time = polyLineDetailsArray[1].time.toDouble() //seconds
        distance = if(distance < 1000.0) 1000.0 else distance
        time = if (time < 60.0) 60.0 else time

        distance = distance/1000.0
        time = time/60.0

        val timeString = String.format("%.2f", time)
        val distanceString = String.format("%.2f", distance)

        getPrices(distance, time)

        binding.textViewTimeAndDistance.text = "$timeString mins - $distanceString km"

       directionUtil.drawPath(WAY_POINT_TAG)
    }

}
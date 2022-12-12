package com.gina.uberclone.activities


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Geocoder

import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.gina.uberclone.R
import com.gina.uberclone.databinding.ActivityMapBinding
import com.gina.uberclone.models.DriverLocation
import com.gina.uberclone.providers.AuthProvider
import com.gina.uberclone.providers.GeoProvider
import com.gina.uberclone.utils.CarMoveAnim
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.SphericalUtil
import org.imperiumlabs.geofirestore.callbacks.GeoQueryEventListener
import java.sql.Driver


class MapActivity : AppCompatActivity(), OnMapReadyCallback, Listener {
    private lateinit var binding: ActivityMapBinding
    private var googleMap: GoogleMap? = null
    private var easyWayLocation: EasyWayLocation? = null
    private var myLocationLatLong: LatLng? = null
    private val geoProvider = GeoProvider()
    private  val authProvider = AuthProvider()

    //google places variables
    private  var places: PlacesClient? = null
    private var autocompleteOrigin: AutocompleteSupportFragment? = null
    private var autocompleteDestination: AutocompleteSupportFragment? = null
    private var originName: String? = null
    private var destinationName: String? = null
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null

    private var isLocationEnabled = false

    private val driverMarkers = ArrayList<Marker>()
    private val driversLocation = ArrayList<DriverLocation>()


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

        startGooglePlaces()

    }

    val locationPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permission ->
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            when {
                permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    Log.d("LOCALIZATION", "Permission Granted")
                                  easyWayLocation?.startLocation()

                }
                permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    Log.d("LOCALIZATION", "Permission Granted With Limitation")
                                   easyWayLocation?.startLocation()

                }
                else -> {
                    Log.d("LOCALIZATION", "Permission Denied")
                }
            }
        }
    }

    private fun getNearbyDrivers() {

        if(myLocationLatLong == null) return

        geoProvider.getNearbyDrivers(myLocationLatLong!!, 20.0).addGeoQueryEventListener(object: GeoQueryEventListener {
            override fun onKeyEntered(documentID: String, location: GeoPoint) {
                for(marker in driverMarkers) {
                    if(marker.tag != null) {
                        if(marker.tag == documentID) {
                            return
                        }
                    }
                }
                //create new marker for new online driver
                val driverLatLng = LatLng(location.latitude, location.longitude)
                val marker = googleMap?.addMarker((
                        MarkerOptions().position(driverLatLng).title("Available Driver").icon(
                            BitmapDescriptorFactory.fromResource(R.drawable.uber_car)
                        )
                        ))
                marker?.tag = documentID
                driverMarkers.add(marker!!)

                val dl = DriverLocation() //driverLocation
                dl.id = documentID
                driversLocation.add(dl)

            }

            override fun onKeyExited(documentID: String) {
                for (marker in driverMarkers) {
                    if(marker.tag != null) {
                        if(marker.tag == documentID) {
                            marker.remove()
                            driverMarkers.remove(marker)
                            driversLocation.removeAt(getPositionDriver(documentID))
                            return
                        }
                    }
                }

            }

            override fun onKeyMoved(documentID: String, location: GeoPoint) {
                for(marker in driverMarkers) {
                    val start = LatLng(location.latitude, location.longitude)
                    var end: LatLng? = null
                    val position = getPositionDriver(marker.tag.toString())
                    if(marker.tag != null) {
                        if(marker.tag == documentID) {
        //                    marker.position = LatLng(location.latitude, location.longitude)
                            if(driversLocation[position].latLng != null) {
                                end = driversLocation[position].latLng
                            }
                            driversLocation[position].latLng = LatLng(location.latitude, location.longitude)
                            if(end != null) {
                                CarMoveAnim.carAnim(marker, end, start)
                            }
                        }
                    }
                }

            }

            override fun onGeoQueryError(exception: Exception) {

            }

            override fun onGeoQueryReady() {

            }




        })
    }

    private fun getPositionDriver(id: String): Int {
        var position = 0
        for (i in driversLocation.indices) {
            if(id == driversLocation[i].id) {
                position = i
                break
            }
        }
        return position
    }

    @SuppressLint("SuspiciousIndentation")
    private fun onCameraMove() {
        googleMap?.setOnCameraIdleListener {
            try {
                val geocoder = Geocoder(this)
                originLatLng = googleMap?.cameraPosition?.target

                    if(originLatLng != null) {
                        val addressList = geocoder.getFromLocation(originLatLng?.latitude!!, originLatLng?.longitude!!, 1)
                        if(addressList.size > 0) {
                            val city = addressList[0].locality
                            val country = addressList[0].countryName
                            val address = addressList[0].getAddressLine(0)
                            originName = "$address $city"
                            autocompleteOrigin?.setText(originName)
                        }

                    }
            }
            catch (e: Exception) {
                Log.d("ERROR: ", "Error Message: ${e.message}")
            }
        }
    }

    private fun startGooglePlaces() {
        if(!Places.isInitialized()) {
            Places.initialize(applicationContext, resources.getString(R.string.google_maps_key))
        }
        places = Places.createClient(this)
        instanceAutocompleteOrigin()
        instanceAutocompleteDestination()
    }

    private fun limitSearch() {
        val northSide = SphericalUtil.computeOffset(myLocationLatLong, 5000.0, 0.0)
        val southSide = SphericalUtil.computeOffset(myLocationLatLong, 5000.0, 180.0)

        autocompleteOrigin?.setLocationBias(RectangularBounds.newInstance(southSide, northSide))
        autocompleteDestination?.setLocationBias(RectangularBounds.newInstance(southSide, northSide))
    }

    private fun instanceAutocompleteOrigin() {
        autocompleteOrigin = supportFragmentManager.findFragmentById(R.id.placesAutocompleteOrigin) as AutocompleteSupportFragment
        autocompleteOrigin?.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
            )
        )
        autocompleteOrigin?.setHint("Pickup Location")
        autocompleteOrigin?.setCountry("CO")
        autocompleteOrigin?.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                originName = place.name!!
                originLatLng = place.latLng
                Log.d("PLACES", "ADDRESS: $originName")
                Log.d("PLACES", "LAT: ${originLatLng?.latitude}")
                Log.d("PLACES", "LONG: ${originLatLng?.longitude}")

            }

            override fun onError(p0: Status) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun instanceAutocompleteDestination() {
        autocompleteDestination = supportFragmentManager.findFragmentById(R.id.placesAutocompleteDestination) as AutocompleteSupportFragment
        autocompleteDestination?.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
            )
        )
        autocompleteDestination?.setHint("Destination")
        autocompleteDestination?.setCountry("CO")
        autocompleteDestination?.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                destinationName = place.name!!
                destinationLatLng = place.latLng
                Log.d("PLACES", "ADDRESS: $destinationName")
                Log.d("PLACES", "LAT: ${destinationLatLng?.latitude}")
                Log.d("PLACES", "LONG: ${destinationLatLng?.longitude}")

            }

            override fun onError(p0: Status) {
                TODO("Not yet implemented")
            }
        })
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
        onCameraMove()
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


        if (!isLocationEnabled) {
            isLocationEnabled = true
            googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder().target(myLocationLatLong!!).zoom(15f).build()
            ))
            getNearbyDrivers()
            limitSearch()
        }

    }

    override fun locationCancelled() {

    }


}


package com.example.helloworldproject

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler

import android.view.MotionEvent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.app.ActivityCompat
import androidx.core.graphics.createBitmap
import androidx.core.os.postDelayed

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.helloworldproject.databinding.ActivityMyMapsBinding
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO:
//  this class needs to be fleshed out and then used as the object to
//  the data for each friends' marker
class friendMarker{
    private var marker : Marker? = null
    private var markerLatLong = LatLng(0.0,0.0)
    fun createMarker(latitude: Double, longitude: Double){

    }
}

class MyMapsActivity : AppCompatActivity(), OnMapReadyCallback{

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMyMapsBinding
    private lateinit var locationManager: LocationManager

    private var counter: Int = 0

    private val arraySize: Int = 5
    private var theMarkers: Array<Marker?> = Array(arraySize,{null})
    private var markerLatLong = Array(arraySize){ LatLng(0.0,0.0)}




    @SuppressLint("ServiceCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize the location manager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        // import marker icon and scale it
        val markerIcon = BitmapDescriptorFactory.fromResource(R.drawable.icon)
        val width = 35 // desired width in pixels
        val height = 50 // desired height in pixels
        val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.icon)
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false)
        val markerIconScaled = BitmapDescriptorFactory.fromBitmap(resizedBitmap)


        val success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
        //mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN;
        // Add a marker in Sydney and move the camera
        markerLatLong[0] = LatLng(34.7335757, -85.2239801)
        //var marker = mMap.addMarker(MarkerOptions()
        theMarkers[0] = mMap.addMarker(MarkerOptions()
            .position(markerLatLong[0])
            .title("Marker " + counter)
            .visible(true))
        theMarkers[0]?.showInfoWindow()
        theMarkers[0]?.setIcon(markerIconScaled)
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myHome, 15f))
        mMap.setOnCameraMoveListener { onCameraMove() }

    }

    fun onCameraMove(){
        theMarkers[0]?.hideInfoWindow()
        theMarkers[0]?.title = "test" + counter++
        theMarkers[0]?.showInfoWindow()
        markerLatLong[0] = LatLng(markerLatLong[0].latitude + 0.000001, markerLatLong[0].longitude)
        theMarkers[0]?.position = LatLng(markerLatLong[0].latitude, markerLatLong[0].longitude)

    }

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private fun checkLocationPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // Permission already granted
            enableLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted
            enableLocation()
        }
    }

    override fun onResume() {
        super.onResume()
        checkLocationPermission()
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(locationListener)
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val currentLatLng = LatLng(location.latitude, location.longitude)
            mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng))
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            // Handle status changes if needed
        }

    }

    private fun enableLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Register for location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
        }
    }
}
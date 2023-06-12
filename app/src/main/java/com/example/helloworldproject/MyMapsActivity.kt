

package com.example.helloworldproject

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater

import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.app.ActivityCompat
import androidx.core.graphics.createBitmap
import androidx.core.os.postDelayed
import androidx.fragment.app.Fragment

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
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.TileProvider
import com.google.android.material.animation.DrawableAlphaProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO:
//  this class needs to be fleshed out and then used as the object to
//  the data for each friends' marker
class FriendMarker{
    private var marker : Marker? = null
    private lateinit var markerLatLong: LatLng
    private lateinit var markerName: String
    private var alive: Boolean = false // friends that go out of range or otherwise stop reporting location are considered dead / false

    fun createMarker(latitude: Double, longitude: Double, map: GoogleMap, icon: BitmapDescriptor){
        markerLatLong = LatLng(latitude,longitude)

        marker = map.addMarker(MarkerOptions()
            .position(markerLatLong)
            .title("Marker")
            .visible(true))
        marker?.showInfoWindow()
        marker?.setIcon(icon)
    }

    fun update(){
        // do all the update stuff for this marker

        //theMarkers[0]?.hideInfoWindow()
        //theMarkers[0]?.title = "test" + counter++
        //theMarkers[0]?.showInfoWindow()
        //markerLatLong[0] = LatLng(markerLatLong[0].latitude + 0.000001, markerLatLong[0].longitude)
        //theMarkers[0]?.position = LatLng(markerLatLong[0].latitude, markerLatLong[0].longitude)
        this.marker?.position = LatLng(this.markerLatLong.latitude, this.markerLatLong.longitude)
    }

    fun setNewLatLong(lat:Double, long:Double){
        // set a new lat long for this marker
    }

    fun getCurrentLatLong(): LatLng{
        return LatLng(this.markerLatLong.latitude,this.markerLatLong.longitude)
    }

    fun changeName(name:String){
        markerName = name
        this.marker?.hideInfoWindow()
        this.marker?.title = markerName
        this.marker?.showInfoWindow()
    }
}

class MyMapsActivity : AppCompatActivity(), OnMapReadyCallback{

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMyMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var overlayText: TextView
    private lateinit var menuButton: Button
    private lateinit var theMenu: LinearLayout
    private lateinit var menuExitButton: Button
    private lateinit var myNameTextInput: EditText
    private lateinit var myNamePrompt: TextView

    private var friendMarkersArr: MutableList<FriendMarker> = MutableList(0,{FriendMarker()})


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

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
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

        //markerLatLong[0] = LatLng(34.7335757, -85.2239801)
        //markerLatLong[0] = LatLng(34.0476818,-84.6960141)

        friendMarkersArr.add(FriendMarker())
        friendMarkersArr[0].createMarker(34.7335757, -85.2239801, mMap, markerIconScaled)

        // link signal indicator text overlay
        overlayText = findViewById(R.id.textView1)

        // Build the menu
        menuButton = findViewById(R.id.menuButton)
        theMenu = findViewById(R.id.menuContainer)
        menuExitButton = findViewById(R.id.menuExitButton)
        myNameTextInput = findViewById(R.id.myNameTextInput)
        myNamePrompt = findViewById(R.id.myNameTextPrompt)

        menuButton.setOnClickListener{
            menuButton.visibility = View.GONE
            theMenu.visibility = View.VISIBLE
        }

        menuExitButton.setOnClickListener{
            menuButton.visibility = View.VISIBLE
            theMenu.visibility = View.GONE
            hideKeyboard() // Not working
            // TODO:
            //  Instead of changing this friend marker, this should update the BT module
            friendMarkersArr[0].changeName(myNameTextInput.text.toString())
        }
        theMenu.setBackgroundColor(Color.GRAY)
        theMenu.visibility = View.GONE


        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myHome, 15f))

        mMap.setOnCameraMoveListener { updateAllElements() }
    }

    private fun updateAllElements(){
        //theMarkers[0]?.hideInfoWindow()
        //theMarkers[0]?.title = "test" + counter++
        //theMarkers[0]?.showInfoWindow()
        //markerLatLong[0] = LatLng(markerLatLong[0].latitude + 0.000001, markerLatLong[0].longitude)
        //theMarkers[0]?.position = LatLng(markerLatLong[0].latitude, markerLatLong[0].longitude)

        // update all the friendMarkers
        for (friend in friendMarkersArr){
            friend.update()
        }

        // TODO:
        //  Once all the markers are updated, create a virtual box around all the markers and users position. Then expand it
        //  by 5% on each side. This represents the zoom level that needs to be calculated and set. The view should be centered on this box.

        // update the signal indicator
        val text = "Signal: Good"
        val spannable = SpannableString(text)
        val colorSpan = ForegroundColorSpan(Color.GREEN)
        spannable.setSpan(colorSpan, 8,12, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        overlayText.text = spannable
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

    fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }
    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

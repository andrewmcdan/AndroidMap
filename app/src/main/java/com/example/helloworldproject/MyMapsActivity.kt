package com.example.helloworldproject
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.helloworldproject.databinding.ActivityMyMapsBinding
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.random.Random


// TODO:
//  this class needs to be fleshed out and then used as the object to
//  the data for each friends' marker
class FriendMarker(latitude: Double, longitude: Double, map: GoogleMap, icon: BitmapDescriptor, invisIcon: BitmapDescriptor){
    private var markerLatLong = LatLng(latitude,longitude)
    private var normalIcon: BitmapDescriptor = icon
    private var invisIcon: BitmapDescriptor = invisIcon
    private var marker : Marker? = map.addMarker(MarkerOptions()
        .position(markerLatLong)
        .title("Marker")
        .visible(true).icon(icon))
    private lateinit var markerName: String
    private var alive: Boolean = false // friends that go out of range or otherwise stop reporting location are considered dead / false
    private val randy: Random = Random
    private var id: Int = randy.nextInt()

    fun update(){
        // do all the update stuff for this marker
        this.marker?.position = markerLatLong
    }

    fun setNewLatLong(lat:Double, long:Double){
        // set a new lat long for this marker
        markerLatLong = LatLng(lat,long)
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

    fun disableMarkerIcon(){
        marker?.setIcon(invisIcon) // Set the transparent icon
        marker?.hideInfoWindow()
    }
    fun enableMarkerIcon(){
        marker?.setIcon(normalIcon) // Set the normal icon
        marker?.showInfoWindow()
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
    private lateinit var autoZoomEnableButton: Button
    private var friendMarkersArr = mutableListOf<FriendMarker>()

    private var autoZoomEnabled: Boolean = true

    private val updateHandler = Handler()

    private val runnable = Runnable {
        updateAllElements()
    }



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
        val width = 35 // desired width in pixels
        val height = 50 // desired height in pixels
        val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.icon)
        var resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false)
        val markerIconScaled = BitmapDescriptorFactory.fromBitmap(resizedBitmap)
        resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 1, 1, false)
        val markerIconScaledZero = BitmapDescriptorFactory.fromBitmap((resizedBitmap))

        val success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))

        var latitude0 = 0.0
        var longitude0 = 0.0
        if (locationManager != null) {
            var location = locationManager
                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                latitude0 = location.latitude;
                longitude0 = location.longitude;
            }
        }


        friendMarkersArr.add(FriendMarker(latitude0, longitude0, mMap, markerIconScaled, markerIconScaledZero))
        friendMarkersArr.add(FriendMarker(34.7335757, -85.2239801, mMap, markerIconScaled, markerIconScaledZero))
        friendMarkersArr[0].disableMarkerIcon()

        // link signal indicator text overlay
        overlayText = findViewById(R.id.textView1)

        // Build the menu
        menuButton = findViewById(R.id.menuButton)
        theMenu = findViewById(R.id.menuContainer)
        menuExitButton = findViewById(R.id.menuExitButton)
        myNameTextInput = findViewById(R.id.myNameTextInput)
        myNamePrompt = findViewById(R.id.myNameTextPrompt)
        autoZoomEnableButton = findViewById(R.id.menuEnableAutoZoom)

        autoZoomEnableButton.text = getString(R.string.disable_autoZoom)

        menuButton.setOnClickListener{
            menuButton.visibility = View.GONE
            theMenu.visibility = View.VISIBLE
        }

        autoZoomEnableButton.setOnClickListener{
            autoZoomEnabled = !autoZoomEnabled
            if(autoZoomEnabled)
                autoZoomEnableButton.text = getString(R.string.disable_autoZoom)
            if(!autoZoomEnabled)
                autoZoomEnableButton.text = getString(R.string.enable_autoZoom)
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

        // call the update function in 1 second
        updateHandler.postDelayed(runnable, 1000)
    }


    private fun updateAllElements(){
        // update all the friendMarkers
        for (friend in friendMarkersArr){
            friend.update()
        }

        // TODO:
        //  Once all the markers are updated, create a virtual box around all the markers and users position. Then expand it
        //  by 5% on each side. This represents the zoom level that needs to be calculated and set. The view should be centered on this box.
        var builder = LatLngBounds.builder()
        for(f in friendMarkersArr)
            builder.include(f.getCurrentLatLong())
        var bounds: LatLngBounds = builder.build()
        var update: CameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds,100)
        if(autoZoomEnabled)
            mMap.animateCamera(update)


        // update the signal indicator
        val text = "Signal: Good"
        val spannable = SpannableString(text)
        val colorSpan = ForegroundColorSpan(Color.GREEN)
        val colorSpan1 = BackgroundColorSpan(Color.BLACK)
        spannable.setSpan(colorSpan, 8,12, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        spannable.setSpan(colorSpan1,0, 12, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        overlayText.text = spannable

        // call this function again in 1 second
        updateHandler.postDelayed(runnable, 1000)
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
            //mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng))
            friendMarkersArr[0].setNewLatLong(location.latitude,location.longitude)
            //friendMarkersArr[0].update()
            //updateAllElements()
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

    private fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }
    private fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}




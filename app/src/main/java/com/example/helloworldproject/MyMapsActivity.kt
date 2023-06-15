package com.example.helloworldproject
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.method.ScrollingMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.helloworldproject.databinding.ActivityMyMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.random.Random


// Object that maintains data about a friends location
class FriendMarker(latitude: Double, longitude: Double, map: GoogleMap,
                   private var normalIcon: BitmapDescriptor,
                   private var invisIcon: BitmapDescriptor
){
    private var markerLatLong = LatLng(latitude,longitude)
    private var marker : Marker? = map.addMarker(MarkerOptions()
        .position(markerLatLong)
        .title("Marker")
        .visible(true).icon(normalIcon))
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

class MyMapsActivity : AppCompatActivity(), OnMapReadyCallback, BluetoothLeUart.Callback{
    // boilerplate vars
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMyMapsBinding
    private lateinit var locationManager: LocationManager


    // UI elements
    private lateinit var overlayText: TextView
    private lateinit var menuButton: Button
    private lateinit var theMenu: LinearLayout
    private lateinit var menuExitButton: Button
    private lateinit var zoomPlus: Button
    private lateinit var zoomMinus: Button
    private lateinit var myNameTextInput: EditText
    private lateinit var myNamePrompt: TextView
    private lateinit var autoZoomEnableButton: Button
    private lateinit var autoCenterEnableButton: Button
    private lateinit var menuEnableOneKlickCircle: Button
    private lateinit var debugSerialOutputTextView:TextView

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private var friendMarkersArr = mutableListOf<FriendMarker>()

    // state trackers
    private var autoZoomEnabled: Boolean = true
    private var autoCenterEnabled: Boolean = true
    private var oneKlickCircleEnabled: Boolean = true

    private var debugSerialOutput: String = ""

    private lateinit var oneKlickCircle: Circle

    private lateinit var uart: BluetoothLeUart

    private val updateHandler = Handler(Looper.myLooper()!!)

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
        uart = BluetoothLeUart(applicationContext)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.setMaxZoomPreference(18.0f)


        // import marker icon and scale it
        val width = 35 // desired width in pixels
        val height = 50 // desired height in pixels
        val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.icon)
        var resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false)
        val markerIconScaled = BitmapDescriptorFactory.fromBitmap(resizedBitmap)
        resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 1, 1, false)
        val markerIconScaledZero = BitmapDescriptorFactory.fromBitmap((resizedBitmap))

        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))

        var latitude0 = 0.0
        var longitude0 = 0.0
        val location = locationManager
            .getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location != null) {
            latitude0 = location.latitude
            longitude0 = location.longitude
        }

        // create friendMarker #0. This represents *this device.
        friendMarkersArr.add(FriendMarker(latitude0, longitude0, mMap, markerIconScaled, markerIconScaledZero))
        friendMarkersArr[0].disableMarkerIcon()

        // temp for debugging
        friendMarkersArr.add(FriendMarker(latitude0, longitude0, mMap, markerIconScaled, markerIconScaledZero))

        // create a red circle with radius of 1km around the users location.
        oneKlickCircle = mMap.addCircle(CircleOptions()
            .center(friendMarkersArr[0].getCurrentLatLong())
            .radius(1000.0)
            .strokeColor(Color.RED)
            .strokeWidth(1.0f)
            .fillColor(Color.TRANSPARENT))

        // link signal indicator text overlay
        overlayText = findViewById(R.id.textView1)

        // build the zoom adjust buttons
        zoomPlus = findViewById(R.id.zoomButtonsPlus)
        zoomMinus = findViewById(R.id.zoomButtonsMinus)
        zoomPlus.setOnClickListener{
            mMap.animateCamera(CameraUpdateFactory.zoomBy(1.0f))
        }
        zoomMinus.setOnClickListener{
            mMap.animateCamera(CameraUpdateFactory.zoomBy(-1.0f))
        }

        // Build the menu
        menuButton = findViewById(R.id.menuButton)
        theMenu = findViewById(R.id.menuContainer)
        menuExitButton = findViewById(R.id.menuExitButton)
        myNameTextInput = findViewById(R.id.myNameTextInput)
        myNamePrompt = findViewById(R.id.myNameTextPrompt)
        autoZoomEnableButton = findViewById(R.id.menuEnableAutoZoom)
        autoCenterEnableButton = findViewById(R.id.menuEnableAutoCenter)
        menuEnableOneKlickCircle = findViewById(R.id.menuEnableOneKlickCircle)
        debugSerialOutputTextView = findViewById(R.id.serialOutputTextView)
        debugSerialOutputTextView.movementMethod = ScrollingMovementMethod()

        autoZoomEnableButton.text = getString(R.string.disable_autoZoom)
        autoCenterEnableButton.text = getString(R.string.disable_autoCenter)
        menuEnableOneKlickCircle.text = getString(R.string.disable_oneKlickCircle)

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

        autoCenterEnableButton.setOnClickListener{
            autoCenterEnabled = !autoCenterEnabled
            if(autoCenterEnabled)
                autoCenterEnableButton.text = getString(R.string.disable_autoCenter)
            if(!autoCenterEnabled)
                autoCenterEnableButton.text = getString(R.string.enable_autoCenter)
        }

        menuEnableOneKlickCircle.setOnClickListener{
            oneKlickCircleEnabled = !oneKlickCircleEnabled
            if(oneKlickCircleEnabled)
                menuEnableOneKlickCircle.text = getString(R.string.disable_oneKlickCircle)
            if(!oneKlickCircleEnabled)
                menuEnableOneKlickCircle.text = getString(R.string.enable_oneKlickCircle)
        }

        menuExitButton.setOnClickListener{
            menuButton.visibility = View.VISIBLE
            theMenu.visibility = View.GONE
            hideKeyboard() // Not working
            // TODO:
            //  Instead of changing this friend marker, this should update the BT module with the name
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

        // if enabled, center the viewport on the bounding box that includes all friendMarkers and,
        // if enabled, zoom into bounding box, not to exceed max zoom set above
        val builder = LatLngBounds.builder()
        for(f in friendMarkersArr)
            builder.include(f.getCurrentLatLong())
        if(autoZoomEnabled && autoCenterEnabled)
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(),100))
        else if(autoCenterEnabled)
            mMap.animateCamera(CameraUpdateFactory.newLatLng(builder.build().center))

        // update the 1km circle
        if(oneKlickCircleEnabled){
            oneKlickCircle.isVisible = true
            oneKlickCircle.center = friendMarkersArr[0].getCurrentLatLong()
        }else{
            oneKlickCircle.isVisible = false
        }

        // update the signal indicator
        // TODO: get state of signal form BT dev
        val signalIndText = "Signal: Good"
        val spannable = SpannableString(signalIndText)
        val colorSpan = ForegroundColorSpan(Color.GREEN)
        val colorSpan1 = BackgroundColorSpan(Color.BLACK)
        spannable.setSpan(colorSpan, 8,signalIndText.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        spannable.setSpan(colorSpan1,0, signalIndText.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        overlayText.text = spannable

        // call this function again in 1 second
        updateHandler.postDelayed(runnable, 1000)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkLocationPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // Permission already granted
            enableLocation()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted
            enableLocation()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onResume() {
        super.onResume()
        checkLocationPermission()
        //uart.registerCallback(this)
        //uart.connectFirstAvailable()
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(locationListener)
        //uart.unregisterCallback(this)
        //uart.disconnect()
    }

    private fun debugWriteLine(chars:String){
        runOnUiThread {
            debugSerialOutput += chars
            debugSerialOutputTextView.text = debugSerialOutput
        }

    }
    override fun onDeviceInfoAvailable(){
        TODO("Not yet implemented")
    }

    override fun onDeviceFound(device: BluetoothDevice){
        TODO("Not yet implemented")
    }

    override fun onReceive(uart: BluetoothLeUart, rx: BluetoothGattCharacteristic){
        TODO("Not yet implemented")
    }

    override fun onDisconnected(uart: BluetoothLeUart?) {
        TODO("Not yet implemented")
    }

    override fun onConnectFailed(uart: BluetoothLeUart?) {
        TODO("Not yet implemented")
    }

    override fun onConnected(uart: BluetoothLeUart?) {
        TODO("Not yet implemented")
        //debugWriteLine(uart?.deviceInfo!!)
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            friendMarkersArr[0].setNewLatLong(location.latitude,location.longitude)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun enableLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Register for location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
            locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 0L, 0f, locationListener)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)
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




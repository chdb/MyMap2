package com.example.mymap2

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.GoogleMap
import android.Manifest
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng


class Locn
    (val mMapAct :MapsActivity)
{

    var mLocnPmn = false
    var mLastLocn : Location? = null
    var mFusedLocnClient : FusedLocationProviderClient? = null

    private val DEFAULT_Locn = LatLng( 42.125, 55.123 )
    private val FINE_LOCN_Code = 1
    private val DEFAULT_Zoom = 10f
    val GRANTED = PackageManager.PERMISSION_GRANTED
    val FINE_LOCN_Pmn = Manifest.permission.ACCESS_FINE_LOCATION

    fun update() {
        updateLocationUI()
        getDeviceLocation()
    }

    private fun getLocationPermission() {
        val pmn = ContextCompat.checkSelfPermission (mMapAct.applicationContext, FINE_LOCN_Pmn)
        if (pmn == GRANTED)
            mLocnPmn = true
        else
            ActivityCompat.requestPermissions (mMapAct, arrayOf(FINE_LOCN_Pmn), FINE_LOCN_Code)
    }

    fun onRequestPermissionsResult (requestCode: Int, permissions: Array<String>, results: IntArray) {
        mLocnPmn = false
        if (requestCode == FINE_LOCN_Code)
            mLocnPmn = (results.isNotEmpty() && (results[0] == GRANTED)) // If request is cancelled, the results array is empty.
        updateLocationUI()
    }

    private fun updateLocationUI() {
        //  if (mMap != null)
        try {
            with (mMapAct.mMap) {
                isMyLocationEnabled = mLocnPmn
                uiSettings.isMyLocationButtonEnabled = mLocnPmn
            }
            if (! mLocnPmn) {
                mLastLocn = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            log("Exception: ${e.message}")
        }
    }

    private fun getDeviceLocation() {
        // Get the best and most recent location of the device, which may be null in rare cases when a location is not available.
        try {
            if (mLocnPmn) {
                val located = mFusedLocnClient?.lastLocation
                located?.addOnCompleteListener { task ->
                    var locn = DEFAULT_Locn
                    if (task.isSuccessful) {
                        mLastLocn = task.result
                        locn = LatLng ( mLastLocn!!.latitude
                            , mLastLocn!!.longitude )
                    } else {
                        log("Current location is null. Using defaults.")
                        log("Exception: ${task.exception}")
                        mMapAct.mMap.uiSettings.isMyLocationButtonEnabled = false
                    }
                    mMapAct.mMap.moveCamera (CameraUpdateFactory.newLatLngZoom (locn, DEFAULT_Zoom)) // Set the map's camera position to the current location of the device.
                }
            }
        } catch (e: SecurityException) {
            log("Exception: ${e.message}")
        }
    }


}
//interface LocnListener {
//    fun locnResponse (locnResult :LocationResult)
//}
//
//class Location_ (  var activity :AppCompatActivity
//                , locnListener :LocnListener)
//{
//    private val prmnFineLocn   = android.Manifest.permission.ACCESS_FINE_LOCATION
//    private val prmnCoarseLocn = android.Manifest.permission.ACCESS_COARSE_LOCATION
//    private val mLOCN_REQUEST_CODE = 100
//    private var fusedLocnClient :FusedLocationProviderClient? = null
//    private var locnRequest :LocationRequest? = null
//    private var callback    :LocationCallback? = null
//    init {
//        fusedLocnClient = FusedLocationProviderClient (activity.applicationContext)
//        initLocnRequest()
//        callback = object :LocationCallback(){
//            override fun onLocationResult (p0 :LocationResult?) {
//                super.onLocationResult (p0)
//                locnListener.locnResponse (p0!!)
//            }
//        }
//    }
//
//    private fun initLocnRequest() {
//        locnRequest = LocationRequest()
//        locnRequest?.interval = 50000
//        locnRequest?.fastestInterval = 5000
//        locnRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//    }
//
//    private fun validatePermissionsLocn() :Boolean {
//        val fineLocnGranted   = ActivityCompat.checkSelfPermission (activity.applicationContext, prmnFineLocn)   == PackageManager.PERMISSION_GRANTED
//        val coarseLocnGranted = ActivityCompat.checkSelfPermission (activity.applicationContext, prmnCoarseLocn) == PackageManager.PERMISSION_GRANTED
//        return fineLocnGranted && coarseLocnGranted
//    }
//
//    private fun requestPermissions() {
//        val contextProvider = ActivityCompat.shouldShowRequestPermissionRationale(activity, prmnFineLocn)
//        if (contextProvider)
//            toast("Permission is requested to obtain locn")
//        permissionRequest()
//    }
//
//    private fun permissionRequest() {
//        ActivityCompat.requestPermissions (activity, arrayOf (prmnFineLocn, prmnCoarseLocn), mLOCN_REQUEST_CODE)
//    }
//
//    fun onRequestPermissionsResult (requestCode:Int, permissions:Array<out String>, grantResults:IntArray) {
//        when(requestCode){
//            mLOCN_REQUEST_CODE -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
//                    getLocn()
//                else
//                    toast("You did not give permissions to get locn")
//
//            }
//        }
//    }
//
//    fun stopUpdateLocn(){
//        this.fusedLocnClient?.removeLocationUpdates (callback)
//    }
//
//    fun initLocn(){
//        if (validatePermissionsLocn())
//            getLocn()
//        else
//            requestPermissions()
//    }
//
//    @SuppressLint("MissingPermission")
//    @Suppress("MissingPermission")
//    private fun getLocn() {
//        validatePermissionsLocn()
//        fusedLocnClient?.requestLocationUpdates (locnRequest, callback, null)
//    }
//}
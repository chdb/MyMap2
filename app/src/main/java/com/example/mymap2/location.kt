package com.example.mymap2

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.credentials.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng


class PermissionMgr
    (val mMapAct: MapsActivity)
{
    var mAccounts = arrayOf<Account>()

    var mLocnPmn = false
    var mLastLocn : Location? = null
    var mFusedLocnClient : FusedLocationProviderClient? = null

    private val DEFAULT_Locn = LatLng( 42.125, 55.123 )
    private val FINE_LOCN_Code = 1
    private val ACCOUNTS_Code = 42
    private val DEFAULT_Zoom = 10f
    val GRANTED = PackageManager.PERMISSION_GRANTED
    val FINE_LOCN_Pmn = Manifest.permission.ACCESS_FINE_LOCATION
    val ACCOUNTS_Pmn  = Manifest.permission.GET_ACCOUNTS

    @RequiresApi(Build.VERSION_CODES.M)
    fun update() {
        updateLocationUI()
        getDeviceLocation()
     //   getAccountPmn()
    }

    private fun getLocationPermission() {
        if (needPermisssion(FINE_LOCN_Pmn))
            ActivityCompat.requestPermissions (mMapAct, arrayOf(FINE_LOCN_Pmn), FINE_LOCN_Code)
        else
           mLocnPmn = true
    }

    private fun needPermisssion(pmn: String) =
        ContextCompat.checkSelfPermission (mMapAct.applicationContext, pmn) != GRANTED

    private val CREDENTIAL_PICKER_REQUEST = 2  // Set to an unused request code

    // Construct a request for phone numbers and show the picker
    private fun requestHint() {
        val hintRequest = HintRequest.Builder()
         //   .setPhoneNumberIdentifierSupported(true) //finds mobile number
            .setEmailAddressIdentifierSupported(true)
            .build()
        val credentialsClient = Credentials.getClient(mMapAct)
        val intent = credentialsClient.getHintPickerIntent(hintRequest)
        mMapAct.startIntentSenderForResult(// calls the dialog to choose an email account BUT it dopes not find OFFICE365 accounts ie dhamma.org
            intent.intentSender,
            CREDENTIAL_PICKER_REQUEST,
            null, 0, 0, 0
        )
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null)
            when (requestCode) {
                CREDENTIAL_PICKER_REQUEST ->
                    // Obtain the phone number from the result
                {
                    val credential = data.getParcelableExtra<Credential>(Credential.EXTRA_KEY)
                    // -- will need to process phone number string
                    log("credential = $credential")
                    log("credential.getId()=${credential?.id}")
                }
                // ...
                ACCOUNTS_Code -> {
                    val accountManager = AccountManager.get(mMapAct)
                    mAccounts = accountManager.getAccountsByType(null)
                    for (acc in mAccounts)
                        log(" 2  account: " + acc.name + "    type: " + acc.type)
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getAccountPmn() {
        requestHint()
//        val hintRequest = HintRequest.Builder()
//            .setHintPickerConfig(
//                CredentialPickerConfig.Builder()
//                    .setShowCancelButton(true)
//                    .build()
//            )
//            .setEmailAddressIdentifierSupported(true)
//            .setAccountTypes(IdentityProviders.GOOGLE)
//            .build()
//
//        val intent: PendingIntent = mCredentialsClient.getHintPickerIntent(hintRequest)
//        try {
//            startIntentSenderForResult(intent.intentSender, RC_HINT, null, 0, 0, 0)
//        } catch (e: SendIntentException) {
//            Log.e(TAG, "Could not start hint picker Intent", e)
//        }

        val accounts: Array<Account> = AccountManager.get(mMapAct.applicationContext).getAccountsByType(null)
        log( "acc.size = ${accounts.size}")
        for (account in accounts) {
            log(" 1 account: " + account.name + " type: " + account.type)
        }
        if (needPermisssion(ACCOUNTS_Pmn)) {
            val intent = AccountManager.newChooseAccountIntent(
                null,
                null,
               arrayOf("com.google"
                   , "com.google.android.legacyimap"
                   , "com.google.android.gm.legacyimap"
                   , "com.google.android.gm.pop3"
                   , "com.microsoft.office.outlook.USER_ACCOUNT"
                   , "com.twitter.android.auth.login"
                   , "com.facebook.auth.login"
                   , "com.linkedin.android"
               ),
                //arrayOf("com.google.android.legacyimap"),
               // null,
                null,
                null,
                null,
                null
            )
            mMapAct.startActivityForResult (intent, ACCOUNTS_Code)
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
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
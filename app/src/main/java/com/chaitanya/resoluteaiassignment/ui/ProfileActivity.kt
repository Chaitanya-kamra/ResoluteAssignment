package com.chaitanya.resoluteaiassignment.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.chaitanya.resoluteaiassignment.R
import com.chaitanya.resoluteaiassignment.databinding.ActivityProfileBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() ,OnMapReadyCallback{
    private lateinit var binding: ActivityProfileBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth
    private val firestore : FirebaseFirestore = FirebaseFirestore.getInstance()
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0
    private var requestPermission : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()){
                permissions ->
            permissions.entries.forEach {
                val permission = it.key
                val isGranted = it.value
                if (isGranted){
                    when (permission) {
                        Manifest.permission.ACCESS_FINE_LOCATION -> {
                            setLocation()
                        }
                    }
                }
                else{
                    if (permission == Manifest.permission.ACCESS_FINE_LOCATION){
                        Toast.makeText(this,"Provide Fine location", Toast.LENGTH_SHORT).show()
                        binding.lnlPermi.visibility = View.VISIBLE
                        binding.lnlProgress.visibility = View.GONE
                        binding.lnlMap.visibility = View.GONE
                        binding.btnRefresh.setOnClickListener {
                            requestCurrentLocation()
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val userDocRef = firestore.collection("User").document(auth.uid.toString())

        userDocRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // User data exists in Firestore.
                    val userData = documentSnapshot.data

                    val name = userData?.get("name") as String
                    val email = userData["email"] as String
                    val phone = userData["phone"] as String
                    binding.tvName.text = "Howdy $name !!"
                    binding.tvMail.text = email
                    binding.tvPhone.text = phone
                } else {
                    // User data doesn't exist in Firestore.
                    // Handle this case (e.g., show a message to the user).
                }
            }
            .addOnFailureListener { e ->
                // Handle any errors that occur during the retrieval process.
            }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

//            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//            startActivity(intent)
        } else {
            requestCurrentLocation()
        }

    }
    private fun isLocationEnabled(): Boolean {
        val locationManager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)

    }

    private  fun requestCurrentLocation(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.CAMERA)){
            showRationalDialogForPermissions()
        } else {
            requestPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }

    @SuppressLint("MissingPermission")
    private fun setLocation() {
        showProgressDialog()
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(100)
            .setMaxUpdateDelayMillis(500)
            .setMaxUpdates(1)
            .build()


        mFusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )
    }
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val lastLocation = locationResult.lastLocation

            if (lastLocation != null) {
                mLatitude = lastLocation.latitude
            }
            if (lastLocation != null) {
                mLongitude = lastLocation.longitude
            }
            Toast.makeText(this@ProfileActivity,mLatitude.toString()+mLongitude.toString(),Toast.LENGTH_SHORT).show()

            setUpMap()
        }
    }

    private fun setUpMap() {
        dismissProgressDialog()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun showProgressDialog() {
        runOnUiThread {
            binding.lnlPermi.visibility = View.GONE
            binding.lnlProgress.visibility = View.VISIBLE
            binding.lnlMap.visibility = View.GONE
        }

    }
    private fun dismissProgressDialog() {
        runOnUiThread {
            binding.lnlPermi.visibility = View.GONE
            binding.lnlProgress.visibility = View.GONE
            binding.lnlMap.visibility = View.VISIBLE
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        val position = LatLng(
            mLatitude,
            mLongitude
        )
        googleMap.addMarker(MarkerOptions().position(position).title("Your Location"))
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 15f)
        googleMap.animateCamera(newLatLngZoom)
    }

}
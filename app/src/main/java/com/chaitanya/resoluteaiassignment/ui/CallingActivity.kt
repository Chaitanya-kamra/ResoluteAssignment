package com.chaitanya.resoluteaiassignment.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.chaitanya.resoluteaiassignment.MainRepository
import com.chaitanya.resoluteaiassignment.R
import com.chaitanya.resoluteaiassignment.databinding.ActivityCallingBinding
import com.chaitanya.resoluteaiassignment.model.DataModelType
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson


class CallingActivity : AppCompatActivity(),MainRepository.Listener {
    private lateinit var binding: ActivityCallingBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var webRTCRepository: MainRepository
    val gson = Gson()
    private lateinit var user: String
    private var previousData: String? = null
    private var isCameraMuted = false
    private var isMicrophoneMuted = false

    private var requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val grantedPermissions =
                permissions.entries.filter { it.value }.map { it.key }.toTypedArray()

            if (grantedPermissions.contains(Manifest.permission.CAMERA) &&
                grantedPermissions.contains(Manifest.permission.RECORD_AUDIO)
            ) {
                binding.lnlFull.visibility = View.VISIBLE
                binding.btnPermiLnl.visibility = View.GONE
                initate()
            } else {
                binding.lnlFull.visibility = View.GONE
                binding.btnPermiLnl.visibility = View.VISIBLE
            }
        }

    private fun initate() {

        webRTCRepository = MainRepository.getInstance()
        webRTCRepository.repolistener = this
        binding.btCall.setOnClickListener {
            binding.btCall.isEnabled = false
            closeKeyboard()
            webRTCRepository.sendCallRequest(binding.etCall.text.toString()) {
                Toast.makeText(
                    this,
                    "couldnt find the target",
                    Toast.LENGTH_SHORT
                ).show()
                binding.btCall.isEnabled = true
            }
        }
        webRTCRepository.initLocalView(binding.localView)
        webRTCRepository.initRemoteView(binding.remoteView)
        webRTCRepository.subscribeForLatestEvent {data->
            if (data.type == DataModelType.StartCall){
                runOnUiThread {
                    closeKeyboard()
                    binding.tvCaller.text = "${data.sender} is Calling You"
                    binding.lnlMakeCall.visibility = View.GONE
                    binding.lnlCll.visibility = View.VISIBLE
                    binding.btdecline.setOnClickListener {
                        binding.lnlMakeCall.visibility = View.VISIBLE
                        binding.lnlCll.visibility = View.GONE
                    }
                    binding.btAns.setOnClickListener {
                        binding.lnlMakeCall.visibility = View.GONE
                        binding.lnlCll.visibility = View.GONE
                        binding.callLayout.visibility = View.VISIBLE
                        webRTCRepository.startCall(data.sender)
                    }
                }
            }
        }

        binding.switchCameraButton.setOnClickListener {
            webRTCRepository.switchCamera()
        }
        binding.micButton.setOnClickListener {
            if (isMicrophoneMuted) {
                binding.micButton.setImageResource(R.drawable.ic_baseline_mic_24)

            } else {
                binding.micButton.setImageResource(R.drawable.ic_baseline_mic_off_24)
            }
            webRTCRepository.toggleAudio(isMicrophoneMuted)
            isMicrophoneMuted = !isMicrophoneMuted
        }
        binding.videoButton.setOnClickListener {
            if (isCameraMuted) {
                binding.videoButton.setImageResource(R.drawable.ic_baseline_videocam_24)
            } else {
                binding.videoButton.setImageResource(R.drawable.ic_baseline_videocam_off_24)

            }
            webRTCRepository.toggleVideo(isCameraMuted)
            isCameraMuted = !isCameraMuted
        }
        binding.endCallButton.setOnClickListener {
            webRTCRepository.endCall()
            finish()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarCallingActivity)
        binding.ivProfile.setOnClickListener {
            val i = Intent(this@CallingActivity,ProfileActivity::class.java)
            startActivity(i)
        }

        requestPermission.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
        binding.givePermi.setOnClickListener {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }
    private fun closeKeyboard() {
        val view = this.currentFocus

        if (view != null) {

            // now assign the system
            // service to InputMethodManager
            val manager = getSystemService(INPUT_METHOD_SERVICE
            ) as InputMethodManager
            manager
                .hideSoftInputFromWindow(
                    view.windowToken, 0
                )
        }
    }

    override fun webrtcConnected() {
        runOnUiThread {
            binding.lnlMakeCall.visibility = View.GONE
            binding.lnlCll.visibility = View.GONE
            binding.callLayout.visibility = View.VISIBLE
        }
    }

    override fun webrtcClosed() {
        Log.e("afa","end")
        runOnUiThread {
            finish()
        }
    }
}

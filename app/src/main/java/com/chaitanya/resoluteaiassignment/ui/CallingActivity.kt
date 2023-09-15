package com.chaitanya.resoluteaiassignment.ui

import WebRTCRepository
import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.chaitanya.resoluteaiassignment.ErrorCallBack
import com.chaitanya.resoluteaiassignment.NewEventCallBack
import com.chaitanya.resoluteaiassignment.SuccessCallBack
import com.chaitanya.resoluteaiassignment.databinding.ActivityCallingBinding
import com.chaitanya.resoluteaiassignment.model.CallModel
import com.chaitanya.resoluteaiassignment.model.CallModelType
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson


class CallingActivity : AppCompatActivity(),WebRTCRepository.Listener {
    private lateinit var binding: ActivityCallingBinding
    private lateinit var database : FirebaseDatabase
    val gson = Gson()
    private lateinit var user : String
    private var previousData: String? = null
    private lateinit var webRTCRepository : WebRTCRepository
    private var isCameraMuted = false
    private var isMicrophoneMuted = false

    private var requestPermission : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            val grantedPermissions = permissions.entries.filter { it.value }.map { it.key }.toTypedArray()

            if (grantedPermissions.contains(Manifest.permission.CAMERA) &&
                grantedPermissions.contains(Manifest.permission.RECORD_AUDIO)) {
                initiate()
                binding.lnlFull.visibility = View.VISIBLE
                binding.btnPermiLnl.visibility = View.GONE
            } else {
                binding.lnlFull.visibility = View.GONE
                binding.btnPermiLnl.visibility = View.VISIBLE
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarCallingActivity)

        requestPermission.launch(arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO))
        binding.givePermi.setOnClickListener {
            requestPermission.launch(arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun initiate() {
        webRTCRepository = WebRTCRepository(this)

        user = intent.getStringExtra("USER").toString()
        webRTCRepository.initializeWebRTCClient(user)
        database = FirebaseDatabase.getInstance()

        binding.ivProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
        binding.btCall.setOnClickListener {
            val target = binding.etCall.text.toString()
            webRTCRepository.sendCallRequest(target, object : ErrorCallBack {
                override fun onError() {
                    Toast.makeText(
                        this@CallingActivity,
                        "couldnt find the target",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
        webRTCRepository.initLocalView(binding.localView)
        webRTCRepository.initRemoteView(binding.remoteView)
        webRTCRepository.listener = this
        webRTCRepository.subscribeForLatestEvent (object :NewEventCallBack{
            override fun onNewEventReceived(model: CallModel?) {
                if (model!!.type === CallModelType.StartCall) {
                    runOnUiThread {
                        binding.tvCaller.text = model!!.sender

                        binding.lnlCll.visibility = View.VISIBLE
                        binding.btAns.setOnClickListener { v ->
                            //star the call here

//                           views.incomingCallLayout.setVisibility(View.GONE)
                        }
                        webRTCRepository.startCall(model.sender)
//                       views.rejectButton.setOnClickListener { v ->
//
//                       }
                    }
                }
            }

        })
        binding.switchCameraButton.setOnClickListener { v -> webRTCRepository.switchCamera() }

        binding.micButton.setOnClickListener { v ->
            if (isMicrophoneMuted) {
//                views.micButton.setImageResource(R.drawable.ic_baseline_mic_off_24)
            } else {
//                views.micButton.setImageResource(R.drawable.ic_baseline_mic_24)
            }
            webRTCRepository.toggleAudio(isMicrophoneMuted)
            isMicrophoneMuted = !isMicrophoneMuted
        }

        binding.videoButton.setOnClickListener { v ->
            if (isCameraMuted) {
//                binding.videoButton.setImageResource(R.drawable.ic_baseline_videocam_off_24)
            } else {
//                binding.videoButton.setImageResource(R.drawable.ic_baseline_videocam_24)
            }
            webRTCRepository.toggleVideo(isCameraMuted)
            isCameraMuted = !isCameraMuted
        }

        binding.endCallButton.setOnClickListener { v ->
            webRTCRepository.endCall()
            finish()
        }
    }

    override fun webrtcConnected() {

    }

    override fun webrtcClosed() {
        runOnUiThread(this::finish)
//        TODO("Not yet implemented")
    }
}
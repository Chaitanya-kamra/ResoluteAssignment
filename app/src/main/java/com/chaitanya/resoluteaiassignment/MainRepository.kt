package com.chaitanya.resoluteaiassignment


import android.content.Context
import android.util.Log
import com.chaitanya.resoluteaiassignment.model.DataModel
import com.chaitanya.resoluteaiassignment.model.DataModelType
import com.chaitanya.resoluteaiassignment.webrtc.MyPeerConnectionObserver
import com.chaitanya.resoluteaiassignment.webrtc.WebRTCClient
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer

class MainRepository private constructor() : WebRTCClient.Listener {

    interface Listener {
        fun webrtcConnected()
        fun webrtcClosed()
    }

    companion object {
        private var instance: MainRepository? = null

        fun getInstance(): MainRepository {
            if (instance == null) {
                instance = MainRepository()
            }
            return instance!!
        }
    }

    private val gson = Gson()
    private val firebaseClient = FirebaseClient()
    private var webRTCClient: WebRTCClient? = null
    private var currentUsername: String? = null
    private var remoteView: SurfaceViewRenderer? = null
    private var target: String? = null
    var repolistener: Listener? = null

    private fun updateCurrentUsername(username: String) {
        currentUsername = username
    }

    fun login(username: String, context: Context, callBack: () -> Unit) {
        firebaseClient.login(username) {
            updateCurrentUsername(username)
            webRTCClient = WebRTCClient(context, object : MyPeerConnectionObserver() {
                override fun onAddStream(mediaStream: MediaStream) {
                    super.onAddStream(mediaStream)
                    try {
                        mediaStream.videoTracks[0].addSink(remoteView)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                    Log.d("TAG", "onConnectionChange: $newState")
                    super.onConnectionChange(newState)
                    if (newState == PeerConnection.PeerConnectionState.CONNECTED && repolistener != null) {
                        Log.d("AVA","FAFA")
                        repolistener?.webrtcConnected()
                    }

                    if (newState == PeerConnection.PeerConnectionState.CLOSED ||
                        newState == PeerConnection.PeerConnectionState.DISCONNECTED
                    ) {
                        Log.d("AVA","FAFA")
                        repolistener?.webrtcClosed()
                    }
                }

                override fun onIceCandidate(iceCandidate: IceCandidate) {
                    super.onIceCandidate(iceCandidate)
                    webRTCClient?.sendIceCandidate(iceCandidate, target!!)
                }
            }, username)
            webRTCClient?.listener = this
            callBack()
        }
    }

    fun initLocalView(view: SurfaceViewRenderer) {
        webRTCClient?.initLocalSurfaceView(view)
    }

    fun initRemoteView(view: SurfaceViewRenderer) {
        webRTCClient?.initRemoteSurfaceView(view)
        remoteView = view
    }

    fun startCall(target: String) {
        webRTCClient?.call(target)
    }

    fun switchCamera() {
        webRTCClient?.switchCamera()
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        webRTCClient?.toggleAudio(shouldBeMuted)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        webRTCClient?.toggleVideo(shouldBeMuted)
    }

    fun sendCallRequest(target: String, errorCallBack: () -> Unit) {
        firebaseClient.sendMessageToOtherUser(
            DataModel(target, currentUsername!!,
                "", DataModelType.StartCall),
            errorCallBack
        )
    }

    fun endCall() {
        webRTCClient?.closeConnection()
    }

    fun subscribeForLatestEvent(newEventCallBack: (DataModel) -> Unit) {
        firebaseClient.observeIncomingLatestEvent { model ->
            when (model.type) {
                DataModelType.Offer -> {
                    this.target = model.sender
                    webRTCClient?.onRemoteSessionReceived(
                        SessionDescription(
                            SessionDescription.Type.OFFER,
                            model.data
                        )
                    )
                    webRTCClient?.answer(model.sender)
                }
                DataModelType.Answer -> {
                    this.target = model.sender
                    webRTCClient?.onRemoteSessionReceived(
                        SessionDescription(
                            SessionDescription.Type.ANSWER,
                            model.data
                        )
                    )
                }
                DataModelType.IceCandidate -> {
                    try {
                        val candidate = gson.fromJson(model.data, IceCandidate::class.java)
                        webRTCClient?.addIceCandidate(candidate)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                DataModelType.StartCall -> {
                    this.target = model.sender
                    newEventCallBack(model)
                }
            }
        }
    }

    override fun onTransferDataToOtherPeer(model: DataModel) {
        firebaseClient.sendMessageToOtherUser(model) {}
    }
}

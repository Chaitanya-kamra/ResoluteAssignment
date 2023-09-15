import android.content.Context
import android.util.Log
import com.chaitanya.resoluteaiassignment.ErrorCallBack
import com.chaitanya.resoluteaiassignment.FirebaseClient
import com.chaitanya.resoluteaiassignment.NewEventCallBack
import com.chaitanya.resoluteaiassignment.SuccessCallBack
import com.chaitanya.resoluteaiassignment.model.CallModel
import com.chaitanya.resoluteaiassignment.model.CallModelType
import com.chaitanya.resoluteaiassignment.webrtc.MyPeerObserver
import com.chaitanya.resoluteaiassignment.webrtc.WebRTCClient
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer

class WebRTCRepository(private val context: Context) : WebRTCClient.Listener {
    private val gson = Gson()
    private val firebaseClient = FirebaseClient()
    private lateinit var webRTCClient: WebRTCClient
    private var currentUsername: String = ""
    private lateinit var remoteView: SurfaceViewRenderer
    private var target: String = ""

    var listener: Listener? = null

    interface Listener {
        fun webrtcConnected()
        fun webrtcClosed()
    }

    fun loginUser(username: String, successCallBack: SuccessCallBack) {
        currentUsername = username
        firebaseClient.login(username, object : SuccessCallBack {
            override fun onSuccess() {
                // Successfully logged in, now initialize WebRTCClient
                initializeWebRTCClient(username)
                successCallBack.onSuccess()
            }
        })
    }

    fun initializeWebRTCClient(username: String) {
        webRTCClient = WebRTCClient(context, object : MyPeerObserver() {
            override fun onAddStream(mediaStream: MediaStream?) {
                super.onAddStream(mediaStream)
                try {
                    mediaStream?.videoTracks?.get(0)?.addSink(remoteView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                Log.d("TAG", "onConnectionChange: $newState")
                super.onConnectionChange(newState)
                if (newState == PeerConnection.PeerConnectionState.CONNECTED && listener != null) {
                    listener?.webrtcConnected()
                }

                if (newState == PeerConnection.PeerConnectionState.CLOSED ||
                    newState == PeerConnection.PeerConnectionState.DISCONNECTED
                ) {
                    if (listener != null) {
                        listener?.webrtcClosed()
                    }
                }
            }

            override fun onIceCandidate(iceCandidate: IceCandidate?) {
                super.onIceCandidate(iceCandidate)
                iceCandidate?.let { webRTCClient?.sendIceCandidate(it, target) }
            }
        }, username)
        webRTCClient?.listener = this
    }

    fun initLocalView(view: SurfaceViewRenderer) {
        webRTCClient.initLocalSurfaceView(view)
    }

    fun initRemoteView(view: SurfaceViewRenderer) {
        remoteView = view
        webRTCClient.initRemoteSurfaceView(view)

    }

    fun startCall(target: String) {
        webRTCClient.call(target)
    }

    fun switchCamera() {
        webRTCClient.switchCamera()
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        webRTCClient.toggleAudio(shouldBeMuted)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        webRTCClient.toggleVideo(shouldBeMuted)
    }

    fun sendCallRequest(target: String, errorCallBack: ErrorCallBack) {
        firebaseClient.sendMessageToOtherUser(
            CallModel(target, currentUsername, "", CallModelType.StartCall),
            errorCallBack
        )
    }

    fun endCall() {
        webRTCClient.closeConnection()
    }

    fun subscribeForLatestEvent(callBack: NewEventCallBack) {
        firebaseClient.observeIncomingLatestEvent(object : NewEventCallBack {
            override fun onNewEventReceived(model: CallModel?) {
                when (model!!.type) {
                    CallModelType.Offer -> {
                        target = model.sender
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.OFFER,
                                model!!.data
                            )
                        )
                        webRTCClient.answer(model.sender)
                    }

                    CallModelType.Answer -> {
                        target = model.sender
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.ANSWER,
                                model!!.data
                            )
                        )
                    }

                    CallModelType.IceCandidate -> {
                        try {
                            val candidate = gson.fromJson(model!!.data, IceCandidate::class.java)
                            webRTCClient.addIceCandidate(candidate)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    CallModelType.StartCall -> {
                        target = model!!.sender
                        callBack.onNewEventReceived(model)
                    }
                }

            }
        })


    }


    override fun onTransferDataToOtherPeer(model: CallModel) {
        firebaseClient.sendMessageToOtherUser(model,object : ErrorCallBack{
            override fun onError() {
//                TODO("Not yet implemented")
            }

        })
    }
}


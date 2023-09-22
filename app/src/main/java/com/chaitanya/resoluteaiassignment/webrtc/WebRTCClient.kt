package com.chaitanya.resoluteaiassignment.webrtc

import android.content.Context
import android.media.AudioManager
import com.chaitanya.resoluteaiassignment.model.DataModel
import com.chaitanya.resoluteaiassignment.model.DataModelType
import com.google.gson.Gson
import org.webrtc.*
import java.util.*

class WebRTCClient(context: Context, observer: PeerConnection.Observer, private val username: String) {

    private val gson = Gson()
    private val eglBaseContext: EglBase.Context = EglBase.create().eglBaseContext
    private val peerConnectionFactory: PeerConnectionFactory
    private val peerConnection: PeerConnection
    private val iceServer: MutableList<PeerConnection.IceServer> = ArrayList()
    private lateinit var videoCapturer: CameraVideoCapturer
    private var localVideoSource: VideoSource
    private var localAudioSource: AudioSource
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val localTrackId = "local_track"
    private val localStreamId = "local_stream"
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var localAudioTrack: AudioTrack
    private lateinit var localStream: MediaStream
    private val mediaConstraints = MediaConstraints()

    var listener: Listener? = null

    init {
        WebRTCClient.context = context
        initPeerConnectionFactory()
        peerConnectionFactory = createPeerConnectionFactory()
        iceServer.add(
            PeerConnection.IceServer.builder("turn:a.relay.metered.ca:443?transport=tcp")
                .setUsername("83eebabf8b4cce9d5dbcb649")
                .setPassword("2D7JvfkOQtBdYW3R")
                .createIceServer()
        )
        peerConnection = createPeerConnection(observer)
        localVideoSource = peerConnectionFactory.createVideoSource(false)
        audioManager.isSpeakerphoneOn = true
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        localAudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
    }

    // Initializing peer connection section
    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        val options = PeerConnectionFactory.Options()
        options.disableEncryption = false
        options.disableNetworkMonitor = false
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBaseContext))
            .setOptions(options)
            .createPeerConnectionFactory()
    }

    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection {
        return peerConnectionFactory.createPeerConnection(iceServer, observer)!!
    }

    // Initializing UI like surface view renderers

    fun initSurfaceViewRenderer(viewRenderer: SurfaceViewRenderer) {
        viewRenderer.setEnableHardwareScaler(true)
        viewRenderer.setMirror(true)
        viewRenderer.init(eglBaseContext, null)
    }

    fun initLocalSurfaceView(view: SurfaceViewRenderer) {
        initSurfaceViewRenderer(view)
        startLocalVideoStreaming(view)
    }

    private fun startLocalVideoStreaming(view: SurfaceViewRenderer) {
        val helper = SurfaceTextureHelper.create(
            Thread.currentThread().name, eglBaseContext
        )

        videoCapturer = getVideoCapturer()
        videoCapturer.initialize(helper, context, localVideoSource.capturerObserver)
        videoCapturer.startCapture(480, 360, 15)
        localVideoTrack = peerConnectionFactory.createVideoTrack(
            localTrackId+"_video", localVideoSource
        )
        localVideoTrack.addSink(view)

        localAudioTrack =
            peerConnectionFactory.createAudioTrack(localTrackId+"_audio", localAudioSource)
        localStream = peerConnectionFactory.createLocalMediaStream(localStreamId)
        localStream.addTrack(localVideoTrack)
        localStream.addTrack(localAudioTrack)
        peerConnection.addStream(localStream)
    }

    private fun getVideoCapturer(): CameraVideoCapturer {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames

        for (device in deviceNames) {
            if (enumerator.isFrontFacing(device)) {
                return enumerator.createCapturer(device, null)
            }
        }
        throw IllegalStateException("Front-facing camera not found")
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer) {
        initSurfaceViewRenderer(view)
    }

    // Negotiation section like call and answer
    fun call(target: String) {
        try {
            peerConnection.createOffer(object : MySdpObserver() {
                override fun onCreateSuccess(sessionDescription: SessionDescription) {
                    super.onCreateSuccess(sessionDescription)
                    peerConnection.setLocalDescription(object : MySdpObserver() {
                        override fun onSetSuccess() {
                            super.onSetSuccess()
                            // It's time to transfer this SDP to the other peer
                            listener?.onTransferDataToOtherPeer(
                                DataModel(
                                    target,
                                    username,
                                    sessionDescription.description,
                                    DataModelType.Offer
                                )
                            )
                        }
                    }, sessionDescription)
                }
            }, mediaConstraints)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun answer(target: String) {
        try {
            peerConnection.createAnswer(object : MySdpObserver() {
                override fun onCreateSuccess(sessionDescription: SessionDescription) {
                    super.onCreateSuccess(sessionDescription)
                    peerConnection.setLocalDescription(object : MySdpObserver() {
                        override fun onSetSuccess() {
                            super.onSetSuccess()
                            // It's time to transfer this SDP to the other peer
                            listener?.onTransferDataToOtherPeer(
                                DataModel(
                                    target,
                                    username,
                                    sessionDescription.description,
                                    DataModelType.Answer
                                )
                            )
                        }
                    }, sessionDescription)
                }
            }, mediaConstraints)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection.setRemoteDescription(MySdpObserver(), sessionDescription)
    }

    fun addIceCandidate(iceCandidate: IceCandidate) {
        peerConnection.addIceCandidate(iceCandidate)
    }

    fun sendIceCandidate(iceCandidate: IceCandidate, target: String) {
        addIceCandidate(iceCandidate)
        listener?.onTransferDataToOtherPeer(
            DataModel(
                target,
                username,
                gson.toJson(iceCandidate),
                DataModelType.IceCandidate
            )
        )
    }

    fun switchCamera() {
        videoCapturer.switchCamera(null)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        localVideoTrack.setEnabled(shouldBeMuted)
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        localAudioTrack.setEnabled(shouldBeMuted)
    }

    fun closeConnection() {
        try {
            localVideoTrack.dispose()
            videoCapturer.stopCapture()
            videoCapturer.dispose()
            peerConnection.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    interface Listener {
        fun onTransferDataToOtherPeer(model: DataModel)
    }

    companion object {
        private lateinit var context: Context
    }
}
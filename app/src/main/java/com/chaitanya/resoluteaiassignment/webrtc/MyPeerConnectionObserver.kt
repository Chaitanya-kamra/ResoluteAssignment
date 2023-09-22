package com.chaitanya.resoluteaiassignment.webrtc

import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver

open class MyPeerConnectionObserver : PeerConnection.Observer {
    override fun onSignalingChange(signalingState: PeerConnection.SignalingState) {
        // Implement your logic here
    }

    override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
        // Implement your logic here
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        // Implement your logic here
    }

    override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState) {
        // Implement your logic here
    }

    override fun onIceCandidate(iceCandidate: IceCandidate) {
        // Implement your logic here
    }

    override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
        // Implement your logic here
    }

    override fun onAddStream(mediaStream: MediaStream) {
        // Implement your logic here
    }

    override fun onRemoveStream(mediaStream: MediaStream) {
        // Implement your logic here
    }

    override fun onDataChannel(dataChannel: DataChannel) {
        // Implement your logic here
    }

    override fun onRenegotiationNeeded() {
        // Implement your logic here
    }

    override fun onAddTrack(rtpReceiver: RtpReceiver, mediaStreams: Array<MediaStream>) {
        // Implement your logic here
    }
}

package com.chaitanya.resoluteaiassignment

import com.chaitanya.resoluteaiassignment.model.CallModel
import com.google.firebase.database.*
import com.google.gson.Gson

class FirebaseClient {

    private val gson = Gson()
    private val dbRef = FirebaseDatabase.getInstance().reference
    private var currentUsername: String? = null
    private val LATEST_EVENT_FIELD_NAME = "latest_event"

    fun login(username: String, callBack: SuccessCallBack) {
        dbRef.child(username).setValue("")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    currentUsername = username
                    callBack.onSuccess()
                }
            }
    }

    fun sendMessageToOtherUser(CallModel: CallModel, errorCallBack: ErrorCallBack) {
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child(CallModel.target).exists()) {
                    // Send the signal to the other user
                    dbRef.child(CallModel.target).child(LATEST_EVENT_FIELD_NAME)
                        .setValue(gson.toJson(CallModel))
                } else {
                    errorCallBack.onError()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                errorCallBack.onError()
            }
        })
    }

    fun observeIncomingLatestEvent(callBack: NewEventCallBack) {
        currentUsername?.let {
            dbRef.child(it).child(LATEST_EVENT_FIELD_NAME).addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val data = snapshot.value?.toString()
                            val callModel = gson.fromJson(data, CallModel::class.java)
                            callBack.onNewEventReceived(callModel)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
            )
        }
    }
}

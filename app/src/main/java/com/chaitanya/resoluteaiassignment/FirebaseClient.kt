package com.chaitanya.resoluteaiassignment

import android.util.Log
import com.chaitanya.resoluteaiassignment.model.DataModel
import com.google.firebase.database.*
import com.google.gson.Gson
import java.util.Objects

class FirebaseClient {
    private val gson = Gson()
    private val dbRef = FirebaseDatabase.getInstance().reference
    private var currentUsername: String? = null
    private val LATEST_EVENT_FIELD_NAME = "latest_event"

    fun login(username: String, callBack: ()-> Unit) {
        dbRef.child(username).setValue("").addOnCompleteListener { task ->
            currentUsername = username
            callBack()
        }
    }

    fun sendMessageToOtherUser(dataModel: DataModel, errorCallBack: () -> Unit) {
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child(dataModel.target).exists()) {
                    // Send the signal to the other user
                    Log.e("Fa","fddd")
                    dbRef.child(dataModel.target)
                        .child(LATEST_EVENT_FIELD_NAME)
                        .setValue(gson.toJson(dataModel))
                } else {
                    Log.e("Fa","err")
                    errorCallBack()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                errorCallBack()
            }
        })
    }

    fun observeIncomingLatestEvent(newEvent: (DataModel) -> Unit) {
        dbRef.child(currentUsername!!).child(LATEST_EVENT_FIELD_NAME)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val data = Objects.requireNonNull(snapshot.value).toString()
                        val dataModel = gson.fromJson(data, DataModel::class.java)
                        newEvent(dataModel)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}

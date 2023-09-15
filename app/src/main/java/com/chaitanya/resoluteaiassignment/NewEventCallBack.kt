package com.chaitanya.resoluteaiassignment

import com.chaitanya.resoluteaiassignment.model.CallModel

interface NewEventCallBack {
    fun onNewEventReceived(model: CallModel?)
}

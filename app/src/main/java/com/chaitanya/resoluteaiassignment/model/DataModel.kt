package com.chaitanya.resoluteaiassignment.model

import com.chaitanya.resoluteaiassignment.model.DataModelType

data class DataModel(
    val target:String,
    val sender: String,
    val data:String,
    val type: DataModelType
)
package com.example.memoapp.model

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class CanvasElement(
    var id: String = "",
    @get:PropertyName("user_id") @set:PropertyName("user_id")
    var userId: String = "",
    var type: String = "RECTANGLE", // RECTANGLE, CIRCLE, TEXT
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 100f,
    var height: Float = 100f,
    var text: String = "",
    var color: Int = 0xFF000000.toInt(),
    @get:PropertyName("created_at") @set:PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis()
) : Serializable

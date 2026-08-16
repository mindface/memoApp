package com.example.memoapp.model

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class CanvasElement(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("user_id") @set:PropertyName("user_id")
    var userId: String = "",

    @get:PropertyName("concept_id") @set:PropertyName("concept_id")
    var conceptId: String = "",

    @get:PropertyName("type") @set:PropertyName("type")
    var type: String = "RECTANGLE", // RECTANGLE, CIRCLE, TEXT

    @get:PropertyName("x") @set:PropertyName("x")
    var x: Float = 0f,

    @get:PropertyName("y") @set:PropertyName("y")
    var y: Float = 0f,

    @get:PropertyName("width") @set:PropertyName("width")
    var width: Float = 100f,

    @get:PropertyName("height") @set:PropertyName("height")
    var height: Float = 100f,

    @get:PropertyName("text") @set:PropertyName("text")
    var text: String = "",

    @get:PropertyName("color") @set:PropertyName("color")
    var color: Int = 0xFF000000.toInt(),

    @get:PropertyName("font_size") @set:PropertyName("font_size")
    var fontSize: Float = 60f,

    @get:PropertyName("z_index") @set:PropertyName("z_index")
    var zIndex: Int = 0,

    @get:PropertyName("created_at") @set:PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis()
) : Serializable

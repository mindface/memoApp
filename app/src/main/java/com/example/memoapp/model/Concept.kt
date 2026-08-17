package com.example.memoapp.model

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class Concept(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("user_id") @set:PropertyName("user_id")
    var userId: String = "",

    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("created_at") @set:PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis(),

    @get:PropertyName("updated_at") @set:PropertyName("updated_at")
    var updatedAt: Long = System.currentTimeMillis(),

    @get:PropertyName("last_view_x") @set:PropertyName("last_view_x")
    var lastViewX: Float = 0f,

    @get:PropertyName("last_view_y") @set:PropertyName("last_view_y")
    var lastViewY: Float = 0f,

    @get:PropertyName("last_view_scale") @set:PropertyName("last_view_scale")
    var lastViewScale: Float = 1f
) : Serializable

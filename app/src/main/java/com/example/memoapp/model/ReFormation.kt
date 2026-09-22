package com.example.memoapp.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class ReFormation(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("user_id") @set:PropertyName("user_id")
    var userId: String = "",

    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("created_at") @set:PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis(),

    @get:PropertyName("updated_at") @set:PropertyName("updated_at")
    var updatedAt: Long = System.currentTimeMillis()
) : Serializable

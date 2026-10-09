package com.example.memoapp.model

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class CounterElement(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("user_id") @set:PropertyName("user_id")
    var userId: String = "",

    @get:PropertyName("measurement_page_id") @set:PropertyName("measurement_page_id")
    var measurementPageId: String = "",

    @get:PropertyName("x") @set:PropertyName("x")
    var x: Float = 0f,

    @get:PropertyName("y") @set:PropertyName("y")
    var y: Float = 0f,

    @get:PropertyName("width") @set:PropertyName("width")
    var width: Float = 180f,

    @get:PropertyName("height") @set:PropertyName("height")
    var height: Float = 100f,

    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("count") @set:PropertyName("count")
    var count: Int = 0,

    @get:PropertyName("symbol_id") @set:PropertyName("symbol_id")
    var symbolId: String? = null,

    @get:PropertyName("linked_item_id") @set:PropertyName("linked_item_id")
    var linkedItemId: String? = null,

    @get:PropertyName("linked_item_type") @set:PropertyName("linked_item_type")
    var linkedItemType: String? = null, // NOTE, LOG_ITEM, CONCEPT, SYMBOL

    @get:PropertyName("z_index") @set:PropertyName("z_index")
    var zIndex: Int = 0,

    @get:PropertyName("color") @set:PropertyName("color")
    var color: Int = 0xFFFFFFFF.toInt(),

    @get:PropertyName("font_size") @set:PropertyName("font_size")
    var fontSize: Float = 20f,

    @get:PropertyName("created_at") @set:PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis()
) : Serializable

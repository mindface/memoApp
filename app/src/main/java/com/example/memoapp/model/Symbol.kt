package com.example.memoapp.model

import java.io.Serializable

data class Symbol(
    var id: String = "",
    var userId: String = "",
    var title: String = "",
    var content: String = "",
    var created_at: String = "",
    var updated_at: String = "",
    var symbolType: String = "",
    var extension: String = "",
    var language: String = ""
) : Serializable

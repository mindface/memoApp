package com.example.memoapp.model

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class Note(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("user_id") @set:PropertyName("user_id")
    var userId: String = "",

    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("content") @set:PropertyName("content")
    var content: String = "",

    @get:PropertyName("created_at") @set:PropertyName("created_at")
    var created_at: Any? = "",

    @get:PropertyName("updated_at") @set:PropertyName("updated_at")
    var updated_at: Any? = ""
) : Serializable {
    // Helper to get as String for UI
    fun getCreatedAtString(): String = created_at?.toString() ?: ""
    fun getUpdatedAtString(): String = updated_at?.toString() ?: ""

    // Backward compatibility for reading old 'userId' field
    @get:PropertyName("userId") @set:PropertyName("userId")
    var userIdOld: String
        get() = userId
        set(value) { 
            if (value.isNotEmpty() && userId.isEmpty()) {
                userId = value 
            }
        }
}

data class ConceptView(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = ""
)

data class RelationView(
    val fromId: String = "",
    val toId: String = ""
)

data class NoteData(
    val note: Note = Note(),
    val concepts: List<ConceptView> = emptyList(),
    val relations: List<RelationView> = emptyList()
)

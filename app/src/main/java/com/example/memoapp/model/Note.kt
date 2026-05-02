package com.example.memoapp.model

import java.io.Serializable

data class Note(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val created_at: String = "",
    val updated_at: String = ""
) : Serializable

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

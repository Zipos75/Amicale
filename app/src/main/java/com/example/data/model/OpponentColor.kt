package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "opponent_colors")
data class OpponentColor(
    @PrimaryKey
    val clubKey: String, // e.g. "Pingouin", "Waterloo Ducks"
    val colorHex: String
)

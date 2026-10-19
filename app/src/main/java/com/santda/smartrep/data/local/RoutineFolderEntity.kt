package com.santda.smartrep.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routine_folder")
data class RoutineFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
)
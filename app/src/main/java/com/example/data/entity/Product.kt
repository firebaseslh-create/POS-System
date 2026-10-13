package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val barcode: String = "",
    val costPrice: Long = 0,
    val sellingPrice: Long = 0,
    val stockQuantity: Int = 0,
    val wholesalePrice: Long = 0,
    val lowStockThreshold: Int = 5,
    val unit: String = "ခု",
    val colorTag: String = "#059669",
    val isAvailable: Boolean = true,
    val currency: String = "MMK",
    val imageUri: String = ""
)

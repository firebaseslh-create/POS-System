package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.OrderDao
import com.example.data.dao.ProductDao
import com.example.data.dao.SavedReceiptDao
import com.example.data.entity.Order
import com.example.data.entity.OrderItem
import com.example.data.entity.Product
import com.example.data.entity.SavedReceipt

@Database(
    entities = [Product::class, Order::class, OrderItem::class, SavedReceipt::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun savedReceiptDao(): SavedReceiptDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myanmar_pos_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

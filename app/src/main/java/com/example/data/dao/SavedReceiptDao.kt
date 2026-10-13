package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.SavedReceipt
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedReceiptDao {
    @Query("SELECT * FROM saved_receipts ORDER BY clearedTimestamp DESC")
    fun getAllSavedReceipts(): Flow<List<SavedReceipt>>

    @Query("SELECT * FROM saved_receipts WHERE id = :id")
    suspend fun getSavedReceiptById(id: Long): SavedReceipt?

    @Query("SELECT * FROM saved_receipts WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getSavedReceiptByInvoiceNumber(invoiceNumber: String): SavedReceipt?

    @Query("SELECT COUNT(*) FROM saved_receipts WHERE invoiceNumber = :invoiceNumber")
    suspend fun countByInvoiceNumber(invoiceNumber: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSavedReceipt(receipt: SavedReceipt): Long

    @Query("DELETE FROM saved_receipts WHERE id = :id")
    suspend fun deleteSavedReceiptById(id: Long)

    @Query("DELETE FROM saved_receipts")
    suspend fun deleteAllSavedReceipts()

    @Query("SELECT COUNT(*) FROM saved_receipts")
    fun getSavedReceiptsCount(): Flow<Int>
}

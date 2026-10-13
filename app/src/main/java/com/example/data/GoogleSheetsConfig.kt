package com.example.data

enum class SyncStatus(val labelEn: String, val labelMm: String) {
    IDLE("Idle", "အဆင်သင့်ဖြစ်"),
    SYNCING("Syncing...", "စင့်ခ်လုပ်နေပါသည်..."),
    SUCCESS("Sync Success", "အောင်မြင်ပါသည်"),
    FAILED("Sync Failed", "မအောင်မြင်ပါ")
}

data class GoogleSheetsConfig(
    val scriptUrl: String = DEFAULT_SCRIPT_URL,
    val sheetName: String = DEFAULT_SHEET_NAME,
    val folderName: String = "",
    val folderId: String = "",
    val shopName: String = "POS System Software",
    val autoSyncSales: Boolean = false,
    val periodicAutoSync: Boolean = false,
    val syncIntervalSeconds: Int = 30,
    val sheetUrl: String = "",
    val lastSyncTime: Long = 0L,
    val lastSyncStatus: String = "စတင်အသုံးပြုရန် အဆင်သင့်ဖြစ်ပါသည်",
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val pendingOfflineActionsCount: Int = 0
) {
    companion object {
        const val DEFAULT_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbwzf_NqXd3BGcE4B5GwP12HgFQtx3tEOigroWg7sv1tMh2JsMiWhDuII22gsZwA4Vue/exec"
        const val DEFAULT_SHEET_NAME = "POS Retail Sales & Inventory"
    }
}

data class OfflineSyncAction(
    val id: String,
    val timestamp: Long,
    val dateTime: String,
    val actionType: String,
    val descriptionMm: String,
    val targetInvoiceNumber: String = "",
    val targetProductBarcode: String = "",
    val targetProductName: String = "",
    val payloadJson: String = "{}"
) {
    val description: String get() = descriptionMm
}


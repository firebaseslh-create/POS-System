package com.example.data

import com.example.data.entity.Product

object InitialData {
    fun getSampleProducts(): List<Product> = listOf(
        // အစားအစာ (Food & Groceries - MMK)
        Product(
            name = "ပေါ်ဆန်းမွှေး ဆန် (၅ ကီလို)",
            category = "အစားအစာ",
            barcode = "NTR-000001",
            costPrice = 18500,
            sellingPrice = 22000,
            wholesalePrice = 20500,
            stockQuantity = 45,
            lowStockThreshold = 10,
            unit = "အိတ်",
            currency = "MMK",
            imageUri = "android.resource://com.example/drawable/img_product_mmk"
        ),
        Product(
            name = "နေကြာဆီ သန့် (၁ လီတာ)",
            category = "အစားအစာ",
            barcode = "NTR-000002",
            costPrice = 7200,
            sellingPrice = 8500,
            wholesalePrice = 8000,
            stockQuantity = 60,
            lowStockThreshold = 10,
            unit = "ဗူး",
            currency = "MMK",
            imageUri = "android.resource://com.example/drawable/img_product_mmk"
        ),
        Product(
            name = "ကြက်ဥ (၁၀ လုံးတွဲ)",
            category = "အစားအစာ",
            barcode = "NTR-000003",
            costPrice = 3200,
            sellingPrice = 4000,
            wholesalePrice = 3700,
            stockQuantity = 50,
            lowStockThreshold = 8,
            unit = "ကတ်",
            currency = "MMK",
            imageUri = "android.resource://com.example/drawable/img_product_mmk"
        ),
        Product(
            name = "လက်ဖက်အကြော်စုံ (အထုပ်ကြီး)",
            category = "အစားအစာ",
            barcode = "NTR-000004",
            costPrice = 2500,
            sellingPrice = 3500,
            wholesalePrice = 3000,
            stockQuantity = 80,
            lowStockThreshold = 10,
            unit = "ထုပ်",
            currency = "MMK",
            imageUri = "android.resource://com.example/drawable/img_product_mmk"
        ),

        // အဖျော်ယမကာ (Beverages - MMK)
        Product(
            name = "ရွှေဖီဦး လက်ဖက်ရည်ထုပ် (၃၀ ထုပ်ပါ)",
            category = "အဖျော်ယမကာ",
            barcode = "NTR-000005",
            costPrice = 5800,
            sellingPrice = 7000,
            wholesalePrice = 6500,
            stockQuantity = 75,
            lowStockThreshold = 10,
            unit = "ထုပ်",
            currency = "MMK",
            imageUri = "android.resource://com.example/drawable/img_product_mmk"
        ),
        Product(
            name = "ပရီးမီးယား ကော်ဖီမစ် (3 in 1)",
            category = "အဖျော်ယမကာ",
            barcode = "NTR-000006",
            costPrice = 5200,
            sellingPrice = 6500,
            wholesalePrice = 6000,
            stockQuantity = 90,
            lowStockThreshold = 12,
            unit = "ထုပ်",
            currency = "MMK",
            imageUri = "android.resource://com.example/drawable/img_product_mmk"
        ),
        Product(
            name = "သောက်ရေသန့် (၁ လီတာ)",
            category = "အဖျော်ယမကာ",
            barcode = "NTR-000007",
            costPrice = 400,
            sellingPrice = 600,
            wholesalePrice = 500,
            stockQuantity = 120,
            lowStockThreshold = 20,
            unit = "ဗူး",
            currency = "MMK",
            imageUri = "android.resource://com.example/drawable/img_product_mmk"
        ),

        // လူသုံးကုန် (Household & Personal Care - MMK)
        Product(
            name = "သွားတိုက်ဆေး နှင့် သွားတိုက်တံတွဲ",
            category = "လူသုံးကုန်",
            barcode = "NTR-000008",
            costPrice = 2800,
            sellingPrice = 3800,
            wholesalePrice = 3400,
            stockQuantity = 40,
            lowStockThreshold = 5,
            unit = "ဘူး",
            currency = "MMK",
            imageUri = "android.resource://com.example/drawable/img_product_mmk"
        ),
        Product(
            name = "ဆပ်ပြာမှုန့် (၁ ကီလိုထုပ်)",
            category = "လူသုံးကုန်",
            barcode = "NTR-000009",
            costPrice = 3500,
            sellingPrice = 4500,
            wholesalePrice = 4100,
            stockQuantity = 55,
            lowStockThreshold = 8,
            unit = "ထုပ်",
            currency = "MMK",
            imageUri = "android.resource://com.example/drawable/img_product_mmk"
        ),
        Product(
            name = "ပန်းကန်ဆေးဆပ်ပြာရည် (အကြီး)",
            category = "လူသုံးကုန်",
            barcode = "NTR-000010",
            costPrice = 2000,
            sellingPrice = 2800,
            wholesalePrice = 2500,
            stockQuantity = 65,
            lowStockThreshold = 10,
            unit = "ဗူး",
            currency = "MMK",
            imageUri = "android.resource://com.example/drawable/img_product_mmk"
        ),

        // ထိုင်းကုန်ပစ္စည်း (Thai Products - THB)
        Product(
            name = "MAMA ခေါက်ဆွဲခြောက် (ဖာ)",
            category = "ထိုင်းကုန်ပစ္စည်း",
            barcode = "THB-000001",
            costPrice = 165,
            sellingPrice = 210,
            wholesalePrice = 195,
            stockQuantity = 35,
            lowStockThreshold = 5,
            unit = "ဖာ",
            currency = "THB",
            imageUri = "android.resource://com.example/drawable/img_product_thb"
        ),
        Product(
            name = "Carabao အားဖြည့်အချိုရည် (ကတ်)",
            category = "ထိုင်းကုန်ပစ္စည်း",
            barcode = "THB-000002",
            costPrice = 95,
            sellingPrice = 120,
            wholesalePrice = 110,
            stockQuantity = 50,
            lowStockThreshold = 8,
            unit = "ကတ်",
            currency = "THB",
            imageUri = "android.resource://com.example/drawable/img_product_thb"
        ),
        Product(
            name = "Lactasoy ပဲနို့ဗူး (၆ ဗူးတွဲ)",
            category = "ထိုင်းကုန်ပစ္စည်း",
            barcode = "THB-000003",
            costPrice = 52,
            sellingPrice = 70,
            wholesalePrice = 62,
            stockQuantity = 45,
            lowStockThreshold = 6,
            unit = "တွဲ",
            currency = "THB",
            imageUri = "android.resource://com.example/drawable/img_product_thb"
        ),
        Product(
            name = "ထိုင်းငံပြာရည် (Tiparos အကြီး)",
            category = "ထိုင်းကုန်ပစ္စည်း",
            barcode = "THB-000004",
            costPrice = 32,
            sellingPrice = 45,
            wholesalePrice = 40,
            stockQuantity = 60,
            lowStockThreshold = 10,
            unit = "ပုလင်း",
            currency = "THB",
            imageUri = "android.resource://com.example/drawable/img_product_thb"
        ),
        Product(
            name = "Fineline အဝတ်လျှော်ဆပ်ပြာရည်",
            category = "ထိုင်းကုန်ပစ္စည်း",
            barcode = "THB-000005",
            costPrice = 68,
            sellingPrice = 90,
            wholesalePrice = 82,
            stockQuantity = 40,
            lowStockThreshold = 5,
            unit = "ထုပ်",
            currency = "THB",
            imageUri = "android.resource://com.example/drawable/img_product_thb"
        ),
        Product(
            name = "Babi Mild ကလေးပေါင်ဒါဗူးကြီး",
            category = "ထိုင်းကုန်ပစ္စည်း",
            barcode = "THB-000006",
            costPrice = 48,
            sellingPrice = 65,
            wholesalePrice = 58,
            stockQuantity = 30,
            lowStockThreshold = 5,
            unit = "ဗူး",
            currency = "THB",
            imageUri = "android.resource://com.example/drawable/img_product_thb"
        )
    )
}




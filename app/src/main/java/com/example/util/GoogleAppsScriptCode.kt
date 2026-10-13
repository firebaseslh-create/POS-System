package com.example.util

object GoogleAppsScriptCode {

    fun getDeploymentInstructions(): String = """
=== Google Apps Script တပ်ဆင်နည်း လမ်းညွှန် ===

၁။ ကွန်ပျူတာ သို့မဟုတ် ဖုန်း Browser တွင် script.google.com သို့ သွားပါ။
၂။ "+ New project" (ပရောဂျက်အသစ်) ကို နှိပ်ပါ။
၃။ ပရောဂျက်အမည်ကို "Myanmar POS API" ဟု ပေးပါ။
၄။ Code.gs ထဲရှိ မူလစာသားများကို ဖျက်ပြီး အောက်ပါ Apps Script ကုဒ်ကို ကူးယူ (Copy) ၍ ထည့် (Paste) ပါ။
၅။ ထိပ်ရှိ "Save" (သိမ်းဆည်းရန်) icon ကို နှိပ်ပါ။
၆။ ညာဘက်ထိပ်ရှိ "Deploy" > "New deployment" ကို နှိပ်ပါ။
၇။ ဘယ်ဘက်齒 (Gear icon) မှ "Web app" ကို ရွေးချယ်ပါ။
၈။ သတ်မှတ်ချက်များ ဖြည့်ပါ -
   - Description: "POS Web App"
   - Execute as: "Me (မိမိအီးမေးလ်)"
   - Who has access: "Anyone" (မဖြစ်မနေ Anyone ဟု ရွေးချယ်ပေးပါ)
၉။ "Deploy" ခလုတ်ကို နှိပ်ပြီး "Authorize access" ပေးပါ။ (Advanced > Go to Myanmar POS API (unsafe) > Allow ကို နှိပ်ပေးပါ)
၁၀။ ရရှိလာသော "Web app URL" (ဥပမာ - https://script.google.com/macros/s/.../exec) ကို ကူးယူပြီး ဤ POS App ထဲရှိ "Google Apps Script Web App URL" နေရာတွင် ထည့်သွင်း အသုံးပြုပါ။
""".trimIndent()

    fun getScriptCode(): String = """
/**
 * Myanmar POS System - Google Sheets & Google Drive Integration API
 * Automatically manages Sheets tabs, formats tables, records sales, products,
 * monthly/confirmed summaries, and uploads receipts to Google Drive.
 */

function doPost(e) {
  try {
    var rawData = e.postData ? e.postData.contents : null;
    if (!rawData && e.parameter && e.parameter.data) {
      rawData = e.parameter.data;
    }
    if (!rawData) {
      return jsonResponse({ status: "error", message: "No data received" });
    }

    var payload = JSON.parse(rawData);
    var action = payload.action || "test";
    var sheetName = payload.sheetName || "POS Retail Sales & Inventory";
    var sheetUrl = payload.sheetUrl || "";
    var folderId = payload.folderId || "";
    var shopName = payload.shopName || "POS System";

    // Open or create the spreadsheet
    var ss = getOrCreateSpreadsheet(sheetName, folderId, sheetUrl);
    ensureAllSheets(ss, shopName);

    switch (action) {
      case "test":
        return jsonResponse({
          status: "success",
          message: "Google Sheets & Apps Script ချိတ်ဆက်မှု အောင်မြင်ပါသည်!",
          sheetUrl: ss.getUrl()
        });

      case "init_sheets":
        ensureAllSheets(ss, shopName);
        return jsonResponse({
          status: "success",
          message: "Sheet Tabs များနှင့် ဇယားကွက်များ အောင်မြင်စွာ တည်ဆောက်ပြီးပါပြီ",
          sheetUrl: ss.getUrl()
        });

      case "sync_products":
        var products = payload.products || [];
        var deletedProducts = payload.deletedProducts || [];
        if (deletedProducts.length > 0) {
          deleteProductsFromSheet(ss, deletedProducts);
        }
        syncProductsSheet(ss, products, folderId);
        var updatedProds = fetchProductsFromSheet(ss);
        return jsonResponse({
          status: "success",
          message: "ကုန်ပစ္စည်းစာရင်း " + products.length + " မျိုးကို Sheets သို့ သိမ်းဆည်းပြီးပါပြီ",
          sheetUrl: ss.getUrl(),
          products: updatedProds
        });

      case "delete_product":
        var delProd = payload.product || {};
        deleteProductsFromSheet(ss, [delProd]);
        if (payload.products) {
          syncProductsSheet(ss, payload.products, folderId);
        }
        appendActionLog(ss, "DELETE_PRODUCT", "ပစ္စည်းဖျက်ခြင်း: " + (delProd.name || delProd.barcode || ""), "", delProd.barcode || "", delProd.name || "");
        return jsonResponse({
          status: "success",
          message: "ပစ္စည်း '" + (delProd.name || "") + "' ကို Google Sheets မှ ဖျက်ပြီးပါပြီ",
          sheetUrl: ss.getUrl(),
          products: fetchProductsFromSheet(ss)
        });

      case "sync_orders":
        var orders = payload.orders || [];
        syncOrdersSheet(ss, orders);
        return jsonResponse({
          status: "success",
          message: "အရောင်းမှတ်တမ်း " + orders.length + " စောင်ကို Sheets သို့ သိမ်းဆည်းပြီးပါပြီ",
          sheetUrl: ss.getUrl()
        });

      case "sync_single_order":
        var order = payload.order;
        if (order) {
          var isAlreadyInSheet = orderExistsInSheet(ss, order.invoiceNumber);
          upsertSingleOrder(ss, order, folderId);
          // Deduct sold item quantities if not already synced via full products array
          if (!isAlreadyInSheet && (!payload.products || payload.products.length === 0) && order.items && order.items.length > 0 && order.status !== "REFUNDED") {
            adjustProductStocksInSheet(ss, order.items, -1);
          }
          appendActionLog(ss, "CHECKOUT_ORDER", "အရောင်းဘောက်ချာအသစ် (" + (order.invoiceNumber || "") + ") - ရောင်းပြီးပစ္စည်း လက်ကျန်နုတ်ယူပြီး", order.invoiceNumber || "", "", order.itemsSummary || "");
        }
        if (payload.products) {
          syncProductsSheet(ss, payload.products, folderId);
        }
        return jsonResponse({
          status: "success",
          message: "အရောင်းမှတ်တမ်းနှင့် လက်ကျန်ပစ္စည်းစာရင်းအား Google Sheets သို့ အလိုအလျောက် မှတ်တမ်းတင်ပြီးပါပြီ",
          sheetUrl: ss.getUrl()
        });

      case "refund_order":
        var refOrder = payload.order || {};
        var refInvoice = payload.invoiceNumber || refOrder.invoiceNumber || "";
        var refItems = payload.items || refOrder.items || [];
        updateOrderStatusInSheet(ss, refInvoice, "REFUNDED", refOrder);
        if (payload.products) {
          syncProductsSheet(ss, payload.products, folderId);
        } else if (refItems && refItems.length > 0) {
          adjustProductStocksInSheet(ss, refItems, 1);
        }
        appendActionLog(ss, "REFUND_ORDER", "ငွေပြန်အမ်းခြင်း (" + refInvoice + ") - ပစ္စည်းလက်ကျန် မူလအတိုင်း ပြန်ပေါင်းထည့်ပြီး", refInvoice, "", "");
        return jsonResponse({
          status: "success",
          message: "ငွေပြန်အမ်းမှုနှင့် လက်ကျန်ပစ္စည်း မူလအတိုင်း ပြန်ထားရှိမှုကို Google Sheets တွင် ပြင်ဆင်ပြီးပါပြီ",
          sheetUrl: ss.getUrl()
        });

      case "undo_refund_order":
        var undoOrder = payload.order || {};
        var undoInvoice = payload.invoiceNumber || undoOrder.invoiceNumber || "";
        var undoItems = payload.items || undoOrder.items || [];
        updateOrderStatusInSheet(ss, undoInvoice, "COMPLETED", undoOrder);
        if (payload.products) {
          syncProductsSheet(ss, payload.products, folderId);
        } else if (undoItems && undoItems.length > 0) {
          adjustProductStocksInSheet(ss, undoItems, -1);
        }
        appendActionLog(ss, "UNDO_REFUND_ORDER", "ငွေပြန်အမ်းမှု ပြန်ဖျက်ခြင်း (" + undoInvoice + ") - မူလအရောင်းအတိုင်း ပြန်ထားပြီး", undoInvoice, "", "");
        return jsonResponse({
          status: "success",
          message: "မူလအရောင်းမှတ်တမ်းအတိုင်း Google Sheets တွင် ပြန်လည်ထားရှိပြီးပါပြီ",
          sheetUrl: ss.getUrl()
        });

      case "delete_order":
        var delInvoice = payload.invoiceNumber || "";
        var wasRefunded = payload.wasRefunded === true;
        var delOrderItems = payload.items || [];
        deleteOrderFromSheetByInvoice(ss, delInvoice);
        if (payload.products) {
          syncProductsSheet(ss, payload.products, folderId);
        } else if (!wasRefunded && delOrderItems.length > 0) {
          adjustProductStocksInSheet(ss, delOrderItems, 1);
        }
        appendActionLog(ss, "DELETE_ORDER", "အရောင်းဘောက်ချာ ဖျက်ခြင်း (" + delInvoice + ")", delInvoice, "", "");
        return jsonResponse({
          status: "success",
          message: "အရောင်းဘောက်ချာ (" + delInvoice + ") ကို Google Sheets မှ ဖျက်ပြီးပါပြီ",
          sheetUrl: ss.getUrl()
        });

      case "sync_saved_receipts":
        var savedReceipts = payload.savedReceipts || [];
        syncSavedReceiptsSheet(ss, savedReceipts);
        return jsonResponse({
          status: "success",
          message: "ရှင်းပြီးပြေစာများ မှတ်တမ်း " + savedReceipts.length + " စောင်ကို Sheets သို့ သိမ်းဆည်းပြီးပါပြီ",
          sheetUrl: ss.getUrl()
        });

      case "sync_single_saved_receipt":
        var savedReceipt = payload.savedReceipt;
        if (savedReceipt) {
          appendSingleSavedReceipt(ss, savedReceipt);
          if (payload.removeFromActiveOrders === true && savedReceipt.invoiceNumber) {
            deleteOrderFromSheetByInvoice(ss, savedReceipt.invoiceNumber);
          }
          appendActionLog(ss, "CLEAR_TO_SAVED_RECEIPT", "ရှင်းပြီးပြေစာသို့ ရွှေ့ပြောင်းသိမ်းဆည်းခြင်း (" + (savedReceipt.invoiceNumber || "") + ")", savedReceipt.invoiceNumber || "", "", "");
        }
        return jsonResponse({
          status: "success",
          message: "ရှင်းပြီးပြေစာ အသစ်အား Google Sheets သို့ အောင်မြင်စွာ သိမ်းဆည်းပြီးပါပြီ",
          sheetUrl: ss.getUrl()
        });

      case "delete_saved_receipt":
        var delSavedInv = payload.invoiceNumber || "";
        deleteSavedReceiptFromSheetByInvoice(ss, delSavedInv);
        appendActionLog(ss, "DELETE_SAVED_RECEIPT", "ရှင်းပြီးပြေစာ ဖျက်ခြင်း (" + delSavedInv + ")", delSavedInv, "", "");
        return jsonResponse({
          status: "success",
          message: "ရှင်းပြီးပြေစာ (" + delSavedInv + ") ကို Google Sheets မှ ဖျက်ပြီးပါပြီ",
          sheetUrl: ss.getUrl()
        });

      case "sync_summaries":
        var salesSummaries = payload.salesSummaries || [];
        var savedSummaries = payload.savedReceiptsSummaries || [];
        syncSalesSummarySheet(ss, salesSummaries);
        syncSavedSummarySheet(ss, savedSummaries);
        return jsonResponse({
          status: "success",
          message: "အရောင်းနှင့် ရှင်းပြီးပြေစာများ စာရင်းချုပ်များကို Sheets သို့ သိမ်းဆည်းပြီးပါပြီ",
          sheetUrl: ss.getUrl()
        });

      case "sync_rates":
        var rates = payload.rates || {};
        syncRatesSheet(ss, rates);
        if (payload.shopProfile) {
          syncShopProfileSheet(ss, payload.shopProfile);
        }
        return jsonResponse({
          status: "success",
          message: "မြန်မာကျပ် နှင့် ဘတ်ငွေ လဲလှယ် နှူန်းများကို Sheets သို့ အောင်မြင်စွာ သိမ်းဆည်းပြီးပါပြီ",
          sheetUrl: ss.getUrl()
        });

      case "sync_shop_profile":
        var shopProf = payload.shopProfile || {};
        syncShopProfileSheet(ss, shopProf);
        if (payload.rates) {
          syncRatesSheet(ss, payload.rates);
        }
        return jsonResponse({
          status: "success",
          message: "ဆိုင်အချက်အလက်များကို Sheets သို့ သိမ်းဆည်းပြီးပါပြီ",
          sheetUrl: ss.getUrl()
        });

      case "replay_offline_queue":
        var queueActions = payload.offlineQueue || [];
        var replayedCount = processOfflineQueueInOrder(ss, queueActions, folderId);
        // If current screen snapshots are also provided, mirror them after ordered replay so Google Sheets matches the exact UI screen state
        if (payload.mirrorScreenState === true) {
          if (payload.products) syncProductsSheet(ss, payload.products, folderId);
          if (payload.orders) syncOrdersSheet(ss, payload.orders);
          if (payload.savedReceipts) syncSavedReceiptsSheet(ss, payload.savedReceipts);
          if (payload.salesSummaries) syncSalesSummarySheet(ss, payload.salesSummaries);
          if (payload.savedReceiptsSummaries) syncSavedSummarySheet(ss, payload.savedReceiptsSummaries);
          if (payload.rates) syncRatesSheet(ss, payload.rates);
          if (payload.shopProfile) syncShopProfileSheet(ss, payload.shopProfile);
        }
        return jsonResponse({
          status: "success",
          message: "အင်တာနက်မရှိချိန် မှတ်သားထားသော လုပ်ဆောင်ချက် (" + replayedCount + ") ခုကို အစဉ်လိုက် ပြန်လည်လုပ်ဆောင်ပြီး Google Sheets နှင့် ထပ်တူညီအောင် ပြင်ဆင်ပြီးပါပြီ",
          sheetUrl: ss.getUrl(),
          replayedCount: replayedCount
        });

      case "sync_all":
        // 1. Replay any pending offline actions in chronological order first
        if (payload.offlineQueue && payload.offlineQueue.length > 0) {
          processOfflineQueueInOrder(ss, payload.offlineQueue, folderId);
        }
        // 2. Mirror exact screen state (including empty lists if user deleted all products/orders/receipts on screen)
        if (payload.products !== undefined && payload.products !== null) syncProductsSheet(ss, payload.products, folderId);
        if (payload.orders !== undefined && payload.orders !== null) syncOrdersSheet(ss, payload.orders);
        if (payload.savedReceipts !== undefined && payload.savedReceipts !== null) syncSavedReceiptsSheet(ss, payload.savedReceipts);
        if (payload.salesSummaries !== undefined && payload.salesSummaries !== null) syncSalesSummarySheet(ss, payload.salesSummaries);
        if (payload.savedReceiptsSummaries !== undefined && payload.savedReceiptsSummaries !== null) syncSavedSummarySheet(ss, payload.savedReceiptsSummaries);
        if (payload.rates) syncRatesSheet(ss, payload.rates);
        if (payload.shopProfile) syncShopProfileSheet(ss, payload.shopProfile);
        var allProds = fetchProductsFromSheet(ss);
        var allOrders = fetchOrdersFromSheet(ss);
        var allSaved = fetchSavedReceiptsFromSheet(ss);
        var allRates = fetchRatesFromSheet(ss);
        var allShopProfile = fetchShopProfileFromSheet(ss);
        return jsonResponse({
          status: "success",
          message: "မျက်နှာပြင်အတိုင်း အချက်အလက်အားလုံးကို Google Sheets နှင့် JSON format ဖြင့် အောင်မြင်စွာ တပြိုင်နက် သိမ်းဆည်းပြီးပါပြီ",
          sheetUrl: ss.getUrl(),
          products: allProds,
          orders: allOrders,
          savedReceipts: allSaved,
          rates: allRates,
          shopProfile: allShopProfile,
          ordersCount: allOrders.length
        });

      case "pull_all":
      case "fetch_all":
        var fetchedProducts = fetchProductsFromSheet(ss);
        var fetchedOrders = fetchOrdersFromSheet(ss);
        var fetchedSaved = fetchSavedReceiptsFromSheet(ss);
        var fetchedRates = fetchRatesFromSheet(ss);
        var fetchedShopProfile = fetchShopProfileFromSheet(ss);
        var fetchedOrdersCount = getOrdersCountFromSheet(ss);
        return jsonResponse({
          status: "success",
          message: "Google Sheet မှ အချက်အလက်များ JSON format ဖြင့် အောင်မြင်စွာ ဆွဲယူပြီးပါပြီ",
          sheetUrl: ss.getUrl(),
          products: fetchedProducts,
          orders: fetchedOrders,
          savedReceipts: fetchedSaved,
          rates: fetchedRates,
          shopProfile: fetchedShopProfile,
          ordersCount: fetchedOrdersCount
        });

      case "upload_file":
        var fileName = payload.fileName || ("Receipt_" + new Date().getTime() + ".txt");
        var base64Data = payload.base64Data || "";
        var mimeType = payload.mimeType || "text/plain";
        var fileUrl = uploadToDrive(fileName, base64Data, mimeType, folderId);
        return jsonResponse({
          status: "success",
          message: "ဖိုင်အား Google Drive သို့ အောင်မြင်စွာ တင်ပြီးပါပြီ",
          fileUrl: fileUrl,
          sheetUrl: ss.getUrl()
        });

      default:
        return jsonResponse({ status: "error", message: "Unknown action: " + action });
    }
  } catch (err) {
    return jsonResponse({ status: "error", message: err.toString() });
  }
}

function doGet(e) {
  try {
    var sheetName = (e && e.parameter && e.parameter.sheetName) || "POS Retail Sales & Inventory";
    var sheetUrl = (e && e.parameter && e.parameter.sheetUrl) || "";
    var folderId = (e && e.parameter && e.parameter.folderId) || "";
    var ss = getOrCreateSpreadsheet(sheetName, folderId, sheetUrl);
    var fetchedProducts = fetchProductsFromSheet(ss);
    var fetchedOrders = fetchOrdersFromSheet(ss);
    var fetchedSaved = fetchSavedReceiptsFromSheet(ss);
    var fetchedRates = fetchRatesFromSheet(ss);
    var fetchedShopProfile = fetchShopProfileFromSheet(ss);
    return jsonResponse({
      status: "success",
      message: "Google Sheet မှ အချက်အလက်များ JSON format ဖြင့် အောင်မြင်စွာ ဆွဲယူပြီးပါပြီ",
      sheetUrl: ss.getUrl(),
      products: fetchedProducts,
      orders: fetchedOrders,
      savedReceipts: fetchedSaved,
      rates: fetchedRates,
      shopProfile: fetchedShopProfile,
      ordersCount: fetchedOrders.length
    });
  } catch (err) {
    return jsonResponse({ status: "error", message: err.toString() });
  }
}

function jsonResponse(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

function getOrCreateSpreadsheet(name, folderId, sheetUrl) {
  if (sheetUrl && sheetUrl.trim().length > 0) {
    try {
      return SpreadsheetApp.openByUrl(sheetUrl.trim());
    } catch (e) {
      var match = sheetUrl.match(/\/d\/([a-zA-Z0-9-_]+)/);
      if (match && match[1]) {
        try {
          return SpreadsheetApp.openById(match[1]);
        } catch (e2) {}
      }
    }
  }

  var files = DriveApp.getFilesByName(name);
  if (files.hasNext()) {
    var file = files.next();
    return SpreadsheetApp.open(file);
  }
  
  var newSs = SpreadsheetApp.create(name);
  if (folderId && folderId.trim().length > 0) {
    try {
      var folder = DriveApp.getFolderById(folderId.trim());
      var file = DriveApp.getFileById(newSs.getId());
      folder.addFile(file);
      DriveApp.getRootFolder().removeFile(file);
    } catch(e) {}
  }
  return newSs;
}

function ensureAllSheets(ss, shopName) {
  var sheetConfigs = [
    {
      name: "Products",
      headers: ["ဘားကုဒ် (Barcode)", "ပစ္စည်းအမည် (Product Name)", "အမျိုးအစား (Category)", "ဝယ်ရင်းဈေး (Cost)", "ရောင်းဈေး (Selling)", "လက်ကားဈေး (Wholesale)", "လက်ကျန် (Stock)", "ယူနစ် (Unit)", "ငွေကြေး (Currency)", "ပုံလင့်ခ် (Image URL)", "နောက်ဆုံးပြင်ဆင်ချိန် (Updated At)"],
      color: "#1E3A8A"
    },
    {
      name: "Sales_Orders",
      headers: ["ပြေစာအမှတ် (Invoice No)", "ရက်စွဲနှင့်အချိန် (Date Time)", "ဝယ်သူအမည် (Customer)", "ဖုန်းနံပါတ် (Phone)", "ငွေချေစနစ် (Payment)", "ပစ္စည်းများ (Items Details)", "စုစုပေါင်း (Subtotal)", "လျှော့ငွေ (Discount)", "ကျသင့်ငွေ (Grand Total)", "ငွေကြေး (Currency)", "ကျပ်ငွေစုစုပေါင်း (Total MMK)", "ဘတ်ငွေစုစုပေါင်း (Total THB)", "ငွေလဲနှုန်း (Exchange Rate)", "အမြတ် ကျပ် (Profit MMK)", "အမြတ် ဘတ် (Profit THB)", "မှတ်ချက် (Note)", "ပြေစာလင့်ခ် (Drive Link)", "အခြေအနေ (Status)", "ပစ္စည်း JSON (Items JSON)"],
      color: "#065F46"
    },
    {
      name: "Saved_Receipts",
      headers: ["ပြေစာအမှတ် (Invoice No)", "ရက်စွဲနှင့်အချိန် (Date Time)", "ဝယ်သူအမည် (Customer)", "ဖုန်းနံပါတ် (Phone)", "ငွေချေစနစ် (Payment)", "ပစ္စည်းများ (Items Details)", "ကျသင့်ငွေ (Grand Total)", "ကျပ်ငွေစုစုပေါင်း (Total MMK)", "ဘတ်ငွေစုစုပေါင်း (Total THB)", "ငွေလဲနှုန်း (Exchange Rate)", "အမြတ် ကျပ် (Profit MMK)", "အမြတ် ဘတ် (Profit THB)", "ရှင်းလင်းသည့်ရက်စွဲ (Cleared Date)", "ငွေကြေး (Currency)", "ပစ္စည်း JSON (Items JSON)"],
      color: "#4F46E5"
    },
    {
      name: "Sales_Summary",
      headers: ["စာရင်းအမျိုးအစား (Type)", "ကာလ/လ (Period)", "ပြေစာစောင်ရေ (Orders Count)", "ရောင်းရယူနစ် (Units Sold)", "ဝင်ငွေ ကျပ် (Revenue MMK)", "ဝင်ငွေ ဘတ် (Revenue THB)", "လျှော့ငွေ ကျပ် (Discount MMK)", "လျှော့ငွေ ဘတ် (Discount THB)", "အသားတင်အမြတ် ကျပ် (Net Profit MMK)", "အသားတင်အမြတ် ဘတ် (Net Profit THB)", "တွက်ချက်သည့်အချိန် (Generated Date)"],
      color: "#7C3AED"
    },
    {
      name: "Saved_Receipts_Summary",
      headers: ["စာရင်းအမျိုးအစား (Type)", "ကာလ/လ (Period)", "ပြေစာစောင်ရေ (Orders Count)", "ရောင်းရယူနစ် (Units Sold)", "ဝင်ငွေ ကျပ် (Revenue MMK)", "ဝင်ငွေ ဘတ် (Revenue THB)", "အသားတင်အမြတ် ကျပ် (Net Profit MMK)", "အသားတင်အမြတ် ဘတ် (Net Profit THB)", "တွက်ချက်သည့်အချိန် (Generated Date)"],
      color: "#B45309"
    },
    {
      name: "Exchange_Rates",
      headers: ["ငွေကြေး (Currency)", "ရောင်းဈေးနှုန်း (Selling Rate)", "ဝယ်ဈေးနှုန်း (Buying Rate)", "ညီမျှသောကျပ်/ဘတ်တန်ဖိုး (Formula/Equivalent)", "နောက်ဆုံးပြင်ဆင်ချိန် (Updated At)"],
      color: "#0369A1"
    },
    {
      name: "Shop_Profile",
      headers: ["ဆိုင်အမည် (Shop Name)", "အင်္ဂလိပ်အမည် (English Name)", "ဖုန်းနံပါတ် (Phone)", "လိပ်စာ (Address)", "ပြေစာအောက်ခြေစာသား (Footer)", "ကောင်တာတာဝန်ခံ (Cashier)", "နောက်ဆုံးပြင်ဆင်ချိန် (Updated At)"],
      color: "#0F766E"
    },
    {
      name: "Action_History",
      headers: ["အမှတ်စဉ် (Action ID)", "အချိန် (Date Time)", "လုပ်ဆောင်ချက်အမျိုးအစား (Action Type)", "အကြောင်းအရာ (Description)", "ဘောက်ချာအမှတ် (Invoice No)", "ဘားကုဒ် (Barcode)", "ပစ္စည်းအမည်/အသေးစိတ် (Item Details)", "အခြေအနေ (Status)"],
      color: "#374151"
    }
  ];

  for (var i = 0; i < sheetConfigs.length; i++) {
    var conf = sheetConfigs[i];
    var sheet = ss.getSheetByName(conf.name);
    if (!sheet) {
      sheet = ss.insertSheet(conf.name);
    }
    if (sheet.getLastRow() === 0) {
      sheet.appendRow(conf.headers);
      var headerRange = sheet.getRange(1, 1, 1, conf.headers.length);
      headerRange.setBackground(conf.color);
      headerRange.setFontColor("#FFFFFF");
      headerRange.setFontWeight("bold");
      headerRange.setHorizontalAlignment("center");
      sheet.setFrozenRows(1);
    }
  }

  // Remove default "Sheet1" if empty and extra sheets exist
  var defaultSheet = ss.getSheetByName("Sheet1");
  if (defaultSheet && ss.getSheets().length > 1 && defaultSheet.getLastRow() === 0) {
    try { ss.deleteSheet(defaultSheet); } catch(e) {}
  }
}

function syncProductsSheet(ss, products, folderId) {
  var sheet = ss.getSheetByName("Products");
  if (!sheet) { ensureAllSheets(ss); sheet = ss.getSheetByName("Products"); }
  
  // Preserve existing valid cloud image URLs by barcode/name in case an incoming product has an empty or local-only path
  var existingImages = {};
  if (sheet.getLastRow() > 1) {
    var oldVals = sheet.getRange(2, 1, sheet.getLastRow() - 1, 10).getValues();
    for (var r = 0; r < oldVals.length; r++) {
      var oldCode = oldVals[r][0] ? String(oldVals[r][0]).trim() : "";
      var oldName = oldVals[r][1] ? String(oldVals[r][1]).trim().toLowerCase() : "";
      var oldImg = oldVals[r][9] ? String(oldVals[r][9]).trim() : "";
      if (oldImg && oldImg.indexOf("file://") !== 0 && oldImg.indexOf("content://") !== 0) {
        if (oldCode) existingImages["code:" + oldCode] = oldImg;
        if (oldName) existingImages["name:" + oldName] = oldImg;
      }
    }
    sheet.getRange(2, 1, sheet.getLastRow() - 1, sheet.getLastColumn()).clearContent();
  }
  
  if (products.length === 0) return;

  var rows = [];
  var now = Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "yyyy-MM-dd HH:mm:ss");
  for (var i = 0; i < products.length; i++) {
    var p = products[i];
    var barcode = p.barcode ? String(p.barcode).trim() : "";
    var name = p.name ? String(p.name).trim() : "";
    var rawImg = p.imageUri ? String(p.imageUri).trim() : "";
    var finalImg = rawImg;

    // If imageUri is a base64 data:image or base64Data is provided, upload to Drive to get a permanent direct image link
    if (p.imageBase64 && String(p.imageBase64).length > 0) {
      var uploaded = uploadImageToDrive("Product_" + (barcode || i) + ".jpg", p.imageBase64, "image/jpeg", folderId);
      if (uploaded) finalImg = uploaded;
    } else if (rawImg.indexOf("data:image") === 0 && rawImg.indexOf("base64,") > 0) {
      var b64 = rawImg.substring(rawImg.indexOf("base64,") + 7);
      var uploadedFromDataUrl = uploadImageToDrive("Product_" + (barcode || i) + ".jpg", b64, "image/jpeg", folderId);
      if (uploadedFromDataUrl) finalImg = uploadedFromDataUrl;
    } else if (!rawImg || rawImg.indexOf("file://") === 0 || rawImg.indexOf("content://") === 0 || rawImg.indexOf("/") === 0) {
      // Fall back to existing cloud image in sheet if present
      var preserved = (barcode && existingImages["code:" + barcode]) || (name && existingImages["name:" + name.toLowerCase()]) || "";
      if (preserved) finalImg = preserved;
    }

    rows.push([
      barcode,
      name,
      p.category || "",
      p.costPrice || 0,
      p.sellingPrice || 0,
      p.wholesalePrice || 0,
      p.stockQuantity || 0,
      p.unit || "ခု",
      p.currency || "MMK",
      finalImg,
      now
    ]);
  }
  sheet.getRange(2, 1, rows.length, rows[0].length).setValues(rows);
}

function syncOrdersSheet(ss, orders) {
  var sheet = ss.getSheetByName("Sales_Orders");
  if (!sheet) { ensureAllSheets(ss); sheet = ss.getSheetByName("Sales_Orders"); }
  
  if (sheet.getLastRow() > 1) {
    sheet.getRange(2, 1, sheet.getLastRow() - 1, sheet.getLastColumn()).clearContent();
  }
  
  if (!orders || orders.length === 0) return;

  var rows = [];
  for (var i = 0; i < orders.length; i++) {
    var o = orders[i];
    var itemsJsonStr = o.itemsJson || (o.items ? JSON.stringify(o.items) : "");
    rows.push([
      o.invoiceNumber || "",
      o.dateTime || "",
      o.customerName || "",
      o.customerPhone || "",
      o.paymentMethod || "ငွေသား",
      o.itemsSummary || "",
      o.subtotal || 0,
      o.discount || 0,
      o.grandTotal || 0,
      o.currency || "MMK",
      o.totalMmk || 0,
      o.totalThb || 0,
      o.exchangeRate || 0,
      o.profitMmk || 0,
      o.profitThb || 0,
      o.note || "",
      o.driveLink || "",
      o.status || "COMPLETED",
      itemsJsonStr
    ]);
  }
  sheet.getRange(2, 1, rows.length, rows[0].length).setValues(rows);
}

function appendSingleOrder(ss, o, folderId) {
  upsertSingleOrder(ss, o, folderId);
}

function orderExistsInSheet(ss, invoiceNumber) {
  if (!invoiceNumber) return false;
  var sheet = ss.getSheetByName("Sales_Orders");
  if (!sheet || sheet.getLastRow() <= 1) return false;
  var target = String(invoiceNumber).trim();
  var vals = sheet.getRange(2, 1, sheet.getLastRow() - 1, 1).getValues();
  for (var i = 0; i < vals.length; i++) {
    if (String(vals[i][0] || "").trim() === target) return true;
  }
  return false;
}

function upsertSingleOrder(ss, o, folderId) {
  var sheet = ss.getSheetByName("Sales_Orders");
  if (!sheet) { ensureAllSheets(ss); sheet = ss.getSheetByName("Sales_Orders"); }

  var driveLink = o.driveLink || "";
  if (o.receiptText && (!driveLink || driveLink.length === 0)) {
    try {
      driveLink = uploadToDrive("Receipt_" + o.invoiceNumber + ".txt", Utilities.base64Encode(o.receiptText), "text/plain", folderId);
    } catch(e) {}
  }

  var itemsJsonStr = o.itemsJson || (o.items ? JSON.stringify(o.items) : "");
  var rowData = [
    o.invoiceNumber || "",
    o.dateTime || Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "yyyy-MM-dd HH:mm:ss"),
    o.customerName || "",
    o.customerPhone || "",
    o.paymentMethod || "ငွေသား",
    o.itemsSummary || "",
    o.subtotal || 0,
    o.discount || 0,
    o.grandTotal || 0,
    o.currency || "MMK",
    o.totalMmk || 0,
    o.totalThb || 0,
    o.exchangeRate || 0,
    o.profitMmk || 0,
    o.profitThb || 0,
    o.note || "",
    driveLink,
    o.status || "COMPLETED",
    itemsJsonStr
  ];

  var targetInv = String(o.invoiceNumber || "").trim();
  if (targetInv && sheet.getLastRow() > 1) {
    var invVals = sheet.getRange(2, 1, sheet.getLastRow() - 1, 1).getValues();
    for (var i = 0; i < invVals.length; i++) {
      if (String(invVals[i][0] || "").trim() === targetInv) {
        sheet.getRange(i + 2, 1, 1, rowData.length).setValues([rowData]);
        return;
      }
    }
  }
  sheet.appendRow(rowData);
}

/**
 * Adjusts product stock quantities in the Products tab.
 * multiplier = -1 for deducting sold items, +1 for restoring refunded/deleted order items.
 */
function adjustProductStocksInSheet(ss, items, multiplier) {
  if (!items || items.length === 0) return;
  var sheet = ss.getSheetByName("Products");
  if (!sheet || sheet.getLastRow() <= 1) return;

  var lastRow = sheet.getLastRow();
  var range = sheet.getRange(2, 1, lastRow - 1, 11);
  var values = range.getValues();
  var now = Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "yyyy-MM-dd HH:mm:ss");
  var changed = false;

  for (var i = 0; i < items.length; i++) {
    var item = items[i];
    var qty = Number(item.quantity) || 0;
    if (qty <= 0) continue;
    var itemBarcode = item.barcode ? String(item.barcode).trim() : "";
    var itemName = (item.productName || item.name) ? String(item.productName || item.name).trim().toLowerCase() : "";

    for (var r = 0; r < values.length; r++) {
      var rowBarcode = values[r][0] ? String(values[r][0]).trim() : "";
      var rowName = values[r][1] ? String(values[r][1]).trim().toLowerCase() : "";
      var matched = (itemBarcode && rowBarcode === itemBarcode) || (itemName && rowName === itemName);
      if (matched) {
        var currentStock = Number(values[r][6]) || 0;
        var nextStock = Math.max(0, currentStock + (qty * multiplier));
        values[r][6] = nextStock;
        values[r][10] = now;
        changed = true;
        break;
      }
    }
  }

  if (changed) {
    range.setValues(values);
  }
}

/**
 * Deletes specified products from the Products sheet by barcode or name.
 */
function deleteProductsFromSheet(ss, deletedProducts) {
  if (!deletedProducts || deletedProducts.length === 0) return;
  var sheet = ss.getSheetByName("Products");
  if (!sheet || sheet.getLastRow() <= 1) return;

  var barcodesToDelete = {};
  var namesToDelete = {};
  for (var i = 0; i < deletedProducts.length; i++) {
    var dp = deletedProducts[i];
    if (!dp) continue;
    var bc = dp.barcode ? String(dp.barcode).trim() : "";
    var nm = dp.name ? String(dp.name).trim().toLowerCase() : "";
    if (bc) barcodesToDelete[bc] = true;
    if (nm) namesToDelete[nm] = true;
  }

  var lastRow = sheet.getLastRow();
  var vals = sheet.getRange(2, 1, lastRow - 1, 2).getValues();
  for (var r = vals.length - 1; r >= 0; r--) {
    var rowBc = vals[r][0] ? String(vals[r][0]).trim() : "";
    var rowNm = vals[r][1] ? String(vals[r][1]).trim().toLowerCase() : "";
    if ((rowBc && barcodesToDelete[rowBc]) || (rowNm && namesToDelete[rowNm])) {
      sheet.deleteRow(r + 2);
    }
  }
}

/**
 * Updates an order's status (e.g., REFUNDED or COMPLETED) in Sales_Orders sheet.
 */
function updateOrderStatusInSheet(ss, invoiceNumber, newStatus, orderObj) {
  var sheet = ss.getSheetByName("Sales_Orders");
  if (!sheet) { ensureAllSheets(ss); sheet = ss.getSheetByName("Sales_Orders"); }
  var targetInv = String(invoiceNumber || (orderObj && orderObj.invoiceNumber) || "").trim();
  if (!targetInv) return;

  if (sheet.getLastRow() > 1) {
    var lastRow = sheet.getLastRow();
    var invVals = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
    for (var i = 0; i < invVals.length; i++) {
      if (String(invVals[i][0] || "").trim() === targetInv) {
        sheet.getRange(i + 2, 18).setValue(newStatus);
        return;
      }
    }
  }
  if (orderObj && orderObj.invoiceNumber) {
    orderObj.status = newStatus;
    upsertSingleOrder(ss, orderObj, "");
  }
}

/**
 * Deletes an order row from Sales_Orders sheet by invoiceNumber.
 */
function deleteOrderFromSheetByInvoice(ss, invoiceNumber) {
  if (!invoiceNumber) return;
  var sheet = ss.getSheetByName("Sales_Orders");
  if (!sheet || sheet.getLastRow() <= 1) return;
  var target = String(invoiceNumber).trim();
  var vals = sheet.getRange(2, 1, sheet.getLastRow() - 1, 1).getValues();
  for (var r = vals.length - 1; r >= 0; r--) {
    if (String(vals[r][0] || "").trim() === target) {
      sheet.deleteRow(r + 2);
    }
  }
}

/**
 * Deletes a saved receipt row from Saved_Receipts sheet by invoiceNumber.
 */
function deleteSavedReceiptFromSheetByInvoice(ss, invoiceNumber) {
  if (!invoiceNumber) return;
  var sheet = ss.getSheetByName("Saved_Receipts");
  if (!sheet || sheet.getLastRow() <= 1) return;
  var target = String(invoiceNumber).trim();
  var vals = sheet.getRange(2, 1, sheet.getLastRow() - 1, 1).getValues();
  for (var r = vals.length - 1; r >= 0; r--) {
    if (String(vals[r][0] || "").trim() === target) {
      sheet.deleteRow(r + 2);
    }
  }
}

/**
 * Logs chronological actions into Action_History tab and avoids duplicate offline action execution.
 */
function appendActionLog(ss, actionType, description, invoiceNo, barcode, itemDetails, actionId, actionTime) {
  var sheet = ss.getSheetByName("Action_History");
  if (!sheet) { ensureAllSheets(ss); sheet = ss.getSheetByName("Action_History"); }
  var idStr = actionId ? String(actionId).trim() : ("ACT-" + new Date().getTime());
  var timeStr = actionTime || Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "yyyy-MM-dd HH:mm:ss");
  sheet.appendRow([
    idStr,
    timeStr,
    actionType || "",
    description || "",
    invoiceNo || "",
    barcode || "",
    itemDetails || "",
    "COMPLETED"
  ]);
}

function isActionAlreadyProcessed(ss, actionId) {
  if (!actionId) return false;
  var sheet = ss.getSheetByName("Action_History");
  if (!sheet || sheet.getLastRow() <= 1) return false;
  var target = String(actionId).trim();
  var startRow = Math.max(2, sheet.getLastRow() - 300);
  var numRows = sheet.getLastRow() - startRow + 1;
  var vals = sheet.getRange(startRow, 1, numRows, 1).getValues();
  for (var i = 0; i < vals.length; i++) {
    if (String(vals[i][0] || "").trim() === target) return true;
  }
  return false;
}

/**
 * Processes queued offline actions strictly in chronological order (FIFO).
 */
function processOfflineQueueInOrder(ss, queueActions, folderId) {
  if (!queueActions || queueActions.length === 0) return 0;
  // Sort by timestamp ascending to guarantee strict sequential execution
  queueActions.sort(function(a, b) {
    return (Number(a.timestamp) || 0) - (Number(b.timestamp) || 0);
  });

  var processedCount = 0;
  for (var i = 0; i < queueActions.length; i++) {
    var act = queueActions[i];
    if (!act) continue;
    var actId = act.id || ("OFFLINE-" + (act.timestamp || i));
    if (isActionAlreadyProcessed(ss, actId)) {
      processedCount++;
      continue;
    }

    var type = String(act.actionType || "").toUpperCase();
    var data = act.payload || {};
    if (typeof data === "string") {
      try { data = JSON.parse(data); } catch(e) { data = {}; }
    }

    switch (type) {
      case "CHECKOUT_ORDER":
        if (data.order) {
          var exists = orderExistsInSheet(ss, data.order.invoiceNumber);
          upsertSingleOrder(ss, data.order, folderId);
          if (!exists && data.order.items && data.order.items.length > 0 && data.order.status !== "REFUNDED") {
            adjustProductStocksInSheet(ss, data.order.items, -1);
          }
        }
        break;

      case "REFUND_ORDER":
        var refInv = act.targetInvoiceNumber || data.invoiceNumber || (data.order && data.order.invoiceNumber) || "";
        var refItems = data.items || (data.order && data.order.items) || [];
        updateOrderStatusInSheet(ss, refInv, "REFUNDED", data.order);
        if (refItems.length > 0) {
          adjustProductStocksInSheet(ss, refItems, 1);
        }
        break;

      case "UNDO_REFUND_ORDER":
        var undoInv = act.targetInvoiceNumber || data.invoiceNumber || (data.order && data.order.invoiceNumber) || "";
        var undoItems = data.items || (data.order && data.order.items) || [];
        updateOrderStatusInSheet(ss, undoInv, "COMPLETED", data.order);
        if (undoItems.length > 0) {
          adjustProductStocksInSheet(ss, undoItems, -1);
        }
        break;

      case "DELETE_ORDER":
        var delInv = act.targetInvoiceNumber || data.invoiceNumber || "";
        var wasRef = data.wasRefunded === true;
        var delItems = data.items || [];
        deleteOrderFromSheetByInvoice(ss, delInv);
        if (!wasRef && delItems.length > 0) {
          adjustProductStocksInSheet(ss, delItems, 1);
        }
        break;

      case "CLEAR_ALL_ORDERS":
        syncOrdersSheet(ss, []);
        break;

      case "SAVE_PRODUCT":
        if (data.product) {
          upsertSingleProductInSheet(ss, data.product, folderId);
        }
        break;

      case "ADJUST_STOCK":
        if (data.barcode || data.name) {
          adjustProductStocksInSheet(ss, [{
            barcode: data.barcode || "",
            productName: data.name || "",
            quantity: Math.abs(Number(data.delta) || 0)
          }], (Number(data.delta) || 0) >= 0 ? 1 : -1);
        }
        break;

      case "DELETE_PRODUCT":
        deleteProductsFromSheet(ss, [{
          barcode: act.targetProductBarcode || data.barcode || "",
          name: act.targetProductName || data.name || ""
        }]);
        break;

      case "DELETE_ALL_PRODUCTS":
        syncProductsSheet(ss, [], folderId);
        break;

      case "CLEAR_TO_SAVED_RECEIPT":
        if (data.savedReceipt) {
          appendSingleSavedReceipt(ss, data.savedReceipt);
          if (data.savedReceipt.invoiceNumber) {
            deleteOrderFromSheetByInvoice(ss, data.savedReceipt.invoiceNumber);
          }
        }
        break;

      case "RESTORE_SAVED_RECEIPT":
        if (data.order) {
          upsertSingleOrder(ss, data.order, folderId);
        }
        if (act.targetInvoiceNumber || data.invoiceNumber) {
          deleteSavedReceiptFromSheetByInvoice(ss, act.targetInvoiceNumber || data.invoiceNumber);
        }
        break;

      case "DELETE_SAVED_RECEIPT":
        deleteSavedReceiptFromSheetByInvoice(ss, act.targetInvoiceNumber || data.invoiceNumber || "");
        break;

      case "DELETE_ALL_SAVED_RECEIPTS":
        syncSavedReceiptsSheet(ss, []);
        break;
    }

    appendActionLog(
      ss,
      type,
      act.descriptionMm || "",
      act.targetInvoiceNumber || "",
      act.targetProductBarcode || "",
      act.targetProductName || "",
      actId,
      act.dateTime || ""
    );
    processedCount++;
  }
  return processedCount;
}

function upsertSingleProductInSheet(ss, p, folderId) {
  var sheet = ss.getSheetByName("Products");
  if (!sheet) { ensureAllSheets(ss); sheet = ss.getSheetByName("Products"); }
  var barcode = p.barcode ? String(p.barcode).trim() : "";
  var name = p.name ? String(p.name).trim() : "";
  var now = Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "yyyy-MM-dd HH:mm:ss");
  var rawImg = p.imageUri ? String(p.imageUri).trim() : "";
  var finalImg = rawImg;
  if (rawImg.indexOf("data:image") === 0 && rawImg.indexOf("base64,") > 0) {
    var b64 = rawImg.substring(rawImg.indexOf("base64,") + 7);
    var uploaded = uploadImageToDrive("Product_" + (barcode || name) + ".jpg", b64, "image/jpeg", folderId);
    if (uploaded) finalImg = uploaded;
  }

  var rowData = [
    barcode,
    name,
    p.category || "အထွေထွေ",
    p.costPrice || 0,
    p.sellingPrice || 0,
    p.wholesalePrice || 0,
    p.stockQuantity || 0,
    p.unit || "ခု",
    p.currency || "MMK",
    finalImg,
    now
  ];

  if (sheet.getLastRow() > 1) {
    var vals = sheet.getRange(2, 1, sheet.getLastRow() - 1, 2).getValues();
    for (var r = 0; r < vals.length; r++) {
      var rBc = vals[r][0] ? String(vals[r][0]).trim() : "";
      var rNm = vals[r][1] ? String(vals[r][1]).trim().toLowerCase() : "";
      if ((barcode && rBc === barcode) || (name && rNm === name.toLowerCase())) {
        if (!finalImg || finalImg.indexOf("file://") === 0 || finalImg.indexOf("content://") === 0) {
          var existingImg = sheet.getRange(r + 2, 10).getValue();
          if (existingImg) rowData[9] = String(existingImg);
        }
        sheet.getRange(r + 2, 1, 1, rowData.length).setValues([rowData]);
        return;
      }
    }
  }
  sheet.appendRow(rowData);
}

function syncSavedReceiptsSheet(ss, savedReceipts) {
  var sheet = ss.getSheetByName("Saved_Receipts");
  if (!sheet) { ensureAllSheets(ss); sheet = ss.getSheetByName("Saved_Receipts"); }
  
  if (sheet.getLastRow() > 1) {
    sheet.getRange(2, 1, sheet.getLastRow() - 1, sheet.getLastColumn()).clearContent();
  }
  
  if (!savedReceipts || savedReceipts.length === 0) return;

  var rows = [];
  for (var i = 0; i < savedReceipts.length; i++) {
    var r = savedReceipts[i];
    rows.push([
      r.invoiceNumber || "",
      r.dateTime || "",
      r.customerName || "",
      r.customerPhone || "",
      r.paymentMethod || "ငွေသား",
      r.itemsSummary || "",
      r.grandTotal || 0,
      r.totalMmk || 0,
      r.totalThb || 0,
      r.exchangeRate || 0,
      r.profitMmk || 0,
      r.profitThb || 0,
      r.clearedDate || "",
      r.currency || "MMK",
      r.itemsJson || ""
    ]);
  }
  sheet.getRange(2, 1, rows.length, rows[0].length).setValues(rows);
}

function appendSingleSavedReceipt(ss, r) {
  var sheet = ss.getSheetByName("Saved_Receipts");
  if (!sheet) { ensureAllSheets(ss); sheet = ss.getSheetByName("Saved_Receipts"); }

  // Avoid duplicate invoiceNumber in Saved_Receipts
  if (sheet.getLastRow() > 1) {
    var existingInvs = sheet.getRange(2, 1, sheet.getLastRow() - 1, 1).getValues();
    for (var i = 0; i < existingInvs.length; i++) {
      if (String(existingInvs[i][0]).trim() === String(r.invoiceNumber || "").trim()) {
        return;
      }
    }
  }

  sheet.appendRow([
    r.invoiceNumber || "",
    r.dateTime || "",
    r.customerName || "",
    r.customerPhone || "",
    r.paymentMethod || "ငွေသား",
    r.itemsSummary || "",
    r.grandTotal || 0,
    r.totalMmk || 0,
    r.totalThb || 0,
    r.exchangeRate || 0,
    r.profitMmk || 0,
    r.profitThb || 0,
    r.clearedDate || Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "yyyy-MM-dd HH:mm:ss"),
    r.currency || "MMK",
    r.itemsJson || ""
  ]);
}

function syncSalesSummarySheet(ss, summaries) {
  var sheet = ss.getSheetByName("Sales_Summary");
  if (!sheet) { ensureAllSheets(ss); sheet = ss.getSheetByName("Sales_Summary"); }
  
  if (sheet.getLastRow() > 1) {
    sheet.getRange(2, 1, sheet.getLastRow() - 1, sheet.getLastColumn()).clearContent();
  }
  
  if (!summaries || summaries.length === 0) return;

  var rows = [];
  for (var i = 0; i < summaries.length; i++) {
    var s = summaries[i];
    rows.push([
      s.type || "",
      s.period || "",
      s.ordersCount || 0,
      s.unitsSold || 0,
      s.revenueMmk || 0,
      s.revenueThb || 0,
      s.discountMmk || 0,
      s.discountThb || 0,
      s.profitMmk || 0,
      s.profitThb || 0,
      s.generatedDate || Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "yyyy-MM-dd HH:mm:ss")
    ]);
  }
  sheet.getRange(2, 1, rows.length, rows[0].length).setValues(rows);
}

function syncSavedSummarySheet(ss, summaries) {
  var sheet = ss.getSheetByName("Saved_Receipts_Summary");
  if (!sheet) { ensureAllSheets(ss); sheet = ss.getSheetByName("Saved_Receipts_Summary"); }
  
  if (sheet.getLastRow() > 1) {
    sheet.getRange(2, 1, sheet.getLastRow() - 1, sheet.getLastColumn()).clearContent();
  }
  
  if (!summaries || summaries.length === 0) return;

  var rows = [];
  for (var i = 0; i < summaries.length; i++) {
    var s = summaries[i];
    rows.push([
      s.type || "",
      s.period || "",
      s.ordersCount || 0,
      s.unitsSold || 0,
      s.revenueMmk || 0,
      s.revenueThb || 0,
      s.profitMmk || 0,
      s.profitThb || 0,
      s.generatedDate || Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "yyyy-MM-dd HH:mm:ss")
    ]);
  }
  sheet.getRange(2, 1, rows.length, rows[0].length).setValues(rows);
}

function syncRatesSheet(ss, rates) {
  var sheet = ss.getSheetByName("Exchange_Rates");
  if (!sheet) { ensureAllSheets(ss); sheet = ss.getSheetByName("Exchange_Rates"); }

  if (sheet.getLastRow() > 1) {
    sheet.getRange(2, 1, sheet.getLastRow() - 1, sheet.getLastColumn()).clearContent();
  }

  var now = Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "yyyy-MM-dd HH:mm:ss");
  var rows = [
    [
      "မြန်မာကျပ် (MMK)",
      rates.mmkSellingRate || "",
      rates.mmkBuyingRate || "",
      "၁ ကျပ် = " + (rates.mmkSellingRate || "0") + " ဘတ် (ရောင်း) / " + (rates.mmkBuyingRate || "0") + " ဘတ် (ဝယ်)",
      now
    ],
    [
      "ထိုင်းဘတ် (THB)",
      rates.thbSellingRate || "",
      rates.thbBuyingRate || "",
      "၁ ဘတ် = " + (rates.thbSellingRate || "0") + " ကျပ် (ရောင်း) / " + (rates.thbBuyingRate || "0") + " ကျပ် (ဝယ်)",
      now
    ]
  ];
  sheet.getRange(2, 1, rows.length, rows[0].length).setValues(rows);
}

function syncShopProfileSheet(ss, prof) {
  var sheet = ss.getSheetByName("Shop_Profile");
  if (!sheet) { ensureAllSheets(ss); sheet = ss.getSheetByName("Shop_Profile"); }

  if (sheet.getLastRow() > 1) {
    sheet.getRange(2, 1, sheet.getLastRow() - 1, sheet.getLastColumn()).clearContent();
  }

  var now = Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "yyyy-MM-dd HH:mm:ss");
  sheet.getRange(2, 1, 1, 7).setValues([[
    prof.shopName || "POS System Software",
    prof.englishName || "POS System Software",
    prof.phone || "",
    prof.address || "",
    prof.receiptFooter || "",
    prof.cashierName || "",
    now
  ]]);
}

function fetchShopProfileFromSheet(ss) {
  var sheet = ss.getSheetByName("Shop_Profile");
  if (!sheet || sheet.getLastRow() <= 1) return null;
  var row = sheet.getRange(2, 1, 1, 6).getValues()[0];
  return {
    shopName: row[0] ? String(row[0]).trim() : "",
    englishName: row[1] ? String(row[1]).trim() : "",
    phone: row[2] ? String(row[2]).trim() : "",
    address: row[3] ? String(row[3]).trim() : "",
    receiptFooter: row[4] ? String(row[4]).trim() : "",
    cashierName: row[5] ? String(row[5]).trim() : ""
  };
}

function uploadToDrive(fileName, base64Data, mimeType, folderId) {
  try {
    var bytes = Utilities.base64Decode(base64Data);
    var blob = Utilities.newBlob(bytes, mimeType, fileName);
    var file;
    if (folderId && folderId.trim().length > 0) {
      var folder = DriveApp.getFolderById(folderId.trim());
      file = folder.createFile(blob);
    } else {
      file = DriveApp.createFile(blob);
    }
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    if (mimeType && mimeType.indexOf("image/") === 0) {
      return "https://drive.google.com/uc?export=view&id=" + file.getId();
    }
    return file.getUrl();
  } catch(e) {
    return "";
  }
}

function uploadImageToDrive(fileName, base64Data, mimeType, folderId) {
  return uploadToDrive(fileName, base64Data, mimeType || "image/jpeg", folderId);
}

function fetchProductsFromSheet(ss) {
  var sheet = ss.getSheetByName("Products");
  if (!sheet || sheet.getLastRow() <= 1) return [];
  var lastRow = sheet.getLastRow();
  var range = sheet.getRange(2, 1, lastRow - 1, 10);
  var values = range.getValues();
  var formulas = range.getFormulas();
  var list = [];
  for (var i = 0; i < values.length; i++) {
    var row = values[i];
    var fRow = formulas[i];
    var barcode = row[0] ? String(row[0]).trim() : "";
    var name = row[1] ? String(row[1]).trim() : "";
    if (!name && !barcode) continue;
    var imgVal = row[9] ? String(row[9]).trim() : "";
    if (!imgVal && fRow && fRow[9]) {
      var fStr = String(fRow[9]).trim();
      var m = fStr.match(/=IMAGE\(\s*"([^"]+)"/i);
      if (m && m[1]) imgVal = m[1];
    }
    var driveMatch = imgVal.match(/\/d\/([a-zA-Z0-9_-]{15,})/);
    if (driveMatch && driveMatch[1] && imgVal.indexOf("drive.google.com") >= 0) {
      imgVal = "https://drive.google.com/uc?export=view&id=" + driveMatch[1];
    }
    list.push({
      barcode: barcode,
      name: name,
      category: row[2] ? String(row[2]).trim() : "အထွေထွေ",
      costPrice: Number(row[3]) || 0,
      sellingPrice: Number(row[4]) || 0,
      wholesalePrice: Number(row[5]) || 0,
      stockQuantity: Number(row[6]) || 0,
      unit: row[7] ? String(row[7]).trim() : "ခု",
      currency: row[8] ? String(row[8]).trim() : "MMK",
      imageUri: imgVal
    });
  }
  return list;
}

function fetchRatesFromSheet(ss) {
  var sheet = ss.getSheetByName("Exchange_Rates");
  if (!sheet || sheet.getLastRow() <= 1) return null;
  var values = sheet.getRange(2, 1, Math.min(sheet.getLastRow() - 1, 5), 4).getValues();
  var result = {};
  for (var i = 0; i < values.length; i++) {
    var curr = String(values[i][0] || "").toUpperCase();
    if (curr.indexOf("THB") >= 0 || curr.indexOf("ဘတ်") >= 0) {
      result.thbSellingRate = String(values[i][1] || "");
      result.thbBuyingRate = String(values[i][2] || "");
    } else if (curr.indexOf("MMK") >= 0 || curr.indexOf("ကျပ်") >= 0) {
      result.mmkSellingRate = String(values[i][1] || "");
      result.mmkBuyingRate = String(values[i][2] || "");
    }
  }
  return result;
}

function getOrdersCountFromSheet(ss) {
  var sheet = ss.getSheetByName("Sales_Orders");
  return sheet ? Math.max(0, sheet.getLastRow() - 1) : 0;
}

function fetchOrdersFromSheet(ss) {
  var sheet = ss.getSheetByName("Sales_Orders");
  if (!sheet || sheet.getLastRow() <= 1) return [];
  var lastRow = sheet.getLastRow();
  var values = sheet.getRange(2, 1, lastRow - 1, Math.min(sheet.getLastColumn(), 19)).getValues();
  var list = [];
  for (var i = 0; i < values.length; i++) {
    var row = values[i];
    var inv = row[0] ? String(row[0]).trim() : "";
    if (!inv) continue;
    var itemsArr = null;
    if (row.length > 18 && row[18]) {
      try { itemsArr = JSON.parse(String(row[18])); } catch(e) {}
    }
    var orderObj = {
      invoiceNumber: inv,
      dateTime: row[1] ? String(row[1]).trim() : "",
      customerName: row[2] ? String(row[2]).trim() : "",
      customerPhone: row[3] ? String(row[3]).trim() : "",
      paymentMethod: row[4] ? String(row[4]).trim() : "ငွေသား",
      itemsSummary: row[5] ? String(row[5]).trim() : "",
      subtotal: Number(row[6]) || 0,
      discount: Number(row[7]) || 0,
      grandTotal: Number(row[8]) || 0,
      currency: row[9] ? String(row[9]).trim() : "MMK",
      totalMmk: Number(row[10]) || 0,
      totalThb: Number(row[11]) || 0,
      exchangeRate: Number(row[12]) || 0,
      profitMmk: Number(row[13]) || 0,
      profitThb: Number(row[14]) || 0,
      note: row[15] ? String(row[15]).trim() : "",
      status: (row.length > 17 && row[17]) ? String(row[17]).trim() : "COMPLETED"
    };
    if (itemsArr) {
      orderObj.items = itemsArr;
    }
    list.push(orderObj);
  }
  return list;
}

function fetchSavedReceiptsFromSheet(ss) {
  var sheet = ss.getSheetByName("Saved_Receipts");
  if (!sheet || sheet.getLastRow() <= 1) return [];
  var lastRow = sheet.getLastRow();
  var values = sheet.getRange(2, 1, lastRow - 1, Math.min(sheet.getLastColumn(), 15)).getValues();
  var list = [];
  for (var i = 0; i < values.length; i++) {
    var row = values[i];
    var inv = row[0] ? String(row[0]).trim() : "";
    if (!inv) continue;
    list.push({
      invoiceNumber: inv,
      dateTime: row[1] ? String(row[1]).trim() : "",
      customerName: row[2] ? String(row[2]).trim() : "",
      customerPhone: row[3] ? String(row[3]).trim() : "",
      paymentMethod: row[4] ? String(row[4]).trim() : "ငွေသား",
      itemsSummary: row[5] ? String(row[5]).trim() : "",
      grandTotal: Number(row[6]) || 0,
      totalMmk: Number(row[7]) || 0,
      totalThb: Number(row[8]) || 0,
      exchangeRate: Number(row[9]) || 0,
      profitMmk: Number(row[10]) || 0,
      profitThb: Number(row[11]) || 0,
      clearedDate: row[12] ? String(row[12]).trim() : "",
      currency: (row.length > 13 && row[13]) ? String(row[13]).trim() : "MMK",
      itemsJson: (row.length > 14 && row[14]) ? String(row[14]).trim() : ""
    });
  }
  return list;
}
""".trimIndent()
}

package com.manish.doomsql.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.manish.doomsql.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TipTier(
    val productId: String,
    val title: String,
    val emoji: String,
    val description: String,
    val fallbackPrice: String
)

sealed interface BillingPurchaseEvent {
    data class Success(val productId: String) : BillingPurchaseEvent
    data object Pending : BillingPurchaseEvent
    data class Error(val message: String) : BillingPurchaseEvent
    data object Cancelled : BillingPurchaseEvent
}

class BillingManager(
    private val context: Context,
    private val userPreferences: UserPreferencesRepository
) : PurchasesUpdatedListener, BillingClientStateListener {

    companion object {
        private const val TAG = "DoomSQL_Billing"

        val TIP_TIERS = listOf(
            TipTier(
                productId = "tip_small",
                title = "Chai & Samosa",
                emoji = "☕",
                description = "Fuel 1 hour of SQL question design",
                fallbackPrice = "₹29"
            ),
            TipTier(
                productId = "tip_medium",
                title = "Coffee & Cookie",
                emoji = "🍪",
                description = "Help maintain sandbox engine & assets",
                fallbackPrice = "₹79"
            ),
            TipTier(
                productId = "tip_large",
                title = "Developer Pizza",
                emoji = "🍕",
                description = "Support offline database tools & sets",
                fallbackPrice = "₹199"
            ),
            TipTier(
                productId = "tip_hero",
                title = "Hero Sponsor",
                emoji = "🚀",
                description = "VIP Patron badge + eternal gratitude",
                fallbackPrice = "₹499"
            )
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap.asStateFlow()

    private val _purchaseEvent = MutableSharedFlow<BillingPurchaseEvent>()
    val purchaseEvent: SharedFlow<BillingPurchaseEvent> = _purchaseEvent.asSharedFlow()

    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
    }

    fun startConnection() {
        if (!billingClient.isReady) {
            billingClient.startConnection(this)
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.i(TAG, "Google Play Billing setup successful.")
            _isReady.value = true
            queryProductDetails()
            queryUnconsumedPurchases()
        } else {
            Log.w(TAG, "Billing setup failed: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            _isReady.value = false
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.w(TAG, "Billing service disconnected. Attempting to reconnect...")
        _isReady.value = false
        scope.launch {
            delay(2000)
            startConnection()
        }
    }

    private fun queryProductDetails() {
        val productList = TIP_TIERS.map { tier ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(tier.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val map = productDetailsList.associateBy { it.productId }
                _productDetailsMap.value = map
                Log.i(TAG, "Loaded ${map.size} in-app products from Google Play Billing.")
            } else {
                Log.w(TAG, "queryProductDetailsAsync failed: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            }
        }
    }

    private fun queryUnconsumedPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                scope.launch {
                    _purchaseEvent.emit(BillingPurchaseEvent.Cancelled)
                }
            }
            else -> {
                scope.launch {
                    _purchaseEvent.emit(BillingPurchaseEvent.Error(billingResult.debugMessage.ifBlank { "Payment could not be completed." }))
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Consumable in-app purchase so user can tip multiple times
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.consumeAsync(consumeParams) { result, _ ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    val prodId = purchase.products.firstOrNull() ?: ""
                    scope.launch {
                        userPreferences.recordTipSuccess()
                        _purchaseEvent.emit(BillingPurchaseEvent.Success(prodId))
                    }
                }
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            // Handles pending transactions (such as UPI approvals in PhonePe / Google Pay)
            scope.launch {
                _purchaseEvent.emit(BillingPurchaseEvent.Pending)
            }
        }
    }

    fun launchTipFlow(
        activity: Activity,
        productId: String,
        onNotConfiguredInPlayConsole: () -> Unit
    ) {
        val details = _productDetailsMap.value[productId]
        if (details == null) {
            onNotConfiguredInPlayConsole()
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "launchBillingFlow failed: ${result.responseCode} - ${result.debugMessage}")
        }
    }

    fun endConnection() {
        try {
            billingClient.endConnection()
        } catch (_: Exception) {}
    }
}

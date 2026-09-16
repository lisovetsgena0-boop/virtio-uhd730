package com.momentum.liferpg;

import android.app.Activity;
import android.widget.Toast;
import com.android.billingclient.api.*;
import java.util.*;

public final class BillingManager implements PurchasesUpdatedListener {
    public interface Listener {
        void onReady(Map<String, ProductDetails> products);
        void onEntitlement(boolean pro);
    }

    public static final String MONTHLY = "momentum_plus_monthly";
    public static final String YEARLY = "momentum_plus_yearly";

    private final Activity activity;
    private final Listener listener;
    private final BillingClient billingClient;
    private final Map<String, ProductDetails> products = new HashMap<>();

    public BillingManager(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        billingClient = BillingClient.newBuilder(activity)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build();
    }

    public void connect() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override public void onBillingSetupFinished(BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProducts();
                    restore();
                }
            }
            @Override public void onBillingServiceDisconnected() { }
        });
    }

    private void queryProducts() {
        List<QueryProductDetailsParams.Product> list = new ArrayList<>();
        list.add(QueryProductDetailsParams.Product.newBuilder().setProductId(MONTHLY).setProductType(BillingClient.ProductType.SUBS).build());
        list.add(QueryProductDetailsParams.Product.newBuilder().setProductId(YEARLY).setProductType(BillingClient.ProductType.SUBS).build());
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder().setProductList(list).build();
        billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override public void onProductDetailsResponse(BillingResult result, QueryProductDetailsResult data) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    products.clear();
                    for (ProductDetails p : data.getProductDetailsList()) products.put(p.getProductId(), p);
                    listener.onReady(new HashMap<>(products));
                }
            }
        });
    }

    public void buy(String productId) {
        ProductDetails p = products.get(productId);
        if (p == null) {
            Toast.makeText(activity, "Підписка ще не доступна. Спочатку створи продукт у Google Play Console.", Toast.LENGTH_LONG).show();
            return;
        }
        List<ProductDetails.SubscriptionOfferDetails> offers = p.getSubscriptionOfferDetails();
        if (offers == null || offers.isEmpty()) {
            Toast.makeText(activity, "Для цієї підписки немає активного base plan/offer.", Toast.LENGTH_LONG).show();
            return;
        }
        BillingFlowParams.ProductDetailsParams productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(p)
                .setOfferToken(offers.get(0).getOfferToken())
                .build();
        BillingFlowParams flow = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(productParams))
                .build();
        billingClient.launchBillingFlow(activity, flow);
    }

    public void restore() {
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build();
        billingClient.queryPurchasesAsync(params, new PurchasesResponseListener() {
            @Override public void onQueryPurchasesResponse(BillingResult result, List<Purchase> purchases) {
                boolean pro = false;
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (Purchase p : purchases) {
                        if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                            pro = true;
                            acknowledge(p);
                        }
                    }
                }
                listener.onEntitlement(pro);
            }
        });
    }

    private void acknowledge(Purchase purchase) {
        if (purchase.isAcknowledged()) return;
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();
        billingClient.acknowledgePurchase(params, new AcknowledgePurchaseResponseListener() {
            @Override public void onAcknowledgePurchaseResponse(BillingResult result) { }
        });
    }

    @Override public void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase p : purchases) if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) acknowledge(p);
            listener.onEntitlement(true);
            Toast.makeText(activity, "Momentum Plus активовано ✦", Toast.LENGTH_SHORT).show();
        }
    }

    public void close() {
        if (billingClient.isReady()) billingClient.endConnection();
    }
}

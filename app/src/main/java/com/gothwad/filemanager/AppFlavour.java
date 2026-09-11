package com.gothwad.manager;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

public abstract class AppFlavour extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
	}

	public static boolean isPurchased() {
		return true;
	}

	public void loadOwnedPurchasesFromGoogle() {
	}

	public void reloadSubscription() {
	}

	public static String getPurchaseId(){
		return "";
	}

	public static String getPurchasedProductId(){
		return "";
	}

	public boolean isBillingSupported() {
		return false;
	}

	public void purchase(Activity activity, String productId){

	}

	public static void openPurchaseActivity(Context context){

	}
}

package admobex;

import android.provider.Settings.Secure;
import android.util.Log;
import com.google.android.gms.ads.AdRequest;

import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;

import org.haxe.extension.Extension;
import org.haxe.lime.HaxeObject;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import android.opengl.GLSurfaceView;
import java.util.ArrayList;
import java.util.List;
import android.os.Handler;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.LoadAdError;

import com.google.android.gms.ads.RequestConfiguration;

public class AdMobEx extends Extension {

	private RewardedAd rewardedAd;
	private AdRequest adRequest;

	private static Boolean failRewarded = false;
	private static Boolean loadingRewarded = false;
	private static String rewardedId = null;

	private static AdMobEx instance = null;
	private static Boolean testingAds = false;

	private static HaxeObject callback = null;

	public static final String LEAVING = "LEAVING";
	public static final String FAILED = "FAILED";
	public static final String CLOSED = "CLOSED";
	public static final String DISPLAYING = "DISPLAYING";
	public static final String LOADED = "LOADED";
	public static final String LOADING = "LOADING";
	public static final String EARNED_REWARD = "EARNED_REWARD";

	public static AdMobEx getInstance() {
		if (instance == null) instance = new AdMobEx();
		return instance;
	}

	public static void init(String rewardedId, String appId, boolean testingAds, boolean tagForChildDirectedTreatment, HaxeObject callback, boolean initMobileAds) {
		AdMobEx.rewardedId = rewardedId;
		AdMobEx.testingAds = testingAds;
		AdMobEx.callback = callback;

		Log.d("AdMobEx MobileAds","appid " + appId);
		
		if (initMobileAds)
		{
			MobileAds.initialize(mainActivity.getApplicationContext(), new OnInitializationCompleteListener() {
				@Override
				public void onInitializationComplete(InitializationStatus initializationStatus) {
					getInstance();
				}
			});
		}
	}
	
	private static void reportRewardedEvent(final String event) {
		if (callback == null) return;
		
		if (Extension.mainView == null) return;

		GLSurfaceView view = (GLSurfaceView) Extension.mainView;
		view.queueEvent(new Runnable() {
			public void run() {
				callback.call1("_onRewardedEvent", event);
			}
		});
	}

	public static boolean showRewarded(final String rewardedId) {
		Log.d("AdMobEx showRewarded","Show Rewarded: Begins " + rewardedId);

		if (loadingRewarded) return false;
		if (failRewarded) {
			mainActivity.runOnUiThread(new Runnable() {
				public void run() { getInstance().reloadRewarded(AdMobEx.rewardedId);}
			});
			Log.d("AdMobEx showRewarded","Show Rewarded: Rewarded not loaded... reloading.");
			return false;
		}

		if (AdMobEx.rewardedId == "") {
			Log.d("AdMobEx showRewarded","Show Rewarded: RewardedID is empty... ignoring.");
			return false;
		}
		mainActivity.runOnUiThread(new Runnable() {
			public void run() {

				if (getInstance().rewardedAd == null)
				{
					reportRewardedEvent(AdMobEx.FAILED);
					Log.d("AdMobEx showRewarded","Show Rewarded: Not loaded (THIS SHOULD NEVER BE THE CASE HERE!)... ignoring.");
					return;
				}

				OnUserEarnedRewardListener rewardedCallback = new OnUserEarnedRewardListener() {

					@Override
					public void onUserEarnedReward(RewardItem rewardItem) {
						
						reportRewardedEvent(AdMobEx.EARNED_REWARD);
							Log.d("AdMobEx", "User earned reward");
					}
				};

				getInstance().rewardedAd.setFullScreenContentCallback(
					new FullScreenContentCallback() {
					@Override
					public void onAdShowedFullScreenContent() {

						reportRewardedEvent(AdMobEx.DISPLAYING);
						Log.d("AdMobEx", "Displaying Rewarded");
					}

					@Override
					public void onAdFailedToShowFullScreenContent(AdError adError) {

						AdMobEx.getInstance().failRewarded = true;

						getInstance().rewardedAd = null;

						reportRewardedEvent(AdMobEx.FAILED);
						Log.d("AdMobEx", "Fail to get Rewarded: " + adError.getCode());

						new Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								AdMobEx.getInstance().reloadRewarded(AdMobEx.rewardedId);
							}
						}, 5000);
					}

					@Override
					public void onAdDismissedFullScreenContent() {
						
						reportRewardedEvent(AdMobEx.CLOSED);
						Log.d("AdMobEx", "Dismiss Rewarded");

						getInstance().rewardedAd = null;

						new Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								AdMobEx.getInstance().reloadRewarded(AdMobEx.rewardedId);
							}
						}, 5000);
					}
				});

				getInstance().rewardedAd.show(mainActivity, rewardedCallback);
			}
		});
		Log.d("AdMobEx","Show Rewarded: end.");
		return true;
	}

	private AdMobEx() {

		adRequest = new AdRequest.Builder().build();

		if (testingAds) {
			String android_id = Secure.getString(mainActivity.getContentResolver(), Secure.ANDROID_ID);
			String deviceId = md5(android_id).toUpperCase();
			Log.d("AdMobEx","DEVICE ID: " + deviceId);

			List<String> testList = new ArrayList<String>();
			testList.add(deviceId);
			testList.add(AdRequest.DEVICE_ID_EMULATOR);

			RequestConfiguration configuration = new RequestConfiguration.Builder().setTestDeviceIds(testList).build();
			MobileAds.setRequestConfiguration(configuration);
		}

		this.reloadRewarded(rewardedId);
	}

	private void reloadRewarded(String rewardedId) {
		if (rewardedId == "") return;
		if (loadingRewarded) return;
		Log.d("AdMobEx","Reload Rewarded");
		
		reportRewardedEvent(AdMobEx.LOADING);
		loadingRewarded = true;

		RewardedAdLoadCallback adLoadCallback = new RewardedAdLoadCallback() {
			@Override
			public void onAdLoaded(RewardedAd rewardedAd) {
				getInstance().rewardedAd = rewardedAd;
				AdMobEx.getInstance().loadingRewarded = false;
				reportRewardedEvent(AdMobEx.LOADED);
				Log.d("AdMobEx","Received Rewarded!");
			}

			@Override
			public void onAdFailedToLoad(LoadAdError adError) { 
				AdMobEx.getInstance().loadingRewarded = false;
				AdMobEx.getInstance().failRewarded = true;

				getInstance().rewardedAd = null;

				reportRewardedEvent(AdMobEx.FAILED);
				Log.d("AdMobEx","Fail to get Rewarded : " + adError.getCode());

				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						AdMobEx.getInstance().reloadRewarded(AdMobEx.rewardedId);
					}
				}, 5000);
			}
		};
		rewardedAd.load(mainActivity, AdMobEx.rewardedId, adRequest, adLoadCallback);
		failRewarded = false;
	}

	private static String md5(String s)  {
		MessageDigest digest;
		try  {
			digest = MessageDigest.getInstance("MD5");
			digest.update(s.getBytes(),0,s.length());
			String hexDigest = new java.math.BigInteger(1, digest.digest()).toString(16);
			if (hexDigest.length() >= 32) return hexDigest;
			else return "00000000000000000000000000000000".substring(hexDigest.length()) + hexDigest;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

}

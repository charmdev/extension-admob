package admobex;

import android.provider.Settings.Secure;
import android.util.Log;
import com.google.android.gms.ads.AdRequest;

import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import org.haxe.extension.Extension;
import org.haxe.lime.HaxeObject;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import android.opengl.GLSurfaceView;

import android.os.Handler;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

public class AdMobEx extends Extension {

	private RewardedAd rewarded;
	private AdRequest adReq;

	private static Boolean failRewarded=false;
	private static Boolean loadingRewarded=false;
	private static String rewardedId= null;

	private static AdMobEx instance=null;
	private static Boolean testingAds=false;
	private static Boolean tagForChildDirectedTreatment=false;

	private static HaxeObject callback=null;

	public static final String LEAVING = "LEAVING";
	public static final String FAILED = "FAILED";
	public static final String CLOSED = "CLOSED";
	public static final String DISPLAYING = "DISPLAYING";
	public static final String LOADED = "LOADED";
	public static final String LOADING = "LOADING";
	public static final String EARNED_REWARD = "EARNED_REWARD";

	public static AdMobEx getInstance(){
		if(instance==null) instance = new AdMobEx();
		return instance;
	}

	public static void init(String rewardedId, String appId, boolean testingAds, boolean tagForChildDirectedTreatment, HaxeObject callback){
		AdMobEx.rewardedId=rewardedId;
		AdMobEx.testingAds=testingAds;
		AdMobEx.callback=callback;
		AdMobEx.tagForChildDirectedTreatment=tagForChildDirectedTreatment;

		Log.d("AdMobEx MobileAds","appid " + appId);
		
		MobileAds.initialize(mainActivity.getApplicationContext(), appId);

		mainActivity.runOnUiThread(new Runnable() {
			public void run() { getInstance(); }
		});
	}

	private static void reportRewardedEvent(final String event){
		reportRewardedEvent(event, null);
	}
	private static void reportRewardedEvent(final String event, final String data){
		if(callback == null) return;
		
		if (Extension.mainView == null) return;

		GLSurfaceView view = (GLSurfaceView) Extension.mainView;
		view.queueEvent(new Runnable() {
			public void run() {
				callback.call2("_onRewardedEvent", event, data);
			}
		});
	}

	public static boolean showRewarded(final String rewardedId) {
		Log.d("AdMobEx showRewarded","Show Rewarded: Begins " + rewardedId);

		if(loadingRewarded) return false;
		if(failRewarded){
			mainActivity.runOnUiThread(new Runnable() {
				public void run() { getInstance().reloadRewarded(AdMobEx.rewardedId);}
			});
			Log.d("AdMobEx showRewarded","Show Rewarded: Rewarded not loaded... reloading.");
			return false;
		}

		if(AdMobEx.rewardedId=="") {
			Log.d("AdMobEx showRewarded","Show Rewarded: RewardedID is empty... ignoring.");
			return false;
		}
		mainActivity.runOnUiThread(new Runnable() {
			public void run() {

				if(!getInstance().rewarded.isLoaded())
				{
					reportRewardedEvent(AdMobEx.FAILED);
					Log.d("AdMobEx showRewarded","Show Rewarded: Not loaded (THIS SHOULD NEVER BE THE CASE HERE!)... ignoring.");
					return;
				}
				RewardedAdCallback rewardedCallback = new RewardedAdCallback() {

					public void onRewardedAdFailedToShow(int errorcode) {
						AdMobEx.getInstance().failRewarded = true;

						reportRewardedEvent(AdMobEx.FAILED);
						Log.d("AdMobEx", "Fail to get Rewarded: " + errorcode);

						new Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								AdMobEx.getInstance().reloadRewarded(AdMobEx.rewardedId);
							}
						}, 5000);
					}

					public void onRewardedAdClosed() {

						reportRewardedEvent(AdMobEx.CLOSED);
						Log.d("AdMobEx", "Dismiss Rewarded");

						new Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								AdMobEx.getInstance().reloadRewarded(AdMobEx.rewardedId);
							}
						}, 5000);
					}

					public void onRewardedAdOpened() {
						reportRewardedEvent(AdMobEx.DISPLAYING);
						Log.d("AdMobEx", "Displaying Rewarded");
					}

					public void onUserEarnedReward(final RewardItem reward) {
						String data = "{\"type\": \"" + reward.getType() + "\", \"amount\": \"" + reward.getAmount() +"\"}";
						reportRewardedEvent(AdMobEx.EARNED_REWARD, data);
						Log.d("AdMobEx", "User earned reward " + data);
					}
				};

				getInstance().rewarded.show(mainActivity, rewardedCallback);
			}
		});
		Log.d("AdMobEx","Show Rewarded: end.");
		return true;
	}

	private AdMobEx() {
		AdRequest.Builder builder = new AdRequest.Builder();

		if (testingAds) {
			String android_id = Secure.getString(mainActivity.getContentResolver(), Secure.ANDROID_ID);
			String deviceId = md5(android_id).toUpperCase();
			Log.d("AdMobEx","DEVICE ID: " + deviceId);
			builder.addTestDevice(deviceId);
			builder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
		}
		
		if(tagForChildDirectedTreatment){
			Log.d("AdMobEx","Enabling COPPA support.");
			builder.tagForChildDirectedTreatment(true);
		}
		adReq = builder.build();

		this.reloadRewarded(rewardedId);
	}

	private void reloadRewarded(String rewardedId){
		if(rewardedId=="") return;
		if(loadingRewarded) return;
		Log.d("AdMobEx","Reload Rewarded");
		rewarded = new RewardedAd(mainActivity, rewardedId);
		reportRewardedEvent(AdMobEx.LOADING);
		loadingRewarded=true;
		RewardedAdLoadCallback adLoadCallback = new RewardedAdLoadCallback() {
			@Override
			public void onRewardedAdLoaded() {
				AdMobEx.getInstance().loadingRewarded=false;
				reportRewardedEvent(AdMobEx.LOADED);
				Log.d("AdMobEx","Received Rewarded!");
			}

			@Override
			public void onRewardedAdFailedToLoad(int errorCode) {
				AdMobEx.getInstance().loadingRewarded=false;
				AdMobEx.getInstance().failRewarded=true;

				reportRewardedEvent(AdMobEx.FAILED);
				Log.d("AdMobEx","Fail to get Rewarded: "+errorCode);

				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						AdMobEx.getInstance().reloadRewarded(AdMobEx.rewardedId);
					}
				}, 5000);
			}
		};
		rewarded.loadAd(adReq, adLoadCallback);
		failRewarded=false;
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

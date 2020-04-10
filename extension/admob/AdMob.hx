package extension.admob;

import haxe.Json;
import nme.Lib;
import nme.JNI;

typedef RewardItem = {
    var type:String;
    var amount:Int;
}

class AdMob {

	private static var initialized:Bool=false;
	private static var testingAds:Bool=false;
	private static var childDirected:Bool=false;

	private static var __initIos:String->String->Array<String>->String->Bool->Bool->Dynamic->Dynamic->Void = function(bannerId:String, interstitialId:String, rewardedIds:Array<String>, gravityMode:String, testingAds:Bool, tagForChildDirectedTreatment:Bool, callback:Dynamic, callback2:Dynamic){};

	private static var __initAndroid:String->String->Bool->Bool->Dynamic->Void = function(rewardedId:String, appId:String, testingAds:Bool, tagForChildDirectedTreatment:Bool, callback:Dynamic){};

	private static var __showRewarded:String->Bool = function(rewardedId:String){ return false; };

	private static var completeCB:Void->Void;
	private static var skipCB:Void->Void;
	private static var rewardFlag:Bool;
	private static var _rewardedId:String;
	private static var _appId:String;
	private static var canshow:Bool = false;

	public static function canShowAds():Bool {
		trace("admob canShowAds", canshow);
		return canshow;
	}

	public static function showRewarded(cb, skip):Bool {
		completeCB = cb;
		skipCB = skip;

		canshow = false;

		try{
			return __showRewarded(AdMob._rewardedId);
		}catch(e:Dynamic){
			trace("ShowRewarded Exception: "+e);
		}
		return false;
	}

	public static function tagForChildDirectedTreatment(){
		if ( childDirected ) return;
		if ( initialized ) {
			var msg:String;
			msg = "FATAL ERROR: If you want to set tagForChildDirectedTreatment, you must enable them before calling INIT!.\n";
			msg+= "Throwing an exception to avoid displaying ads withtou tagForChildDirectedTreatment.";
			trace(msg);
			throw msg;
			return;
		}
		childDirected = true;		
	}
	
	public static function enableTestingAds() {
		if ( testingAds ) return;
		if ( initialized ) {
			var msg:String;
			msg = "FATAL ERROR: If you want to enable Testing Ads, you must enable them before calling INIT!.\n";
			msg+= "Throwing an exception to avoid displaying read ads when you want testing ads.";
			trace(msg);
			throw msg;
			return;
		}
		testingAds = true;
	}

	public static function init(rewardedId:String, appId:String){

		AdMob._rewardedId = rewardedId;
		AdMob._appId = appId;

		if (initialized) return;
		initialized = true;

		#if android
		try{
			__initAndroid = JNI.createStaticMethod("admobex/AdMobEx", "init", "(Ljava/lang/String;Ljava/lang/String;ZZLorg/haxe/lime/HaxeObject;)V");
			__showRewarded = JNI.createStaticMethod("admobex/AdMobEx", "showRewarded", "(Ljava/lang/String;)Z");
			__initAndroid(rewardedId, appId, testingAds, childDirected, getInstance());
		}catch(e:Dynamic){
			trace("Android INIT Exception: "+e);
		}
		#elseif ios
		try{
			__initIos = cpp.Lib.load("adMobEx","admobex_init",8);
			__showRewarded = cpp.Lib.load("adMobEx","admobex_rewarded_show",1);
			__initIos("","",[rewardedId],"",testingAds, childDirected, getInstance()._onEvent, getInstance()._onRewardedEvent);
		}catch(e:Dynamic){
			trace("iOS INIT Exception: "+e);
		}
		#end
	}
	

	public static inline var LEAVING:String = "LEAVING";
	public static inline var FAILED:String = "FAILED";
	public static inline var CLOSED:String = "CLOSED";
	public static inline var DISPLAYING:String = "DISPLAYING";
	public static inline var LOADED:String = "LOADED";
	public static inline var LOADING:String = "LOADING";
	public static inline var EARNED_REWARD:String = "EARNED_REWARD";

	////////////////////////////////////////////////////////////////////////////

	public static var onRewardedEvent:String->RewardItem->Void = null;
	private static var instance:AdMob = null;

	private static function getInstance():AdMob{
		if (instance == null) instance = new AdMob();
		return instance;
	}

	////////////////////////////////////////////////////////////////////////////

	private function new(){}

	public function _onEvent(event:String){
		
	}

	public function _onRewardedEvent(event:String, ?data:String){
		if(onRewardedEvent != null) {
			try{
				var item:RewardItem = null;
				if(data != null) item = Json.parse(data);

				trace("onRewardedEvent", event);

				switch (event)
				{
					case LOADED:
						canshow = true;

					case EARNED_REWARD:
						rewardFlag = true;

					case FAILED:
						skipCB();
						rewardFlag = false;
						canshow = false;

					case CLOSED:
						rewardFlag ? completeCB() : skipCB();
						rewardFlag = false;
						canshow = false;
				}

				onRewardedEvent(event, item);
			}
			catch(err:Dynamic){
				trace("ERROR PARSING ", data, " err : ", err);
			}
		}
		else trace("Rewarded event: "+event+ " (assign AdMob.onRewardedEvent to get this events and avoid this traces)");
	}
	
}

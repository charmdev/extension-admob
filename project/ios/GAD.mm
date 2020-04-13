#import <AdMobEx.h>
#import <UIKit/UIKit.h>
#import "GoogleMobileAds/GADRewardedAd.h"

extern "C" void reportRewardedEvent (const char* event, const char* data);
static const char* ADMOB_LEAVING = "LEAVING";
static const char* ADMOB_FAILED = "FAILED";
static const char* ADMOB_CLOSED = "CLOSED";
static const char* ADMOB_DISPLAYING = "DISPLAYING";
static const char* ADMOB_LOADED = "LOADED";
static const char* ADMOB_LOADING = "LOADING";
static const char* ADMOB_EARNED_REWARD = "EARNED_REWARD";

////////////////////////////////////////////////////////////////////////

static bool _admobexChildDirected;

GADRequest *_admobexGetGADRequest(){
    GADRequest *request = [GADRequest request];
    if(_admobexChildDirected){
        NSLog(@"AdMobEx: enabling COPPA support");
        [GADMobileAds.sharedInstance.requestConfiguration tagForChildDirectedTreatment:YES];
    }
    return request;        
}

////////////////////////////////////////////////////////////////////////


@interface RewardedListener : NSObject <GADRewardedAdDelegate> {
    @public
    GADRewardedAd         *ad;
    NSString              *adId;
}

- (id)initWithID:(NSString*)ID;
- (void)show;
- (bool)isReady;

@end

@implementation RewardedListener

- (id)initWithID:(NSString*)ID {
    self = [super init];
    if(!self) return nil;
    adId = ID;
    ad = [[GADRewardedAd alloc] initWithAdUnitID:ID];
   
    //GADMobileAds.sharedInstance.requestConfiguration.testDeviceIdentifiers = @[ kGADSimulatorID ];

    [self loadRequest];

    NSLog(@"AdMob Loading Rewarded");
    reportRewardedEvent(ADMOB_LOADING, nil);
    return self;
}

- (void)loadRequest{

    GADRequest *request = _admobexGetGADRequest();
    [ad loadRequest:request completionHandler:^(GADRequestError * _Nullable error) {
        if (error) {
            NSLog(@"rewardedDidFailToReceiveAdWithError: %@", [error localizedDescription]);
            reportRewardedEvent(ADMOB_FAILED, nil);
        } else {
            NSLog(@"rewardedDidReceiveAd");
            reportRewardedEvent(ADMOB_LOADED, nil);
        }
    }];
}

- (bool)isReady{
    return (ad != nil && ad.isReady);
}

- (void)show{
    if (![self isReady]) return;
    [ad presentFromRootViewController:[[[UIApplication sharedApplication] keyWindow] rootViewController] delegate:self];
}

/// Tells the delegate that the user earned a reward.
- (void)rewardedAd:(GADRewardedAd *)rewardedAd userDidEarnReward:(GADAdReward *)reward {
    NSString *data = [NSString stringWithFormat:@"{\"type\": \"%@\", \"amount\": \"%@\"}", reward.type, reward.amount];
    NSLog(@"rewardedUserDidEarnReward: %@", data);
    reportRewardedEvent(ADMOB_EARNED_REWARD, [data UTF8String]);
}

/// Tells the delegate that the rewarded ad was presented.
- (void)rewardedAdDidPresent:(GADRewardedAd *)rewardedAd {
    NSLog(@"rewardedDidPresentScreen");
    reportRewardedEvent(ADMOB_DISPLAYING, nil);
}

/// Tells the delegate that the rewarded ad failed to present.
- (void)rewardedAd:(GADRewardedAd *)rewardedAd didFailToPresentWithError:(NSError *)error {
    NSLog(@"rewardedDidFailToPresentWithError: %@", [error localizedDescription]);
    reportRewardedEvent(ADMOB_FAILED, nil);
}

/// Tells the delegate that the rewarded ad was dismissed.
- (void)rewardedAdDidDismiss:(GADRewardedAd *)rewardedAd {
    NSLog(@"rewardedDidDismiss");
    reportRewardedEvent(ADMOB_CLOSED, nil);
}

@end

namespace admobex {
	
    static NSMutableDictionary *rewardeds = [[NSMutableDictionary alloc]init];
    static NSMutableArray *rewardedIDs = [[NSMutableArray alloc] init];
	UIViewController *root;
    
	void init(const char *__BannerID, const char *__InterstitialID, std::vector<std::string> &__rewardedIDs, const char *gravityMode, bool testingAds, bool tagForChildDirectedTreatment){
		root = [[[UIApplication sharedApplication] keyWindow] rootViewController];
        
        _admobexChildDirected = tagForChildDirectedTreatment;
        for (auto p : __rewardedIDs) {
			[rewardedIDs addObject:[NSString stringWithUTF8String:p.c_str()]];
		}

        //GADMobileAds.sharedInstance.requestConfiguration.testDeviceIdentifiers = @[ kGADSimulatorID ];
		
        for(NSString* rewardedID in rewardedIDs){
            [rewardeds setObject:[[RewardedListener alloc] initWithID:rewardedID] forKey:rewardedID];
        }
    }
    

    bool showRewarded(const char *rewardedId){
        RewardedListener *rewardedListener = [rewardeds objectForKey:[NSString stringWithUTF8String:rewardedId]];
        if(rewardedListener==nil) return false;
        if(![rewardedListener isReady]) return false;
        [rewardedListener show];
        [rewardeds setObject:[[RewardedListener alloc] initWithID:[NSString stringWithUTF8String:rewardedId]] forKey:[NSString stringWithUTF8String:rewardedId]];
        return true;
    }
}

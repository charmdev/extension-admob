#import "ViewController.h"
#import <GoogleMobileAds/GoogleMobileAds.h>

extern "C" void reportRewardedEvent (const char* event, const char* data);

static const char* ADMOB_FAILED = "FAILED";
static const char* ADMOB_CLOSED = "CLOSED";
static const char* ADMOB_DISPLAYING = "DISPLAYING";
static const char* ADMOB_LOADED = "LOADED";
static const char* ADMOB_LOADING = "LOADING";
static const char* ADMOB_EARNED_REWARD = "EARNED_REWARD";

@interface ViewController () <GADFullScreenContentDelegate>

@property(nonatomic, strong) GADRewardedAd *rewardedAd;
@property(nonatomic, strong) NSString *adId;

@end

@implementation ViewController

- (id)initWithID:(NSString*)ID {
	
	self = [super init];
	if(!self) return nil;
	self.adId = ID;

	if (!self.rewardedAd) {
		[self loadRewardedAd];
	}

	return self;
}

- (void)loadRewardedAd {

	NSLog(@"Rewarded loading Rewarded ad");
	reportRewardedEvent(ADMOB_LOADING, nil);

	GADRequest *request = [GADRequest request];

	[GADRewardedAd loadWithAdUnitID:self.adId request:request completionHandler:^(GADRewardedAd *ad, NSError *error) {
		if (error) {
			NSLog(@"Rewarded Rewarded ad failed to load with error");
			reportRewardedEvent(ADMOB_FAILED, nil);
			return;
		}
		
		self.rewardedAd = ad;
		NSLog(@"Rewarded Rewarded ad loaded");
		self.rewardedAd.fullScreenContentDelegate = self;

		reportRewardedEvent(ADMOB_LOADED, nil);
	}];
}

- (void)show {

	UIViewController *rootViewController = [[[UIApplication sharedApplication] keyWindow] rootViewController];

	if (self.rewardedAd && [self.rewardedAd canPresentFromRootViewController:rootViewController error:nil]) {
		[self.rewardedAd presentFromRootViewController:rootViewController userDidEarnRewardHandler:^ {
		
			reportRewardedEvent(ADMOB_EARNED_REWARD, "reward");
		}];
	}
	else
	{
		NSLog(@"Admob Rewarded ad not ready");
	}
}

- (void)adDidPresentFullScreenContent:(id)ad {

	NSLog(@"Admob Rewarded ad displaying");
	reportRewardedEvent(ADMOB_DISPLAYING, nil);
}

- (void)ad:(id)ad didFailToPresentFullScreenContentWithError:(NSError *)error {

	NSLog(@"Admob Rewarded ad failed to present with error");
	reportRewardedEvent(ADMOB_FAILED, nil);
}

- (void)adDidDismissFullScreenContent:(id)ad {
	
	NSLog(@"Admob Rewarded ad dismissed");
	reportRewardedEvent(ADMOB_CLOSED, nil);
	[self loadRewardedAd];
}
@end

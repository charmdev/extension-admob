#include <AdMobEx.h>
#include "ViewController.m"

namespace admobex {
	
	static ViewController *vc;
	
	void init(const char *rewardedId_) {
		NSString *rewardedId = [NSString stringWithUTF8String:rewardedId_];
		vc = [[ViewController alloc] initWithID:rewardedId];
	}
	
	bool showRewarded(const char *rewardedId) {
		if (vc == nil || !vc) return false;
		[vc show];
		return true;
	}
}

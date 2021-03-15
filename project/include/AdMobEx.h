#include <string>
#include <vector>

#ifndef ADMOBEX_H
#define ADMOBEX_H


namespace admobex {

	void init(const char *BannerID, const char *InterstitialID, std::vector<std::string> &rewardedIDs, const char *gravityMode, bool testingAds, bool tagForChildDirectedTreatment);
	
	bool showRewarded(const char* rewarded_id);

}

#endif
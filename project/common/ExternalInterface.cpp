#ifndef STATIC_LINK
#define IMPLEMENT_API
#endif

#if defined(HX_WINDOWS) || defined(HX_MACOS) || defined(HX_LINUX)
#define NEKO_COMPATIBLE
#endif


#include <hx/CFFI.h>

#include <string>
#include <vector>

#include <AdMobEx.h>

using namespace admobex;

AutoGCRoot* rewardedEventHandle = NULL;

static value admobex_init(value *args, int count){
	
	value on_rewarded_event = args[7];
	
	rewardedEventHandle = new AutoGCRoot(on_rewarded_event);
	
	bool testing_ads = val_bool(args[4]);
	bool child_directed_treatment = val_bool(args[5]);

	std::vector<std::string> rewarded_id_arr;
	int n = val_array_size(args[2]);
	for (int i=0;i<n;++i) {
		std::string str(val_string(val_array_i(args[2], i)));
		rewarded_id_arr.push_back(str);
	}

	init("", "", rewarded_id_arr, "", testing_ads, child_directed_treatment);

	return alloc_null();
}
DEFINE_PRIM_MULT(admobex_init);

extern "C" void admobex_main () {	
	val_int(0); // Fix Neko init
	
}
DEFINE_ENTRY_POINT (admobex_main);


static value admobex_rewarded_show(value rewarded_id){
	return alloc_bool(showRewarded(val_string(rewarded_id)));
}
DEFINE_PRIM(admobex_rewarded_show,1);

extern "C" int admobex_register_prims () { return 0; }

extern "C" void reportRewardedEvent(const char* event, const char* data)
{
	if(rewardedEventHandle == NULL) return;
	if(data == NULL)
		val_call1(rewardedEventHandle->get(), alloc_string(event));
	else
    	val_call2(rewardedEventHandle->get(), alloc_string(event), alloc_string(data));
}

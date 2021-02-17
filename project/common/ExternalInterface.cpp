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

static value admobex_init(value rewarded_id, value on_rewarded_event){
	
	rewardedEventHandle = new AutoGCRoot(on_rewarded_event);

	init(val_string(rewarded_id));

	return alloc_null();
}
DEFINE_PRIM(admobex_init,2);

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
	if (rewardedEventHandle == NULL) return;

	if (data == NULL)
		val_call1(rewardedEventHandle->get(), alloc_string(event));
	else
		val_call2(rewardedEventHandle->get(), alloc_string(event), alloc_string(data));
}

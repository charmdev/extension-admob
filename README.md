## extension-admob

OpenFL extension for "Google AdMob" on iOS and Android.
This extension allows you to easily integrate Google AdMob on your OpenFL (or HaxeFlixel) game / application.

#Features
 * Rewarded Video
 
You must set the app id in your project.xml
```xml
<haxelib name="extension-admob" />
<setenv name="ADMOB_APPID" value="ca-app-pub-XXXXX123457" if="android"/>
<setenv name="ADMOB_APPID" value="ca-app-pub-XXXXX123458" if="ios"/>
```
iOS : the admob appid is added in ::EXPORT::/ios/::APP::/extension-admob-Info.plist. You must merge this file with ::APP::-Info.plist manually or with /usr/libexec/PlistBuddy


```xml

import extension.admob.AdMob;

#if ios

AdMob.init("PlacementID");
AdMob.showRewarded(
  function(){
    trace("CB VIDEO SUCCESSFUL");
  },
  function() {
    trace("CB VIDEO SKIPPED");
  }
);

#elseif android

AdMob.init("PlacementID");
AdMob.showRewarded(
  function(){
    trace("CB VIDEO SUCCESSFUL");
  },
  function() {
    trace("CB VIDEO SKIPPED");
  }
);

#end

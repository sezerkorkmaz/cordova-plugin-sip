# cordova-plugin-sip
<h2>SIP plugin for Cordova & Phonegap Apps (IOS and Android)</h2>

<h3>IOS</h3>

<h4>Build Settings -> Header Search Paths</h4>

```
    "$(SRCROOT)/YOURPROJECTNAME/Plugins/cordova-plugin-sip/include"
    "$(SRCROOT)/YOURPROJECTNAME/Plugins/cordova-plugin-sip/include/belle-sip"
    "$(SRCROOT)/YOURPROJECTNAME/Plugins/cordova-plugin-sip/include/ortp"
    "$(SRCROOT)/YOURPROJECTNAME/Plugins/cordova-plugin-sip/include/linphone"
    "$(SRCROOT)/YOURPROJECTNAME/Plugins/cordova-plugin-sip/include/mediastreamer2"
```

<h4>You must import these files in the  Bridging Header File</h4>

```
    #include "Plugins/cordova-plugin-sip/include/linphone/lpconfig.h"
    #include "Plugins/cordova-plugin-sip/include/linphone/linphonecore.h"
    #include "Plugins/cordova-plugin-sip/include/linphone/linphonecore_utils.h"
```

<h4>IOS Permissions</h4>
  
You must include following permissions
```
        <key>NSCameraUsageDescription</key>
        <string>Description Why you use this permission</string>
        <key>NSMicrophoneUsageDescription</key>
        <string>Description Why you use this permission</string>
```


<h3>Android </h3>

Deploy and Run!



<h3>Usage</h3>

Update 1/2/2018 - Typescript definitions are partially implemented.  Will finish soon.


```
    var sipManager = {
        register: function () {
            cordova.plugins.sip.login('203', '203', '192.168.1.111:5060', function (e) {

                if (e == 'RegistrationSuccess') {
                    console.log(e);
                    sipManager.listen();
                } else {
                    alert("Registration Failed!");
                }

            }, function (e) { console.log(e) })
        },
        call: function () {
            cordova.plugins.sip.call('sip:111@192.168.1.111:5060', '203', sipManager.events, sipManager.events)
        },
        listen: function () {
            cordova.plugins.sip.listenCall(sipManager.events, sipManager.events);
        },
        hangup: function () {
            cordova.plugins.sip.hangup(function (e) { console.log(e) }, function (e) { console.log(e) })
        },
        events: function (e) {
            console.log(e);
            if (e == 'Incoming') {
                var r = confirm("Incoming Call");
                if (r == true) {
                    cordova.plugins.sip.accept(true, sipManager.events, sipManager.events);
                } else {

                }
            }
            if (e == 'Connected') {
                alert("Connected!");
                sipManager.listen();
            }
            if (e == 'Error') {
                alert("Call Error!");
                sipManager.listen();
            }
            if (e == 'End') {
                alert("Call End!");
                sipManager.listen();
            }


        }
    }
```

<h3>Contributing</h3>

Fork it

Create your feature branch (git checkout -b my-new-feature)

Commit your changes (git commit -am 'Add some feature')

Push to the branch (git push origin my-new-feature)

Create new Pull Request

import { Injectable } from '@angular/core';
import { Plugin, IonicNativePlugin, Cordova } from '@ionic-native/core';

@Plugin({
    pluginName: 'Linphone', // should match the name of the wrapper class
    plugin: 'cordova-plugin-sip', // NPM package name
    pluginRef: 'cordova.plugins.sip', // name of the object exposed by the plugin
    //repo: 'https://github.com/hoerresb/WifiWizard', // plugin repository URL
    platforms: ['Android'] // supported platforms
})

@Injectable()
export class Linphone extends IonicNativePlugin {

    @Cordova()
    login(username: string, password: string, domain: string): Promise<any> {return;}

}
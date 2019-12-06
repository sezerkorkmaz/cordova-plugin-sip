package com.sip.linphone;
/*
LinphoneMiniManager.java
Copyright (C) 2014  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.view.SurfaceView;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.linphone.core.Address;
import org.linphone.core.AuthInfo;
import org.linphone.core.AuthMethod;
import org.linphone.core.Call;
import org.linphone.core.Call.State;
import org.linphone.core.CallLog;
import org.linphone.core.CallParams;
import org.linphone.core.CallStats;
import org.linphone.core.ChatMessage;
import org.linphone.core.ChatRoom;
import org.linphone.core.ConfiguringState;
import org.linphone.core.Content;
import org.linphone.core.Core;

import org.linphone.core.CoreException;
import org.linphone.core.EcCalibratorStatus;
import org.linphone.core.Factory;
import org.linphone.core.CoreListener;
import org.linphone.core.Event;
import org.linphone.core.Friend;
import org.linphone.core.FriendList;
import org.linphone.core.GlobalState;
import org.linphone.core.InfoMessage;
import org.linphone.core.PresenceModel;
import org.linphone.core.ProxyConfig;
import org.linphone.core.PublishState;
import org.linphone.core.RegistrationState;
import org.linphone.core.SubscriptionState;
import org.linphone.core.VersionUpdateCheckResult;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration.AndroidCamera;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Sylvain Berfini
 */
public class LinphoneMiniManager implements CoreListener {
	private static final String TAG = "LM_MNGR";
	public static LinphoneMiniManager mInstance;
	public static Context mContext;
	public static Core mCore;
    //public static LinphonePreferences mPrefs;
	public static Timer mTimer;
	public static SurfaceView mCaptureView;
	public CallbackContext mCallbackContext;
	public CallbackContext mLoginCallbackContext;

	public LinphoneMiniManager(Context c) {
		mContext = c;
		Factory.instance().setDebugMode(true, "Linphone Mini");
        //mPrefs = LinphonePreferences.instance();


		try {

			String basePath = mContext.getFilesDir().getAbsolutePath();
			copyAssetsFromPackage(basePath);
			mCore = Factory.instance().createCore(basePath + "/.linphonerc", basePath + "/linphonerc", mContext);
			initCoreValues(basePath);

			setUserAgent();
			setFrontCamAsDefault();
			startIterate();
			mInstance = this;
			mCore.setNetworkReachable(true); // Let's assume it's true
			mCore.addListener(this);
			mCaptureView = new SurfaceView(mContext);
			mCore.start();
		} catch (IOException e) {
			Log.e(new Object[]{"Error initializing Linphone",e.getMessage()});

		}
	}

    public Core getLc(){
        return mCore;
    }

	public static LinphoneMiniManager getInstance() {
		return mInstance;
	}

	public void destroy() {
		try {
			mTimer.cancel();
			mCore.stopRinging();
			mCore.stopConferenceRecording();
			mCore.stopDtmf();
			mCore.stopDtmfStream();
			mCore.stopEchoTester();
		}
		catch (RuntimeException e) {
		}
		finally {
			mCore = null;
			mInstance = null;
		}
	}

	private void startIterate() {
		TimerTask lTask = new TimerTask() {
			@Override
			public void run() {
				mCore.iterate();
			}
		};

		mTimer = new Timer("LinphoneMini scheduler");
		mTimer.schedule(lTask, 0, 20);
	}



	private void setUserAgent() {
		try {
			String versionName = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
			if (versionName == null) {
				versionName = String.valueOf(mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode);
			}
			mCore.setUserAgent("LinphoneMiniAndroid", versionName);
		} catch (NameNotFoundException e) {
		}
	}

	private void setFrontCamAsDefault() {
		int camId = 0;
		AndroidCamera[] cameras = AndroidCameraConfiguration.retrieveCameras();
		for (AndroidCamera androidCamera : cameras) {
			if (androidCamera.frontFacing)
				camId = androidCamera.id;
		}
		mCore.setVideoDevice(""+camId);
	}

	private void copyAssetsFromPackage(String basePath) throws IOException {
		String package_name = mContext.getPackageName();
		Resources resources = mContext.getResources();

		LinphoneMiniUtils.copyIfNotExist(mContext, resources.getIdentifier("oldphone_mono", "raw", package_name), basePath + "/oldphone_mono.wav");
		LinphoneMiniUtils.copyIfNotExist(mContext, resources.getIdentifier("ringback", "raw", package_name), basePath + "/ringback.wav");
		LinphoneMiniUtils.copyIfNotExist(mContext, resources.getIdentifier("toy_mono", "raw", package_name), basePath + "/toy_mono.wav");
		LinphoneMiniUtils.copyIfNotExist(mContext, resources.getIdentifier("linphonerc_default", "raw", package_name), basePath + "/.linphonerc");
		LinphoneMiniUtils.copyFromPackage(mContext, resources.getIdentifier("linphonerc_factory", "raw", package_name), new File(basePath + "/linphonerc").getName());
		LinphoneMiniUtils.copyIfNotExist(mContext, resources.getIdentifier("lpconfig", "raw", package_name), basePath + "/lpconfig.xsd");
		LinphoneMiniUtils.copyIfNotExist(mContext, resources.getIdentifier("rootca", "raw", package_name), basePath + "/rootca.pem");
		LinphoneMiniUtils.copyIfNotExist(mContext, resources.getIdentifier("vcard_grammar", "raw", package_name), basePath + "/vcard_grammar.pem");
		LinphoneMiniUtils.copyIfNotExist(mContext, resources.getIdentifier("cpim_grammar", "raw", package_name), basePath + "/cpim_grammar.pem");
	}

	private void initCoreValues(String basePath) {
//		mCore.setContext(mContext);
		mCore.setRing(null);
		mCore.setRootCa(basePath + "/rootca.pem");
		mCore.setPlayFile(basePath + "/toy_mono.wav");
		mCore.setCallLogsDatabasePath(basePath + "/linphone-history.db");

//		int availableCores = Runtime.getRuntime().availableProcessors();
//		mCore.getConfig(availableCores);
	}

    public void newOutgoingCall(String to, String displayName) {
        Address lAddress;
		lAddress = mCore.interpretUrl(to);

		ProxyConfig lpc = mCore.getDefaultProxyConfig();

		if (lpc!=null && lAddress.asStringUriOnly().equals(lpc.getDomain())) {
			return;
		}

        lAddress.setDisplayName(displayName);

        if(mCore.isNetworkReachable()) {
			CallParams params = mCore.createCallParams(mCore.getCurrentCall());
			params.enableVideo(false);
			mCore.inviteAddressWithParams(lAddress, params);
        } else {
            Log.e(new Object[]{"Error: Network unreachable"});
        }

    }

    public void terminateCall() {
        if (mCore.inCall()) {
            Call c = mCore.getCurrentCall();
            if (c != null) c.terminate();
        }
    }

    public boolean toggleEnableCamera() {
        if (mCore.inCall()) {
            boolean enabled = !mCore.getCurrentCall().cameraEnabled();
            enableCamera(mCore.getCurrentCall(), enabled);
            return enabled;
        }
        return false;
    }

    public boolean toggleEnableSpeaker() {
//        if (mCore.inCall()) {
//			boolean enabled = !mCore.;
//			mCore.enableSpeaker(enabled);
//            return enabled;
//        }
        return false;
    }

    public boolean toggleMute() {
//        if (mCore.inCall()) {
//			boolean enabled = !mCore.();
//			mCore.muteMic(enabled);
//			return enabled;
//        }
        return false;
    }

    public void enableCamera(Call call, boolean enable) {
        if (call != null) {
            call.enableCamera(enable);
		}
    }

    public void sendDtmf(char number) {
		mCore.getCurrentCall().sendDtmf(number);
    }

	public void updateCall() {
		Core lc = mCore;
		Call lCall = lc.getCurrentCall();
		if (lCall == null) {
			Log.e(new Object[]{"Trying to updateCall while not in call: doing nothing"});
		} else {
			CallParams params = lCall.getParams();
			lc.getCurrentCall().setParams(params);
		}
	}




	public void listenCall(CallbackContext callbackContext){
		mCallbackContext = callbackContext;
	}

	public void acceptCall(CallbackContext callbackContext) {
		mCallbackContext = callbackContext;
        Call call = mCore.getCurrentCall();
		if(call != null){
			call.accept();
		}

	}

	public void call(String address, String displayName, CallbackContext callbackContext) {

		mCallbackContext = callbackContext;
		newOutgoingCall(address, displayName);
	}

	public void hangup(CallbackContext callbackContext) {
		terminateCall();
	}

	public void login(String username, String password, String domain, CallbackContext callbackContext) {
		Factory lcFactory = Factory.instance();


		Address address = lcFactory.createAddress("sip:" + username + "@" + domain);
		username = address.getUsername();
		domain = address.getDomain();
		if(password != null) {
			mCore.addAuthInfo(lcFactory.createAuthInfo(username, (String)null ,password, (String)null, domain, domain));
		}


//			ProxyConfig proxyCfg = mCore.createProxyConfig("sip:" + username + "@" + domain, domain, (String)null, true);
		ProxyConfig proxyCfg = mCore.createProxyConfig();
		proxyCfg.edit();
		proxyCfg.setIdentityAddress(address);
		proxyCfg.done();

		proxyCfg.setServerAddr(domain);


		proxyCfg.enableRegister(true);
		mCore.addProxyConfig(proxyCfg);
		mCore.setDefaultProxyConfig(proxyCfg);




		mLoginCallbackContext = callbackContext;



	}

	@Override
	public void onGlobalStateChanged(Core core, GlobalState globalState, String s) {
		android.util.Log.d(TAG,"Global state changed");
		android.util.Log.d(TAG,globalState.name());
		android.util.Log.d(TAG,s);
	}

	@Override
	public void onRegistrationStateChanged(Core core, ProxyConfig proxyConfig, RegistrationState registrationState, String s) {
		if(registrationState == RegistrationState.Ok)
		{
			mLoginCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,"RegistrationSuccess"));

		}
		else if(registrationState == RegistrationState.Failed)
		{
			mLoginCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,"RegistrationFailed:: "+s));
		}
	}

	@Override
	public void onCallStateChanged(Core core, Call call, State state, String s) {
		if(state == State.Connected)
		{
			mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "Connected"));
		}
		else if(state == State.IncomingReceived)
		{
			mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,"Incoming"));
		}
		else if(state == State.End)
		{
			mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,"End"));
		}
		else if(state == State.Error)
		{
			mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,"Error"));
		}
		Log.d("Call state: " + state + "(" + s + ")");

	}

	@Override
	public void onNotifyPresenceReceived(Core core, Friend friend) {

	}

	@Override
	public void onNotifyPresenceReceivedForUriOrTel(Core core, Friend friend, String s, PresenceModel presenceModel) {

	}

	@Override
	public void onNewSubscriptionRequested(Core core, Friend friend, String s) {

	}

	@Override
	public void onAuthenticationRequested(Core core, AuthInfo authInfo, AuthMethod authMethod) {
		android.util.Log.d(TAG, "Authentication requested");
	}

	@Override
	public void onCallLogUpdated(Core core, CallLog callLog) {
		android.util.Log.d(TAG, "Call log updated"+callLog.toStr());
	}

	@Override
	public void onMessageReceived(Core core, ChatRoom chatRoom, ChatMessage chatMessage) {

	}

	@Override
	public void onMessageReceivedUnableDecrypt(Core core, ChatRoom chatRoom, ChatMessage chatMessage) {

	}

	@Override
	public void onIsComposingReceived(Core core, ChatRoom chatRoom) {

	}

	@Override
	public void onDtmfReceived(Core core, Call call, int i) {
		android.util.Log.d(TAG, "DTMF RECEIVED");
	}

	@Override
	public void onReferReceived(Core core, String s) {

	}

	@Override
	public void onCallEncryptionChanged(Core core, Call call, boolean b, String s) {

	}

	@Override
	public void onTransferStateChanged(Core core, Call call, State state) {

	}

	@Override
	public void onBuddyInfoUpdated(Core core, Friend friend) {

	}

	@Override
	public void onCallStatsUpdated(Core core, Call call, CallStats callStats) {
		android.util.Log.d(TAG, "Call stats updated:: Download bandwidth :: "+callStats.getDownloadBandwidth());
	}

	@Override
	public void onInfoReceived(Core core, Call call, InfoMessage infoMessage) {
		android.util.Log.d(TAG, "Info message received :: "+infoMessage.getContent().getStringBuffer());
	}

	@Override
	public void onSubscriptionStateChanged(Core core, Event event, SubscriptionState subscriptionState) {
		android.util.Log.d(TAG, "Subscription state changed :: "+subscriptionState.name());
	}

	@Override
	public void onNotifyReceived(Core core, Event event, String s, Content content) {

	}

	@Override
	public void onSubscribeReceived(Core core, Event event, String s, Content content) {

	}

	@Override
	public void onPublishStateChanged(Core core, Event event, PublishState publishState) {

	}

	@Override
	public void onConfiguringStatus(Core core, ConfiguringState configuringState, String s) {

	}

	@Override
	public void onNetworkReachable(Core core, boolean b) {
		android.util.Log.d(TAG, "Network reachable??"+b);
	}

	@Override
	public void onLogCollectionUploadStateChanged(Core core, Core.LogCollectionUploadState logCollectionUploadState, String s) {

	}

	@Override
	public void onLogCollectionUploadProgressIndication(Core core, int i, int i1) {

	}

	@Override
	public void onFriendListCreated(Core core, FriendList friendList) {

	}

	@Override
	public void onFriendListRemoved(Core core, FriendList friendList) {

	}

	@Override
	public void onCallCreated(Core core, Call call) {

	}

	@Override
	public void onVersionUpdateCheckResultReceived(Core core, VersionUpdateCheckResult versionUpdateCheckResult, String s, String s1) {

	}

	@Override
	public void onChatRoomStateChanged(Core core, ChatRoom chatRoom, ChatRoom.State state) {

	}

	@Override
	public void onQrcodeFound(Core core, String s) {

	}

	@Override
	public void onEcCalibrationResult(Core core, EcCalibratorStatus ecCalibratorStatus, int i) {

	}

	@Override
	public void onEcCalibrationAudioInit(Core core) {

	}

	@Override
	public void onEcCalibrationAudioUninit(Core core) {

	}
}



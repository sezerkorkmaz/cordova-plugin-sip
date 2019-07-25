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
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneCallStats;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneContent;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCore.EcCalibratorStatus;
import org.linphone.core.LinphoneCore.GlobalState;
import org.linphone.core.LinphoneCore.RegistrationState;
import org.linphone.core.LinphoneCore.RemoteProvisioningState;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListener;
import org.linphone.core.LinphoneEvent;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneFriendList;
import org.linphone.core.LinphoneInfoMessage;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.PublishState;
import org.linphone.core.SubscriptionState;
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
public class LinphoneMiniManager implements LinphoneCoreListener {
	public static LinphoneMiniManager mInstance;
	public static Context mContext;
	public static LinphoneCore mLinphoneCore;
    //public static LinphonePreferences mPrefs;
	public static Timer mTimer;
	public static SurfaceView mCaptureView;
	public CallbackContext mCallbackContext;
	public CallbackContext mLoginCallbackContext;

	public LinphoneMiniManager(Context c) {
		mContext = c;
		LinphoneCoreFactory.instance().setDebugMode(true, "Linphone Mini");
        //mPrefs = LinphonePreferences.instance();

		try {
			String basePath = mContext.getFilesDir().getAbsolutePath();
			copyAssetsFromPackage(basePath);
			mLinphoneCore = LinphoneCoreFactory.instance().createLinphoneCore(this, basePath + "/.linphonerc", basePath + "/linphonerc", null, mContext);
			initLinphoneCoreValues(basePath);

			setUserAgent();
			setFrontCamAsDefault();
			startIterate();
			mInstance = this;
	        mLinphoneCore.setNetworkReachable(true); // Let's assume it's true

			mCaptureView = new SurfaceView(mContext);

		} catch (LinphoneCoreException e) {
		} catch (IOException e) {
		}
	}

    public LinphoneCore getLc(){
        return mLinphoneCore;
    }

	public static LinphoneMiniManager getInstance() {
		return mInstance;
	}

	public void destroy() {
		try {
			mTimer.cancel();
			mLinphoneCore.destroy();
		}
		catch (RuntimeException e) {
		}
		finally {
			mLinphoneCore = null;
			mInstance = null;
		}
	}

	private void startIterate() {
		TimerTask lTask = new TimerTask() {
			@Override
			public void run() {
				mLinphoneCore.iterate();
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
			mLinphoneCore.setUserAgent("LinphoneMiniAndroid", versionName);
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
		mLinphoneCore.setVideoDevice(camId);
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
	}

	private void initLinphoneCoreValues(String basePath) {
		mLinphoneCore.setContext(mContext);
		mLinphoneCore.setRing(null);
		mLinphoneCore.setRootCA(basePath + "/rootca.pem");
		mLinphoneCore.setPlayFile(basePath + "/toy_mono.wav");
		mLinphoneCore.setChatDatabasePath(basePath + "/linphone-history.db");

		int availableCores = Runtime.getRuntime().availableProcessors();
		mLinphoneCore.setCpuCount(availableCores);
	}

    public void newOutgoingCall(String to, String displayName) {
        LinphoneAddress lAddress;
        try {
            lAddress = mLinphoneCore.interpretUrl(to);
            LinphoneProxyConfig isLowBandwidthConnection = mLinphoneCore.getDefaultProxyConfig();

            LinphoneProxyConfig lpc = mLinphoneCore.getDefaultProxyConfig();

            if (lpc!=null && lAddress.asStringUriOnly().equals(lpc.getIdentity())) {
                return;
            }
        } catch (LinphoneCoreException var8) {
            return;
        }

        lAddress.setDisplayName(displayName);

        if(mLinphoneCore.isNetworkReachable()) {
            try {
				//TODO:SEZER
                LinphoneCallParams params = mLinphoneCore.createCallParams(mLinphoneCore.getCurrentCall());
                params.setVideoEnabled(false);
                mLinphoneCore.inviteAddressWithParams(lAddress, params);
            } catch (LinphoneCoreException var7) {
                return;
            }
        } else {
            Log.e(new Object[]{"Error: Network unreachable"});
        }

    }

    public void terminateCall() {
        if (mLinphoneCore.isIncall()) {
            mLinphoneCore.terminateCall(mLinphoneCore.getCurrentCall());
        }
    }

    public boolean toggleEnableCamera() {
        if (mLinphoneCore.isIncall()) {
            boolean enabled = !mLinphoneCore.getCurrentCall().cameraEnabled();
            enableCamera(mLinphoneCore.getCurrentCall(), enabled);
            return enabled;
        }
        return false;
    }

    public boolean toggleEnableSpeaker() {
        if (mLinphoneCore.isIncall()) {
			boolean enabled = !mLinphoneCore.isSpeakerEnabled();
			mLinphoneCore.enableSpeaker(enabled);
            return enabled;
        }
        return false;
    }

    public boolean toggleMute() {
        if (mLinphoneCore.isIncall()) {
			boolean enabled = !mLinphoneCore.isMicMuted();
			mLinphoneCore.muteMic(enabled);
			return enabled;
        }
        return false;
    }

    public void enableCamera(LinphoneCall call, boolean enable) {
        if (call != null) {
            call.enableCamera(enable);
		}
    }

    public void sendDtmf(char number) {
		mLinphoneCore.sendDtmf(number);
    }

	public void updateCall() {
		LinphoneCore lc = mLinphoneCore;
		LinphoneCall lCall = lc.getCurrentCall();
		if (lCall == null) {
			Log.e(new Object[]{"Trying to updateCall while not in call: doing nothing"});
		} else {
			LinphoneCallParams params = lCall.getCurrentParamsCopy();
			lc.updateCall(lCall, (LinphoneCallParams) null);
		}
	}

	@Override
	public void globalState(LinphoneCore lc, GlobalState state, String message) {
		Log.d("Global state: " + state + "(" + message + ")");
	}

	@Override
	public void callState(LinphoneCore lc, LinphoneCall call, State cstate,
			String message) {

			if(cstate == State.Connected)
			{
				mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "Connected"));
			}
			else if(cstate == State.IncomingReceived)
			{
                mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,"Incoming"));
			}
			else if(cstate == State.CallEnd)
			{
				mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,"End"));
			}
			else if(cstate == State.Error)
			{
				mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,"Error"));
			}
		Log.d("Call state: " + cstate + "(" + message + ")");
	}

	@Override
	public void authInfoRequested(LinphoneCore linphoneCore, String s, String s1, String s2) {
		Log.d("authInfoRequested");
	}

	@Override
	public void authenticationRequested(LinphoneCore linphoneCore, LinphoneAuthInfo linphoneAuthInfo, LinphoneCore.AuthMethod authMethod) {
		Log.d("authenticationRequested");
	}

	@Override
	public void callStatsUpdated(LinphoneCore lc, LinphoneCall call,
			LinphoneCallStats stats) {

	}

	@Override
	public void callEncryptionChanged(LinphoneCore lc, LinphoneCall call,
			boolean encrypted, String authenticationToken) {

	}

	@Override
	public void registrationState(LinphoneCore lc, LinphoneProxyConfig cfg,
			RegistrationState cstate, String smessage) {
		if(cstate == RegistrationState.RegistrationOk)
		{
			mLoginCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,"RegistrationSuccess"));

		}
		else if(cstate == RegistrationState.RegistrationFailed)
		{
			mLoginCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,"RegistrationFailed:: "+smessage));
		}
	}

	@Override
	public void newSubscriptionRequest(LinphoneCore lc, LinphoneFriend lf,
			String url) {

	}

	@Override
	public void notifyPresenceReceived(LinphoneCore lc, LinphoneFriend lf) {
		Log.d("notifyPresenceReceived");
	}

	@Override
	public void messageReceived(LinphoneCore lc, LinphoneChatRoom cr,
			LinphoneChatMessage message) {
		Log.d("Message received from " + cr.getPeerAddress().asString() + " : " + message.getText() + "(" + message.getExternalBodyUrl() + ")");
	}

	@Override
	public void messageReceivedUnableToDecrypted(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom, LinphoneChatMessage linphoneChatMessage) {
		Log.d("messageReceivedUnableToDecrypted");
	}

	@Override
	public void isComposingReceived(LinphoneCore lc, LinphoneChatRoom cr) {
		Log.d("Composing received from " + cr.getPeerAddress().asString());
	}

	@Override
	public void dtmfReceived(LinphoneCore lc, LinphoneCall call, int dtmf) {

	}

	@Override
	public void ecCalibrationStatus(LinphoneCore lc, EcCalibratorStatus status,
			int delay_ms, Object data) {

	}

	@Override
	public void uploadProgressIndication(LinphoneCore linphoneCore, int i, int i1) {

	}

	@Override
	public void uploadStateChanged(LinphoneCore linphoneCore, LinphoneCore.LogCollectionUploadState logCollectionUploadState, String s) {

	}

	@Override
	public void friendListCreated(LinphoneCore linphoneCore, LinphoneFriendList linphoneFriendList) {

	}

	@Override
	public void friendListRemoved(LinphoneCore linphoneCore, LinphoneFriendList linphoneFriendList) {

	}

	@Override
	public void networkReachableChanged(LinphoneCore linphoneCore, boolean b) {

	}

	@Override
	public void notifyReceived(LinphoneCore lc, LinphoneCall call,
			LinphoneAddress from, byte[] event) {

	}

	@Override
	public void transferState(LinphoneCore lc, LinphoneCall call,
			State new_call_state) {

	}

	@Override
	public void infoReceived(LinphoneCore lc, LinphoneCall call,
			LinphoneInfoMessage info) {

	}

	@Override
	public void subscriptionStateChanged(LinphoneCore lc, LinphoneEvent ev,
			SubscriptionState state) {

	}

	@Override
	public void notifyReceived(LinphoneCore lc, LinphoneEvent ev,
			String eventName, LinphoneContent content) {
		Log.d("Notify received: " + eventName + " -> " + content.getDataAsString());
	}

	@Override
	public void publishStateChanged(LinphoneCore lc, LinphoneEvent ev,
			PublishState state) {

	}

	@Override
	public void configuringStatus(LinphoneCore lc,
			RemoteProvisioningState state, String message) {
		Log.d("Configuration state: " + state + "(" + message + ")");
	}

	@Override
	public void show(LinphoneCore lc) {

	}

	@Override
	public void displayStatus(LinphoneCore lc, String message) {

	}

	@Override
	public void displayMessage(LinphoneCore lc, String message) {

	}

	@Override
	public void displayWarning(LinphoneCore lc, String message) {

	}

	@Override
	public void fileTransferProgressIndication(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, int i) {

	}

	@Override
	public void fileTransferRecv(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, byte[] bytes, int i) {

	}

	@Override
	public int fileTransferSend(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, ByteBuffer byteBuffer, int i) {
		return 0;
	}

	public void listenCall(CallbackContext callbackContext){
		mCallbackContext = callbackContext;
	}

	public void acceptCall(CallbackContext callbackContext) {
		mCallbackContext = callbackContext;
        LinphoneCall call = mLinphoneCore.getCurrentCall();
        try {
            mLinphoneCore.acceptCall(call);
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
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
		try {
		//LinphoneAddress address = LinphoneCoreFactory.instance().createLinphoneAddress("sip:" + username + "@" + domain);



//		LinphonePreferences.AccountBuilder builder = new LinphonePreferences.AccountBuilder(getLc())
//				.setUsername(username)
//				.setDomain(domain)
//				.setPassword(password);

			LinphoneCoreFactory lcFactory = LinphoneCoreFactory.instance();


			LinphoneAddress address = lcFactory.createLinphoneAddress("sip:" + username + "@" + domain);
			username = address.getUserName();
			domain = address.getDomain();
			if(password != null) {
				mLinphoneCore.addAuthInfo(lcFactory.createAuthInfo(username, password, (String)null, domain));
			}


			LinphoneProxyConfig proxyCfg = mLinphoneCore.createProxyConfig("sip:" + username + "@" + domain, domain, (String)null, true);

			proxyCfg.enableRegister(true);
			mLinphoneCore.addProxyConfig(proxyCfg);
			mLinphoneCore.setDefaultProxyConfig(proxyCfg);




			mLoginCallbackContext = callbackContext;



			//builder.saveNewAccount();
		} catch (LinphoneCoreException e) {
			e.printStackTrace();
		}
	}

}



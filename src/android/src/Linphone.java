package com.sip.linphone;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipAudioCall;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.linphone.core.Core;
import org.linphone.core.ProxyConfig;
import org.linphone.mediastream.Log;

import java.util.Timer;

public class Linphone extends CordovaPlugin  {
    public static Linphone mInstance;
    public static LinphoneMiniManager mLinphoneManager;
    public static Core mLinphoneCore;
    public static Context mContext;
    private static final int RC_MIC_PERM = 2;
    public static Timer mTimer;
    public CallbackContext mListenCallback;
    CordovaInterface cordova;

    public SipManager manager = null;
    public SipProfile me = null;
    public SipAudioCall call = null;


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        this.cordova = cordova;
        mContext = cordova.getActivity().getApplicationContext();
        mLinphoneManager = new LinphoneMiniManager(mContext);
        mLinphoneCore = mLinphoneManager.getLc();
        mInstance = this;
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext)
            throws JSONException {
        switch (action) {
            case "login":
                android.util.Log.d("CORE","LOGIN IN");
                login(args.getString(0), args.getString(1), args.getString(2), callbackContext);
                return true;
            case "logout":
                logout(callbackContext);
                return true;
            case "call":
                call(args.getString(0), args.getString(1), callbackContext);
                return true;
            case "listenCall":
                listenCall(callbackContext);
                return true;
            case "acceptCall":
                acceptCall(args.getString(0), callbackContext);
                return true;
            case "videocall":
                videocall(args.getString(0), args.getString(1), callbackContext);
                return true;
            case "hangup":
                hangup(callbackContext);
                return true;
            case "toggleVideo":
                toggleVideo(callbackContext);
                return true;
            case "toggleSpeaker":
                toggleSpeaker(callbackContext);
                return true;
            case "toggleMute":
                toggleMute(callbackContext);
                return true;
            case "sendDtmf":
                sendDtmf(args.getString(0), callbackContext);
                return true;
        }
        return false;
    }

    public void login(final String username, final String password, final String domain, final CallbackContext callbackContext) {
      if (!cordova.hasPermission(Manifest.permission.RECORD_AUDIO)) {
        cordova.requestPermission(this, RC_MIC_PERM, Manifest.permission.RECORD_AUDIO);
      }

      mLinphoneManager.login(username,password,domain,callbackContext);
    }

  @Override
  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {

  }

    public static synchronized void logout(final CallbackContext callbackContext) {
        try{
            Log.d("logout");
            ProxyConfig[] prxCfgs = mLinphoneManager.getLc().getProxyConfigList();
            final ProxyConfig proxyCfg = prxCfgs[0];
            mLinphoneManager.getLc().removeProxyConfig(proxyCfg);
            Log.d("logout sukses");
            callbackContext.success();
        }catch (Exception e){
            Log.d("Logout error", e.getMessage());
            callbackContext.error(e.getMessage());
        }
    }

    public static synchronized void call(final String address, final String displayName, final CallbackContext callbackContext) {
        try {
            mLinphoneManager.call(address,displayName,callbackContext);
        }catch (Exception e){
            Log.d("call error", e.getMessage());
        }
    }

    public static synchronized void hangup(final CallbackContext callbackContext) {
        try{
            mLinphoneManager.hangup(callbackContext);
        }catch (Exception e){
            Log.d("hangup error", e.getMessage());
        }
    }

    public static synchronized void listenCall( final CallbackContext callbackContext){
        mLinphoneManager.listenCall(callbackContext);
    }

    public static synchronized void acceptCall( final String isAcceptCall, final CallbackContext callbackContext){
        if("true".equals(isAcceptCall))
            mLinphoneManager.acceptCall(callbackContext);
        else
            mLinphoneManager.terminateCall();
    }

    public static synchronized void videocall(final String address, final String displayName, final CallbackContext callbackContext) {
        try{
            Log.d("incall", address, displayName);
            Intent intent = new Intent(mContext, LinphoneMiniActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("address", address);
            intent.putExtra("displayName", displayName);
            mContext.startActivity(intent);
            Log.d("incall sukses");
            callbackContext.success();
        }catch (Exception e){
            Log.d("incall error", e.getMessage());
            callbackContext.error(e.getMessage());
        }
    }

    public static synchronized void toggleVideo(final CallbackContext callbackContext) {
        try{
            boolean isenabled = mLinphoneManager.toggleEnableCamera();
            callbackContext.success(isenabled ? 1 : 0);
        }catch (Exception e){
            Log.d("toggleVideo error", e.getMessage());
            callbackContext.error(e.getMessage());
        }
    }

    public static synchronized void toggleSpeaker(final CallbackContext callbackContext) {
        try{
            Log.d("toggleSpeaker");
            boolean isenabled = mLinphoneManager.toggleEnableSpeaker();
            Log.d("toggleSpeaker sukses",isenabled);
            callbackContext.success(isenabled ? 1 : 0);
        }catch (Exception e){
            Log.d("toggleSpeaker error", e.getMessage());
            callbackContext.error(e.getMessage());
        }
    }

    public static synchronized void toggleMute(final CallbackContext callbackContext) {
        try{
            Log.d("toggleMute");
            boolean isenabled = mLinphoneManager.toggleMute();
            Log.d("toggleMute sukses",isenabled);
            callbackContext.success(isenabled ? 1 : 0);
        }catch (Exception e){
            Log.d("toggleMute error", e.getMessage());
            callbackContext.error(e.getMessage());
        }
    }

    public static synchronized void sendDtmf(final String number, final CallbackContext callbackContext) {
        try{
            Log.d("sendDtmf");
            mLinphoneManager.sendDtmf(number.charAt(0));
            Log.d("sendDtmf sukses",number);
            callbackContext.success();
        } catch (Exception e){
            Log.d("sendDtmf error", e.getMessage());
            callbackContext.error(e.getMessage());
        }
    }
}
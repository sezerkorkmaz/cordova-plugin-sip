package com.sip.linphone;

import android.content.Context;
import android.content.Intent;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.mediastream.Log;

import java.util.Timer;

public class Linphone extends CordovaPlugin  {
    public static Linphone mInstance;
    public static LinphoneMiniManager mLinphoneManager;
    public static LinphoneCore mLinphoneCore;
    public static Context mContext;
    public static Timer mTimer;
    public CallbackContext mListenCallback;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        mContext = cordova.getActivity().getApplicationContext();
        mLinphoneManager = new LinphoneMiniManager(mContext);
        mLinphoneCore = mLinphoneManager.getLc();
        mInstance = this;
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext)
            throws JSONException {
        if (action.equals("login")) {
            login(args.getString(0), args.getString(1), args.getString(2), callbackContext);
            return true;
        }else if (action.equals("logout")) {
            logout(callbackContext);
            return true;
        }else if (action.equals("call")) {
            call(args.getString(0), args.getString(1), callbackContext);
            return true;
        }else if(action.equals("listenCall")){
            listenCall(callbackContext);
            return true;
        }
        else if(action.equals("acceptCall")){
            acceptCall(args.getString(0), callbackContext);
            return true;
        }else if (action.equals("videocall")) {
            videocall(args.getString(0), args.getString(1), callbackContext);
            return true;
        }else if(action.equals("hangup")){
            hangup(callbackContext);
            return true;
        }else if(action.equals("toggleVideo")){
            toggleVideo(callbackContext);
            return true;
        }else if(action.equals("toggleSpeaker")){
            toggleSpeaker(callbackContext);
            return true;
        }else if(action.equals("toggleMute")){
            toggleMute(callbackContext);
            return true;
        }else if(action.equals("sendDtmf")){
            sendDtmf(args.getString(0), callbackContext);
            return true;
        }
        return false;
    }

    public static synchronized void login(final String username, final String password, final String domain, final CallbackContext callbackContext) {
        try{
            mLinphoneManager.login(username,password,domain,callbackContext);
        }catch (Exception e){
            Log.d("login error", e.getMessage());
            callbackContext.error(e.getMessage());
        }
    }

    public static synchronized void logout(final CallbackContext callbackContext) {
        try{
            Log.d("logout");
            LinphoneProxyConfig[] prxCfgs = mLinphoneManager.getLc().getProxyConfigList();
            final LinphoneProxyConfig proxyCfg = prxCfgs[0];
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
        if(isAcceptCall == "true")
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
        }catch (Exception e){
            Log.d("sendDtmf error", e.getMessage());
            callbackContext.error(e.getMessage());
        }
    }

}

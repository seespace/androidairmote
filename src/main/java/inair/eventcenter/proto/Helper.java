package inair.eventcenter.proto;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.view.Display;
import android.view.WindowManager;

import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Copyright (c) 2014 SeeSpace.co. All rights reserved.
 */
public final class Helper {

  //region Helper methods
  public static Proto.Event parseFrom(byte[] data) {
    try {
      return Proto.Event.parseFrom(data);
    } catch (InvalidProtocolBufferNanoException e) {
      e.printStackTrace();
    }

    return null;
  }

  public static long now() {
    return System.currentTimeMillis();
  }

  final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static byte[] dataFromEvent(Proto.Event event) {
    byte[] eventBA = MessageNano.toByteArray(event);
    byte[] lengthBA = ByteBuffer.allocate(4).putInt(eventBA.length).array();

    ByteBuffer bb = ByteBuffer.allocate(eventBA.length + lengthBA.length);
    bb.put(lengthBA);
    bb.put(eventBA);

    return bb.array();
  }

  static Point cachedScreenSize;

  public static Point screenSize(Context context) {
    if (cachedScreenSize == null) {
      WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
      Display display = wm.getDefaultDisplay();
      cachedScreenSize = new Point();
      display.getSize(cachedScreenSize);
    }

    return cachedScreenSize;
  }

  private static Proto.Event buildEvent(long timestamp, int height, int width, int subType, String replyTo, String target) {
    Proto.Event event = new Proto.Event();
    event.version = Proto.CURRENT;
    event.timestamp = timestamp;
    event.trackingAreaHeight = height;
    event.trackingAreaWidth = width;
    event.type = subType;
    event.deviceType = Proto.Device.ANDROID;
    if (replyTo != null) {
      event.replyTo = replyTo;
    }
    if (target != null) {
      event.target = target;
    }
    return event;
  }
  //endregion

  //region Device Message
  public static Proto.Device currentDevice(Context context) {
    Proto.Device device = new Proto.Device();
    device.name = Build.MODEL;
    device.type = Proto.Device.ANDROID;
    device.productId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    try {
      device.version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
    device.hasKeyboard = true;

    return device;
  }

  public static Proto.Event newDeviceEvent(Context context, long timestamp, int type, boolean error, String reason) {
    Proto.DeviceEvent event = new Proto.DeviceEvent();
    event.device = currentDevice(context);
    event.type = type;
    event.error = error;
    event.reason = reason;

    return buildEvent(timestamp, 0, 0, Proto.Event.DEVICE, null, null)
        .setExtension(Proto.DeviceEvent.event, event);
  }

  public static Proto.Event newDeviceEvent(long timestamp, int type, boolean error, String reason) {
    Proto.DeviceEvent event = new Proto.DeviceEvent();
    event.device = null;
    event.type = type;
    event.error = error;
    event.reason = reason;

    return buildEvent(timestamp, 0, 0, Proto.Event.DEVICE, null, null)
      .setExtension(Proto.DeviceEvent.event, event);
  }
  //endregion

  //region Gesture
  private static Proto.Event buildGestureEvent(long timestamp, int trackingAreaHeight, int trackingAreaWidth, int subType) {
    return buildEvent(timestamp, trackingAreaHeight, trackingAreaWidth, subType, null, null);
  }

  public static Proto.Event newTouchEvent(long timestamp, float locationX, float locationY, int trackingAreaWidth, int trackingAreaHeight, int phase) {
    Proto.TouchEvent event = new Proto.TouchEvent();
    event.locationX = locationX;
    event.locationY = locationY;
    event.phase = phase;

    return buildGestureEvent(timestamp, trackingAreaHeight, trackingAreaWidth, Proto.Event.TOUCH)
        .setExtension(Proto.TouchEvent.event, event);
  }

  public static Proto.Event newKeypressEvent(long timestamp, int state, int keyCode) {
    Proto.KeypressEvent event = new Proto.KeypressEvent();
    event.state = state;
    event.keycode = keyCode;

    return buildGestureEvent(timestamp, 0, 0, Proto.Event.KEYPRESS)
        .setExtension(Proto.KeypressEvent.event, event);
  }

  public static Proto.Event newTapEvent(long timestamp, float locationX, float locationY, int trackingAreaWidth, int trackingAreaHeight, int state, int tapCount) {
    Proto.GestureEvent event = new Proto.GestureEvent();
    event.locationX = locationX;
    event.locationY = locationY;
    event.type = Proto.GestureEvent.TAP;
    event.state = state;
    event.tapCount = tapCount;

    return buildGestureEvent(timestamp, trackingAreaHeight, trackingAreaWidth, Proto.Event.GESTURE)
        .setExtension(Proto.GestureEvent.event, event);
  }

  public static Proto.Event newPanEvent(long timestamp, float locationX, float locationY, int trackingAreaWidth, int trackingAreaHeight, int state, float translationX, float translationY, float velocityX, float velocityY) {
    Proto.GestureEvent event = new Proto.GestureEvent();
    event.locationX = locationX;
    event.locationY = locationY;
    event.type = Proto.GestureEvent.PAN;
    event.state = state;
    event.panTranslationX = translationX;
    event.panTranslationY = translationY;
    event.panVelocityX = velocityX;
    event.panVelocityY = velocityY;

    return buildGestureEvent(timestamp, trackingAreaHeight, trackingAreaWidth, Proto.Event.GESTURE)
        .setExtension(Proto.GestureEvent.event, event);
  }

  public static Proto.Event newSwipeEvent(long timestamp, float locationX, float locationY, int trackingAreaWidth, int trackingAreaHeight, int state, int direction) {
    Proto.GestureEvent event = new Proto.GestureEvent();
    event.locationX = locationX;
    event.locationY = locationY;
    event.type = Proto.GestureEvent.SWIPE;
    event.state = state;
    event.swipeDirection = direction;

    return buildGestureEvent(timestamp, trackingAreaHeight, trackingAreaWidth, Proto.Event.GESTURE)
        .setExtension(Proto.GestureEvent.event, event);
  }

  public static Proto.Event newLongPressEvent(long timestamp, float locationX, float locationY, int trackingAreaWidth, int trackingAreaHeight, int state, long duration) {
    Proto.GestureEvent event = new Proto.GestureEvent();
    event.locationX = locationX;
    event.locationY = locationY;
    event.type = Proto.GestureEvent.LONGPRESS;
    event.state = state;
    event.pressDuration = duration;

    return buildGestureEvent(timestamp, trackingAreaHeight, trackingAreaWidth, Proto.Event.GESTURE)
        .setExtension(Proto.GestureEvent.event, event);
  }
  //endregion

  //region OAuth
  public static Proto.Event newOAuthRequestEvent(String authUrl, String replyTo) {
    Proto.OAuthRequestEvent event = new Proto.OAuthRequestEvent();
    event.authUrl = authUrl;

    return buildEvent(now(), 0, 0, Proto.Event.OAUTH_REQUEST, replyTo, null)
        .setExtension(Proto.OAuthRequestEvent.event, event);
  }

  public static Proto.Event newOAuthResponseEvent(String authCode, String target) {
    Proto.OAuthResponseEvent event = new Proto.OAuthResponseEvent();
    event.authCode = authCode;

    return buildEvent(now(), 0, 0, Proto.Event.OAUTH_RESPONSE, null, target)
        .setExtension(Proto.OAuthResponseEvent.event, event);
  }
  //endregion

  //region Text input
  public static Proto.Event newTextInputRequestEvent() {
    return newTextInputRequestEvent(Proto.TextInputRequestEvent.PASSWORD);
  }

  public static Proto.Event newTextInputRequestEvent(int type) {
    return newTextInputRequestEvent(type, 0);
  }

  public static Proto.Event newTextInputRequestEvent(int type, int maxLength) {
    Proto.TextInputRequestEvent event = new Proto.TextInputRequestEvent();
    event.type = type;

    if (maxLength > 0) {
      event.maxLength = maxLength;
    }

    return buildEvent(now(), 0, 0, Proto.Event.TEXT_INPUT_REQUEST, null, null)
        .setExtension(Proto.TextInputRequestEvent.event, event);
  }
  //endregion

  //region Wifi setup
  public static Proto.Event newCodeResponseEvent(String code) {
    Proto.SetupResponseEvent event = new Proto.SetupResponseEvent();
    event.phase = Proto.REQUEST_CODE;
    event.error = false;
    event.code = code;

    return buildEvent(now(), 0, 0, Proto.Event.SETUP_RESPONSE, null, null)
        .setExtension(Proto.SetupResponseEvent.event, event);
  }

  public static Proto.Event newRenameResponseEvent(boolean error, String errorMessage) {
    Proto.SetupResponseEvent event = new Proto.SetupResponseEvent();
    event.phase = Proto.REQUEST_RENAME;
    event.error = error;
    event.errorMessage = errorMessage;

    return buildEvent(now(), 0, 0, Proto.Event.SETUP_RESPONSE, null, null)
        .setExtension(Proto.SetupResponseEvent.event, event);
  }

  public static Proto.Event newRenameResponseEvent() {
    return newRenameResponseEvent(false, "");
  }

  public static Proto.Event newNetworkListResponseEvent(String errorMessage) {
    Proto.SetupResponseEvent event = new Proto.SetupResponseEvent();
    event.phase = Proto.REQUEST_WIFI_SCAN;
    event.error = true;
    event.errorMessage = errorMessage;

    return buildEvent(now(), 0, 0, Proto.Event.SETUP_RESPONSE, null, null)
        .setExtension(Proto.SetupResponseEvent.event, event);
  }

  public static Proto.Event newNetworkListResponseEvent(List<ScanResult> scanResults) {
    Proto.SetupResponseEvent event = new Proto.SetupResponseEvent();
    event.phase = Proto.REQUEST_WIFI_SCAN;
    event.error = false;

    if (scanResults.size() > 0) {
      event.wifiNetworks = new Proto.WifiNetwork[scanResults.size()];
      for (int i = 0; i < scanResults.size(); i++) {
        ScanResult result = scanResults.get(i);
        Proto.WifiNetwork network = new Proto.WifiNetwork();
        network.ssid = result.SSID;
        network.bssid = result.BSSID;
        network.strength = WifiManager.calculateSignalLevel(result.level, 4);
        network.capabilities = result.capabilities;
        event.wifiNetworks[i] = network;
      }
    }

    return buildEvent(now(), 0, 0, Proto.Event.SETUP_RESPONSE, null, null)
        .setExtension(Proto.SetupResponseEvent.event, event);
  }

  public static Proto.Event newNetworkConnectResponseEvent() {
    return newNetworkConnectResponseEvent(false, "");
  }

  public static Proto.Event newNetworkConnectResponseEvent(boolean error, String errorMessage) {
    Proto.SetupResponseEvent event = new Proto.SetupResponseEvent();
    event.phase = Proto.REQUEST_WIFI_CONNECT;
    event.error = error;
    event.errorMessage = errorMessage;

    return buildEvent(now(), 0, 0, Proto.Event.SETUP_RESPONSE, null, null)
        .setExtension(Proto.SetupResponseEvent.event, event);
  }
  //endregion

  //region WebView
  public static Proto.Event newWebViewRequestEvent(String url, String replyTo) {
    Proto.WebViewRequestEvent event = new Proto.WebViewRequestEvent();
    event.url = url;

    return buildEvent(now(), 0, 0, Proto.Event.WEBVIEW_REQUEST, replyTo, null)
      .setExtension(Proto.WebViewRequestEvent.event, event);
  }

  public static Proto.Event newWebViewResponseEvent(String data, String replyTo) {
    Proto.WebViewResponseEvent event = new Proto.WebViewResponseEvent();
    event.data = data;

    return buildEvent(now(), 0, 0, Proto.Event.WEBVIEW_RESPONSE, replyTo, null)
      .setExtension(Proto.WebViewResponseEvent.event, event);
  }
  //endregion

  //region Ping Pong
  public static Proto.Event newPingEvent() {
    Proto.PingEvent event = new Proto.PingEvent();

    return buildEvent(now(), 0, 0, Proto.Event.PING, null, null)
      .setExtension(Proto.PingEvent.event, event);
  }

  public static Proto.Event newPongEvent() {
    Proto.PongEvent event = new Proto.PongEvent();

    return buildEvent(now(), 0, 0, Proto.Event.PONG, null, null)
      .setExtension(Proto.PongEvent.event, event);
  }
  //endregion

  //region Wifi setup request
  public static Proto.Event setupWifiScanRequest() {
    Proto.SetupRequestEvent event = new Proto.SetupRequestEvent();
    event.phase = Proto.REQUEST_WIFI_SCAN;
    return buildEvent(now(), 0, 0, Proto.Event.SETUP_REQUEST, null, null).setExtension(Proto.SetupRequestEvent.event, event);
  }

  public static Proto.Event setupWifiConnectRequestWithSSID(String ssid, String password) {
    Proto.SetupRequestEvent event = new Proto.SetupRequestEvent();
    event.phase = Proto.REQUEST_WIFI_CONNECT;
    event.ssid = ssid;
    event.password = password;
    return buildEvent(now(), 0, 0, Proto.Event.SETUP_REQUEST, null, null).setExtension(Proto.SetupRequestEvent.event, event);
  }
  //endregion

  public static Proto.Event newFunctionEvent(int key) {
    Proto.FunctionEvent event = new Proto.FunctionEvent();
    event.key = key;
    return buildEvent(now(), 0, 0, Proto.Event.FUNCTION_EVENT, null, null).setExtension(Proto.FunctionEvent.event, event);
  }
}

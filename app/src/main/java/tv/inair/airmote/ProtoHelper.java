package tv.inair.airmote;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.provider.Settings;
import android.view.Display;
import android.view.WindowManager;
import com.google.protobuf.ExtensionRegistryLite;
import inair.eventcenter.proto.Proto;

import java.nio.ByteBuffer;

/**
 * Copyright (c) 2014 SeeSpace.co. All rights reserved.
 */
public class ProtoHelper {
  private static ProtoHelper ourInstance = new ProtoHelper();

  public static ProtoHelper getInstance() {
    return ourInstance;
  }

  private ExtensionRegistryLite mRegistry;

  private ProtoHelper() {
    mRegistry = ExtensionRegistryLite.newInstance();
    Proto.registerAllExtensions(mRegistry);
  }

  public static long now() {
    return System.currentTimeMillis();
  }

  final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for ( int j = 0; j < bytes.length; j++ ) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static byte[] dataFromEvent(Proto.Event event) {
    byte[] eventBA = event.toByteArray();
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

  public static Proto.Device currentDevice(Context context) {
    PackageInfo pInfo = null;
    try {
      pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
    int appVersion = pInfo.versionCode;

    String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    Proto.Device device = Proto.Device.newBuilder()
      .setName(android.os.Build.MODEL)
      .setVendor(Proto.Device.Vendor.ANDROID)
      .setProductId(androidId)
      .setVersion(appVersion)
      .setHasKeyboard(true)
      .build();

    return device;
  }

  public static Proto.Event newDeviceEvent(Context context, long timestamp, Proto.DeviceEvent.Type type) {
    Proto.DeviceEvent event = Proto.DeviceEvent.newBuilder()
      .setDevice(currentDevice(context))
      .setType(type)
      .build();

    return Proto.Event.newBuilder()
      .setTimestamp(timestamp)
      .setTrackingAreaWidth(0)
      .setTrackingAreaHeight(0)
      .setType(Proto.Event.Type.DEVICE)
      .setExtension(Proto.DeviceEvent.event, event)
      .build();
  }

  public static Proto.Event newTouchEvent(long timestamp, float locationX, float locationY, int trackingAreaWidth, int trackingAreaHeight, Proto.Phase phase) {
    Proto.TouchEvent event = Proto.TouchEvent.newBuilder()
      .setLocationX(locationX)
      .setLocationY(locationY)
      .setPhase(phase)
      .build();

    return Proto.Event.newBuilder()
      .setTimestamp(timestamp)
      .setTrackingAreaWidth(trackingAreaWidth)
      .setTrackingAreaHeight(trackingAreaHeight)
      .setType(Proto.Event.Type.TOUCH)
      .setExtension(Proto.TouchEvent.event, event)
      .build();
  }

  public static Proto.Event newKeypressEvent(long timestamp, Proto.KeypressEvent.State state, int keyCode) {
    Proto.KeypressEvent event = Proto.KeypressEvent.newBuilder()
      .setState(state)
      .setKeycode(keyCode)
      .build();

    return Proto.Event.newBuilder()
      .setTimestamp(timestamp)
      .setTrackingAreaWidth(0)
      .setTrackingAreaHeight(0)
      .setType(Proto.Event.Type.KEYPRESS)
      .setExtension(Proto.KeypressEvent.event, event)
      .build();
  }

  public static Proto.Event newTapEvent(long timestamp, float locationX, float locationY, int trackingAreaWidth, int trackingAreaHeight, Proto.GestureEvent.State state, int tapCount) {
    Proto.GestureEvent event = Proto.GestureEvent.newBuilder()
      .setLocationX(locationX)
      .setLocationY(locationY)
      .setType(Proto.GestureEvent.Type.TAP)
      .setState(state)
      .setTapCount(tapCount)
      .build();

    return Proto.Event.newBuilder()
      .setTimestamp(timestamp)
      .setTrackingAreaWidth(trackingAreaWidth)
      .setTrackingAreaHeight(trackingAreaHeight)
      .setType(Proto.Event.Type.GESTURE)
      .setExtension(Proto.GestureEvent.event, event)
      .build();
  }

  public static Proto.Event newPanEvent(long timestamp, float locationX, float locationY, int trackingAreaWidth, int trackingAreaHeight, Proto.GestureEvent.State state, float translationX, float translationY, float velocityX, float velocityY) {
    Proto.GestureEvent event = Proto.GestureEvent.newBuilder()
      .setLocationX(locationX)
      .setLocationY(locationY)
      .setType(Proto.GestureEvent.Type.PAN)
      .setState(state)
      .setPanTranslationX(translationX)
      .setPanTranslationY(translationY)
      .setPanVelocityX(velocityX)
      .setPanVelocityY(velocityY)
      .build();

    return Proto.Event.newBuilder()
      .setTimestamp(timestamp)
      .setTrackingAreaWidth(trackingAreaWidth)
      .setTrackingAreaHeight(trackingAreaHeight)
      .setType(Proto.Event.Type.GESTURE)
      .setExtension(Proto.GestureEvent.event, event)
      .build();
  }

  public static Proto.Event newSwipeEvent(long timestamp, float locationX, float locationY, int trackingAreaWidth, int trackingAreaHeight, Proto.GestureEvent.State state, Proto.GestureEvent.SwipeDirection direction) {
    Proto.GestureEvent event = Proto.GestureEvent.newBuilder()
      .setLocationX(locationX)
      .setLocationY(locationY)
      .setType(Proto.GestureEvent.Type.SWIPE)
      .setState(state)
      .setSwipeDirection(direction)
      .build();

    return Proto.Event.newBuilder()
      .setTimestamp(timestamp)
      .setTrackingAreaWidth(trackingAreaWidth)
      .setTrackingAreaHeight(trackingAreaHeight)
      .setType(Proto.Event.Type.GESTURE)
      .setExtension(Proto.GestureEvent.event, event)
      .build();
  }

  public static Proto.Event newLongPressEvent(long timestamp, float locationX, float locationY, int trackingAreaWidth, int trackingAreaHeight, Proto.GestureEvent.State state, long duration) {
    Proto.GestureEvent event = Proto.GestureEvent.newBuilder()
      .setLocationX(locationX)
      .setLocationY(locationY)
      .setType(Proto.GestureEvent.Type.LONGPRESS)
      .setState(state)
      .setPressDuration(duration)
      .build();

    return Proto.Event.newBuilder()
      .setTimestamp(timestamp)
      .setTrackingAreaWidth(trackingAreaWidth)
      .setTrackingAreaHeight(trackingAreaHeight)
      .setType(Proto.Event.Type.GESTURE)
      .setExtension(Proto.GestureEvent.event, event)
      .build();
  }

}

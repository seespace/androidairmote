package tv.inair.airmote;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class NsdHelper {

  public static class Item {
    public String mDisplayName;
    public String mHostName;
  }

  Context mContext;

  NsdManager mNsdManager;
  NsdManager.ResolveListener mResolveListener;
  NsdManager.DiscoveryListener mDiscoveryListener;
  NsdManager.RegistrationListener mRegistrationListener;

  public static final String TAG = "NsdHelper";
  public String mServiceName = "NsdAiRMote";

  private final Map<String, Item> mMap = new HashMap<>();

  public DeviceAdapter mAdapter;

  public NsdHelper(Context context) {
    mContext = context;
    mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

    initializeResolveListener();
    initializeDiscoveryListener();
    initializeRegistrationListener();

    mAdapter = new DeviceAdapter(context);
  }

  public void initializeDiscoveryListener() {
    mDiscoveryListener = new NsdManager.DiscoveryListener() {
      @Override
      public void onDiscoveryStarted(String regType) {
        Log.d(TAG, "Service discovery started");
      }
      @Override
      public void onServiceFound(NsdServiceInfo service) {
        Log.d(TAG, "Service discovery success " + service);
        if (!service.getServiceType().equals("")) {
          Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
        } else if (service.getServiceName().equals(mServiceName)) {
          Log.d(TAG, "Same machine: " + mServiceName);
        } else if (!service.getServiceName().contains(mServiceName)) {
          mNsdManager.resolveService(service, mResolveListener);
        }
      }
      @Override
      public void onServiceLost(NsdServiceInfo service) {
        Log.e(TAG, "service lost" + service);
      }

      @Override
      public void onDiscoveryStopped(String serviceType) {
        Log.i(TAG, "Discovery stopped: " + serviceType);
      }

      @Override
      public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "Discovery failed: Error code:" + errorCode);
        mNsdManager.stopServiceDiscovery(this);
      }

      @Override
      public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "Discovery failed: Error code:" + errorCode);
        mNsdManager.stopServiceDiscovery(this);
      }
    };
  }

  public void initializeResolveListener() {
    mResolveListener = new NsdManager.ResolveListener() {
      @Override
      public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.e(TAG, "Resolve failed" + errorCode);
      }

      @Override
      public void onServiceResolved(NsdServiceInfo serviceInfo) {
        Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

        if (serviceInfo.getServiceName().equals(mServiceName)) {
          Log.d(TAG, "Same IP.");
          return;
        }
        if (!mMap.containsKey(serviceInfo.getHost().getHostAddress())) {
          Item device = new Item();
          device.mDisplayName = serviceInfo.getServiceName().replace("\\032", " ");
          device.mHostName = serviceInfo.getHost().getHostAddress();
          mMap.put(device.mHostName, device);
          mAdapter.addItem(device);
        }
      }
    };
  }

  public void initializeRegistrationListener() {
    mRegistrationListener = new NsdManager.RegistrationListener() {
      @Override
      public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
        mServiceName = NsdServiceInfo.getServiceName();
      }

      @Override
      public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
      }

      @Override
      public void onServiceUnregistered(NsdServiceInfo arg0) {
      }

      @Override
      public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
      }

    };
  }

  public void registerService() {
    NsdServiceInfo serviceInfo = new NsdServiceInfo();
    serviceInfo.setPort(8989);
    serviceInfo.setServiceName(mServiceName);
    serviceInfo.setServiceType("_irpc._tcp.");
    mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
  }

  public void discoverServices() {
//    mMap.clear();
//    mAdapter.clear();
//    mNsdManager.discoverServices("_irpc._tcp.", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
  }

  public void stopDiscovery() {
//    mNsdManager.stopServiceDiscovery(mDiscoveryListener);
  }

  public void tearDown() {
//    mNsdManager.unregisterService(mRegistrationListener);
  }
}

package tv.inair.airmote;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import inair.eventcenter.proto.Helper;
import inair.eventcenter.proto.Proto;
import tv.inair.airmote.connection.OnEventReceived;
import tv.inair.airmote.connection.OnSocketStateChanged;
import tv.inair.airmote.connection.SocketClient;

/**
 * <p>
 * Note this class is currently under early design and development.
 * The API will likely change in later updates of the compatibility library,
 * requiring changes to the source code of apps when they are compiled against the newer version.
 * </p>
 * <p/>
 * <p>Copyright (c) 2015 SeeSpace.co. All rights reserved.</p>
 */
public class WifiListActivity extends ListActivity implements OnEventReceived, OnSocketStateChanged {
  //region Adapter
  private class RowItem {
    String ssid;
    int strength;
    String bssid;
    String capabilities;

    String password;

    boolean isOpenNetwork() {
      return !capabilities.contains("PSK") && !capabilities.contains("WEP");
    }

    boolean notRemove;
  }

  private class ListAdapter extends ArrayAdapter<RowItem> {
    public ListAdapter(Context context, int resourceId) {
      super(context, resourceId);
    }

    /*private view holder class*/
    private class ViewHolder {
      ImageView signalView;
      TextView ssidView;
    }

    public synchronized RowItem getItem(String ssid, String bssid) {
      if (ssid == null || bssid == null) {
        return null;
      }
      for (int i = 0; i < getCount(); i++) {
        RowItem item = getItem(i);
        if (ssid.equalsIgnoreCase(item.ssid) && bssid.equalsIgnoreCase(item.bssid)) {
          return item;
        }
      }
      return null;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      ViewHolder holder;
      RowItem rowItem = getItem(position);

      if (convertView == null) {
        LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        convertView = mInflater.inflate(R.layout.wifi_item, null);
        holder = new ViewHolder();
        holder.ssidView = (TextView) convertView.findViewById(R.id.ssid);
        holder.signalView = (ImageView) convertView.findViewById(R.id.signal);
        convertView.setTag(holder);
      } else {
        holder = (ViewHolder) convertView.getTag();
      }

      if (rowItem.isOpenNetwork()) {
        holder.signalView.setImageResource(R.drawable.wifi_signal_open_light);
      } else {
        holder.signalView.setImageResource(R.drawable.wifi_signal_lock_light);
      }
      holder.signalView.setImageLevel(rowItem.strength);
      holder.ssidView.setText(rowItem.ssid);

      return convertView;
    }
  }
  //endregion

  //region Local Handler
  public static final int MSG_DISMISS_DIALOG = 1;

  private static class LocalHandler extends Handler {
    WeakReference<WifiListActivity> mActivity;

    public LocalHandler(WifiListActivity activity) {
      mActivity = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
      if (mActivity == null || mActivity.get() == null || mActivity.get().mDestroyed) {
        return;
      }
      AlertDialog hud = mActivity.get().mHud;
      switch (msg.what) {
        case MSG_DISMISS_DIALOG:
          if (hud != null && hud.isShowing()) {
            hud.dismiss();
          }
          break;

        default:
          break;
      }
    }
  }
  //endregion

  private LocalHandler mHandler;
  private ListAdapter adapter;
  private AlertDialog mHud;
  private ProgressDialog mProgress;
  private AlertDialog mAlert;
  private SocketClient mClient;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setResult(RESULT_CANCELED);

    setTitle("Choose network");
    adapter = new ListAdapter(this, R.layout.wifi_item);
    setListAdapter(adapter);

    ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    mProgress = new ProgressDialog(this);
    mProgress.setIndeterminate(true);
    mAlert = new AlertDialog.Builder(this).create();
    mHandler = new LocalHandler(this);

    mClient = Application.getSocketClient();
    mClient.addEventReceivedListener(this);
    mClient.addSocketStateChangedListener(this);

    startScan();
  }

  private void startScan() {
    setupHUD("Scanning wifi", true).show();
    mClient.sendEvent(Helper.setupWifiScanRequest());
  }

  private boolean mDestroyed = false;
  @Override
  protected void onDestroy() {
    mDestroyed = true;
    mClient.changeToSettingMode(false);
    super.onDestroy();
  }

  public static final String EXTRA_POSITION = "#row_pos";

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    RowItem item = adapter.getItem(position);
    if (item.isOpenNetwork()) {
      mClient.sendEvent(Helper.setupWifiConnectRequestWithSSID(item.ssid, ""));
      finishSetup(true);
    } else {
      Intent i = new Intent(this, WifiConnectActivity.class);
      i.putExtra(WifiConnectActivity.EXTRA_SSID, item.ssid);
      i.putExtra(EXTRA_POSITION, position);
      startActivityForResult(i, MainActivity.REQUEST_WIFI_CONNECT);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode != Activity.RESULT_OK) {
      return;
    }
    switch (requestCode) {
      case MainActivity.REQUEST_WIFI_CONNECT:
        RowItem item = adapter.getItem(data.getIntExtra(EXTRA_POSITION, -1));
        if (item == null) {
          return;
        }
        item.password = data.getStringExtra(WifiConnectActivity.EXTRA_PASSWORD);
        setupHUD("Connecting to " + item.ssid, true).show();
        mClient.sendEvent(Helper.setupWifiConnectRequestWithSSID(item.ssid, item.password));
        break;
    }
  }

  final List<RowItem> mItems = new ArrayList<>();

  @Override
  public void onEventReceived(Proto.Event event) {
    if (event == null || event.type != Proto.Event.SETUP_RESPONSE) {
      return;
    }

    Proto.SetupResponseEvent responseEvent = event.getExtension(Proto.SetupResponseEvent.event);
    switch (responseEvent.phase) {
      case Proto.REQUEST_WIFI_SCAN:
        if (responseEvent.wifiNetworks == null || responseEvent.wifiNetworks.length == 0) {
          setupHUD("No wifi available", false).show();
          mItems.clear();
          adapter.clear();
          return;
        }

        synchronized (mItems) {
          for (Proto.WifiNetwork wifi : responseEvent.wifiNetworks) {
            RowItem item = adapter.getItem(wifi.ssid, wifi.bssid);
            if (item == null) {
              item = new RowItem();
              item.ssid = wifi.ssid;
              item.bssid = wifi.bssid;
              mItems.add(item);
              adapter.add(item);
            }
            item.strength = wifi.strength;
            item.capabilities = wifi.capabilities;
            item.notRemove = true;
          }

          for (int i = 0; i < mItems.size(); ++i) {
            RowItem item = mItems.get(i);
            if (!item.notRemove) {
              mItems.remove(i);
              adapter.remove(item);
              --i;
            }
          }
        }
        if (mHud != null) {
          mHud.dismiss();
        }
        break;

      case Proto.REQUEST_WIFI_CONNECT:
        if (mHud != null) {
          mHud.dismiss();
        }
        if (responseEvent.error) {
          setupHUD(responseEvent.errorMessage, false).show();
        } else {
          finishSetup(true);
        }
        break;
    }
  }

  @Override
  public void onStateChanged(boolean connect, String message) {
    if (mDestroyed) {
      return;
    }
    System.out.println("WifiListActivity.onStateChanged " + connect + " " + message);
    if (!connect) {
      adapter.clear();
      mItems.clear();
      //finishSetup(false);
      if (mHud != null) {
        mHud.dismiss();
        //Toast.makeText(this, "Fail to setup InAiR Device", Toast.LENGTH_LONG).show();
      }
    } else {
      startScan();
    }
  }

  private AlertDialog setupHUD(String mes, boolean isProgress) {
    if (mHud != null) {
      mHud.dismiss();
    }
    AlertDialog dialog = isProgress ? mProgress : mAlert;
    dialog.setMessage(mes);
    System.out.println("WifiListActivity.setupHUD " + mes);
    mHud = dialog;
    return mHud;
  }

  private void finishSetup(boolean ok) {
    if (mDestroyed) {
      return;
    }
    setupHUD(ok ? "Success" : "Fail to setup InAiR Device, please go back to main screen and try again", false);
    setResult(ok ? RESULT_OK : RESULT_CANCELED);
    mHud.setOnCancelListener(new DialogInterface.OnCancelListener() {
      @Override
      public void onCancel(DialogInterface dialog) {
        WifiListActivity.this.finish();
      }
    });
    mHud.setOnDismissListener(new DialogInterface.OnDismissListener() {
      @Override
      public void onDismiss(DialogInterface dialog) {
        WifiListActivity.this.finish();
      }
    });
    mHud.show();
    mHandler.sendEmptyMessageDelayed(MSG_DISMISS_DIALOG, ok ? 1000 : 5000);
  }
}

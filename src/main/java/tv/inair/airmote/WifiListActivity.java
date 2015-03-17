package tv.inair.airmote;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import inair.eventcenter.proto.Proto;
import tv.inair.airmote.connection.OnEventReceived;
import tv.inair.airmote.connection.SocketClient;
import tv.inair.airmote.remote.Helper;

/**
 * <p>
 * Note this class is currently under early design and development.
 * The API will likely change in later updates of the compatibility library,
 * requiring changes to the source code of apps when they are compiled against the newer version.
 * </p>
 * <p/>
 * <p>Copyright (c) 2015 SeeSpace.co. All rights reserved.</p>
 */
public class WifiListActivity extends Activity implements AdapterView.OnItemClickListener, OnEventReceived {

  public static final String EXTRA_SSID = "@wla_ssid";
  public static final String EXTRA_BSSID = "@wla_bssid";
  public static final String EXTRA_CAPABILITIES = "@wla_capabilities";
  public static final String EXTRA_PASSWORD = "@wla_password";

  private class RowItem {
    String ssid;
    int strength;
    String bssid;
    String capabilities;

    boolean isOpenNetwork() {
      return !capabilities.contains("PSK") && !capabilities.contains("WEP");
    }
  }

  private class WifiListAdapter extends ArrayAdapter<RowItem> {
    public WifiListAdapter(Context context, int resourceId) {
      super(context, resourceId);
    }

    /*private view holder class*/
    private class ViewHolder {
      ImageView signalView;
      TextView ssidView;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      ViewHolder holder;
      RowItem rowItem = getItem(position);

      LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
      if (convertView == null) {
        convertView = mInflater.inflate(R.layout.wifi_item, null);
        holder = new ViewHolder();
        holder.ssidView = (TextView) convertView.findViewById(R.id.ssid);
        holder.signalView = (ImageView) convertView.findViewById(R.id.signal);
        convertView.setTag(holder);
      } else {
        holder = (ViewHolder) convertView.getTag();
      }

      if (rowItem.isOpenNetwork()) {
        holder.signalView.setImageResource(R.drawable.wifi_signal_open_dark);
      } else {
        holder.signalView.setImageResource(R.drawable.wifi_signal_lock_dark);
      }
      holder.signalView.setImageLevel(rowItem.strength);
      holder.ssidView.setText(rowItem.ssid);

      return convertView;
    }
  }

  private WifiListAdapter adapter;
  private SocketClient mClient;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    setResult(RESULT_CANCELED);

    mClient = Application.getSocketClient();
    mClient.addEventReceivedListener(this);

    adapter = new WifiListAdapter(this, R.layout.wifi_item);

    Application.notify(this, "Scanning Wifi");
    mClient.sendEvent(Helper.setupWifiScanRequest());

    setContentView(R.layout.activity_list);

    setTitle("Choose a Network");

    // Find and set up the ListView for newly discovered devices
    ListView newDevicesListView = (ListView) findViewById(R.id.listview);
    newDevicesListView.setAdapter(adapter);
    newDevicesListView.setOnItemClickListener(this);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        mClient.changeToSettingMode(false);
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onDestroy() {
    mClient.changeToSettingMode(false);
    super.onDestroy();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK) {
      RowItem item = adapter.getItem(requestCode);
      connectToNetwork(item, data.getStringExtra(WifiConnectActivity.EXTRA_PASSWORD));
    }
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    RowItem item = adapter.getItem(position);
    if (item.isOpenNetwork()) {
      mClient.sendEvent(Helper.setupWifiConnectRequestWithSSID(item.ssid, ""));
      connectToNetwork(item, null);
    } else {
      Intent i = new Intent(this, WifiConnectActivity.class);
      i.putExtra(WifiConnectActivity.EXTRA_SSID, item.ssid);
      startActivityForResult(i, position);
    }
  }

  private void connectToNetwork(RowItem item, String password) {
    Intent res = new Intent();
    res.putExtra(EXTRA_SSID, item.ssid);
    res.putExtra(EXTRA_BSSID, item.bssid);
    res.putExtra(EXTRA_CAPABILITIES, item.capabilities);
    res.putExtra(EXTRA_PASSWORD, password);
    setResult(Activity.RESULT_OK, res);
    mClient.changeToSettingMode(false);
    finish();
  }

  @Override
  public void onEventReceived(Proto.Event event) {
    if (event != null && event.type != null) {
      Proto.SetupResponseEvent responseEvent = event.getExtension(Proto.SetupResponseEvent.event);
      assert responseEvent != null;
      if (responseEvent.phase == Proto.REQUEST_WIFI_SCAN) {
        if (responseEvent.wifiNetworks != null && responseEvent.wifiNetworks.length > 0) {
          adapter.clear();
          for (Proto.WifiNetwork wifi : responseEvent.wifiNetworks) {
            RowItem item = new RowItem();
            item.ssid = wifi.ssid;
            item.strength = wifi.strength;
            item.bssid = wifi.bssid;
            item.capabilities = wifi.capabilities;
            System.out.println("WifiListActivity.onEventReceived " + wifi.ssid + " " + wifi.capabilities + " " + wifi.strength + " " + item.bssid);
            adapter.add(item);
          }
        } else {
          Application.notify(this, "No wifi available");
        }
      }
    }
  }
}

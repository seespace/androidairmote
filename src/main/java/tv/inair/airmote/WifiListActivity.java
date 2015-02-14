package tv.inair.airmote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import tv.inair.airmote.connection.OnEventReceived;
import tv.inair.airmote.remote.Helper;
import tv.inair.airmote.remote.Proto;

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

  private class RowItem {
    int signal;
    String ssid;
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

      holder.signalView.setImageResource(rowItem.signal);
      holder.ssidView.setText(rowItem.ssid);

      return convertView;
    }
  }

  private WifiListAdapter adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Application.getSocketClient().addEventReceivedListener(this);

    adapter = new WifiListAdapter(this, R.layout.wifi_item);
    getWifiList();
    setContentView(R.layout.activity_list);

    setTitle("Connect to Wifi");

    // Find and set up the ListView for newly discovered devices
    ListView newDevicesListView = (ListView) findViewById(R.id.listview);
    newDevicesListView.setAdapter(adapter);
    newDevicesListView.setOnItemClickListener(this);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    Intent i = new Intent(this, WifiConnectActivity.class);
    i.putExtra(WifiConnectActivity.EXTRA_SSID, adapter.getItem(position).ssid);
    startActivityForResult(i, 0);
  }

  @Override
  public void onEventReceived(Proto.Event event) {
    if (event != null && event.type != null && event.type == Proto.Event.SETUP_RESPONSE) {
      Proto.SetupResponseEvent responseEvent = event.getExtension(Proto.SetupResponseEvent.event);
      switch (responseEvent.phase) {
        case Proto.REQUEST_WIFI_SCAN:
          if (responseEvent.wifiNetworks != null && responseEvent.wifiNetworks.length > 0) {
            adapter.clear();
            for (Proto.WifiNetwork wifi : responseEvent.wifiNetworks) {
              RowItem item = new RowItem();
              item.ssid = wifi.ssid;
              adapter.add(item);
            }
          } else {
            Toast.makeText(this, "No wifi available", Toast.LENGTH_SHORT).show();
            finish();
          }
          break;

        case Proto.REQUEST_WIFI_CONNECT: {
          if (responseEvent.error) {
            Toast.makeText(this, responseEvent.errorMessage, Toast.LENGTH_SHORT).show();
          } else {
            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
            finish();
          }
          break;
        }
      }
    }
  }

  private void getWifiList() {
    Application.getSocketClient().sendEvent(Helper.setupWifiScanRequest());
  }
}

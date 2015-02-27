package tv.inair.airmote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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
public class WifiConnectActivity extends Activity implements OnEventReceived {

  public static final String EXTRA_SSID = "extra_ssid";

  private EditText passwordView;
  private String ssid;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Application.getSocketClient().addEventReceivedListener(this);
    setResult(Activity.RESULT_CANCELED);

    setTitle("Connect to WIFI");
    setContentView(R.layout.activity_connect);

    Intent i = getIntent();
    ssid = i.getStringExtra(EXTRA_SSID);

    passwordView = ((EditText) findViewById(R.id.editText));
    TextView des = ((TextView) findViewById(R.id.description));
    des.setText(getResources().getString(R.string.connectDesTxt) + " " + ssid);
  }

  @Override
  public void onEventReceived(Proto.Event event) {
    if (event != null && event.type != null) {
      Proto.SetupResponseEvent responseEvent = event.getExtension(Proto.SetupResponseEvent.event);
      if (responseEvent.phase == Proto.REQUEST_WIFI_CONNECT) {
        if (responseEvent.error) {
          Toast.makeText(this, responseEvent.errorMessage, Toast.LENGTH_SHORT).show();
        } else {
          Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
          setResult(Activity.RESULT_OK);
          finish();
        }
      }
    }
  }

  public void onConnectButtonClicked(View view) {
    System.out.println("WifiConnectActivity.onConnectButtonClicked");
    Application.getSocketClient().sendEvent(Helper.setupWifiConnectRequestWithSSID(ssid, passwordView.getText().toString()));
  }
}

package tv.inair.airmote;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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
public class WifiConnectActivity extends Activity implements OnEventReceived {

  public static final String EXTRA_SSID = "#wca_ssid";
  public static final String EXTRA_PASSWORD = "#wca_password";

  private EditText passwordView;
  private String ssid;

  private SocketClient mClient;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    setResult(Activity.RESULT_CANCELED);

    mClient = Application.getSocketClient();
    mClient.addEventReceivedListener(this);

    setTitle("Enter Password");
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
      assert responseEvent != null;
      if (responseEvent.phase == Proto.REQUEST_WIFI_CONNECT) {
        if (responseEvent.error) {
          Application.notify(this, responseEvent.errorMessage);
        } else {
          System.out.println("WifiConnectActivity.onEventReceived connected");
          Intent res = new Intent();
          res.putExtra(EXTRA_SSID, ssid);
          res.putExtra(EXTRA_PASSWORD, passwordView.getText().toString());
          setResult(Activity.RESULT_OK, res);
          finish();
        }
      }
    }
  }

  public void onConnectButtonClicked(View view) {
    mClient.sendEvent(Helper.setupWifiConnectRequestWithSSID(ssid, passwordView.getText().toString()));
  }
}

package tv.inair.airmote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * <p>
 * Note this class is currently under early design and development.
 * The API will likely change in later updates of the compatibility library,
 * requiring changes to the source code of apps when they are compiled against the newer version.
 * </p>
 * <p/>
 * <p>Copyright (c) 2015 SeeSpace.co. All rights reserved.</p>
 */
public class WifiConnectActivity extends Activity {

  public static final String EXTRA_SSID = "#wca_ssid";
  public static final String EXTRA_PASSWORD = "#wca_password";

  private EditText passwordView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_connect);
    setTitle("Enter Password");

    setResult(Activity.RESULT_CANCELED);

    Intent i = getIntent();
    String ssid = i.getStringExtra(EXTRA_SSID);

    passwordView = ((EditText) findViewById(R.id.editText));
    TextView des = ((TextView) findViewById(R.id.description));
    des.setText(getResources().getString(R.string.connectDesTxt) + " " + ssid);

  }

  public void onConnectButtonClicked(View view) {
    Intent res = getIntent();
    res.putExtra(EXTRA_PASSWORD, passwordView.getText().toString());
    setResult(Activity.RESULT_OK, res);
    finish();
  }
}

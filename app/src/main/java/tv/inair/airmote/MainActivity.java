package tv.inair.airmote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.Toast;
import com.walnutlabs.android.ProgressHUD;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


public class MainActivity extends Activity {

  private static final String SERVER_IP_KEY = "SERVERIDKEY";
  private Client mClient;
  private Socket mSocket;
  private String mServerIp;
  private boolean isDialogDisplaying = false;

  private static final int SERVER_PORT = 8989;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    if (mGestureDetector == null) {
      mGestureDetector = new GestureDetector(this, mGestureListener);
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  // Events

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (mClient != null && mClient.isConnected()) {
      mClient.sendTouchEvent(e);
      mGestureDetector.onTouchEvent(e);
    } else {
      initConnection();
    }

    return super.onTouchEvent(e);
  }

  private void initConnection() {
    SharedPreferences settings = getPreferences(MODE_PRIVATE);
    mServerIp = settings.getString(SERVER_IP_KEY, "");

    if (mServerIp.isEmpty() && isDialogDisplaying) {
      return;
    }

    if (mServerIp.isEmpty()) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle("AirServer");

      // Set up the input
      final EditText input = new EditText(this);

      input.setInputType(InputType.TYPE_CLASS_TEXT);
      input.setHint("inair.local or 127.0.0.1");


      builder.setView(input);

      // Set up the buttons
      builder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          mServerIp = input.getText().toString();
          isDialogDisplaying = false;
          if (!mServerIp.isEmpty()) {
            new ConnectTask().execute();
          } else {
            initConnection();
          }
        }
      });

      builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          dialog.cancel();
          isDialogDisplaying = false;
        }
      });

      builder.show();
      isDialogDisplaying = true;
    } else {
      new ConnectTask().execute();
    }
  }

  // Connectivity

  private class ConnectTask extends AsyncTask<Void, String, Void> implements DialogInterface.OnCancelListener {
    ProgressHUD mProgressHUD;

    @Override
    protected void onPreExecute() {
      mProgressHUD = ProgressHUD.show(MainActivity.this, "Connecting", false, true, this);
      super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... params) {
      try {
        InetAddress serverAddr = InetAddress.getByName(mServerIp);

        mSocket = new Socket(serverAddr, SERVER_PORT);
        mSocket.setTcpNoDelay(true);

        mClient = new Client(MainActivity.this, mSocket);
        mClient.registerDevice();
      } catch (UnknownHostException e1) {
        e1.printStackTrace();

        publishProgress(e1.getLocalizedMessage());
      } catch (IOException e1) {
        e1.printStackTrace();

        publishProgress(e1.getLocalizedMessage());
      }
      return null;
    }

    @Override
    protected void onProgressUpdate(String... values) {
      mProgressHUD.setMessage(values[0]);
      Toast.makeText(MainActivity.this, values[0], Toast.LENGTH_LONG).show();
      super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Void result) {
      mProgressHUD.dismiss();

      if (mClient != null && mClient.isConnected()) {
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SERVER_IP_KEY, mServerIp);
        editor.commit();

        Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
      }

      super.onPostExecute(result);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
      this.cancel(true);
      mProgressHUD.dismiss();
    }
  }

  // Gesture Detector

  private GestureDetector mGestureDetector;
  private GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {

    @Override
    public void onLongPress(MotionEvent e) {
      if (e == null) {
        return;
      }
      if (mClient != null && mClient.isConnected()) {
        mClient.sendLongPressEvent(e);
      } else {
        initConnection();
      }
      super.onLongPress(e);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
      if (e1 == null || e2 == null) {
        return false;
      }
      if (mClient != null && mClient.isConnected()) {
        mClient.sendSwipeEvent(e1, e2, velocityX, velocityY);
      } else {
        initConnection();
      }
      return super.onFling(e1, e2, velocityX, velocityY);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
      if (e == null) {
        return false;
      }
      if (mClient != null && mClient.isConnected()) {
        mClient.sendDoubleTapEvent(e);
      } else {
        initConnection();
      }
      return super.onDoubleTap(e);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
      if (e == null) {
        return false;
      }
      if (mClient != null && mClient.isConnected()) {
        mClient.sendSingleTapEvent(e);
      } else {
        initConnection();
      }
      return super.onSingleTapConfirmed(e);
    }
  };
}

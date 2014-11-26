/**
 *
 * The MIT License
 *
 * Copyright (c) 2014, SeeSpace. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package tv.inair.airmote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import inair.eventcenter.proto.Proto;

public class MainActivity extends Activity implements OnEventReceived, OnSocketStateChanged {

//  private SocketClient mSocketClient;
//  private Socket mSocket;
//  private String mServerIp;
//  private Handler mHandler;
//
//  private boolean isDialogDisplaying = false;
//  public static final int MAX_RECONNECT_ATTEMPTS = 3;

  private GestureControl mGestureControl;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mGestureControl = new GestureControl(this, findViewById(R.id.rootView));

    AiRmote.getSocketClient().setOnEventReceived(this);
    AiRmote.getSocketClient().setOnSocketStateChanged(this);
  }

  @Override
  public void onEventReceived(Proto.Event event) {
  }

  Dialog mDialog;
  @Override
  public void onStateChanged(boolean connect, String message) {
    if (!connect) {
      if (mDialog == null) {
        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("inair.local or 127.0.0.1");

        mDialog = new AlertDialog.Builder(this)
            .setTitle("Connect to inAiR")
            .setView(input)
            .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                CharSequence hostname = input.getText();
                if (hostname.length() > 0) {
                  AiRmote.getSocketClient().connectTo(hostname.toString());
                }
              }
            })
            .create();

        mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            mDialog = null;
          }
        });

        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
          @Override
          public void onDismiss(DialogInterface dialog) {
            mDialog = null;
          }
        });

        mDialog.show();

      }
    } else {
      Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    AiRmote.getSocketClient().disconnect();
  }
}

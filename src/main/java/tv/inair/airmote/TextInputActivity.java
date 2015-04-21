package tv.inair.airmote;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import inair.eventcenter.proto.Helper;
import inair.eventcenter.proto.Proto;
import tv.inair.airmote.connection.SocketClient;

public class TextInputActivity extends Activity implements TextWatcher {

  public static final String EXTRA_REPLY_TO = "extra_reply_to";
  public static final String EXTRA_MAX_LENGTH = "extra_max_length";

  private String replyTo;
  private SocketClient mClient;
  private EditText editText;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent i = getIntent();
    replyTo = i.getStringExtra(EXTRA_REPLY_TO);

    int maxLength = i.getIntExtra(EXTRA_MAX_LENGTH, Integer.MAX_VALUE);

    InputFilter[] filterArray = new InputFilter[1];
    filterArray[0] = new InputFilter.LengthFilter(maxLength);

    mClient = Application.getSocketClient();

    setContentView(R.layout.activity_text_input);

    setResult(Activity.RESULT_CANCELED);

    editText = ((EditText) findViewById(R.id.inputText));
    editText.addTextChangedListener(this);
    editText.setFilters(filterArray);
  }

  @Override
  protected void onResume() {
    super.onResume();

    View decorView = getWindow().getDecorView();
    // Hide the status bar.
    int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
    decorView.setSystemUiVisibility(uiOptions);
    // Remember that you should never show the action bar if the
    // status bar is hidden, so hide that too if necessary.
    ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.hide();
    }
  }

  private int mResponse = Proto.TextInputResponseEvent.CANCELLED;

  public void onOkButtonClicked(View view) {
    mResponse = Proto.TextInputResponseEvent.ENDED;
    finish();
  }

  public void onCancelButtonClicked(View view) {
    finish();
  }

  @Override
  protected void onDestroy() {
    Proto.Event event = Helper.newTextInputResponseEvent(editText.getText().toString(), mResponse);
    mClient.sendEvent(event);
  }

  //region Implement TextWatcher
  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {
  }

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    Proto.Event event = Helper.newTextInputResponseEvent(editText.getText().toString(), Proto.TextInputResponseEvent.CHANGED);
    mClient.sendEvent(event);
  }

  @Override
  public void afterTextChanged(Editable s) {
  }
  //endregion
}

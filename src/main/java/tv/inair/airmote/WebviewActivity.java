package tv.inair.airmote;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Set;

import inair.eventcenter.proto.Helper;
import inair.eventcenter.proto.Proto;
import tv.inair.airmote.connection.OnEventReceived;
import tv.inair.airmote.connection.SocketClient;

public class WebViewActivity extends Activity implements OnEventReceived {

  public static final String EXTRA_URL = "extra_url";
  public static final String EXTRA_REPLY_TO = "extra_reply_to";
  public static final String EXTRA_IS_WEBVIEW = "extra_is_webview";

  private static final String OAUTH_SERVER = "oauth.inair.tv";
  private static final String INAIR_SCHEMA = "inair";
  private static final String INAIR_ACTION = "close";

  private WebView webView;

  private String url;
  private String replyTo;
  private Boolean isWebView;
  private SocketClient mClient;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getWindow().requestFeature(Window.FEATURE_PROGRESS);

    mClient = Application.getSocketClient();

    ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    Intent i = getIntent();
    url = i.getStringExtra(EXTRA_URL);
    replyTo = i.getStringExtra(EXTRA_REPLY_TO);
    isWebView = i.getBooleanExtra(EXTRA_IS_WEBVIEW, false);

    webView = new WebView(this);
    setContentView(webView);

    WebSettings st = webView.getSettings();
    st.setSaveFormData(false);
    st.setJavaScriptEnabled(true);

    webView.setWebChromeClient(new WebChromeClient() {
      public void onProgressChanged(WebView view, int progress) {
        // Activities and WebViews measure progress with different scales.
        // The progress meter will automatically disappear when we reach 100%
        WebViewActivity.this.setProgress(progress * 100);
      }
    });

    webView.setWebViewClient(new Client());
    webView.loadUrl(url);
  }

  private void dismissActivity() {
    finish(); // finish activity
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if ((keyCode == KeyEvent.KEYCODE_BACK) ) {
      dismissActivity();
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public void onEventReceived(Proto.Event event) {
    if (isFinishing()) {
      return;
    }

    if (event != null && event.type != null) {
      switch (event.type) {
        case Proto.Event.WEBVIEW_REQUEST:
          break;

        case Proto.Event.WEBVIEW_RESPONSE:
          Proto.WebViewResponseEvent e = event.getExtension(Proto.WebViewResponseEvent.event);
          String javascript="javascript: InAir.emit('newMessage', '" + e.data + "');";
          webView.loadUrl(javascript);
          break;
      }
    }
  }

  private class Client extends WebViewClient {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      Uri uri = Uri.parse(url);
      System.out.println("AAA " + uri);
      if (OAUTH_SERVER.equalsIgnoreCase(uri.getHost())) {
        String code = null;
        Set<String> queryParamNames = uri.getQueryParameterNames();
        if (queryParamNames.contains("code")) {
          code = uri.getQueryParameter("code");
        } else if (queryParamNames.contains("oauth_verifier")) {
          code = uri.getQueryParameter("oauth_verifier");
        }
        if (code != null) {
          System.out.println("Has code: " + code);
          mClient.sendEvent(Helper.newOAuthResponseEvent(code, replyTo));
        }
        dismissActivity();
      } else {
        // ignore legit webview requests so they load normally
        if (INAIR_SCHEMA.equalsIgnoreCase(uri.getScheme())) {
          String action = uri.getHost();
          String jsonDictString = uri.getFragment();

          System.out.println("Client.shouldOverrideUrlLoading " + action + " " + jsonDictString);

          if (!jsonDictString.isEmpty()) {
            Proto.Event response = Helper.newWebViewResponseEvent(jsonDictString, replyTo);
            mClient.sendEvent(response);
          }

          if (INAIR_ACTION.equalsIgnoreCase(action)) {
            dismissActivity();
          }

          // make sure to return true so that your webview doesn't try to load your made-up URL
          return true;
        }
      }

      return super.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
      System.out.println("Client.onReceivedError " + description + " " + failingUrl);
      dismissActivity();
    }
  }
}

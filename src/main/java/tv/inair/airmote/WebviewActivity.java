package tv.inair.airmote;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Set;

import inair.eventcenter.proto.Helper;

public class WebviewActivity extends Activity {

  public static final String EXTRA_URL = "extra_url";
  public static final String EXTRA_REPLY_TO = "extra_reply_to";

  private static final String OAUTH_SERVER = "oauth.inair.tv";

  private WebView webView;

  private String url;
  private String replyTo;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    Intent i = getIntent();
    url = i.getStringExtra(EXTRA_URL);
    replyTo = i.getStringExtra(EXTRA_REPLY_TO);

    getWindow().requestFeature(Window.FEATURE_PROGRESS);

    webView = new WebView(this);
    setContentView(webView);

    webView.getSettings().setJavaScriptEnabled(true);

    webView.setWebChromeClient(new WebChromeClient() {
      public void onProgressChanged(WebView view, int progress) {
        // Activities and WebViews measure progress with different scales.
        // The progress meter will automatically disappear when we reach 100%
        WebviewActivity.this.setProgress(progress * 1000);
      }
    });

    webView.setWebViewClient(new Client());
    webView.loadUrl(url);
  }

  private void dismissActivity() {
    webView.destroy();
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

  private class Client extends WebViewClient {

    @Override
    public void onPageFinished(WebView view, String url) {
      Uri uri = Uri.parse(url);
      if (OAUTH_SERVER.equalsIgnoreCase(uri.getHost())) {
        String code = null;
        Set<String> queryParamNames = uri.getQueryParameterNames();
        if (queryParamNames.contains("code")) {
          code = uri.getQueryParameter("code");
        } else if (queryParamNames.contains("oauth_verifier")) {
          code = uri.getQueryParameter("oauth_verifier");
        }
        if (code != null) {
          System.out.println("Client.onPageFinished " + code);
          Application.getSocketClient().sendEvent(Helper.newOAuthResponseEvent(code, replyTo));
        }
        dismissActivity();
      }
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
      System.out.println("Client.onReceivedError " + description + " " + failingUrl);
      dismissActivity();
    }
  }
}

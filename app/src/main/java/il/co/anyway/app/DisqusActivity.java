package il.co.anyway.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.maps.model.LatLng;


public class DisqusActivity extends AppCompatActivity {

    public static final String DISQUS_TALK_IDENTIFIER = "il.co.anyway.app.DISQUS_TALK_IDENTIFIER";
    public static final String DISQUS_LOCATION = "il.co.anyway.app.DISQUS_TALK_LOCATION";
    public static final String DISQUS_NEW = "il.co.anyway.app.DISQUS_TALK_NEW";

    private static final String DISCUSSION_URL = "https://www.anyway.co.il/discussion";
    private final String LOG_TAG = DisqusActivity.class.getSimpleName();

    private WebView mWebView;
    private String mIdentifier, mUrl;
    private LatLng mLocation;
    private boolean mNewDiscussion;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disqus);

        // get the location of the discussion
        Intent intent = getIntent();

        // check if activity accessed from anyway://disqus?identifier=x
        Uri data = intent.getData();
        if (data == null) {
            // activity accessed from MainActivity, get disqus ID from extra
            mIdentifier = intent.getExtras().getString(DISQUS_TALK_IDENTIFIER);
            mLocation = (LatLng) intent.getExtras().get(DISQUS_LOCATION);
            mNewDiscussion = intent.getExtras().getBoolean(DISQUS_NEW);

        } else {
            mIdentifier = data.getQueryParameter("identifier");
            mLocation = null;
            mNewDiscussion = false;
        }

        // get the web view
        mWebView = (WebView) findViewById(R.id.disqus);
        if (mWebView != null) {
            showDisqus();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_disqus, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh_comments) {
            mWebView.loadUrl(mUrl);
            mWebView.clearHistory();
            return true;
        }
        else if (id == R.id.action_share) {

            String currentStringUri = "http://www.anyway.co.il/discussion?identifier=" + mIdentifier;
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_disqus_title));
            i.putExtra(Intent.EXTRA_TEXT, currentStringUri);
            startActivity(Intent.createChooser(i, getString(R.string.share_disqus_title)));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Build and set disqus url and parameters
     */
    private void buildDisqusUrl() {
        // build url of Disqus
        Uri.Builder uriBuilder = Uri.parse(DISCUSSION_URL).buildUpon()
                .appendQueryParameter("identifier", mIdentifier);

        if (mNewDiscussion) {
            uriBuilder.appendQueryParameter("lat", Double.toString(mLocation.latitude))
                    .appendQueryParameter("lon", Double.toString(mLocation.longitude));
        }

        mUrl = uriBuilder.build().toString();
        Log.i(LOG_TAG, mUrl);
    }

    /**
     * Set WebView parameters and load discussion
     */
    private void showDisqus() {

        // set the discussion url
        buildDisqusUrl();

        WebSettings webSettings = mWebView.getSettings();

        // enable javascript
        webSettings.setJavaScriptEnabled(true);

        // in lollipop, the WebView by default do not allow third party cookies (like google or disqus)
        // if not enabled - login with SSO in not available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView, true);

        mWebView.requestFocusFromTouch();

        // the WebChromeClient is for links to be open inside the app and not in another browser
        mWebView.setWebChromeClient(new WebChromeClient() {

        });

        // the WebViewClient is here to solve a bug in the login procedure of Disqus
        // when login the page stays show "busy" icon and do nothing.
        // here we catch that situation and handle it
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (url.contains("logout") || url.contains("disqus.com/next/login-success")) {
                    view.loadUrl(mUrl);

                }
                if (url.contains("disqus.com/_ax/twitter/complete") ||
                        url.contains("disqus.com/_ax/facebook/complete") ||
                        url.startsWith("https://disqus.com/_ax/google/complete") ||
                        url.startsWith("http://disqus.com/_ax/google/complete")) {

                    view.loadUrl(DISCUSSION_URL + "/login");

                }
                if (url.contains(DISCUSSION_URL + "/login")) {
                    view.loadUrl(mUrl);
                    view.clearHistory();
                }
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.i("disqus error", "failed: " + failingUrl + ", error code: " + errorCode + " [" + description + "]");
            }
        });

        mWebView.loadUrl(mUrl);
    }

    @Override
    public void onBackPressed() {

        // go back inside WebView, only if WebView can't go back let
        // the system handle back action (go back to map)
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

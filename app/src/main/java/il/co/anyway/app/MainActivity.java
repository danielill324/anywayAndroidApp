package il.co.anyway.app;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.androidmapsextensions.ClusterGroup;
import com.androidmapsextensions.ClusteringSettings;
import com.androidmapsextensions.GoogleMap;
import com.androidmapsextensions.GoogleMap.OnCameraChangeListener;
import com.androidmapsextensions.GoogleMap.OnInfoWindowClickListener;
import com.androidmapsextensions.GoogleMap.OnMapLoadedCallback;
import com.androidmapsextensions.GoogleMap.OnMapLongClickListener;
import com.androidmapsextensions.GoogleMap.OnMarkerClickListener;
import com.androidmapsextensions.Marker;
import com.androidmapsextensions.MarkerOptions;
import com.androidmapsextensions.OnMapReadyCallback;
import com.androidmapsextensions.PolygonOptions;
import com.androidmapsextensions.SupportMapFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import il.co.anyway.app.dialogs.AccidentsDialogs;
import il.co.anyway.app.dialogs.ConfirmDiscussionCreateDialogFragment;
import il.co.anyway.app.dialogs.InternetRequiredDialogFragment;
import il.co.anyway.app.dialogs.SearchAddress;
import il.co.anyway.app.models.Accident;
import il.co.anyway.app.models.AccidentCluster;
import il.co.anyway.app.models.Discussion;
import il.co.anyway.app.singletons.AnywayRequestQueue;
import il.co.anyway.app.singletons.MarkersManager;
public class MainActivity extends AppCompatActivity
        implements
        OnInfoWindowClickListener,
        OnMapLongClickListener,
        OnCameraChangeListener,
        OnMapReadyCallback,
        OnMapLoadedCallback,
        OnMarkerClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
        {

    private static final LatLng START_LOCATION = new LatLng(31.774511, 35.011642);
    private static final int START_ZOOM_LEVEL = 14;
    private static final int MINIMUM_ZOOM_LEVEL_TO_SHOW_ACCIDENTS = 16;
    private static final String TUTORIAL_SHOWED_KEY = "il.co.anyway.app.TUTORIAL_SHOWED";

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private GoogleMap mMap;
    private FragmentManager mFragmentManager;
    private SupportMapFragment mMapFragment;
    private boolean mNewInstance,
            mMapIsInClusterMode,
            mDoubleBackToExitPressedOnce,
            mSentUserLocation,
            mShowedDialogAboutInternetConnection,
            mShowedTutorialBefore;

    private boolean mInDanger = false;
    private List<AccidentCluster> mLastAccidentsClusters = null;

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPrefListener;

    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private Marker mPositionMarker, mSearchResultMarker;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private boolean mGpsDialogNeverShowed;
    private final String WS_URI="ws://anyway-server-danielill324.c9users.io:8081";
    public WebSocketClient mWebSocketClient;
    public  com.androidmapsextensions.Polygon poly;
    public  final double alpha = 0.0013;
   public final double beta= 0.0018;
    public int i=0;
    private TextToSpeech tts;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // flag to send user location to anyway when opening app
        // location update taking place in onLocationChange
        mSentUserLocation = false;

        // force double pressing on back key to leave discussion
        mDoubleBackToExitPressedOnce = false;

        // map start in START_ZOOM_LEVEL
        mMapIsInClusterMode = true;

        // Dialog informing the user this app need internet connection never shown before
        mShowedDialogAboutInternetConnection = false;

        // find out if this is the first instance of the activity
        mNewInstance = (savedInstanceState == null);

        // Kick off the process of building the GoogleApiClient, LocationRequest, and
        // LocationSettingsRequest objects.
        buildGoogleApiClient();
        createLocationRequest();
        buildLocationSettingsRequest();
        mGpsDialogNeverShowed = true;

        // add Preference changed listener int order to update map data after preference changed
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedPrefListener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

                        // reset accident manager
                        MarkersManager.getInstance().addAllAccidents(null, MarkersManager.DO_RESET);
                        clearMap();

                        // map is being cleared, so we need to re add all discussion markers
                        MarkersManager.getInstance().setAllMarkersAsNotShownOnTheMap(); // for discussions
                        addMarkersToMap();

                        // fetch markers by new preferences
                        getMarkersFromServer();

                    }
                };
        prefs.registerOnSharedPreferenceChangeListener(mSharedPrefListener);

        // get tutorial show status from shared preferences to make sure tutorial will only open once
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mShowedTutorialBefore = sp.getBoolean(TUTORIAL_SHOWED_KEY, false);
        //set tts Listner and init function
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "This Language is not supported");
                    }
                    speak("Hello");

                } else {
                    Log.e("TTS", "Initilization Failed!");
                }
            }
        });
        /** try to do connect of websoket**/

        try {
            connectWebSocket();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

            /**
             *
             * @param text, convert the text to audio
             */
            private void speak(String text){
                Log.i("speak", "entered to speak");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                }else{
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                }
            }

            /**
             *
             */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // remove shared preferences listener
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(mSharedPrefListener);
        //close the tts
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
       if(mWebSocketClient.getConnection().isOpen())
        {
            //mWebSocketClient.close();
            mWebSocketClient.onClose(1000,"onDestroy", true);

        }


    }

    @Override
    protected void onStart() {
        super.onStart();

        // set the map fragment and call the map settings
        setUpMapIfNeeded();

        // register activity updates from MarkersManager
        MarkersManager.getInstance().registerListenerActivity(this);

        // re-call fetching markers from server, needed when coming
        // back from from discussion to fetch new discussion marker
        if (mMap != null && !mMapIsInClusterMode)
            getMarkersFromServer();

        // check for available network connection
        if (!Utility.isNetworkConnectionAvailable(this) && !mShowedDialogAboutInternetConnection) {

            // show dialog
            new InternetRequiredDialogFragment().show(getSupportFragmentManager(), "InternetDialog");

            // don't show again
            mShowedDialogAboutInternetConnection = true;
        }

        mGoogleApiClient.connect();
        checkLocationSettings();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        // unregister activity updates from MarkersManager
        MarkersManager.getInstance().unregisterListenerActivity();
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

            /**
             *
             */
    private void setUpMapIfNeeded() {
        mFragmentManager = getSupportFragmentManager();
        mMapFragment = (SupportMapFragment) mFragmentManager.findFragmentById(R.id.map_container);
        if (mMapFragment == null) {
            mMapFragment = SupportMapFragment.newInstance();

            FragmentTransaction tx = mFragmentManager.beginTransaction();
            tx.add(R.id.map_container, mMapFragment);
            tx.commit();
        }
        mMapFragment.getExtendedMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // set map to start location if previous instance exist
        if (mNewInstance)
            setMapToLocation(START_LOCATION, START_ZOOM_LEVEL, false);

        // Set Clustering
        ClusteringSettings settings = new ClusteringSettings();
        settings.clusterOptionsProvider(new AnywayClusterOptionsProvider(getResources())).addMarkersDynamically(true);
        mMap.setClustering(settings);

        // Disable location button and blue dot
        mMap.setMyLocationEnabled(false);

        // Disable toolbar on the right bottom corner(taking user to google maps app)
        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Enable zoom +/- controls and compass
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // set listener and layout to markers
        mMap.setInfoWindowAdapter(new MarkerInfoWindowAdapter(getLayoutInflater()));
        mMap.setOnInfoWindowClickListener(this);

        // set listener to open Disqus on long click
        mMap.setOnMapLongClickListener(this);

        // set listener to fetch accidents from server when map change
        mMap.setOnCameraChangeListener(this);

        // when map finish rendering - set camera to user location
        mMap.setOnMapLoadedCallback(this);

        // set listener for marker click, enable to zoom in automatically when cluster marker is clicked
        mMap.setOnMarkerClickListener(this);

        // accident manager is static, so me need to make sure the markers of accident re-added to the map
        MarkersManager.getInstance().setAllMarkersAsNotShownOnTheMap();
        clearMap();

        // call both markers and clusters, only one of them will run, the other return
        addMarkersToMap();
        addClustersToMap(mLastAccidentsClusters);

        // check if app opened by a link to specific location, if so -
        // show confirm dialog to the user
        checkIfAppOpenedFromLink();

        // show My location button
        ImageButton imageButton = (ImageButton)findViewById(R.id.myLocationImageButton);
        if (imageButton != null)
            imageButton.setVisibility(View.VISIBLE);

        // show app tutorial (if it wasn't showed before)
        showTutorialIfNeeded();

    }

    @Override
    public void onMapLoaded() {

        // if the user didn't opened the map before try to set the map to user location
        if (mNewInstance) {
            setMapToLocation(mLocation, MINIMUM_ZOOM_LEVEL_TO_SHOW_ACCIDENTS, true);
            mNewInstance = false;
        }

    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Sets up the location request.
     */
    protected void createLocationRequest() {

        final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
        final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

        mLocationRequest = new LocationRequest();
        // Sets the desired interval for active location updates. This interval is inexact.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and the
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                mLocationRequest,
                this
        );

    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient,
                this
        );
    }

    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    protected void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    /**
     * Check if the device's location settings are adequate for the app's needs using the
     * {@link com.google.android.gms.location.SettingsApi#checkLocationSettings(GoogleApiClient,
     * LocationSettingsRequest)} method, with the results provided through a {@code PendingResult}.
     */
    protected void checkLocationSettings() {
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        mGoogleApiClient,
                        mLocationSettingsRequest
                );
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        //Log.i(LOG_TAG, "All location settings are satisfied.");
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        //Location settings are not satisfied. Show the user a dialog to upgrade location settings

                        // show the use a dialog to enable GPS in location settings and show it only once
                        if (mGpsDialogNeverShowed) {
                            mGpsDialogNeverShowed = false;
                            try {
                                // Show the dialog by calling startResolutionForResult(), and check the result
                                // in onActivityResult().

                                status.startResolutionForResult(MainActivity.this, 0x1);
                            } catch (IntentSender.SendIntentException e) {
                                Log.i(LOG_TAG, "PendingIntent unable to execute request.");
                            }
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        //Log.i(LOG_TAG, "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }

            /**
             * update the map when the user connect
             * @param bundle
             */
    @Override
    public void onConnected(Bundle bundle) {
        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it.
        if (mLocation == null)
            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

            /**
             *
             *
             * @param location get tcurrent ocation
             *                 send to server the new location
             */
    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        if (location == null)
            return;
        try {
            if (mWebSocketClient.getConnection().isOpen()) {
                Log.d("location", String.valueOf(mWebSocketClient.getConnection().isOpen()));
                mWebSocketClient.send("location "+location.getLatitude() + "," + location.getLongitude());
            }
        }
        catch (Exception e)
        {
            Log.i("location", "catch");
            connectWebSocket();
            e.printStackTrace();
        }

        if (!mSentUserLocation) {
            AnywayRequestQueue.getInstance(this)
                    .sendUserAndSearchedLocation(location.getLatitude(), location.getLongitude(),
                            AnywayRequestQueue.HIGHLIGHT_TYPE_USER_GPS
                    );

            mSentUserLocation = true;

        }

        if (mPositionMarker == null) {

            mPositionMarker = mMap.addMarker(new MarkerOptions()
                    .flat(true)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.position_indicator))
                    .title(MarkerInfoWindowAdapter.FORCE_SIMPLE_SNIPPET_SHOW)
                    .snippet(getString(R.string.my_location))
                    .position(
                            new LatLng(location.getLatitude(), location
                                    .getLongitude())));
            mPositionMarker.setClusterGroup(ClusterGroup.NOT_CLUSTERED);

        } else {

            mPositionMarker.setPosition(new LatLng(location.getLatitude(), location
                    .getLongitude()));
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

            /**
             *
             * @param item set the appliction by the user settungs
             * @return
             */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_search) {
            new SearchAddress(this);
            return true;
        }
        if (id == R.id.action_share) {
            String currentStringUri = Utility.getCurrentPositionStringURI(mMap.getCameraPosition().target,
                    (int) mMap.getCameraPosition().zoom, this);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
            i.putExtra(Intent.EXTRA_TEXT, currentStringUri);
            startActivity(Intent.createChooser(i, getString(R.string.share_title)));
            return true;
        }
        if (id == R.id.action_report_bug) {

            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", "samuel.regev@gmail.com", null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "problem in anyway appliction");
            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(emailIntent, getString(R.string.action_report_bug)));

        }
        if (id == R.id.action_about) {
            showAboutInfoDialog();
        }


        return super.onOptionsItemSelected(item);
    }

            /**
             *
             * @param marker zoom on and show detaild on the accident
             * @return
             */
    @Override
    public boolean onMarkerClick(Marker marker) {

        int currentZoomLevel = (int) mMap.getCameraPosition().zoom;

        // open discussion if marker represent discussion
        if (marker.getData() instanceof Discussion) {
            Intent disqusIntent = new Intent(this, DisqusActivity.class);
            disqusIntent.putExtra(DisqusActivity.DISQUS_TALK_IDENTIFIER, ((Discussion) marker.getData()).getIdentifier());
            disqusIntent.putExtra(DisqusActivity.DISQUS_LOCATION, ((Discussion) marker.getData()).getLocation());
            disqusIntent.putExtra(DisqusActivity.DISQUS_NEW, false);

            startActivity(disqusIntent);
            return true;
        }

        // zoom in when clicking on AccidentCluster marker
        if (marker.getData() instanceof AccidentCluster) {

            // zoom in in jumps of two levels each time
            int requiredZoomLevel = currentZoomLevel + 2;

            // but not more then MINIMUM_ZOOM_LEVEL_TO_SHOW_ACCIDENTS
            requiredZoomLevel = requiredZoomLevel > MINIMUM_ZOOM_LEVEL_TO_SHOW_ACCIDENTS ? MINIMUM_ZOOM_LEVEL_TO_SHOW_ACCIDENTS : requiredZoomLevel;

            // if cluster contain only one accident, zoom in all the way
            AccidentCluster accidentCluster = marker.getData();
            if (accidentCluster.getCount() == 1)
                requiredZoomLevel = MINIMUM_ZOOM_LEVEL_TO_SHOW_ACCIDENTS + 1;

            setMapToLocation(marker.getPosition(), requiredZoomLevel, true);
            return true;
        }

        return false;
    }

            /**
             *
             * @param marker click on info botton marker show details on the accident
             */
    @Override
    public void onInfoWindowClick(Marker marker) {

        if (marker.getData() instanceof Accident)
            AccidentsDialogs.showAccidentDetailsInDialog((Accident) marker.getData(), this);

        else if (marker.isCluster())
            AccidentsDialogs.showAccidentsClusterAsListDialog(marker, this);


    }

            /**
             *
             * @param latLng - the positopn of the click in, is the user press long click on the map he can add new discussion
             *
             */
    @Override
    public void onMapLongClick(LatLng latLng) {

        // get location and show dialog confirming create discussion
        Bundle args = new Bundle();
        args.putParcelable("location", latLng);
        ConfirmDiscussionCreateDialogFragment dialog = new ConfirmDiscussionCreateDialogFragment();
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "");

    }

            /**
             *
             * @param cameraPosition the position to move to
             *                       the position is LatLng of ne and se
             *                       the function send to the server this new posion
             */
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        int zoomLevel = (int) mMap.getCameraPosition().zoom;

        if (zoomLevel < MINIMUM_ZOOM_LEVEL_TO_SHOW_ACCIDENTS && !mMapIsInClusterMode) {

            mMapIsInClusterMode = true;
            clearMap();
            MarkersManager.getInstance().setAllMarkersAsNotShownOnTheMap();

        } else if (zoomLevel >= MINIMUM_ZOOM_LEVEL_TO_SHOW_ACCIDENTS && mMapIsInClusterMode) {

            mMapIsInClusterMode = false;
            mLastAccidentsClusters = null;
            clearMap();
            addMarkersToMap();

        }

        getMarkersFromServer();
        /***/

        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        LatLng ne = bounds.northeast;
        LatLng sw = bounds.southwest;


        /****/
        try {
            if (mWebSocketClient.getConnection().isOpen()) {
                Log.i("Websocket", String.valueOf(mWebSocketClient.getConnection().isOpen())+"Camera");
                mWebSocketClient.send(ne+","+sw);
                Log.i("onCamera",ne.toString()+" "+ sw.toString());
            } else {
                mWebSocketClient.connect();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    /**
     * check if user opened the intent from a link shared with him, like: http://www.anyway.co.il/...parameters
     *
     * @return true if user open the app from the link, false otherwise
     */
    private boolean checkIfAppOpenedFromLink() {

        if (getIntent().getData() != null) {

            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)

                    .setMessage(R.string.confirm_move_to_url_data)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            getDataFromSharedURL();

                        }

                    })
                    .setNegativeButton(R.string.no, null)
                    .show();

            return true;

        }

        return false;
    }

    /**
     * get parameters data from URL and set app preferences and map location accordingly
     *
     * @return true if all parameters are correct, false otherwise
     */
    private boolean getDataFromSharedURL() {

        // url parameters: start_date, end_date, show_fatal,
        // show_severe, show_light, show_inaccurate, zoom, lat, lon

        Uri data = getIntent().getData();
        if (data == null)
            return false;
        else {
            // disable the moving to user location (if available) in onMapLoaded
            mNewInstance = false;

            Log.i(LOG_TAG + "_URL", "query: " + data.getQuery());

            String start_date_str = data.getQueryParameter("start_date");
            String end_date_str = data.getQueryParameter("end_date");

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date start_date;
            Date end_date;

            int show_fatal, show_severe, show_light, show_inaccurate, zoom;
            double latitude, longitude;
            try {
                show_fatal = Integer.parseInt(data.getQueryParameter("show_fatal"));
                show_severe = Integer.parseInt(data.getQueryParameter("show_severe"));
                show_light = Integer.parseInt(data.getQueryParameter("show_light"));
                show_inaccurate = Integer.parseInt(data.getQueryParameter("show_inaccurate"));
                zoom = Integer.parseInt(data.getQueryParameter("zoom"));
                latitude = Double.parseDouble(data.getQueryParameter("lat"));
                longitude = Double.parseDouble(data.getQueryParameter("lon"));

                start_date = dateFormat.parse(start_date_str);
                end_date = dateFormat.parse(end_date_str);

                Log.i(LOG_TAG + "Url", "All args from url parsed successfully");

                setSettingsAndMoveToLocation(start_date, end_date, show_fatal, show_severe, show_light, show_inaccurate, zoom, latitude, longitude);

                // clear intent data, so this operation will only occur once
                getIntent().setData(null);
                return true;
            } catch (Exception e) {
                // NumberFormatException || ParseException
                Log.e(LOG_TAG + "_URL", "Error on parsing url", e);
                return false;
            }
        }
    }

    /**
     * Save all parameters to SharedPreferences and move to lat,lng specified
     *
     * @param start_date      start date
     * @param end_date        end date
     * @param show_fatal      show fatal accidents
     * @param show_severe     show severe accidents
     * @param show_light      show light accidents
     * @param show_inaccurate show inaccurate location accidents
     * @param zoom            zoom level
     * @param latitude        latitude to move to
     * @param longitude       longitude to move to
     */
    private void setSettingsAndMoveToLocation(Date start_date, Date end_date,
                                              int show_fatal, int show_severe, int show_light, int show_inaccurate,
                                              int zoom, double latitude, double longitude) {

        // Get preferences form SharedPreferences
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");

        sharedPrefs.edit().putBoolean(getString(R.string.pref_accidents_fatal_key), show_fatal == 1)
                .putBoolean(getString(R.string.pref_accidents_severe_key), show_severe == 1)
                .putBoolean(getString(R.string.pref_accidents_light_key), show_light == 1)
                .putBoolean(getString(R.string.pref_accidents_inaccurate_key), show_inaccurate == 1)
                .putString(getString(R.string.pref_from_date_key), df.format(start_date))
                .putString(getString(R.string.pref_to_date_key), df.format(end_date))
                .apply();

        setMapToLocation(new LatLng(latitude, longitude), zoom, false);

    }

    /**
     * move the camera to specific location, if the map is ready
     *
     * @param location  location to move to
     * @param zoomLevel zoom level of the map after new location set
     * @param animate   animate the map movement if true
     */
    private boolean setMapToLocation(Location location, int zoomLevel, boolean animate) {
        if (location == null || mMap == null)
            return false;

        if (animate)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude()), zoomLevel), null);
        else
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude()), zoomLevel));

        return true;
    }

    /**
     * move the camera to specific location, if the map is ready
     *
     * @param latLng    location to move to
     * @param zoomLevel zoom level of the map after new location set
     * @param animate   animate the map movement if true
     */
    private boolean setMapToLocation(LatLng latLng, int zoomLevel, boolean animate) {
        Location l = new Location("");
        l.setLongitude(latLng.longitude);
        l.setLatitude(latLng.latitude);

        return setMapToLocation(l, zoomLevel, animate);
    }

    /**
     * get parameters and start fetching accidents for current view and settings
     */
    private void getMarkersFromServer() {
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        int zoomLevel = (int) mMap.getCameraPosition().zoom;

        // fetch accidents from anyway or fetch from cluster decided by zoom level
        if (zoomLevel >= MINIMUM_ZOOM_LEVEL_TO_SHOW_ACCIDENTS)
            Utility.getMarkersByParameters(bounds, zoomLevel, this);
        else
            new FetchClusteredAccidents(bounds, zoomLevel, this);
    }

    /**
     * Add all AccidentCluster list as markers to map
     *
     * @param accidentClusterList List to add
     */
    public void addClustersToMap(List<AccidentCluster> accidentClusterList) {

        if (accidentClusterList == null)
            return;

        if (!mMapIsInClusterMode)
            return;

        clearMap();
        for (AccidentCluster ac : accidentClusterList) {

            // add cluster marker to map
            Marker m = mMap.addMarker(new MarkerOptions()
                            .anchor(0.5f, 0.5f)
                            .position(ac.getLocation())
                            .icon(BitmapDescriptorFactory.fromBitmap(
                                            Utility.drawTextToBitmap(this,
                                                    Utility.getClusterImageByCountOfAccidents(ac.getCount()),
                                                    Integer.toString(ac.getCount())
                                            )
                                    )
                            )
            );

            m.setClusterGroup(ClusterGroup.NOT_CLUSTERED);
            m.setData(ac);

        }

        mLastAccidentsClusters = accidentClusterList;
    }

    /**
     * get all accidents and discussions that is not on the map from MarkersManager and add them to map
     */
    public void addMarkersToMap() {

        if (mMapIsInClusterMode)
            return;

        // add new accidents
        List<Accident> newAccidents = MarkersManager.getInstance().getAllNewAccidents();
        for (Accident a : newAccidents)
            addAccidentToMap(a);

        // add new discussions
        List<Discussion> newDiscussions = MarkersManager.getInstance().getAllNewDiscussions();
        for (Discussion d : newDiscussions)
            addDiscussionToMap(d);
    }

    /**
     * Add accident marker to map
     *
     * @param a Accident to add
     */
    public void addAccidentToMap(Accident a) {

        Log.e(LOG_TAG, "adding "+a);
        System.out.println("adding "+a);
        // if map is in cluster mode, do not add accident marker
        if (mMapIsInClusterMode)
            return;

        mMap.addMarker(new MarkerOptions()
                .title(Utility.getAccidentTypeByIndex(a.getSubType(), this))
                .snippet(getString(R.string.marker_default_desc))
                .icon(BitmapDescriptorFactory.fromResource(Utility.getIconForMarker(a.getSeverity(), a.getSubType())))
                .position(a.getLocation()))
                .setData(a);
        System.out.println("added");

        a.setMarkerAddedToMap(true);
    }

    /**
     * Add Discussion marker to map
     *
     * @param d Discussion to add
     */
    public void addDiscussionToMap(Discussion d) {

        // if map is in cluster mode, do not add discussion marker
        if (mMapIsInClusterMode)
            return;

        Marker m = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.discussion))
                .position(d.getLocation()));

        m.setData(d);
        m.setClusterGroup(ClusterGroup.NOT_CLUSTERED);

        d.setMarkerAddedToMap(true);
    }

    /**
     * Show information about Anyway in dialog
     */
    private void showAboutInfoDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setView(getLayoutInflater().inflate(R.layout.info_dialog, null));
        adb.setPositiveButton(getString(R.string.close), null);
        adb.show();
    }

    /**
     * Move map to location and add marker to specify searched location
     * When search result selected in the result dialog(@dialogs.SearchDialogs.searchAddress) it call this method
     *
     * @param searchResultLocation Location of search result
     * @param searchResultAddress  Address of search result
     */
    public void updateMapFromSearchResult(LatLng searchResultLocation, String searchResultAddress) {

        setMapToLocation(searchResultLocation, MINIMUM_ZOOM_LEVEL_TO_SHOW_ACCIDENTS, true);

        if (mSearchResultMarker == null) {
            mSearchResultMarker = mMap.addMarker(new MarkerOptions()
                    .position(searchResultLocation)
                    .title(MarkerInfoWindowAdapter.FORCE_SIMPLE_SNIPPET_SHOW)
                    .snippet(searchResultAddress)
                    .clusterGroup(ClusterGroup.NOT_CLUSTERED));
        } else {
            mSearchResultMarker.setPosition(searchResultLocation);
            mSearchResultMarker.setSnippet(searchResultAddress);
        }

    }

    // force double click to exit app
    @Override
    public void onBackPressed() {

        if (mDoubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        mDoubleBackToExitPressedOnce = true;
        Toast.makeText(this, getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                mDoubleBackToExitPressedOnce = false;
            }
        }, 2000);

    }

    /**
     * Clear the map from markers and re-adding search and location markers
     */
    private void clearMap() {
        mMap.clear();

        // force re-adding my location marker and search result marker
        if (mSearchResultMarker != null)
            mSearchResultMarker = mMap.addMarker(new MarkerOptions()
                    .snippet(mSearchResultMarker.getSnippet())
                    .position(mSearchResultMarker.getPosition())
                    .title(mSearchResultMarker.getTitle())
                    .clusterGroup(ClusterGroup.NOT_CLUSTERED));

        mPositionMarker = null;
        onLocationChanged(mLocation);

    }

    /**
     * Handler for 'MyLocation' button
     * @param view Button clicked
     */
    public void onMyLocationButtonClick(View view) {
        if (mLocation == null) {
            mGpsDialogNeverShowed = true;
            checkLocationSettings();
        }

        setMapToLocation(mLocation, MINIMUM_ZOOM_LEVEL_TO_SHOW_ACCIDENTS, true);
    }

    /**
     * Show tutorial activity if it never shown before
     * Also save the state of activity as 'shown' in Shared Preferneces
     */
    private void showTutorialIfNeeded() {

        if (!mShowedTutorialBefore) {
            // set tutorial as showed
            // using shared preferences
            // to make sure it won't show again
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putBoolean(TUTORIAL_SHOWED_KEY, true)
                    .apply();

            mShowedTutorialBefore = true;

            // start tutorial activity
            startActivity(new Intent(this, TutorialActivity.class));
        }
    }


            /**
             * this function handle  all the issues of the websocket connection
             */
    private void connectWebSocket() {
        URI uri;
        try {
            uri = new URI(WS_URI);
        } catch (URISyntaxException e) {

            Toast.makeText(getApplicationContext(),"uri failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        }
        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            /**
             * create connection with the server
             */
            public void onOpen(ServerHandshake handshakedata) {
                Log.i("Websocket", "Opened"+ handshakedata.getHttpStatusMessage());
                mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);

            }

            @Override
            /**
             * the function received a message from the server, and create a runnable.
             * the function react due to the message context
             */
            public void onMessage(String s) {
                final String message = s;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("Websocket", "got message: " + message);
                        if(message.equals("2") || message.equals("3"))

                        {
                            if( message.equals("2")) {
                                if (!mInDanger) {
                                    addNotification();
                                    mInDanger = true;
                                    try {
                                        speak("be carefull");
                                    } catch (Exception e) {
                                        Log.i("ring", "problem");

                                    }
                                }
                            }
                            else //message of 3 arrived
                            {
                                if (mInDanger) { //was in not dangerous area
                                    mInDanger = false;
                                    try {
                                        speak("out of danger");
                                    } catch (Exception e) {
                                        Log.i("ring", "problem");

                                    }
                                }

                            }
                        }
                        else
                        try {
                            clearMap();
                            JSONArray jsonArr = new JSONArray(message);

                            for (int i = 0; i < jsonArr.length(); i++)
                            {
                                JSONObject jsonObj = jsonArr.getJSONObject(i);

                                Log.i("json", String.valueOf(jsonObj.getDouble("lat")));
                                getColor(jsonObj.getInt("color"), jsonObj.getDouble("lat"), jsonObj.getDouble("lng"));
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }

            @Override
            /**
             * closing the connection with the server, send  the server close message
             * if the reason of calling that function is not from on destroy the function will try to reconnect
             */
            public void onClose(int code, String reason, boolean remote) {
                mWebSocketClient.send("closing" + reason);
                mWebSocketClient.getConnection().close(code, reason);

                Log.i("Websocket", "Closed " + reason);
                clearMap();
                try {
                    if (reason != "onDestroy") {
                        mWebSocketClient.connect();
                    }
                }
                catch (Exception e)
                {
                    Toast.makeText(getApplicationContext(),"connaction failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception ex) {
                Log.i("Websocket", "Error \n" + ex);
            }
        };
        mWebSocketClient.connect();
    }

            /**
             *
             * @param H - hue of the square color
             * @param lat - latitude of the left bottom corner of the square
             * @param lng - longitude  of the right bottom  of the square
             *            the function draw a polygon on the map and color it according to the H parameter
             */
    private void getColor(int H, double lat, double lng)
    {
        float [] HSL={0,0,0};
        HSL[0] = (float) (H);
        HSL[1] = (float) (1);
        HSL[2] = (float) (0.5);
        int rgb = ColorUtils.HSLToColor(HSL);
        int r = Color.red(rgb);
        int g = Color.green(rgb);
        int b = Color.blue(rgb);
        poly = mMap.addPolygon(new PolygonOptions()
                .add(new LatLng(lat, lng), new LatLng(lat, lng+beta), new LatLng(lat+alpha, lng+beta), new LatLng(lat+alpha, lng))
                .strokeWidth(0).
                 fillColor(Color.argb(80, r ,g ,b)));



    }

            /**
             * function that bulids the notifications
             * logo= awLsixty size: 64*64 px
             * intent that connect the notification to main actitivity
             * build the notification by systemService
             */
            private void addNotification() {
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.awlsixty)
                                .setContentTitle("Cearfull")
                                .setContentText("risk area");

                Intent notificationIntent = new Intent(this, MainActivity.class);
                PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(contentIntent);

                // Add anywar notification
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(0, builder.build());
            }

        }

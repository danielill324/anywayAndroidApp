package il.co.anyway.app.singletons;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import il.co.anyway.app.PriorityJsonObjectRequest;
import il.co.anyway.app.Utility;
import il.co.anyway.app.models.Accident;
import il.co.anyway.app.models.Discussion;

// fetching accidents from Anyway servers
public class AnywayRequestQueue {

    public final static String ANYWAY_BASE_URL = "https://www.anyway.co.il/";
    public final static String ANYWAY_MARKERS_BASE_URL = ANYWAY_BASE_URL + "markers?";
    public final static String ANYWAY_DISCUSSION_POST_URL = ANYWAY_BASE_URL + "discussion";
    public final static String ANYWAY_HIGHLIGHT_POINTS = ANYWAY_BASE_URL + "highlightpoints";

    public final static int HIGHLIGHT_TYPE_USER_SEARCH = 1;
    public final static int HIGHLIGHT_TYPE_USER_GPS = 2;

    private final static String LOG_TAG = AnywayRequestQueue.class.getSimpleName();
    private static AnywayRequestQueue instance = null;
    RequestQueue mRequestQueue;

    /**
     * constructor is private to keep singleton behavior
     * allowing only one instance of the class to exist
     *
     * @param context context which request queue will be created
     */
    private AnywayRequestQueue(Context context) {
        // Instantiate the RequestQueue - Volley will choose network and cache automatically.
        // This also start the queue
        mRequestQueue = Volley.newRequestQueue(context);
    }

    /**
     * Create/return the instance of the request queue
     *
     * @param context context which the request queue will crated, can be null if request queue already exist
     * @return AnywayRequestQueue instance, or null if instance and context is null
     */
    public static AnywayRequestQueue getInstance(Context context) {
        if (instance == null) {
            if (context == null)
                return null;
            else
                instance = new AnywayRequestQueue(context);
        }

        return instance;
    }

    /**
     * Add a request to get accidents from Anyway by parameters
     * Build the request url and call @code #addMarkersRequest(url)
     *
     * @param ne_lat north east bound latitude
     * @param ne_lng north east bound longitude
     * @param sw_lat south west bound latitude
     * @param sw_lng south west bound longitude
     * @param zoom zoom level
     * @param start_date start date (timestamp as String)
     * @param end_date end date (timestamp as String)
     * @param show_fatal show fatal accidents
     * @param show_severe show severe accidents
     * @param show_light show light accidents
     * @param show_inaccurate show inaccurate accidents
     */
    public void addMarkersRequest(double ne_lat, double ne_lng, double sw_lat, double sw_lng, int zoom,
                                  String start_date, String end_date,
                                  boolean show_fatal, boolean show_severe, boolean show_light, boolean show_inaccurate) {

        final String DEFAULT_REQUEST_FORMAT = "json";

        // Construct the URL for the Anyway accidents query
        Uri builtUri = Uri.parse(ANYWAY_MARKERS_BASE_URL).buildUpon()
                .appendQueryParameter("ne_lat", Double.toString(ne_lat))
                .appendQueryParameter("ne_lng", Double.toString(ne_lng))
                .appendQueryParameter("sw_lat", Double.toString(sw_lat))
                .appendQueryParameter("sw_lng", Double.toString(sw_lng))
                .appendQueryParameter("zoom", Integer.toString(zoom))
                .appendQueryParameter("thin_markers","false")
                .appendQueryParameter("start_date", start_date)
                .appendQueryParameter("end_date", end_date)
                .appendQueryParameter("show_fatal", show_fatal ? "1" : "0")
                .appendQueryParameter("show_severe", show_severe ? "1" : "0")
                .appendQueryParameter("show_light", show_light ? "1" : "0")
                .appendQueryParameter("show_inaccurate", show_inaccurate ? "1" : "0")
                .appendQueryParameter("format", DEFAULT_REQUEST_FORMAT)

                // TODO add this options in user preferences
                .appendQueryParameter("show_markers","1")
                .appendQueryParameter("show_discussions","1")
                .build();

        try {
            URL url = new URL(builtUri.toString());
            addMarkersRequest(url.toString());
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error building the URL: " + e.getMessage());
        }
    }

    /**
     * Create a request for accidents details by url and add it to request queue
     *
     * @param url URL of request
     */
    private void addMarkersRequest(String url) {

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        List<Accident> fetchedAccidents = new ArrayList<>();
                        List<Discussion> fetchedDiscussion = new ArrayList<>();

                        int fetchStatus = Utility.getMarkersDataFromJson(response, fetchedAccidents, fetchedDiscussion);

                        if (fetchStatus == 0) {
                            MarkersManager.getInstance().addAllAccidents(fetchedAccidents, MarkersManager.DO_NOT_RESET);
                            MarkersManager.getInstance().addAllDiscussions(fetchedDiscussion, MarkersManager.DO_NOT_RESET);
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(LOG_TAG, error.toString());
                    }
                });

        // Add the request to the RequestQueue.
        mRequestQueue.add(jsObjRequest);
    }

    /**
     * send location fore statistics
     *
     * @param lat location latitude
     * @param lng location longitude
     * @param type HIGHLIGHT_TYPE_USER_SEARCH / HIGHLIGHT_TYPE_USER_GPS
     */
    public void sendUserAndSearchedLocation(double lat, double lng, int type) {

        JSONObject jsonObjectData = new JSONObject();
        try {
            jsonObjectData.put("latitude", Double.toString((lat)));
            jsonObjectData.put("longitude", Double.toString(lng));
            jsonObjectData.put("type", Integer.toString(type));
        } catch (JSONException e) {
            //Log.e(LOG_TAG, "Error parsing statistics");
            return;
        }

        PriorityJsonObjectRequest newLocationRequest = new PriorityJsonObjectRequest
                (Request.Method.POST, ANYWAY_HIGHLIGHT_POINTS, jsonObjectData, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        //Log.i(LOG_TAG, "Statistics sent");
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //Log.e(LOG_TAG, "Error sending statistics");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json");
                return params;
            }
        };

        newLocationRequest.setShouldCache(false);
        newLocationRequest.setPriority(Request.Priority.LOW);
        mRequestQueue.add(newLocationRequest);

    }
}
package il.co.anyway.app;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import il.co.anyway.app.models.AccidentCluster;

public class FetchClusteredAccidents {

    private final static String LOG_TAG = FetchClusteredAccidents.class.getSimpleName();
    private final static String ANYWAY_CLUSTER_URL = "http://anywaycluster.azurewebsites.net/api/cluster";

    private static FetchAsync currentRunningTask = null;

    private MainActivity mCallingActivity;
    private LatLngBounds mBounds;
    private int mZoomLevel;

    public FetchClusteredAccidents(LatLngBounds bounds, int zoomLevel, MainActivity activity) {
        mBounds = bounds;
        mZoomLevel = zoomLevel;
        mCallingActivity = activity;

        if (currentRunningTask != null)
            currentRunningTask.cancel(true);

        currentRunningTask = new FetchAsync();
        currentRunningTask.execute();
    }


    private class FetchAsync extends AsyncTask<Void, Void, List<AccidentCluster>> {

        ProgressBar pbLoading = (ProgressBar) mCallingActivity.findViewById(R.id.progressBar);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pbLoading.setVisibility(View.VISIBLE);

        }

        @Override
        protected List<AccidentCluster> doInBackground(Void... params) {

            Log.i(LOG_TAG, "Start fetching cluster");

            List<AccidentCluster> result = new ArrayList<>();

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Get preferences form SharedPreferences
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mCallingActivity);

            Boolean show_fatal = sharedPrefs.getBoolean(mCallingActivity.getString(R.string.pref_accidents_fatal_key), true);
            Boolean show_severe = sharedPrefs.getBoolean(mCallingActivity.getString(R.string.pref_accidents_severe_key), true);
            Boolean show_light = sharedPrefs.getBoolean(mCallingActivity.getString(R.string.pref_accidents_light_key), true);
            Boolean show_inaccurate = sharedPrefs.getBoolean(mCallingActivity.getString(R.string.pref_accidents_inaccurate_key), false);
            String fromDate = sharedPrefs.getString(mCallingActivity.getString(R.string.pref_from_date_key), mCallingActivity.getString(R.string.pref_default_from_date));
            String toDate = sharedPrefs.getString(mCallingActivity.getString(R.string.pref_to_date_key), mCallingActivity.getString(R.string.pref_default_to_date));

            try {
                // Construct the URL for the Anyway cluster query
                Uri builtUri = Uri.parse(ANYWAY_CLUSTER_URL).buildUpon()
                        .appendQueryParameter("ne_lat", Double.toString(mBounds.northeast.latitude))
                        .appendQueryParameter("ne_lng", Double.toString(mBounds.northeast.longitude))
                        .appendQueryParameter("sw_lat", Double.toString(mBounds.southwest.latitude))
                        .appendQueryParameter("sw_lng", Double.toString(mBounds.southwest.longitude))
                        .appendQueryParameter("zoom_level", Integer.toString(mZoomLevel))
                        .appendQueryParameter("start_date", Utility.getTimeStamp(fromDate))
                        .appendQueryParameter("end_date", Utility.getTimeStamp(toDate))
                        .appendQueryParameter("show_fatal", show_fatal ? "1" : "0")
                        .appendQueryParameter("show_severe", show_severe ? "1" : "0")
                        .appendQueryParameter("show_light", show_light ? "1" : "0")
                        .appendQueryParameter("show_inaccurate", show_inaccurate ? "1" : "0")
                        .build();

                URL url = new URL(builtUri.toString());

                // Create the request to Anyway, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null)
                    buffer.append(line);

                String JSONResp = buffer.toString();
                ;

                Log.i(LOG_TAG, "Fetched " + JSONResp.length() + " chars");

                JSONArray arr = new JSONArray(JSONResp);
                for (int i = 0; i < arr.length(); i++) {

                    JSONObject jsonCluster = arr.getJSONObject(i);
                    double lat = jsonCluster.getDouble("lat");
                    double lng = jsonCluster.getDouble("lng");
                    int count = jsonCluster.getInt("count");

                    AccidentCluster ac = new AccidentCluster(count, new LatLng(lat, lng));

                    result.add(ac);
                }

                Log.i(LOG_TAG, "Converted to " + result.size() + " clusters");
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error " + e.getMessage());
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error " + e.getMessage());
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();

                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<AccidentCluster> accidentClusters) {
            super.onPostExecute(accidentClusters);

            pbLoading.setVisibility(View.GONE);
            currentRunningTask = null;

            if (accidentClusters != null && accidentClusters.size() > 0)
                mCallingActivity.addClustersToMap(accidentClusters);
        }
    }
}

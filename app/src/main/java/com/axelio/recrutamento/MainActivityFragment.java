package com.axelio.recrutamento;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.net.Uri;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.net.HttpURLConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment
{

    private TextView textViewHeader;
    private TextView textViewRating;
    private ListView listViewEpisodes;
    private NetworkImageView imageViewSerie;
    private NetworkImageView imageViewThumbnail;
    RequestQueue queue;
    ImageLoader imageLoader;

    private View spacer;

    private ArrayAdapter adapter;

    public MainActivityFragment()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        listViewEpisodes = (ListView) rootView.findViewById(R.id.listview);
        imageViewSerie = (NetworkImageView) rootView.findViewById(R.id.imageview_header);
        imageViewThumbnail = (NetworkImageView) rootView.findViewById(R.id.imageview_thumbnail);
        textViewHeader = (TextView) rootView.findViewById(R.id.textview_header);
        textViewRating = (TextView)rootView.findViewById(R.id.textview_season_rating);

        LayoutInflater inflaterFr = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View listHeader = inflaterFr.inflate(R.layout.list_header_episode, null);
        spacer = listHeader.findViewById(R.id.viewAuxiliar);

        listViewEpisodes.addHeaderView(listHeader);




        listViewEpisodes.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleCount, int totalItemCount) {
                if (listViewEpisodes.getFirstVisiblePosition() == 0) {
                    View firstChild = listViewEpisodes.getChildAt(0);
                    int topY = 0;
                    if (firstChild != null) {
                        topY = firstChild.getTop();
                    }

                    int spacerTopY = spacer.getTop();
                    textViewHeader.setY(Math.max(0, spacerTopY + topY));

                    imageViewSerie.setY(topY * 0.5f);
                }
            }

        });

        List<String> modelList = new ArrayList<>();
        modelList.add("Cargando...");


        adapter = new EpisodeAdapter(getActivity(), R.layout.list_row_episode, modelList);
        listViewEpisodes.setAdapter(adapter);

        queue = Volley.newRequestQueue(getActivity());
        String URL_BASE_EPISODES = "https://api.trakt.tv/shows/game-of-thrones/seasons/6?extended=images";
        String URL_BASE_SEASON = "https://api.trakt.tv/shows/game-of-thrones/seasons?extended=full,images";

        final Map<String, String> headers = new ArrayMap<String, String>();
        headers.put("trakt-api-version", "2");
        headers.put("trakt-api-key", getActivity().getResources().getString(R.string.trakt_client_id));
        headers.put("Content-Type", "application/json");


        StringRequest stringRequestEpisodes = new StringRequest(Request.Method.GET, URL_BASE_SEASON, responseListenerSeason, responseErrorListenerErrorSeason
        ) {
            @Override
            public Map<String, String> getHeaders() {

                return headers;
            }
        };

        StringRequest stringRequestSeason = new StringRequest(Request.Method.GET, URL_BASE_EPISODES, responseListenerEpisode, responseErrorListenerErrorEpisode
        ) {
            @Override
            public Map<String, String> getHeaders() {

                return headers;
            }
        };

        imageLoader = new ImageLoader(queue, new ImageLoader.ImageCache()
        {
            private final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(10);

            public void putBitmap(String url, Bitmap bitmap) {
                mCache.put(url, bitmap);
            }

            public Bitmap getBitmap(String url) {
                return mCache.get(url);
            }
        });

        queue.add(stringRequestSeason);
        queue.add(stringRequestEpisodes);
        return rootView;
    }

    Response.Listener<String> responseListenerEpisode = new Response.Listener<String>()
    {
        @Override
        public void onResponse(String response)
        {
            try
            {
                List<String> titles = getEpisodesTitlesFromJson(response);
                adapter.clear();
                for(String title : titles)
                {
                    adapter.add(title);
                }
                String urlImage = getEpisodeImageURLFromJson(response);
                imageViewSerie.setImageUrl(urlImage, imageLoader);

            }
            catch (JSONException e)
            {
                Log.e("JSON ERROR", "Parseo");
            }
        }
    };

    Response.ErrorListener responseErrorListenerErrorEpisode = new Response.ErrorListener()
    {
        @Override
        public void onErrorResponse(VolleyError error)
        {
            Toast.makeText(getActivity(), "No se logro conectar al servidor", Toast.LENGTH_SHORT).show();
        }
    };

    Response.Listener<String> responseListenerSeason = new Response.Listener<String>()
    {
        @Override
        public void onResponse(String response)
        {
            try
            {
                Double rating = getRatingFromJson(response, 6);
                textViewRating.setText(String.format("%.1f",rating));
                String urlImage = getSeasonImageURLFromJson(response,6);
                imageViewThumbnail.setImageUrl(urlImage, imageLoader);
            }
            catch (JSONException e)
            {
                Log.e("JSON ERROR", "Parseo");
            }

        }
    };

    Response.ErrorListener responseErrorListenerErrorSeason = new Response.ErrorListener()
    {
        @Override
        public void onErrorResponse(VolleyError error)
        {
            Toast.makeText(getActivity(), "Error al cargar rating", Toast.LENGTH_SHORT).show();
        }
    };


    // Utilities
    private List<String> getEpisodesTitlesFromJson(String jsonString) throws JSONException
    {
        final String JSON_KEY_TITLE = "title";

        List<String> results = new ArrayList<String>();

        JSONArray rootJson = new JSONArray(jsonString);
        for(int i = 0; i < rootJson.length(); i++)
        {
            JSONObject episode = rootJson.getJSONObject(i);
            String title = episode.getString(JSON_KEY_TITLE);

            results.add(title);
        }

        return results;
    }

    private Double getRatingFromJson(String jsonString, int season) throws JSONException
    {
        List<String> results = new ArrayList<String>();

        final String JSON_KEY_RATING = "rating";

        JSONArray rootJson = new JSONArray(jsonString);

        JSONObject seasonJson = rootJson.getJSONObject(season);
        Double rating = seasonJson.getDouble(JSON_KEY_RATING);

        return rating;

    }

    private String getSeasonImageURLFromJson(String jsonString, int season) throws JSONException
    {
        List<String> results = new ArrayList<String>();

        final String JSON_KEY_IMAGES = "images";
        final String JSON_KEY_POSTER = "poster";
        final String JSON_KEY_THUMB = "thumb";

        JSONArray rootJson = new JSONArray(jsonString);

        JSONObject seasonJson = rootJson.getJSONObject(season);
        JSONObject imagesJson = seasonJson.getJSONObject(JSON_KEY_IMAGES);
        JSONObject posterJson = imagesJson.getJSONObject(JSON_KEY_POSTER);
        String urlImage = posterJson.getString(JSON_KEY_THUMB);

        return urlImage;

    }

    private String getEpisodeImageURLFromJson(String jsonString) throws JSONException
    {
        List<String> results = new ArrayList<String>();

        final String JSON_KEY_IMAGES = "images";
                final String JSON_KEY_SCREENSHOT = "screenshot";
        final String JSON_KEY_THUMB = "thumb";

        JSONArray rootJson = new JSONArray(jsonString);

        JSONObject seasonJson = rootJson.getJSONObject(8);
        JSONObject imagesJson = seasonJson.getJSONObject(JSON_KEY_IMAGES);
        JSONObject posterJson = imagesJson.getJSONObject(JSON_KEY_SCREENSHOT);
        String urlImage = posterJson.getString(JSON_KEY_THUMB);

        return urlImage;

    }



}

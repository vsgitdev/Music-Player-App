package com.example.musicplayer.Activities;

import androidx.appcompat.app.AppCompatActivity;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;
import com.example.musicplayer.Model.Song;
import com.example.musicplayer.Adapter.SongAdapter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PlaylistActivity extends AppCompatActivity {

    // Tag for logging
    private static final String TAG = "MainActivity";

    // UI Components
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private SeekBar seekBar;
    private TextView timingTextView;

    // Media player to play songs
    private MediaPlayer mediaPlayer;
    // Handler for updating UI
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize SeekBar and TextView
        seekBar = findViewById(R.id.seekBar);
        timingTextView = findViewById(R.id.timingTextView);

        // Fetch playlist from URL
        new FetchData().execute("http://mad.mywork.gr/get_playlist.php?t=1546");

        // Initialize MediaPlayer
        mediaPlayer = new MediaPlayer();

        // Set up SeekBar change listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Seek to the specified position if the change was initiated by the user
                    mediaPlayer.seekTo(progress);
                }
                // Update the timing TextView
                updateTimingTextView();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // Remove callbacks when media playback is completed
        mediaPlayer.setOnCompletionListener(mp -> handler.removeCallbacks(updateSeekBarRunnable));
    }

    // AsyncTask to fetch playlist data from URL
    private class FetchData extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            Log.d(TAG, "doInBackground: Fetching data from URL");
            return Request(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "onPostExecute: Data fetched");
            // Parse the fetched data into a list of songs
           Log.d("testt",result);
            List<Song> songs = parse(result);
            Log.d(TAG, "onPostExecute: Number of songs fetched = " + songs.size());
            // Initialize the adapter with the fetched songs and set it to the RecyclerView
            songAdapter = new SongAdapter(songs, mediaPlayer, seekBar, timingTextView, handler, updateSeekBarRunnable);
            recyclerView.setAdapter(songAdapter);
        }
    }

    // Parse XML data to create a list of Song objects
    private List<Song> parse(String xml) {
        List<Song> songs = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            NodeList songNodes = doc.getElementsByTagName("song");

            for (int i = 0; i < songNodes.getLength(); i++) {
                Element songElement = (Element) songNodes.item(i);

                String title = songElement.getElementsByTagName("title").item(0).getTextContent();
                String artist = songElement.getElementsByTagName("artist").item(0).getTextContent();
                String url = songElement.getElementsByTagName("url").item(0).getTextContent();
                String duration = songElement.getElementsByTagName("duration").item(0).getTextContent();

                songs.add(new Song(title, artist, url, duration));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return songs;
    }

    // Make a GET request to fetch data from the specified URL
    private String Request(String urlString) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        StringBuilder result = new StringBuilder();

        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStreamReader inputStream = new InputStreamReader(urlConnection.getInputStream());
            reader = new BufferedReader(inputStream);
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result.toString();
    }

    // Update the SeekBar and timing TextView
    private void updateSeekBar() {
        seekBar.setProgress(mediaPlayer.getCurrentPosition());
        updateTimingTextView();
        handler.postDelayed(updateSeekBarRunnable, 1000);
    }

    // Runnable to periodically update the SeekBar and timing TextView
    private Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            updateSeekBar();
        }
    };

    // Update the timing TextView to show elapsed time and total duration
    private void updateTimingTextView() {
        int elapsedMillis = mediaPlayer.getCurrentPosition();
        int durationMillis = mediaPlayer.getDuration();

        String elapsed = formatTime(elapsedMillis);
        String duration = formatTime(durationMillis);

        timingTextView.setText(elapsed + " / " + duration);
    }

    // Format time in milliseconds to "MM:SS"
    private String formatTime(int millis) {
        int minutes = (millis / 1000) / 60;
        int seconds = (millis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (songAdapter != null) {
            songAdapter.releaseMediaPlayer();
        }
        if (mediaPlayer != null) {
            handler.removeCallbacks(updateSeekBarRunnable);
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}

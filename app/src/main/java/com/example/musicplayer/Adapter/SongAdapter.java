package com.example.musicplayer.Adapter;

import android.media.MediaPlayer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.Model.Song;
import com.example.musicplayer.R;

import java.io.IOException;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private static final String TAG = "SongAdapter";
    private final List<Song> songList;
    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private TextView timingTextView;
    private Handler handler;
    private Runnable updateSeekBarRunnable;
    private int currentPlayingPosition = -1;
    private SongViewHolder currentPlayingHolder = null;

    // Constructor to initialize the adapter with the required parameters
    public SongAdapter(List<Song> songList, MediaPlayer mediaPlayer, SeekBar seekBar, TextView timingTextView, Handler handler, Runnable updateSeekBarRunnable) {
        this.songList = songList;
        this.mediaPlayer = mediaPlayer;
        this.seekBar = seekBar;
        this.timingTextView = timingTextView;
        this.handler = handler;
        this.updateSeekBarRunnable = updateSeekBarRunnable;
        // Set a listener to reset the current playing when the media player completes a song
        mediaPlayer.setOnCompletionListener(mp -> resetCurrentPlaying());
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_item, parent, false);
        return new SongViewHolder(view);
    }

    // Bind data to the views (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        // Get the song at the current position
        Song song = songList.get(position);
        // Set the song title and artist in the corresponding TextViews
        holder.titleTextView.setText(song.getTitle());
        holder.artistTextView.setText(song.getArtist());

        // Set click listeners for play and pause buttons
        holder.playButton.setOnClickListener(v -> handlePlayButton(holder, position, song));
        holder.pauseButton.setOnClickListener(v -> handlePauseButton(holder));
    }

    // Handle the play button click event
    private void handlePlayButton(SongViewHolder holder, int position, Song song) {
        if (currentPlayingPosition != position) {
            // If a different song is clicked, reset the current playing song
            resetCurrentPlaying();
            try {
                // Reset and set the new data source for the media player
                mediaPlayer.reset();
                mediaPlayer.setDataSource(song.getUrl());
                mediaPlayer.prepare();
                mediaPlayer.start();
                currentPlayingPosition = position;
                currentPlayingHolder = holder;
                holder.playButton.setVisibility(View.GONE);
                holder.pauseButton.setVisibility(View.VISIBLE);
                // Start updating the seek bar
                startSeekBarUpdate();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // If the same song is clicked, resume playback
            mediaPlayer.start();
            holder.playButton.setVisibility(View.GONE);
            holder.pauseButton.setVisibility(View.VISIBLE);
            startSeekBarUpdate();
        }
    }

    // Handle the pause button click event
    private void handlePauseButton(SongViewHolder holder) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            holder.playButton.setVisibility(View.VISIBLE);
            holder.pauseButton.setVisibility(View.GONE);
            // Stop updating the seek bar
            handler.removeCallbacks(updateSeekBarRunnable);
        }
    }

    // Reset the current playing song
    private void resetCurrentPlaying() {
        if (currentPlayingHolder != null) {
            currentPlayingHolder.playButton.setVisibility(View.VISIBLE);
            currentPlayingHolder.pauseButton.setVisibility(View.GONE);
            currentPlayingPosition = -1;
            currentPlayingHolder = null;
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        handler.removeCallbacks(updateSeekBarRunnable);
        seekBar.setProgress(0);
        timingTextView.setText("00:00 / 00:00");
    }

    // Start updating the seek bar
    private void startSeekBarUpdate() {
        seekBar.setMax(mediaPlayer.getDuration());
        updateSeekBar();
        handler.postDelayed(updateSeekBarRunnable, 1000);
    }

    // Update the seek bar and timing text view
    private void updateSeekBar() {
        seekBar.setProgress(mediaPlayer.getCurrentPosition());
        updateTimingTextView();
        handler.postDelayed(updateSeekBarRunnable, 1000);
    }

    // Update the timing text view
    private void updateTimingTextView() {
        int elapsedMillis = mediaPlayer.getCurrentPosition();
        int durationMillis = mediaPlayer.getDuration();

        String elapsed = formatTime(elapsedMillis);
        String duration = formatTime(durationMillis);

        timingTextView.setText(elapsed + " / " + duration);
    }

    // Format time from milliseconds to "MM:SS"
    private String formatTime(int millis) {
        int minutes = (millis / 1000) / 60;
        int seconds = (millis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // Return the size of the dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return songList.size();
    }

    // Handle the view being recycled (invoked by the layout manager)
    @Override
    public void onViewRecycled(@NonNull SongViewHolder holder) {
        super.onViewRecycled(holder);
        if (currentPlayingHolder == holder) {
            resetCurrentPlaying();
        }
    }

    // Provide a reference to the type of views that you are using (custom ViewHolder)
    public static class SongViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView;
        public TextView artistTextView;
        public ImageButton playButton;
        public ImageButton pauseButton;

        public SongViewHolder(View view) {
            super(view);
            titleTextView = view.findViewById(R.id.titleTextView);
            artistTextView = view.findViewById(R.id.artistTextView);
            playButton = view.findViewById(R.id.playButton);
            pauseButton = view.findViewById(R.id.pauseButton);
        }
    }

    // Release the media player resources
    public void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}

/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

import twitter4j.ExtendedMediaEntity;

/**
 * Created by akihit on 2016/07/17.
 */
public class VideoMediaFragment extends MediaViewActivity.MediaFragment {
  @SuppressWarnings("unused")
  private static final String TAG = VideoMediaFragment.class.getSimpleName();
  private VideoView videoView;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    return inflater.inflate(R.layout.view_video, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    videoView = (VideoView) view.findViewById(R.id.media_video);
  }

  @Override
  public void onStart() {
    super.onStart();
    final String url = selectVideo();
    if (url == null) {
      return;
    }

    videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mediaPlayer) {
        videoView.start();
      }
    });
    videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mediaPlayer) {
        videoView.stopPlayback();
      }
    });
    Log.d(TAG, "onStart: video: " + url);
    videoView.setVideoURI(Uri.parse(url));
  }

  private String selectVideo() {
    final ExtendedMediaEntity.Variant[] videoVariants = mediaEntity.getVideoVariants();
    for (ExtendedMediaEntity.Variant v : videoVariants) {
      if (v.getContentType().equals("video/mp4")) {
        return v.getUrl();
      }
    }
    return null;
  }

  @Override
  public void onStop() {
    videoView.stopPlayback();
    videoView.setOnPreparedListener(null);
    videoView.setOnCompletionListener(null);
    videoView.setVideoURI(null);
    super.onStop();
  }
}

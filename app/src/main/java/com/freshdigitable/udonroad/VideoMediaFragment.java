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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import twitter4j.ExtendedMediaEntity;

/**
 * Created by akihit on 2016/07/17.
 */
public class VideoMediaFragment extends MediaViewActivity.MediaFragment {
  @SuppressWarnings("unused")
  private static final String TAG = VideoMediaFragment.class.getSimpleName();
  private VideoView videoView;
  private View rootView;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    rootView = inflater.inflate(R.layout.view_video, container, false);
    rootView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (pageClickListener != null) {
          pageClickListener.onClick(view);
        }
        Log.d(TAG, "onClick: video");
        if (isCompleted) {
          videoView.seekTo(0);
          videoView.resume();
          isCompleted = false;
        }
      }
    });
    return rootView;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    videoView = (VideoView) view.findViewById(R.id.media_video);
  }

  private boolean isCompleted = false;

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
//        Log.d(TAG, "onPrepared: ");
        videoView.start();
      }
    });
    videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mediaPlayer) {
//        Log.d(TAG, "onCompletion: ");
        videoView.stopPlayback();
        isCompleted = true;
      }
    });
    videoView.setVideoURI(Uri.parse(url));
  }

  private String selectVideo() {
    final List<ExtendedMediaEntity.Variant> playableMedia = findPlayableMedia();
    if (playableMedia.size() == 0) {
      return null;
    } else if (playableMedia.size() == 1) {
      return playableMedia.get(0).getUrl();
    }

    Collections.sort(playableMedia, new Comparator<ExtendedMediaEntity.Variant>() {
      @Override
      public int compare(ExtendedMediaEntity.Variant l, ExtendedMediaEntity.Variant r) {
        return l.getBitrate() - r.getBitrate();
      }
    });
    return playableMedia.get(1).getUrl();
  }

  private List<ExtendedMediaEntity.Variant> findPlayableMedia() {
    final ExtendedMediaEntity.Variant[] videoVariants = mediaEntity.getVideoVariants();
    List<ExtendedMediaEntity.Variant> res = new ArrayList<>(videoVariants.length);
    for (ExtendedMediaEntity.Variant v : videoVariants) {
      if (v.getContentType().equals("video/mp4")) {
        res.add(v);
      }
    }
    return res;
  }

  @Override
  public void onStop() {
    videoView.stopPlayback();
    videoView.setOnPreparedListener(null);
    videoView.setOnCompletionListener(null);
    videoView.setVideoURI(null);
    super.onStop();
  }

  @Override
  public void onDestroyView() {
    rootView.setOnClickListener(null);
    super.onDestroyView();
  }
}

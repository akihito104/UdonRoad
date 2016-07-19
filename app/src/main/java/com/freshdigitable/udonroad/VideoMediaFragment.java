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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import twitter4j.ExtendedMediaEntity.Variant;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Created by akihit on 2016/07/17.
 */
public class VideoMediaFragment extends MediaViewActivity.MediaFragment {
  @SuppressWarnings("unused")
  private static final String TAG = VideoMediaFragment.class.getSimpleName();
  private VideoView videoView;
  private View rootView;
  private ProgressBar progressBar;
  private Subscription subscribe;
  private TextView progressText;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    rootView = inflater.inflate(R.layout.view_video, container, false);
    return rootView;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    videoView = (VideoView) view.findViewById(R.id.media_video);
    progressBar = (ProgressBar) view.findViewById(R.id.media_progressBar);
    progressText = (TextView) view.findViewById(R.id.media_progressText);
  }

  private boolean isCompleted = false;

  @Override
  public void onStart() {
    super.onStart();
    final String url = selectVideo();
    if (url == null) {
      return;
    }

    rootView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (pageClickListener != null) {
          pageClickListener.onClick(view);
        }
        if (isCompleted) {
          final VideoView video = (VideoView) view.findViewById(R.id.media_video);
          video.seekTo(0);
          video.resume();
          isCompleted = false;
        }
      }
    });
    rootView.setOnTouchListener(super.touchListener);

    videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mediaPlayer) {
        progressBar.setMax(mediaPlayer.getDuration());
        videoView.start();
      }
    });
    videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mediaPlayer) {
        videoView.stopPlayback();
        isCompleted = true;
      }
    });
    videoView.setVideoURI(Uri.parse(url));

    subscribe = Observable.interval(500, MILLISECONDS, Schedulers.io())
        .onBackpressureLatest()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<Long>() {
          @Override
          public void call(Long aLong) {
            if (!videoView.isPlaying()) {
              return;
            }

            final int currentPosition = videoView.getCurrentPosition();
            progressBar.setProgress(currentPosition);

            final int remain = videoView.getDuration() - currentPosition;
            final long minutes = MILLISECONDS.toMinutes(remain);
            final long seconds = MILLISECONDS.toSeconds(remain - MINUTES.toMillis(minutes));
            progressText.setText(
                String.format(getString(R.string.media_remain_time), minutes, seconds));
          }
        }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            Log.e(TAG, "call: ", throwable);
          }
        });
  }

  private String selectVideo() {
    final List<Variant> playableMedia = findPlayableMedia();
    if (playableMedia.size() == 0) {
      return null;
    } else if (playableMedia.size() == 1) {
      return playableMedia.get(0).getUrl();
    }

    Collections.sort(playableMedia, new Comparator<Variant>() {
      @Override
      public int compare(Variant l, Variant r) {
        return l.getBitrate() - r.getBitrate();
      }
    });
    return playableMedia.get(1).getUrl();
  }

  private List<Variant> findPlayableMedia() {
    final Variant[] videoVariants = mediaEntity.getVideoVariants();
    List<Variant> res = new ArrayList<>(videoVariants.length);
    for (Variant v : videoVariants) {
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
    rootView.setOnClickListener(null);
    rootView.setOnTouchListener(null);
    if (subscribe != null && !subscribe.isUnsubscribed()) {
      subscribe.unsubscribe();
    }
    super.onStop();
  }
}

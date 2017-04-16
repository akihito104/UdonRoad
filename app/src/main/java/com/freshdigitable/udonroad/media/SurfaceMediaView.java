/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.udonroad.media;

import android.databinding.DataBindingUtil;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.databinding.FragmentSurfaceMediaBinding;
import com.freshdigitable.udonroad.media.MediaViewActivity.MediaFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import twitter4j.MediaEntity;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Created by akihit on 2017/04/15.
 */

public class SurfaceMediaView extends MediaFragment {
  private static final String TAG = SurfaceMediaView.class.getSimpleName();
  private FragmentSurfaceMediaBinding binding;
  private final MediaPlayer mediaPlayer = new MediaPlayer();
  private Subscription subscribe;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    binding = DataBindingUtil.inflate(inflater, R.layout.fragment_surface_media, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    final View.OnClickListener pageClickListener = getOnClickListener();
    view.setOnClickListener(v -> {
      if (pageClickListener != null) {
        pageClickListener.onClick(v);
      }
      if (!mediaPlayer.isPlaying()) {
        mediaPlayer.seekTo(0);
        mediaPlayer.start();
      }
    });
    view.setOnTouchListener(super.getTouchListener());
    binding.mediaVideo.getHolder().addCallback(new SurfaceHolder.Callback() {
      @Override
      public void surfaceCreated(SurfaceHolder holder) {
        mediaPlayer.setDisplay(holder);
      }

      @Override
      public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
      }

      @Override
      public void surfaceDestroyed(SurfaceHolder holder) {
        holder.removeCallback(this);
      }
    });
  }

  @Override
  public void onStart() {
    super.onStart();
    try {
      setupMediaPlayer();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    setupProgressBar();
  }

  @Override
  public void onResume() {
    super.onResume();
    mediaPlayer.prepareAsync();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (subscribe != null && !subscribe.isUnsubscribed()) {
      subscribe.unsubscribe();
    }
    mediaPlayer.setDisplay(null);
    mediaPlayer.stop();
    mediaPlayer.release();
    mediaPlayer.setOnPreparedListener(null);
    mediaPlayer.setOnVideoSizeChangedListener(null);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    final View rootView = getView();
    if (rootView != null) {
      rootView.setOnClickListener(null);
      rootView.setOnTouchListener(null);
    }
  }

  private void setupMediaPlayer() throws IOException {
    final Uri mediaUri = Uri.parse(selectVideo());
    mediaPlayer.setDataSource(getContext(), mediaUri);

    mediaPlayer.setOnPreparedListener(mp -> {
      final int duration = mp.getDuration();
      binding.mediaProgressBar.setMax(duration);
      mp.start();
    });
    mediaPlayer.setOnVideoSizeChangedListener((mp, width, height) -> {
      final int videoWidth = mp.getVideoWidth();
      final int videoHeight = mp.getVideoHeight();
      binding.mediaVideo.getHolder().setFixedSize(videoWidth, videoHeight);
      final int parentWidth = ((View) binding.mediaVideo.getParent()).getWidth();
      final int parentHeight = ((View) binding.mediaVideo.getParent()).getHeight();
      final float factor = videoWidth * parentHeight > videoHeight * parentWidth
          ? (float) parentWidth / videoWidth
          : (float) parentHeight / videoHeight;
      final ViewGroup.LayoutParams layoutParams = binding.mediaVideo.getLayoutParams();
      layoutParams.width = (int) (factor * videoWidth);
      layoutParams.height = (int) (factor * videoHeight);
      binding.mediaVideo.setLayoutParams(layoutParams);
    });
  }

  private void setupProgressBar() {
    final String timeElapseFormat = getString(R.string.media_remain_time);
    subscribe = Observable.interval(500, MILLISECONDS, Schedulers.io())
        .onBackpressureLatest()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(aLong -> {
          if (!mediaPlayer.isPlaying()) {
            return;
          }

          final int currentPosition = mediaPlayer.getCurrentPosition();
          binding.mediaProgressBar.setProgress(currentPosition);

          final int remain = mediaPlayer.getDuration() - currentPosition;
          final long minutes = MILLISECONDS.toMinutes(remain);
          final long seconds = MILLISECONDS.toSeconds(remain - MINUTES.toMillis(minutes));
          binding.mediaProgressText.setText(String.format(timeElapseFormat, minutes, seconds));
        }, throwable -> Log.e(TAG, "call: ", throwable));
  }

  private String selectVideo() {
    final List<MediaEntity.Variant> playableMedia = findPlayableMedia();
    if (playableMedia.size() == 0) {
      return null;
    } else if (playableMedia.size() == 1) {
      return playableMedia.get(0).getUrl();
    }

    Collections.sort(playableMedia, (l, r) -> l.getBitrate() - r.getBitrate());
    return playableMedia.get(1).getUrl();
  }

  private List<MediaEntity.Variant> findPlayableMedia() {
    final MediaEntity.Variant[] videoVariants = mediaEntity.getVideoVariants();
    List<MediaEntity.Variant> res = new ArrayList<>(videoVariants.length);
    for (MediaEntity.Variant v : videoVariants) {
      if (v.getContentType().equals("video/mp4")) {
        res.add(v);
      }
    }
    return res;
  }
}

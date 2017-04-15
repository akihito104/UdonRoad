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
  private MediaPlayer mediaPlayer;

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
  public void onStart() {
    super.onStart();
    final View rootView = getView();
    if (rootView == null) {
      return;
    }
    final View.OnClickListener pageClickListener = getOnClickListener();
    rootView.setOnClickListener(view -> {
      if (pageClickListener != null) {
        pageClickListener.onClick(view);
      }
      if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
        mediaPlayer.seekTo(0);
        mediaPlayer.start();
      }
    });
    rootView.setOnTouchListener(super.getTouchListener());

    final Uri mediaUri = Uri.parse(selectVideo());
    final String timeElapseFormat = getString(R.string.media_remain_time);
    binding.mediaVideo.getHolder().addCallback(new SurfaceHolder.Callback() {
      private Subscription subscribe;

      @Override
      public void surfaceCreated(SurfaceHolder holder) {
        mediaPlayer = MediaPlayer.create(getContext(), mediaUri, holder);
        mediaPlayer.setOnPreparedListener(mp -> {
          final int duration = mp.getDuration();
          binding.mediaProgressBar.setMax(duration);
          mp.start();
          subscribe = Observable.interval(500, MILLISECONDS, Schedulers.io())
              .onBackpressureLatest()
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(aLong -> {
                if (!mp.isPlaying()) {
                  return;
                }

                final int currentPosition = mp.getCurrentPosition();
                binding.mediaProgressBar.setProgress(currentPosition);

                final int remain = duration - currentPosition;
                final long minutes = MILLISECONDS.toMinutes(remain);
                final long seconds = MILLISECONDS.toSeconds(remain - MINUTES.toMillis(minutes));
                binding.mediaProgressText.setText(String.format(timeElapseFormat, minutes, seconds));
              }, throwable -> Log.e(TAG, "call: ", throwable));
        });
      }

      @Override
      public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
      }

      @Override
      public void surfaceDestroyed(SurfaceHolder holder) {
        if (subscribe != null && !subscribe.isUnsubscribed()) {
          subscribe.unsubscribe();
        }
        if (mediaPlayer != null) {
          mediaPlayer.setDisplay(null);
          mediaPlayer.stop();
          mediaPlayer.release();
        }
        holder.removeCallback(this);
      }
    });
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

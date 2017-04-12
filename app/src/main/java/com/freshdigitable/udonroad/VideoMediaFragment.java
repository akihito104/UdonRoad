/*
 * Copyright (c) 2016. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad;

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
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import twitter4j.MediaEntity.Variant;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * VideoMediaFragment shows video media.
 *
 * Created by akihit on 2016/07/17.
 */
public class VideoMediaFragment extends MediaViewActivity.MediaFragment {
  @SuppressWarnings("unused")
  private static final String TAG = VideoMediaFragment.class.getSimpleName();
  private VideoView videoView;
  private Subscription subscribe;

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
    final View rootView = getView();
    if (rootView == null) {
      return;
    }
    final VideoView vv = this.videoView;
    final View.OnClickListener pageClickListener = getOnClickListener();
    rootView.setOnClickListener(view -> {
      if (pageClickListener != null) {
        pageClickListener.onClick(view);
      }
      if (!vv.isPlaying()) {
        vv.seekTo(0);
        vv.resume();
      }
    });
    rootView.setOnTouchListener(super.getTouchListener());

    final ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.media_progressBar);
    videoView.setOnPreparedListener(mediaPlayer -> {
      progressBar.setMax(mediaPlayer.getDuration());
      vv.start();
    });
    final TextView progressText = (TextView) rootView.findViewById(R.id.media_progressText);
    final String timeElapseFormat = getString(R.string.media_remain_time);
    subscribe = Observable.interval(500, MILLISECONDS, Schedulers.io())
        .onBackpressureLatest()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(aLong -> {
          if (!vv.isPlaying()) {
            return;
          }

          final int currentPosition = vv.getCurrentPosition();
          progressBar.setProgress(currentPosition);

          final int remain = vv.getDuration() - currentPosition;
          final long minutes = MILLISECONDS.toMinutes(remain);
          final long seconds = MILLISECONDS.toSeconds(remain - MINUTES.toMillis(minutes));
          progressText.setText(String.format(timeElapseFormat, minutes, seconds));
        }, throwable -> Log.e(TAG, "call: ", throwable));

    videoView.setVideoURI(Uri.parse(url));
  }

  private String selectVideo() {
    final List<Variant> playableMedia = findPlayableMedia();
    if (playableMedia.size() == 0) {
      return null;
    } else if (playableMedia.size() == 1) {
      return playableMedia.get(0).getUrl();
    }

    Collections.sort(playableMedia, (l, r) -> l.getBitrate() - r.getBitrate());
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
  public void onPause() {
    super.onPause();
    videoView.stopPlayback();
  }

  @Override
  public void onStop() {
    super.onStop();
    videoView.setOnPreparedListener(null);
    final View rootView = getView();
    if (rootView != null) {
      rootView.setOnClickListener(null);
      rootView.setOnTouchListener(null);
    }
    if (subscribe != null && !subscribe.isUnsubscribed()) {
      subscribe.unsubscribe();
    }
  }
}

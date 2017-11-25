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

package com.freshdigitable.udonroad.media;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.repository.ImageQuery;
import com.freshdigitable.udonroad.repository.ImageRepository;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;
import twitter4j.MediaEntity;

/**
 * PhotoMediaFragment shows general image file such a photo.
 *
 * Created by akihit on 2016/07/17.
 */
public class PhotoMediaFragment extends MediaViewActivity.MediaFragment {
  @SuppressWarnings("unused")
  private static final String TAG = PhotoMediaFragment.class.getSimpleName();
  private ImageView imageView;
  private ProgressBar progressBar;
  @Inject
  ImageRepository imageRepository;
  private Disposable imageSubs;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    InjectionUtil.getComponent(this).inject(this);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    final View v = inflater.inflate(R.layout.fragment_photo_media, container, false);
    imageView = v.findViewById(R.id.media_image_view);
    progressBar = v.findViewById(R.id.media_progress_bar);
    return v;
  }

  @Override
  public void onStart() {
    super.onStart();
    imageView.setOnClickListener(super.getOnClickListener());
    imageView.setOnTouchListener(super.getTouchListener());
    if (!Utils.isSubscribed(imageSubs)) {
      progressBar.setVisibility(View.VISIBLE);
      imageSubs = imageRepository.queryImage(new ImageQuery.Builder(getUrl()).build())
          .subscribe(d -> {
            progressBar.setVisibility(View.GONE);
            imageView.setImageDrawable(d);
          }, th -> {
            Toast.makeText(getContext(), R.string.msg_media_failed_loading, Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
          });
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    imageView.setOnClickListener(null);
    imageView.setOnTouchListener(null);
    progressBar.setVisibility(View.GONE);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    Utils.maybeDispose(imageSubs);
  }

  private static final String ARGS_URL = "url";

  static PhotoMediaFragment getInstance(MediaEntity mediaEntity) {
    final Bundle args = new Bundle();
    final String url = mediaEntity.getMediaURLHttps() + ":medium";
    args.putString(ARGS_URL, url);
    final PhotoMediaFragment fragment = new PhotoMediaFragment();
    fragment.setArguments(args);
    return fragment;
  }

  String getUrl() {
    return getArguments().getString(ARGS_URL);
  }
}

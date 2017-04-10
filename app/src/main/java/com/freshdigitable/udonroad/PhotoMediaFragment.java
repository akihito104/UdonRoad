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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

/**
 * PhotoMediaFragment shows general image file such a photo.
 *
 * Created by akihit on 2016/07/17.
 */
public class PhotoMediaFragment extends MediaViewActivity.MediaFragment {
  public static final String TAG = PhotoMediaFragment.class.getSimpleName();
  private ImageView imageView;
  private String loadingTag;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    imageView = new ImageView(getContext());
    return imageView;
  }

  @Override
  public void onStart() {
    super.onStart();
    imageView.setOnClickListener(super.getOnClickListener());
    imageView.setOnTouchListener(touchListener);
    loadingTag = "media:" + mediaEntity.getId();
    Picasso.with(getContext())
        .load(mediaEntity.getMediaURLHttps() + ":medium")
        .tag(loadingTag)
        .into((ImageView) getView());
  }

  @Override
  public void onStop() {
    super.onStop();
    Picasso.with(getContext()).cancelTag(loadingTag);
    imageView.setOnClickListener(null);
    imageView.setOnTouchListener(null);
    imageView.setImageDrawable(null);
  }
}

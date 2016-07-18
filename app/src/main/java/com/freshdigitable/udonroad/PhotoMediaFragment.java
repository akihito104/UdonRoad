/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
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
 * Created by akihit on 2016/07/17.
 */
public class PhotoMediaFragment extends MediaViewActivity.MediaFragment {
  public static final String TAG = PhotoMediaFragment.class.getSimpleName();
  private ImageView imageView;

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
    imageView.setOnClickListener(super.pageClickListener);
    imageView.setOnTouchListener(super.touchListener);
    Picasso.with(getContext())
        .load(mediaEntity.getMediaURLHttps() + ":medium")
        .into((ImageView) getView());
  }

  @Override
  public void onStop() {
    Picasso.with(getContext()).cancelRequest((ImageView) getView());
    imageView.setOnClickListener(null);
    imageView.setOnTouchListener(null);
    super.onStop();
  }
}

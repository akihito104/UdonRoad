/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * Created by akihit on 2016/07/10.
 */
public class MediaImageView extends AppCompatImageView {

  public MediaImageView(Context context) {
    this(context, null);
  }

  public MediaImageView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public MediaImageView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setContentDescription(getResources().getString(R.string.tweet_media_descs));
    setVisibility(GONE);
  }
}

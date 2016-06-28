/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;

/**
 * Created by akihit on 2016/06/26.
 */
public class QuotedStatusView extends StatusViewBase {
  public QuotedStatusView(Context context) {
    this(context, null);
  }

  public QuotedStatusView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public QuotedStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    setBackgroundResource(R.drawable.s_quoted_frame);

    final View v = View.inflate(context, R.layout.view_quoted_status, this);
    createdAt = (TextView) v.findViewById(R.id.q_create_at);
    icon = (ImageView) v.findViewById(R.id.q_icon);
    names = (TextView) v.findViewById(R.id.q_names);
    tweet = (TextView) v.findViewById(R.id.q_tweet);
    clientName = (TextView) v.findViewById(R.id.q_via);
    rtIcon = (ImageView) v.findViewById(R.id.q_rt_icon);
    rtCount = (TextView) v.findViewById(R.id.q_rtcount);
    favIcon = (ImageView) v.findViewById(R.id.q_fav_icon);
    favCount = (TextView) v.findViewById(R.id.q_favcount);
    mediaGroup = v.findViewById(R.id.q_image_group);
    mediaImages = new ImageView[]{
        (ImageView) v.findViewById(R.id.q_image_1),
        (ImageView) v.findViewById(R.id.q_image_2),
        (ImageView) v.findViewById(R.id.q_image_3),
        (ImageView) v.findViewById(R.id.q_image_4)
    };
  }

  @Override
  public void reset() {
    super.reset();
    setBackgroundResource(R.drawable.s_quoted_frame);
  }

  @Override
  protected String parseText(Status status) {
    String text = getBindingStatus(status).getText();
    final URLEntity[] urlEntities = status.getURLEntities();
    for (URLEntity u : urlEntities) {
      text = text.replace(u.getURL(), u.getDisplayURL());
    }
    final ExtendedMediaEntity[] extendedMediaEntities = status.getExtendedMediaEntities();
    for (ExtendedMediaEntity eme : extendedMediaEntities) {
      text = text.replace(eme.getURL(), "");
    }
    return text;
  }
}

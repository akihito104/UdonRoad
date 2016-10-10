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

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.v4.view.ViewCompat;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import twitter4j.URLEntity;
import twitter4j.User;

/**
 * User information ViewGroup
 *
 * Created by akihit on 2016/06/03.
 */
public class UserInfoView extends RelativeLayout {
  private ImageView banner;
  private TextView name;
  private TextView screenName;
  private ImageView icon;
  private TextView description;
  private TextView url;
  private TextView location;
  private View urlIcon;
  private View locationIcon;

  public UserInfoView(Context context) {
    this(context, null);
  }

  public UserInfoView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public UserInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    final View v = View.inflate(context, R.layout.view_user_info, this);
    name = (TextView) v.findViewById(R.id.user_name);
    screenName = (TextView) v.findViewById(R.id.user_screen_name);
    description = (TextView) v.findViewById(R.id.user_description);
    description.setMovementMethod(LinkMovementMethod.getInstance());
    banner = (ImageView) v.findViewById(R.id.user_banner);
    icon = (ImageView) v.findViewById(R.id.user_icon);
    url = (TextView) v.findViewById(R.id.user_url);
    urlIcon = v.findViewById(R.id.user_url_icon);
    location = (TextView) v.findViewById(R.id.user_location);
    locationIcon = v.findViewById(R.id.user_location_icon);
    ViewCompat.setTransitionName(icon, "user_icon");
  }

  public void bindData(User user) {
    name.setText(user.getName());

    UserInfoActivity.bindUserScreenName(screenName, user);
    final CharSequence desc = SpannableStringUtil.create(user.getDescription(),
        user.getDescriptionURLEntities());
    description.setText(desc);

    final String profileLinkColor = user.getProfileLinkColor();
    if (isColorParsable(profileLinkColor)) {
      final int color = parseColor(profileLinkColor);
      banner.setBackgroundColor(color);
    }

    bindURL(user);

    if (TextUtils.isEmpty(user.getLocation())) {
      locationIcon.setVisibility(GONE);
      location.setVisibility(GONE);
    } else {
      locationIcon.setVisibility(VISIBLE);
      location.setVisibility(VISIBLE);
      location.setText(user.getLocation());
    }
  }

  private void bindURL(User user) {
    final URLEntity urlEntity = user.getURLEntity();
    if (urlEntity != null) {
      final String displayURL = urlEntity.getDisplayURL();
      final String expandedURL = urlEntity.getExpandedURL();
      final String plainUrl = urlEntity.getURL();
      bindURL(displayURL != null ? displayURL : plainUrl,
          expandedURL != null ? expandedURL : plainUrl);
      return;
    }
    final String url = user.getURL();
    if (!TextUtils.isEmpty(url)) {
      bindURL(url, url);
      return;
    }
    urlIcon.setVisibility(GONE);
    this.url.setVisibility(GONE);
  }

  private void bindURL(String displayUrl, String actualUrl) {
    if (TextUtils.isEmpty(displayUrl) || TextUtils.isEmpty(actualUrl)) {
      return;
    }
    final SpannableStringBuilder ssb = new SpannableStringBuilder(displayUrl);
    ssb.setSpan(new UnderlineSpan(), 0, displayUrl.length(), 0);
    url.setText(ssb);
    url.setVisibility(VISIBLE);
    urlIcon.setVisibility(VISIBLE);
    final OnClickListener clickListener = create(actualUrl);
    url.setOnClickListener(clickListener);
    urlIcon.setOnClickListener(clickListener);
  }

  private OnClickListener create(final String actualUrl) {
    if (TextUtils.isEmpty(actualUrl)) {
      return null;
    }
    return new OnClickListener() {
      @Override
      public void onClick(View v) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(actualUrl));
        v.getContext().startActivity(intent);
      }
    };
  }

  private static boolean isColorParsable(String colorString) {
    return colorString != null
        && colorString.length() >= 6
        && colorString.length() <= 8;
  }

  @ColorInt
  private static int parseColor(String colorString) {
    return Color.parseColor("#" + colorString);
  }

  public ImageView getBanner() {
    return banner;
  }

  public ImageView getIcon() {
    return icon;
  }
}

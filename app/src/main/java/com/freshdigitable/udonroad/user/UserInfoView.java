/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad.user;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.SpannableStringUtil;
import com.freshdigitable.udonroad.Utils;

import twitter4j.Relationship;
import twitter4j.URLEntity;
import twitter4j.User;

/**
 * User information ViewGroup
 *
 * Created by akihit on 2016/06/03.
 */
public class UserInfoView extends RelativeLayout {
  private final ImageView banner;
  private final TextView name;
  private final TextView screenName;
  private final ImageView icon;
  private final TextView description;
  private final TextView url;
  private final TextView location;
  private final View verifiedIcon;
  private final View protectedIcon;
  private final TextView followingStatus;
  private final View mutedStatus;

  public UserInfoView(Context context) {
    this(context, null);
  }

  public UserInfoView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public UserInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    final View v = View.inflate(context, R.layout.view_user_info, this);
    name = v.findViewById(R.id.user_name);
    screenName = v.findViewById(R.id.user_screen_name);
    description = v.findViewById(R.id.user_description);
    banner = v.findViewById(R.id.user_banner);
    icon = v.findViewById(R.id.user_icon);
    url = v.findViewById(R.id.user_url);
    location = v.findViewById(R.id.user_location);
    verifiedIcon = v.findViewById(R.id.user_verified_icon);
    protectedIcon = v.findViewById(R.id.user_protected_icon);
    followingStatus = v.findViewById(R.id.user_following);
    mutedStatus = v.findViewById(R.id.user_muted);
  }

  public void bindData(User user) {
    name.setText(user.getName());
    if (user.isVerified()) {
      verifiedIcon.setVisibility(VISIBLE);
    }
    if (user.isProtected()) {
      protectedIcon.setVisibility(VISIBLE);
    }
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
      location.setVisibility(GONE);
    } else {
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
    this.url.setVisibility(GONE);
  }

  private void bindURL(String displayUrl, String actualUrl) {
    if (TextUtils.isEmpty(displayUrl) || TextUtils.isEmpty(actualUrl)) {
      return;
    }
    Utils.colorStateLinkify(url);
    final SpannableStringBuilder ssb = new SpannableStringBuilder(displayUrl);
    ssb.setSpan(new URLSpan(actualUrl), 0, displayUrl.length(), 0);
    url.setText(ssb);
    url.setVisibility(VISIBLE);
  }

  public void bindRelationship(Relationship relationship) {
    if (relationship.isSourceFollowingTarget()) {
      bindFollowingStatus(R.string.user_following, R.color.twitter_primary);
    } else if (relationship.isSourceBlockingTarget()) {
      bindFollowingStatus(R.string.user_blocking, R.color.twitter_muted);
    } else {
      followingStatus.setVisibility(GONE);
    }
    mutedStatus.setVisibility(
        relationship.isSourceMutingTarget() ? VISIBLE : GONE);
  }

  private void bindFollowingStatus(@StringRes int status, @ColorRes int color) {
    followingStatus.setText(status);
    ViewCompat.setBackgroundTintList(followingStatus,
        ColorStateList.valueOf(ContextCompat.getColor(getContext(), color)));
    followingStatus.setVisibility(VISIBLE);
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

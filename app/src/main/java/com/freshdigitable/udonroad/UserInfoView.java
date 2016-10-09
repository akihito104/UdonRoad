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
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.v4.view.ViewCompat;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

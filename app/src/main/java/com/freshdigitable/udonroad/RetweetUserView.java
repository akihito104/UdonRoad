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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatTextView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.AttributeSet;

/**
 * RetweetUserView shows retweeting user name with icon
 *
 * Created by akihit on 2016/12/15.
 */

public class RetweetUserView extends AppCompatTextView {

  private final String rtBy;
  private final String screenNameTemplate;
  private final int iconSize;

  public RetweetUserView(Context context) {
    this(context, null);
  }

  public RetweetUserView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public RetweetUserView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    rtBy = getResources().getString(R.string.tweet_rtby);
    screenNameTemplate = getResources().getString(R.string.tweet_name);
    iconSize = getResources().getDimensionPixelSize(R.dimen.small_user_icon);
  }

  public void bindUser(Bitmap icon, String screenName) {
    bindUser(new RefinedImageSpan(getContext(), icon), screenName);
  }

  public void bindUser(Drawable drawable, String screenName) {
    drawable.setBounds(0, 0, iconSize, iconSize);
    bindUser(new RefinedImageSpan(drawable), screenName);
  }

  private void bindUser(ImageSpan icon, String screenName) {
    SpannableStringBuilder ssb = new SpannableStringBuilder(rtBy + "   ");
    ssb.setSpan(icon, rtBy.length() + 1, rtBy.length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    ssb.append(String.format(screenNameTemplate, screenName));
    setText(ssb);
  }
}

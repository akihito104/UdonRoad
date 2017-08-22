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
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatTextView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.AttributeSet;

/**
 * RetweetUserView shows retweeting user name with icon.<br>
 * Icon image is inserted before `@`.
 *
 * Created by akihit on 2016/12/15.
 */

public class RetweetUserView extends AppCompatTextView {

  private final int iconSize;
  private final int iconMargin;
  private final Drawable maskerDrawable;
  private final String retweetUserTemplate;
  private final int iconStart;
  private final int iconEnd;

  public RetweetUserView(Context context) {
    this(context, null);
  }

  public RetweetUserView(Context context, AttributeSet attrs) {
    this(context, attrs, R.attr.retweetUserViewStyle);
  }

  public RetweetUserView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    final String rtTemplate = context.getString(R.string.tweet_retweet_user);
    final int atIndex = rtTemplate.indexOf("@");
    if (atIndex < 0) {
      throw new IllegalArgumentException("R.string.tweet_retweet_user must contain '@', but now one is: " + rtTemplate);
    } else if (atIndex == 0) {
      retweetUserTemplate = " " + rtTemplate;
      iconStart = 0;
    } else {
      if (rtTemplate.charAt(atIndex - 1) == ' ') {
        retweetUserTemplate = rtTemplate;
        iconStart = atIndex - 1;
      } else {
        retweetUserTemplate = rtTemplate.replace("@", " @");
        iconStart = atIndex;
      }
    }
    iconEnd = iconStart + 1;

    maskerDrawable = AppCompatResources.getDrawable(context, R.drawable.s_rounded_mask_small);
    final TypedArray a = context.obtainStyledAttributes(
        attrs, R.styleable.RetweetUserView, defStyleAttr, R.style.Widget_RetweetUserView);
    try {
      iconSize = a.getDimensionPixelSize(R.styleable.RetweetUserView_iconSize, -1);
      iconMargin = a.getDimensionPixelSize(R.styleable.RetweetUserView_iconMargin, -1);
    } finally {
      a.recycle();
    }
    setHeight(iconSize);
  }

  public void bindUser(Bitmap icon, String screenName) {
    final BitmapDrawable wrappedIcon = new BitmapDrawable(getResources(), icon);
    bindUser(wrappedIcon, screenName);
  }

  public void bindUser(Drawable icon, String screenName) {
    final RoundedCornerDrawable roundedIcon = icon instanceof RoundedCornerDrawable
        ? ((RoundedCornerDrawable) icon)
        : new RoundedCornerDrawable(maskerDrawable, icon);
    roundedIcon.setBounds(0, 0, iconSize, iconSize);
    bindUser(createIconSpan(roundedIcon), screenName);
  }

  private void bindUser(ImageSpan icon, String screenName) {
    final String newText = String.format(retweetUserTemplate, screenName);
    SpannableStringBuilder ssb = new SpannableStringBuilder(newText);
    ssb.setSpan(icon, iconStart, iconEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    setText(ssb);
  }

  @NonNull
  private RefinedImageSpan createIconSpan(RoundedCornerDrawable roundedCornerDrawable) {
    return new RefinedImageSpan(roundedCornerDrawable, RefinedImageSpan.ALIGN_CENTER,
        iconStart == 0 ? 0 : iconMargin, iconMargin);
  }
}

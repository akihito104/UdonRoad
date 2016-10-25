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
import android.graphics.Typeface;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.AppCompatTextView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.AttributeSet;

import twitter4j.User;

/**
 * CombinedScreenNameTextView combines screen name and account with specified format.
 *
 * Created by akihit on 2016/07/09.
 */
public class CombinedScreenNameTextView extends AppCompatTextView {
  public CombinedScreenNameTextView(Context context) {
    this(context, null);
  }

  public CombinedScreenNameTextView(Context context, AttributeSet attrs) {
    this(context, attrs, android.R.attr.textViewStyle);
  }

  public CombinedScreenNameTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  private String name;
  private String screenName;

  public void setNames(User user) {
    final String name = user.getName();
    final String screenName = user.getScreenName();
    if (this.name != null && this.name.equals(name)
        && this.screenName != null && this.screenName.equals(screenName)) {
      return;
    }
    final String formatted = name
        + (TextViewCompat.getMaxLines(this) == 2 ? "\n" : " ")
        + "@" + screenName;
    final SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(formatted);
    spannableStringBuilder.setSpan(STYLE_BOLD, 0, name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    setText(spannableStringBuilder);
    this.name = name;
    this.screenName = screenName;
  }

  private static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);
}
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
import android.os.Build;
import android.support.v7.widget.AppCompatTextView;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;

import twitter4j.User;

/**
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

  public void setNames(User user) {
    final String template = getResources().getString(R.string.tweet_name_screenName);
    final String formatted = String.format(template, user.getName(), user.getScreenName());
    setText(fromHtmlCompat(formatted));
  }

  private static Spanned fromHtmlCompat(String html) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
    } else {
      return Html.fromHtml(html);
    }
  }
}
/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
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
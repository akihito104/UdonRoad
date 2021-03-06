/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad.input;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TextInputEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.freshdigitable.udonroad.CombinedScreenNameTextView;
import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.listitem.TwitterCombinedName;
import com.freshdigitable.udonroad.media.ThumbnailContainer;

import timber.log.Timber;
import twitter4j.User;

/**
 * TweetInputView accepts user input tweet and send it.
 *
 * Created by akihit on 2016/01/17.
 */
public class TweetInputView extends ConstraintLayout {
  @SuppressWarnings("unused")
  private static final String TAG = TweetInputView.class.getSimpleName();
  private final TextInputEditText inputText;
  private final CombinedScreenNameTextView name;
  private final ImageView icon;
  private final TextView counter;
  private final TextView inReplyToMark;
  private final TextView quoteMark;
  private final ImageButton appendImage;
  private final ThumbnailContainer mediaContainer;

  public TweetInputView(Context context) {
    this(context, null);
  }

  public TweetInputView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public TweetInputView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    final View v = View.inflate(context, R.layout.view_tweet_input, this);
    inputText = v.findViewById(R.id.tw_intext);
    name = v.findViewById(R.id.tw_name);
    icon = v.findViewById(R.id.tw_icon);
    counter = v.findViewById(R.id.tw_counter);
    inReplyToMark = v.findViewById(R.id.tw_replyTo);
    quoteMark = v.findViewById(R.id.tw_quote);
    appendImage = v.findViewById(R.id.tw_append_image);
    mediaContainer = v.findViewById(R.id.tw_media_container);
    final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TweetInputView, defStyleAttr, 0);
    try {
      final int textColor = a.getColor(R.styleable.TweetInputView_textColor, -1);
      inputText.setTextColor(textColor);
      name.setTextColor(textColor);
      counter.setTextColor(textColor);
      inReplyToMark.setTextColor(textColor);
      quoteMark.setTextColor(textColor);
    } finally {
      a.recycle();
    }
  }

  public boolean isVisible() {
    return getVisibility() == VISIBLE;
  }

  public void show() {
    setVisibility(VISIBLE);
    inputText.requestFocus();
  }

  public void hide() {
    clearFocus();
    setVisibility(GONE);
  }

  public void setUserInfo(User user) {
    name.setNames(new TwitterCombinedName(user));
  }

  public Editable getText() {
    return inputText.getText();
  }

  public ImageView getIcon() {
    return icon;
  }

  public ImageButton getAppendImageButton() {
    return appendImage;
  }

  public void addTextWatcher(TextWatcher textWatcher) {
    inputText.addTextChangedListener(textWatcher);
  }

  public void removeTextWatcher(TextWatcher textWatcher) {
    inputText.removeTextChangedListener(textWatcher);
  }

  public void setText(String text) {
    if (inputText.getText().toString().equals(text)) {
      return;
    }
    inputText.setText(text);
  }

  public void setInReplyTo() {
    inReplyToMark.setVisibility(VISIBLE);
  }

  public void setQuote() {
    quoteMark.setVisibility(VISIBLE);
  }

  public void reset() {
    inputText.getText().clear();
    inReplyToMark.setVisibility(GONE);
    quoteMark.setVisibility(GONE);
  }

  @SuppressLint("SetTextI18n")
  public void setRemainCount(int remainCount) {
    counter.setText(Integer.toString(remainCount));
  }

  @Override
  protected void onAttachedToWindow() {
    Timber.tag(TAG).d("onAttachedToWindow: ");
    super.onAttachedToWindow();
    final InputMethodManager inputMethodManager
        = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    if (inputMethodManager != null) {
      inputText.setOnFocusChangeListener((v, hasFocus) -> {
        if (hasFocus) {
          inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_FORCED);
        } else {
          inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
      });
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    Timber.tag(TAG).d("onDetachedFromWindow: ");
    super.onDetachedFromWindow();
    inputText.setOnFocusChangeListener(null);
  }

  @Override
  public void setClickable(boolean clickable) {
    super.setClickable(clickable);
    inputText.setClickable(clickable);
    appendImage.setClickable(clickable);
  }

  public ThumbnailContainer getMediaContainer() {
    return mediaContainer;
  }
}

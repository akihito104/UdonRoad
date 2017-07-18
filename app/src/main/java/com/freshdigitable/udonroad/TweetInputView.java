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
import android.support.design.widget.TextInputEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import twitter4j.User;

/**
 * TweetInputView accepts user input tweet and send it.
 *
 * Created by akihit on 2016/01/17.
 */
public class TweetInputView extends RelativeLayout {
  @SuppressWarnings("unused")
  private static final String TAG = TweetInputView.class.getSimpleName();
  private final TextInputEditText inputText;
  private final CombinedScreenNameTextView name;
  private final ImageView icon;
  private final TextView counter;
  private final TextView inReplyToMark;
  private final TextView quoteMark;
  private final ImageButton appendImage;

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
  }

  public boolean isVisible() {
    return getVisibility() == View.VISIBLE;
  }

  public void appearing() {
    setVisibility(View.VISIBLE);
    inputText.requestFocus();
    updateTextCounter(inputText.getText());
  }

  public void disappearing() {
    clearFocus();
    setVisibility(View.GONE);
  }

  public void setUserInfo(User user) {
    name.setNames(user);
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

  public void addText(String text) {
    inputText.append(text);
  }

  public void setInReplyTo() {
    inReplyToMark.setVisibility(VISIBLE);
  }

  public void setQuote() {
    quoteMark.setVisibility(VISIBLE);
  }

  public void reset() {
    inputText.getText().clear();
    inReplyToMark.setVisibility(INVISIBLE);
    quoteMark.setVisibility(INVISIBLE);
    icon.setImageDrawable(null);
  }

  private int shortUrlLength;

  public void setShortUrlLength(int shortUrlLength) {
    this.shortUrlLength = shortUrlLength;
  }

  final TextWatcher textWatcher = new TextWatcher() {
    @Override
    public void afterTextChanged(Editable editable) {
      updateTextCounter(editable);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }
  };

  public void updateTextCounter(Editable editable) {
    int length = editable.length();
    if (quoteMark.getVisibility() == VISIBLE) {
      if (length > 0) {
        length++; // add space
      }
      length += shortUrlLength;
    }
    counter.setText(Integer.toString(140 - length));
  }

  @Override
  protected void onAttachedToWindow() {
    Log.d(TAG, "onAttachedToWindow: ");
    super.onAttachedToWindow();
    final InputMethodManager inputMethodManager
        = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    inputText.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) {
        inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_FORCED);
      } else {
        inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
      }
    });
    inputText.addTextChangedListener(textWatcher);
  }

  @Override
  protected void onDetachedFromWindow() {
    Log.d(TAG, "onDetachedFromWindow: ");
    super.onDetachedFromWindow();
    inputText.setOnFocusChangeListener(null);
    inputText.removeTextChangedListener(textWatcher);
  }
}

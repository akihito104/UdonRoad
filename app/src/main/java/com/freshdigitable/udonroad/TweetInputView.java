package com.freshdigitable.udonroad;

import android.content.Context;
import android.support.design.widget.TextInputEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import twitter4j.User;

/**
 * TweetInputView accepts user input tweet and send it.
 *
 * Created by akihit on 2016/01/17.
 */
public class TweetInputView extends RelativeLayout {
  @SuppressWarnings("unused")
  private static final String TAG = TweetInputView.class.getSimpleName();
  private TextInputEditText inputText;
  private CombinedScreenNameTextView name;
  private ImageView icon;

  public TweetInputView(Context context) {
    this(context, null);
  }

  public TweetInputView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public TweetInputView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    final View v = View.inflate(context, R.layout.view_tweet_input, this);
    inputText = (TextInputEditText) v.findViewById(R.id.tw_intext);
    name = (CombinedScreenNameTextView) v.findViewById(R.id.tw_name);
    icon = (ImageView) v.findViewById(R.id.tw_icon);
  }

  public boolean isVisible() {
    return getVisibility() == View.VISIBLE;
  }

  public void appearing() {
    setVisibility(View.VISIBLE);
    inputText.requestFocus();
  }

  public void disappearing() {
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

  public void addTextWatcher(TextWatcher textWatcher) {
    inputText.addTextChangedListener(textWatcher);
  }

  public void removeTextWatcher(TextWatcher textWatcher) {
    inputText.removeTextChangedListener(textWatcher);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    final InputMethodManager inputMethodManager
        = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    inputText.setOnFocusChangeListener(new OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_FORCED);
        } else {
          inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
      }
    });
  }

  @Override
  protected void onDetachedFromWindow() {
    inputText.setOnFocusChangeListener(null);
    super.onDetachedFromWindow();
  }
}

package com.freshdigitable.udonroad;

import android.content.Context;
import android.support.design.widget.TextInputEditText;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
  private TextInputEditText inputText;
  private TextView name;
  private TextView account;
  private ImageView icon;

  public TweetInputView(Context context) {
    this(context, null);
  }

  public TweetInputView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public TweetInputView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    final View v = View.inflate(context, R.layout.tweet_input_view, this);
    inputText = (TextInputEditText) v.findViewById(R.id.tw_intext);
    final InputMethodManager inputMethodManager
        = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
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

    name = (TextView) v.findViewById(R.id.tw_name);
    account = (TextView) v.findViewById(R.id.tw_account);
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
    name.setText(user.getName());
    account.setText(user.getScreenName());
  }

  public Editable getText() {
    return inputText.getText();
  }

  public ImageView getIcon() {
    return icon;
  }
}

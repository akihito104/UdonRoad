package com.freshdigitable.udonroad;

import android.content.Context;
import android.support.design.widget.TextInputEditText;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import twitter4j.Status;
import twitter4j.User;

/**
 * TweetInputView accepts user input tweet and send it.
 *
 * Created by akihit on 2016/01/17.
 */
public class TweetInputView extends RelativeLayout {
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

    final ImageButton tweetSendButton = (ImageButton) v.findViewById(R.id.tw_send_intweet);
    tweetSendButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        String sendingText = inputText.getText().toString();
        if (sendingText.isEmpty()) {
          return;
        }
        tweetSendButton.setClickable(false);
        onStatusSending.sendStatus(sendingText)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Status>() {
              @Override
              public void onNext(Status status) {
                inputText.getText().clear();
                inputText.clearFocus();
                onStatusSending.onSuccess(status);
                disappearing();
              }

              @Override
              public void onError(Throwable e) {
                onStatusSending.onFailure(e);
              }

              @Override
              public void onCompleted() {
                tweetSendButton.setClickable(true);
              }
            });
      }
    });

    name = (TextView) v.findViewById(R.id.tw_name);
    account = (TextView) v.findViewById(R.id.tw_account);
    icon = (ImageView) v.findViewById(R.id.tw_icon);
  }

  private Observable<User> userObservable;

  public void setUserObservable(Observable<User> observable) {
    this.userObservable = observable;
  }

  private OnStatusSending onStatusSending;

  public boolean isVisible() {
    return getVisibility() == View.VISIBLE;
  }

  public void appearing(OnStatusSending listener) {
    userObservable.observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<User>() {
          @Override
          public void call(User user) {
            name.setText(user.getName());
            account.setText(user.getScreenName());
            Picasso.with(TweetInputView.this.getContext()).load(
                user.getProfileImageURLHttps()).fit().into(icon);
          }
        }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            Log.d(TAG, throwable.getMessage(), throwable);
          }
        });
    setVisibility(View.VISIBLE);
    inputText.requestFocus();
    this.onStatusSending = listener;
  }

  public void disappearing() {
    setVisibility(View.GONE);
    this.onStatusSending = null;
  }

  interface OnStatusSending {
    Observable<Status> sendStatus(String text);

    void onSuccess(Status status);

    void onFailure(Throwable e);
  }
}

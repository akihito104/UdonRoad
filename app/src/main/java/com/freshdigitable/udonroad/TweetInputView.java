package com.freshdigitable.udonroad;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.freshdigitable.udonroad.databinding.TweetInputViewBinding;
import com.squareup.picasso.Picasso;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import twitter4j.Status;
import twitter4j.User;

/**
 * Created by akihit on 2016/01/17.
 */
public class TweetInputView extends RelativeLayout {
  private TweetInputViewBinding binding;

  public TweetInputView(Context context) {
    this(context, null);
  }

  public TweetInputView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public TweetInputView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    if (isInEditMode()) {
      return;
    }
    binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.tweet_input_view, this, true);

    binding.twSendIntweet.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        String sendingText = binding.twIntext.getText().toString();
        if (sendingText.isEmpty()) {
          return;
        }
        binding.twSendIntweet.setClickable(false);
        onStatusSending.sendStatus(sendingText)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Status>() {
              @Override
              public void onNext(Status status) {
                binding.twIntext.getText().clear();
                binding.twIntext.clearFocus();
                onStatusSending.onSuccess(status);
              }

              @Override
              public void onError(Throwable e) {
                onStatusSending.onFailure(e);
              }

              @Override
              public void onCompleted() {
                binding.twSendIntweet.setClickable(true);
              }
            });
      }
    });
  }

  public void setUserInfo(User user) {
    binding.twName.setText(user.getName());
    binding.twAccount.setText(user.getScreenName());
    Picasso.with(this.getContext()).load(user.getProfileImageURLHttps()).fit().into(binding.twIcon);
  }
  private OnStatusSending onStatusSending;

  interface OnStatusSending {
    Observable<Status> sendStatus(String text);

    void onSuccess(Status status);

    void onFailure(Throwable e);
  }

  public boolean isVisible() {
    return getVisibility() == View.VISIBLE;
  }

  public void appearing(OnStatusSending listener) {
    setVisibility(View.VISIBLE);
    binding.twIntext.requestFocus();
    this.onStatusSending = listener;
  }

  public void disappearing() {
    setVisibility(View.GONE);
    this.onStatusSending = null;
  }

  public void setOnInputFieldFocusChangeListener(OnFocusChangeListener listener) {
    binding.twIntext.setOnFocusChangeListener(listener);
  }
}

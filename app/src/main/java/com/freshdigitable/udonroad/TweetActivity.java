package com.freshdigitable.udonroad;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

@EActivity(R.layout.activity_tweet)
public class TweetActivity extends AppCompatActivity {
  private static final String TAG = TweetActivity.class.getName();

  @ViewById(R.id.tw_tweet)
  protected Button tweet;

  @ViewById(R.id.tw_text)
  protected TextView text;

  @Click(R.id.tw_tweet)
  protected void tweetClicked() {
    tweet.setEnabled(false);
    startUpdatingStatus();
  }

  @Background
  protected void startUpdatingStatus() {
    Twitter twitter = AccessUtil.getTwitterInstance(this);
    try {
      twitter.updateStatus(text.getText().toString());
      dismissActivity();
    } catch (TwitterException e) {
      Log.e(TAG, "update: ", e);
    }
  }

  @UiThread
  protected void dismissActivity() {
    finish();
  }
}

package com.freshdigitable.udonroad;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class TweetActivity extends AppCompatActivity {
  private static final String TAG = TweetActivity.class.getName();

  private Button tweet;
  private TextView text;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_tweet);

    text = (TextView) findViewById(R.id.tw_text);
    tweet = ((Button) findViewById(R.id.tw_tweet));
    tweet.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        tweetClicked();
      }
    });
  }

  private void tweetClicked() {
    final String tweetText = text.getText().toString();
    if (tweetText.length() <= 0) {
      return;
    }
    tweet.setEnabled(false);
    Observable.create(
        new Observable.OnSubscribe<Status>() {
          @Override
          public void call(Subscriber<? super Status> subscriber) {
            Twitter twitter = TwitterApi.getTwitterInstance(TweetActivity.this);
            try {
              subscriber.onNext(twitter.updateStatus(tweetText));
            } catch (TwitterException e) {
              subscriber.onError(e);
            }
            subscriber.onCompleted();
          }
        }
    )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<Status>() {
          @Override
          public void onNext(Status status) {
          }

          @Override
          public void onError(Throwable e) {
            Log.e(TAG, "onUpdateStatus: ", e);
            tweet.setEnabled(true);
          }

          @Override
          public void onCompleted() {
            finish();
          }
        });
  }
}

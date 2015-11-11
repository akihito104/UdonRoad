package com.freshdigitable.udonroad;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import twitter4j.Twitter;
import twitter4j.TwitterStream;

public class TweetCacheService extends Service {
  public TweetCacheService() {
  }

  private Twitter twitterInstance;
  private TwitterStream twitterStreamInstance;

  @Override
  public void onCreate() {
    super.onCreate();
    twitterInstance = AccessUtil.getTwitterInstance(this);
    twitterStreamInstance = AccessUtil.getTwitterStreamInstance(this);
  }

  @Override
  public IBinder onBind(Intent intent) {
    // TODO: Return the communication channel to the service.
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    twitterStreamInstance.user();
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public void onDestroy() {
    twitterStreamInstance.shutdown();
    super.onDestroy();
  }
}

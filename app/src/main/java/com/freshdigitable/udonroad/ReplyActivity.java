/*
 * Copyright (c) 2016. Akihito Matsuda (akihito104)
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

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;

import com.freshdigitable.udonroad.databinding.ActivityReplyBinding;
import com.freshdigitable.udonroad.datastore.StatusCache;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Inject;

import twitter4j.Status;

/**
 * Created by akihit on 2016/07/27.
 */
public class ReplyActivity extends AppCompatActivity {
  public static final String TRANSITION_NAME = "replied_status";
  public static final String EXTRA_STATUS_ID = "statusId";
  public static final String EXTRA_TWEET_TYPE = "tweet_type";
  private ActivityReplyBinding binding;
  @Inject
  StatusCache statusCache;
  private TweetAppbarFragment tweetAppbar;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
    }
    binding = DataBindingUtil.setContentView(this, R.layout.activity_reply);
    ViewCompat.setTransitionName(binding.replyStatus, TRANSITION_NAME);

    InjectionUtil.getComponent(this).inject(this);
    final long statusId = getStatusId();
    if (statusId < 0) {
      finish();
    }
  }

  private long getStatusId() {
    return getIntent().getLongExtra(EXTRA_STATUS_ID, -1);
  }

  private int getTweetType() {
    return getIntent().getIntExtra(EXTRA_TWEET_TYPE, TYPE_REPLY);
  }

  @Override
  protected void onStart() {
    super.onStart();

    final long statusId = getStatusId();
    statusCache.open(getApplicationContext());
    final Status status = statusCache.getStatus(statusId);
    binding.replyStatus.bindStatus(status);

    tweetAppbar = (TweetAppbarFragment) getSupportFragmentManager().findFragmentById(R.id.reply_input);
    tweetAppbar.setTweetSendFab(binding.replySendTweet);
    final int tweetType = getTweetType();
    if (tweetType == TYPE_REPLY) {
      tweetAppbar.stretchTweetInputViewWithInReplyTo(new TweetAppbarFragment.OnStatusSending() {
        @Override
        public void onSuccess(Status status) {
          // todo
        }

        @Override
        public void onFailure(Throwable e) {
          // todo
        }
      }, status);
    } else {
      tweetAppbar.stretchTweetInputViewWithQuoteStatus(new TweetAppbarFragment.OnStatusSending() {
        @Override
        public void onSuccess(Status status) {

        }

        @Override
        public void onFailure(Throwable e) {

        }
      }, statusId);
    }
  }

  @Override
  protected void onStop() {
    statusCache.close();
    tweetAppbar.collapseStatusInputView();
    tweetAppbar.setTweetSendFab(null);
    super.onStop();
  }

  public static void start(Activity activity, long statusId, View view) {
    start(activity, statusId, TYPE_REPLY, view);
  }

  public static void start(Activity activity, long statusId, @TweetType int type, View view) {
    ViewCompat.setTransitionName(view, TRANSITION_NAME);
    final Intent intent = new Intent(activity.getApplicationContext(), ReplyActivity.class);
    intent.putExtra(EXTRA_STATUS_ID, statusId);
    intent.putExtra(EXTRA_TWEET_TYPE, type);
    ActivityCompat.startActivity(activity, intent,
        ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, TRANSITION_NAME).toBundle());
  }

  public static final int TYPE_REPLY = 1;
  public static final int TYPE_QUOTE = 2;

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {TYPE_REPLY, TYPE_QUOTE})
  public @interface TweetType {
  }
}


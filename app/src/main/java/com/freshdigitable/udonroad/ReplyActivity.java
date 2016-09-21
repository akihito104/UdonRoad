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

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;

import com.freshdigitable.udonroad.TweetInputFragment.TweetType;
import com.freshdigitable.udonroad.databinding.ActivityReplyBinding;
import com.freshdigitable.udonroad.datastore.TypedCache;

import javax.inject.Inject;

import twitter4j.Status;

import static com.freshdigitable.udonroad.TweetInputFragment.TYPE_REPLY;

/**
 * ReplyActivity provides to input reply or quote tweet. It is started by the context does not have
 * TweetInputView or its Fragment.
 *
 * Created by akihit on 2016/07/27.
 */
public class ReplyActivity extends AppCompatActivity {
  public static final String TRANSITION_NAME = "replied_status";
  public static final String EXTRA_STATUS_ID = "statusId";
  public static final String EXTRA_TWEET_TYPE = "tweet_type";
  private ActivityReplyBinding binding;
  @Inject
  TypedCache<Status> statusCache;
  private TweetInputFragment tweetInputFragment;

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
    final Status status = statusCache.find(statusId);
    binding.replyStatus.bindStatus(status);

    tweetInputFragment = (TweetInputFragment) getSupportFragmentManager().findFragmentById(R.id.reply_input);
    tweetInputFragment.setTweetSendFab(binding.replySendTweet);
    final @TweetType int tweetType = getTweetType();
    tweetInputFragment.stretchTweetInputView(tweetType, statusId);
  }

  @Override
  protected void onStop() {
    statusCache.close();
    tweetInputFragment.collapseStatusInputView();
    tweetInputFragment.setTweetSendFab(null);
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
}


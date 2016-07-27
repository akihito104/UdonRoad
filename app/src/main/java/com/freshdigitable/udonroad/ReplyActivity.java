/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
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
import android.view.Window;

import com.freshdigitable.udonroad.databinding.ActivityReplyBinding;
import com.freshdigitable.udonroad.datastore.StatusCache;

import javax.inject.Inject;

import twitter4j.Status;

/**
 * Created by akihit on 2016/07/27.
 */
public class ReplyActivity extends AppCompatActivity {

  public static final String TRANSITION_NAME = "replied_status";
  public static final String EXTRA_STATUS_ID = "statusId";
  private ActivityReplyBinding binding;
  @Inject
  StatusCache statusCache;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
    }
    binding = DataBindingUtil.setContentView(this, R.layout.activity_reply);
    ViewCompat.setTransitionName(binding.replyStatus, TRANSITION_NAME);

    InjectionUtil.getComponent(this).inject(this);
    final long statusId = getIntent().getLongExtra(EXTRA_STATUS_ID, -1);
    if (statusId < 0) {
      finish();
    }
    final Status status = statusCache.getStatus(statusId);
    binding.replyStatus.bindStatus(status);
  }

  public static void start(Activity activity, Status status, StatusView view) {
    ViewCompat.setTransitionName(view, TRANSITION_NAME);
    final Intent intent = new Intent(activity.getApplicationContext(), ReplyActivity.class);
    intent.putExtra(EXTRA_STATUS_ID, status.getId());
    ActivityCompat.startActivity(activity, intent,
        ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, TRANSITION_NAME).toBundle());
  }
}

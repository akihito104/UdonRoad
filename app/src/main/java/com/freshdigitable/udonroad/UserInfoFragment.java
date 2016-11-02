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

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.databinding.FragmentUserInfoBinding;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;
import com.freshdigitable.udonroad.subscriber.ConfigSubscriber;
import com.freshdigitable.udonroad.subscriber.FeedbackAction;
import com.freshdigitable.udonroad.subscriber.UserFeedbackSubscriber;
import com.freshdigitable.udonroad.subscriber.UserSubscriber;
import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import twitter4j.Relationship;
import twitter4j.User;

/**
 * UserInfoFragment wraps UserInfoView.
 *
 * Created by akihit on 2016/02/07.
 */
public class UserInfoFragment extends Fragment {
  private FragmentUserInfoBinding binding;
  @Inject
  TypedCache<User> userCache;
  private Subscription subscription;
  @Inject
  TwitterApi twitterApi;
  @Inject
  UserFeedbackSubscriber userFeedback;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    InjectionUtil.getComponent(this).inject(this);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_info, container, false);
    return binding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
  }

  @Override
  public void onStart() {
    super.onStart();
    userCache.open();

    final long userId = getUserId();
    subscription = userCache.observeById(userId)
        .subscribe(new Action1<User>() {
          @Override
          public void call(User user) {
            showUserInfo(user);
          }
        });
    twitterApi.showFriendship(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<Relationship>() {
          @Override
          public void call(Relationship relationship) {
            binding.userInfoUserInfoView.bindRelationship(relationship);
          }
        }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            Log.e("UserInfoFragment", "call: ", throwable);
          }
        });

//    final FeedbackAction feedbackAction = getFeedbackAction();
    userSubscriber = new UserSubscriber<>(twitterApi, userCache, userFeedback);
    configSubscriber.setFeedbackSubscriber(userFeedback);
  }

  @Override
  public void onStop() {
    if (subscription != null && !subscription.isUnsubscribed()) {
      subscription.unsubscribe();
    }
    dismissUserInfo();
    userCache.close();
    super.onStop();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.user_info, menu);
  }

  private UserSubscriber<TypedCache<User>> userSubscriber;
  @Inject
  ConfigSubscriber configSubscriber;

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final int itemId = item.getItemId();
    final long userId = getUserId();
    if (itemId == R.id.action_follow) {
      userSubscriber.createFriendship(userId);
    } else if (itemId == R.id.action_remove) {
      userSubscriber.destroyFriendship(userId);
    } else if (itemId == R.id.action_block) {
      configSubscriber.createBlock(userId);
    } else if (itemId == R.id.action_block_retweet) {
      // todo
    } else if (itemId == R.id.action_mute) {
      configSubscriber.createMute(userId);
    } else if (itemId == R.id.action_r4s) {
      configSubscriber.reportSpam(userId);
    }
    return false;
  }

  private void showUserInfo(User user) {
    Picasso.with(getContext())
        .load(user.getProfileImageURLHttps())
        .into(binding.userInfoUserInfoView.getIcon());
    Picasso.with(getContext())
        .load(user.getProfileBannerMobileURL())
        .fit()
        .into(binding.userInfoUserInfoView.getBanner());
    binding.userInfoUserInfoView.bindData(user);
  }

  private void dismissUserInfo() {
    Picasso.with(getContext())
        .cancelRequest(binding.userInfoUserInfoView.getBanner());
    Picasso.with(getContext())
        .cancelRequest(binding.userInfoUserInfoView.getIcon());
  }

  public static UserInfoFragment create(long userId) {
    final UserInfoFragment userInfoAppbarFragment = new UserInfoFragment();
    final Bundle args = new Bundle();
    args.putLong("userId", userId);
    userInfoAppbarFragment.setArguments(args);
    return userInfoAppbarFragment;
  }

  private long getUserId() {
    final Bundle arguments = getArguments();
    return arguments.getLong("userId");
  }

  private FeedbackAction getFeedbackAction() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof FeedbackAction) {
      return (FeedbackAction) activity;
    } else {
      return new FeedbackAction.ToastFeedback(getContext());
    }
  }
}

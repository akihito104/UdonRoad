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
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.databinding.FragmentUserInfoBinding;
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.subscriber.ConfigRequestWorker;
import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import twitter4j.Relationship;
import twitter4j.User;

/**
 * UserInfoFragment wraps UserInfoView.
 *
 * Created by akihit on 2016/02/07.
 */
public class UserInfoFragment extends Fragment {
  private static final String LOADINGTAG_USER_INFO_IMAGES = "UserInfoImages";
  private FragmentUserInfoBinding binding;
  private CompositeDisposable subscription;

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
    if (binding == null) {
      binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_info, container, false);
      ViewCompat.setTransitionName(binding.userInfoUserInfoView.getIcon(), UserInfoActivity.getUserIconTransitionName(getUserId()));
    }
    return binding.getRoot();
  }

  @Inject
  TypedCache<User> userCache;
  @Inject
  ConfigStore configStore;

  void onEnterAnimationComplete() {
    configRequestWorker.fetchRelationship(getUserId());
  }

  @Override
  public void onStart() {
    super.onStart();

    final long userId = getUserId();
    userCache.open();
    configStore.open();
    final User user = userCache.find(userId);
    showUserInfo(user);
    subscription = new CompositeDisposable();
    subscription.add(userCache.observeById(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::showUserInfo,
            e -> Log.e("UserInfoFragment", "userUpdated: ", e)));
    subscription.add(configStore.observeRelationshipById(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::setRelationship));
  }

  @Override
  public void onStop() {
    super.onStop();
    if (subscription != null && !subscription.isDisposed()) {
      subscription.dispose();
    }
    userCache.close();
    configStore.close();
  }

  @Override
  public void onDetach() {
    super.onDetach();
    dismissUserInfo();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.user_info, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    if (relationship != null) {
      switchVisibility(relationship.isSourceFollowingTarget(),
          R.id.action_unfollow, R.id.action_follow, menu);
      switchVisibility(relationship.isSourceBlockingTarget(),
          R.id.action_unblock, R.id.action_block, menu);
      switchVisibility(relationship.isSourceMutingTarget(),
          R.id.action_unmute, R.id.action_mute, menu);
      if (relationship.isSourceFollowingTarget()) {
        switchVisibility(relationship.isSourceWantRetweets(),
            R.id.action_block_retweet, R.id.action_unblock_retweet, menu);
      } else {
        menu.findItem(R.id.action_block_retweet).setVisible(false);
        menu.findItem(R.id.action_unblock_retweet).setVisible(false);
      }
    }
  }

  private static void switchVisibility(boolean cond,
                                       @IdRes int actionOnIfTrue, @IdRes int actionOffIfFalse,
                                       @NonNull Menu menu) {
    menu.findItem(actionOnIfTrue).setVisible(cond);
    menu.findItem(actionOffIfFalse).setVisible(!cond);
  }

  @Inject
  ConfigRequestWorker configRequestWorker;

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final int itemId = item.getItemId();
    final long userId = getUserId();
    if (itemId == R.id.action_follow) {
      configRequestWorker.fetchCreateFriendship(userId);
    } else if (itemId == R.id.action_unfollow) {
      configRequestWorker.fetchDestroyFriendship(userId);
    } else if (itemId == R.id.action_block) {
      configRequestWorker.fetchCreateBlock(userId);
    } else if (itemId == R.id.action_unblock) {
      configRequestWorker.fetchDestroyBlock(userId);
    } else if (itemId == R.id.action_block_retweet) {
      configRequestWorker.fetchBlockRetweet(relationship);
    } else if (itemId == R.id.action_unblock_retweet) {
      configRequestWorker.fetchUnblockRetweet(relationship);
    } else if (itemId == R.id.action_mute) {
      configRequestWorker.fetchCreateMute(userId);
    } else if (itemId == R.id.action_unmute) {
      configRequestWorker.fetchDestroyMute(userId);
    } else if (itemId == R.id.action_r4s) {
      configRequestWorker.reportSpam(userId);
    }
    return false;
  }

  private Relationship relationship;

  private void setRelationship(Relationship relationship) {
    this.relationship = relationship;
    binding.userInfoUserInfoView.bindRelationship(relationship);
    getActivity().invalidateOptionsMenu();
  }

  private void showUserInfo(User user) {
    if (user == null) {
      return;
    }
    Log.d("UserInfoFragment", "showUserInfo: ");
    loadUserIcon(user.getProfileImageURLHttps());
    loadBanner(user.getProfileBannerMobileURL());
    binding.userInfoUserInfoView.bindData(user);
  }

  private String userIconUrl = "";
  private void loadUserIcon(String url) {
    if (url == null || userIconUrl.equals(url)) {
      return;
    }
    Log.d("UserInfoFragment", "loadUserIcon: ");
    this.userIconUrl = url;
    Picasso.with(getContext())
        .load(url)
        .tag(LOADINGTAG_USER_INFO_IMAGES)
        .into(binding.userInfoUserInfoView.getIcon());
  }

  private String bannerUrl = "";
  private void loadBanner(String url) {
    if (url == null || bannerUrl.equals(url)) {
      return;
    }
    Log.d("UserInfoFragment", "loadBanner: ");
    this.bannerUrl = url;
    Picasso.with(getContext())
        .load(url)
        .fit()
        .tag(LOADINGTAG_USER_INFO_IMAGES)
        .into(binding.userInfoUserInfoView.getBanner());
  }

  private void dismissUserInfo() {
    Picasso.with(getContext()).cancelTag(LOADINGTAG_USER_INFO_IMAGES);
    binding.userInfoUserInfoView.getBanner().setImageDrawable(null);
    binding.userInfoUserInfoView.getIcon().setImageDrawable(null);
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
}

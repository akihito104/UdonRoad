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
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.subscriber.ConfigRequestWorker;
import com.freshdigitable.udonroad.subscriber.UserRequestWorker;
import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import twitter4j.RateLimitStatus;
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
  private Disposable subscription;

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

  void onEnterAnimationComplete() {
    configRequestWorker.observeFetchRelationship(getUserId())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(updateRelationship(), e -> {});
  }

  @Override
  public void onStart() {
    super.onStart();

    final long userId = getUserId();
    userCache.open();
    final User user = userCache.find(userId);
    showUserInfo(user);
    subscription = userCache.observeById(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::showUserInfo,
            e-> Log.e("UserInfoFragment", "userUpdated: ", e));
  }

  @Override
  public void onStop() {
    super.onStop();
    if (subscription != null && !subscription.isDisposed()) {
      subscription.dispose();
    }
    userCache.close();
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
  UserRequestWorker userRequestWorker;
  @Inject
  ConfigRequestWorker configRequestWorker;

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final int itemId = item.getItemId();
    final long userId = getUserId();
    if (itemId == R.id.action_follow) {
      userRequestWorker.observeCreateFriendship(userId)
          .subscribe(updateFollowing(true), e -> {});
    } else if (itemId == R.id.action_unfollow) {
      userRequestWorker.observeDestroyFriendship(userId)
          .subscribe(updateFollowing(false), e -> {});
    } else if (itemId == R.id.action_block) {
      configRequestWorker.observeCreateBlock(userId)
          .subscribe(updateBlocking(true), e -> {});
    } else if (itemId == R.id.action_unblock) {
      configRequestWorker.observeDestroyBlock(userId)
          .subscribe(updateBlocking(false), e -> {});
    } else if (itemId == R.id.action_block_retweet) {
      configRequestWorker.observeBlockRetweet(relationship)
          .subscribe(updateRelationship(), e -> {});
    } else if (itemId == R.id.action_unblock_retweet) {
      configRequestWorker.observeUnblockRetweet(relationship)
          .subscribe(updateRelationship(), e -> {});
    } else if (itemId == R.id.action_mute) {
      configRequestWorker.observeCreateMute(userId)
          .subscribe(updateMuting(true), e -> {});
    } else if (itemId == R.id.action_unmute) {
      configRequestWorker.observeDestroyMute(userId)
          .subscribe(updateMuting(false), e -> {});
    } else if (itemId == R.id.action_r4s) {
      configRequestWorker.reportSpam(userId);
    }
    return false;
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

  private RelationshipImpl relationship;

  private void setRelationship(RelationshipImpl relationship) {
    this.relationship = relationship;
    notifyRelationshipChanged();
  }

  private void notifyRelationshipChanged() {
    binding.userInfoUserInfoView.bindRelationship(relationship);
    getActivity().invalidateOptionsMenu();
  }

  @NonNull
  private Consumer<User> updateFollowing(final boolean following) {
    return u -> {
      relationship.setFollowing(following);
      notifyRelationshipChanged();
    };
  }

  @NonNull
  private Consumer<User> updateBlocking(final boolean blocking) {
    return u -> {
      relationship.setBlocking(blocking);
      notifyRelationshipChanged();
    };
  }

  @NonNull
  private Consumer<User> updateMuting(final boolean muting) {
    return u -> {
      relationship.setMuting(muting);
      notifyRelationshipChanged();
    };
  }

  private Consumer<Relationship> updateRelationship() {
    return relationship1 -> setRelationship(new RelationshipImpl(relationship1));
  }

  private static class RelationshipImpl implements Relationship {
    private final Relationship relationship;

    RelationshipImpl(Relationship relationship) {
      this.relationship = relationship;
      this.following = relationship.isSourceFollowingTarget();
      this.blocking = relationship.isSourceBlockingTarget();
      this.muting = relationship.isSourceMutingTarget();
    }
    private boolean blocking;

    @Override
    public boolean isSourceBlockingTarget() {
      return blocking;
    }

    void setBlocking(boolean blocking) {
      this.blocking = blocking;
    }

    private boolean muting;

    @Override
    public boolean isSourceMutingTarget() {
      return muting;
    }

    void setMuting(boolean muting) {
      this.muting = muting;
    }

    private boolean following;

    @Override
    public boolean isSourceFollowingTarget() {
      return following;
    }

    void setFollowing(boolean following) {
      this.following = following;
    }

    @Override
    public long getSourceUserId() {
      return relationship.getSourceUserId();
    }

    @Override
    public long getTargetUserId() {
      return relationship.getTargetUserId();
    }

    @Override
    public String getSourceUserScreenName() {
      return relationship.getSourceUserScreenName();
    }

    @Override
    public String getTargetUserScreenName() {
      return relationship.getTargetUserScreenName();
    }

    @Override
    public boolean isTargetFollowingSource() {
      return relationship.isTargetFollowingSource();
    }

    @Override
    public boolean isSourceFollowedByTarget() {
      return relationship.isSourceFollowedByTarget();
    }

    @Override
    public boolean isTargetFollowedBySource() {
      return following;
    }

    @Override
    public boolean canSourceDm() {
      return relationship.canSourceDm();
    }

    @Override
    public boolean isSourceNotificationsEnabled() {
      return relationship.isSourceNotificationsEnabled();
    }

    @Override
    public boolean isSourceWantRetweets() {
      return relationship.isSourceWantRetweets();
    }

    @Override
    public RateLimitStatus getRateLimitStatus() {
      return relationship.getRateLimitStatus();
    }

    @Override
    public int getAccessLevel() {
      return relationship.getAccessLevel();
    }
  }
}

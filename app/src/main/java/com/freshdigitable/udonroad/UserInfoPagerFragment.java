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
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.subscriber.RequestWorkerBase;
import com.freshdigitable.udonroad.subscriber.StatusRequestWorker;
import com.freshdigitable.udonroad.subscriber.UserFeedbackSubscriber;
import com.freshdigitable.udonroad.subscriber.UserRequestWorker;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import rx.Observable;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.User;

/**
 * UserInfoPagerFragment provides ViewPager to show specified user tweets.
 *
 * Created by akihit on 2016/06/06.
 */
public class UserInfoPagerFragment extends Fragment {
  private static final String TAG = UserInfoPagerFragment.class.getSimpleName();
  public static final String ARGS_USER_ID = "userId";

  public static UserInfoPagerFragment create(long userId) {
    final Bundle args = new Bundle();
    args.putLong(ARGS_USER_ID, userId);
    final UserInfoPagerFragment res = new UserInfoPagerFragment();
    res.setArguments(args);
    return res;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    InjectionUtil.getComponent(this).inject(this);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_user_info_pager, container, false);
  }

  private ViewPager viewPager;

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    final View v = getView();
    if (v == null) {
      return;
    }
    viewPager = (ViewPager) v.findViewById(R.id.user_pager);
  }

  private PagerAdapter pagerAdapter;

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    pagerAdapter = new PagerAdapter(getChildFragmentManager());
    setupTimeline(UserPageInfo.TWEET, userHomeRequestWorker);
    setupUserList(UserPageInfo.FOLLOWER, userFollowersRequestWorker);
    setupUserList(UserPageInfo.FRIEND, userFriendsRequestWorker);
    setupTimeline(UserPageInfo.FAV, userFavRequestWorker);
    viewPager.setAdapter(pagerAdapter);
  }

  private Map<UserPageInfo, StatusRequestWorker<SortedCache<Status>>> timelineSubscriberMap = new HashMap<>();
  private Map<UserPageInfo, UserRequestWorker<SortedCache<User>>> userSubscriberMap = new HashMap<>();

  @Inject
  StatusRequestWorker<SortedCache<Status>> userHomeRequestWorker;
  @Inject
  StatusRequestWorker<SortedCache<Status>> userFavRequestWorker;
  @Inject
  UserRequestWorker<SortedCache<User>> userFollowersRequestWorker;
  @Inject
  UserRequestWorker<SortedCache<User>> userFriendsRequestWorker;

  private void setupTimeline(@NonNull UserPageInfo page,
                             @NonNull StatusRequestWorker<SortedCache<Status>> requestWorker) {
    if (!page.isStatus()) {
      throw new IllegalArgumentException("page must be for Status. passed: " + page.name());
    }
    timelineSubscriberMap.put(page, requestWorker);
    putToPagerAdapter(page, requestWorker);
  }

  private void setupUserList(@NonNull UserPageInfo page,
                             @NonNull UserRequestWorker<SortedCache<User>> requestWorker) {
    if (!page.isUser()) {
      throw new IllegalArgumentException("page must be for User. passed: " + page.name());
    }
    userSubscriberMap.put(page, requestWorker);
    putToPagerAdapter(page, requestWorker);
  }

  private <T> void putToPagerAdapter(@NonNull UserPageInfo page,
                                 RequestWorkerBase<SortedCache<T>> worker) {
    final TimelineFragment<T> fragment = page.setup(worker, this);
    pagerAdapter.putFragment(page, fragment);
  }

  @Inject
  UserFeedbackSubscriber userFeedback;

  @Override
  public void onStart() {
    super.onStart();
    final FragmentActivity activity = getActivity();
    if (activity instanceof FabHandleable) {
      viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
          Log.d(TAG, "onPageSelected: " + position);
          TimelineFragment fragment = pagerAdapter.getItem(position);
          if (fragment.isTweetSelected()) {
            ((FabHandleable) activity).showFab();
          } else {
            ((FabHandleable) activity).hideFab();
          }
        }
      });
    }
    userFeedback.registerRootView(viewPager);
  }

  private TimelineFragment getCurrentFragment() {
    final int currentItem = viewPager.getCurrentItem();
    return pagerAdapter.getItem(currentItem);
  }

  @Override
  public void onStop() {
    super.onStop();
    viewPager.clearOnPageChangeListeners();
    userFeedback.unregisterRootView(viewPager);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    viewPager.setAdapter(null);
    for (StatusRequestWorker ts : timelineSubscriberMap.values()) {
      ts.close();
    }
    timelineSubscriberMap.clear();
    for (UserRequestWorker us : userSubscriberMap.values()) {
      us.close();
    }
    userSubscriberMap.clear();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    final UserPageInfo reqPage = UserPageInfo.findByRequestCode(requestCode);
    if (reqPage == null) {
      super.onActivityResult(requestCode, resultCode, data);
      return;
    }
    final long userId = getArguments().getLong(ARGS_USER_ID);
    final Paging paging = (Paging) data.getSerializableExtra(TimelineFragment.EXTRA_PAGING);
    if (reqPage.isStatus()) {
      final StatusRequestWorker<SortedCache<Status>> statusRequestWorker = timelineSubscriberMap.get(reqPage);
      if (reqPage == UserPageInfo.TWEET) {
        statusRequestWorker.fetchHomeTimeline(userId, paging);
      } else if (reqPage == UserPageInfo.FAV) {
        statusRequestWorker.fetchFavorites(userId, paging);
      }
    } else if (reqPage.isUser()) {
      final UserRequestWorker<SortedCache<User>> userRequestWorker = userSubscriberMap.get(reqPage);
      final long nextCursor = paging != null
          ? paging.getMaxId()
          : -1;
      if (reqPage == UserPageInfo.FOLLOWER) {
        userRequestWorker.fetchFollowers(userId, nextCursor);
      } else if (reqPage == UserPageInfo.FRIEND) {
        userRequestWorker.fetchFriends(userId, nextCursor);
      }
    }
  }

  public ViewPager getViewPager() {
    return viewPager;
  }

  public void clearSelectedTweet() {
    final TimelineFragment currentFragment = getCurrentFragment();
    currentFragment.clearSelectedTweet();
  }

  private static class PagerAdapter extends FragmentPagerAdapter {
    private PagerAdapter(FragmentManager fm) {
      super(fm);
    }

    private Map<UserPageInfo, TimelineFragment> pages = new HashMap<>();

    private void putFragment(UserPageInfo page, TimelineFragment<?> fragment) {
      pages.put(page, fragment);
    }

    @Override
    public TimelineFragment getItem(int position) {
      return pages.get(UserPageInfo.values()[position]);
    }

    @Override
    public int getCount() {
      return pages.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return UserPageInfo.values()[position].name();
    }
  }

  long getCurrentSelectedStatusId() {
    final UserPageInfo currentPage = getCurrentPage();
    if (!currentPage.isStatus()) {
      return -1;
    }
    return getCurrentFragment().getSelectedTweetId();
  }

  void createFavorite() {
    final UserPageInfo currentPage = getCurrentPage();
    if (!currentPage.isStatus()) {
      return;
    }
    final long statusId = getCurrentFragment().getSelectedTweetId();
    if (statusId < 0) {
      return;
    }
    final StatusRequestWorker<SortedCache<Status>> statusRequestWorker
        = timelineSubscriberMap.get(currentPage);
    if (statusRequestWorker != null) {
      statusRequestWorker.createFavorite(statusId);
    }
  }

  void retweetStatus() {
    final UserPageInfo currentPage = getCurrentPage();
    if (!currentPage.isStatus()) {
      return;
    }
    final long statusId = getCurrentFragment().getSelectedTweetId();
    if (statusId < 0) {
      return;
    }
    final StatusRequestWorker<SortedCache<Status>> statusRequestWorker
        = timelineSubscriberMap.get(currentPage);
    if (statusRequestWorker != null) {
      statusRequestWorker.retweetStatus(statusId);
    }
  }

  void createFavAndRetweet() {
    final UserPageInfo currentPage = getCurrentPage();
    if (!currentPage.isStatus()) {
      return;
    }
    final long statusId = getCurrentFragment().getSelectedTweetId();
    if (statusId < 0) {
      return;
    }
    final StatusRequestWorker<SortedCache<Status>> statusRequestWorker = timelineSubscriberMap.get(currentPage);
    if (statusRequestWorker != null) {
      Observable.concatDelayError(Arrays.asList(
          statusRequestWorker.observeCreateFavorite(statusId),
          statusRequestWorker.observeRetweetStatus(statusId))
      ).subscribe(RequestWorkerBase.<Status>nopSubscriber());
    }
  }

  @NonNull
  UserPageInfo getCurrentPage() {
    final int currentItem = viewPager.getCurrentItem();
    return UserPageInfo.values()[currentItem];
  }

  public static final int REQUEST_CODE_USER_HOME = 10;
  public static final int REQUEST_CODE_USER_FAVS = 11;
  public static final int REQUEST_CODE_USER_FOLLOWERS = 12;
  public static final int REQUEST_CODE_USER_FRIENDS = 13;

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {REQUEST_CODE_USER_HOME, REQUEST_CODE_USER_FAVS,
      REQUEST_CODE_USER_FOLLOWERS, REQUEST_CODE_USER_FRIENDS})
  @interface RequestCodeEnum {
  }

  enum UserPageInfo {
    TWEET(REQUEST_CODE_USER_HOME, "user_home") {
      @Override
      public String createTitle(User user) {
        return name() + "\n" + user.getStatusesCount();
      }
    },
    FRIEND(REQUEST_CODE_USER_FRIENDS, "user_friends") {
      @Override
      public String createTitle(User user) {
        return name() + "\n" + user.getFriendsCount();
      }
    },
    FOLLOWER(REQUEST_CODE_USER_FOLLOWERS, "user_followers") {
      @Override
      public String createTitle(User user) {
        return name() + "\n" + user.getFollowersCount();
      }
    },
    FAV(REQUEST_CODE_USER_FAVS, "user_favs") {
      @Override
      public String createTitle(User user) {
        return name() + "\n" + user.getFavouritesCount();
      }
    },;

    final @RequestCodeEnum int requestCode;
    final String cacheName;

    UserPageInfo(@RequestCodeEnum int requestCode, String cacheName) {
      this.requestCode = requestCode;
      this.cacheName = cacheName;
    }

    <T> TimelineFragment<T> setup(RequestWorkerBase<SortedCache<T>> worker,
                                  UserInfoPagerFragment target) {
      worker.open(cacheName);
      worker.getCache().clear();
      final TimelineFragment<T> fragment = TimelineFragment.getInstance(target, requestCode);
      fragment.setSortedCache(worker.getCache());
      return fragment;
    }

    @Nullable
    static UserPageInfo findByRequestCode(int requestCode) {
      for (UserPageInfo p : values()) {
        if (p.requestCode == requestCode) {
          return p;
        }
      }
      return null;
    }

    public boolean isStatus() {
      return this == TWEET || this == FAV;
    }

    public boolean isUser() {
      return this == FOLLOWER || this == FRIEND;
    }

    public abstract String createTitle(User user);
  }
}

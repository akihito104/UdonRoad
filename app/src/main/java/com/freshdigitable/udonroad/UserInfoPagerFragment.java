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
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.FeedbackSubscriber.SnackbarFeedback;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.datastore.TypedCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

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
  public static final int REQUEST_CODE_USER_HOME = 10;
  public static final int REQUEST_CODE_USER_FAVS = 11;
  @Inject
  TwitterApi twitterApi;
  @Inject
  SortedCache<Status> userHomeTimeline;
  @Inject
  SortedCache<Status> userFavTimeline;
  private Map<Integer, TimelineSubscriber<SortedCache<Status>>> timelineSubscriberMap = new HashMap<>();
  @Inject
  TypedCache<User> userCache;

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
    final SnackbarFeedback userFeedback = new SnackbarFeedback(viewPager);
    timelineSubscriberMap.put(REQUEST_CODE_USER_HOME,
        new TimelineSubscriber<>(twitterApi, userHomeTimeline, userFeedback));
    timelineSubscriberMap.put(REQUEST_CODE_USER_FAVS,
        new TimelineSubscriber<>(twitterApi, userFavTimeline, userFeedback));
    userCache.open(getContext());
    final User user = userCache.find(userId);
    if (user == null) {//XXX
      return;
    }

    pagerAdapter = new PagerAdapter(getChildFragmentManager());

    final TimelineFragment home = TimelineFragment.getInstance(this, REQUEST_CODE_USER_HOME);
    userHomeTimeline.open(getContext(), "user_home");
    userHomeTimeline.clear();
    home.setSortedCache(timelineSubscriberMap.get(REQUEST_CODE_USER_HOME).getStatusStore());
    pagerAdapter.addFragment(home, "Tweets\n" + user.getStatusesCount(), REQUEST_CODE_USER_HOME);

    final TimelineFragment favs = TimelineFragment.getInstance(this, REQUEST_CODE_USER_FAVS);
    userFavTimeline.open(getContext(), "user_favs");
    userFavTimeline.clear();
    favs.setSortedCache(timelineSubscriberMap.get(REQUEST_CODE_USER_FAVS).getStatusStore());
    pagerAdapter.addFragment(favs, "likes\n" + user.getFavouritesCount(), REQUEST_CODE_USER_FAVS);

    viewPager.setAdapter(pagerAdapter);
    userCache.close();
  }

  @Override
  public void onStart() {
    super.onStart();
    final FragmentActivity activity = getActivity();
    if (activity instanceof FabHandleable) {
      viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
          Log.d(TAG, "onPageSelected: " + position);
          final Fragment item = pagerAdapter.getItem(position);
          if (item instanceof TimelineFragment) {
            TimelineFragment fragment = (TimelineFragment) item;
            if (fragment.isTweetSelected()) {
              ((FabHandleable) activity).showFab();
            } else {
              ((FabHandleable) activity).hideFab();
            }
          }
        }
      });
    }
    tab.setupWithViewPager(viewPager);
  }

  public Fragment getCurrentFragment() {
    final int currentItem = viewPager.getCurrentItem();
    return pagerAdapter.getItem(currentItem);
  }

  @Override
  public void onStop() {
    viewPager.clearOnPageChangeListeners();
    tab.removeAllTabs();
    super.onStop();
  }

  @Override
  public void onDestroyView() {
    viewPager.setAdapter(null);
    userHomeTimeline.close();
    userFavTimeline.close();
    timelineSubscriberMap.clear();
    super.onDestroyView();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    final TimelineSubscriber<SortedCache<Status>> timelineSubscriber = timelineSubscriberMap.get(requestCode);
    final Paging paging = (Paging) data.getSerializableExtra(TimelineFragment.EXTRA_PAGING);
    if (requestCode == REQUEST_CODE_USER_HOME) {
      if (paging == null) {
        timelineSubscriber.fetchHomeTimeline(userId);
      } else {
        timelineSubscriber.fetchHomeTimeline(userId, paging);
      }
      return;
    }
    if (requestCode == REQUEST_CODE_USER_FAVS) {
      if (paging == null) {
        timelineSubscriber.fetchFavorites(userId);
      } else {
        timelineSubscriber.fetchFavorites(userId, paging);
      }
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  private long userId;

  public void setUser(long userId) {
    this.userId = userId;
  }

  private TabLayout tab;

  public void setTabLayout(TabLayout tab) {
    this.tab = tab;
  }

  public void clearSelectedTweet() {
    final Fragment currentFragment = getCurrentFragment();
    if (currentFragment instanceof TimelineFragment) {
      ((TimelineFragment) currentFragment).clearSelectedTweet();
    }
  }

  public void scrollToTop() {
    final Fragment item = getCurrentFragment();
    if (item instanceof TimelineFragment) {
      ((TimelineFragment) item).scrollToTop();
    }
  }

  private static class PagerAdapter extends FragmentPagerAdapter {
    private final List<FragmentPage> pages = new ArrayList<>();

    public PagerAdapter(FragmentManager fm) {
      super(fm);
    }

    public void addFragment(Fragment fragment, String title, int requestCode) {
      final FragmentPage fragmentPage = new FragmentPage();
      fragmentPage.fragment = fragment;
      fragmentPage.title = title;
      fragmentPage.requestCode = requestCode;
      pages.add(fragmentPage);
    }

    @Override
    public Fragment getItem(int position) {
      return pages.get(position).fragment;
    }

    @Override
    public int getCount() {
      return pages.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return pages.get(position).title;
    }

    int getPageRequestCode(int position) {
      return pages.get(position).requestCode;
    }
  }

  private static class FragmentPage {
    Fragment fragment;
    String title;
    int requestCode;
  }

  long getCurrentSelectedStatusId() {
    final Fragment currentFragment = getCurrentFragment();
    if (!(currentFragment instanceof TimelineFragment)) {
      return -1;
    }
    return ((TimelineFragment) currentFragment).getSelectedTweetId();
  }

  @Nullable
  private TimelineSubscriber<SortedCache<Status>> getCurrentTimelineSubscriber() {
    final int currentItem = viewPager.getCurrentItem();
    final int pageRequestCode = pagerAdapter.getPageRequestCode(currentItem);
    return timelineSubscriberMap.get(pageRequestCode);
  }

  void createFavorite() {
    final long statusId = getCurrentSelectedStatusId();
    if (statusId < 0) {
      return;
    }
    final TimelineSubscriber<SortedCache<Status>> timelineSubscriber = getCurrentTimelineSubscriber();
    if (timelineSubscriber != null) {
      timelineSubscriber.createFavorite(statusId);
    }
  }

  void retweetStatus() {
    final long statusId = getCurrentSelectedStatusId();
    if (statusId < 0) {
      return;
    }
    final TimelineSubscriber<SortedCache<Status>> timelineSubscriber = getCurrentTimelineSubscriber();
    if (timelineSubscriber!=null) {
      timelineSubscriber.retweetStatus(statusId);
    }
  }
}

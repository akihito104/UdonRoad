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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.datastore.TimelineStore;
import com.freshdigitable.udonroad.ffab.FlingableFAB;
import com.freshdigitable.udonroad.ffab.FlingableFABHelper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by akihit on 2016/06/06.
 */
public class UserInfoPagerFragment extends Fragment {
  private static final String TAG = UserInfoPagerFragment.class.getSimpleName();
  @Inject
  TwitterApi twitterApi;
  @Inject
  TimelineStore userHomeTimeline;
  @Inject
  TimelineStore userFavTimeline;

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

    final UserHomeTimelineFragment home = UserHomeTimelineFragment.getInstance(userId);
    userHomeTimeline.open(getContext(), "user_home");
    userHomeTimeline.clear();
    home.setTimelineSubscriber(new TimelineSubscriber<>(twitterApi, userHomeTimeline,
            new TimelineSubscriber.SnackbarFeedback(viewPager)));
    home.setFABHelper(fabHelper);
    pagerAdapter.putFragment(home, "Tweets");

    final UserFavsFragment favs = UserFavsFragment.getInstance(userId);
    userFavTimeline.open(getContext(), "user_favs");
    userFavTimeline.clear();
    favs.setTimelineSubscriber(new TimelineSubscriber<>(twitterApi, userFavTimeline,
        new TimelineSubscriber.SnackbarFeedback(viewPager)));
    favs.setFABHelper(fabHelper);
    pagerAdapter.putFragment(favs, "likes");

    viewPager.setAdapter(pagerAdapter);
  }

  @Override
  public void onStart() {
    super.onStart();

    final FlingableFAB ffab = fabHelper.getFab();
    viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        Log.d(TAG, "onPageSelected: " + position);
        final Fragment item = pagerAdapter.getItem(position);
        if (item instanceof TimelineFragment) {
          TimelineFragment fragment = (TimelineFragment) item;
          if (fragment.isTweetSelected()) {
            ffab.show();
          } else {
            ffab.hide();
          }
        }
      }
    });
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
    super.onDestroyView();
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
    fabHelper.getFab().hide();
  }

  public void scrollToTop() {
    final Fragment item = getCurrentFragment();
    if (item instanceof TimelineFragment) {
      ((TimelineFragment) item).scrollToTop();
    }
  }

  private FlingableFABHelper fabHelper;

  public void setFABHelper(FlingableFABHelper flingableFABHelper) {
    this.fabHelper = flingableFABHelper;
  }

  private static class PagerAdapter extends FragmentPagerAdapter {
    private final List<String> fragmentsTitle = new ArrayList<>();
    private final List<Fragment> fragments = new ArrayList<>();

    public PagerAdapter(FragmentManager fm) {
      super(fm);
    }

    public void putFragment(Fragment fragment, String title) {
      fragments.add(fragment);
      fragmentsTitle.add(title);
    }

    List<Fragment> getFragments() {
      return fragments;
    }

    @Override
    public Fragment getItem(int position) {
      return fragments.get(position);
    }

    @Override
    public int getCount() {
      return fragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return fragmentsTitle.get(position);
    }
  }
}

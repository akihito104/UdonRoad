/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

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

import com.freshdigitable.udonroad.fab.FlingableFloatingActionButton;
import com.freshdigitable.udonroad.realmdata.RealmUserFavsFragment;
import com.freshdigitable.udonroad.realmdata.RealmUserHomeTimelineFragment;

import java.util.ArrayList;
import java.util.List;

import twitter4j.User;

/**
 * Created by akihit on 2016/06/06.
 */
public class UserInfoPagerFragment extends Fragment {
  private static final String TAG = UserInfoPagerFragment.class.getSimpleName();

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
    final User user = getUser();

    pagerAdapter = new PagerAdapter(getChildFragmentManager());
    final RealmUserHomeTimelineFragment home = RealmUserHomeTimelineFragment.getInstance(user);
    home.setFAB(ffab);
    pagerAdapter.putFragment(home, "Tweets");
    final RealmUserFavsFragment favs = RealmUserFavsFragment.getInstance(user);
    favs.setFAB(ffab);
    pagerAdapter.putFragment(favs, "likes");
    viewPager.setAdapter(pagerAdapter);
    viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        Log.d(TAG, "onPageSelected: " + position);
        final Fragment item = pagerAdapter.getItem(position);
        if (!(item instanceof TimelineFragment)) {
          return;
        }
        TimelineFragment fragment = (TimelineFragment) item;
        if (fragment.isTweetSelected()) {
          ffab.getFab().show();
        } else {
          ffab.getFab().hide();
        }
      }
    });
    tab.setupWithViewPager(viewPager);
    tab.setVisibility(View.VISIBLE);
  }

  @Override
  public void onStop() {
    viewPager.clearOnPageChangeListeners();
    tab.setVisibility(View.GONE);
    tab.removeAllTabs();
    ffab.getFab().hide();
    super.onStop();
  }

  public static UserInfoPagerFragment getInstance(User user) {
    final Bundle args = new Bundle();
    args.putSerializable("USER", user);
    final UserInfoPagerFragment fragment = new UserInfoPagerFragment();
    fragment.setArguments(args);
    return fragment;
  }

  private User getUser() {
    final Bundle arguments = getArguments();
    return (User) arguments.get("USER");
  }

  private TabLayout tab;

  public void setTabLayout(TabLayout tab) {
    this.tab = tab;
  }

  public void clearSelectedTweet() {
    final List<Fragment> fragments = pagerAdapter.getFragments();
    for (Fragment f : fragments) {
      if (f instanceof TimelineFragment) {
        ((TimelineFragment) f).clearSelectedTweet();
      }
    }
  }

  private FlingableFloatingActionButton ffab;

  public void setFAB(FlingableFloatingActionButton ffab) {
    this.ffab = ffab;
  }

  public void scrollToTop() {
    final int currentItem = viewPager.getCurrentItem();
    final Fragment item = pagerAdapter.getItem(currentItem);
    if (item instanceof TimelineFragment) {
      ((TimelineFragment) item).scrollToTop();
    }
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
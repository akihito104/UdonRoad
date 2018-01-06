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

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.module.InjectionUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;
import twitter4j.User;

/**
 * UserInfoPagerFragment provides ViewPager to show specified user tweets.
 *
 * Created by akihit on 2016/06/06.
 */
public class UserInfoPagerFragment extends Fragment implements ItemSelectable {
  private static final String TAG = UserInfoPagerFragment.class.getSimpleName();
  private static final String ARGS_USER_ID = "userId";
  @Inject
  TypedCache<User> userCache;
  private Disposable subscription;

  public static UserInfoPagerFragment create(long userId) {
    final Bundle args = new Bundle();
    args.putLong(ARGS_USER_ID, userId);
    final UserInfoPagerFragment res = new UserInfoPagerFragment();
    res.setArguments(args);
    return res;
  }

  private long getUserId() {
    return getArguments().getLong(ARGS_USER_ID);
  }

  @Override
  public void onAttach(Context context) {
    Timber.tag(TAG).d("onAttach: ");
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
  public View onCreateView(@NonNull LayoutInflater inflater,
                           @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    Timber.tag(TAG).d("onCreateView: ");
    return inflater.inflate(R.layout.fragment_user_info_pager, container, false);
  }

  private ViewPager viewPager;
  private TabLayout tabLayout;
  private PagerAdapter pagerAdapter;

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    Timber.tag(TAG).d("onViewCreated: ");
    super.onViewCreated(view, savedInstanceState);
    viewPager = view.findViewById(R.id.user_pager);
    tabLayout = view.findViewById(R.id.pager_tabs);
    tabLayout.setupWithViewPager(viewPager);

    pagerAdapter = new PagerAdapter(getChildFragmentManager());
    for (UserPageInfo page : UserPageInfo.values()) {
      putToPagerAdapter(page);
    }
    viewPager.setAdapter(pagerAdapter);
  }

  private void putToPagerAdapter(@NonNull UserPageInfo page) {
    final TimelineFragment f = findTimeline(page);
    final TimelineFragment<?> timelineFragment = f != null ? f : page.setup(getUserId());
    pagerAdapter.putFragment(page, timelineFragment);
  }

  private TimelineFragment findTimeline(@NonNull UserPageInfo page) {
    final List<Fragment> fragments = getChildFragmentManager().getFragments();
    for (Fragment f : fragments) {
      if (f instanceof TimelineFragment) {
        final TimelineFragment timeline = (TimelineFragment) f;
        if (page.storeType.nameWithSuffix(getUserId(), "").equals(timeline.getStoreName())) {
          return timeline;
        }
      }
    }
    return null;
  }

  @Override
  public void onStart() {
    Timber.tag(TAG).d("onStart: ");
    super.onStart();
    final FabViewModel fabViewModel = ViewModelProviders.of(getActivity()).get(FabViewModel.class);
    viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        Timber.tag(TAG).d("onPageSelected: %s", position);
        TimelineFragment fragment = pagerAdapter.getItem(position);
        if (fragment.isItemSelected()) {
          fabViewModel.showFab(FabViewModel.Type.FAB);
        } else {
          fabViewModel.hideFab();
        }
      }
    });
    userCache.open();
    if (!Utils.isSubscribed(subscription)) {
      subscription = userCache.observeById(getUserId())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(this::updateTabs,
              e -> Timber.tag(TAG).e(e, "userUpdated: "));
    }
  }

  public TimelineFragment getCurrentFragment() {
    final int currentItem = viewPager.getCurrentItem();
    return pagerAdapter.getItem(currentItem);
  }

  @Override
  public void onStop() {
    Timber.tag(TAG).d("onStop: ");
    super.onStop();
    viewPager.clearOnPageChangeListeners();
    if (subscription != null) {
      subscription.dispose();
    }
    userCache.close();
  }

  @Override
  public void onDetach() {
    Timber.tag(TAG).d("onDetach: ");
    super.onDetach();
    viewPager.setAdapter(null);
  }

  private void updateTabs(User user) {
    for (UserPageInfo p : UserPageInfo.values()) {
      final TabLayout.Tab tab = tabLayout.getTabAt(p.ordinal());
      if (tab != null) {
        tab.setText(p.createTitle(user));
      }
    }
  }

  @Override
  public void clearSelectedItem() {
    final TimelineFragment currentFragment = getCurrentFragment();
    currentFragment.clearSelectedItem();
  }

  @Override
  public boolean isItemSelected() {
    return getCurrentFragment().isItemSelected();
  }

  @Override
  public long getSelectedItemId() {
    return getCurrentSelectedStatusId();
  }

  private static class PagerAdapter extends FragmentPagerAdapter {
    private PagerAdapter(FragmentManager fm) {
      super(fm);
    }

    private final Map<UserPageInfo, TimelineFragment> pages = new HashMap<>();

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

  private long getCurrentSelectedStatusId() {
    final UserPageInfo currentPage = getCurrentPage();
    if (!currentPage.isStatus()) {
      return -1;
    }
    return getCurrentFragment().getSelectedItemId();
  }

  @NonNull
  UserPageInfo getCurrentPage() {
    final int currentItem = viewPager.getCurrentItem();
    return UserPageInfo.values()[currentItem];
  }

  enum UserPageInfo {
    TWEET(StoreType.USER_HOME) {
      @Override
      public String createTitle(User user) {
        return name() + "\n" + user.getStatusesCount();
      }
    },
    FRIEND(StoreType.USER_FRIEND) {
      @Override
      public String createTitle(User user) {
        return name() + "\n" + user.getFriendsCount();
      }
    },
    FOLLOWER(StoreType.USER_FOLLOWER) {
      @Override
      public String createTitle(User user) {
        return name() + "\n" + user.getFollowersCount();
      }
    },
    FAV(StoreType.USER_FAV) {
      @Override
      public String createTitle(User user) {
        return name() + "\n" + user.getFavouritesCount();
      }
    },
    LISTED(StoreType.USER_LIST) {
      @Override
      public String createTitle(User user) {
        final int listedCount = user.getListedCount();
        return name() + "\n" + (listedCount >= 0 ? listedCount : "0");
      }
    },
    MEDIA(StoreType.USER_MEDIA) {
      @Override
      public String createTitle(User user) {
        return name();
      }
    },;

    final StoreType storeType;

    UserPageInfo(StoreType type) {
      this.storeType = type;
    }

    TimelineFragment<?> setup(long id) {
      return TimelineFragment.getInstance(storeType, id);
    }

    public boolean isStatus() {
      return storeType.isForStatus();
    }

    public boolean isUser() {
      return storeType.isForUser();
    }

    public abstract String createTitle(User user);
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    final Animation animation = TimelineContainerSwitcher.makeSwitchingAnimation(getContext(), transit, enter);
    return animation != null ? animation
        : super.onCreateAnimation(transit, enter, nextAnim);
  }
}

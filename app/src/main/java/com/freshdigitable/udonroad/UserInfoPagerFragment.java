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
import android.view.animation.Animation;

import com.freshdigitable.udonroad.module.InjectionUtil;

import java.util.HashMap;
import java.util.Map;

import twitter4j.User;

/**
 * UserInfoPagerFragment provides ViewPager to show specified user tweets.
 *
 * Created by akihit on 2016/06/06.
 */
public class UserInfoPagerFragment extends Fragment implements ItemSelectable {
  private static final String TAG = UserInfoPagerFragment.class.getSimpleName();
  private static final String ARGS_USER_ID = "userId";

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
    Log.d(TAG, "onAttach: ");
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
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView: ");
    return viewPager == null ?
        inflater.inflate(R.layout.fragment_user_info_pager, container, false)
        : viewPager;
  }

  private ViewPager viewPager;

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    Log.d(TAG, "onViewCreated: ");
    super.onViewCreated(view, savedInstanceState);
    if (viewPager == null) {
      viewPager = view.findViewById(R.id.user_pager);
    }
  }

  private PagerAdapter pagerAdapter;

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    Log.d(TAG, "onActivityCreated: ");
    super.onActivityCreated(savedInstanceState);
    if (pagerAdapter == null) {
      pagerAdapter = new PagerAdapter(getChildFragmentManager());
      for (UserPageInfo page : UserPageInfo.values()) {
        putToPagerAdapter(page);
      }
      viewPager.setAdapter(pagerAdapter);
    }
  }

  private void putToPagerAdapter(@NonNull UserPageInfo page) {
    final TimelineFragment<?> fragment = page.setup(getUserId());
    pagerAdapter.putFragment(page, fragment);
  }

  @Override
  public void onStart() {
    Log.d(TAG, "onStart: ");
    super.onStart();
    final FragmentActivity activity = getActivity();
    if (activity instanceof FabHandleable) {
      viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
          Log.d(TAG, "onPageSelected: " + position);
          TimelineFragment fragment = pagerAdapter.getItem(position);
          if (fragment.isItemSelected()) {
            ((FabHandleable) activity).showFab();
          } else {
            ((FabHandleable) activity).hideFab();
          }
        }
      });
    }
  }

  private TimelineFragment getCurrentFragment() {
    final int currentItem = viewPager.getCurrentItem();
    return pagerAdapter.getItem(currentItem);
  }

  @Override
  public void onStop() {
    Log.d(TAG, "onStop: ");
    super.onStop();
    viewPager.clearOnPageChangeListeners();
  }

  @Override
  public void onDetach() {
    Log.d(TAG, "onDetach: ");
    super.onDetach();
    viewPager.setAdapter(null);
  }

  public ViewPager getViewPager() {
    return viewPager;
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

  long getCurrentSelectedStatusId() {
    final UserPageInfo currentPage = getCurrentPage();
    if (!currentPage.isStatus()) {
      return -1;
    }
    return getCurrentFragment().getSelectedTweetId();
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

package com.freshdigitable.udonroad;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.AppBarLayout.OnOffsetChangedListener;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.freshdigitable.udonroad.databinding.FragmentUserInfoAppbarBinding;
import com.freshdigitable.udonroad.realmdata.StatusCache;
import com.squareup.picasso.Picasso;

import twitter4j.User;

/**
 * Created by akihit on 2016/02/07.
 */
public class UserInfoAppbarFragment extends Fragment {
  private FragmentUserInfoAppbarBinding binding;
  private StatusCache statusCache;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    setHasOptionsMenu(true);
    View view = inflater.inflate(R.layout.fragment_user_info_appbar, container, false);
    binding = DataBindingUtil.bind(view);
    return view;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    binding.userInfoToolbar.setTitle("");
    final TextView toolbarTitle = binding.userInfoToolbarTitle;
    binding.userInfoAppbarLayout.addOnOffsetChangedListener(new OnOffsetChangedListener() {
      private boolean isTitleVisible = toolbarTitle.getVisibility() == View.VISIBLE;

      @Override
      public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        final int totalScrollRange = appBarLayout.getTotalScrollRange();
        final float percent = (float) Math.abs(verticalOffset) / (float) totalScrollRange;
        if (percent > 0.9) {
          if (!isTitleVisible) {
            startAnimation(toolbarTitle, View.VISIBLE);
            isTitleVisible = true;
          }
        } else {
          if (isTitleVisible) {
            startAnimation(toolbarTitle, View.INVISIBLE);
            isTitleVisible = false;
          }
        }
      }

      private void startAnimation(View v, int visibility) {
        AlphaAnimation animation = (visibility == View.VISIBLE)
            ? new AlphaAnimation(0f, 1f)
            : new AlphaAnimation(1f, 0f);
        animation.setDuration(200);
        animation.setFillAfter(true);
        v.startAnimation(animation);
      }
    });
  }

  @Override
  public void onStart() {
    super.onStart();

    final AppCompatActivity activity = (AppCompatActivity) getActivity();
    activity.setSupportActionBar(binding.userInfoToolbar);
    final ActionBar supportActionBar = activity.getSupportActionBar();
    if (supportActionBar != null) {
      supportActionBar.setDisplayHomeAsUpEnabled(true);
      supportActionBar.setHomeButtonEnabled(true);
    }
    statusCache = new StatusCache(getContext());
    final User user = statusCache.getUser(userId);
    showUserInfo(user);
  }

  @Override
  public void onStop() {
    dismissUserInfo();
    statusCache.close();
    super.onStop();
  }

  private long userId;

  public void setUser(long userId) {
    this.userId = userId;
  }

  private void showUserInfo(User user) {
    UserInfoActivity.bindUserScreenName(binding.userInfoToolbarTitle, user);
    binding.userInfoUserInfoView.bindData(user);
    Picasso.with(getContext())
        .load(user.getProfileBannerMobileURL())
        .fit()
        .into(binding.userInfoUserInfoView.getBanner());
    Picasso.with(getContext())
        .load(user.getProfileImageURLHttps())
        .into(binding.userInfoUserInfoView.getIcon());
  }

  private void dismissUserInfo() {
    binding.userInfoToolbarTitle.setText("");
    binding.userInfoCollapsingToolbar.setTitleEnabled(false);
    binding.userInfoTabs.removeAllTabs();
    Picasso.with(getContext())
        .cancelRequest(binding.userInfoUserInfoView.getBanner());
    Picasso.with(getContext())
        .cancelRequest(binding.userInfoUserInfoView.getIcon());
  }

  public TabLayout getTabLayout() {
    return binding.userInfoTabs;
  }
}

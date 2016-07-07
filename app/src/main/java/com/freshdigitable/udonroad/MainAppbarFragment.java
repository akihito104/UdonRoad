package com.freshdigitable.udonroad;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.freshdigitable.udonroad.databinding.FragmentMainAppbarBinding;

import rx.Observable;
import twitter4j.User;

/**
 * MainAppbarFragment integrates Appbar parts.
 *
 * Created by akihit on 2016/02/06.
 */
public class MainAppbarFragment extends Fragment {
  private static final String TAG = MainAppbarFragment.class.getSimpleName();
  private FragmentMainAppbarBinding binding;
  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView: ");
    View view = inflater.inflate(R.layout.fragment_main_appbar, container, false);
    binding = DataBindingUtil.bind(view);
    return view;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    Log.d(TAG, "onActivityCreated: ");
    super.onActivityCreated(savedInstanceState);
    // android:titleTextColor is required up to API level 23
    binding.mainToolbar.setTitleTextColor(Color.WHITE);
    binding.mainTweetInputView.setUserObservable(userObservable);
    binding.mainToolbar.setTitle("Home");

    final TextView toolbarTitle = binding.mainToolbarTitle;
    binding.mainAppbarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
      // it is visible (alpha=1.0) at initial state.
      private boolean isTitleVisible = true;

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
    activity.setSupportActionBar(binding.mainToolbar);
    final ActionBar supportActionBar = activity.getSupportActionBar();
    if (supportActionBar != null) {
      supportActionBar.setDisplayHomeAsUpEnabled(true);
      supportActionBar.setHomeButtonEnabled(true);
    }
  }

  public void stretchStatusInputView(TweetInputView.OnStatusSending statusSending) {
    binding.mainTweetInputView.appearing(statusSending);
  }

  public void collapseStatusInputView() {
    binding.mainTweetInputView.disappearing();
  }

  public boolean isStatusInputViewVisible() {
    return binding.mainTweetInputView.isVisible();
  }

  private Observable<User> userObservable;

  public void setUserObservable(Observable<User> user) {
    this.userObservable = user;
  }

  public Toolbar getToolbar() {
    return binding.mainToolbar;
  }

  public boolean isUserInfoVisible() {
    return binding.mainUserInfoView.getVisibility() == View.VISIBLE;
  }

  public void showUserInfo(User user) {
    binding.mainUserInfoView.setVisibility(View.VISIBLE);
    binding.mainToolbar.setTitle("");
    binding.mainToolbarTitle.setText("@" + user.getScreenName());
    binding.mainUserInfoView.bindData(user);
  }

  public void dismissUserInfo() {
    binding.mainUserInfoView.setVisibility(View.GONE);
    binding.mainToolbarTitle.setText("");
    binding.mainToolbar.setTitle("Home");
    binding.mainCollapsingToolbar.setTitleEnabled(false);
    binding.mainTabs.setVisibility(View.GONE);
    binding.mainTabs.removeAllTabs();
  }

  public TabLayout getTabLayout() {
    return binding.mainTabs;
  }
}

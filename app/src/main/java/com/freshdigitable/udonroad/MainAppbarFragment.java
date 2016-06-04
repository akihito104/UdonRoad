package com.freshdigitable.udonroad;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
    binding.mainCollapsingToolbar.setTitleEnabled(true);
    binding.mainCollapsingToolbar.setCollapsedTitleTextColor(Color.WHITE);
    binding.mainCollapsingToolbar.setExpandedTitleColor(Color.TRANSPARENT);
    binding.mainCollapsingToolbar.setTitle("@" + user.getScreenName());
    binding.mainUserInfoView.bindData(user);
  }

  public void dismissUserInfo() {
    binding.mainUserInfoView.setVisibility(View.GONE);
    binding.mainCollapsingToolbar.setTitleEnabled(false);
  }
}

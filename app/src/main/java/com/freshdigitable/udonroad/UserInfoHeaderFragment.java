package com.freshdigitable.udonroad;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.databinding.FragmentUserInfoBinding;
import com.squareup.picasso.Picasso;

import java.io.Serializable;

import twitter4j.User;

/**
 * Created by akihit on 2016/02/07.
 */
public class UserInfoHeaderFragment extends Fragment {
  private static final String KEY_USER = "user";
  private FragmentUserInfoBinding binding;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    setHasOptionsMenu(true);
    View view = inflater.inflate(R.layout.fragment_user_info, container, false);
    binding = DataBindingUtil.bind(view);
    return view;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    User user = parseUser();
    if (user != null) {
      bindUserInfo(user);
    }
  }

  @Override
  public void onStart() {
    super.onStart();

    final AppCompatActivity activity = (AppCompatActivity) getActivity();
    activity.setSupportActionBar(binding.userToolbar);
    final ActionBar supportActionBar = activity.getSupportActionBar();
    if (supportActionBar != null) {
      supportActionBar.setDisplayHomeAsUpEnabled(true);
      supportActionBar.setHomeButtonEnabled(true);
    }
  }

  public void bindUserInfo(User user) {
    binding.userScreenName.setText("@" + user.getScreenName());
    binding.userName.setText(user.getName());
    binding.userDescription.setText(user.getDescription());
    Picasso.with(binding.userIcon.getContext()).load(user.getProfileImageURLHttps()).into(binding.userIcon);
    Picasso.with(binding.userBanner.getContext()).load(user.getProfileBannerMobileURL()).fit().into(binding.userBanner);
    binding.userTweetsCount.setText("* " + user.getStatusesCount());
    binding.userFollowerCount.setText("> " + user.getFollowersCount());
    binding.userFriendsCount.setText("< " + user.getFriendsCount());

    binding.userToolbar.setTitle(user.getName());
    binding.userToolbar.setSubtitle("@" + user.getScreenName());
  }

  public static UserInfoHeaderFragment getInstance(User user) {
    UserInfoHeaderFragment instance = new UserInfoHeaderFragment();
    Bundle args = new Bundle();
    args.putSerializable(KEY_USER, user);
    instance.setArguments(args);
    return instance;
  }

  @Nullable
  private User parseUser() {
    Bundle args = getArguments();
    if (args == null) {
      return null;
    }
    Serializable ret = args.getSerializable(KEY_USER);
    return ret != null ? (User) ret : null;
  }

  public Toolbar getToolbar() {
    return binding.userToolbar;
  }
}

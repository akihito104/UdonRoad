/*
 * Copyright (c) 2016. Akihito Matsuda (akihito104)
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
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.databinding.FragmentUserInfoBinding;
import com.freshdigitable.udonroad.datastore.StatusCache;
import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import twitter4j.User;

/**
 * Created by akihit on 2016/02/07.
 */
public class UserInfoFragment extends Fragment {
  private FragmentUserInfoBinding binding;
  @Inject
  StatusCache statusCache;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    InjectionUtil.getComponent(this).inject(this);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    setHasOptionsMenu(true);
    binding = FragmentUserInfoBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
  }

  @Override
  public void onStart() {
    super.onStart();
    statusCache.open(getContext());

    final long userId = getUserId();
    final User user = statusCache.getUser(userId);
    showUserInfo(user);
  }

  @Override
  public void onStop() {
    dismissUserInfo();
    statusCache.close();
    super.onStop();
  }

  private void showUserInfo(User user) {
    Picasso.with(getContext())
        .load(user.getProfileImageURLHttps())
        .into(binding.userInfoUserInfoView.getIcon());
    Picasso.with(getContext())
        .load(user.getProfileBannerMobileURL())
        .fit()
        .into(binding.userInfoUserInfoView.getBanner());
    binding.userInfoUserInfoView.bindData(user);
  }

  private void dismissUserInfo() {
    Picasso.with(getContext())
        .cancelRequest(binding.userInfoUserInfoView.getBanner());
    Picasso.with(getContext())
        .cancelRequest(binding.userInfoUserInfoView.getIcon());
  }

  public static UserInfoFragment create(long userId) {
    final UserInfoFragment userInfoAppbarFragment = new UserInfoFragment();
    final Bundle args = new Bundle();
    args.putLong("userId", userId);
    userInfoAppbarFragment.setArguments(args);
    return userInfoAppbarFragment;
  }

  private long getUserId() {
    final Bundle arguments = getArguments();
    return arguments.getLong("userId");
  }
}

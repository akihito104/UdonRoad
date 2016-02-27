/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.databinding.FragmentStatusDetailBinding;

import twitter4j.Status;

public class StatusDetailFragment extends Fragment {
  private FragmentStatusDetailBinding binding;
  private Status status;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_status_detail, container, false);
    binding = DataBindingUtil.bind(view);
    return view;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    this.status = (Status) getArguments().get("status");
    binding.statusView.bindStatus(this.status);
  }

  public static StatusDetailFragment getInstance(final Status status) {
    Bundle args = new Bundle();
    args.putSerializable("status", status);
    final StatusDetailFragment statusDetailFragment = new StatusDetailFragment();
    statusDetailFragment.setArguments(args);
    return statusDetailFragment;
  }
}

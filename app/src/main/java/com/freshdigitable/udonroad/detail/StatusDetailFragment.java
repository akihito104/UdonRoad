/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad.detail;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.freshdigitable.udonroad.FabViewModel;
import com.freshdigitable.udonroad.OnSpanClickListener;
import com.freshdigitable.udonroad.OnSpanClickListener.SpanItem;
import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.databinding.FragmentStatusDetailBinding;
import com.freshdigitable.udonroad.databinding.ViewStatusDetailBinding;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.user.UserInfoActivity;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import io.reactivex.disposables.Disposable;
import twitter4j.User;

/**
 * StatusDetailFragment shows Status with link text and twitter card.
 *
 * Created by Akihit.
 */
public class StatusDetailFragment extends Fragment {
  @SuppressWarnings("unused")
  private static final String TAG = StatusDetailFragment.class.getSimpleName();
  private FragmentStatusDetailBinding binding;
  @Inject
  StatusDetailViewImageLoader imageLoader;
  private FabViewModel fabViewModel;
  private Disposable imagesSubs;
  @Inject
  StatusDetailBindingComponent bindingComponent;
  @Inject
  ViewModelProvider.Factory factory;
  private StatusDetailViewModel statusDetailViewModel;

  public static StatusDetailFragment newInstance(final long statusId) {
    Bundle args = new Bundle();
    args.putLong("statusId", statusId);
    final StatusDetailFragment statusDetailFragment = new StatusDetailFragment();
    statusDetailFragment.setArguments(args);
    return statusDetailFragment;
  }

  @Override
  public void onAttach(Context context) {
    AndroidSupportInjection.inject(this);
    super.onAttach(context);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_status_detail, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    binding = DataBindingUtil.bind(view, bindingComponent);
    statusDetailViewModel = StatusDetailViewModel.getInstance(getActivity(), factory);
    binding.setViewModel(statusDetailViewModel);
  }

  @Override
  public void onStart() {
    super.onStart();
    final long statusId = getStatusId();
    fabViewModel = ViewModelProviders.of(getActivity()).get(FabViewModel.class);
    setupActionToolbar(statusId);

    final ViewStatusDetailBinding statusView = binding.statusView;
    statusDetailViewModel.observeById(statusId).observe(this, item -> {
      if (item == null) {
        return;
      }
      imagesSubs = imageLoader.loadImages(statusView, item.statusListItem);

      final User user = item.user;
      final ImageView icon = statusView.dIcon;
      final OnUserIconClickedListener userIconClickedListener = createUserIconClickedListener();
      icon.setOnClickListener(
          view -> userIconClickedListener.onUserIconClicked(view, user));
      statusView.dNames.setOnClickListener(
          view -> userIconClickedListener.onUserIconClicked(icon, user));
      binding.statusView.dTwitterBird.setOnClickListener(v -> {
        final Intent intent = new Intent();
        intent.setData(Uri.parse("https://twitter.com/" + item.user.getScreenName() + "/status/" + item.id));
        v.getContext().startActivity(intent);
      });

      updateFabMenuItem(item);
    });

    final FragmentActivity activity = getActivity();
    final OnSpanClickListener spanClickListener = activity instanceof OnSpanClickListener ?
        (OnSpanClickListener) activity : (v, i) -> {};
    statusDetailViewModel.getSpanClickEvent().observe(this, event -> {
      if (event == null) {
        return;
      }
      if (event.item.getType() == SpanItem.TYPE_URL) {
        new URLSpan(event.item.getQuery()).onClick(event.view);
      } else if (event.item.getType() == SpanItem.TYPE_MENTION) {
        UserInfoActivity.start(event.view.getContext(), event.item.getId());
      } else if (event.item.getType() == SpanItem.TYPE_HASHTAG) {
        spanClickListener.onSpanClicked(event.view, event.item);
      }
    });
  }

  private void updateFabMenuItem(final DetailItem item) {
    fabViewModel.setMenuState(item.stats);
  }

  @Override
  public void onResume() {
    super.onResume();
    fabViewModel.showFab(FabViewModel.Type.TOOLBAR);
  }

  @Override
  public void onStop() {
    super.onStop();
    binding.statusView.dRtUser.setOnClickListener(null);
    binding.statusView.dIcon.setOnClickListener(null);
    binding.statusView.dNames.setOnClickListener(null);
    binding.statusView.dImageGroup.setOnMediaClickListener(null);
    binding.sdTwitterCard.getRoot().setOnClickListener(null);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    Utils.maybeDispose(imagesSubs);
    bindingComponent.dispose();
  }

  private OnUserIconClickedListener createUserIconClickedListener() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof OnUserIconClickedListener) {
      return (OnUserIconClickedListener) activity;
    } else {
      return (view, user) -> UserInfoActivity.start(activity, user, view);
    }
  }

  private void setupActionToolbar(long statusId) {
    fabViewModel.getMenuItem().observe(this, item -> {
      if (item == null) {
        return;
      }
      if (item.getItemId() == R.id.iffabMenu_main_fav) {
        if (!item.isChecked()) {
          statusDetailViewModel.createFavorite(statusId);
        } else {
          statusDetailViewModel.destroyFavorite(statusId);
        }
      } else if (item.getItemId() == R.id.iffabMenu_main_rt) {
        if (!item.isChecked()) {
          statusDetailViewModel.retweet(statusId);
        } else {
          statusDetailViewModel.unretweet(statusId);
        }
      } else if (item.getItemId() == R.id.iffabMenu_main_delete) {
        statusDetailViewModel.delete(statusId);
      }
    });
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    if (transit == FragmentTransaction.TRANSIT_FRAGMENT_OPEN) {
      if (enter) {
        return AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
      } else {
        return AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
      }
    }
    if (transit == FragmentTransaction.TRANSIT_FRAGMENT_CLOSE) {
      if (enter) {
        return AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
      } else {
        return AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
      }
    }
    return super.onCreateAnimation(transit, enter, nextAnim);
  }

  public long getStatusId() {
    final Bundle arguments = getArguments();
    if (arguments == null) {
      throw new IllegalStateException("arguments: statusId should be set. use StatusDetailFragment.newInstance().");
    }
    return (long) arguments.get("statusId");
  }
}

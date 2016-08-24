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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.freshdigitable.udonroad.MediaContainer.OnMediaClickListener;
import com.freshdigitable.udonroad.StatusViewBase.OnUserIconClickedListener;
import com.freshdigitable.udonroad.TweetInputFragment.TweetSendable;
import com.freshdigitable.udonroad.TweetInputFragment.TweetType;
import com.freshdigitable.udonroad.databinding.FragmentStatusDetailBinding;
import com.freshdigitable.udonroad.datastore.StatusCache;

import javax.inject.Inject;

import twitter4j.Status;
import twitter4j.User;
import twitter4j.UserMentionEntity;

public class StatusDetailFragment extends Fragment {
  @SuppressWarnings("unused")
  private static final String TAG = StatusDetailFragment.class.getSimpleName();
  private FragmentStatusDetailBinding binding;
  private Status status;
  @Inject
  StatusCache statusCache;
  @Inject
  TwitterApi twitterApi;
  private TimelineSubscriber<StatusCache> statusCacheSubscriber;

  public static StatusDetailFragment getInstance(final long statusId) {
    Bundle args = new Bundle();
    args.putLong("statusId", statusId);
    final StatusDetailFragment statusDetailFragment = new StatusDetailFragment();
    statusDetailFragment.setArguments(args);
    return statusDetailFragment;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    InjectionUtil.getComponent(this).inject(this);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    binding = FragmentStatusDetailBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onStart() {
    super.onStart();
    long id = (long) getArguments().get("statusId");
    statusCache.open(getContext());
    status = statusCache.findStatus(id);
    if (status == null) {
      Toast.makeText(getContext(), "status is not found", Toast.LENGTH_SHORT).show();
      return;
    }
    final UserMentionEntity[] userMentionEntities = status.getUserMentionEntities();
    for (UserMentionEntity u : userMentionEntities) {
      statusCache.upsertUser(u);
    }
    statusCacheSubscriber = new TimelineSubscriber<>(twitterApi, statusCache,
        new TimelineSubscriber.SnackbarFeedback(binding.getRoot()));

    final DetailStatusView statusView = binding.statusView;
    statusView.bindStatus(status);
    StatusViewImageHelper.load(status, statusView);
    final User user = StatusViewImageHelper.getBindingUser(status);
    final ImageView icon = statusView.getIcon();
    icon.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        userIconClickedListener.onClicked(view, user);
      }
    });
    statusView.getUserName().setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        userIconClickedListener.onClicked(icon, user);
      }
    });
    statusView.getMediaContainer().setOnMediaClickListener(new OnMediaClickListener() {
      @Override
      public void onMediaClicked(View view, int index) {
        MediaViewActivity.start(view.getContext(), status, index);
      }
    });

    binding.sdFav.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (status.isFavorited()) {
          statusCacheSubscriber.destroyFavorite(status.getId());
        } else {
          statusCacheSubscriber.createFavorite(status.getId());
        }
      }
    });
    binding.sdRetweet.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (status.isRetweeted()) {
          statusCacheSubscriber.destroyRetweet(status.getId());
        } else {
          statusCacheSubscriber.retweetStatus(status.getId());
        }
      }
    });
    binding.sdRetweet.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        setupInput(TweetInputFragment.TYPE_REPLY);
      }
    });
    binding.sdQuote.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        setupInput(TweetInputFragment.TYPE_QUOTE);
      }
    });
  }

  private void setupInput(@TweetType int type) {
    final FragmentActivity activity = getActivity();
    if (activity instanceof TweetSendable) {
      ((TweetSendable) activity).setupInput(type, status.getId());
    } else {
      ReplyActivity.start(activity, status.getId(), type, null);
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    binding.statusView.getIcon().setOnClickListener(null);
    binding.statusView.getUserName().setOnClickListener(null);
    binding.statusView.getMediaContainer().setOnMediaClickListener(null);
    binding.statusView.reset();

    binding.sdFav.setOnClickListener(null);
    binding.sdRetweet.setOnClickListener(null);
    binding.sdReply.setOnClickListener(null);
    binding.sdQuote.setOnClickListener(null);

    if (status != null) {
      StatusViewImageHelper.unload(getContext(), status.getId());
    }
    statusCache.close();
    status = null;
  }

  private OnUserIconClickedListener userIconClickedListener;

  public void setOnUserIconClickedListener(OnUserIconClickedListener listener) {
    this.userIconClickedListener = listener;
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
}

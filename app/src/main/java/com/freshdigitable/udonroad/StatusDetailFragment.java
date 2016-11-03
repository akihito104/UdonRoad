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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;
import android.util.Log;
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
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;
import com.freshdigitable.udonroad.subscriber.TimelineSubscriber;
import com.freshdigitable.udonroad.subscriber.UserFeedbackSubscriber;
import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import twitter4j.Status;
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
  TypedCache<Status> statusCache;
  @Inject
  TwitterApi twitterApi;
  private TimelineSubscriber<TypedCache<Status>> statusCacheSubscriber;
  private Subscription subscription;
  @Inject
  UserFeedbackSubscriber userFeedback;

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
    binding = DataBindingUtil.inflate(inflater, R.layout.fragment_status_detail, container, false);
    return binding.getRoot();
  }

  @Override
  public void onStart() {
    super.onStart();

    final long statusId = getStatusId();
    statusCache.open();
    final Status status = statusCache.find(statusId);
    if (status == null) {
      Toast.makeText(getContext(), "status is not found", Toast.LENGTH_SHORT).show();
      return;
    }

    final StatusDetailView statusView = binding.statusView;
    StatusViewImageHelper.load(status, statusView);
    final User user = StatusViewImageHelper.getBindingUser(status);

    final ImageView icon = statusView.getIcon();
    final OnUserIconClickedListener userIconClickedListener = createUserIconClickedListener();
    icon.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        userIconClickedListener.onUserIconClicked(view, user);
      }
    });
    statusView.getUserName().setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        userIconClickedListener.onUserIconClicked(icon, user);
      }
    });
    statusView.getMediaContainer().setOnMediaClickListener(new OnMediaClickListener() {
      @Override
      public void onMediaClicked(View view, int index) {
        MediaViewActivity.start(view.getContext(), status, index);
      }
    });

    statusCacheSubscriber = new TimelineSubscriber<>(twitterApi, statusCache,userFeedback);
    statusCacheSubscriber.registerRootView(binding.getRoot());
    setTintList(binding.sdFav.getDrawable(), R.color.selector_fav_icon);
    binding.sdFav.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (status.isFavorited()) {
          statusCacheSubscriber.destroyFavorite(statusId);
        } else {
          statusCacheSubscriber.createFavorite(statusId);
        }
      }
    });
    setTintList(binding.sdRetweet.getDrawable(), R.color.selector_rt_icon);
    binding.sdRetweet.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (status.isRetweeted()) {
          statusCacheSubscriber.destroyRetweet(statusId);
        } else {
          statusCacheSubscriber.retweetStatus(statusId);
        }
      }
    });
    DrawableCompat.setTint(binding.sdReply.getDrawable(),
        ContextCompat.getColor(getContext(), R.color.twitter_action_normal));
    binding.sdReply.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        setupInput(TweetInputFragment.TYPE_REPLY);
      }
    });
    setTintList(binding.sdQuote.getDrawable(), R.color.twitter_action_normal);
    binding.sdQuote.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        setupInput(TweetInputFragment.TYPE_QUOTE);
      }
    });

    subscription = statusCache.observeById(statusId)
        .subscribe(new Action1<Status>() {
          @Override
          public void call(Status status) {
            binding.statusView.bindStatus(status);
            binding.sdFav.setActivated(status.isFavorited());
            binding.sdRetweet.setActivated(status.isRetweeted());
          }
        });

    final Status bindingStatus = StatusViewImageHelper.getBindingStatus(status);
    if (bindingStatus.getURLEntities().length < 1) {
      return;
    }
    if (twitterCard != null) {
      setupTwitterCard(twitterCard);
    } else {
      TwitterCardFetcher.observeFetch(bindingStatus.getURLEntities()[0].getExpandedURL())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new Action1<TwitterCard>() {
            @Override
            public void call(final TwitterCard twitterCard) {
              setupTwitterCard(twitterCard);
            }
          }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
              Log.e(TAG, "card fetch: ", throwable);
            }
          });
    }
  }

  private TwitterCard twitterCard;

  private void setupTwitterCard(@NonNull final TwitterCard twitterCard) {
    this.twitterCard = twitterCard;
    if (!isValidForView(twitterCard)) {
      return;
    }
    final long statusId = getStatusId();
    binding.sdTwitterCard.setVisibility(View.VISIBLE);
    binding.sdTwitterCard.bindData(twitterCard);
    final String imageUrl = twitterCard.getImageUrl();
    if (!TextUtils.isEmpty(imageUrl)) {
      Picasso.with(getContext())
          .load(imageUrl)
          .resizeDimen(R.dimen.card_summary_image, R.dimen.card_summary_image)
          .centerCrop()
          .tag(statusId)
          .into(binding.sdTwitterCard.getImage());
    }

    final Intent intent = new Intent(Intent.ACTION_VIEW);
    final String appUrl = twitterCard.getAppUrl();
    if (!TextUtils.isEmpty(appUrl)) {
      intent.setData(Uri.parse(appUrl));
      final ComponentName componentName = intent.resolveActivity(getContext().getPackageManager());
      if (componentName == null) {
        intent.setData(Uri.parse(twitterCard.getUrl()));
      }
    } else {
      intent.setData(Uri.parse(twitterCard.getUrl()));
    }
    binding.sdTwitterCard.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        getContext().startActivity(intent);
      }
    });
  }

  private boolean isValidForView(TwitterCard twitterCard) {
    return !TextUtils.isEmpty(twitterCard.getTitle())
        && !TextUtils.isEmpty(twitterCard.getUrl());
  }

  private void setTintList(Drawable drawable, @ColorRes int color) {
    DrawableCompat.setTintList(drawable,
        ContextCompat.getColorStateList(getContext(), color));
  }

  private void setupInput(@TweetType int type) {
    final FragmentActivity activity = getActivity();
    final long statusId = getStatusId();
    if (activity instanceof TweetSendable) {
      ((TweetSendable) activity).setupInput(type, statusId);
    } else {
      ReplyActivity.start(activity, statusId, type, null);
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    binding.statusView.getIcon().setOnClickListener(null);
    binding.statusView.getUserName().setOnClickListener(null);
    binding.statusView.getMediaContainer().setOnMediaClickListener(null);
    binding.statusView.reset();
    binding.sdTwitterCard.setOnClickListener(null);
    binding.sdFav.setOnClickListener(null);
    binding.sdRetweet.setOnClickListener(null);
    binding.sdReply.setOnClickListener(null);
    binding.sdQuote.setOnClickListener(null);
    if (subscription != null && !subscription.isUnsubscribed()) {
      subscription.unsubscribe();
    }
    final long statusId = getStatusId();
    Picasso.with(getContext()).cancelTag(statusId);
    StatusViewImageHelper.unload(binding.statusView, statusId);
    statusCacheSubscriber.close();
    statusCache.close();
  }

  @Override
  public void onDetach() {
    super.onDetach();
    binding.sdTwitterCard.setVisibility(View.GONE);
    DrawableCompat.setTintList(binding.sdFav.getDrawable(), null);
    DrawableCompat.setTintList(binding.sdRetweet.getDrawable(), null);
  }

  private OnUserIconClickedListener createUserIconClickedListener() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof OnUserIconClickedListener) {
      return (OnUserIconClickedListener) activity;
    } else {
      return new OnUserIconClickedListener() {
        @Override
        public void onUserIconClicked(View view, User user) {
          UserInfoActivity.start(activity, user, view);
        }
      };
    }
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

  private long getStatusId() {
    return (long) getArguments().get("statusId");
  }
}

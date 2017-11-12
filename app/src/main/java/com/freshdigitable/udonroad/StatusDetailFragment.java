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
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.freshdigitable.udonroad.OnSpanClickListener.SpanItem;
import com.freshdigitable.udonroad.databinding.FragmentStatusDetailBinding;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.listitem.StatusDetailView;
import com.freshdigitable.udonroad.listitem.StatusListItem;
import com.freshdigitable.udonroad.listitem.StatusListItem.TextType;
import com.freshdigitable.udonroad.listitem.StatusListItem.TimeTextType;
import com.freshdigitable.udonroad.listitem.StatusViewImageHelper;
import com.freshdigitable.udonroad.listitem.TwitterReactionContainer.ReactionIcon;
import com.freshdigitable.udonroad.media.MediaViewActivity;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.repository.ImageRepository;
import com.freshdigitable.udonroad.subscriber.StatusRequestWorker;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import twitter4j.Status;
import twitter4j.User;

import static android.view.View.GONE;
import static com.freshdigitable.udonroad.Utils.getBindingStatus;

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
  ImageRepository imageRepository;
  private Disposable statusSubscription;
  private Disposable cardSubscription;
  private Disposable iconSubscription;
  private Disposable cardSummaryImageSubs;

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
    if (binding == null) {
      binding = DataBindingUtil.inflate(inflater, R.layout.fragment_status_detail, container, false);
    }
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

    setupActionToolbar(statusId);

    final StatusDetailView statusView = binding.statusView;
    final StatusListItem item = new StatusListItem(status, TextType.DETAIL, TimeTextType.ABSOLUTE);
    if (!Utils.isSubscribed(iconSubscription)) {
      iconSubscription = StatusViewImageHelper.load(item, statusView, imageRepository);
    }

    final User user = item.getUser();
    final ImageView icon = statusView.getIcon();

    final OnUserIconClickedListener userIconClickedListener = createUserIconClickedListener();
    final long rtUserId = item.getRetweetUser().getId();
    statusView.getRtUser().setOnClickListener(
        view -> UserInfoActivity.start(view.getContext(), rtUserId));
    icon.setOnClickListener(
        view -> userIconClickedListener.onUserIconClicked(view, user));
    statusView.getUserName().setOnClickListener(
        view -> userIconClickedListener.onUserIconClicked(icon, user));
    statusView.getThumbnailContainer().setOnMediaClickListener(
        (view, index) -> MediaViewActivity.start(view.getContext(), item, index));

    statusView.bind(item);
    updateFabMenuItem(item);
    final List<SpanItem> spanItems = item.createSpanItems();
    final FragmentActivity activity = getActivity();
    final OnSpanClickListener spanClickListener = activity instanceof OnSpanClickListener ?
        (OnSpanClickListener) activity : (v, i) -> {};
    if (!spanItems.isEmpty()) {
      statusView.setClickableItems(spanItems, (v, si) -> {
        if (si.getType() == SpanItem.TYPE_URL) {
          new URLSpan(si.getQuery()).onClick(v);
        } else if (si.getType() == SpanItem.TYPE_MENTION) {
          UserInfoActivity.start(v.getContext(), si.getId());
        } else if (si.getType() == SpanItem.TYPE_HASHTAG) {
          spanClickListener.onSpanClicked(v, si);
        }
      });
    }
    statusSubscription = statusCache.observeById(statusId)
        .map(s -> new StatusListItem(s, TextType.DETAIL, TimeTextType.ABSOLUTE))
        .subscribe(listItem -> {
              statusView.update(listItem);
              updateFabMenuItem(listItem);
            },
            e -> Log.e(TAG, "onStart: ", e));

    final Status bindingStatus = getBindingStatus(status);
    if (bindingStatus.getURLEntities().length < 1) {
      return;
    }
    if (twitterCard != null) {
      setupTwitterCard(twitterCard);
    } else {
      final String expandedURL = bindingStatus.getURLEntities()[0].getExpandedURL();
      cardSubscription = TwitterCard.observeFetch(expandedURL)
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(this::setupTwitterCard,
              throwable -> {
                Log.e(TAG, "card fetch: ", throwable);
                setupTwitterCard(new TwitterCard());
              });
    }
  }

  private TwitterCard twitterCard;

  private void setupTwitterCard(@NonNull final TwitterCard twitterCard) {
    this.twitterCard = twitterCard;
    if (!this.twitterCard.isValid()) {
      return;
    }
    binding.sdTwitterCard.setVisibility(View.VISIBLE);
    binding.sdTwitterCard.bindData(this.twitterCard);
    final String imageUrl = this.twitterCard.getImageUrl();
    if (!TextUtils.isEmpty(imageUrl) && !Utils.isSubscribed(cardSummaryImageSubs)) {
      cardSummaryImageSubs = imageRepository.querySquareImage(imageUrl, R.dimen.card_summary_image, getStatusId())
          .subscribe(d -> binding.sdTwitterCard.getImage().setImageDrawable(d));
    }

    final Intent intent = new Intent(Intent.ACTION_VIEW);
    final String appUrl = this.twitterCard.getAppUrl();
    if (!TextUtils.isEmpty(appUrl)) {
      intent.setData(Uri.parse(appUrl));
      final ComponentName componentName = intent.resolveActivity(getContext().getPackageManager());
      if (componentName == null) {
        intent.setData(Uri.parse(this.twitterCard.getUrl()));
      }
    } else {
      intent.setData(Uri.parse(this.twitterCard.getUrl()));
    }
    binding.sdTwitterCard.setOnClickListener(view -> view.getContext().startActivity(intent));
  }

  private void updateFabMenuItem(final StatusListItem status) {
    final FragmentActivity activity = getActivity();
    if (!(activity instanceof FabHandleable)) {
      return;
    }
    final FabHandleable fabHandleable = (FabHandleable) activity;
    for (ListItem.Stat stat : status.getStats()) {
      final int type = stat.getType();
      if (type == ReactionIcon.RETWEET.type) {
        fabHandleable.setCheckedFabMenuItem(R.id.iffabMenu_main_rt, stat.isMarked());
      } else if (type == ReactionIcon.FAV.type) {
        fabHandleable.setCheckedFabMenuItem(R.id.iffabMenu_main_fav, stat.isMarked());
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (getActivity() instanceof FabHandleable) {
      final FabHandleable fabHandleable = (FabHandleable) getActivity();
      fabHandleable.showFab(FabHandleable.TYPE_TOOLBAR);
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    tearDownActionToolbar();
    binding.statusView.getRtUser().setOnClickListener(null);
    binding.statusView.getIcon().setOnClickListener(null);
    binding.statusView.getUserName().setOnClickListener(null);
    binding.statusView.getThumbnailContainer().setOnMediaClickListener(null);
    binding.sdTwitterCard.setOnClickListener(null);
    Utils.maybeDispose(statusSubscription);
    Utils.maybeDispose(cardSubscription);
    statusCache.close();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    Utils.maybeDispose(iconSubscription);
    Utils.maybeDispose(cardSummaryImageSubs);
    binding.statusView.reset();
    binding.sdTwitterCard.setVisibility(GONE);
    twitterCard = null;
  }

  private OnUserIconClickedListener createUserIconClickedListener() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof OnUserIconClickedListener) {
      return (OnUserIconClickedListener) activity;
    } else {
      return (view, user) -> UserInfoActivity.start(activity, user, view);
    }
  }

  @Inject
  StatusRequestWorker statusRequestWorker;
  private IndicatableFFAB.OnIffabItemSelectedListener onIffabItemSelectedListener;

  private void setupActionToolbar(long statusId) {
    if (getActivity() instanceof FabHandleable) {
      final FabHandleable fabHandleable = (FabHandleable) getActivity();
      onIffabItemSelectedListener = statusRequestWorker.getOnIffabItemSelectedListener(statusId);
      fabHandleable.addOnItemSelectedListener(onIffabItemSelectedListener);
    }
  }

  private void tearDownActionToolbar() {
    if (getActivity() instanceof FabHandleable) {
      final FabHandleable fabHandleable = (FabHandleable) getActivity();
      fabHandleable.removeOnItemSelectedListener(onIffabItemSelectedListener);
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

  long getStatusId() {
    return (long) getArguments().get("statusId");
  }
}

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

package com.freshdigitable.udonroad.timeline;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import com.freshdigitable.udonroad.AppViewModelProviderFactory;
import com.freshdigitable.udonroad.FabViewModel;
import com.freshdigitable.udonroad.ItemSelectable;
import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.TimelineContainerSwitcher;
import com.freshdigitable.udonroad.TimelineContainerSwitcher.ContentType;
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.databinding.FragmentTimelineBinding;
import com.freshdigitable.udonroad.datastore.UpdateEvent;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.listitem.ListsListItem;
import com.freshdigitable.udonroad.listitem.OnItemViewClickListener;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.listitem.StatusViewHolder;
import com.freshdigitable.udonroad.subscriber.StatusRequestWorker;
import com.freshdigitable.udonroad.user.UserInfoPagerFragment;

import java.util.EnumSet;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

/**
 * TimelineFragment provides RecyclerView to show timeline.
 *
 * Created by Akihit.
 */
public class TimelineFragment extends Fragment implements ItemSelectable {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineFragment.class.getSimpleName();
  private static final String SS_TOP_ITEM_ID = "ss_topItemId";
  private static final String SS_TOP_ITEM_TOP = "ss_topItemTop";
  private static final String SS_AUTO_SCROLL_STATE = "ss_auto_scroll_state";
  private static final String SS_SELECTED_ITEM = "ss_selectedItem";
  private FragmentTimelineBinding binding;
  protected TimelineAdapter tlAdapter;
  private LinearLayoutManager tlLayoutManager;
  private Disposable updateEventSubscription;
  private TimelineDecoration timelineDecoration;
  private TimelineAnimator timelineAnimator;
  private MenuItem heading;
  private FabViewModel fabViewModel;
  @Inject
  StatusRequestWorker requestWorker;
  @Inject
  AppViewModelProviderFactory viewModelFactory;
  private TimelineViewModel timelineViewModel;
  private Disposable timestampSubscription;

  @Override
  public void onAttach(Context context) {
    AndroidSupportInjection.inject(this);
    super.onAttach(context);
    timelineViewModel = TimelineViewModel.getInstance(this, viewModelFactory);
    timelineViewModel.init(getStoreType(), getEntityId(), getQuery());
    updateEventSubscription = timelineViewModel.observeUpdateEvent()
        .retry()
        .doOnSubscribe(subs -> Timber.tag(TAG).d("onAttach: updateEvent is subscribed"))
        .subscribe(event -> {
              if (event.type == UpdateEvent.EventType.INSERT) {
                tlAdapter.notifyItemRangeInserted(event.index, event.length);
              } else if (event.type == UpdateEvent.EventType.CHANGE) {
                tlAdapter.notifyItemRangeChanged(event.index, event.length);
              } else if (event.type == UpdateEvent.EventType.DELETE) {
                tlAdapter.notifyItemRangeRemoved(event.index, event.length);
              }
              updateCreatedAt();
            },
            e -> Timber.tag(TAG).e(e, "updateEvent: "));
    tlAdapter = timelineViewModel.createAdapter();
  }

  private void updateCreatedAt() {
    final int childCount = binding.timeline.getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View v = binding.timeline.getChildAt(i);
      final RecyclerView.ViewHolder vh = binding.timeline.getChildViewHolder(v);
      if (!(vh instanceof StatusViewHolder)) {
        continue;
      }
      final StatusViewHolder viewHolder = (StatusViewHolder) vh;
      viewHolder.updateTime();
    }
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.timeline, menu);
    heading = menu.findItem(R.id.action_heading);
    switchHeadingEnabled();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final int itemId = item.getItemId();
    if (itemId == R.id.action_heading) {
      scrollToTop();
    }
    return false;
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    Timber.tag(TAG).d("onCreateView: %s", getStoreName());
    if (savedInstanceState != null) {
      autoScrollState = savedInstanceState.getParcelable(SS_AUTO_SCROLL_STATE);
      final SelectedItem selectedItem = savedInstanceState.getParcelable(SS_SELECTED_ITEM);
      timelineViewModel.setSelectedItem(selectedItem);
    }
    binding = FragmentTimelineBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    binding.timeline.setHasFixedSize(true);
    binding.timeline.setAdapter(tlAdapter);

    tlLayoutManager = new LinearLayoutManager(getContext());
    binding.timeline.setLayoutManager(tlLayoutManager);

    if (timelineDecoration == null) {
      timelineDecoration = new TimelineDecoration(view.getContext());
    }
    binding.timeline.addItemDecoration(timelineDecoration);

    if (timelineAnimator == null) {
      timelineAnimator = new TimelineAnimator();
    }
    binding.timeline.setItemAnimator(timelineAnimator);

    binding.timelineSwipeLayout.setColorSchemeResources(R.color.accent, R.color.twitter_primary,
        R.color.twitter_action_retweeted, R.color.twitter_action_faved);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    Timber.tag(TAG).d("onSaveInstanceState: %s", getStoreName());
    super.onSaveInstanceState(outState);
    outState.putParcelable(SS_AUTO_SCROLL_STATE, autoScrollState);
    outState.putLong(SS_TOP_ITEM_ID, topItemId);
    outState.putInt(SS_TOP_ITEM_TOP, firstVisibleItemTopOnStop);
    outState.putParcelable(SS_SELECTED_ITEM, timelineViewModel.getSelectedItem().getValue());
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    Timber.tag(TAG).d("onActivityCreated: %s", getStoreName());
    super.onActivityCreated(savedInstanceState);
    if (getUserVisibleHint()) {
      timelineViewModel.getInitList();
    }
  }

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    super.setUserVisibleHint(isVisibleToUser);
    if (isVisibleToUser && timelineViewModel != null) {
      timelineViewModel.getInitList();
    }
  }

  private final AdapterDataObserver itemInsertedObserver
      = new AdapterDataObserver() {
    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
      super.onItemRangeInserted(positionStart, itemCount);
      if (positionStart != 0) {
        return;
      }
      if (canAutoScroll()) {
//        Log.d(TAG, "onItemRangeInserted: ");
        scrollTo(0);
      } else {
        addAutoScrollStopper(AutoScrollStopper.ADDED_UNTIL_STOPPED);
      }
    }
  };

  private final RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
      if (newState == RecyclerView.SCROLL_STATE_IDLE) {
        final int firstVisibleItemPosition = tlLayoutManager.findFirstVisibleItemPosition();
        if (firstVisibleItemPosition != 0) {
          addAutoScrollStopper(AutoScrollStopper.SCROLLED_BY_USER);
        } else {
          removeAutoScrollStopper(AutoScrollStopper.SCROLLED_BY_USER);
        }
        // if first visible item is updated, setAddedUntilStopped also should be updated.
        setAddedUntilStopped();
      } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
        addAutoScrollStopper(AutoScrollStopper.SCROLLED_BY_USER);
      }
    }
  };

  private long topItemId = -1;
  private int firstVisibleItemTopOnStop;

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    Timber.tag(TAG).d("onViewStateRestored: %s", getStoreName());
    super.onViewStateRestored(savedInstanceState);
    if (savedInstanceState == null) {
      return;
    }
    topItemId = savedInstanceState.getLong(SS_TOP_ITEM_ID, -1);
    firstVisibleItemTopOnStop = savedInstanceState.getInt(SS_TOP_ITEM_TOP, 0);
  }

  @Override
  public void onStart() {
    Timber.tag(TAG).d("onStart: %s", getStoreName());
    super.onStart();
    if (topItemId >= 0) {
      final int pos = timelineViewModel.getPositionById(topItemId);
      tlLayoutManager.scrollToPositionWithOffset(pos, firstVisibleItemTopOnStop);
      topItemId = -1;
      if (pos > 0) {
        addAutoScrollStopper(AutoScrollStopper.ADDED_UNTIL_STOPPED);
      }
    }
    binding.timeline.addOnScrollListener(onScrollListener);

    fabViewModel = ViewModelProviders.of(getActivity()).get(FabViewModel.class);

    timelineViewModel.getSelectedItem().observe(this, item -> {
      if (item == SelectedItem.NONE || item == null) {
        hideFab();
        removeAutoScrollStopper(AutoScrollStopper.ITEM_SELECTED);
      } else {
        showFab();
        addAutoScrollStopper(AutoScrollStopper.ITEM_SELECTED);
      }
    });
    tlAdapter.setLastItemBoundListener(() -> timelineViewModel.getListOnEnd());
    final OnUserIconClickedListener userIconClickedListener = createUserIconClickedListener();
    tlAdapter.setOnUserIconClickedListener(userIconClickedListener);
    tlAdapter.registerAdapterDataObserver(itemInsertedObserver);
    tlAdapter.setOnItemViewClickListener(createOnItemViewClickListener());
  }

  @Override
  public void onResume() {
    super.onResume();
    if (!isChildOfViewPager() || isVisibleOnViewPager()) {
      if (timelineViewModel.isItemSelected()) {
        showFab();
      } else {
        hideFab();
      }
    }
    final SwipeRefreshLayout swipeLayout = binding.timelineSwipeLayout;
    swipeLayout.setOnRefreshListener(() -> {
      timelineViewModel.getListOnStart();
      swipeLayout.setRefreshing(false);
    });
    fabViewModel.getMenuItem().observe(this, getMenuItemObserver());
  }

  protected Observer<MenuItem> getMenuItemObserver() {
    return item -> {
      if (item == null) {
        return;
      }
      requestWorker.getOnIffabItemSelectedListener(getSelectedItemId()).onItemSelected(item);
    };
  }

  private boolean isChildOfViewPager() {
    final Fragment parent = getParentFragment();
    return parent instanceof UserInfoPagerFragment;
  }

  private boolean isVisibleOnViewPager() {
    final Fragment parent = getParentFragment();
    final UserInfoPagerFragment pager = (UserInfoPagerFragment) parent;
    return pager != null && this == pager.getCurrentFragment();
  }

  @Override
  public void onPause() {
    super.onPause();
    int adapterPos = tlLayoutManager.findFirstVisibleItemPosition();
    if (adapterPos >= 0) {
      final RecyclerView.ViewHolder vh = binding.timeline.findViewHolderForAdapterPosition(adapterPos);
      if (vh != null) {
        topItemId = timelineViewModel.getItemIdByPosition(adapterPos);
        firstVisibleItemTopOnStop = vh.itemView.getTop();
      }
    }
    binding.timelineSwipeLayout.setOnRefreshListener(null);
    fabViewModel.getMenuItem().removeObservers(this);
  }

  @Override
  public void onStop() {
    super.onStop();
    tlAdapter.setLastItemBoundListener(null);
    tlAdapter.setOnUserIconClickedListener(null);
    binding.timeline.setOnTouchListener(null);
    binding.timeline.removeOnScrollListener(onScrollListener);
    tlAdapter.unregisterAdapterDataObserver(itemInsertedObserver);
    tlAdapter.setOnItemViewClickListener(null);
  }

  @Override
  public void onDetach() {
    Timber.tag(TAG).d("onDetach: %s", getStoreName());
    super.onDetach();
    if (binding != null) {
      binding.timeline.setItemAnimator(null);
      timelineAnimator = null;
      if (!binding.timeline.isComputingLayout()) {
        binding.timeline.removeItemDecoration(timelineDecoration);
        timelineDecoration = null;
      }
      tlLayoutManager.removeAllViews();
      binding.timeline.setLayoutManager(null);
      tlLayoutManager = null;
      binding.timeline.setAdapter(null);
      tlAdapter = null;
    }
    Utils.maybeDispose(updateEventSubscription);
    Utils.maybeDispose(timestampSubscription);
  }

  private void showFab() {
    fabViewModel.showFab(FabViewModel.Type.FAB);
  }

  private void hideFab() {
    fabViewModel.hideFab();
  }

  private AutoScrollState autoScrollState = new AutoScrollState();

  private void setAddedUntilStopped() {
    if (tlLayoutManager.getChildCount() > 0
        && tlLayoutManager.findFirstVisibleItemPosition() != 0) {
      addAutoScrollStopper(AutoScrollStopper.ADDED_UNTIL_STOPPED);
    } else {
      removeAutoScrollStopper(AutoScrollStopper.ADDED_UNTIL_STOPPED);
    }
  }

  public void stopScroll() {
    addAutoScrollStopper(AutoScrollStopper.STOP_SCROLL);
  }

  public void startScroll() {
    removeAutoScrollStopper(AutoScrollStopper.STOP_SCROLL);
  }

  private boolean canAutoScroll() {
    return isVisible() && autoScrollState.isAutoScrollEnabled();
  }

  public void scrollToTop() {
    clearSelectedItem();
    binding.timeline.setLayoutFrozen(false);
    enableAutoScroll();
    scrollTo(0);
  }

  private enum AutoScrollStopper {
    STOP_SCROLL, SCROLLED_BY_USER, ADDED_UNTIL_STOPPED, ITEM_SELECTED
  }

  private void addAutoScrollStopper(AutoScrollStopper flag) {
    autoScrollState.autoScrollFlags.add(flag);
    switchHeadingEnabled();
  }

  private void removeAutoScrollStopper(AutoScrollStopper flag) {
    autoScrollState.autoScrollFlags.remove(flag);
    switchHeadingEnabled();
  }

  private void enableAutoScroll() {
    autoScrollState.autoScrollFlags.clear();
    switchHeadingEnabled();
  }

  private void switchHeadingEnabled() {
    if (heading != null) {
      heading.setEnabled(!canAutoScroll());
    }
  }

  private static class AutoScrollState implements Parcelable {
    private final EnumSet<AutoScrollStopper> autoScrollFlags;

    AutoScrollState() {
      autoScrollFlags = EnumSet.noneOf(AutoScrollStopper.class);
    }

    private boolean isAutoScrollEnabled() {
      return autoScrollFlags.isEmpty();
    }

    AutoScrollState(Parcel in) {
      this.autoScrollFlags = (EnumSet<AutoScrollStopper>) in.readSerializable();
    }

    public static final Creator<AutoScrollState> CREATOR = new Creator<AutoScrollState>() {
      @Override
      public AutoScrollState createFromParcel(Parcel in) {
        return new AutoScrollState(in);
      }

      @Override
      public AutoScrollState[] newArray(int size) {
        return new AutoScrollState[size];
      }
    };

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeSerializable(autoScrollFlags);
    }
  }

  public void scrollToSelectedItem() {
    binding.timeline.setLayoutFrozen(false);
    tlLayoutManager.scrollToPositionWithOffset(timelineViewModel.getSelectedItemViewPosition(), 0);
  }

  private void scrollTo(int position) {
//    Log.d(TAG, "scrollTo: " + position);
    final int firstVisibleItemPosition = tlLayoutManager.findFirstVisibleItemPosition();
    if (firstVisibleItemPosition - position < 4) {
      binding.timeline.smoothScrollToPosition(position);
    } else {
      binding.timeline.scrollToPosition(position + 3);
      binding.timeline.smoothScrollToPosition(position);
    }
  }

  @Override
  public void clearSelectedItem() {
    timelineViewModel.clearSelectedItem();
  }

  @Override
  public boolean isItemSelected() {
    return timelineViewModel != null && timelineViewModel.isItemSelected();
  }

  @Override
  public long getSelectedItemId() {
    return timelineViewModel.getSelectedItemId();
  }

  private OnUserIconClickedListener createUserIconClickedListener() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof OnUserIconClickedListener) {
      return ((OnUserIconClickedListener) activity);
    } else {
      return (view, user) -> { /* nop */ };
    }
  }

  public void dropCache() {
    timelineViewModel.drop();
  }

  public interface OnItemClickedListener {
    void onItemClicked(ContentType type, long id, String query);
  }

  private static final String ARGS_STORE_NAME = "store_name";
  private static final String ARGS_ENTITY_ID = "entity_id";
  private static final String ARGS_QUERY = "query";

  public static Bundle createArgs(StoreType storeType, long entityId, String query) {
    final Bundle args = new Bundle();
    args.putSerializable(ARGS_STORE_NAME, storeType);
    if (entityId > 0) {
      args.putLong(ARGS_ENTITY_ID, entityId);
    }
    args.putString(ARGS_QUERY, query);
    return args;
  }

  public static TimelineFragment getInstance(StoreType storeType) {
    return getInstance(storeType, -1);
  }

  public static TimelineFragment getInstance(StoreType storeType, long entityId) {
    return getInstance(storeType, entityId, "");
  }

  public static TimelineFragment getInstance(StoreType storeType, String query) {
    if (!storeType.isForStatus()) {
      throw new IllegalArgumentException("not capable StoreType: " + storeType);
    }
    return getInstance(storeType, -1, query);
  }

  private static TimelineFragment getInstance(StoreType type, long id, String query) {
    if (!type.isForStatus() && !type.isForUser() && !type.isForLists()) {
      throw new IllegalArgumentException("not capable StoreType: " + type);
    }
    final TimelineFragment timelineFragment = new TimelineFragment();
    final Bundle args = createArgs(type, id, query);
    timelineFragment.setArguments(args);
    return timelineFragment;
  }

  public String getStoreName() {
    return getStoreType().nameWithSuffix(getEntityId(), getQuery());
  }

  private StoreType getStoreType() {
    return (StoreType) getArguments().getSerializable(ARGS_STORE_NAME);
  }

  private long getEntityId() {
    return getArguments().getLong(ARGS_ENTITY_ID, -1);
  }

  private String getQuery() {
    return getArguments().getString(ARGS_QUERY, "");
  }

  protected OnItemViewClickListener createOnItemViewClickListener() {
    final StoreType storeType = getStoreType();
    if (storeType.isForUser()) {
      final OnUserIconClickedListener userIconClickedListener = createUserIconClickedListener();
      return (viewHolder, itemId, clickedView) -> {
        final ListItem user = timelineViewModel.findById(itemId);
        userIconClickedListener.onUserIconClicked(viewHolder.getUserIcon(), user.getUser());
      };
    } else if (storeType.isForLists()) {
      return (viewHolder, itemId, clickedView) -> {
        final FragmentActivity activity = getActivity();
        if (!(activity instanceof TimelineFragment.OnItemClickedListener)) {
          return;
        }
        final OnItemClickedListener listener = (OnItemClickedListener) activity;
        final ListsListItem item = ((ListsListItem) timelineViewModel.findById(itemId));
        listener.onItemClicked(ContentType.LISTS, itemId, item.getCombinedName().getName());
      };
    } else if (storeType.isForStatus()) {
      return null;
    }
    throw new IllegalStateException("not capable of StoreType: " + storeType);
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    final Animation animation = TimelineContainerSwitcher.makeSwitchingAnimation(getContext(), transit, enter);
    return animation != null ? animation
        : super.onCreateAnimation(transit, enter, nextAnim);
  }
}

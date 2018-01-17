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

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.databinding.DataBindingUtil;
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

import com.freshdigitable.udonroad.TimelineAdapter.OnSelectedItemChangeListener;
import com.freshdigitable.udonroad.TimelineContainerSwitcher.ContentType;
import com.freshdigitable.udonroad.databinding.FragmentTimelineBinding;
import com.freshdigitable.udonroad.datastore.UpdateEvent;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.listitem.ListsListItem;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.listitem.StatusView;
import com.freshdigitable.udonroad.listitem.StatusViewImageLoader;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.subscriber.StatusRequestWorker;
import com.freshdigitable.udonroad.timeline.repository.ListItemRepository;
import com.freshdigitable.udonroad.timeline.repository.ListItemRepositoryProvider;

import java.util.EnumSet;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;
import timber.log.Timber;

/**
 * TimelineFragment provides RecyclerView to show timeline.
 *
 * Created by Akihit.
 */
public abstract class TimelineFragment extends Fragment implements ItemSelectable {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineFragment.class.getSimpleName();
  private static final String SS_DONE_FIRST_FETCH = "ss_doneFirstFetch";
  private static final String SS_TOP_ITEM_ID = "ss_topItemId";
  private static final String SS_TOP_ITEM_TOP = "ss_topItemTop";
  private static final String SS_ADAPTER = "ss_adapter";
  private static final String SS_AUTO_SCROLL_STATE = "ss_auto_scroll_state";
  private FragmentTimelineBinding binding;
  TimelineAdapter tlAdapter;
  private LinearLayoutManager tlLayoutManager;
  private Disposable updateEventSubscription;
  private TimelineDecoration timelineDecoration;
  private TimelineAnimator timelineAnimator;
  private MenuItem heading;
  @Inject
  StatusViewImageLoader imageLoader;
  private FabViewModel fabViewModel;
  @Inject
  ListItemRepositoryProvider repositoryProvider;
  ListItemRepository repository;
  @Inject
  StatusRequestWorker requestWorker;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    InjectionUtil.getComponent(this).inject(this);
    repository = repositoryProvider.get(getStoreType()).get();
    repository.init(getEntityId(), getQuery());
    updateEventSubscription = repository.observeUpdateEvent()
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
            },
            e -> Timber.tag(TAG).e(e, "updateEvent: "));
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
      doneFirstFetch = savedInstanceState.getBoolean(SS_DONE_FIRST_FETCH);
      tlAdapter.onRestoreInstanceState(savedInstanceState.getParcelable(SS_ADAPTER));
    }
    return inflater.inflate(R.layout.fragment_timeline, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    binding = DataBindingUtil.bind(view);
    binding.timeline.setHasFixedSize(true);
    binding.timeline.setAdapter(tlAdapter);

    tlLayoutManager = new LinearLayoutManager(getContext());
    tlLayoutManager.setAutoMeasureEnabled(true);
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
    outState.putBoolean(SS_DONE_FIRST_FETCH, doneFirstFetch);
    outState.putLong(SS_TOP_ITEM_ID, topItemId);
    outState.putInt(SS_TOP_ITEM_TOP, firstVisibleItemTopOnStop);
    outState.putParcelable(SS_ADAPTER, tlAdapter.onSaveInstanceState());
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    Timber.tag(TAG).d("onActivityCreated: %s", getStoreName());
    super.onActivityCreated(savedInstanceState);
    if (!doneFirstFetch && getUserVisibleHint()) {
      repository.getInitList();
      doneFirstFetch = true;
    }
  }

  private boolean doneFirstFetch = false;

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    super.setUserVisibleHint(isVisibleToUser);
    if (isVisibleToUser && !doneFirstFetch && repository != null) {
      repository.getInitList();
      doneFirstFetch = true;
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
      final int pos = repository.getPositionById(topItemId);
      tlLayoutManager.scrollToPositionWithOffset(pos, firstVisibleItemTopOnStop);
      topItemId = -1;
      if (pos > 0) {
        addAutoScrollStopper(AutoScrollStopper.ADDED_UNTIL_STOPPED);
      }
    }
    binding.timeline.addOnScrollListener(onScrollListener);

    fabViewModel = ViewModelProviders.of(getActivity()).get(FabViewModel.class);

    tlAdapter.setOnSelectedItemChangeListener(new OnSelectedItemChangeListener() {
      @Override
      public void onItemSelected(long entityId) {
        showFab();
        addAutoScrollStopper(AutoScrollStopper.ITEM_SELECTED);
      }

      @Override
      public void onItemUnselected() {
        hideFab();
        removeAutoScrollStopper(AutoScrollStopper.ITEM_SELECTED);
      }
    });
    tlAdapter.setLastItemBoundListener(() -> repository.getListOnEnd());
    final OnUserIconClickedListener userIconClickedListener = createUserIconClickedListener();
    tlAdapter.setOnUserIconClickedListener(userIconClickedListener);
    tlAdapter.registerAdapterDataObserver(itemInsertedObserver);
  }

  @Override
  public void onResume() {
    super.onResume();
    if (!isChildOfViewPager() || isVisibleOnViewPager()) {
      if (tlAdapter.isItemSelected()) {
        showFab();
      } else {
        hideFab();
      }
    }
    final SwipeRefreshLayout swipeLayout = binding.timelineSwipeLayout;
    swipeLayout.setOnRefreshListener(() -> {
      repository.getInitList();
      swipeLayout.setRefreshing(false);
    });
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
        topItemId = repository.getId(adapterPos);
        firstVisibleItemTopOnStop = vh.itemView.getTop();
      }
    }
    binding.timelineSwipeLayout.setOnRefreshListener(null);
  }

  @Override
  public void onStop() {
    super.onStop();
    removeOnItemSelectedListener();
    tlAdapter.setLastItemBoundListener(null);
    tlAdapter.setOnSelectedItemChangeListener(null);
    tlAdapter.setOnUserIconClickedListener(null);
    binding.timeline.setOnTouchListener(null);
    binding.timeline.removeOnScrollListener(onScrollListener);
    tlAdapter.unregisterAdapterDataObserver(itemInsertedObserver);
  }

  @Override
  public void onDetach() {
    Timber.tag(TAG).d("onDetach: %s", getStoreName());
    super.onDetach();
    if (binding != null) {
      binding.timeline.setItemAnimator(null);
      timelineAnimator = null;
      binding.timeline.removeItemDecoration(timelineDecoration);
      timelineDecoration = null;
      tlLayoutManager.removeAllViews();
      binding.timeline.setLayoutManager(null);
      tlLayoutManager = null;
      binding.timeline.setAdapter(null);
      tlAdapter = null;
    }
    updateEventSubscription.dispose();
    repository.close();
  }

  private OnIffabItemSelectedListener iffabItemSelectedListener;

  private void showFab() {
    fabViewModel.showFab(FabViewModel.Type.FAB);
    fabViewModel.removeOnItemSelectedListener(iffabItemSelectedListener);
    iffabItemSelectedListener = requestWorker.getOnIffabItemSelectedListener(getSelectedItemId());
    fabViewModel.addOnItemSelectedListener(iffabItemSelectedListener);
  }

  private void hideFab() {
    fabViewModel.hideFab();
    removeOnItemSelectedListener();
  }

  private void removeOnItemSelectedListener() {
    fabViewModel.removeOnItemSelectedListener(iffabItemSelectedListener);
    iffabItemSelectedListener = null;
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
    tlLayoutManager.scrollToPositionWithOffset(tlAdapter.getSelectedItemViewPosition(), 0);
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
    tlAdapter.clearSelectedItem();
  }

  @Override
  public boolean isItemSelected() {
    return tlAdapter != null && tlAdapter.isItemSelected();
  }

  @Override
  public long getSelectedItemId() {
    return tlAdapter.getSelectedItemId();
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
    repository.drop();
  }

  interface OnItemClickedListener {
    void onItemClicked(ContentType type, long id, String query);
  }

  private static final String ARGS_STORE_NAME = "store_name";
  private static final String ARGS_ENTITY_ID = "entity_id";
  private static final String ARGS_QUERY = "query";

  static Bundle createArgs(StoreType storeType, long entityId, String query) {
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
    final TimelineFragment fragment;
    if (storeType.isForStatus()) {
      fragment = new StatusListFragment();
    } else if (storeType.isForUser()) {
      fragment = new UserListFragment();
    } else if (storeType.isForLists()) {
      fragment = new ListsListFragment();
    } else {
      throw new IllegalArgumentException("storeType: " + storeType.name() + " is not capable...");
    }
    final Bundle args = TimelineFragment.createArgs(storeType, entityId, "");
    fragment.setArguments(args);
    return fragment;
  }

  public static TimelineFragment getInstance(StoreType storeType, String query) {
    if (!storeType.isForStatus()) {
      throw new IllegalArgumentException("storeType: " + storeType.name() + " is not capable...");
    }
    final StatusListFragment fragment = new StatusListFragment();
    final Bundle args = TimelineFragment.createArgs(storeType, -1, query);
    fragment.setArguments(args);
    return fragment;
  }

  String getStoreName() {
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

  public static class StatusListFragment extends TimelineFragment {
    @Override
    public void onAttach(Context context) {
      super.onAttach(context);
      super.tlAdapter = new TimelineAdapter.StatusTimelineAdapter(repository, imageLoader);
    }

    @Override
    public void onStart() {
      super.onStart();
      super.tlAdapter.registerAdapterDataObserver(createdAtObserver);
    }

    @Override
    public void onStop() {
      super.onStop();
      super.tlAdapter.unregisterAdapterDataObserver(createdAtObserver);
    }

    private final AdapterDataObserver createdAtObserver = new AdapterDataObserver() {
      @Override
      public void onItemRangeChanged(int positionStart, int itemCount) {
        super.onItemRangeChanged(positionStart, itemCount);
        updateTime();
      }

      @Override
      public void onItemRangeInserted(int positionStart, int itemCount) {
        super.onItemRangeInserted(positionStart, itemCount);
        updateTime();
      }

      @Override
      public void onItemRangeRemoved(int positionStart, int itemCount) {
        super.onItemRangeRemoved(positionStart, itemCount);
        updateTime();
      }

      private void updateTime() {
        final int childCount = StatusListFragment.super.binding.timeline.getChildCount();
        for (int i = 0; i < childCount; i++) {
          final View v = StatusListFragment.super.binding.timeline.getChildAt(i);
          if (v instanceof StatusView) {
            ((StatusView) v).updateTime();
          }
        }
      }
    };
  }

  public static class UserListFragment extends TimelineFragment {
    @Override
    public void onAttach(Context context) {
      super.onAttach(context);
      super.tlAdapter = new TimelineAdapter(repository, imageLoader);
    }

    @Override
    public void onStart() {
      super.onStart();
      final OnUserIconClickedListener userIconClickedListener = super.createUserIconClickedListener();
      super.tlAdapter.setOnItemViewClickListener((viewHolder, itemId, clickedView) -> {
        final int pos = repository.getPositionById(itemId);
        final ListItem user = repository.get(pos);
        userIconClickedListener.onUserIconClicked(viewHolder.getUserIcon(), user.getUser());
      });
    }

    @Override
    public void onStop() {
      super.onStop();
      super.tlAdapter.setOnItemViewClickListener(null);
    }
  }

  public static class ListsListFragment extends TimelineFragment {
    @Override
    public void onAttach(Context context) {
      super.onAttach(context);
      super.tlAdapter = new TimelineAdapter(repository, imageLoader);
    }

    @Override
    public void onStart() {
      super.onStart();
      super.tlAdapter.setOnItemViewClickListener((viewHolder, itemId, clickedView) -> {
        final FragmentActivity activity = getActivity();
        if (!(activity instanceof TimelineFragment.OnItemClickedListener)) {
          return;
        }
        final OnItemClickedListener listener = (OnItemClickedListener) activity;
        final int pos = repository.getPositionById(itemId);
        final ListsListItem item = ((ListsListItem) repository.get(pos));
        listener.onItemClicked(ContentType.LISTS, itemId, item.getCombinedName().getName());
      });
    }

    @Override
    public void onStop() {
      super.onStop();
      super.tlAdapter.setOnItemViewClickListener(null);
    }
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    final Animation animation = TimelineContainerSwitcher.makeSwitchingAnimation(getContext(), transit, enter);
    return animation != null ? animation
        : super.onCreateAnimation(transit, enter, nextAnim);
  }
}

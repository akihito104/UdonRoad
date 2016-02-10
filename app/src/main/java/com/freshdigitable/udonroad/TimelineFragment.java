package com.freshdigitable.udonroad;

import android.databinding.DataBindingUtil;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.databinding.FragmentTimelineBinding;

import java.util.List;

import twitter4j.Status;

/**
 * Created by akihit on 2016/01/31.
 */
public class TimelineFragment extends Fragment {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineFragment.class.getSimpleName();
  private FragmentTimelineBinding binding;
  private TimelineAdapter tlAdapter;
  private LinearLayoutManager tlLayoutManager;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_timeline, container, false);
    binding = DataBindingUtil.bind(view);
    return view;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    binding.timeline.setHasFixedSize(true);
    RecyclerView.ItemDecoration itemDecoration = new MyItemDecoration();
    binding.timeline.addItemDecoration(itemDecoration);
    tlLayoutManager = new LinearLayoutManager(getActivity());
    binding.timeline.setLayoutManager(tlLayoutManager);
    binding.timeline.setItemAnimator(new TimelineAnimator());
    tlAdapter = new TimelineAdapter();
    tlAdapter.setOnSelectedTweetChangeListener(selectedTweetChangeListener);
    tlAdapter.setLastItemBoundListener(lastItemBoundListener);
    binding.timeline.setAdapter(tlAdapter);

    binding.fab.setVisibility(View.GONE);
    binding.fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
      }
    });
  }

  private TimelineAdapter.LastItemBoundListener lastItemBoundListener;

  public void setLastItemBoundListener(TimelineAdapter.LastItemBoundListener lastItemBoundListener) {
    this.lastItemBoundListener = lastItemBoundListener;
  }

  private boolean stopScroll = false;

  public void setStopScroll(boolean isStopScroll) {
    stopScroll = isStopScroll;
  }

  private boolean canScrollToAdd() {
    int firstVisibleItem = tlLayoutManager.findFirstVisibleItemPosition();
    return firstVisibleItem == 0
        && !tlAdapter.isStatusViewSelected()
        && !stopScroll;
  }

  public void addNewStatus(Status status) {
    if (canScrollToAdd()) {
      tlAdapter.addNewStatus(status);
      scrollTo(0);
    } else {
      tlAdapter.addNewStatus(status);
    }
  }

  public void addNewStatuses(List<Status> statuses) {
    tlAdapter.addNewStatuses(statuses);
  }

  public void addStatusesAtLast(List<Status> statuses) {
    tlAdapter.addNewStatusesAtLast(statuses);
  }

  public void deleteStatus(long id) {
    tlAdapter.deleteStatus(id);
    tlAdapter.notifyDataSetChanged();
  }

  public void scrollTo(int position) {
    binding.timeline.smoothScrollToPosition(position);
    stopScroll = false;
  }

  public void clearSelectedTweet() {
    tlAdapter.clearSelectedTweet();
  }

  public boolean isTweetSelected() {
    return tlAdapter.isStatusViewSelected();
  }

  public long getSelectedTweetId() {
    return tlAdapter.getSelectedTweetId();
  }

  public FlingableFloatingActionButton getFab() {
    return binding.fab;
  }

  private final TimelineAdapter.OnSelectedTweetChangeListener selectedTweetChangeListener
      = new TimelineAdapter.OnSelectedTweetChangeListener() {
    @Override
    public void onTweetSelected() {
      binding.fab.setVisibility(View.VISIBLE);
    }

    @Override
    public void onTweetUnselected() {
      binding.fab.setVisibility(View.GONE);
    }
  };

  private static class MyItemDecoration extends RecyclerView.ItemDecoration {
    private final Paint paint;
    private final int dividerHeight;

    MyItemDecoration() {
      paint = new Paint(Paint.ANTI_ALIAS_FLAG);
      paint.setColor(Color.GRAY);
      this.dividerHeight = 1;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
      int position = ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewLayoutPosition();
      int top = position == 0 ? 0 : dividerHeight;
      outRect.set(0, top, 0, 0);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
      super.onDraw(c, parent, state);
      final float left = parent.getPaddingLeft();
      final float right = parent.getWidth() - parent.getPaddingRight();
      final int childCount = parent.getChildCount();
      final RecyclerView.LayoutManager manager = parent.getLayoutManager();

      for (int i = 0; i < childCount; i++) {
        final View child = parent.getChildAt(i);
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
        if (params.getViewLayoutPosition() == 0) {
          continue;
        }
        final float top = manager.getDecoratedTop(child) - params.topMargin
            + ViewCompat.getTranslationY(child);
        final float bottom = top + dividerHeight;
        c.drawRect(left, top, right, bottom, paint);
      }
    }
  }
}

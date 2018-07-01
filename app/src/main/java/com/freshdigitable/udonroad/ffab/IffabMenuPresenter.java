/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad.ffab;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.freshdigitable.udonroad.R;

import java.util.List;

import timber.log.Timber;

import static com.freshdigitable.udonroad.ffab.IndicatableFFAB.MODE_FAB;
import static com.freshdigitable.udonroad.ffab.IndicatableFFAB.MODE_SHEET;
import static com.freshdigitable.udonroad.ffab.IndicatableFFAB.MODE_TOOLBAR;

/**
 * IffabMenuPresenter manages iffab view object, FlingableFAB and IndicatorView.
 *
 * Created by akihit on 2017/01/18.
 */

class IffabMenuPresenter {
  private final ActionIndicatorView indicator;
  private final IndicatableFFAB ffab;
  private final int indicatorMargin;
  private final IffabMenu menu;

  private final View bottomSheet;
  private final BottomSheetBehavior<View> bottomSheetBehavior;
  private final BottomButtonsToolbar bbt;

  private final TransformAnimator transformAnimator;
  private final int bottomBarHeight;
  private RecyclerView.Adapter<SheetMenuViewHolder> sheetAdapter;

  IffabMenuPresenter(IndicatableFFAB ffab, AttributeSet attrs, int defStyleAttr) {
    this.ffab = ffab;
    final Context context = ffab.getContext();
    indicator = new ActionIndicatorView(context);
    menu = new IffabMenu(context, this);

    final TypedArray a = context.obtainStyledAttributes(attrs,
        R.styleable.IndicatableFFAB, defStyleAttr, R.style.Widget_FFAB_IndicatableFFAB);
    final boolean toolbarEnabled;
    try {
      final int indicatorTint = a.getColor(R.styleable.IndicatableFFAB_indicatorTint, 0);
      indicator.setBackgroundColor(indicatorTint);
      final int indicatorIconTint = a.getColor(R.styleable.IndicatableFFAB_indicatorIconTint, 0);
      indicator.setIndicatorIconTint(indicatorIconTint);
      this.indicatorMargin = a.getDimensionPixelSize(R.styleable.IndicatableFFAB_marginFabToIndicator, 0);

      toolbarEnabled = a.getBoolean(R.styleable.IndicatableFFAB_enableToolbar, true);

      if (a.hasValue(R.styleable.IndicatableFFAB_menu)) {
        final int menuRes = a.getResourceId(R.styleable.IndicatableFFAB_menu, 0);
        inflateMenu(context, menuRes);
      }
    } finally {
      a.recycle();
    }
    initView();

    if (toolbarEnabled) {
      final ViewGroup parent = (ViewGroup) ffab.getParent();
      bottomSheet = LayoutInflater.from(context).inflate(R.layout.view_bottom_sheet, parent, false);
      bottomSheet.setVisibility(View.INVISIBLE);
      bbt = bottomSheet.findViewById(R.id.iffabSheet_button);
      bbt.setMenu(menu);
      bottomSheetBehavior = new BottomSheetBehavior<>();
      bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
          if (newState == BottomSheetBehavior.STATE_EXPANDED) {
            mode = MODE_SHEET;
          } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
            mode = MODE_TOOLBAR;
          }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
          final View moreIcon = bbt.getMoreIcon();
          moreIcon.setPivotX(moreIcon.getWidth() / 2);
          moreIcon.setPivotY(moreIcon.getHeight() / 2);
          moreIcon.setRotation(180 * slideOffset);
        }
      });
      bottomBarHeight = BottomButtonsToolbar.getHeight(context);
      bbt.setMoreClickListener(v -> {
        Timber.tag("IffabMP").d("IffabMenuPresenter: ");
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
      });
      transformAnimator = TransformAnimator.create(ffab, bottomSheet);

      final Message message = handler.obtainMessage(MSG_LAYOUT_BOTTOM_TOOLBAR, this);
      handler.sendMessage(message);
    } else {
      bottomSheet = null;
      bottomBarHeight = 0;
      bbt = null;
      bottomSheetBehavior = null;
      transformAnimator = null;
    }
  }

  private void inflateMenu(Context context, int menuRes) {
    pendingUpdate = true;
    final IffabMenuItemInflater menuInflater = new IffabMenuItemInflater(context);
    menuInflater.inflate(menuRes, menu);
    pendingUpdate = false;
    updateMenu();
  }

  private void initView() {
    ffab.setOnFlickListener(new OnFlickListener() {
      private Direction prevSelected = Direction.UNDEFINED;

      @Override
      public void onStart() {
        indicator.onActionLeave(prevSelected);
        prevSelected = Direction.UNDEFINED;
        indicator.setVisibility(View.VISIBLE);
      }

      @Override
      public void onMoving(Direction direction) {
        if (prevSelected == direction) {
          return;
        }
        indicator.onActionLeave(prevSelected);
        if (menu.isVisibleByDirection(direction)) {
          indicator.onActionSelected(direction);
        }
        prevSelected = direction;
      }

      @Override
      public void onFlick(Direction direction) {
        menu.dispatchSelectedMenuItem(direction);
        final Message msg = handler.obtainMessage(
            MSG_DISMISS_ACTION_ITEM_VIEW, View.INVISIBLE, 0, indicator);
        handler.sendMessageDelayed(msg, 200);
      }

      @Override
      public void onCancel() {
        indicator.onActionLeave(prevSelected);
        prevSelected = Direction.UNDEFINED;
        indicator.setVisibility(View.INVISIBLE);
      }
    });

    ViewCompat.setElevation(indicator, ffab.getCompatElevation());
  }

  void clear() {
    if (indicator != null) {
      indicator.clear();
    }
    if (bbt != null) {
      bbt.clear();
    }
  }

  void updateMenu() {
    if (pendingUpdate) {
      return;
    }
    final List<IffabMenuItem> items = menu.getEnableItems();
    for (IffabMenuItem i : items) {
      indicator.setDrawable(i.getDirection(), i.getIcon());
    }
    if (bbt != null) {
      bbt.updateItems();
    }
    if (sheetAdapter != null) {
      sheetAdapter.notifyDataSetChanged();
    }
  }

  private boolean pendingUpdate = false;

  private static final int MSG_LAYOUT_ACTION_ITEM_VIEW = 1;
  private static final int MSG_DISMISS_ACTION_ITEM_VIEW = 2;
  private static final int MSG_LAYOUT_BOTTOM_TOOLBAR = 3;

  private final Handler handler = new Handler(Looper.getMainLooper(), msg -> {
    if (msg.what == MSG_LAYOUT_ACTION_ITEM_VIEW) {
      ((IffabMenuPresenter) msg.obj).layoutActionItemView();
      return true;
    }
    if (msg.what == MSG_DISMISS_ACTION_ITEM_VIEW) {
      ((ActionIndicatorView) msg.obj).setVisibility(msg.arg1);
      return true;
    }
    if (msg.what == MSG_LAYOUT_BOTTOM_TOOLBAR) {
      ((IffabMenuPresenter) msg.obj).layoutToolbar();
      return true;
    }
    return false;
  });

  void onFabAttachedToWindow() {
    if (indicator.getParent() != null) {
      return;
    }
    final Message msg = handler.obtainMessage(MSG_LAYOUT_ACTION_ITEM_VIEW, this);
    handler.sendMessage(msg);
  }

  private void layoutActionItemView() {
    final ViewGroup.LayoutParams baseLp = indicator.generateDefaultLayoutParams();
    final ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) ffab.getLayoutParams();
    if (mlp instanceof FrameLayout.LayoutParams) {
      final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(baseLp);
      lp.gravity = ((FrameLayout.LayoutParams) mlp).gravity;
      setIndicatorMarginFromEdge(lp, lp.gravity);
      ((ViewGroup) ffab.getParent()).addView(indicator, lp);
    } else if (mlp instanceof CoordinatorLayout.LayoutParams) {
      final CoordinatorLayout.LayoutParams lp = new CoordinatorLayout.LayoutParams(baseLp);
      lp.gravity = ((CoordinatorLayout.LayoutParams) mlp).gravity;
      setIndicatorMarginFromEdge(lp, lp.gravity);
      ((ViewGroup) ffab.getParent()).addView(indicator, lp);
    }
  }

  private void setIndicatorMarginFromEdge(ViewGroup.MarginLayoutParams mlp, int gravity) {
    final ViewGroup.MarginLayoutParams ffabLp = (ViewGroup.MarginLayoutParams) ffab.getLayoutParams();
    if (hasGravityFlagOf(Gravity.BOTTOM, gravity)) {
      mlp.bottomMargin = ffabLp.bottomMargin + mlp.height + indicatorMargin;
    } else if (hasGravityFlagOf(Gravity.TOP, gravity)) {
      mlp.topMargin = ffabLp.topMargin + mlp.height + indicatorMargin;
    } else if (hasGravityFlagOf(Gravity.RIGHT, gravity)) {
      mlp.rightMargin = ffabLp.rightMargin + mlp.width + indicatorMargin;
    } else if (hasGravityFlagOf(Gravity.LEFT, gravity)) {
      mlp.leftMargin = ffabLp.leftMargin + mlp.width + indicatorMargin;
    } else if (hasGravityFlagOf(Gravity.START, gravity)) {
      MarginLayoutParamsCompat.setMarginStart(
          mlp, MarginLayoutParamsCompat.getMarginStart(ffabLp) + mlp.width + indicatorMargin);
    } else if (hasGravityFlagOf(Gravity.END, gravity)) {
      MarginLayoutParamsCompat.setMarginEnd(
          mlp, MarginLayoutParamsCompat.getMarginEnd(ffabLp) + mlp.width + indicatorMargin);
    }
  }

  private static boolean hasGravityFlagOf(int expected, int actual) {
    return (expected & actual) == expected;
  }

  private void layoutToolbar() {
    final ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) ffab.getLayoutParams();
    if (layoutParams instanceof CoordinatorLayout.LayoutParams) {
      final CoordinatorLayout.LayoutParams mlp = new CoordinatorLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
      mlp.gravity = ((CoordinatorLayout.LayoutParams) layoutParams).gravity;
      bottomSheetBehavior.setPeekHeight(bottomBarHeight);
      bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
      mlp.setBehavior(bottomSheetBehavior);
      setupBottomSheet();
      ((ViewGroup) ffab.getParent()).addView(bottomSheet, mlp);
    }
  }

  private void setupBottomSheet() {
    if (bottomSheet == null) {
      return;
    }
    final RecyclerView menuList = bottomSheet.findViewById(R.id.iffabSheet_list);
    menuList.setLayoutManager(new LinearLayoutManager(bottomSheet.getContext()));
    sheetAdapter = new RecyclerView.Adapter<SheetMenuViewHolder>() {
      @Override
      public SheetMenuViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.view_menu_item, parent, false);
        return new SheetMenuViewHolder(v);
      }

      @Override
      public void onBindViewHolder(SheetMenuViewHolder holder, int position) {
        Timber.tag("IffabMP").d("onBindViewHolder: %s", position);
        final MenuItem item = menu.getSheetItem(position);
        holder.icon.setImageDrawable(item.getIcon());
        holder.text.setText(item.getTitle());
      }

      @Override
      public int getItemCount() {
        return menu.sheetSize();
      }

      @Override
      public void onViewAttachedToWindow(SheetMenuViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        final int adapterPosition = holder.getAdapterPosition();
        final int itemId = menu.getSheetItem(adapterPosition).getItemId();
        holder.itemView.setOnClickListener(v -> menu.dispatchSelectedMenuItem(itemId));
      }

      @Override
      public void onViewDetachedFromWindow(SheetMenuViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.setOnClickListener(null);
      }
    };
    menuList.setAdapter(sheetAdapter);
  }

  private static class SheetMenuViewHolder extends RecyclerView.ViewHolder {
    private final ImageView icon;
    private final TextView text;

    SheetMenuViewHolder(View itemView) {
      super(itemView);
      icon = itemView.findViewById(R.id.menuItem_icon);
      text = itemView.findViewById(R.id.menuItem_text);
    }
  }

  private int mode = MODE_FAB;

  int getMode() {
    return mode;
  }

  boolean isFabMode() {
    return mode == MODE_FAB;
  }

  void transToToolbar() {
    if (mode == MODE_TOOLBAR || mode == MODE_SHEET) {
      return;
    }
    updateMenuItemCheckable(true);
    if (bottomSheet.getVisibility() != View.VISIBLE) {
      bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
      transformAnimator.transToToolbar();
    }
    mode = MODE_TOOLBAR;
  }

  private void updateMenuItemCheckable(boolean checkable) {
    final int menuSize = menu.size();
    pendingUpdate = true;
    for (int i = 0; i < menuSize; i++) {
      menu.getItem(i).setCheckable(checkable);
    }
    pendingUpdate = false;
  }

  void transToFAB(int afterVisibility) {
    if (mode == MODE_FAB) {
      if (afterVisibility == View.VISIBLE) {
        ffab.show();
      } else {
        ffab.hide();
      }
    } else if (mode == MODE_SHEET) {
      bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
      mode = MODE_TOOLBAR;
    } else {
      mode = MODE_FAB;
      transformAnimator.transToFab(afterVisibility);
      updateMenuItemCheckable(false);
      updateMenu();
    }
  }

  void hideToolbar() {
    if (bottomSheet.getVisibility() != View.VISIBLE) {
      return;
    }
    bbt.animate()
        .translationY(bbt.getHeight())
        .setInterpolator(new DecelerateInterpolator())
        .setDuration(200)
        .setListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            bbt.setVisibility(View.INVISIBLE);
            bbt.setTranslationY(0);
          }
        })
        .start();
  }

  void showToolbar() {
    if (bottomSheet.getVisibility() == View.VISIBLE) {
      return;
    }
    bbt.setTranslationY(bbt.getHeight());
    bbt.animate()
        .translationY(0)
        .setInterpolator(new AccelerateInterpolator())
        .setDuration(200)
        .setListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            bbt.setVisibility(View.VISIBLE);
            bbt.setTranslationY(0);
          }
        })
        .start();
  }

  IffabMenu getMenu() {
    return menu;
  }

  void onSaveInstanceState(SavedState savedState) {
    savedState.mode = mode;
  }

  void onRestoreInstanceState(SavedState state) {
    mode = state.mode;
    syncState();
  }

  private void syncState() {
    if (mode == MODE_FAB) {
      ffab.setVisibility(View.VISIBLE);
      bottomSheet.setVisibility(View.INVISIBLE);
    } else {
      ffab.setVisibility(View.INVISIBLE);
      bottomSheet.setVisibility(View.VISIBLE);
    }
    updateMenuItemCheckable(mode == MODE_TOOLBAR || mode == MODE_SHEET);
    updateMenu();
  }

  static class SavedState extends View.BaseSavedState {
    int mode;

    SavedState(Parcelable source) {
      super(source);
    }

    private SavedState(Parcel in) {
      super(in);
      mode = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
      super.writeToParcel(out, flags);
      out.writeInt(mode);
    }

    public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {
      @Override
      public SavedState createFromParcel(Parcel parcel) {
        return new SavedState(parcel);
      }

      @Override
      public SavedState[] newArray(int size) {
        return new SavedState[size];
      }
    };
  }
}

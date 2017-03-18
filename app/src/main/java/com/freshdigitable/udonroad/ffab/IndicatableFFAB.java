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

package com.freshdigitable.udonroad.ffab;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;

import com.freshdigitable.udonroad.R;

/**
 * IndicatableFFAB is a view group of FlickableFAB and ActionIndicatorView.
 *
 * Created by akihit on 2016/09/05.
 */
public class IndicatableFFAB extends FlickableFAB {
  private final IffabMenu menu;
  private final IffabMenuPresenter presenter = new IffabMenuPresenter();

  public IndicatableFFAB(Context context) {
    this(context, null);
  }

  public IndicatableFFAB(Context context, AttributeSet attrs) {
    this(context, attrs, R.attr.iffabStyle);
  }

  public IndicatableFFAB(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    presenter.initView(this);
    menu = new IffabMenu(context, presenter);

    final TypedArray a = context.obtainStyledAttributes(attrs,
        R.styleable.IndicatableFFAB, defStyleAttr, R.style.Widget_FFAB_IndicatableFFAB);
    try {
      final int indicatorTint = a.getColor(R.styleable.IndicatableFFAB_indicatorTint, 0);
      presenter.setIndicatorTint(indicatorTint);
      final int indicatorIconTint = a.getColor(R.styleable.IndicatableFFAB_indicatorIconTint, 0);
      presenter.setIndicatorIconTint(indicatorIconTint);
      final int indicatorMargin = a.getDimensionPixelSize(R.styleable.IndicatableFFAB_marginFabToIndicator, 0);
      presenter.setIndicatorMargin(indicatorMargin);

      presenter.initForMenu(menu);
      if (a.hasValue(R.styleable.IndicatableFFAB_menu)) {
        final int menuRes = a.getResourceId(R.styleable.IndicatableFFAB_menu, 0);
        inflateMenu(menuRes);
      }
    } finally {
      a.recycle();
    }

    if (isInEditMode()) {
      presenter.setIndicatorVisibility(VISIBLE);
    }
  }

  private void inflateMenu(int menuRes) {
    presenter.setPendingUpdate(true);
    final IffabMenuItemInflater menuInflater = new IffabMenuItemInflater(getContext());
    menuInflater.inflate(menuRes, menu);
    presenter.setPendingUpdate(false);
    presenter.updateMenu();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    presenter.onFabAttachedToWindow();
  }

  public Menu getMenu() {
    return menu;
  }

  public void setOnIffabItemSelectedListener(OnIffabItemSelectedListener selectedListener) {
    menu.setOnIffabItemSelectedListener(selectedListener);
  }

  public void clear() {
    presenter.clear();
    setOnIffabItemSelectedListener(null);
  }

  public void transToToolbar() {
    presenter.showToolbar();
  }

  public void transToFAB() {
    presenter.hideToolbar();
  }

  public interface OnIffabItemSelectedListener {
    void onItemSelected(@NonNull MenuItem item);
  }
}

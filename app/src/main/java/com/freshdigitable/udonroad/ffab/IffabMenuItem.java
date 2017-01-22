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

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import com.freshdigitable.udonroad.ffab.OnFlingListener.Direction;

import static android.view.View.NO_ID;

/**
 * IffabMenuItem is for menu item of IndicatableFFAB.
 *
 * Created by akihit on 2017/01/11.
 */

class IffabMenuItem implements MenuItem {
  private final IffabMenu menu;
  private final int id;
  private final int groupId;
  private final int order;
  private final Direction direction;

  IffabMenuItem(@NonNull IffabMenu menu, int groupId, int id,
                int order, CharSequence title) {
    this.menu = menu;
    this.id = id;
    this.groupId = groupId;
    this.order = order;
    this.direction = Direction.findByAngle(order);
    this.title = title;
  }

  @Override
  public int getItemId() {
    return id;
  }

  @Override
  public int getGroupId() {
    return groupId;
  }

  @Override
  public int getOrder() {
    return order;
  }

  Direction getDirection() {
    return direction;
  }

  private CharSequence title;

  @Override
  public MenuItem setTitle(CharSequence title) {
    this.title = title;
    return this;
  }

  @Override
  public MenuItem setTitle(@StringRes int title) {
    return setTitle(menu.getContext().getString(title));
  }

  @Override
  public CharSequence getTitle() {
    return title != null ? title : condensedTitle;
  }

  private CharSequence condensedTitle;

  @Override
  public MenuItem setTitleCondensed(CharSequence title) {
    this.condensedTitle = title;
    return this;
  }

  @Override
  public CharSequence getTitleCondensed() {
    return condensedTitle;
  }

  private Drawable icon;
  private int iconRes = NO_ID;

  @Override
  public MenuItem setIcon(Drawable icon) {
    this.icon = icon;
    this.iconRes = NO_ID;
    menu.dispatchUpdatePresenter();
    return this;
  }

  @Override
  public MenuItem setIcon(@DrawableRes int iconRes) {
    this.iconRes = iconRes;
    this.icon = null;
    menu.dispatchUpdatePresenter();
    return this;
  }

  @Override
  public Drawable getIcon() {
    if (this.icon == null && this.iconRes > 0) {
      final Drawable icon = ContextCompat.getDrawable(menu.getContext(), this.iconRes).mutate();
      setIcon(icon);
    }
    return this.icon;
  }

  private Intent intent;

  @Override
  public MenuItem setIntent(Intent intent) {
    this.intent = intent;
    return this;
  }

  @Override
  public Intent getIntent() {
    return intent;
  }

  @Override
  public MenuItem setShortcut(char numericChar, char alphaChar) {
    return this;
  }

  @Override
  public MenuItem setNumericShortcut(char numericChar) {
    return this;
  }

  @Override
  public char getNumericShortcut() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setAlphabeticShortcut(char alphaChar) {
    return this;
  }

  @Override
  public char getAlphabeticShortcut() {
    throw new RuntimeException("not implemented yet...");
  }

  private boolean visible = false;

  @Override
  public MenuItem setVisible(boolean visible) {
    this.visible = visible;
    this.enabled = visible;
    menu.dispatchUpdatePresenter();
    return this;
  }

  @Override
  public boolean isVisible() {
    return visible;
  }

  private boolean enabled = false;

  @Override
  public MenuItem setEnabled(boolean enabled) {
    this.enabled = enabled;
    this.visible = enabled;
    menu.dispatchUpdatePresenter();
    return this;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public ContextMenu.ContextMenuInfo getMenuInfo() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public void setShowAsAction(int actionEnum) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setShowAsActionFlags(int actionEnum) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setActionView(View view) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setActionView(@LayoutRes int resId) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public View getActionView() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setActionProvider(ActionProvider actionProvider) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public ActionProvider getActionProvider() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public boolean expandActionView() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public boolean collapseActionView() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public boolean isActionViewExpanded() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setCheckable(boolean checkable) {
    return this;
  }

  @Override
  public boolean isCheckable() {
    return false;
  }

  @Override
  public MenuItem setChecked(boolean checked) {
    return this;
  }

  @Override
  public boolean isChecked() {
    return false;
  }

  @Override
  public boolean hasSubMenu() {
    return false;
  }

  @Override
  public SubMenu getSubMenu() {
    throw new RuntimeException("IndicatableFFAB does not accept sub menu.");
  }
}

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
import android.support.annotation.StringRes;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

/**
 * Created by akihit on 2017/01/11.
 */

class IffabMenuItem implements MenuItem {
  @Override
  public int getItemId() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public int getGroupId() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public int getOrder() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setTitle(CharSequence title) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setTitle(@StringRes int title) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public CharSequence getTitle() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setTitleCondensed(CharSequence title) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public CharSequence getTitleCondensed() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setIcon(Drawable icon) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setIcon(@DrawableRes int iconRes) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public Drawable getIcon() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setIntent(Intent intent) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public Intent getIntent() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setShortcut(char numericChar, char alphaChar) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setNumericShortcut(char numericChar) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public char getNumericShortcut() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setAlphabeticShortcut(char alphaChar) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public char getAlphabeticShortcut() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setCheckable(boolean checkable) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public boolean isCheckable() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setChecked(boolean checked) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public boolean isChecked() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setVisible(boolean visible) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public boolean isVisible() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem setEnabled(boolean enabled) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public boolean isEnabled() {
    throw new RuntimeException("not implemented yet...");
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
  public boolean hasSubMenu() {
    throw new RuntimeException("IndicatableFFAB does not accept sub menu.");
  }

  @Override
  public SubMenu getSubMenu() {
    throw new RuntimeException("IndicatableFFAB does not accept sub menu.");
  }
}

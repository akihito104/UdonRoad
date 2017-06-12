package com.freshdigitable.udonroad.listitem;

import android.view.View;

import twitter4j.User;

/**
 * Created by akihit on 2017/06/12.
 */
public interface OnUserIconClickedListener {
  void onUserIconClicked(View view, User user);
}

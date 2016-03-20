package com.freshdigitable.udonroad;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.freshdigitable.udonroad.databinding.UserAccountBinding;

import twitter4j.User;

/**
 * Created by akihit on 2016/01/30.
 */
public class UserAccountActivity extends AppCompatActivity {
  private UserAccountBinding binding;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = DataBindingUtil.setContentView(this, R.layout.user_account);
    User user = parseIntent();
    UserInfoHeaderFragment headerFragment = (UserInfoHeaderFragment) getSupportFragmentManager().findFragmentById(R.id.user_header_fragment);
    headerFragment.bindUserInfo(user);
  }

  public static Intent createIntent(Context context, User user) {
    Intent intent = new Intent(context, UserAccountActivity.class);
    intent.putExtra("user", user);
    return intent;
  }

  private User parseIntent() {
    return (User) getIntent().getSerializableExtra("user");
  }
}

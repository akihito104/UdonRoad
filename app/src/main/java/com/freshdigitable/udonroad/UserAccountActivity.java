package com.freshdigitable.udonroad;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.freshdigitable.udonroad.databinding.UserAccountBinding;
import com.squareup.picasso.Picasso;

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
    binding.userScreenName.setText("@" + user.getScreenName());
    binding.userName.setText(user.getName());
    binding.userDescription.setText(user.getDescription());
    Picasso.with(this).load(user.getProfileImageURLHttps()).into(binding.userIcon);
    Picasso.with(this).load(user.getProfileBannerMobileURL()).fit().into(binding.userBanner);
    binding.userTweetsCount.setText("* " + user.getStatusesCount());
    binding.userFollowerCount.setText("> " + user.getFollowersCount());
    binding.userFriendsCount.setText("< " + user.getFriendsCount());
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

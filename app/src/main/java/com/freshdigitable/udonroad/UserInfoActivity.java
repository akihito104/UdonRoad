package com.freshdigitable.udonroad;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.freshdigitable.udonroad.databinding.ActivityUserInfoBinding;
import com.freshdigitable.udonroad.ffab.FlingableFABHelper;
import com.freshdigitable.udonroad.realmdata.UserRealm;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import twitter4j.User;

/**
 * Created by akihit on 2016/01/30.
 */
public class UserInfoActivity extends AppCompatActivity {

  private Realm realm;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ActivityUserInfoBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_user_info);
    binding.ffab.hide();

    User user = parseIntent();
    final UserInfoAppbarFragment appbar = (UserInfoAppbarFragment) getSupportFragmentManager().findFragmentById(R.id.userInfo_appbar);
    appbar.setUser(user);

    final UserInfoPagerFragment viewPager = (UserInfoPagerFragment) getSupportFragmentManager().findFragmentById(R.id.userInfo_pagerFragment);
    viewPager.setTabLayout(appbar.getTabLayout());
    viewPager.setFABHelper(new FlingableFABHelper(binding.fabIndicator, binding.ffab));
    viewPager.setUser(user);
  }

  @Override
  protected void onStop() {
    realm.close();
    super.onStop();
  }

  public static Intent createIntent(Context context, User user) {
    Intent intent = new Intent(context, UserInfoActivity.class);
    intent.putExtra("user", user.getId());
    return intent;
  }

  private User parseIntent() {
    final long userId = getIntent().getLongExtra("user", -1L);
    final RealmConfiguration realmConfig = new RealmConfiguration.Builder(getApplicationContext())
        .name("home")
        .build();
    realm = Realm.getInstance(realmConfig);
    return realm.where(UserRealm.class)
        .equalTo("id", userId)
        .findFirst();
  }
}

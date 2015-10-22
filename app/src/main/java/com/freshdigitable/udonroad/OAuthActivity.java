package com.freshdigitable.udonroad;

import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.StringRes;

import twitter4j.Twitter;

/**
 * Created by akihit on 15/10/22.
 */
@EActivity(R.layout.activity_login)
public class OAuthActivity extends AppCompatActivity {
  @StringRes(R.string.callback_url)
  String callbackUrl;

  @ViewById
  Button oauthButton;

  private Twitter twitter;

  @AfterViews
  protected void afterViews() {
    twitter = AccessUtil.getTwitterInstance(this);
  }

  @Click
  protected void onClicked() {
  }
}


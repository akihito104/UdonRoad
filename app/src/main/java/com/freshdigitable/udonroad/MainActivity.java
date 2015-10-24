package com.freshdigitable.udonroad;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {
  @ViewById(R.id.timeline)
  RecyclerView timeline;

  private RecyclerView.Adapter tlAdapter;
  private RecyclerView.LayoutManager tlLayoutManager;

  @AfterViews
  void afterViews() {
    if (!AccessUtil.hasAccessToken(this)) {
      Intent intent = new Intent(this, OAuthActivity_.class);
      startActivity(intent);
      finish();
    }

    timeline.setHasFixedSize(true);
    tlLayoutManager = new LinearLayoutManager(this);
    timeline.setLayoutManager(tlLayoutManager);
    tlAdapter = new TimelineAdapter();
    timeline.setAdapter(tlAdapter);
  }

}


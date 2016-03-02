/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.freshdigitable.udonroad.databinding.FragmentStatusDetailBinding;
import com.google.repacked.kotlin.jvm.functions.Function1;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import twitter4j.Status;

public class StatusDetailFragment extends Fragment {
  private FragmentStatusDetailBinding binding;
  private Status status;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return  inflater.inflate(R.layout.fragment_status_detail, container, false);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    binding = DataBindingUtil.bind(getView());

    this.status = (Status) getArguments().get("status");
    binding.statusView.bindStatus(this.status);

    final ArrayAdapter<DetailMenu> arrayAdapter
        = new ArrayAdapter<DetailMenu>(getContext(), android.R.layout.simple_list_item_1){
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        final DetailMenu item = getItem(position);
        final int strres = item.getStringRes();
        ((TextView)v).setText(strres);
        return v;
      }
    };

    binding.detailMenu.setAdapter(arrayAdapter);
    arrayAdapter.add(status.isRetweetedByMe() ? DetailMenu.RT_DELETE : DetailMenu.RT_CREATE);
    arrayAdapter.add(status.isFavorited() ? DetailMenu.FAV_DELETE : DetailMenu.FAV_CREATE);
    arrayAdapter.add(DetailMenu.REPLY);
    arrayAdapter.add(DetailMenu.QUOTE);
    binding.detailMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final DetailMenu menu = (DetailMenu) parent.getItemAtPosition(position);
        menu.action(twitterApi, status);
      }
    });
  }

  private TwitterApi twitterApi;

  public void setTwitterApi(TwitterApi twitterApi) {
    this.twitterApi = twitterApi;
  }

  public static StatusDetailFragment getInstance(final Status status) {
    Bundle args = new Bundle();
    args.putSerializable("status", status);
    final StatusDetailFragment statusDetailFragment = new StatusDetailFragment();
    statusDetailFragment.setArguments(args);
    return statusDetailFragment;
  }

  enum DetailMenu implements DetailMenuInterface {
    RT_CREATE {
      @Override
      public int getStringRes() {
        return R.string.detail_rt_create;
      }

      @Override
      public void action(TwitterApi api, Status status) {
        api.retweetStatus(status.getId())
            .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<Status>() {
          @Override
          public void call(Status status) {

          }
        }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {

          }
        }, new Action0() {
          @Override
          public void call() {
          }
        });
      }
    }, RT_DELETE {
      @Override
      public int getStringRes() {
        return R.string.detail_rt_delete;
      }
    }, FAV_CREATE {
      @Override
      public int getStringRes() {
        return R.string.detail_fav_create;
      }
    }, FAV_DELETE {
      @Override
      public int getStringRes() {
        return R.string.detail_fav_delete;
      }
    }, REPLY {
      @Override
      public int getStringRes() {
        return R.string.detail_reply;
      }
    }, QUOTE {
      @Override
      public int getStringRes() {
        return R.string.detail_quote;
      }
    },;
  }

  interface DetailMenuInterface {
    @StringRes
    int getStringRes();

    void action(TwitterApi api, Status status);
  }

  final Function1<String, Action0> toastAction = new Function1<String, Action0>() {
    @Override
    public Action0 invoke(String s) {
      return new Action1<String>() {
        @Override
        public void call(String s) {

        }
      };
    }
  }
}

package com.yooiistudios.news.ui.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import com.yooiistudios.news.model.news.NewsSelectPageUrlProvider;
import com.yooiistudios.news.ui.fragment.NewsSelectFragment;

/**
 * Created by Dongheyon Jeong on in News-Android-L from Yooii Studios Co., LTD. on 2014. 9. 9.
 *
 * NewsSelectPagerAdapter
 *  뉴스 선택화면을 구성하는 뷰페이저 어댑터
 */
public class NewsSelectPagerAdapter extends FragmentPagerAdapter {

    public NewsSelectPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int i) {
        return NewsSelectFragment.newInstance(i);
    }

    @Override
    public int getCount() {
        return NewsSelectPageUrlProvider.getLanguageCount();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return NewsSelectPageUrlProvider.getLanguageAt(position);
    }
}

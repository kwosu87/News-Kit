package com.yooiistudios.news.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.yooiistudios.news.R;
import com.yooiistudios.news.model.language.Language;
import com.yooiistudios.news.ui.fragment.SettingFragment;

import static com.yooiistudios.news.ui.fragment.SettingFragment.SettingItem;
import static com.yooiistudios.news.ui.widget.MainBottomContainerLayout.PanelMatrixType;

/**
 * Created by Dongheyon Jeong on in News-Android-L from Yooii Studios Co., LTD. on 14. 11. 3.
 *
 * SettingAdapter
 *  세팅화면에 사용될 어뎁터
 */
public class SettingAdapter extends BaseAdapter {
    private Context mContext;

    public SettingAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return SettingItem.values().length;
    }

    @Override
    public Object getItem(int position) {
        return SettingItem.values()[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return SettingItemFactory.inflate(mContext, parent, position);
    }

    private static class SettingItemFactory {
        private static View inflate(final Context context, ViewGroup parent, int position) {
            View view = null;

            SettingItem item = SettingItem.values()[position];
            switch (item) {
                case MAIN_SUB_HEADER:
                    view = LayoutInflater.from(context).inflate(R.layout.setting_item_sub_header, parent, false);
                    break;

                case LANGUAGE:
                case MAIN_AUTO_REFRESH_INTERVAL:
                case MAIN_PANEL_MATRIX:
                    view = LayoutInflater.from(context).inflate(R.layout.setting_item_base, parent, false);
                    initBaseItem(context, item, view);
                    break;

                case KEEP_SCREEN_ON:
                    view = LayoutInflater.from(context).inflate(R.layout.setting_item_switch, parent, false);
                    initKeepScreenOnItem(context, view);
                    break;

                case MAIN_AUTO_REFRESH_SPEED:
                    view = LayoutInflater.from(context).inflate(R.layout.setting_item_seekbar, parent, false);
                    // TODO 자동 스크롤 시작 시간 구현 필요
                    break;

                case TUTORIAL:
                    view = LayoutInflater.from(context).inflate(R.layout.setting_item_single, parent, false);
                    break;
            }

            TextView titleTextView = (TextView) view.findViewById(R.id.setting_item_title_textview);
            titleTextView.setText(item.getTitleResId());

            // TODO 나중에 폰트의 영어 높이가 너무 높은 부분에 대해서 고민하기. 마이너스 마진을 통해서 해결해야 하지 않을까 생각
//            if (item == SettingItem.MAIN_SUB_HEADER || item == SettingItem.NEWS_FEED_SUB_HEADER) {
//                titleTextView.setTypeface(TypefaceUtils.getMediumTypeface(context));
//                titleTextView.setTypeface(TypefaceUtils.getEngRegularTypeface(context));
//            } else {
//                titleTextView.setTypeface(TypefaceUtils.getRegularTypeface(context));
//                titleTextView.setTypeface(TypefaceUtils.getEngRegularTypeface(context));
//            }

//            TextView descriptionTextView =
//                    (TextView) view.findViewById(R.id.setting_item_description_textview);
//            if (descriptionTextView != null) {
//                descriptionTextView.setTypeface(TypefaceUtils.getEngRegularTypeface(context));
//            }
            return view;
        }
    }

    private static void initBaseItem(Context context, SettingItem item, View view) {
        TextView descriptionTextView =
                (TextView) view.findViewById(R.id.setting_item_description_textview);

        if (item == SettingItem.LANGUAGE) {
            descriptionTextView.setText(
                    Language.getCurrentLanguageType(context).getLocalNotationStringId());
        } else if (item == SettingItem.MAIN_AUTO_REFRESH_INTERVAL) {
            // TODO 초 설정 기능 구현 필요
            descriptionTextView.setText("5 sec");
        } else if (item == SettingItem.MAIN_PANEL_MATRIX) {
            PanelMatrixType currentPanelMatrix =
                    PanelMatrixType.getCurrentPanelMatrix(context);
            descriptionTextView.setText(
                    context.getString(R.string.setting_main_panel_matrix_description,
                            currentPanelMatrix.displayName));
        }
    }

    private static void initKeepScreenOnItem(Context context, View view) {
        SwitchCompat keepScreenSwitch = (SwitchCompat) view.findViewById(R.id.setting_item_switch);
        final SharedPreferences preferences = context.getSharedPreferences(
                SettingFragment.KEEP_SCREEN_ON_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        keepScreenSwitch.setChecked(preferences.getBoolean(SettingFragment.KEEP_SCREEN_ON_KEY, false));
        keepScreenSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(SettingFragment.KEEP_SCREEN_ON_KEY, isChecked).apply();
            }
        });
    }
}

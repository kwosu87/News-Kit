package com.yooiistudios.news.model.news;

import android.content.Context;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Created by Dongheyon Jeong on in News-Android-L from Yooii Studios Co., LTD. on 2014. 9. 11.
 *
 * NewsSelectPageUrlProvider
 *  뉴스 선택 화면에서 탭의 국가 및 뉴스피드 목록을 제공함
 */
public class NewsContentProvider {

    private static NewsContentProvider instance;
    private ArrayList<NewsRegion> mNewsRegionList;

    public static NewsContentProvider getInstance(Context context) {
        if (instance == null) {
            instance = new NewsContentProvider(context);
        }

        return instance;
    }

    private NewsContentProvider(Context context) {
        mNewsRegionList = new ArrayList<>();
        for (int i = 0; i < NewsProviderLangType.values().length; i++) {
            // int를 raw id로 변환
            mNewsRegionList.add(getNewsProvidersByResource(context,
                    NewsProviderLangType.valueOf(i).getResourceId()));
        }
    }

    public NewsTopic getNewsTopic(String languageCode, @Nullable String regionCode,
                                         int newsProviderId, int newsTopicId) {
        NewsProvider newsProvider = getNewsProvider(languageCode, regionCode, newsProviderId);
        return newsProvider.getNewsTopic(newsTopicId);
    }

    public NewsProvider getNewsProvider(NewsFeed newsFeed) {
        return getNewsProvider(newsFeed.getTopicLanguageCode(), newsFeed.getTopicRegionCode(),
                newsFeed.getTopicProviderId());
    }


    public NewsProvider getNewsProvider(String languageCodeToCompare,
                                        @Nullable String regionCodeToCompare,
                                        int providerIdToCompare) {
        for (NewsRegion newsRegion : mNewsRegionList) {
            String languageCode = newsRegion.getLanguageCode();
            String regionCode = newsRegion.getRegionCode();
            if (languageCode.equalsIgnoreCase(languageCodeToCompare)
                    && (regionCode == null
                        || regionCodeToCompare == null
                        || regionCode.equalsIgnoreCase(regionCodeToCompare))) {

                for (NewsProvider newsProvider : newsRegion.getNewsProviders()) {
                    if (newsProvider.getId() == providerIdToCompare) {
                        return newsProvider;
                    }
                }
            }
        }

        return null;
    }

    public NewsRegion getNewsRegion(int position) {
        if (position < mNewsRegionList.size()) {
            return mNewsRegionList.get(position);
        } else {
            return null;
        }
    }

    private static NewsRegion getNewsProvidersByResource(Context context, int resourceId) {
        // raw id 에서 json 스트링을 만들고 JSONObject 로 변환
        try {
            InputStream file;
            file = context.getResources().openRawResource(resourceId);

            BufferedReader reader = new BufferedReader(new InputStreamReader(file, "UTF-8"));
            char[] tC = new char[file.available()];
            reader.read(tC);

            String content = new String(tC);

            // 변환한 JSONObject 를 NewsProviders 로 변환
            JSONObject newsData = new JSONObject(content);

            // lang info
            NewsRegion newsRegion = new NewsRegion();
            newsRegion.englishLanguageName = newsData.getString("lang_name_english");
            newsRegion.regionalLanguageName = newsData.getString("lang_name_regional");
            newsRegion.languageCode = newsData.getString("lang_code");
            String regionCode = newsData.getString("region_code");
            if (!regionCode.equals("null")) {
                newsRegion.regionCode = regionCode;
            }

            // providers
            JSONArray providersArray = newsData.getJSONArray("news_providers");
            for (int i = 0; i < providersArray.length(); i++) {
                JSONObject newsProviderObject = providersArray.getJSONObject(i);

                NewsProvider newsProvider = new NewsProvider();
                newsProvider.setName(newsProviderObject.getString("provider_name"));
                newsProvider.setId(newsProviderObject.getInt("provider_id"));

                newsProvider.setLanguageCode(newsRegion.languageCode);
                newsProvider.setRegionCode(newsRegion.regionCode);

                JSONArray topicArray = newsProviderObject.getJSONArray("topics");
                for (int j = 0; j < topicArray.length(); j++) {
                    JSONObject topicObject = topicArray.getJSONObject(j);

                    NewsTopic newsTopic = new NewsTopic();
                    newsTopic.setTitle(topicObject.getString("topic_name"));
                    newsTopic.setId(topicObject.getInt("topic_id"));

                    newsTopic.setLanguageCode(newsRegion.languageCode);
                    newsTopic.setRegionCode(newsRegion.regionCode);
                    newsTopic.setNewsProviderId(newsProvider.getId());

                    String url = topicObject.getString("url");
                    newsTopic.setNewsFeedUrl(new NewsFeedUrl(url, NewsFeedUrlType.GENERAL));

                    if (topicObject.has("isDefault")) {
                        newsTopic.setDefault(Boolean.valueOf(topicObject.getString("isDefault")));
                    }

                    newsProvider.addNewsTopic(newsTopic);
                }

                newsRegion.newsProviders.add(newsProvider);
            }

            /*
            // Test
            NLLog.now("lang: " + newsProviderList.englishLanguageName);
            NLLog.now("lang_region: " + newsProviderList.regionalLanguageName);
            NLLog.now("lang_code: " + newsProviderList.languageCode);
            NLLog.now("region_code: " + newsProviderList.languageCode);
            NLLog.now("newsProviders");

            for (NewsProvider newsProvider : newsProviderList.newsProviders) {
                NLLog.now("provider name: " + newsProvider.getName());
                for (NewsFeed newsFeed : newsProvider.getNewsFeedList()) {
                    NLLog.now("newsFeed name: " + newsFeed.getTitle());
                    NLLog.now("newsFeed url: " + newsFeed.getNewsFeedUrl().getUrl());
                }
            }
            */

            /*
            for (NewsProvider provider : newsRegion.getNewsProviders()) {
                NLLog.now("provider     language    : " + provider.getLanguageCode());
                NLLog.now("provider     region      : " + provider.getRegionCode());
                NLLog.now("provider     id          : " + provider.getId());

                for (NewsTopic topic : provider.getNewsTopicList()) {
                    NLLog.now("topic    language    : " + topic.getLanguageCode());
                    NLLog.now("topic    region      : " + topic.getRegionCode());
                    NLLog.now("topic    providerId  : " + topic.getNewsProviderId());
                    NLLog.now("topic    id          : " + topic.getId());
                }
            }
            */

            return newsRegion;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
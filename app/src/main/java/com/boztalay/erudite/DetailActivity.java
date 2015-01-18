package com.boztalay.erudite;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;

/**
 * Created by boztalay on 1/17/15.
 */
public class DetailActivity extends Activity {

    private static final int CHARS_PER_PAGE = 230;

    private CardScrollView mCardScroller;
    private CardScrollAdapter mCardAdapter;

    private String mWord;
    private String mWikiContents;
    private ArrayList<String> mPaginatedWikiContents;

    private TextToSpeech mTextToSpeech;
    private boolean isTTSReady;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        isTTSReady = false;
        mTextToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS) {
                    isTTSReady = true;
                }
            }
        });

        mWikiContents = getIntent().getExtras().getString("WikiContents");
        mWord = getIntent().getExtras().getString("Word");
        mPaginatedWikiContents = new ArrayList<String>();

        if (mWikiContents != null && mWikiContents.length() > 0) {
            paginateWikiContents();
        } else {
            mPaginatedWikiContents.add("Couldn't load the Wikipedia page!");
        }

        mCardScroller = new CardScrollView(this);
        mCardAdapter = new CardScrollAdapter() {
            @Override
            public int getCount() {
                return mPaginatedWikiContents.size();
            }

            @Override
            public Object getItem(int position) {
                return mPaginatedWikiContents.get(position);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                CardBuilder card = new CardBuilder(DetailActivity.this, CardBuilder.Layout.TEXT_FIXED);


                String wikiContents = (String) getItem(position);
                card.setText(wikiContents);
                card.setFootnote(mWord);

                return card.getView();
            }

            @Override
            public int getPosition(Object item) {
                return mPaginatedWikiContents.indexOf(item);
            }
        };
        mCardScroller.setAdapter(mCardAdapter);


        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(isTTSReady) {
                    mTextToSpeech.speak(mWikiContents, TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });

        setContentView(mCardScroller);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void paginateWikiContents() {
        int currentPageStartIndex = 0;
        int currentPageEndIndex = 0;
        int currentEndSearchIndex = 0;

        while(currentPageEndIndex < mWikiContents.length()) {
            if(mWikiContents.length() - currentPageStartIndex <= CHARS_PER_PAGE) {
                currentPageEndIndex = mWikiContents.length();
            } else {
                while (currentEndSearchIndex - currentPageStartIndex <= CHARS_PER_PAGE) {
                    currentPageEndIndex = currentEndSearchIndex;
                    currentEndSearchIndex = mWikiContents.indexOf(' ', currentEndSearchIndex + 1);
                }
            }

            mPaginatedWikiContents.add(mWikiContents.substring(currentPageStartIndex, currentPageEndIndex).trim());
            currentPageStartIndex = currentPageEndIndex;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        mTextToSpeech.stop();
        super.onPause();
    }
}

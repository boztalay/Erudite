package com.boztalay.erudite;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

/**
 * Created by boztalay on 1/17/15.
 */
public class StopWordChecker {
    private HashSet<String> mStopWords;

    public StopWordChecker(Context context) {
        setUpStopWordList(context);
    }

    private void setUpStopWordList(Context context) {
        mStopWords = new HashSet<String>();

        InputStream inputStream = context.getResources().openRawResource(com.boztalay.erudite.R.raw.stop_words_clean);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        while(true) {
            String line;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                continue;
            }

            if(line == null) {
                break;
            } else {
                mStopWords.add(line.trim());
            }
        }
    }

    public boolean isStopWord(String word) {
        return mStopWords.contains(word.toLowerCase());
    }
}

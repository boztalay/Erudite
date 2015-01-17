package com.boztalay.erudite;

import android.content.Intent;
import android.os.AsyncTask;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.WindowManager;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends Activity implements RecognitionListener {

    private final static int MAX_WORDS = 20;

    private CardScrollView mCardScroller;
    private CardScrollAdapter mCardAdapter;

    private SpeechRecognizer mSpeechRecognizer;
    private ArrayList<String> mWordsSeen;
    private StopWordChecker mStopWordChecker;
    private HashMap<String, String> mWikiContents;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mWordsSeen = new ArrayList<String>();
        mStopWordChecker = new StopWordChecker(this);
        mWikiContents = new HashMap<String, String>();

        mCardScroller = new CardScrollView(this);
        mCardAdapter = new CardScrollAdapter() {
            @Override
            public int getCount() {
                return mWordsSeen.size();
            }

            @Override
            public Object getItem(int position) {
                return mWordsSeen.get(position);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                CardBuilder card = new CardBuilder(MainActivity.this, CardBuilder.Layout.TEXT);

                String word = (String) getItem(position);
                String wikiContents = mWikiContents.get(word);

                card.setText(word);

                if(wikiContents != null) {
                    card.setFootnote("Loaded");
                }

                return card.getView();
            }

            @Override
            public int getPosition(Object item) {
                return mWordsSeen.indexOf(item);
            }
        };
        mCardScroller.setAdapter(mCardAdapter);

        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String word = mWordsSeen.get(position);

                // This is jank, but it's a hackathon

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        if(mWikiContents.get(word) != null) {
                            return null;
                        }

                        HttpURLConnection urlConnection;

                        try {
                            URL url = new URL("https://en.wikipedia.org/w/api.php?action=query&prop=extracts&format=json&exintro=&titles=" + word);
                            urlConnection = (HttpURLConnection) url.openConnection();
                        } catch (Exception e) {
                            return null;
                        }

                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                            String thing = reader.readLine();

                            JSONObject json = new JSONObject(thing);
                            Iterator it = json.getJSONObject("query").getJSONObject("pages").keys();
                            String pageKey = (String) it.next();
                            String extractContent = json.getJSONObject("query").getJSONObject("pages").getJSONObject(pageKey).getString("extract");

                            String contents;
                            if(!extractContent.contains("may refer to")) {
                                contents = extractContent.substring(0, extractContent.indexOf("</p>"));
                            } else {
                                contents = extractContent.substring(extractContent.indexOf("<li>"), extractContent.indexOf("</li>") + 1);
                            }

                            contents = contents.replaceAll("\\<.*?\\>", "");
                            mWikiContents.put(word, contents);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            urlConnection.disconnect();
                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        mCardAdapter.notifyDataSetChanged();

                        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                        intent.putExtra("WikiContents", mWikiContents.get(word));
                        intent.putExtra("Word", word);
                        MainActivity.this.startActivity(intent);
                    }
                }.execute();
            }
        });

        setContentView(mCardScroller);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(this);
        startRecognition();
    }

    private void startRecognition() {
        mSpeechRecognizer.stopListening();
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        mSpeechRecognizer.startListening(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
        startRecognition();
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        mSpeechRecognizer.stopListening();
        super.onPause();
    }

    // Speech recognition

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        Log.d("Recognition", "onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d("Recognition", "onBeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float v) {
//        Log.d("Recognition", "onRmsChanged: " + v);
    }

    @Override
    public void onBufferReceived(byte[] bytes) {
        Log.d("Recognition", "onBufferReceived: " + new String(bytes));
    }

    @Override
    public void onEndOfSpeech() {
        Log.d("Recognition", "onEndOfSpeech");
    }

    @Override
    public void onError(int i) {
        switch (i) {
            case SpeechRecognizer.ERROR_AUDIO:
                Log.d("Recognition", "AUDIO ERROR");
                startRecognition();
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                Log.d("Recognition", "CLIENT ERROR");
                startRecognition();
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                Log.d("Recognition", "PERMISSIONS ERROR");
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                Log.d("Recognition", "NETWORK ERROR");
                startRecognition();
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                Log.d("Recognition", "NETWORK TIMEOUT ERROR");
                startRecognition();
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                Log.d("Recognition", "NO MATCH ERROR");
                startRecognition();
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                Log.d("Recognition", "BUSY ERROR");
                break;
            case SpeechRecognizer.ERROR_SERVER:
                Log.d("Recognition", "SERVER ERROR");
                startRecognition();
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                Log.d("Recognition", "SPEECH TIMEOUT ERROR");
                startRecognition();
                break;
        }
    }

    @Override
    public void onResults(Bundle bundle) {
        Log.d("Recognition", "onResults");
        startRecognition();
    }

    @Override
    public void onPartialResults(Bundle bundle) {
        ArrayList<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String resultsString = "";
        for (String result : results) {
            resultsString += result + " ";
        }

        Log.d("Recognition", "onPartialResults: " + resultsString);

        ArrayList<String> rawResults = new ArrayList<String>();
        for (String result : results) {
            String[] words = result.split(" ");
            for (String word : words) {
                String trimmedWord = word.trim();
                if (trimmedWord.length() > 0) {
                    rawResults.add(word.trim());
                }
            }
        }

        for (String result : rawResults) {
            // Might get expensive...
            if (!mWordsSeen.contains(result) && !mStopWordChecker.isStopWord(result)) {
                mWordsSeen.add(0, result);

                if (mWordsSeen.size() > MAX_WORDS) {
                    mWordsSeen.remove(mWordsSeen.size() - 1);
                }
            }
        }

        mCardAdapter.notifyDataSetChanged();
    }

    @Override
    public void onEvent(int i, Bundle bundle) {
        Log.d("Recognition", "onEvent: " + i);
    }
}
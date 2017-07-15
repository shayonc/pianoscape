package piano.pianotrainer.fragments;

/**
 * Created by Ekteshaf Chowdhury on 2017-07-10.
 */

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import piano.pianotrainer.R;

public class MusicScoreImportActivity extends AppCompatActivity {

    public static final String FRAGMENT_MUSIC_SCORE_VIEWER_BASIC = "music_score_viewer_basic";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_score_import);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new MusicScoreViewerFragment(),
                            FRAGMENT_MUSIC_SCORE_VIEWER_BASIC)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scoreviewer, menu);
        return true;
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.action_info:
//                new AlertDialog.Builder(this)
//                        .setMessage(R.string.intro_message)
//                        .setPositiveButton(android.R.string.ok, null)
//                        .show();
//                return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
}
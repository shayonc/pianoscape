package piano.pianotrainer.fragments;

import android.Manifest;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import piano.pianotrainer.R;
import piano.pianotrainer.db.DBHelper;
import piano.pianotrainer.parser.XMLMusicParser;

public class HomeActivity extends AppCompatActivity {

    private TextView mTextMessage;
    private DBHelper dbHelper;
    private SQLiteDatabase db;

    private Context context;
    private XMLMusicParser xmlparser;
    private String filename = "MozartPianoSonata.mxl";
    private static final String OUTPUT_FOLDER = "XMLFiles";


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_camera);
                    return true;
                case R.id.navigation_settings:
                    mTextMessage.setText(R.string.summary);
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        context = getApplicationContext();
        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase(); // get writable

        try {
            if (isExternalStorageWritable()) {
                int permissionCheck = ContextCompat.checkSelfPermission(HomeActivity.this,
                        Manifest.permission.WRITE_CALENDAR);

                xmlparser = new XMLMusicParser(filename, OUTPUT_FOLDER);
                xmlparser.parseMXL(); // parse the .mxl file
            }
            else  {
                CharSequence text = "External storage not available for read and write.";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        }
        catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

}

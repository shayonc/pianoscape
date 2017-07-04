package piano.pianotrainer.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
    private String filename = "Dichterliebe01";
    private static final String OUTPUT_FOLDER = "XMLFiles";

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

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

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                try {
                    if (isExternalStorageWritable()) {
                        verifyStoragePermissions(HomeActivity.this);
                        int permissionCheck = ContextCompat.checkSelfPermission(HomeActivity.this,
                                Manifest.permission.WRITE_CALENDAR);

                        xmlparser = new XMLMusicParser(filename, OUTPUT_FOLDER);
                        xmlparser.parseMXL(); // parse the .mxl file
                        xmlparser.parseXML(); // parse the .xml file

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
        });


    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

}

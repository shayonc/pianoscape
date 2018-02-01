package piano.pianotrainer.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import piano.pianotrainer.R;
import piano.pianotrainer.model.MusicFile;

/**
 * Created by Matthew on 11/22/2017.
 * https://www.shareicon.net/file-quaver-music-note-music-document-musical-720402
 */

public class ImageAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<MusicFile> musicFileList;

        public ImageAdapter(Context c, ArrayList<MusicFile> musicFileList) {
            mContext = c;
            this.musicFileList = musicFileList;
        }

        public int getCount() {
            return musicFileList.size();
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
//            ImageView imageView;
            View grid;
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                grid = new View(mContext);
                grid = inflater.inflate(R.layout.grid_layout, null);
                TextView textView = (TextView) grid.findViewById(R.id.grid_text);
                ImageView imageView = (ImageView)grid.findViewById(R.id.grid_image);
                String imageCaption = musicFileList.get(position).getFilename();
                if (musicFileList.get(position).getDateModified() != null) {
                    imageCaption = musicFileList.get(position).getFilename() + " " + musicFileList.get(position).getDateModified().toString();
                }
                textView.setText(imageCaption);
                textView.setTextColor(Color.BLACK);
                imageView.setImageResource(musicFileList.get(position).getThumbnail());

                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                grid = (View) convertView;
            }
            return grid;
        }

}


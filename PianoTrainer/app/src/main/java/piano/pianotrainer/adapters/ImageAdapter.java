package piano.pianotrainer.adapters;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;

import piano.pianotrainer.R;
import piano.pianotrainer.model.MusicFile;

/**
 * Created by Matthew on 11/22/2017.
 * http://www.clipartpanda.com/clipart_images/purple-musical-note-clip-art-30682946
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
            ImageView imageView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(350, 350));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }
            imageView.setImageResource(musicFileList.get(position).getThumbnail());
            return imageView;
        }

}


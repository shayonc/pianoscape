package piano.pianotrainer.fragments;

import android.app.AlertDialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import piano.pianotrainer.R;

/**
 * Created by Matthew on 11/24/2017.
 */

public class MusicDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.music_dialog)
                .setItems(R.array.musicDialogOptions, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                    }
                });
        return builder.create();
    }
}

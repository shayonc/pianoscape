package piano.pianotrainer.fragments;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import android.app.Activity;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.media.midi.MidiSender;
import android.os.Handler;
import android.util.Log;

import com.mobileer.miditools.MidiDispatcher;
import com.mobileer.miditools.MidiPortSelector;
import com.mobileer.miditools.MidiPortWrapper;

import java.io.IOException;

public class MidiOutputPortSelector extends MidiPortSelector {
    public static final String TAG = "MidiOutputPortSelector";
    private MidiOutputPort mOutputPort;
    private MidiDispatcher mDispatcher = new MidiDispatcher();
    private MidiDevice mOpenDevice;

    public MidiOutputPortSelector(MidiManager midiManager, Activity activity, int spinnerId) {
        super(midiManager, activity, spinnerId, 2);
    }

    public void onPortSelected(final MidiPortWrapper wrapper) {
        this.close();
        final MidiDeviceInfo info = wrapper.getDeviceInfo();
        if(info != null) {
            this.mMidiManager.openDevice(info, new MidiManager.OnDeviceOpenedListener() {
                public void onDeviceOpened(MidiDevice device) {
                    if(device == null) {
                        Log.e("MidiTools", "could not open " + info);
                    } else {
                        MidiOutputPortSelector.this.mOpenDevice = device;
                        MidiOutputPortSelector.this.mOutputPort = device.openOutputPort(0);
                        if(MidiOutputPortSelector.this.mOutputPort == null) {
                            Log.e("MidiTools", "could not open output port for " + info);
                            return;
                        }

                        MidiOutputPortSelector.this.mOutputPort.connect(MidiOutputPortSelector.this.mDispatcher);
                    }

                }
            }, (Handler)null);
        }

    }

    public void onClose() {
        try {
            if(this.mOutputPort != null) {
                this.mOutputPort.disconnect(this.mDispatcher);
            }

            this.mOutputPort = null;
            if(this.mOpenDevice != null) {
                this.mOpenDevice.close();
            }

            this.mOpenDevice = null;
        } catch (IOException var2) {
            Log.e("MidiTools", "cleanup failed", var2);
        }

        super.onClose();
    }

    public MidiSender getSender() {
        return this.mDispatcher.getSender();
    }
}

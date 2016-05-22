package org.ncameron.helloworld;

import android.content.Context;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Vibrator;

// For making noises. Manages resources (ringtones, etc.).
public class Beeper {
    MainActivity activity;
    Ringtone tone = null;
    Vibrator vibe = null;

    public Beeper(MainActivity activity) {
        this.activity = activity;
    }

    public void beep() {
        if (tone == null) {
            RingtoneManager man = new RingtoneManager(activity);
            man.setType(RingtoneManager.TYPE_NOTIFICATION);
            Cursor c = man.getCursor();

            tone = man.getRingtone(2);

            Context ctxt = activity.getApplicationContext();
            vibe = (Vibrator)ctxt.getSystemService(Context.VIBRATOR_SERVICE);

            if (tone == null) {
                return;
            }
        }

        tone.play();
        if (vibe != null) {
            vibe.vibrate(300);
        }
    }
}

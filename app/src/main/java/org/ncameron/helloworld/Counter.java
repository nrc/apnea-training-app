package org.ncameron.helloworld;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import org.ncameron.helloworld.logbook.Book;
import org.ncameron.helloworld.logbook.Entry;

import java.util.Date;

import static java.lang.Thread.*;


// State machine for the counter.
// Hard-coded for 'quick' CO2 tables.
public class Counter implements Runnable {
    private final static int REPS = 8;
    private final static int SECONDS = 90;

    private final static int MSG_RENDER = 7878;
    private final static int MSG_START = 7879;
    private final static int MSG_STOP = 7890;

    private State state = State.READY;
    private int seconds = SECONDS;
    private int reps = REPS;

    private Handler handler;
    private Beeper beeper;
    private MainActivity activity;

    private Thread thread = null;

    private static class CounterHandler extends Handler {
        private Renderer renderer;

        CounterHandler(Looper l, Renderer r) {
            super(l);
            renderer = r;
        }

        @Override
        public void handleMessage(Message inputMessage) {
            if (inputMessage.what == MSG_RENDER) {
                renderer.render();
            } else if (inputMessage.what == MSG_START) {
                renderer.renderStart();
            } else if (inputMessage.what == MSG_STOP) {
                renderer.renderStop();
            }
        }
    }

    public Counter(MainActivity activity) {
        handler = new CounterHandler(Looper.getMainLooper(), activity);
        beeper = new Beeper(activity);
        this.activity = activity;
    }

    public static Counter fromState(Bundle savedInstanceState, MainActivity activity) {
        Counter result = new Counter(activity);
        if (savedInstanceState == null || savedInstanceState.getString("state") == null) {
            return result;
        }

        result.state = State.valueOf(savedInstanceState.getString("state"));
        result.reps = savedInstanceState.getInt("reps");
        result.seconds = savedInstanceState.getInt("seconds");
        result.assertStateInvariants();

        if (result.state == State.RUNNING) {
            result.runTimer();
        } else {
            result.handler.sendEmptyMessage(MSG_RENDER);
        }

        return result;
    }

    public void saveState(Bundle savedInstanceState) {
        savedInstanceState.putString("state", state.name());
        savedInstanceState.putInt("seconds", seconds);
        savedInstanceState.putInt("reps", reps);
    }

    public synchronized void tick() {
        assertStateInvariants();
        if (state == State.RUNNING) {
            if (seconds <= 0) {
                reps -= 1;
                if (reps <= 0) {
                    reps = 0;
                    seconds = 0;
                    state = State.STOPPED;
                    Entry log_entry = make_log_entry();
                    handler.sendEmptyMessage(MSG_STOP);
                    write_log_entry(log_entry);
                } else {
                    seconds = SECONDS;
                    state = State.RESTING;
                }
                beeper.beep();
            }
        }
        assertStateInvariants();
    }

    public synchronized String getReps() {
        assertStateInvariants();
        return Integer.toString(reps);
    }

    public synchronized String getMins() {
        assertStateInvariants();
        return Integer.toString(seconds / 60);
    }

    public synchronized String getSeconds() {
        assertStateInvariants();
        return String.format("%02d", seconds % 60);
    }

    // The action of hitting the pause button.
    public synchronized void pause() {
        assertStateInvariants();
        if (state == State.PAUSED) {
            doResume();
        } else if (state == State.RUNNING) {
            doPause();
        }
        assertStateInvariants();
    }

    // Pause counting
    public synchronized void doPause() {
        state = State.PAUSED;
        if (thread != null) {
            thread.interrupt();
        }
    }

    // Resume counting.
    public synchronized void doResume() {
        runTimer();
    }

    public synchronized void reset() {
        state = State.STOPPING;
        assertStateInvariants();
        Entry log_entry = make_log_entry();
        state = State.READY;
        seconds = SECONDS;
        reps = REPS;
        assertStateInvariants();
        handler.sendEmptyMessage(MSG_STOP);
        handler.sendEmptyMessage(MSG_RENDER);
        write_log_entry(log_entry);
    }

    public synchronized void tap() {
        assertStateInvariants();
        if (state == State.READY || state == State.RESTING || state == State.PAUSED) {
            runTimer();
        }
        assertStateInvariants();
    }

    void runTimer() {
        while (thread != null) {
            if (state != State.RUNNING) {
                // This is bad, we should never be in this state, but not clear how to recover.
                return;
            }
            thread.interrupt();
        }

        state = State.RUNNING;
        handler.sendEmptyMessage(MSG_START);
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        long millis = SystemClock.elapsedRealtime();

        while (true) {
            if (state != State.RUNNING) {
                thread = null;
                return;
            }

            long new_time = SystemClock.elapsedRealtime();
            long diff = new_time - millis;
            long diff_s = diff / 1000;
            millis += diff_s * 1000;

            if (diff_s > 0) {
                synchronized (this) {
                    seconds -= diff_s;
                }

                tick();
                handler.sendEmptyMessage(MSG_RENDER);
            }

            try {
                sleep(1000 - diff % 1000);
            } catch (InterruptedException e) {
                // Ignore.
            }
        }
    }

    // Invariant: this.state = STOPPED | STOPPING
    private Entry make_log_entry() {
        // Don't log a run where we didn't manage a single rep.
        if (state == State.STOPPING &&
                this.REPS - this.reps == 0) {
            return null;
        }

        return new Entry(new Date(), this.REPS - this.reps, this.REPS, "", "Built-in quick CO2", new int[]{REPS, SECONDS});
    }

    private void write_log_entry(Entry entry) {
        if (entry != null) {
            Book.write_entry(activity.getApplicationContext(), entry);
        }
    }

    // TODO these do fuck all
    synchronized void assertStateInvariants() {
        switch (state) {
            case READY:
                assert(seconds == SECONDS);
                assert(reps == REPS);
                assert(thread == null);
                break;
            case STOPPED:
                assert(seconds == 0);
                assert(reps == 0);
                assert(thread == null);
                break;
            case RESTING:
                assert(seconds == SECONDS);
                assert(reps > 0);
                assert(thread == null);
                break;
            case RUNNING:
            case STOPPING:
                assert(seconds > 0);
                assert(reps > 0);
                assert(thread != null);
                break;
            case PAUSED:
                assert(seconds > 0);
                assert(reps > 0);
                assert(thread == null);
                break;
        }
    }

    private enum State {
        READY, RUNNING, RESTING, PAUSED, STOPPED, STOPPING
    }
}


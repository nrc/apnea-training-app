package org.ncameron.helloworld;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import org.ncameron.helloworld.logbook.Book;
import org.ncameron.helloworld.logbook.Entry;
import org.ncameron.helloworld.scripts.Executable;
import org.ncameron.helloworld.scripts.Script;

import java.util.Date;

import static java.lang.Thread.*;


// State machine for the counter.
public class Counter implements Runnable {
    private final static int MSG_RENDER = 7878;
    private final static int MSG_START = 7879;
    private final static int MSG_STOP = 7890;

    private State state = State.READY;
    private Executable exec;
    private int exec_index = 0;
    private int seconds;
    private int reps;
    private boolean should_beep = false;
    private String cur_label = "";

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

    private Counter(MainActivity activity) {
        handler = new CounterHandler(Looper.getMainLooper(), activity);
        beeper = new Beeper(activity);
        this.activity = activity;
    }

    // Returns if successful.
    public boolean set_script(int index, Script script) {
        Executable exec = script.get_executable(index);
        if (exec == null) {
            exec = script.get_executable(0);
            if (exec == null) {
                Log.e("Counter", "No scripts?");
                return false;
            }
        }
        this.exec_index = index;
        this.init_script(exec);

        return true;
    }

    public int getScript() {
        return exec_index;
    }

    private void init_script(Executable exec) {
        this.exec = exec;
        this.reps = exec.total_reps();
        this.seconds = exec.init_seconds();
        this.cur_label = exec.init_label();
        this.state = State.READY;
    }

    public static Counter fromState(Bundle savedInstanceState, MainActivity activity, Script script) {
        Counter result = new Counter(activity);

        if (savedInstanceState == null || savedInstanceState.getString("state") == null) {
            Log.e("Counter", "No state");
            if (!result.set_script(0, script)) {
                Log.e("Counter", "No script");
                return null;
            }
            return result;
        }

        if (!result.set_script(savedInstanceState.getInt("exec_index"), script)) {
            Log.e("Counter", "No script");
            return null;
        }

        result.state = State.valueOf(savedInstanceState.getString("state"));
        result.reps = savedInstanceState.getInt("reps");
        result.seconds = savedInstanceState.getInt("seconds");

        if (result.state == State.RUNNING) {
            result.runTimer();
        } else {
            result.handler.sendEmptyMessage(MSG_RENDER);
        }

        result.assertStateInvariants();
        return result;
    }

    public void saveState(Bundle savedInstanceState) {
        savedInstanceState.putInt("exec_index", exec_index);
        savedInstanceState.putString("state", state.name());
        savedInstanceState.putInt("seconds", seconds);
        savedInstanceState.putInt("reps", reps);
    }

    public synchronized void tick() {
        if (state == State.RUNNING) {
            if (seconds <= 0) {
                boolean should_beep = this.should_beep;

                set_from_state(exec.next());

                if (should_beep) {
                    beeper.beep();
                }
            }

            if (state != State.RUNNING && thread != null) {
                thread.interrupt();
                thread = null;
            }
        }
        assertStateInvariants();
    }

    private synchronized void set_from_state(Executable.State state) {
        if (state == null) {
            reps = 0;
            seconds = 0;
            this.cur_label = "";
            this.state = State.STOPPED;
            Entry log_entry = make_log_entry();
            handler.sendEmptyMessage(MSG_STOP);
            write_log_entry(log_entry);
            return;
        }
        this.reps = state.reps;
        this.seconds = state.time;
        this.should_beep = state.beep;
        this.cur_label = state.label;
        if (state.wait) {
            this.state = State.RESTING;
        } else {
            this.state = State.RUNNING;
        }
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

    public synchronized String getLabel() {
        assertStateInvariants();
        return cur_label;
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
        if (state == State.RUNNING) {
            state = State.PAUSED;
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
        }
    }

    // Resume counting.
    public synchronized void doResume() {
        state = State.RUNNING;
        runTimer();
    }

    public synchronized void reset() {
        if (state == State.RUNNING) {
            return;
        }
        assertStateInvariants();
        Entry log_entry = make_log_entry();
        state = State.READY;
        exec.reset();
        seconds = exec.init_seconds();
        reps = exec.total_reps();
        cur_label = exec.init_label();
        should_beep = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        assertStateInvariants();
        handler.sendEmptyMessage(MSG_STOP);
        handler.sendEmptyMessage(MSG_RENDER);
        write_log_entry(log_entry);
    }

    public synchronized void tap() {
        assertStateInvariants();
        if (state == State.READY || state == State.RESTING || state == State.PAUSED) {
            if (state == State.READY || state == State.RESTING) {
                set_from_state(exec.next());
            }
            state = State.RUNNING;
            runTimer();
        }
        assertStateInvariants();
    }

    void runTimer() {
        while (thread != null) {
            if (state != State.RUNNING) {
                // This is bad, we should never be in this state, but not clear how to recover.
                Log.e("Counter", "Bad state - thread exists, but not running.");
                return;
            }
            thread.interrupt();
            thread = null;
        }

        handler.sendEmptyMessage(MSG_START);
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        long millis = SystemClock.elapsedRealtime();

        while (true) {
            if (state != State.RUNNING) {
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
                // Ignore. Will handle by returning on next loop.
            }
        }
    }

    // Invariant: this.state = STOPPED | STOPPING
    private Entry make_log_entry() {
        int total_reps = exec.total_reps();
        // Don't log a run where we didn't manage a single rep.
        if (state == State.STOPPING &&
                total_reps - this.reps == 0) {
            return null;
        }

        return new Entry(new Date(), total_reps - this.reps, total_reps, exec.name(), exec.template(), exec.variables());
    }

    private void write_log_entry(Entry entry) {
        if (entry != null) {
            Book.write_entry(activity.getApplicationContext(), entry);
        }
    }

    synchronized void assertStateInvariants() {
        switch (state) {
            case READY:
                if (seconds != exec.init_seconds()) throw new AssertionError();
                if (reps != exec.total_reps()) throw new AssertionError();
                if (thread != null) throw new AssertionError();
                break;
            case STOPPED:
                if (seconds != 0) throw new AssertionError();
                if (reps != 0) throw new AssertionError();
                if (thread != null) throw new AssertionError();
                break;
            case RESTING:
                if (reps == 0) throw new AssertionError();
                if (thread != null) throw new AssertionError();
                break;
            case RUNNING:
            case STOPPING:
                // TODO failing at finish
                if (seconds < 0) throw new AssertionError();
                if (reps < 0) throw new AssertionError();
                if (thread == null) throw new AssertionError();
                break;
            case PAUSED:
                if (reps == 0) throw new AssertionError();
                if (thread != null) throw new AssertionError();
                break;
        }
    }

    private enum State {
        READY, RUNNING, RESTING, PAUSED, STOPPED, STOPPING
    }
}


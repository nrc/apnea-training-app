package org.ncameron.helloworld;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.ncameron.helloworld.scripts.Script;

public class MainActivity extends AppCompatActivity implements Renderer {
    public final static String EXTRA_MESSAGE = "org.ncameron.helloworld.MESSAGE";

    private Counter counter;
    private Script script;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        script = Script.read_script(getApplicationContext());
        counter = Counter.fromState(savedInstanceState, this, script);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onStart() {
        super.onStart();

        touchListeners();

        render();
    }

    @Override
    protected void onStop() {
        super.onStop();
        counter.doPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // Used to call counter.doResume() here, which means un-pausing when returning to the app
        // but that would start the timer even if it were not paused. Could remember the previous
        // state to choose whether to restart or not. But probably if we want to have that behaviour
        // when switching app, we want to continue counting, so maybe need different behaviour around
        // onStop?
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        counter.saveState(savedInstanceState);
    }

    private void touchListeners() {
        // Basically the whole screen, technically the counter and space around it.
        View text_counter = findViewById(R.id.text_counter);
        text_counter.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    counter.tap();
                }
                return true;
            }
        });

        // Pause button.
        View pause_button = findViewById(R.id.pause_button);
        pause_button.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    counter.pause();
                }
                return true;
            }
        });

        // Reset button.
        View reset_button = findViewById(R.id.reset_button);
        reset_button.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    counter.reset();
                }
                return true;
            }
        });

        // Logbook button.
        View logbook_button = findViewById(R.id.logbook_button);
        logbook_button.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    counter.pause();
                    Intent intent = new Intent(MainActivity.this, LogbookActivity.class);
                    startActivity(intent);
                }
                return true;
            }
        });

        // Scripts button.
        View scripts_button = findViewById(R.id.scripts_button);
        scripts_button.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    counter.pause();
                    Intent intent = new Intent(MainActivity.this, ScriptsActivity.class);
                    intent.putExtra("cur_index", counter.getScript());
                    startActivityForResult(intent, 0);
                }
                return true;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            counter.set_script(data.getIntExtra("index", 0), script);
        }
    }

    @Override
    public void renderStart() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void renderStop() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void render() {
        if (counter == null || script == null) {
            return;
        }

        String time_str = counter.getMins() + ":" + counter.getSeconds();
        TextView text_counter = (TextView) findViewById(R.id.text_counter);
        text_counter.setText(time_str);

        TextView text_reps = (TextView) findViewById(R.id.text_reps);
        text_reps.setText(counter.getReps());

        TextView text_label = (TextView) findViewById(R.id.text_label);
        text_label.setText(counter.getLabel());
    }
}

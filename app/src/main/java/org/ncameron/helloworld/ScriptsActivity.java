package org.ncameron.helloworld;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.ncameron.helloworld.scripts.Script;

public class ScriptsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scripts);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Script s = Script.read_script(getApplicationContext());
        if (s == null) {
            return;
        }
        String[] data = s.script_names();
        int cur_index = getIntent().getIntExtra("cur_index", 0);
        ListAdapter adapter = new ScriptsAdapter(getApplicationContext(), data, cur_index, this);
        ListView listView = (ListView) findViewById(R.id.scripts_list);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Back button.
        View back_button = findViewById(R.id.back_button);
        back_button.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ScriptsActivity.this.finish();
                }
                return true;
            }
        });
    }
}

class ScriptsAdapter extends ArrayAdapter<String> {
    ScriptsActivity activity;
    int cur_index;

    ScriptsAdapter(Context context, String[] data, int cur_index, ScriptsActivity activity) {
        super(context, R.layout.activity_scripts, data);
        this.activity = activity;
        this.cur_index = cur_index;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        String data = getItem(position);

        LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.content_scripts, null);

        TextView name_view = (TextView) view.findViewById(R.id.script_name);
        name_view.setText(data);
        name_view.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Intent intent = new Intent();
                    intent.putExtra("index", position);
                    activity.setResult(Activity.RESULT_OK, intent);
                    activity.finish();
                }
                return true;
            }
        });
        if (position == cur_index) {
            name_view.setBackgroundColor(0xFF404040);
        }


        return view;
    }
}

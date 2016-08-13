package org.ncameron.helloworld;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.ncameron.helloworld.logbook.Book;

public class LogbookActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logbook);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Book.LogBookData[] data = Book.read_data(getApplicationContext());
        if (data == null) {
            return;
        }
        ListAdapter adapter = new LogBookAdapter(getApplicationContext(), data);
        ListView listView = (ListView) findViewById(R.id.logbook_list);
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
                    LogbookActivity.this.finish();
                }
                return true;
            }
        });
    }
}

class LogBookAdapter extends ArrayAdapter<Book.LogBookData> {
    int length;

    LogBookAdapter(Context context, Book.LogBookData[] data) {
        super(context, R.layout.activity_logbook, data);
        length = data.length;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Book.LogBookData data = getItem(length - position - 1);

        LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.content_logbook, null);

        TextView date_view = (TextView) view.findViewById(R.id.log_date);
        date_view.setText(data.date_string);
        TextView name_view = (TextView) view.findViewById(R.id.log_name);
        name_view.setText(data.name_string);
        TextView count_view = (TextView) view.findViewById(R.id.log_count);
        count_view.setText(data.count_string);

        return view;
    }
}

package org.ncameron.helloworld.logbook;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ncameron.helloworld.scripts.Script;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class Book {
    Entry[] entries;

    private static String FILE_NAME = "apnea_logbook.json";

    public static class LogBookData {
        public String date_string;
        public String name_string;
        public String count_string;
    }

    public static LogBookData[] read_data(Context ctxt /*, Script script */) {
        JSONArray json = read_logbook(ctxt);
        Book logbook = from_json(json);
        if (logbook == null) {
            return null;
        }
        
        int length = Math.min(logbook.entries.length, 32);
        LogBookData[] result = new LogBookData[length];
        for (int i = 0; i < length; ++i) {
            result[i] = new LogBookData();
            result[i].date_string = logbook.entries[i].date_string();
            result[i].name_string = logbook.entries[i].name_string(/* script */);
            result[i].count_string = logbook.entries[i].count_string();
        }

        return result;
    }

    public static Book from_json(JSONArray input) {
        Book result = new Book();
        result.entries = new Entry[input.length()];
        try {
            for (int i = 0; i < input.length(); ++i) {
                result.entries[i] = Entry.from_json(input.getJSONObject(i));
            }
        } catch (JSONException e) {
            Log.e("Entry", "Error parsing logbook from JSON", e);
            return null;
        }

        return result;
    }

    public static JSONArray read_logbook(Context ctxt) {
        File file = new File(ctxt.getFilesDir(), FILE_NAME);
        if (file == null) {
            Log.e("Entry", "Error parsing logbook from JSON - unable to open logbook file");
            return new JSONArray();
        }
        if (!file.exists()) {
            Log.w("Entry", "Error parsing logbook from JSON - unable to find logbook file");
            return new JSONArray();
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return new JSONArray(sb.toString());
        } catch (IOException e) {
            Log.e("Entry", "Error reading logbook", e);
            return new JSONArray();
        } catch (JSONException e) {
            Log.e("Entry", "Error parsing logbook from JSON", e);
            return new JSONArray();
        }
    }

    static void write_logbook(Context ctxt, JSONArray json_logbook) {
        File file = new File(ctxt.getFilesDir(), FILE_NAME);
        if (file == null) {
            Log.e("Entry", "Error writing logbook - can't open logbook file");
            return;
        }

        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(json_logbook.toString().getBytes());
            out.close();
            Log.i("Entry", "written to " + file);
        } catch (IOException e) {
            Log.e("Entry", "Error writing logbook", e);
        }
    }

    public static void write_entry(Context ctxt, Entry entry) {
        JSONArray json_logbook = read_logbook(ctxt);
        JSONObject json_entry = entry.to_json();
        if (json_entry != null) {
            json_logbook.put(json_entry);
        }
        write_logbook(ctxt, json_logbook);
    }
}

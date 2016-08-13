package org.ncameron.helloworld.logbook;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ncameron.helloworld.scripts.Script;
import org.ncameron.helloworld.scripts.Template;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Entry {
    Date date;
    int completed;
    int count;
    String name;
    String template;
    int[] variables;

    private static SimpleDateFormat STORED_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd/MM/yy");

    public Entry(Date date, int completed, int count, String name, String template, int[] variables) {
        this.date = date;
        this.completed = completed;
        this.count = count;
        this.name = name;
        this.template = template;
        this.variables = variables;
    }

    public static Entry from_json(JSONObject input) {
        try {
            String raw_date = input.getString("date");
            JSONArray input_variables = input.getJSONArray("variables");
            int[] variables = new int[input_variables.length()];
            for (int i = 0; i < input_variables.length(); ++i) {
                variables[i] = input_variables.getInt(i);
            }
            return new Entry(STORED_DATE_FORMAT.parse(raw_date),
                    input.getInt("completed"),
                    input.getInt("count"),
                    input.getString("name"),
                    input.getString("template"),
                    variables);
        } catch (JSONException e) {
            Log.e("Entry", "Error parsing logbook entry from JSON", e);
            return null;
        } catch (ParseException e) {
            Log.e("Entry", "Error parsing logbook entry from JSON", e);
            return null;
        }
    }

    public JSONObject to_json() {
        try {
            JSONObject result = new JSONObject();
            result.put("date", STORED_DATE_FORMAT.format(this.date));
            result.put("completed", this.completed);
            result.put("count", this.count);
            result.put("name", this.name);
            result.put("template", this.template);
            JSONArray variables = new JSONArray();
            for (int v: this.variables) {
                variables.put(v);
            }
            result.put("variables", variables);
            return result;
        } catch (JSONException e) {
            Log.e("Entry", "Error writing logbook entry to JSON", e);
            return null;
        }
    }

    public String date_string() {
        return DISPLAY_DATE_FORMAT.format(this.date);
    }

    public String count_string() {
        return this.completed + "/" + this.count;
    }

    public String name_string(/*Script script*/) {
//        Template template = script.get_template(this.template);
        if (this.name == null || this.name.isEmpty()) {
            return this.template;
//            return template.display_name(this.variables);
        } else {
            return this.name + " (" + this.template + ")";
//            return this.name + " (" + template.display_name(this.variables) + ")";
        }
    }
}

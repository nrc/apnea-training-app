package org.ncameron.helloworld.scripts;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Template {
    String name;
    String count;
    String[] variables;
    Segment[] body;

    public static Template from_json(JSONObject input) {
        try {
            Template result = new Template();
            result.name = input.getString("name");
            result.count = input.getString("count");
            JSONArray variables = input.getJSONArray("variables");
            result.variables = new String[variables.length()];
            for (int i = 0; i < variables.length(); ++i) {
                result.variables[i] = variables.getString(i);
            }
            JSONArray body = input.getJSONArray("body");
            result.body = new Segment[body.length()];
            for (int i = 0; i < body.length(); ++i) {
                result.body[i] = Segment.from_json(body.getJSONObject(i));
            }
            return result;
        } catch (JSONException e) {
            Log.e("Template", "Error parsing template from JSON", e);
            return null;
        }
    }

    public String display_name(int[] variables) {
        return name + "<" + variable_string(variables) + ">";
    }

    String variable_string(int[] variables) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < this.variables.length; ++i) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(this.variables[i]);
            result.append(": ");
            result.append(variables[i]);
        }
        return result.toString();
    }

    public int eval(String input, int[] variables) {
        return eval(input, variables, -1);
    }

    public int eval(String input, int[] variables, int i) {
        // TODO
        return -1;
    }
}

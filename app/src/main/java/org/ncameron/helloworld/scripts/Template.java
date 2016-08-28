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

    // i is the iteration counter over the rounds of the script.
    public int eval(String input, int[] variables, int i) {
        // Grammar
        // var ::= [a..z]*
        // lit ::= [0..9]*
        // expr ::= var | lit | (expr binop expr)
        // binop ::= + | - | *

        Evaluator e = new Evaluator(this.variables, variables, i);
        return e.expr(input.trim());
    }
}

class Evaluator {
    int i;
    String[] formals;
    int[] actuals;

    Evaluator(String[] formals, int[] actuals, int i) {
        this.i = i;
        this.formals = formals;
        this.actuals = actuals;
    }

    String read_str(String input) {
        for (int i = 0; i < input.length(); ++i) {
            char cur = input.charAt(i);
            switch (cur) {
                case ' ':
                case '+':
                case '-':
                case '*':
                case '(':
                case ')':
                    return input.substring(0, i);
            }
        }

        return input;
    }

    int expr(String input) {
        if (input.isEmpty()) {
            Log.e("Evaluator", "Parsing empty string");
            return -1;
        }

        char first = input.charAt(0);
        if (first == '(') {
            if (input.length() < 5) {
                Log.e("Evaluator", "Incorrect input: " + input);
                return -1;
            }
            String remaining = input.substring(1).trim();
            String lhs_str = read_str(remaining);
            remaining = remaining.substring(lhs_str.length()).trim();
            if (remaining.length() < 2) {
                Log.e("Evaluator", "Incorrect input: " + input);
                return -1;
            }

            char op = remaining.charAt(0);
            remaining = remaining.substring(1).trim();
            String rhs_str = read_str(remaining);
            remaining = remaining.substring(rhs_str.length()).trim();
            if (remaining.charAt(0) != ')') {
                Log.e("Evaluator", "Unclosed paren in: " + input);
                return -1;
            }

            int lhs = expr(lhs_str);
            int rhs = expr(rhs_str);

            if (lhs < 0 || rhs < 0) {
                return -1;
            }

            switch (op) {
                case '+':
                    return lhs + rhs;
                case '-':
                    return lhs - rhs;
                case '*':
                    return lhs * rhs;
                default:
                    Log.e("Evaluator", "Unexpected operator: " + op);
                    return -1;
            }
        } else if (Character.isAlphabetic(first)) {
            return var(input);
        } else if (Character.isDigit(first)) {
            return lit(input);
        } else {
            Log.e("Evaluator", "Unexpected character: " + first + " in " + input);
            return -1;
        }
    }

    int var(String input) {
        String var = input.trim();

        if (var.equals("i")) {
            return this.i;
        }

        for (int j = 0; j < formals.length; ++j) {
            if (var.equals(formals[j])) {
                return this.actuals[j];
            }
        }

        Log.e("Evaluator", "Unknown variable: " + var);
        return -1;
    }

    int lit(String input) {
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            Log.e("Evaluator", "Bad int literal: " + input);
            return -1;
        }
    }
}

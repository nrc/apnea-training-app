package org.ncameron.helloworld.scripts;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Script {
    Template[] templates;
    Instance[] instances;

    // TODO make async
    public static Script read_script(Context context) {
        JSONObject json;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("programme.json")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            json = new JSONObject(sb.toString());
        } catch (IOException e) {
            Log.e("Script", "Error reading script", e);
            return null;
        } catch (JSONException e) {
            Log.e("Script", "Error parsing script from JSON", e);
            return null;
        }

        return from_json(json);
    }

    static Script from_json(JSONObject input) {
        try {
            JSONArray templates = input.getJSONArray("templates");
            JSONArray instances = input.getJSONArray("instances");

            Script result = new Script();
            result.templates = new Template[templates.length()];
            result.instances = new Instance[instances.length()];
            for (int i = 0; i < templates.length(); ++i) {
                result.templates[i] = Template.from_json(templates.getJSONObject(i));
            }
            for (int i = 0; i < instances.length(); ++i) {
                result.instances[i] = Instance.from_json(instances.getJSONObject(i));
            }
            return result;
        } catch (JSONException e) {
            Log.e("Script", "Error parsing script from JSON", e);
            return null;
        }
    }

    public String[] script_names() {
        ArrayList<CompiledInstance> compiled = compile_instances();
        String[] result = new String[compiled.size()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = compiled.get(i).display_name;
        }

        return result;
    }

    ArrayList<CompiledInstance> compile_instances() {
        ArrayList<CompiledInstance> result = new ArrayList<>();
        for (Instance instance: instances) {
            Template template = get_template(instance.template);
            if (template == null) {
                Log.e("Script", "Error compiling script instance - missing template");
                continue;
            }
            if (instance.variables.length != template.variables.length) {
                Log.e("Script", "Error compiling script instance - variable count mismatch");
                continue;
            }
            CompiledInstance compiled = new CompiledInstance();
            compiled.name = instance.name;
            compiled.template = template;
            compiled.variables = instance.variables;
            compiled.display_name = instance.display_name(template.display_name(instance.variables));
            result.add(compiled);
        }
        return result;
    }

    public Executable get_executable(int index) {
        ArrayList<CompiledInstance> compiled = compile_instances();
        if (index >= compiled.size()) {
            return null;
        }

        return compiled.get(index).to_executable();
    }

    public Template get_template(String name) {
        for (Template t: templates) {
            if (name.equals(t.name)) {
                return t;
            }
        }

        Log.e("Script", "Can't find template: " + name);
        return null;
    }
}

class Segment {
    String label;
    String time;
    boolean wait;
    boolean beep;

    public static Segment from_json(JSONObject input) {
        Segment result = new Segment();
        result.label = input.optString("label", null);
        result.time = input.optString("time", null);
        result.wait = input.optBoolean("wait", false);
        result.beep = input.optBoolean("beep", false);

        if (!result.wait && result.time.isEmpty()) {
            Log.e("Segment", "Error parsing script from JSON - missing time or wait in segment");
            return null;
        }
        return result;
    }

    ExecSegment to_executable(Template template, int[] variables, int i) {
        ExecSegment result = new ExecSegment();
        result.label = this.label;
        result.beep = this.beep;
        if (this.wait) {
            result.time = -1;
        } else {
            int time = template.eval(this.time, variables, i);
            if (time < 0) {
                return null;
            }
            result.time = time;
        }
        return result;
    }
}

class Instance {
    String name;
    String template;
    int[] variables;

    public static Instance from_json(JSONObject input) {
        try {
            Instance result = new Instance();
            result.name = input.optString("name", null);
            result.template = input.getString("template");
            JSONArray variables = input.getJSONArray("variables");
            result.variables = new int[variables.length()];
            for (int i = 0; i < variables.length(); ++i) {
                result.variables[i] = variables.getInt(i);
            }
            return result;
        } catch (JSONException e) {
            Log.e("Instance", "Error parsing script instance from JSON", e);
            return null;
        }
    }

    String display_name(String template_display_name) {
        if (this.name == null) {
            return template_display_name;
        } else {
            return this.name + " (" + template_display_name + ")";
        }
    }
}

class CompiledInstance {
    Template template;
    String name;
    String display_name;
    int[] variables;

    Executable to_executable() {
        Executable exec = new Executable();
        exec.instance = this;
        int count = template.eval(template.count, variables);
        exec.reps = new ExecRep[count];
        for (int i = 0; i < count; ++i) {
            exec.reps[i] = new ExecRep();
            ExecSegment[] body = new ExecSegment[template.body.length];
            for (int j = 0; j < template.body.length; ++j) {
                ExecSegment segment = template.body[j].to_executable(template, variables, i);
                if (segment == null) {
                    return null;
                }
                body[j] = segment;
            }
            exec.reps[i].segments = body;
        }
        return exec;
    }
}
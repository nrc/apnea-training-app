package org.ncameron.helloworld.scripts;

import android.util.Log;

public class Executable {
    ExecRep[] reps;
    CompiledInstance instance;

    int cur_rep;
    int cur_segment;

    Executable() {
        reset();
    }

    public void reset() {
        cur_rep = 0;
        cur_segment = 0;
    }

    // return null => finished
    public State next() {
        if (cur_rep >= reps.length) {
            return null;
        }

        ExecSegment segment = reps[cur_rep].segments[cur_segment];
        State result = new State();
        result.beep = segment.beep;
        if (segment.time < 0) {
            result.wait = true;
            ExecSegment next_segment = peek_segment();
            if (next_segment != null) {
                result.time = next_segment.time;
            }
        } else {
            result.time = segment.time;
        }
        result.reps = reps.length - cur_rep;
        if (segment.label != null) {
            result.label = segment.label;
        }

        cur_segment += 1;
        if (cur_segment >= reps[cur_rep].segments.length) {
            cur_segment = 0;
            cur_rep += 1;
        }

        return result;
    }

    ExecSegment peek_segment() {
        if (cur_segment == reps[cur_rep].segments.length - 1) {
            // Current segment is the last one

            if (cur_rep == reps.length - 1) {
                // Current rep is the last one, so no more segments.
                return null;
            }

            return reps[cur_rep + 1].segments[0];
        }

        return reps[cur_rep].segments[cur_segment + 1];
    }

    public int total_reps() {
        return reps.length;
    }

    public int init_seconds() {
        if (reps.length == 0) {
            return 0;
        }
        ExecRep rep = reps[0];
        if (rep.segments.length == 0) {
            return 0;
        }
        int result = rep.segments[0].time;
        if (result < 0) {
            return 0;
        }
        return result;
    }

    public String init_label() {
        if (reps.length == 0) {
            return "";
        }
        ExecRep rep = reps[0];
        if (rep.segments.length == 0) {
            return "";
        }
        return rep.segments[0].label;
    }

    public String name() {
        return instance.name;
    }

    public String template() {
        return instance.template.name;
    }

    public int[] variables() {
        return instance.variables;
    }

    public class State {
        public boolean beep = false;
        public boolean wait = false;
        public int time = 0;
        public int reps = 0;
        public String label = "";
    }
}

class ExecRep {
    ExecSegment[] segments;
}

class ExecSegment {
    String label;
    boolean beep;
    // time < 0 => wait for tap.
    int time;
}
package org.ncameron.helloworld.scripts;

public class Executable {
    ExecRep[] reps;
    CompiledInstance instance;

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
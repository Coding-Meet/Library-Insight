package com.meet.sample;

public class JavaLibrary {
    public static final String CONSTANT_VAL = "JAVA_RULEZ";
    
    private String name;
    public int count;

    public JavaLibrary(String name) {
        this.name = name;
        this.count = 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void increment() {
        this.count++;
    }

    @Deprecated
    public void deprecatedMethod() {
        // No-op
    }
}

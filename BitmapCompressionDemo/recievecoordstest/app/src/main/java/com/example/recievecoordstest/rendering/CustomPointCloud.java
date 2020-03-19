package com.example.recievecoordstest.rendering;

import java.nio.FloatBuffer;

public class CustomPointCloud {
    private FloatBuffer floatBuffer;
    private long timestamp;

    public CustomPointCloud(FloatBuffer floatBuffer, long timestamp){
        this.floatBuffer = floatBuffer;
        this.timestamp = timestamp;
    }

    public FloatBuffer getFloatBuffer() {
        return floatBuffer;
    }

    public void setFloatBuffer(FloatBuffer floatBuffer) {
        this.floatBuffer = floatBuffer;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}

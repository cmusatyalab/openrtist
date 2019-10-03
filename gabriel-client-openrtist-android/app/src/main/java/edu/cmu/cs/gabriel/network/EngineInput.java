package edu.cmu.cs.gabriel.network;

import android.hardware.Camera;

public class EngineInput {
    byte[] frame;
    Camera.Parameters parameters;
    String style_type;

    public EngineInput(byte[] frame, Camera.Parameters parameters, String style_type) {
        this.frame = frame;
        this.parameters = parameters;
        this.style_type = style_type;
    }
}

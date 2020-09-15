package edu.cmu.cs.gabriel.network;

import android.hardware.Camera;

public class EngineInput {
    final private byte[] frame;
    final private Camera.Parameters parameters;
    final private String styleType;

    public EngineInput(byte[] frame, Camera.Parameters parameters, String styleType) {
        this.frame = frame;
        this.parameters = parameters;
        this.styleType = styleType;
    }

    public byte[] getFrame() {
        return frame;
    }

    public Camera.Parameters getParameters() {
        return parameters;
    }

    public String getStyleType() {
        return styleType;
    }
}

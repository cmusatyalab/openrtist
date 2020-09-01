package edu.cmu.cs.gabriel.network;

import android.hardware.Camera;

public class EngineInput {
    final private byte[] frame;
//    final private byte[] depth_map;
//    final private Camera.Parameters parameters;
    final private int height;
    final private int width;
    final private String styleType;

//    public EngineInput(byte[] frame, byte[] depth_map, Camera.Parameters parameters, String styleType) {
    public EngineInput(byte[] frame, int height, int width, String styleType) {
        this.frame = frame;
//        this.depth_map = depth_map;
        this.height = height;
        this.width = width;
        this.styleType = styleType;
    }

    public byte[] getFrame() {
        return frame;
    }

//    public byte[] getDepth_map() {
//        return depth_map;
//    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public String getStyleType() {
        return styleType;
    }
}

package edu.cmu.cs.gabriel.network;

import android.media.Image;

public class EngineInput {
//    final private Image.Plane[] frame;
//    final private Image.Plane[] depth_map;
    final private byte[] frame;
    final private byte[] depth_map;
    final private int height;
    final private int width;
    final private String styleType;
    final private int depth_threshold;

    public EngineInput(byte[] frame, byte[] depth_map, int height, int width, String styleType, int depth_threshold) {
        this.frame = frame;
        this.depth_map = depth_map;
        this.height = height;
        this.width = width;
        this.styleType = styleType;
        this.depth_threshold = depth_threshold;
    }

    public byte[] getFrame() { return frame; }

    public byte[] getDepth_map() {
        return depth_map;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public String getStyleType() {
        return styleType;
    }

    public int getDepthThreshold() {
        return depth_threshold;
    }
}

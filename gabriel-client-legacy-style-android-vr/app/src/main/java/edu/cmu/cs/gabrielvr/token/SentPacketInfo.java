package edu.cmu.cs.gabrielvr.token;

public class SentPacketInfo {
    public long generatedTime;
    public long compressedTime;

    public SentPacketInfo(long generatedTime, long compressedTime) {
        this.generatedTime = generatedTime;
        this.compressedTime = compressedTime;
    }
}

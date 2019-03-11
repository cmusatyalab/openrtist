// Copyright 2018 Carnegie Mellon University
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package edu.cmu.cs.gabriel.token;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.network.NetworkProtocol;

public class TokenController {
    private static final String LOG_TAG = "TokenController";

    // the number of tokens remained
    private int currentToken = 0;

    // information about all sent packets, the key is the frameID and the value documents relevant timestamps
    private ConcurrentHashMap<Long, SentPacketInfo> sentPackets = new ConcurrentHashMap<Long, SentPacketInfo>();

    private Object tokenLock = new Object();

    private FileWriter fileWriter = null;

    private long  totalRTT = 0;
    private long framesSent = 1;


    // timestamp when the last ACK was received
    private long prevRecvFrameID = 0;

    public TokenController(int tokenSize, File resultSavingPath) {
        this.currentToken = tokenSize;
        if (Const.IS_EXPERIMENT) {
            try {
                fileWriter = new FileWriter(resultSavingPath);
                fileWriter.write("FrameID\tEngineID\tStartTime\tCompressedTime\tRecvTime\tDoneTime\tStatus\n");
            } catch (IOException e) {
                Log.e(LOG_TAG, "Result file cannot be properly opened", e);
            }
        }
    }

    public void reset() {
        this.currentToken = Const.TOKEN_SIZE;
        this.prevRecvFrameID = 0;
        this.sentPackets.clear();
    }

    public void writeString(String str) {
        try {
            Log.d(LOG_TAG, "writeString received string:" + str);
            fileWriter.write(str);
        } catch (IOException e) {}
    }

    public long getAvgRTT() {
        return (totalRTT /framesSent);
    }

    public Handler tokenHandler = new Handler() {

        public void handleMessage(Message msg) {
            if (msg.what == NetworkProtocol.NETWORK_RET_SYNC) {
                try {
                    if (Const.IS_EXPERIMENT){
                        String log = (String) msg.obj;
                        Log.i(LOG_TAG, "got message:" + log);
                        fileWriter.write(log);
                        fileWriter.flush();
                        Log.i(LOG_TAG, "why?");
                    }
                } catch (IOException e) {}
            }
            if (msg.what == NetworkProtocol.NETWORK_RET_TOKEN) {
                ReceivedPacketInfo receivedPacket = (ReceivedPacketInfo) msg.obj;
                long recvFrameID = receivedPacket.frameID;
                String recvEngineID = receivedPacket.engineID;

                // increase appropriate amount of tokens
                long increaseCount = 0;
                for (long frameID = prevRecvFrameID + 1; frameID < recvFrameID; frameID++) {
                    SentPacketInfo sentPacket = null;
                    if (Const.IS_EXPERIMENT) {
                        // Do not remove since we need to measure latency even for the late response
                        sentPacket = sentPackets.get(frameID);
                    } else {
                        sentPacket = sentPackets.remove(frameID);
                    }
                    if (sentPacket != null) {
                        increaseCount++;
                    }
                }
                increaseTokens(increaseCount);

                // deal with the current response
                SentPacketInfo sentPacket = sentPackets.get(recvFrameID);
                if (sentPacket != null) {
                    Log.d("Latency", "Frame ID: " + recvFrameID + " RTT: " + (receivedPacket.msgRecvTime - sentPacket.generatedTime));
                    totalRTT += (receivedPacket.msgRecvTime - sentPacket.generatedTime);
                    framesSent++;
                    // do not increase token if have already received duplicated ack
                    if (recvFrameID > prevRecvFrameID) {
                        increaseTokens(1);
                    }

                    if (Const.IS_EXPERIMENT) {
                        try {
                            String log = recvFrameID + "\t" + recvEngineID + "\t" +
                                    sentPacket.generatedTime + "\t" + sentPacket.compressedTime + "\t" +
                                    receivedPacket.msgRecvTime + "\t" + receivedPacket.guidanceDoneTime + "\t" +
                                    receivedPacket.status;
                            fileWriter.write(log + "\n");
                        } catch (IOException e) {}
                    }
                }
                if (recvFrameID > prevRecvFrameID)
                    prevRecvFrameID = recvFrameID;
            }
        }
    };

    public void logSentPacket(long frameID, long dataTime, long compressedTime) {
        this.sentPackets.put(frameID, new SentPacketInfo(dataTime, compressedTime));
    }

    /**
     * Blocks and only returns when token > 0
     * @return the current token number
     */
    public int getCurrentToken() {
        synchronized (tokenLock) {
            if (this.currentToken > 0) {
                return this.currentToken;
            } else {
                try {
                    tokenLock.wait();
                } catch (InterruptedException e) {}
                return this.currentToken;
            }
        }
    }

    public void increaseTokens(long count) {
        if (count == 0) return;
        synchronized (tokenLock) {
            this.currentToken += count;
            this.tokenLock.notify();
        }
    }

    public void decreaseToken() {
        synchronized (tokenLock) {
            if (this.currentToken > 0) {
                this.currentToken--;
            }
            this.tokenLock.notify();
        }
    }

    public void close() {
        sentPackets.clear();
        if (Const.IS_EXPERIMENT) {
            try {
                fileWriter.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error in closing latency file");
            }
        }
    }
}

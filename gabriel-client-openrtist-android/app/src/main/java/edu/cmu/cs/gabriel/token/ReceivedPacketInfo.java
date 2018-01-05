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

public class ReceivedPacketInfo {
    public long frameID;
    public String engineID;
    public String status;
    public long msgRecvTime;
    public long guidanceDoneTime;

    public ReceivedPacketInfo(long frameID, String engineID, String status) {
        this.frameID = frameID;
        this.engineID = engineID;
        this.status = status;
        this.msgRecvTime = -1;
        this.guidanceDoneTime = -1;
    }

    public void setMsgRecvTime(long time) {
        msgRecvTime = time;
    }

    public void setGuidanceDoneTime(long time) {
        guidanceDoneTime = time;
    }
}

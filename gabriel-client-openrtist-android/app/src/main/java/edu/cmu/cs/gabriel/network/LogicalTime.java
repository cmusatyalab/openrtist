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

package edu.cmu.cs.gabriel.network;

import java.util.concurrent.atomic.AtomicInteger;

import edu.cmu.cs.gabriel.Const;

public class LogicalTime {
    public AtomicInteger imageTime; // in # of frames
    public double audioTime; // in seconds

    public LogicalTime() {
        this.imageTime = new AtomicInteger(0);
        this.audioTime = 0;
    }

    public void increaseAudioTime(double n) { // n is in seconds
        this.audioTime += n;
        this.imageTime.set((int) (this.audioTime * 15));
    }

    public void increaseImageTime(int n) { // n is in # of frames
        this.imageTime.getAndAdd(n);
        this.audioTime = this.imageTime.doubleValue() / 15;
    }
}

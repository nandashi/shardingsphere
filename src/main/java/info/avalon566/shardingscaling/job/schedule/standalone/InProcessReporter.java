/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.avalon566.shardingscaling.job.schedule.standalone;

import info.avalon566.shardingscaling.job.schedule.Event;
import info.avalon566.shardingscaling.job.schedule.Reporter;
import lombok.var;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author avalon566
 */
public class InProcessReporter implements Reporter {

    private ConcurrentLinkedQueue<Event> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void report(Event event) {
        queue.offer(event);
    }

    @Override
    public Event consumeEvent() {
        while (true) {
            var event = queue.poll();
            if (null != event) {
                return event;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}

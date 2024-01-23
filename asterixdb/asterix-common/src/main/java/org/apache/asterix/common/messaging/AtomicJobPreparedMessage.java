/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.asterix.common.messaging;

import java.util.Map;

import org.apache.asterix.common.dataflow.ICcApplicationContext;
import org.apache.asterix.common.messaging.api.ICcAddressedMessage;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.api.job.JobId;
import org.apache.hyracks.storage.am.lsm.common.api.ILSMComponentId;

/**
 * Message sent from an NC to CC for every partition handled by it after all
 * the components generated by an atomic statement/job are flushed to disk.
 */
public class AtomicJobPreparedMessage implements ICcAddressedMessage {

    private static final long serialVersionUID = 1L;
    private final JobId jobId;
    private final String nodeId;
    private final Map<String, ILSMComponentId> componentIdMap;

    public AtomicJobPreparedMessage(JobId jobId, String nodeId, Map<String, ILSMComponentId> componentIdMap) {
        this.nodeId = nodeId;
        this.componentIdMap = componentIdMap;
        this.jobId = jobId;
    }

    @Override
    public void handle(ICcApplicationContext appCtx) throws HyracksDataException, InterruptedException {
        appCtx.getGlobalTxManager().handleJobPreparedMessage(jobId, nodeId, componentIdMap);
    }

    @Override
    public String toString() {
        return "AtomicJobPreparedMessage{" + "jobId=" + jobId + ", nodeId='" + nodeId + '\'' + '}';
    }
}
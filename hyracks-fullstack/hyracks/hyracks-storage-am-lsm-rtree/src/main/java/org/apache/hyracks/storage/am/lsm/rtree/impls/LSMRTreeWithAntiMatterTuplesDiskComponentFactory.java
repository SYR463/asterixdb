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

package org.apache.hyracks.storage.am.lsm.rtree.impls;

import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.storage.am.lsm.common.api.IComponentFilterHelper;
import org.apache.hyracks.storage.am.lsm.common.api.ILSMDiskComponentFactory;
import org.apache.hyracks.storage.am.lsm.common.impls.AbstractLSMIndex;
import org.apache.hyracks.storage.am.lsm.common.impls.LSMComponentFileReferences;
import org.apache.hyracks.storage.am.lsm.common.impls.TreeIndexFactory;
import org.apache.hyracks.storage.am.trtree.impls.RTree;

public class LSMRTreeWithAntiMatterTuplesDiskComponentFactory implements ILSMDiskComponentFactory {
    private final TreeIndexFactory<RTree> rtreeFactory;
    private final IComponentFilterHelper filterHelper;

    public LSMRTreeWithAntiMatterTuplesDiskComponentFactory(TreeIndexFactory<RTree> rtreeFactory,
            IComponentFilterHelper filterHelper) {
        this.rtreeFactory = rtreeFactory;
        this.filterHelper = filterHelper;
    }

    @Override
    public LSMRTreeWithAntimatterDiskComponent createComponent(AbstractLSMIndex lsmIndex,
            LSMComponentFileReferences cfr) throws HyracksDataException {
        return new LSMRTreeWithAntimatterDiskComponent(lsmIndex,
                rtreeFactory.createIndexInstance(cfr.getInsertIndexFileReference()),
                filterHelper == null ? null : filterHelper.createFilter());
    }
}

/*
 * Copyright (c) 2016, 2017, 2023 Memorial Sloan Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan Kettering Cancer
 * Center has been advised of the possibility of such damage.
 */

/*
 * This file is part of cBioPortal CMO-Pipelines.
 *
 * cBioPortal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.mskcc.cmo.ks.darwin.pipeline.mskimpactbrainspinetimeline;

import java.util.*;
import org.mskcc.cmo.ks.darwin.pipeline.model.MskimpactBrainSpineCompositeTimeline;
import org.mskcc.cmo.ks.darwin.pipeline.util.DarwinUtils;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author jake
 */
public class MskimpactTimelineBrainSpineCompositeToCompositeProcessor implements ItemProcessor<MskimpactBrainSpineCompositeTimeline, MskimpactBrainSpineCompositeTimeline> {

    @Autowired
    private DarwinUtils darwinUtils;

    private BrainSpineTimelineType type;

    public MskimpactTimelineBrainSpineCompositeToCompositeProcessor(BrainSpineTimelineType type) {
        this.type = type;
    }

    @Override
    public MskimpactBrainSpineCompositeTimeline process(final MskimpactBrainSpineCompositeTimeline composite) throws Exception{
        List<String> recordPost = new ArrayList<>();
        for (String field : (List<String>)composite.getRecord().getClass().getMethod("get" + type.toString() + "Fields").invoke(composite.getRecord())) {
            String value = composite.getRecord().getClass().getMethod("get" + field).invoke(composite.getRecord()).toString();
            recordPost.add(darwinUtils.convertWhitespace(value));
        }
        if(recordPost.contains(type.toString().toUpperCase())){
            composite.getClass().getMethod("set" + type.toString() + "Result", String.class).invoke(composite, String.join("\t", recordPost));
        }
        return composite;
    }
}

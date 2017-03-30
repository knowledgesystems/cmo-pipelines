/*
 * Copyright (c) 2016 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan-Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan-Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan-Kettering Cancer
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

import org.mskcc.cmo.ks.darwin.pipeline.model.MskimpactBrainSpineCompositeTimeline;
import org.mskcc.cmo.ks.darwin.pipeline.model.MskimpactBrainSpineTimeline;

import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.mskcc.cmo.ks.darwin.pipeline.BatchConfiguration;
import org.springframework.batch.item.ItemProcessor;
/**
 *
 * @author jake
 */
public class MskimpactTimelineBrainSpineModelToCompositeProcessor implements ItemProcessor<MskimpactBrainSpineTimeline, MskimpactBrainSpineCompositeTimeline>{
    private BatchConfiguration.BrainSpineTimelineType type;
    
    public MskimpactTimelineBrainSpineModelToCompositeProcessor(BatchConfiguration.BrainSpineTimelineType type) {
        this.type = type;
    }
    @Override
    public MskimpactBrainSpineCompositeTimeline process(final MskimpactBrainSpineTimeline timelineBrainSpine) throws Exception{
        MskimpactBrainSpineCompositeTimeline composite = new MskimpactBrainSpineCompositeTimeline(timelineBrainSpine);
        List<String> recordPost = new ArrayList<>();
        for(String field : (List<String>)composite.getRecord().getClass().getMethod("get" + type.toString() + "Fields").invoke(composite.getRecord())){
            recordPost.add(composite.getRecord().getClass().getMethod("get" + field).invoke(composite.getRecord()).toString());
        }
        if(recordPost.contains(type.toString().toUpperCase())){
            composite.getClass().getMethod("set" + type.toString() + "Result", String.class).invoke(composite, StringUtils.join(recordPost, "\t"));
        }
        
        return composite;
    }
}

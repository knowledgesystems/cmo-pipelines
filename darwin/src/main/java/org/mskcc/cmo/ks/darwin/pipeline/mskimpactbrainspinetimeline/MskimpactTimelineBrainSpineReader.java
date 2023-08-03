/*
 * Copyright (c) 2016, 2018, 2023  Memorial Sloan Kettering Cancer Center.
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

import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;
import java.util.*;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.darwin.pipeline.model.MskimpactBrainSpineTimeline;
import org.mskcc.cmo.ks.darwin.pipeline.util.DarwinSampleListUtil;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import static com.querydsl.core.alias.Alias.*;

/**
 *
 * @author jake
 */
public class MskimpactTimelineBrainSpineReader implements ItemStreamReader<MskimpactBrainSpineTimeline>{
    @Value("${darwin.timeline_view}")
    private String timelineBrainSpineView;

    @Autowired
    SQLQueryFactory darwinQueryFactory;

    @Autowired
    public DarwinSampleListUtil darwinSampleListUtil;

    private List<MskimpactBrainSpineTimeline> darwinTimelineResults;

    Logger log = Logger.getLogger(MskimpactTimelineBrainSpineReader.class);

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException{
        this.darwinTimelineResults = getDarwinTimelineResults();
        if (darwinTimelineResults == null || darwinTimelineResults.isEmpty()) {
            throw new ItemStreamException("Error fetching records from Darwin Brain Spine Timeline Views");
        }
    }

    // This method was added for unit testing of the read() method. An upgrade of mockito and jvp seems to not allow use of mocked objects (instantiated classes) into @Transactional blocks.
    public void openForTestingAndSetDarwinTimelineResults(List<MskimpactBrainSpineTimeline> testingDarwinTimelineResults) {
        darwinTimelineResults = testingDarwinTimelineResults;
    }

    @Transactional
    private List<MskimpactBrainSpineTimeline> getDarwinTimelineResults(){
        log.info("Start of Darwin Timeline Brain Spine View import...");
        MskimpactBrainSpineTimeline qDTR = alias(MskimpactBrainSpineTimeline.class, timelineBrainSpineView);
        List<MskimpactBrainSpineTimeline> darwinTimelineResults = darwinQueryFactory.selectDistinct(Projections.constructor(MskimpactBrainSpineTimeline.class, $(qDTR.getDMT_PATIENT_ID_BRAINSPINETMLN()),
                        $(qDTR.getDMP_PATIENT_ID_MIN_BRAINSPINETMLN()),
                        $(qDTR.getDMP_PATIENT_ID_MAX_BRAINSPINETMLN()), $(qDTR.getDMP_PATIENT_ID_COUNT_BRAINSPINETMLN()),
                        $(qDTR.getDMP_PATIENT_ID_ALL_BRAINSPINETMLN()), $(qDTR.getSTART_DATE()), $(qDTR.getSTOP_DATE()),
                        $(qDTR.getEVENT_TYPE()), $(qDTR.getTREATMENT_TYPE()), $(qDTR.getSUBTYPE()), $(qDTR.getAGENT()),
                        $(qDTR.getSPECIMEN_REFERENCE_NUMBER()), $(qDTR.getSPECIMEN_SITE()), $(qDTR.getSPECIMEN_TYPE()),
                        $(qDTR.getSTATUS()), $(qDTR.getKARNOFSKY_PERFORMANCE_SCORE()), $(qDTR.getSURGERY_DETAILS()),
                        $(qDTR.getEVENT_TYPE_DETAILED()), $(qDTR.getHISTOLOGY()), $(qDTR.getWHO_GRADE()),
                        $(qDTR.getMGMT_STATUS()), $(qDTR.getSOURCE_PATHOLOGY()), $(qDTR.getNOTE()), $(qDTR.getDIAGNOSTIC_TYPE()),
                        $(qDTR.getDIAGNOSTIC_TYPE_DETAILED()), $(qDTR.getSOURCE())))
                .from($(qDTR))
                .where($(qDTR.getDMT_PATIENT_ID_BRAINSPINETMLN()).isNotNull())
                .fetch();

        log.info("Imported " + darwinTimelineResults.size() + " records from Darwin Timeline Brain Spine View.");
        return darwinTimelineResults;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException{}

    @Override
    public void close() throws ItemStreamException{}

    @Override
    public MskimpactBrainSpineTimeline read() throws Exception{
        while(!darwinTimelineResults.isEmpty()){
            MskimpactBrainSpineTimeline darwinTimelineResult = darwinTimelineResults.remove(0);
            if (darwinTimelineResult.getSTART_DATE().equals("NA")) {
                darwinSampleListUtil.addFilteredMskimpactBrainSpineTimeline(darwinTimelineResult);
            } else {
                return darwinTimelineResult;
            }
        }
        return null;
    }
}

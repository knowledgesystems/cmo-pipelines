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
package org.mskcc.cmo.ks.darwin.pipeline.mskimpactbrainspineclinical;

import org.mskcc.cmo.ks.darwin.pipeline.model.MskimpactBrainSpineClinical;

import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;
import static com.querydsl.core.alias.Alias.*;

import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.apache.log4j.Logger;

import java.util.*;
import javax.annotation.Resource;

/**
 *
 * @author jake
 */
public class MskimpactBrainSpineClinicalReader implements ItemStreamReader<MskimpactBrainSpineClinical>{
    @Value("${darwin.clinical_view}")
    private String clinicalBrainSpineView;

    @Value("#{jobParameters[studyID]}")
    private String studyID;

    @Autowired
    SQLQueryFactory darwinQueryFactory;

    @Resource(name="studyIdRegexMap")
    Map<String, String> studyIdRegexMap;

    private List<MskimpactBrainSpineClinical> clinicalBrainSpineResults;

    Logger log = Logger.getLogger(MskimpactBrainSpineClinicalReader.class);

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException{
        this.clinicalBrainSpineResults = getClinicalBrainSpineResults();
    }

    @Transactional
    private List<MskimpactBrainSpineClinical> getClinicalBrainSpineResults(){
        log.info("Start of Clinical Brain Spine View Import...");
        MskimpactBrainSpineClinical qCBSR = alias(MskimpactBrainSpineClinical.class, clinicalBrainSpineView);
        List<MskimpactBrainSpineClinical> clinicalBrainSpineResults = darwinQueryFactory.select(Projections.constructor(MskimpactBrainSpineClinical.class,
                $(qCBSR.getDMP_PATIENT_ID_BRAINSPINECLIN()),
                $(qCBSR.getDMP_SAMPLE_ID_BRAINSPINECLIN()),
                $(qCBSR.getAGE()),
                $(qCBSR.getSEX()),
                $(qCBSR.getOS_STATUS()),
                $(qCBSR.getOS_MONTHS()),
                $(qCBSR.getDFS_STATUS()),
                $(qCBSR.getDFS_MONTHS()),
                $(qCBSR.getHISTOLOGY()),
                $(qCBSR.getWHO_GRADE()),
                $(qCBSR.getMGMT_STATUS())))
            .where($(qCBSR.getDMP_PATIENT_ID_BRAINSPINECLIN()).isNotEmpty().and($(qCBSR.getDMP_SAMPLE_ID_BRAINSPINECLIN()).like(studyIdRegexMap.get(studyID))))
            .from($(qCBSR))
            .fetch();

        log.info("Imported " + clinicalBrainSpineResults.size() + " records from Clinical Brain Spine View.");
        return clinicalBrainSpineResults;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException{}

    @Override
    public void close() throws ItemStreamException{}

    @Override
    public MskimpactBrainSpineClinical read() throws Exception{
        if(!clinicalBrainSpineResults.isEmpty()){
            return clinicalBrainSpineResults.remove(0);
        }
        return null;
    }

}

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
package org.mskcc.cmo.ks.darwin.pipeline.mskimpactdemographics;

import org.mskcc.cmo.ks.darwin.pipeline.model.*;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.apache.log4j.Logger;
import java.util.*;
import static com.querydsl.core.alias.Alias.$;
import static com.querydsl.core.alias.Alias.alias;
import javax.annotation.Resource;
/**
 *
 * @author jake
 */
public class MskimpactPatientDemographicsReader implements ItemStreamReader<MskimpactPatientDemographics>{
    @Value("${darwin.demographics_view}")
    private String patientDemographicsView;

    @Value("${darwin.icdo_view}")
    private String patientIcdoView;

    @Value("${darwin.latest_activity_view}")
    private String latestActivityView;

    @Value("${darwin.pathology_dmp_view}")
    private String pathologyDmpView;

    @Value("#{jobParameters[studyID]}")
    private String studyID;

    @Autowired
    SQLQueryFactory darwinQueryFactory;

    @Resource(name="studyIdRegexMap")
    Map<String, String> studyIdRegexMap;

    private List<MskimpactPatientDemographics> darwinDemographicsResults;
    private Set<String> processedIds = new HashSet<>();
    private Integer missingTM_DX_YEAR = 0;

    Logger log = Logger.getLogger(MskimpactPatientDemographicsReader.class);

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException{
        this.darwinDemographicsResults = getDarwinDemographicsResults();
    }

    @Transactional
    private List<MskimpactPatientDemographics> getDarwinDemographicsResults(){
        log.info("Start of Darwin Patient Demographics View Import...");
        MskimpactPatientDemographics qMskImpactPatientDemographics = alias(MskimpactPatientDemographics.class, patientDemographicsView);
        MskimpactPatientIcdoRecord qMskImpactPatientIcdoRecord = alias(MskimpactPatientIcdoRecord.class, patientIcdoView);
        MskimpactLatestActivity qMskImpactLatestActivity = alias(MskimpactLatestActivity.class, latestActivityView);
        MskimpactPathologyDmp qMskimpactPathologyDmp = alias(MskimpactPathologyDmp.class, pathologyDmpView);
        List<MskimpactPatientDemographics> darwinDemographicsResults = darwinQueryFactory.selectDistinct(Projections.constructor(MskimpactPatientDemographics.class,
                $(qMskImpactPatientDemographics.getDMP_ID_DEMO()),
                $(qMskImpactPatientDemographics.getGENDER()),
                $(qMskImpactPatientDemographics.getRACE()),
                $(qMskImpactPatientDemographics.getRELIGION()),
                $(qMskImpactPatientDemographics.getPT_VITAL_STATUS()),
                $(qMskImpactPatientDemographics.getPT_BIRTH_YEAR()),
                $(qMskImpactPatientDemographics.getPT_DEATH_YEAR()),
                $(qMskImpactPatientIcdoRecord.getTM_DX_YEAR()),
                $(qMskImpactLatestActivity.getAGE_AT_LAST_KNOWN_ALIVE_YEAR_IN_DAYS()),
                $(qMskImpactPatientIcdoRecord.getAGE_AT_TM_DX_DATE_IN_DAYS()),
                $(qMskImpactPatientDemographics.getAGE_AT_DATE_OF_DEATH_IN_DAYS())))
                .from($(qMskImpactPatientDemographics))
                .fullJoin($(qMskImpactPatientIcdoRecord))
                .on($(qMskImpactPatientDemographics.getDMP_ID_DEMO()).eq($(qMskImpactPatientIcdoRecord.getDMP_ID_ICDO())))
                .fullJoin($(qMskImpactLatestActivity))
                .on($(qMskImpactPatientDemographics.getDMP_ID_DEMO()).eq($(qMskImpactLatestActivity.getDMP_ID_PLA())))
                .innerJoin($(qMskimpactPathologyDmp))
                .on($(qMskImpactPatientDemographics.getPT_ID_DEMO()).eq($(qMskimpactPathologyDmp.getPT_ID_PATH_DMP())))
                .orderBy($(qMskImpactPatientIcdoRecord.getTM_DX_YEAR()).asc())
                .where($(qMskimpactPathologyDmp.getSAMPLE_ID_PATH_DMP()).like(studyIdRegexMap.get(studyID)))
                .fetch();
        return darwinDemographicsResults;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException{}

    @Override
    public void close() throws ItemStreamException{}

    @Override
    public MskimpactPatientDemographics read() throws Exception{
        return getRecord();
    }

    // Recursively remove items from darwinDemographicsResults looking for record to return.
    // Patients can have multiple entries in the icdo view - we only want to return the one with the oldest year
    // The records are ordered by TM_DX_YEAR
    private MskimpactPatientDemographics getRecord() {
        if (!darwinDemographicsResults.isEmpty()) {
            MskimpactPatientDemographics record = darwinDemographicsResults.remove(0);
            // Check if this patient has as already been processed. If it has, call this method again to find one that hasn't been.
            if (!processedIds.contains(record.getDMP_ID_DEMO())) {
                processedIds.add(record.getDMP_ID_DEMO());
                if(record.getTM_DX_YEAR().equals(-1)){
                    missingTM_DX_YEAR++;
                }
                return record;
            }
            return getRecord();
        }
        log.info("Imported " + processedIds.size() + " records from Demographics View.");
        log.info(missingTM_DX_YEAR + " records missing TM_DX_YEAR!");
        return null;
    }
}

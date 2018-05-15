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
import javax.annotation.Resource;
import static com.querydsl.core.alias.Alias.$;
import static com.querydsl.core.alias.Alias.alias;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    @Value("${darwin.naaccr.ethnicity_mapping_table}")
    private String naacrEthnicityMappingTable;

    @Value("${darwin.naaccr.race_mapping_table}")
    private String naacrRaceMappingTable;

    @Value("${darwin.naaccr.sex_mapping_table}")
    private String naacrSexMappingTable;

    @Value("#{jobParameters[studyID]}")
    private String studyID;

    @Resource(name="studyIdRegexMap")
    Map<String, Pattern> studyIdRegexMap;

    @Autowired
    SQLQueryFactory darwinQueryFactory;

    private List<MskimpactPatientDemographics> darwinDemographicsResults;
    private Set<String> processedIds = new HashSet<>();
    private Integer missingTM_DX_YEAR = 0;

    Logger log = Logger.getLogger(MskimpactPatientDemographicsReader.class);

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException{
        this.darwinDemographicsResults = getDarwinDemographicsResults();
        if (darwinDemographicsResults == null || darwinDemographicsResults.isEmpty()) {
            throw new ItemStreamException("Error fetching records from Darwin Demographics Views");
        }
    }

    @Transactional
    private List<MskimpactPatientDemographics> getDarwinDemographicsResults(){
        log.info("Start of Darwin Patient Demographics View Import...");

        Map<Integer, String> naaccrEthnicityMap = new HashMap<>();
        Map<Integer, String> naaccrRaceMap = new HashMap<>();
        Map<Integer, String> naaccrSexMap = new HashMap<>();

        log.info("Query NAACR tables for code mappings...");
        NAACCREthnicityMapping qNAACCREthnicityMapping = alias(NAACCREthnicityMapping.class, naacrEthnicityMappingTable);
        NAACCRRaceMapping qNAACCRRaceMapping = alias(NAACCRRaceMapping.class, naacrRaceMappingTable);
        NAACCRSexMapping qNAACCRSexMapping = alias(NAACCRSexMapping.class, naacrSexMappingTable);
        List<NAACCREthnicityMapping> naaccrEthnicityMappings = darwinQueryFactory.selectDistinct(Projections.constructor(NAACCREthnicityMapping.class,
                $(qNAACCREthnicityMapping.getNECM_CODE()),
                $(qNAACCREthnicityMapping.getNECM_CBIOPORTAL_LABEL())))
                .from($(qNAACCREthnicityMapping))
                .fetch();
        List<NAACCRRaceMapping> naaccrRaceMappings = darwinQueryFactory.selectDistinct(Projections.constructor(NAACCRRaceMapping.class,
                $(qNAACCRRaceMapping.getNRCM_CODE()),
                $(qNAACCRRaceMapping.getNRCM_CBIOPORTAL_LABEL())))
                .from($(qNAACCRRaceMapping))
                .fetch();
        List<NAACCRSexMapping> naaccrSexMappings = darwinQueryFactory.selectDistinct(Projections.constructor(NAACCRSexMapping.class,
                $(qNAACCRSexMapping.getNSCM_CODE()),
                $(qNAACCRSexMapping.getNSCM_CBIOPORTAL_LABEL())))
                .from($(qNAACCRSexMapping))
                .fetch();

        // Make maps out of the NAACCR lists for quick lookups
        for (NAACCREthnicityMapping ethnicityMapping : naaccrEthnicityMappings) {
            naaccrEthnicityMap.put(ethnicityMapping.getNECM_CODE(), ethnicityMapping.getNECM_CBIOPORTAL_LABEL());
        }
        for (NAACCRRaceMapping raceMapping : naaccrRaceMappings) {
            naaccrRaceMap.put(raceMapping.getNRCM_CODE(), raceMapping.getNRCM_CBIOPORTAL_LABEL());
        }
        for (NAACCRSexMapping sexMapping : naaccrSexMappings) {
            naaccrSexMap.put(sexMapping.getNSCM_CODE(), sexMapping.getNSCM_CBIOPORTAL_LABEL());
        }

        log.info("Query darwin tables for patient/sample demographics data...");
        MskimpactNAACCRClinical qMskimpactNAACCRClinical = alias(MskimpactNAACCRClinical.class, patientDemographicsView);
        MskimpactPatientDemographics qMskImpactPatientDemographics = alias(MskimpactPatientDemographics.class, patientDemographicsView);
        MskimpactPatientIcdoRecord qMskImpactPatientIcdoRecord = alias(MskimpactPatientIcdoRecord.class, patientIcdoView);
        MskimpactLatestActivity qMskImpactLatestActivity = alias(MskimpactLatestActivity.class, latestActivityView);
        MskimpactPathologyDmp qMskimpactPathologyDmp = alias(MskimpactPathologyDmp.class, pathologyDmpView);
        List<MskimpactPatientDemographics> darwinDemographicsResultsList = darwinQueryFactory.selectDistinct(Projections.constructor(MskimpactPatientDemographics.class,
                $(qMskImpactPatientDemographics.getDMP_ID_DEMO()),
                $(qMskImpactPatientDemographics.getPT_NAACCR_SEX_CODE()),
                $(qMskImpactPatientDemographics.getPT_NAACCR_RACE_CODE_PRIMARY()),
                $(qMskImpactPatientDemographics.getPT_NAACCR_ETHNICITY_CODE()),
                $(qMskImpactPatientDemographics.getRELIGION()),
                $(qMskImpactPatientDemographics.getPT_VITAL_STATUS()),
                $(qMskImpactPatientDemographics.getPT_BIRTH_YEAR()),
                $(qMskImpactPatientDemographics.getPT_DEATH_YEAR()),
                $(qMskImpactPatientIcdoRecord.getTM_DX_YEAR()),
                $(qMskImpactLatestActivity.getAGE_AT_LAST_KNOWN_ALIVE_YEAR_IN_DAYS()),
                $(qMskImpactPatientIcdoRecord.getAGE_AT_TM_DX_DATE_IN_DAYS()),
                $(qMskImpactPatientDemographics.getAGE_AT_DATE_OF_DEATH_IN_DAYS()),
                $(qMskImpactPatientDemographics.getPED_IND()),
                $(qMskimpactPathologyDmp.getSAMPLE_ID_PATH_DMP())))
                .from($(qMskImpactPatientDemographics))
                .fullJoin($(qMskImpactPatientIcdoRecord))
                .on($(qMskImpactPatientDemographics.getDMP_ID_DEMO()).eq($(qMskImpactPatientIcdoRecord.getDMP_ID_ICDO())))
                .fullJoin($(qMskImpactLatestActivity))
                .on($(qMskImpactPatientDemographics.getDMP_ID_DEMO()).eq($(qMskImpactLatestActivity.getDMP_ID_PLA())))
                .innerJoin($(qMskimpactPathologyDmp))
                .on($(qMskImpactPatientDemographics.getPT_ID_DEMO()).eq($(qMskimpactPathologyDmp.getPT_ID_PATH_DMP())))
                .orderBy($(qMskImpactPatientIcdoRecord.getTM_DX_YEAR()).asc())
                .fetch();

        // Translate the NAACCR codes for each result
        List<MskimpactPatientDemographics> filteredDarwinDemographicsResults = new ArrayList<>();
        for (MskimpactPatientDemographics result : darwinDemographicsResultsList) {
            Matcher matcher = studyIdRegexMap.get(studyID).matcher(result.getSAMPLE_ID_PATH_DMP());
            if (!matcher.matches()) {
                continue;
            }
            result.setGENDER(naaccrSexMap.getOrDefault(Integer.parseInt(result.getPT_NAACCR_SEX_CODE()), "NA"));
            result.setRACE(naaccrRaceMap.getOrDefault(Integer.parseInt(result.getPT_NAACCR_RACE_CODE_PRIMARY()), "NA"));
            result.setETHNICITY(naaccrEthnicityMap.getOrDefault(Integer.parseInt(result.getPT_NAACCR_ETHNICITY_CODE()), "NA"));
            filteredDarwinDemographicsResults.add(result);
        }
        return filteredDarwinDemographicsResults;
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

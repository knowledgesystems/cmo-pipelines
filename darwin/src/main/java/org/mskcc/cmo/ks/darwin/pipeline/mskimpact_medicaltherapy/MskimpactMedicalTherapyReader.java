/*
 * Copyright (c) 2017 Memorial Sloan-Kettering Cancer Center.
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
package org.mskcc.cmo.ks.darwin.pipeline.mskimpact_medicaltherapy;

import org.mskcc.cmo.ks.darwin.pipeline.model.*;

import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.core.types.Projections;
import com.querydsl.core.BooleanBuilder;
import static com.querydsl.core.alias.Alias.*;

import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import org.apache.log4j.Logger;
import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.map.MultiKeyMap;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Resource;

/**
 * Code reads information out of DVCBIO.PHARMACY_V looking
 * for records in which immunotherapy related drugs were dispensed.
 *
 * @author Benjamin Gross
 */
public class MskimpactMedicalTherapyReader implements ItemStreamReader<List<MskimpactMedicalTherapy>>
{
    private String pharmacyView = "PHARMACY_V";

    @Value("${darwin.pathology_dmp_view}")
    private String pathologyView;

    @Value("${darwin.medicaltherapy.pharmacy_druglist}")
    private String pharmacyViewDrugList;

    @Value("${darwin.medicaltherapy.skip_negative_dosage_records}")
    private Boolean skipNegativeDosageRecords;

    @Value("#{jobParameters[studyID]}")
    private String studyID;

    @Resource(name="studyIdRegexMap")
    Map<String, Pattern> studyIdRegexMap;

    @Autowired
    SQLQueryFactory darwinQueryFactory;

    private MultiKeyMap byPharmacyRecordAttributes;

    Logger log = Logger.getLogger(MskimpactMedicalTherapyReader.class);

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        List<MskimpactMedicalTherapy> pharmacyViewResults = getPharmacyViewResults();
        if (pharmacyViewResults == null || pharmacyViewResults.isEmpty()) {
            throw new ItemStreamException("Error fetching records from Darwin Pharmacy Views");
        }
        this.byPharmacyRecordAttributes = groupPharmacyViewResults(pharmacyViewResults);
    }

    @Transactional
    private List<MskimpactMedicalTherapy> getPharmacyViewResults() {

        if (log.isInfoEnabled()) {
            log.info("Start of " + pharmacyView + " record fetching...");
        }

        MskimpactMedicalTherapy pharmacyViewRecord = alias(MskimpactMedicalTherapy.class, pharmacyView);
        MskimpactPathologyDmp pathologyViewRecord = alias(MskimpactPathologyDmp.class, pathologyView);
        List<MskimpactMedicalTherapy> pharmacyViewResults =
            darwinQueryFactory.select(Projections.constructor(MskimpactMedicalTherapy.class,
                $(pharmacyViewRecord.getPT_ID_PHARMACY()),
                $(pharmacyViewRecord.getDMP_ID_PHARMACY()),
                $(pharmacyViewRecord.getAGE_AT_DISPENSE_DATE_IN_DAYS()),
                $(pharmacyViewRecord.getGENERIC_DRUG_NAME()),
                $(pharmacyViewRecord.getDOSAGE()),
                $(pharmacyViewRecord.getDOSE_UNIT()),
                $(pharmacyViewRecord.getDISPENSED_QUANTITY())))
            .from($(pharmacyViewRecord))
            .innerJoin($(pathologyViewRecord))
            .on($(pharmacyViewRecord.getPT_ID_PHARMACY()).eq($(pathologyViewRecord.getPT_ID_PATH_DMP())))
            .where(getDrugClause(pharmacyViewRecord))
            .fetch();

        List<MskimpactMedicalTherapy> filteredPharmacyViewResults = new ArrayList<>();
        for (MskimpactMedicalTherapy result : pharmacyViewResults) {
            Matcher matcher = studyIdRegexMap.get(studyID).matcher(result.getSAMPLE_ID_PATH_DMP());
            if (matcher.matches()) {
                filteredPharmacyViewResults.add(result);
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Fetched " + filteredPharmacyViewResults.size() + " records from " + pharmacyView + ".");
        }

        return filteredPharmacyViewResults;
    }

    /**
     * This method constructs part of the pharmacy view 'where' clause
     * such that only records that contain a drug or drugs of interest (provided by application.properties)
     * are returned from the query.
     */
    private BooleanBuilder getDrugClause(MskimpactMedicalTherapy pharmacyViewRecord) {
        BooleanBuilder builder = new BooleanBuilder();
        for (String drug : pharmacyViewDrugList.split(";")) {
            builder.or($(pharmacyViewRecord.getGENERIC_DRUG_NAME()).toUpperCase().like("%" + drug + "%"));
        }
        return builder;
    }

    /**
     * This method collects all pharmacy records for a patient into a list group by
     * DMP-ID/drug-name/dosage/dose-unit/dispensed quantity.
     */
    private MultiKeyMap groupPharmacyViewResults(List<MskimpactMedicalTherapy> pharmacyViewResults) {
        MultiKeyMap toReturn = new MultiKeyMap();
        for (MskimpactMedicalTherapy pharmacyViewResult : pharmacyViewResults) {
            if (skipNegativeDosageRecords && pharmacyViewResult.getDOSAGE() < 0) continue;
            if (!toReturn.containsKey(pharmacyViewResult.getDMP_ID_PHARMACY(),
                                      pharmacyViewResult.getGENERIC_DRUG_NAME(),
                                      pharmacyViewResult.getDOSAGE(),
                                      pharmacyViewResult.getDOSE_UNIT(),
                                      pharmacyViewResult.getDISPENSED_QUANTITY())) {
                List<MskimpactMedicalTherapy> newMedicalTherapyList = new ArrayList<MskimpactMedicalTherapy>();
                newMedicalTherapyList.add(pharmacyViewResult);
                toReturn.put(pharmacyViewResult.getDMP_ID_PHARMACY(),
                             pharmacyViewResult.getGENERIC_DRUG_NAME(),
                             pharmacyViewResult.getDOSAGE(),
                             pharmacyViewResult.getDOSE_UNIT(),
                             pharmacyViewResult.getDISPENSED_QUANTITY(),
                             newMedicalTherapyList);
            }
            else {
                List<MskimpactMedicalTherapy> existingMedicalTherapyList =
                    (List<MskimpactMedicalTherapy>)toReturn.get(pharmacyViewResult.getDMP_ID_PHARMACY(),
                                                                pharmacyViewResult.getGENERIC_DRUG_NAME(),
                                                                pharmacyViewResult.getDOSAGE(),
                                                                pharmacyViewResult.getDOSE_UNIT(),
                                                                pharmacyViewResult.getDISPENSED_QUANTITY());
                existingMedicalTherapyList.add(pharmacyViewResult);
                toReturn.put(pharmacyViewResult.getDMP_ID_PHARMACY(),
                             pharmacyViewResult.getGENERIC_DRUG_NAME(),
                             pharmacyViewResult.getDOSAGE(),
                             pharmacyViewResult.getDOSE_UNIT(),
                             pharmacyViewResult.getDISPENSED_QUANTITY(),
                             existingMedicalTherapyList);
            }
        }
        return toReturn;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException{}

    @Override
    public void close() throws ItemStreamException{}

    @Override
    public List<MskimpactMedicalTherapy> read() throws Exception {
        MapIterator it = byPharmacyRecordAttributes.mapIterator();
        if (it.hasNext()) {
            it.next(); // calling .next() allows us to make the subsequent getValue() call
            List<MskimpactMedicalTherapy> toReturn = (List<MskimpactMedicalTherapy>)it.getValue();
            it.remove();
            return toReturn;
        }
        return null;
    }

}

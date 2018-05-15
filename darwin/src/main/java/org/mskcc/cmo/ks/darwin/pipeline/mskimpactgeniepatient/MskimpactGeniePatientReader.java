/*
 * Copyright (c) 2016-2017 Memorial Sloan-Kettering Cancer Center.
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
package org.mskcc.cmo.ks.darwin.pipeline.mskimpactgeniepatient;

import org.mskcc.cmo.ks.darwin.pipeline.model.MskimpactNAACCRClinical;
import org.springframework.batch.item.*;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.log4j.Logger;
import java.util.*;
import static com.querydsl.core.alias.Alias.$;
import static com.querydsl.core.alias.Alias.alias;

/**
 *
 * @author heinsz
 */
public class MskimpactGeniePatientReader implements ItemStreamReader<MskimpactNAACCRClinical> {
    @Value("${darwin.demographics_view}")
    private String patientDemographicsView;

    @Autowired
    SQLQueryFactory darwinQueryFactory;

    private List<MskimpactNAACCRClinical> darwinDemographicsResults;

    Logger log = Logger.getLogger(MskimpactGeniePatientReader.class);

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        this.darwinDemographicsResults = getDarwinDemographicsResults();
        if (darwinDemographicsResults == null || darwinDemographicsResults.isEmpty()) {
            throw new ItemStreamException("Error fetching records from Darwin Demographics Views");
        }
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public MskimpactNAACCRClinical read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!darwinDemographicsResults.isEmpty()) {
            return darwinDemographicsResults.remove(0);
        }
        return null;
    }

    private List<MskimpactNAACCRClinical> getDarwinDemographicsResults() {
        log.info("Start of Darwin NAACCR View Import...");
        MskimpactNAACCRClinical qMskimpactNAACCRClinical = alias(MskimpactNAACCRClinical.class, patientDemographicsView);
        List<MskimpactNAACCRClinical> darwinDemographicsResultsList = darwinQueryFactory.selectDistinct(Projections.constructor(MskimpactNAACCRClinical.class,
                $(qMskimpactNAACCRClinical.getDMP_ID_DEMO()),
                $(qMskimpactNAACCRClinical.getPT_NAACCR_SEX_CODE()),
                $(qMskimpactNAACCRClinical.getPT_NAACCR_RACE_CODE_PRIMARY()),
                $(qMskimpactNAACCRClinical.getPT_NAACCR_RACE_CODE_SECONDARY()),
                $(qMskimpactNAACCRClinical.getPT_NAACCR_RACE_CODE_TERTIARY()),
                $(qMskimpactNAACCRClinical.getPT_NAACCR_ETHNICITY_CODE()),
                $(qMskimpactNAACCRClinical.getPT_BIRTH_YEAR())))
                .from($(qMskimpactNAACCRClinical))
                .fetch();
       return darwinDemographicsResultsList;
    }

}

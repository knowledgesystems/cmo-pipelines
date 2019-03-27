/*
 * Copyright (c) 2019 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.crdb.pipeline;

import static com.querydsl.core.alias.Alias.$;
import static com.querydsl.core.alias.Alias.alias;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;
import java.util.*;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.crdb.pipeline.model.CRDBPDXClinicalAnnotationMapping;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

/**
 * Class for querying the CRDB PDX Clinical Annotations Mapping view.
 *
 * @author Avery Wang
 */

public class CRDBPDXClinicalAnnotationMappingReader implements ItemStreamReader<CRDBPDXClinicalAnnotationMapping> {

    @Value("${crdb.pdx_clinical_annotation_mappings_view}")
    private String crdbPDXClinicalAnnotationMappingView;

    @Autowired
    SQLQueryFactory crdbQueryFactory;

    private List<CRDBPDXClinicalAnnotationMapping> crdbPDXClinicalAnnotationMappingResults;
    private final Logger LOG = Logger.getLogger(CRDBPDXClinicalAnnotationMappingReader.class);

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.crdbPDXClinicalAnnotationMappingResults = getCrdbPDXClinicalAnnotationMappingResults();
        if (crdbPDXClinicalAnnotationMappingResults.isEmpty()) {
            throw new ItemStreamException("Error fetching records from CRDB PDX Clinical Annotations Mapping View");
        }
    }

    /**
     * Creates an alias for the CRDB PDX Clinical Annotations Mapping view query type and projects query as
     * a list of CRDBPDXClinicalAnnotationMapping objects
     *
     * @return List<CRDBPDXClinicalAnnotationMapping>
     */
    @Transactional
    private List<CRDBPDXClinicalAnnotationMapping> getCrdbPDXClinicalAnnotationMappingResults() {
        LOG.info("Beginning CRDB PDX Clinical Annotations Mapping View import...");

        CRDBPDXClinicalAnnotationMapping qCRDBD = alias(CRDBPDXClinicalAnnotationMapping.class, crdbPDXClinicalAnnotationMappingView);
        List<CRDBPDXClinicalAnnotationMapping> crdbPDXClinicalAnnotationMappingResults = crdbQueryFactory.selectDistinct(
                Projections.constructor(CRDBPDXClinicalAnnotationMapping.class, $(qCRDBD.getSOURCE_STUDY_ID()), 
                    $(qCRDBD.getCLINICAL_ATTRIBUTE()), $(qCRDBD.getDESTINATION_STUDY_ID())))
                .from($(qCRDBD))
                .fetch();

        LOG.info("Imported " + crdbPDXClinicalAnnotationMappingResults.size() + " records from CRDB PDX Clinical Annotations Mapping View.");
        return crdbPDXClinicalAnnotationMappingResults;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public CRDBPDXClinicalAnnotationMapping read() throws Exception {
        if (!crdbPDXClinicalAnnotationMappingResults.isEmpty()) {
            return crdbPDXClinicalAnnotationMappingResults.remove(0);
        }
        return null;
    }
}

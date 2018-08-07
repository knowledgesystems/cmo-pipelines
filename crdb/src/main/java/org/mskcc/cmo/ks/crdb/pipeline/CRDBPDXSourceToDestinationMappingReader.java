/*
 * Copyright (c) 2016-2018 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.crdb;

import org.mskcc.cmo.ks.crdb.model.CRDBPDXSourceToDestinationMapping;

import static com.querydsl.core.alias.Alias.$;
import static com.querydsl.core.alias.Alias.alias;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;

import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Class for querying the CRDB PDX Source To Destination Mapping view.
 *
 * @author ochoaa
 */

public class CRDBPDXSourceToDestinationMappingReader implements ItemStreamReader<CRDBPDXSourceToDestinationMapping> {
    @Value("${crdb.source_to_destination_mappings_view}")
    private String crdbPDXSourceToDestinationMappingView;

    @Autowired
    SQLQueryFactory crdbQueryFactory;

    private List<CRDBPDXSourceToDestinationMapping> crdbPDXSourceToDestinationMappingResults;

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.crdbPDXSourceToDestinationMappingResults = getCrdbPDXSourceToDestinationMappingResults();
        if (crdbPDXSourceToDestinationMappingResults.isEmpty()) {
            throw new ItemStreamException("Error fetching records from CRDB PDX Source To Destination Mapping View");
        }
    }

    /**
     * Creates an alias for the CRDB PDX Source To Destination Mapping view query type and projects query as
     * a list of CRDBPDXSourceToDestinationMapping objects
     *
     * @return List<CRDBPDXSourceToDestinationMapping>
     */
    @Transactional
    private List<CRDBPDXSourceToDestinationMapping> getCrdbPDXSourceToDestinationMappingResults() {
        System.out.println("Beginning CRDB PDX Source To Destination Mapping View import...");

        CRDBPDXSourceToDestinationMapping qCRDBD = alias(CRDBPDXSourceToDestinationMapping.class, crdbPDXSourceToDestinationMappingView);
        List<CRDBPDXSourceToDestinationMapping> crdbPDXSourceToDestinationMappingResults = crdbQueryFactory.selectDistinct(
                Projections.constructor(CRDBPDXSourceToDestinationMapping.class, $(qCRDBD.getPATIENT_ID()),
                    $(qCRDBD.getSOURCE_STUDY_ID()), $(qCRDBD.getDESTINATION_STUDY_ID()),
                    $(qCRDBD.getDESTINATION_PATIENT_ID())))
                .from($(qCRDBD))
                .fetch();

        System.out.println("Imported " + crdbPDXSourceToDestinationMappingResults.size() + " records from CRDB PDX Source To Destination Mapping View.");
        return crdbPDXSourceToDestinationMappingResults;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public CRDBPDXSourceToDestinationMapping read() throws Exception {
        if (!crdbPDXSourceToDestinationMappingResults.isEmpty()) {
            return crdbPDXSourceToDestinationMappingResults.remove(0);
        }
        return null;
    }
}

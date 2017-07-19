/*
 * Copyright (c) 2016 - 2017 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.redcap.pipeline;

import java.util.*;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.source.*;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author heinsz
 */
public class TimelineReader implements ItemStreamReader<Map<String, String>> {

    @Autowired
    public ClinicalDataSource clinicalDataSource;

    @Autowired
    public MetadataManager metadataManager;

    @Value("#{jobParameters[stableId]}")
    public String stableId;

    private final Logger log = Logger.getLogger(ClinicalDataReader.class);

    public List<Map<String, String>> records = new ArrayList<>();

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        String projectTitle = clinicalDataSource.getNextTimelineProjectTitle(stableId);
        ec.put("projectId", projectTitle);
        log.info("Getting timeline header for project: " + projectTitle);
        ec.put("combinedHeader", clinicalDataSource.getTimelineHeader(stableId));
        records = clinicalDataSource.getTimelineData(stableId);
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public Map<String, String> read() throws Exception {
        if (!records.isEmpty()) {
            return records.remove(0);
        }
        return null;
    }
}

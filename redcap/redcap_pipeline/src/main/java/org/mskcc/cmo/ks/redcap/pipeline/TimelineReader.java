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

    @Value("#{jobParameters[rawData]}")
    private Boolean rawData;

    @Value("#{jobParameters[redcapProjectTitle]}")
    private String redcapProjectTitle;

    @Value("#{jobParameters[stableId]}")
    public String stableId;

    private final Logger log = Logger.getLogger(ClinicalDataReader.class);

    public List<Map<String, String>> timelineRecords = new ArrayList<>();
    private List<String> timelineHeader = new ArrayList<>();

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        boolean writeTimelineData = true;
        if (redcapProjectTitle == null) {
            writeTimelineData = clinicalDataSource.hasMoreTimelineData(stableId);
        }
        if (writeTimelineData) {
            String projectTitle = (redcapProjectTitle == null) ? clinicalDataSource.getNextTimelineProjectTitle(stableId) : redcapProjectTitle;
            log.info("Getting timeline header for project: " + projectTitle);
            timelineHeader = clinicalDataSource.getProjectHeader(projectTitle);
            timelineRecords = clinicalDataSource.exportRawDataForProjectTitle(projectTitle);
            if (!rawData) {
                // merge remaining timeline data sources if in merge mode and more timeline data exists
                if (clinicalDataSource.hasMoreTimelineData(stableId)) {
                    mergeTimelineDataSources();
                }
            }
            // update execution context with project title and full timeline header        
            ec.put("projectTitle", projectTitle);
            ec.put("timelineHeader", timelineHeader);
        }
        else {
            String message = "No timeline data for ";
            if (stableId != null) {
                message += "stable id: " + stableId;
            }
            else {
                message += "redcap project title: " + redcapProjectTitle;
            }
            log.warn(message);
        }
        ec.put("writeTimelineData", writeTimelineData);
    }
    
    private void mergeTimelineDataSources() {
        while (clinicalDataSource.hasMoreTimelineData(stableId)) {
            String projectTitle = clinicalDataSource.getNextTimelineProjectTitle(stableId);
            
            // get timeline data header for project and merge with global timeline header
            log.info("Merging timeline data for project: " + projectTitle);
            List<String> header = clinicalDataSource.getTimelineHeader(stableId);
            for (String column : header) {
                if (timelineHeader.contains(column)) {
                    continue;
                }
                timelineHeader.add(column);
            }
            // now add all timeline data records for current project
            timelineRecords.addAll(clinicalDataSource.getTimelineData(stableId));
        }
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public Map<String, String> read() throws Exception {
        if (!timelineRecords.isEmpty()) {
            return timelineRecords.remove(0);
        }
        return null;
    }
}

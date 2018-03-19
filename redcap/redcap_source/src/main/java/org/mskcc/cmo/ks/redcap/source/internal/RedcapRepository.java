/*
 * Copyright (c) 2017 - 2018 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.redcap.source.internal;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.models.RedcapAttributeMetadata;
import org.mskcc.cmo.ks.redcap.models.RedcapProjectAttribute;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Repository;

@Repository
public class RedcapRepository {

    @Autowired
    private MetadataCache metadataCache;

    @Autowired
    private RedcapSessionManager redcapSessionManager;

    private final Logger log = Logger.getLogger(RedcapRepository.class);

    public boolean projectExists(String projectTitle) {
        return redcapSessionManager.getTokenByProjectTitle(projectTitle) != null;
    }

    public boolean redcapDataTypeIsTimeline(String projectTitle) {
        return redcapSessionManager.redcapDataTypeIsTimeline(projectTitle);
    }

    //TODO: eliminate this function and make upper layers unaware of token handling
    public String getTokenByProjectTitle(String projectTitle) {
        return redcapSessionManager.getTokenByProjectTitle(projectTitle);
    }

    //TODO: eliminate this function and make upper layers unaware of token handling
    public Map<String, String> getClinicalTokenMapByStableId(String stableId) {
        return redcapSessionManager.getClinicalTokenMapByStableId(stableId);
    }

    //TODO: eliminate this function and make upper layers unaware of token handling
    public Map<String, String> getTimelineTokenMapByStableId(String stableId) {
        return redcapSessionManager.getTimelineTokenMapByStableId(stableId);
    }

    //TODO: change this to accept the project name instead .. not token
    public void deleteRedcapProjectData(String projectToken) {
        redcapSessionManager.deleteRedcapProjectData(projectToken);
    }

    //TODO: drop this function
    public void importClinicalData(String projectToken, String dataForImport) {
        redcapSessionManager.importClinicalData(projectToken, dataForImport);
    }

    public List<RedcapProjectAttribute> getAttributesByToken(String projectToken) {
        RedcapProjectAttribute[] redcapAttributeByToken = redcapSessionManager.getRedcapAttributeByToken(projectToken);
        List<RedcapProjectAttribute> redcapProjectAttributeList = new ArrayList<>(redcapAttributeByToken.length);
        String redcapInstrumentCompleteFieldName = redcapSessionManager.getRedcapInstrumentNameByToken(projectToken) + "_complete";
        for (RedcapProjectAttribute redcapProjectAttribute : redcapAttributeByToken) {
            if (!redcapInstrumentCompleteFieldName.equals(redcapProjectAttribute.getFieldName())) {
                redcapProjectAttributeList.add(redcapProjectAttribute);
            }
        }
        return redcapProjectAttributeList;
    }

    public List<Map<String, String>> getRedcapDataForProject(String projectToken) {
        JsonNode[] redcapDataRecords = redcapSessionManager.getRedcapDataForProjectByToken(projectToken);
        //TODO : we could eliminate the next line if we store the instrument name at the time the the headers are requested through ClinicalDataSource.get[Project|Sample|Patient]Header()
        String redcapInstrumentCompleteFieldName = redcapSessionManager.getRedcapInstrumentNameByToken(projectToken) + "_complete";
        List<Map<String, String>> redcapDataForProject = new ArrayList<>();
        for (JsonNode redcapResponse : redcapDataRecords) {
            Map<String, String> redcapDataRecord = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> redcapNodeIterator = redcapResponse.fields();
            while (redcapNodeIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>)redcapNodeIterator.next();
                String redcapId = entry.getKey();
                RedcapAttributeMetadata metadata = null;
                if (redcapId.equals(redcapInstrumentCompleteFieldName)) {
                    continue;
                }
                try {
                    metadata = metadataCache.getMetadataByRedcapId(redcapId);
                } catch (RuntimeException e) {
                    String errorString = "Error: attempt to export data from redcap failed due to redcap_id " +
                            redcapId + " not having metadata defined in the Google clinical attributes worksheet";
                    log.warn(errorString);
                    throw new RuntimeException(errorString);
                }
                redcapDataRecord.put(metadata.getNormalizedColumnHeader(), entry.getValue().asText());
            }
            redcapDataForProject.add(redcapDataRecord);
        }
        return redcapDataForProject;
    }

    /** add record_id column if missing in data file contents and present in redcap project */
    //TODO make this method private
    public void adjustDataForRedcapImport(List<String> dataFileContentsTSV, String projectToken) {
        if (dataFileContentsTSV.get(0).startsWith(RedcapSessionManager.REDCAP_FIELD_NAME_FOR_RECORD_ID)) {
            return; // RECORD_ID field is already the first field in the file
        }
        Integer maximumRecordIdInProject = redcapSessionManager.getMaximumRecordIdInRedcapProjectIfPresent(projectToken);
        if (maximumRecordIdInProject == null) {
            return; // record_id field is not present in project
        }
        int nextRecordId = maximumRecordIdInProject + 1;
        boolean headerHandled = false;
        for (int index = 0; index < dataFileContentsTSV.size(); index++) {
            if (headerHandled) {
                String expandedLine = Integer.toString(nextRecordId) + "\t" + dataFileContentsTSV.get(index);
                dataFileContentsTSV.set(index, expandedLine);
                nextRecordId = nextRecordId + 1;
            } else {
                String expandedLine = RedcapSessionManager.REDCAP_FIELD_NAME_FOR_RECORD_ID + "\t" + dataFileContentsTSV.get(index);
                dataFileContentsTSV.set(index, expandedLine);
                headerHandled = true;
            }
        }
    }

}

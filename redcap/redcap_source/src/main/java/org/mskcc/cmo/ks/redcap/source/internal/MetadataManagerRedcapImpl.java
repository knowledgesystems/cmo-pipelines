/*
 * Copyright (c) 2016 - 2018 Memorial Sloan-Kettering Cancer Center.
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

import java.util.*;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.models.RedcapAttributeMetadata;
import org.mskcc.cmo.ks.redcap.models.OverriddenCancerStudy;
import org.mskcc.cmo.ks.redcap.models.RedcapToken;
import org.mskcc.cmo.ks.redcap.source.MetadataManager;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Zachary Heins, Avery Wang
 *
 * Use Redcap to fetch clinical metadata and data
 *
 */

@Repository
public class MetadataManagerRedcapImpl implements MetadataManager {

    @Autowired
    private MetadataCache metadataCache;

    @Autowired
    private CDDSessionManager cddSessionManager;

    private final Logger log = Logger.getLogger(MetadataManagerRedcapImpl.class);

    @Override
    public Map<String, List<String>> getFullHeader(List<String> header) {
        Map<String, RedcapAttributeMetadata> combinedAttributeMap = new LinkedHashMap<>();
        for (String attribute : header) {
            combinedAttributeMap.put(attribute, metadataCache.getMetadataByNormalizedColumnHeader(attribute));
        }
        return makeHeader(combinedAttributeMap);
    }

    /**
     * Generates list of patient attributes from the passed in fullHeader
     * @param fullHeader
     * @return
     */
    @Override
    public Map<String, List<String>> getFullPatientHeader(Map<String, List<String>> fullHeader) {
        return filterFullHeader(fullHeader, "attribute_types", "PATIENT");
    }

    /**
     * Generates list of sample attributes from the passed in fullHeader
     * @param fullHeader
     * @return
     */
    @Override
    public Map<String, List<String>> getFullSampleHeader(Map<String, List<String>> fullHeader) {
        return filterFullHeader(fullHeader, "attribute_types", "SAMPLE");
    }

    @Override
    public boolean allHeadersAreValidClinicalAttributes(List<String> headers) {
        ArrayList<String> invalidHeaders = new ArrayList<>();
        for (String header : headers) {
            try {
                metadataCache.getMetadataByNormalizedColumnHeader(header);
            } catch (RuntimeException e) {
                invalidHeaders.add(header);
            }
        }
        if (invalidHeaders.size() > 0) {
            StringBuilder message = new StringBuilder("Invalid headers (not defined in Clinical Data Dictionary) :");
            for (String header : invalidHeaders) {
                message.append(" " + header);
            }
            throw new RuntimeException(message.toString());
        }
        return true;
    }

    private Map<String, List<String>> filterFullHeader(Map<String, List<String>> fullHeader, String metadataAttributeKey, String metadataAttributeValue) {
        List<String> displayNames = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();
        List<String> datatypes = new ArrayList<>();
        List<String> priorities = new ArrayList<>();
        List<String> header = new ArrayList<>();
        if (fullHeader != null) {
            List<String> metadataAttributeValues = fullHeader.get(metadataAttributeKey);
            for (int i=0; i<fullHeader.get("header").size(); i++) {
                if (metadataAttributeValues == null || metadataAttributeValues.get(i).equals(metadataAttributeValue)) {
                    displayNames.add(fullHeader.get("display_names").get(i));
                    descriptions.add(fullHeader.get("descriptions").get(i));
                    datatypes.add(fullHeader.get("datatypes").get(i));
                    priorities.add(fullHeader.get("priorities").get(i));
                    header.add(fullHeader.get("header").get(i));
                }
            }
        }
        Map<String, List<String>> filteredFullHeader = new HashMap<>();
        filteredFullHeader.put("display_names", displayNames);
        filteredFullHeader.put("descriptions", descriptions);
        filteredFullHeader.put("datatypes", datatypes);
        filteredFullHeader.put("priorities", priorities);
        filteredFullHeader.put("header", header);
        return filteredFullHeader;
    }

    private List<RedcapAttributeMetadata> getMetadata() {
        return metadataCache.getMetadata();
    }

    public boolean checkOverridesExist(String studyId) {
        List<OverriddenCancerStudy> listOfOverriddenStudies = null;
        try {
            listOfOverriddenStudies = Arrays.asList(cddSessionManager.getOverriddenStudies(studyId));
            for (OverriddenCancerStudy overriddenStudy : listOfOverriddenStudies) {
                if (overriddenStudy.getName().equals(studyId)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return false;
    }

    public void setOverrideStudyId(String studyId) {
        metadataCache.setOverrideStudyId(studyId);
    }

    private Map<String,List<String>> makeHeader(Map<String, RedcapAttributeMetadata> attributeMap) {
        Map<String, List<String>> headerMap = new LinkedHashMap<>();
        List<String> displayNames = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();
        List<String> datatypes = new ArrayList<>();
        List<String> priorities = new ArrayList<>();
        List<String> attributeTypes = new ArrayList<>();
        List<String> header = new ArrayList<>();

        for (Map.Entry<String, RedcapAttributeMetadata> entry : attributeMap.entrySet()) {
            displayNames.add(entry.getValue().getDisplayName());
            descriptions.add(entry.getValue().getDescriptions());
            datatypes.add(entry.getValue().getDatatype());
            priorities.add(entry.getValue().getPriority());
            attributeTypes.add(entry.getValue().getAttributeType());
            header.add(entry.getValue().getNormalizedColumnHeader());
        }

        headerMap.put("display_names", displayNames);
        headerMap.put("descriptions", descriptions);
        headerMap.put("datatypes", datatypes);
        headerMap.put("priorities", priorities);
        headerMap.put("attribute_types", attributeTypes);
        headerMap.put("header", header);
        return headerMap;
    }

}

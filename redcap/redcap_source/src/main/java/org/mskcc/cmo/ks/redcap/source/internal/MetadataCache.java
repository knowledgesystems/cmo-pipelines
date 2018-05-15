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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.models.RedcapAttributeMetadata;
import org.mskcc.cmo.ks.redcap.models.RedcapProjectAttribute;
import org.mskcc.cmo.ks.redcap.models.RedcapToken;
import org.mskcc.cmo.ks.redcap.source.ClinicalDataSource;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class MetadataCache {

    @Autowired
    private CDDSessionManager cddSessionManager;

    private RedcapAttributeMetadata[] metadataArray = null;
    // mapping of normalized column header name to "RedcapProjectAttribute" object
    // where normalized column header is all caps and no spaces (i.e SAMPLE_ID : RedcapProjectAttribute(sample_id))
    private Map<String, RedcapAttributeMetadata> normalizedColumnHeaderToMetadata = null;
    private String overrideStudyId = null;
    private final Logger log = Logger.getLogger(MetadataCache.class);

    public MetadataCache() {}

    public void setOverrideStudyId(String studyId) {
        if (metadataArray != null) {
            String errorString = "Error : Attempting to set override studyId when metadata cache is already initialized.";
            log.warn(errorString);
            throw new RuntimeException(errorString);
        } else {
            overrideStudyId = studyId;
        }
    }

    public String getOverrideStudyId() {
        return overrideStudyId;
    }

    public RedcapAttributeMetadata getMetadataByNormalizedColumnHeader(String normalizedColumnHeader) {
        ensureThatCacheIsInitialized();
        RedcapAttributeMetadata redcapAttributeMetadata = normalizedColumnHeaderToMetadata.get(normalizedColumnHeader);
        if (redcapAttributeMetadata != null) {
            return redcapAttributeMetadata;
        } else {
            String errorString = "Error : No RedcapAttributeMetadata found associated with normalized column header: " + normalizedColumnHeader;
            log.warn(errorString);
            throw new RuntimeException(errorString);
        }
    }

    public List<RedcapAttributeMetadata> getMetadata() {
        ensureThatCacheIsInitialized();
        return Arrays.asList(metadataArray);
    }

    private void ensureThatCacheIsInitialized() {
        if (normalizedColumnHeaderToMetadata != null && metadataArray != null) {
            return; // already initialized
        }
        initializeMetadataList();
        initializeMetadataMaps();
    }

    private void initializeMetadataList() {
        if(overrideStudyId != null) {
            metadataArray = cddSessionManager.getRedcapMetadataWithOverrides(overrideStudyId);
        } else {
            metadataArray = cddSessionManager.getRedcapMetadata();
        }
    }

    private void initializeMetadataMaps() {
        if (metadataArray == null) {
            initializeMetadataList();
        }
        normalizedColumnHeaderToMetadata = new HashMap<String, RedcapAttributeMetadata>(metadataArray.length);
        for (RedcapAttributeMetadata metadataElement : metadataArray) {
            addToNormalizedColumnHeaderMap(metadataElement);
        }
    }

    private void addToNormalizedColumnHeaderMap(RedcapAttributeMetadata metadataElement) {
        String normalizedColumnHeader = metadataElement.getNormalizedColumnHeader();
        if (normalizedColumnHeader == null) {
            String errorString = "Error : no defined clinical attribute in Clinical Data Dictionary";
            log.warn(errorString);
            throw new RuntimeException(errorString);
        }
        if (normalizedColumnHeaderToMetadata.put(normalizedColumnHeader, metadataElement) != null) {
            log.warn("overwrote normalizedColumnHeader '" + normalizedColumnHeader +
                    "' with new value in normalizedColumnHeaderToMetadata map (normalizedColumnHeader duplicated in Clinical Data Dictionary ---- SOMETHING WEIRD HAPPENED ---- should not be possible)");
        }
    }
}

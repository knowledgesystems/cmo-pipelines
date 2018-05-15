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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package org.mskcc.cmo.ks.redcap.source.internal;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.models.RedcapAttributeMetadata;
import org.mskcc.cmo.ks.redcap.models.RedcapProjectAttribute;
import org.mskcc.cmo.ks.redcap.source.ClinicalDataSource;
import org.mskcc.cmo.ks.redcap.source.MetadataManager;
import org.mskcc.cmo.ks.redcap.source.internal.MetadataCache;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Zachary Heins
 *
 * Use Redcap to import/export clinical metadata and data
 *
 */
@Repository
public class ClinicalDataSourceRedcapImpl implements ClinicalDataSource {

    //TODO: The design of this class should migrate so that it becomes unaware of redcap token handling. Migrate such lower level logic downwards.

    @Autowired
    private MetadataCache metadataCache;

    @Autowired
    private MetadataManager metadataManager;

    @Autowired
    private RedcapRepository redcapRepository;

    private Map<String, String> clinicalDataTokens = null;
    private Map<String, String> clinicalTimelineTokens = null;

    private List<String> sampleHeader;
    private List<String> patientHeader;
    private List<String> combinedHeader;
    private Map<String, List<String>> fullPatientHeader = new HashMap<>();
    private Map<String, List<String>> fullSampleHeader = new HashMap<>();
    private String nextClinicalId;
    private String nextTimelineId;

    private final Logger log = Logger.getLogger(ClinicalDataSourceRedcapImpl.class);

    @Override
    public boolean projectExists(String projectTitle) {
        return redcapRepository.getTokenByProjectTitle(projectTitle) != null;
    }

    @Override
    public boolean redcapDataTypeIsTimeline(String projectTitle) {
        return redcapRepository.redcapDataTypeIsTimeline(projectTitle);
    }

    @Override
    public List<Map<String, String>> exportRawDataForProjectTitle(String projectTitle) {
        String projectToken = redcapRepository.getTokenByProjectTitle(projectTitle);
        List<Map<String, String>> data = redcapRepository.getRedcapDataForProject(projectToken);
        if (redcapDataTypeIsTimeline(projectTitle)) {
            if (clinicalTimelineTokens != null) {
                clinicalTimelineTokens.remove(projectTitle);
            }
        } else {
            if (clinicalDataTokens != null) {
                clinicalDataTokens.remove(projectTitle);
            }
        }
        return data;
    }

    @Override
    public boolean projectsExistForStableId(String stableId) {
        return !redcapRepository.getClinicalTokenMapByStableId(stableId).isEmpty() ||
                !redcapRepository.getTimelineTokenMapByStableId(stableId).isEmpty();
    }

    @Override
    public List<String> getProjectHeader(String projectTitle) {
        String projectToken = redcapRepository.getTokenByProjectTitle(projectTitle);
        return getNormalizedColumnHeaders(projectToken);
    }

    @Override
    public List<String> getSampleHeader(String stableId) {
        checkTokensByStableId(stableId);
        getClinicalHeaderData();
        return sampleHeader;
    }

    @Override
    public List<String> getPatientHeader(String stableId) {
        checkTokensByStableId(stableId);
        getClinicalHeaderData();
        return patientHeader;
    }

    @Override
    public List<String> getTimelineHeader(String stableId) {
        checkTokensByStableId(stableId);
        getTimelineHeaderData();
        return combinedHeader;
    }

    @Override
    public List<Map<String, String>> getClinicalData(String stableId) {
        checkTokensByStableId(stableId);
        String projectToken = clinicalDataTokens.remove(nextClinicalId);
        return redcapRepository.getRedcapDataForProject(projectToken);
    }

    @Override
    public List<Map<String, String>> getTimelineData(String stableId) {
        checkTokensByStableId(stableId);
        String projectToken = clinicalTimelineTokens.remove(nextTimelineId);
        return redcapRepository.getRedcapDataForProject(projectToken);
    }

    @Override
    public String getNextClinicalProjectTitle(String stableId) {
        checkTokensByStableId(stableId);
        Iterator<String> keySetIterator = clinicalDataTokens.keySet().iterator();
        nextClinicalId = null; // blank out in case of exception on following line
        nextClinicalId = keySetIterator.next();
        return nextClinicalId;
    }

    @Override
    public String getNextTimelineProjectTitle(String stableId) {
        checkTokensByStableId(stableId);
        Iterator<String> keySetIterator = clinicalTimelineTokens.keySet().iterator();
        nextTimelineId = null;
        nextTimelineId = keySetIterator.next();
        return nextTimelineId;
    }

    @Override
    public boolean hasMoreTimelineData(String stableId) {
        checkTokensByStableId(stableId);
        return !clinicalTimelineTokens.isEmpty();
    }

    @Override
    public boolean hasMoreClinicalData(String stableId) {
        checkTokensByStableId(stableId);
        return !clinicalDataTokens.isEmpty();
    }

    @Override
    public ListIterator<String> getClinicalProjectTitleIterator(String stableId) {
        return redcapRepository.getClinicalProjectTitleIterator(stableId);
    }

    @Override
    public ListIterator<String> getTimelineProjectTitleIterator(String stableId) {
        return redcapRepository.getTimelineProjectTitleIterator(stableId);
    }

    @Override
    public void importClinicalDataFile(String projectTitle, String filename, boolean keepExistingProjectData) throws Exception {
        String projectToken = redcapRepository.getTokenByProjectTitle(projectTitle);
        if (projectToken == null) {
            log.error("Project not found in redcap clinicalDataTokens or clincalTimelineTokens: " + projectTitle);
            return;
        }
        try {
            File file = new File(filename);
            if (!file.exists()) {
                log.error("Error : could not find file " + filename);
                throw new Exception("Error: could not find file: " + filename);
            }
            List<String> dataFileContentsTSV = readClinicalFile(file);
            if (dataFileContentsTSV.size() == 0) {
                log.error("Error: file " + filename + " was empty ... aborting attempt to import data");
                throw new Exception("Error: file " + filename + " was empty ... aborting attempt to import data");
            }
            if (!metadataManager.allHeadersAreValidClinicalAttributes(Arrays.asList(dataFileContentsTSV.get(0).split("\t",-1)))) {
                log.error("Error: file " + filename + " has headers that are not defined in the Clinical Data Dictionary ... aborting attempt to import data");
                throw new Exception("Error: file " + filename + " has headers that are not defined in Clinical Data Dictionary ... aborting attempt to import data");
            }
            redcapRepository.importClinicalData(projectToken, dataFileContentsTSV, keepExistingProjectData);
        } catch (IOException e) {
            log.error("IOException thrown while attempting to read file " + filename + " : " + e.getMessage());
            throw new IOException("IOException thrown while attempting to read file " + filename + " : " + e.getMessage());
        } catch (Exception e) {
            log.error("Error importing file: " + filename + ", " + e.getMessage());
            throw new Exception("Error importing file: " + filename + ", " + e.getMessage());
        }
    }

    private List<String> getNormalizedColumnHeaders(String projectToken) {
        List<RedcapProjectAttribute> attributes = redcapRepository.getAttributesByToken(projectToken);
        Map<RedcapProjectAttribute, RedcapAttributeMetadata> attributeMap = new LinkedHashMap<>();
        for (RedcapProjectAttribute attribute : attributes) {
            attributeMap.put(attribute, metadataCache.getMetadataByNormalizedColumnHeader(redcapRepository.convertRedcapIdToColumnHeader(attribute.getFieldName())));
        }
        return makeHeader(attributeMap);
    }

    // Sets the sampleHeader and patientHeader data members for the current clinical project
    private void getClinicalHeaderData() {
        List<RedcapProjectAttribute> attributes = getAttributes(false);
        Map<RedcapProjectAttribute, RedcapAttributeMetadata> sampleAttributeMap = new LinkedHashMap<>();
        Map<RedcapProjectAttribute, RedcapAttributeMetadata> patientAttributeMap = new LinkedHashMap<>();
        for (RedcapProjectAttribute attribute : attributes) {
            RedcapAttributeMetadata meta = metadataCache.getMetadataByNormalizedColumnHeader(redcapRepository.convertRedcapIdToColumnHeader(attribute.getFieldName()));
            if (attribute.getFieldName().equalsIgnoreCase("PATIENT_ID")) {
                //PATIENT_ID is both a sample and a patient attribute
                sampleAttributeMap.put(attribute, meta);
                patientAttributeMap.put(attribute, meta);
                continue;
            } else if (meta.getAttributeType().equals("SAMPLE")) {
                sampleAttributeMap.put(attribute, meta);
            } else {
                patientAttributeMap.put(attribute, meta);
            }
        }
        sampleHeader = makeHeader(sampleAttributeMap);
        patientHeader = makeHeader(patientAttributeMap);
    }

    private void getTimelineHeaderData() {
        List<RedcapProjectAttribute> attributes = getAttributes(true);
        Map<RedcapProjectAttribute, RedcapAttributeMetadata> combinedAttributeMap = new LinkedHashMap<>();
        for (RedcapProjectAttribute attribute : attributes) {
            combinedAttributeMap.put(attribute, metadataCache.getMetadataByNormalizedColumnHeader(redcapRepository.convertRedcapIdToColumnHeader(attribute.getFieldName())));
        }
        combinedHeader = makeHeader(combinedAttributeMap);
    }

    private List<RedcapProjectAttribute> getAttributes(boolean timelineData) {
        String projectToken;
        if (timelineData) {
            projectToken = clinicalTimelineTokens.get(nextTimelineId);
        } else {
            projectToken = clinicalDataTokens.get(nextClinicalId);
        }
        return redcapRepository.getAttributesByToken(projectToken);
    }

    private List<String> makeHeader(Map<RedcapProjectAttribute, RedcapAttributeMetadata> attributeMap) {
        List<String> header = new ArrayList<>();
        for (Map.Entry<RedcapProjectAttribute, RedcapAttributeMetadata> entry : attributeMap.entrySet()) {
            header.add(entry.getValue().getNormalizedColumnHeader());
        }
        return header;
    }

    private boolean tokensHaveBeenSelected() {
        return clinicalTimelineTokens != null && clinicalDataTokens != null;
    }

    private void checkTokensByStableId(String stableId) {
        if (!tokensHaveBeenSelected()) {
            clinicalTimelineTokens = redcapRepository.getTimelineTokenMapByStableId(stableId);
            clinicalDataTokens = redcapRepository.getClinicalTokenMapByStableId(stableId);
        }
    }

    private List<String> readClinicalFile(File file) throws IOException {
        LinkedList<String> lineList = new LinkedList<>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        while (bufferedReader.ready()) {
            String line = bufferedReader.readLine();
            if (line != null) {
                lineList.add(line);
            }
        }
        return lineList;
    }

    public static void main(String[] args) {}
}

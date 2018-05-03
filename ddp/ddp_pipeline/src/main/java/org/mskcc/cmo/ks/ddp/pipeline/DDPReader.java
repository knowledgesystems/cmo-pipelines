/*
 * Copyright (c) 2018 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.ddp.pipeline;

import org.mskcc.cmo.ks.ddp.source.DDPDataSource;
import org.mskcc.cmo.ks.ddp.source.composite.DDPCompositeRecord;
import org.mskcc.cmo.ks.ddp.source.model.CohortPatient;
import org.mskcc.cmo.ks.ddp.source.model.PatientIdentifiers;
import org.mskcc.cmo.ks.ddp.pipeline.util.DDPPatientListUtil;

import com.google.common.base.Strings;
import java.io.*;
import java.util.*;
import javax.annotation.Resource;
import org.apache.log4j.Logger;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.*;

/**
 *
 * @author ochoaa
 */
public class DDPReader implements ItemStreamReader<DDPCompositeRecord> {

    @Value("#{jobParameters[cohortName]}")
    private String cohortName;

    @Value("#{jobParameters[subsetFilename]}")
    private String subsetFilename;

    @Value("#{jobParameters[excludedPatientsFilename]}")
    private String excludedPatientsFilename;

    @Value("#{jobParameters[testMode]}")
    private Boolean testMode;

    @Resource(name = "cohortMapping")
    private Map<String, Integer> cohortMapping;

    @Autowired
    private DDPDataSource ddpDataSource;

    @Autowired
    private DDPPatientListUtil ddpPatientListUtil;

    private List<DDPCompositeRecord> ddpCompositeRecordList;
    private Set<String> excludedPatientIds = new HashSet<>();
    private final Integer TEST_MODE_PATIENT_THRESHOLD = 500;

    private Logger LOG = Logger.getLogger(DDPReader.class);

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        if (testMode) {
            LOG.info("Running DDP pipeline in test mode - data will only be processed for 50 patients");
        }
        // load patient ids to exclude from final result set if excludePatientsFilename provided
        if (!Strings.isNullOrEmpty(excludedPatientsFilename)) {
            try {
                this.excludedPatientIds = loadPatientIdsFromFile(excludedPatientsFilename);
            } catch (FileNotFoundException e) {
                throw new ItemStreamException("Error loading excluded patient ids from: " + excludedPatientsFilename, e);
            }
        }
        // get composite records by cohort id if given, otherwise use patient ids from subsetFilename
        if (!Strings.isNullOrEmpty(cohortName)) {
            try {
                this.ddpCompositeRecordList = getCompositeRecordsByCohortId();
            }
            catch (Exception e) {
                throw new ItemStreamException("Error fetching DDP records by cohort name: " + cohortName, e);
            }
        }
        else {
            try {
                this.ddpCompositeRecordList = getCompositeRecordsByPatientIds();
            } catch (Exception e) {
                throw new ItemStreamException("Error fetching DDP records by subset ids: " + subsetFilename, e);
            }
        }
        LOG.info("Fetched " + ddpCompositeRecordList.size()+  " DDP records");
    }

    /**
     * Returns a unique set of composite records for given cohort id..
     *
     * @return
     */
    private List<DDPCompositeRecord> getCompositeRecordsByCohortId() throws Exception {
        Integer cohortId = cohortMapping.get(cohortName);
        if (cohortId == null) {
            throw new ItemStreamException("Cohort not known by name: " + cohortName);
        }
        // get composite records from DDP by cohort id
        List<DDPCompositeRecord> records = getDDPCompositeRecordsByCohortId(cohortId);
        return records;
    }

    /**
     * Helper function to return a unique set of composite records for cohort ids.
     *
     * Removes records matching ids in 'excludedPatientIds' if necessary.
     *
     * @param cohortId
     * @return
     */
    private List<DDPCompositeRecord> getDDPCompositeRecordsByCohortId(Integer cohortId) throws Exception {
        List<CohortPatient> records = ddpDataSource.getPatientRecordsByCohortId(cohortId);
        LOG.info("Fetched " + records.size()+  " active patients for cohort: " + cohortName);

        List<DDPCompositeRecord> compositeRecords = new ArrayList<>();
        int count = 0;
        for (CohortPatient record : records) {
            PatientIdentifiers pids;
            try {
                pids = ddpDataSource.getPatientIdentifiers(record.getPID().toString());
            } catch (Exception e) {
                LOG.error("Failed to resolve dmp id's for record '" + record.getPID() + "' -- skipping");
                ddpPatientListUtil.addPatientsMissingDMPId(record.getPID());
                continue;
            }
            if (pids != null && !Strings.isNullOrEmpty(pids.getDmpPatientId())) {
                compositeRecords.add(new DDPCompositeRecord(pids.getDmpPatientId(), pids.getDmpSampleIds(), record));
            } else {
                LOG.error("Failed to resolve dmp id's for record '" + record.getPID() + "' -- skipping");
                ddpPatientListUtil.addPatientsMissingDMPId(record.getPID());
            }
            count++;
            if (testMode && count >= TEST_MODE_PATIENT_THRESHOLD) {
                break;
            }
        }
        // filter composite records to return if excluded patient id list is not empty
        if (!excludedPatientIds.isEmpty()) {
            return filterCompositeRecords(compositeRecords);
        }
        return compositeRecords;
    }

    /**
     * Returns a unique set of composite records for patient ids stored in subsetFilename.
     *
     * @return
     */
    private List<DDPCompositeRecord> getCompositeRecordsByPatientIds() throws Exception {
        Set<String> patientIds = new HashSet<>();
        try {
            patientIds = loadPatientIdsFromFile(subsetFilename);
        } catch (FileNotFoundException e) {
            throw new ItemStreamException("Error loading patient ids from: " + subsetFilename, e);
        }
        List<DDPCompositeRecord> records = getDDPCompositeRecordsByPatientIds(patientIds);
        return records;
    }

    /**
     * Helper function to return a unique set of composite records for given patient ids.
     *
     * Removes records matching patient ids in 'excludedPatientIds' if necessary.
     *
     * @param patientIds
     * @return
     */
    private List<DDPCompositeRecord> getDDPCompositeRecordsByPatientIds(Set<String> patientIds) {
        List<DDPCompositeRecord> compositeRecords = new ArrayList<>();
        int count = 0;
        for (String patientId : patientIds) {
            compositeRecords.add(new DDPCompositeRecord(patientId));
            count++;
            if (testMode && count >= TEST_MODE_PATIENT_THRESHOLD) {
                break;
            }
        }
        // filter composite records to return if excluded patient id list is not empty
        if (!excludedPatientIds.isEmpty()) {
            return filterCompositeRecords(compositeRecords);
        }
        return compositeRecords;
    }

    /**
     * Loads patient ids from given file.
     *
     * File contains line-delimited list of patient ids.
     *
     * @param filename
     * @return
     * @throws FileNotFoundException
     */
    private Set<String> loadPatientIdsFromFile(String filename) throws FileNotFoundException {
        Set<String> patientIds = new HashSet<>();
        Scanner reader = new Scanner(new File(filename));
        while (reader.hasNext()) {
            patientIds.add(reader.nextLine().trim());
        }
        reader.close();
        if (patientIds.isEmpty()) {
            throw new ItemStreamException("Error loading patient ids from: " + subsetFilename);
        }
        LOG.info("Loaded " + patientIds.size() + " patient ids from: " + subsetFilename);
        return patientIds;
    }

    /**
     * Removes records from the given composite record list if found in the excluded patient ids list.
     *
     * @return
     */
    private List<DDPCompositeRecord> filterCompositeRecords(List<DDPCompositeRecord> compositeRecords) {
        LOG.info("Removing composite records matching ids in 'excludedPatientIds'...");
        List<DDPCompositeRecord> filteredCompositeRecords = new ArrayList<>();
        for (DDPCompositeRecord record : compositeRecords) {
            if (excludedPatientIds.contains(record.getDmpPatientId())) {
                continue;
            }
            filteredCompositeRecords.add(record);
        }
        LOG.info("Removed " + (compositeRecords.size() - filteredCompositeRecords.size()) + " from final composite record set");
        return filteredCompositeRecords;
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public DDPCompositeRecord read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!ddpCompositeRecordList.isEmpty()) {
            return ddpCompositeRecordList.remove(0);
        }
        return null;
    }
}

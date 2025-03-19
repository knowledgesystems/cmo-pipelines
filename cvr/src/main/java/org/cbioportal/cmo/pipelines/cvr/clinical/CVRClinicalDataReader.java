/*
 * Copyright (c) 2016 - 2022, 2025 Memorial Sloan-Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr.clinical;

import org.cbioportal.cmo.pipelines.cvr.model.staging.MskimpactSeqDate;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRClinicalRecord;
import com.google.common.base.Strings;
import org.cbioportal.cmo.pipelines.cvr.*;
import org.cbioportal.cmo.pipelines.cvr.model.*;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.io.FileSystemResource;

/**
 *
 * @author heinsz
 */
public class CVRClinicalDataReader implements ItemStreamReader<CVRClinicalRecord> {
    @Value("${comppath.wsv.baseurl}")
    private String wholeSlideViewerBaseURL;

    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    @Value("#{jobParameters[privateDirectory]}")
    private String privateDirectory;

    @Value("#{jobParameters[studyId]}")
    private String studyId;

    @Value("#{jobParameters[clinicalFilename]}")
    private String clinicalFilename;

    @Value("#{jobParameters[masterListDoesNotExcludeSamples]}")
    private boolean masterListDoesNotExcludeSamples;

    @Autowired
    public CVRUtilities cvrUtilities;

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    private final Deque<CVRClinicalRecord> clinicalRecords = new LinkedList<>();
    private Map<String, List<CVRClinicalRecord>> patientToRecordMap = new HashMap();

    Logger log = Logger.getLogger(CVRClinicalDataReader.class);

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        // validate url
        if (Strings.isNullOrEmpty(wholeSlideViewerBaseURL) || !wholeSlideViewerBaseURL.contains("IMAGE_ID")) {
            String message = "wholeSlideViewerBaseURL is empty or does not contain 'IMAGE_ID'  - please check value of 'comppath.wsv.baseurl' in application.properties: " + wholeSlideViewerBaseURL;
            log.error(message);
            throw new RuntimeException(message);
        }
        processClinicalFile(ec);
        if (CVRUtilities.SUPPORTED_SEQ_DATE_STUDY_IDS.contains(studyId)) {
            processSeqDateFile(ec);
        }
        // It's important that we process the JSON file *after* backfilling the sequencing date.
        // Otherwise, the old SEQ_DATE value from the clinical file will overwrite the value from the CVR queue.
        processJsonFile();
        // updates portalSamplesNotInDmpList and dmpSamplesNotInPortal sample lists
        // portalSamples list is only updated if threshold check for max num samples to remove passes
        cvrSampleListUtil.updateSampleLists(masterListDoesNotExcludeSamples);
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
    }

    @Override
    public CVRClinicalRecord read() throws Exception {
        while (!clinicalRecords.isEmpty()) {
            CVRClinicalRecord record = clinicalRecords.pollFirst();
            // portal samples may or may not be filtered by 'portalSamplesNotInDmp' is threshold check above
            // so we want to skip samples that aren't in this list
            if (!record.getSAMPLE_ID().startsWith(record.getPATIENT_ID())) {
                cvrSampleListUtil.addSamplesInvalidPatientIdList(record.getSAMPLE_ID());
                continue;
            }
            if (!cvrSampleListUtil.getPortalSamples().contains(record.getSAMPLE_ID())) {
                continue;
            }
            return record;
        }
        return null;
    }

    private void processClinicalFile(ExecutionContext ec) {
        File mskimpactClinicalFile = new File(stagingDirectory, clinicalFilename);
        if (!mskimpactClinicalFile.exists()) {
            log.error("File does not exist - skipping data loading from clinical file: " + mskimpactClinicalFile.getName());
            return;
        }
        log.info("Loading clinical data from: " + mskimpactClinicalFile.getName());
        FlatFileItemReader<CVRClinicalRecord> reader = null;
        try {
            reader = ClinicalFileReaderUtil.createReader(mskimpactClinicalFile);
            reader.open(ec);
            CVRClinicalRecord to_add;
            while ((to_add = reader.read()) != null) {
                if (to_add.getSAMPLE_ID() != null && !cvrSampleListUtil.getNewDmpSamples().contains(to_add.getSAMPLE_ID())) {
                    clinicalRecords.add(to_add);
                    cvrSampleListUtil.addPortalSample(to_add.getSAMPLE_ID());
                    List<CVRClinicalRecord> records = patientToRecordMap.getOrDefault(to_add.getPATIENT_ID(), new ArrayList<CVRClinicalRecord>());
                    records.add(to_add);
                    patientToRecordMap.put(to_add.getPATIENT_ID(), records);
                }
            }
        }
        catch (Exception e) {
            log.error("Error reading data from clinical file: " + mskimpactClinicalFile.getName());
            throw new ItemStreamException(e);
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private void processJsonFile() {
        CVRData cvrData = new CVRData();
        // load cvr data from cvr_data.json file
        File cvrFile = new File(privateDirectory, CVRUtilities.CVR_FILE);
        try {
            cvrData = cvrUtilities.readJson(cvrFile);
        } catch (IOException e) {
            log.error("Error reading file: " + cvrFile.getName());
            throw new ItemStreamException(e);
        }
        for (CVRMergedResult result : cvrData.getResults()) {
            CVRClinicalRecord record = new CVRClinicalRecord(result.getMetaData(), wholeSlideViewerBaseURL, studyId);
            List<CVRClinicalRecord> records = patientToRecordMap.getOrDefault(record.getPATIENT_ID(), new ArrayList<CVRClinicalRecord>());
            records.add(record);
            patientToRecordMap.put(record.getPATIENT_ID(), records);
            clinicalRecords.add(record);
        }
    }

    private void processSeqDateFile(ExecutionContext ec) {
        File mskimpactSeqDateFile = new File(stagingDirectory, CVRUtilities.SEQ_DATE_CLINICAL_FILE);
        if (!mskimpactSeqDateFile.exists()) {
            log.error("File does not exist - skipping data loading from seq date file: " + mskimpactSeqDateFile.getName());
            return;
        }
        log.info("Loading seq date data from: " + mskimpactSeqDateFile.getName());
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB);
        DefaultLineMapper<MskimpactSeqDate> mapper = new DefaultLineMapper<>();
        mapper.setLineTokenizer(tokenizer);
        mapper.setFieldSetMapper(new MskimpactSeqDateFieldSetMapper());
        FlatFileItemReader<MskimpactSeqDate> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(mskimpactSeqDateFile));
        reader.setLineMapper(mapper);
        reader.setLinesToSkip(1);
        reader.open(ec);

        MskimpactSeqDate mskimpactSeqDate;
        try{
            while ((mskimpactSeqDate = reader.read()) != null) {
                // using the same patient - record map for now. If patients start to have significant number
                // of samples, we might want a separate sampleToRecordMap for performance
                if (patientToRecordMap.containsKey(mskimpactSeqDate.getPATIENT_ID())) {
                    for(CVRClinicalRecord record : patientToRecordMap.get(mskimpactSeqDate.getPATIENT_ID())) {
                        if (record.getSAMPLE_ID().equals(mskimpactSeqDate.getSAMPLE_ID())) {
                            record.setSEQ_DATE(mskimpactSeqDate.getSEQ_DATE());
                            break;
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            log.error("Error reading data from seq date file: " + mskimpactSeqDateFile.getName());
            throw new ItemStreamException(e);
        }
        finally {
            reader.close();
        }
    }
}

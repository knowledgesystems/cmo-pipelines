/*
 * Copyright (c) 2018 - 2023 Memorial Sloan Kettering Cancer Center.
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

import java.io.IOException;
import org.mskcc.cmo.ks.ddp.pipeline.model.AgeAtSeqDateRecord;
import org.mskcc.cmo.ks.ddp.pipeline.model.ClinicalRecord;
import org.mskcc.cmo.ks.ddp.pipeline.model.TimelineChemoRecord;
import org.mskcc.cmo.ks.ddp.pipeline.model.TimelineRadiationRecord;
import org.mskcc.cmo.ks.ddp.pipeline.model.TimelineSurgeryRecord;
import org.mskcc.cmo.ks.ddp.pipeline.util.DDPUtils;

import org.apache.log4j.Logger;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;

import org.apache.commons.lang.StringUtils;
import java.nio.file.*;
import java.util.*;

/**
 *
 * @author Avery Wang
 */
public class DDPSortTasklet implements Tasklet {

    @Value("#{jobParameters[outputDirectory]}")
    private String outputDirectory;
    @Value("${ddp.clinical_filename}")
    private String clinicalFilename;
    @Value("${ddp.age_at_seq_date_filename}")
    private String ageAtSeqDateFilename;
    @Value("${ddp.timeline_chemotherapy_filename}")
    private String timelineChemotherapyFilename;
    @Value("${ddp.timeline_radiation_filename}")
    private String timelineRadiationFilename;
    @Value("${ddp.timeline_surgery_filename}")
    private String timelineSurgeryFilename;
    @Value("#{jobParameters[includeDiagnosis]}")
    private Boolean includeDiagnosis;
    @Value("#{jobParameters[includeAgeAtSeqDate]}")
    private Boolean includeAgeAtSeqDate;
    @Value("#{jobParameters[includeRadiation]}")
    private Boolean includeRadiation;
    @Value("#{jobParameters[includeChemotherapy]}")
    private Boolean includeChemotherapy;
    @Value("#{jobParameters[includeSurgery]}")
    private Boolean includeSurgery;
    @Value("#{jobParameters[currentDemographicsRecCount]}")
    private Integer currentDemographicsRecCount;

    private final double DEMOGRAPHIC_RECORD_DROP_THRESHOLD = 0.9;

    private final Logger LOG = Logger.getLogger(DDPSortTasklet.class);

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        // sort and overwrite demographics file
        Path clinicalFilePath = Paths.get(outputDirectory, clinicalFilename);
        validateDemographicsRecordCount(clinicalFilePath.toString());
        sortAndOverwriteFile(clinicalFilePath, ClinicalRecord.getFieldNames(includeDiagnosis, includeRadiation, includeChemotherapy, includeSurgery));

        if (includeAgeAtSeqDate) {
            Path ageAtSeqDateFilePath = Paths.get(outputDirectory, ageAtSeqDateFilename);
            sortAndOverwriteFile(ageAtSeqDateFilePath, AgeAtSeqDateRecord.getFieldNames());
        }

        // sort and overwrite timeline files if applicable
        if (includeChemotherapy) {
            String timelineChemotherapyFilePath = Paths.get(outputDirectory, timelineChemotherapyFilename).toString();
            String timelineChemotherapyHeader = StringUtils.join(TimelineChemoRecord.getFieldNames(), "\t");
            LOG.info("Sorting and overwriting " + timelineChemotherapyFilePath);
            DDPUtils.sortAndWrite(timelineChemotherapyFilePath, timelineChemotherapyHeader);
        }

        if (includeRadiation) {
            Path timelineRadiationFilePath = Paths.get(outputDirectory, timelineRadiationFilename);
            sortAndOverwriteFile(timelineRadiationFilePath, TimelineRadiationRecord.getFieldNames());
        }

        if (includeSurgery) {
            Path timelineSurgeryFilePath = Paths.get(outputDirectory, timelineSurgeryFilename);
            sortAndOverwriteFile(timelineSurgeryFilePath, TimelineSurgeryRecord.getFieldNames());
        }

        return RepeatStatus.FINISHED;
    }

    private void validateDemographicsRecordCount(String demographicsFilePath) throws Exception {
        List<String> demographicsRecords = DDPUtils.readRecordsFromFile(demographicsFilePath);
        if (demographicsRecords.size() < (DEMOGRAPHIC_RECORD_DROP_THRESHOLD * currentDemographicsRecCount)) {
            throw new RuntimeException("Number of records in latest demographics fetch (" + Integer.toString(demographicsRecords.size()) +
                    ") dropped greater than 90% of current record count (" + currentDemographicsRecCount +
                    ") in backup demographics file - exiting...");
        }
    }

    private void sortAndOverwriteFile(Path filePath, List<String> fieldNames) throws IOException {
        LOG.info("Sorting and overwriting file:" + filePath.toString());
        DDPUtils.sortAndWrite(filePath.toString(), StringUtils.join(fieldNames, "\t"));
    }
}

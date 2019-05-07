/*
 * Copyright (c) 2019 Memorial Sloan-Kettering Cancer Center.
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

import org.mskcc.cmo.ks.ddp.pipeline.util.DDPUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @author Manda Wilson
 */
public class DDPSeqDateTasklet implements Tasklet {

    @Value("#{jobParameters[seqDateFilename]}")
    private String seqDateFilename;
    @Value("#{jobParameters[includeSurvival]}")
    private Boolean includeSurvival;

    private final Logger LOG = Logger.getLogger(DDPSeqDateTasklet.class);

    private static final String PATIENT_ID_COLUMN_LABEL = "PATIENT_ID";
    private static final String SAMPLE_ID_COLUMN_LABEL = "SAMPLE_ID";
    private static final String SEQ_DATE_COLUMN_LABEL = "SEQ_DATE";
    private static final List<String> EXPECTED_SEQ_DATE_FILE_HEADER = Arrays.asList(SAMPLE_ID_COLUMN_LABEL, PATIENT_ID_COLUMN_LABEL, SEQ_DATE_COLUMN_LABEL);
    private static final int MAX_ERRORS_TO_SHOW = 30;

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        if (!StringUtils.isEmpty(seqDateFilename)) {
            LOG.debug("Seq date file is: " + seqDateFilename);
            BufferedReader reader = new BufferedReader(new FileReader(seqDateFilename));
            // pass reader as parameter so we can mock it in tests
            DDPUtils.setPatientFirstSeqDateMap(getFirstSeqDatePerPatientFromFile(seqDateFilename, reader));
            DDPUtils.setUseSeqDateOsMonthsMethod(Boolean.TRUE);
        } else {
            LOG.debug("No seq date file given.");
        }
        return RepeatStatus.FINISHED;
    }

    public Map<String, Date> getFirstSeqDatePerPatientFromFile(String filename, BufferedReader reader) throws IOException {
        // this method is public for unit testing
        Map <String, Date> patientFirstSeqDateMap = new HashMap<String, Date>();
        // skip first line (header)
        List<String> header = Arrays.asList(reader.readLine().split("\t"));
        int dmpPatientIdColumnIndex = header.indexOf(PATIENT_ID_COLUMN_LABEL);
        int seqDateColumnIndex = header.indexOf(SEQ_DATE_COLUMN_LABEL);
        Collections.sort(EXPECTED_SEQ_DATE_FILE_HEADER);
        Collections.sort(header);
        if (dmpPatientIdColumnIndex == -1 || seqDateColumnIndex == -1 || !EXPECTED_SEQ_DATE_FILE_HEADER.equals(header)) {
            LOG.warn("Invalid header in '" + filename + "', expected '" + String.join(",", EXPECTED_SEQ_DATE_FILE_HEADER) + "', found '" + String.join(",", header)+ "'");
            return patientFirstSeqDateMap; // empty map
        } 
        // e.g. Wed, 16 Mar 2016 18:09:02 GMT
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        String line;
        List<String> warnings = new ArrayList<String>();
        while ((line = reader.readLine()) != null) {
            String[] record = line.split("\t");
            String dmpPatientId = record[dmpPatientIdColumnIndex];
            String seqDateString = record[seqDateColumnIndex];
            if (!StringUtils.isEmpty(seqDateString)) {
                try {
                    Date parsedRecordSeqDate = simpleDateFormat.parse(seqDateString);
                    if (!patientFirstSeqDateMap.containsKey(dmpPatientId) || parsedRecordSeqDate.before(patientFirstSeqDateMap.get(dmpPatientId))) {
                        patientFirstSeqDateMap.put(dmpPatientId, parsedRecordSeqDate);
                    }
                } catch (ParseException pe) {
                    // do nothing, handle empty records later
                    warnings.add("Invalid date format: '" + seqDateString + "' for sample '" + record[0] +"'");
                }
            } else {
                warnings.add("Empty date: '" + seqDateString + "' for sample '" + record[0] +"'");
            }
        }
        reader.close();
        if (warnings.size() > 0) {
            LOG.warn(warnings.size() + " sample(s) found with invalid records.  The first " + MAX_ERRORS_TO_SHOW + " are listed below:");
            for (int w = 0; w < warnings.size() && w < MAX_ERRORS_TO_SHOW; w++) {
                LOG.warn(warnings.get(w)); 
            }
        }
        return patientFirstSeqDateMap;
    }
}

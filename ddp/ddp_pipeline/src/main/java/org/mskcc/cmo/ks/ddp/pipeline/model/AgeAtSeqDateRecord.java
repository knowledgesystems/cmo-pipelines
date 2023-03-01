/*
 * Copyright (c) 2023 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.ddp.pipeline.model;

import org.mskcc.cmo.ks.ddp.pipeline.util.DDPUtils;

import java.text.ParseException;
import java.util.*;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author Manda Wilson and Calla Chennault
 */
public class AgeAtSeqDateRecord {
    private String PATIENT_ID;
    private String SAMPLE_ID;
    private String AGE_AT_SEQ_REPORTED_YEARS;

    public AgeAtSeqDateRecord(){}

    public AgeAtSeqDateRecord(String patientId, String sampleId, String patientBirthDate) throws ParseException {
        this.PATIENT_ID = patientId;
        this.SAMPLE_ID = sampleId;
        this.AGE_AT_SEQ_REPORTED_YEARS = DDPUtils.resolveAgeAtSeqDate(sampleId, patientBirthDate);
    }

    /**
     * @return the PATIENT_ID
     */
    public String getPATIENT_ID() {
        return PATIENT_ID;
    }

    /**
     * @param PATIENT_ID the PATIENT_ID to set
     */
    public void setPATIENT_ID(String PATIENT_ID) {
        this.PATIENT_ID = PATIENT_ID;
    }

    /**
     * @return the SAMPLE_ID
     */
    public String getSAMPLE_ID() {
        return SAMPLE_ID;
    }

    /**
     * @param SAMPLE_ID the SAMPLE_ID to set
     */
    public void setSAMPLE_ID(String SAMPLE_ID) {
        this.SAMPLE_ID = SAMPLE_ID;
    }

    /**
     * @return the AGE_AT_SEQ_REPORTED_YEARS
     */
    public String getAGE_AT_SEQ_REPORTED_YEARS() {
        return AGE_AT_SEQ_REPORTED_YEARS;
    }

    /**
     * @param AGE_AT_SEQ_REPORTED_YEARS the AGE_AT_SEQ_REPORTED_YEARS to set
     */
    public void setAGE_AT_SEQ_REPORTED_YEARS(String AGE_AT_SEQ_REPORTED_YEARS) {
        this.AGE_AT_SEQ_REPORTED_YEARS = AGE_AT_SEQ_REPORTED_YEARS;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     * Returns field names as list of strings.
     *
     * @return
     */
    public static List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("SAMPLE_ID");
        fieldNames.add("AGE_AT_SEQ_REPORTED_YEARS");
        return fieldNames;
    }
}

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

package org.mskcc.cmo.ks.ddp.pipeline.model;

import org.mskcc.cmo.ks.ddp.pipeline.util.DDPUtils;
import org.mskcc.cmo.ks.ddp.source.model.Surgery;

import java.text.ParseException;
import java.util.*;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author ochoaa
 */
public class TimelineSurgeryRecord {
    private String PATIENT_ID;
    private String START_DATE;
    private String EVENT_TYPE;
    private String SURGERY_DETAILS;

    public TimelineSurgeryRecord(){}

    public TimelineSurgeryRecord(String patientId, String tumorDiagnosisDate, Surgery surgery) throws ParseException {
        this.PATIENT_ID = patientId;
        this.START_DATE = DDPUtils.resolveTimelineEventDateInDays(tumorDiagnosisDate, surgery.getProcedureDate());
        this.EVENT_TYPE = "SURGERY";
        this.SURGERY_DETAILS = surgery.getProcedureDescription();
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
     * @return the START_DATE
     */
    public String getSTART_DATE() {
        return START_DATE;
    }

    /**
     * @param START_DATE the START_DATE to set
     */
    public void setSTART_DATE(String START_DATE) {
        this.START_DATE = START_DATE;
    }

    /**
     * @return the EVENT_TYPE
     */
    public String getEVENT_TYPE() {
        return EVENT_TYPE;
    }

    /**
     * @param EVENT_TYPE the EVENT_TYPE to set
     */
    public void setEVENT_TYPE(String EVENT_TYPE) {
        this.EVENT_TYPE = EVENT_TYPE;
    }

    /**
     * @return the SURGERY_DETAILS
     */
    public String getSURGERY_DETAILS() {
        return SURGERY_DETAILS;
    }

    /**
     * @param SURGERY_DETAILS the SURGERY_DETAILS to set
     */
    public void setSURGERY_DETAILS(String SURGERY_DETAILS) {
        this.SURGERY_DETAILS = SURGERY_DETAILS;
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
        fieldNames.add("START_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("SURGERY_DETAILS");
        return fieldNames;
    }
}

/*
 * Copyright (c) 2018, 2024 Memorial Sloan Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan Kettering Cancer
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
import org.mskcc.cmo.ks.ddp.source.model.Chemotherapy;

import java.text.ParseException;
import java.util.*;

/**
 *
 * @author ochoaa
 */
public class TimelineChemoRecord {
    private String PATIENT_ID;
    private String START_DATE;
    private String STOP_DATE;
    private String EVENT_TYPE;
    private String TREATMENT_TYPE;
    private String SUBTYPE;
    private String AGENT;

    public TimelineChemoRecord(){}

    public TimelineChemoRecord(String patientId, String tumorDiagnosisDate, Chemotherapy chemotherapy) throws ParseException {
        this.PATIENT_ID = patientId;
        this.START_DATE = DDPUtils.resolveTimelineEventDateInDays(tumorDiagnosisDate, chemotherapy.getSTARTDATE());
        this.STOP_DATE = DDPUtils.resolveTimelineEventDateInDays(tumorDiagnosisDate, chemotherapy.getSTOPDATE());
        this.EVENT_TYPE = "TREATMENT";
        this.TREATMENT_TYPE = "Medical Therapy";
        this.SUBTYPE = "Chemotherapy";
        this.AGENT = chemotherapy.getORDNAME();
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
     * @return the STOP_DATE
     */
    public String getSTOP_DATE() {
        return STOP_DATE;
    }

    /**
     * @param STOP_DATE the STOP_DATE to set
     */
    public void setSTOP_DATE(String STOP_DATE) {
        this.STOP_DATE = STOP_DATE;
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
     * @return the TREATMENT_TYPE
     */
    public String getTREATMENT_TYPE() {
        return TREATMENT_TYPE;
    }

    /**
     * @param TREATMENT_TYPE the TREATMENT_TYPE to set
     */
    public void setTREATMENT_TYPE(String TREATMENT_TYPE) {
        this.TREATMENT_TYPE = TREATMENT_TYPE;
    }

    /**
     * @return the SUBTYPE
     */
    public String getSUBTYPE() {
        return SUBTYPE;
    }

    /**
     * @param SUBTYPE the SUBTYPE to set
     */
    public void setSUBTYPE(String SUBTYPE) {
        this.SUBTYPE = SUBTYPE;
    }

    /**
     * @return the AGENT
     */
    public String getAGENT() {
        return AGENT;
    }

    /**
     * @param AGENT the AGENT to set
     */
    public void setAGENT(String AGENT) {
        this.AGENT = AGENT;
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
        fieldNames.add("STOP_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("TREATMENT_TYPE");
        fieldNames.add("SUBTYPE");
        fieldNames.add("AGENT");
        return fieldNames;
    }
}

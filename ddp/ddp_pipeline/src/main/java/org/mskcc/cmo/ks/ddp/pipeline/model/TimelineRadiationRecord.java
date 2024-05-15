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

import com.google.common.base.Strings;
import org.mskcc.cmo.ks.ddp.pipeline.util.DDPUtils;
import org.mskcc.cmo.ks.ddp.source.model.Radiation;

import java.text.ParseException;
import java.util.*;

/**
 *
 * @author ochoaa
 */
public class TimelineRadiationRecord {
    private String PATIENT_ID;
    private String START_DATE;
    private String STOP_DATE;
    private String EVENT_TYPE;
    private String TREATMENT_TYPE;
    private String SUBTYPE;
    private String ANATOMY;
    private String PLANNED_DOSE;
    private String DELIVERED_DOSE;
    private String PLANNED_FRACTIONS;
    private String DELIVERED_FRACTIONS;
    private String REF_POINT_SITE;


    public TimelineRadiationRecord(){}

    public TimelineRadiationRecord(String patientId, String tumorDiagnosisDate, Radiation radiation) throws ParseException {
        this.PATIENT_ID = patientId;
        this.START_DATE = DDPUtils.resolveTimelineEventDateInDays(tumorDiagnosisDate, radiation.getRadOncTreatmentCourseStartDate());
        this.STOP_DATE = DDPUtils.resolveTimelineEventDateInDays(tumorDiagnosisDate, radiation.getRadOncTreatmentCourseStopDate());
        this.EVENT_TYPE = "TREATMENT";
        this.TREATMENT_TYPE = "Radiation Therapy";
        this.SUBTYPE = radiation.getPlanName();
        this.ANATOMY = (!Strings.isNullOrEmpty(radiation.getPlanNameAnatomy())) ? radiation.getPlanNameAnatomy() : "";
        this.PLANNED_DOSE = (!Strings.isNullOrEmpty(radiation.getPlannedDose())) ? radiation.getPlannedDose() : "";
        this.DELIVERED_DOSE = (!Strings.isNullOrEmpty(radiation.getDeliveredDose())) ? radiation.getDeliveredDose() : "";
        this.PLANNED_FRACTIONS = (radiation.getPlannedFractions() != null) ? String.valueOf(radiation.getPlannedFractions()) : "";
        this.DELIVERED_FRACTIONS = (radiation.getDeliveredFractions()!= null) ? String.valueOf(radiation.getDeliveredFractions()) : "";
        this.REF_POINT_SITE = (!Strings.isNullOrEmpty(radiation.getReferencePointSite())) ? radiation.getReferencePointSite() : "";

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
     * @return the ANATOMY
     */
    public String getANATOMY() {
        return ANATOMY;
    }

    /**
     * @param ANATOMY the ANATOMY to set
     */
    public void setANATOMY(String ANATOMY) {
        this.ANATOMY = ANATOMY;
    }

    /**
     * @return the PLANNED_DOSE
     */
    public String getPLANNED_DOSE() {
        return PLANNED_DOSE;
    }

    /**
     * @param PLANNED_DOSE the PLANNED_DOSE to set
     */
    public void setPLANNED_DOSE(String PLANNED_DOSE) {
        this.PLANNED_DOSE = PLANNED_DOSE;
    }

    /**
     * @return the DELIVERED_DOSE
     */
    public String getDELIVERED_DOSE() {
        return DELIVERED_DOSE;
    }

    /**
     * @param DELIVERED_DOSE the DELIVERED_DOSE to set
     */
    public void setDELIVERED_DOSE(String DELIVERED_DOSE) {
        this.DELIVERED_DOSE = DELIVERED_DOSE;
    }

    /**
     * @return the PLANNED_FRACTIONS
     */
    public String getPLANNED_FRACTIONS() {
        return PLANNED_FRACTIONS;
    }

    /**
     * @param PLANNED_FRACTIONS the PLANNED_FRACTIONS to set
     */
    public void setPLANNED_FRACTIONS(String PLANNED_FRACTIONS) {
        this.PLANNED_FRACTIONS = PLANNED_FRACTIONS;
    }

    /**
     * @return the DELIVERED_FRACTIONS
     */
    public String getDELIVERED_FRACTIONS() {
        return DELIVERED_FRACTIONS;
    }

    /**
     * @param DELIVERED_FRACTIONS the DELIVERED_FRACTIONS to set
     */
    public void setDELIVERED_FRACTIONS(String DELIVERED_FRACTIONS) {
        this.DELIVERED_FRACTIONS = DELIVERED_FRACTIONS;
    }

    /**
     * @return the REF_POINT_SITE
     */
    public String getREF_POINT_SITE() {
        return REF_POINT_SITE;
    }

    /**
     * @param REF_POINT_SITE the REF_POINT_SITE to set
     */
    public void setREF_POINT_SITE(String REF_POINT_SITE) {
        this.REF_POINT_SITE = REF_POINT_SITE;
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
        fieldNames.add("ANATOMY");
        fieldNames.add("PLANNED_DOSE");
        fieldNames.add("DELIVERED_DOSE");
        fieldNames.add("PLANNED_FRACTIONS");
        fieldNames.add("DELIVERED_FRACTIONS");
        fieldNames.add("REF_POINT_SITE");
        return fieldNames;
    }
}

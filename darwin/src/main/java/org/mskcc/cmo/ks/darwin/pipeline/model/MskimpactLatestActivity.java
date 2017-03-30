/*
 * Copyright (c) 2016 Memorial Sloan-Kettering Cancer Center.
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
package org.mskcc.cmo.ks.darwin.pipeline.model;

/**
 *
 * @author heinsz
 */
public class MskimpactLatestActivity {
    private Integer ptIdPla;
    private String dmpIdPla;
    private Integer lastAppointmentYear;
    private Integer lastDrVisitYear;
    private Integer lastInpatientAdmissionYear;
    private Integer lastInpatientDischargeYear;
    private Integer lastContactYear;
    private Integer lastActivityYear;
    private Integer lastMskChemoYear;
    private Integer lastMskRadiationTherapyYear;
    private Integer lastMskSurgeryYear;
    private Integer lastMskTreatmentYear;
    private Integer lastKnowAliveYear;
    private Integer ageAtLastAppointmentYearInDays;
    private Integer ageAtLastDrVisitYearInDays;
    private Integer ageAtLastInpatientAdmissionYearInDays;
    private Integer ageAtLastInpatientDischargeYearInDays;
    private Integer ageAtLastContactYearInDays;
    private Integer lastActivityYearInDays;
    private Integer ageAtLastMskChemoYearInDays;
    private Integer ageAtLastMskRadiationTherapy;
    private Integer ageAtLastMskSurgeryYearInDays;
    private Integer ageAtLastMskTreatmentYear;
    private Integer ageAtLastKnownAliveYearInDays;
    
    public MskimpactLatestActivity() {}    
    
    public Integer getPT_ID_PLA() {
        return ptIdPla;
    }
    
    public void setPT_ID_PLA(Integer ptIdPla) {
        this.ptIdPla = ptIdPla;
    }
    
    public String getDMP_ID_PLA() {
        return dmpIdPla;
    }
    
    public void setDMP_ID_PLA(String dmpIdPla) {
        this.dmpIdPla = dmpIdPla;
    }
    
    public Integer getLAST_APPOINTMENT_YEAR() {
        return lastAppointmentYear;
    }
    
    public void setLAST_APPOINTMENT_YEAR(Integer lastAppointmentYear) {
        this.lastAppointmentYear = lastAppointmentYear;
    }
    
    public Integer getLAST_DR_VISIT_YEAR() {
        return lastDrVisitYear;
    }
    
    public void setLAST_DR_VISIT_YEAR(Integer lastDrVisitYear) {
        this.lastDrVisitYear = lastDrVisitYear;
    }

    public Integer getLAST_INPATIENT_ADMISSION_YEAR() {
        return lastInpatientAdmissionYear;
    }
    
    public void setLAST_INPATIENT_ADMISSION_YEAR(Integer lastInpatientAdmissionYear) {
        this.lastInpatientAdmissionYear = lastInpatientAdmissionYear;
    }

    public Integer getLAST_INPATIENT_DISCHARGE_YEAR() {
        return lastInpatientDischargeYear;
    }
    
    public void setLAST_INPATIENT_DISCHARGE_YEAR(Integer lastInpatientDischargeYear) {
        this.lastInpatientDischargeYear = lastInpatientDischargeYear;
    }

    public Integer getLAST_CONTACT_YEAR() {
        return lastContactYear;
    }
    
    public void setLAST_CONTACT_YEAR(Integer lastContactYear) {
        this.lastContactYear = lastContactYear;
    }

    public Integer getLAST_ACTIVITY_YEAR() {
        return lastActivityYear;
    }
    
    public void setLAST_ACTIVITY_YEAR(Integer lastActivityYear) {
        this.lastActivityYear = lastActivityYear;
    }

    public Integer getLAST_MSK_CHEMO_YEAR() {
        return lastMskChemoYear;
    }
    
    public void setLAST_MSK_CHEMO_YEAR(Integer lastMskChemoYear) {
        this.lastMskChemoYear = lastMskChemoYear;
    }

    public Integer getLAST_MSK_RADIATION_THERAPY_YEAR() {
        return lastMskRadiationTherapyYear;
    }
    
    public void setLAST_MSK_RADIATION_THERAPY_YEAR(Integer lastMskRadiationTherapyYear) {
        this.lastMskRadiationTherapyYear = lastMskRadiationTherapyYear;
    }

    public Integer getLAST_MSK_SURGERY_YEAR() {
        return lastMskSurgeryYear;
    }
    
    public void setLAST_MSK_SURGERY_YEAR(Integer lastMskSurgeryYear) {
        this.lastMskSurgeryYear = lastMskSurgeryYear;
    }

    public Integer getLAST_MSK_TREATMENT_YEAR() {
        return lastMskTreatmentYear;
    }
    
    public void setLAST_MSK_TREATMENT_YEAR(Integer lastMskTreatmentYear) {
        this.lastMskTreatmentYear = lastMskTreatmentYear;
    }

    public Integer getLAST_KNOWN_ALIVE_YEAR() {
        return lastKnowAliveYear;
    }
    
    public void setLAST_KNOWN_ALIVE_YEAR(Integer lastKnowAliveYear) {
        this.lastKnowAliveYear = lastKnowAliveYear;
    }

    public Integer getAGE_AT_LAST_APPOINTMENT_YEAR_IN_DAYS() {
        return ageAtLastAppointmentYearInDays;
    }
    
    public void setAGE_AT_LAST_APPOINTMENT_YEAR_IN_DAYS(Integer ageAtLastAppointmentYearInDays) {
        this.ageAtLastAppointmentYearInDays = ageAtLastAppointmentYearInDays;
    }

    public Integer getAGE_AT_LAST_DR_VISIT_YEAR_IN_DAYS() {
        return ageAtLastDrVisitYearInDays;
    }
    
    public void setAGE_AT_LAST_DR_VISIT_YEAR_IN_DAYS(Integer ageAtLastDrVisitYearInDays) {
        this.ageAtLastDrVisitYearInDays = ageAtLastDrVisitYearInDays;
    }
    
    public Integer getAGE_AT_LAST_INPATIENT_ADMISSION_YEAR_IN_DAYS() {
        return ageAtLastInpatientAdmissionYearInDays;
    }
    
    public void setAGE_AT_LAST_INPATIENT_ADMISSION_YEAR_IN_DAYS(Integer ageAtLastInpatientAdmissionYearInDays) {
        this.ageAtLastInpatientAdmissionYearInDays = ageAtLastInpatientAdmissionYearInDays;
    } 

    public Integer getAGE_AT_LAST_INPATIENT_DISCHARGE_YEAR_IN_DAYS() {
        return ageAtLastInpatientDischargeYearInDays;
    }
    
    public void setAGE_AT_LAST_INPATIENT_DISCHARGE_YEAR_IN_DAYS(Integer ageAtLastInpatientDischargeYearInDays) {
        this.ageAtLastInpatientDischargeYearInDays = ageAtLastInpatientDischargeYearInDays;
    } 

    public Integer getAGE_AT_LAST_CONTACT_YEAR_IN_DAYS() {
        return ageAtLastContactYearInDays;
    }
    
    public void setAGE_AT_LAST_CONTACT_YEAR_IN_DAYS(Integer ageAtLastContactYearInDays) {
        this.ageAtLastContactYearInDays = ageAtLastContactYearInDays;
    } 

    public Integer getLAST_ACTIVITY_YEAR_IN_DAYS() {
        return lastActivityYearInDays;
    }
    
    public void setLAST_ACTIVITY_YEAR_IN_DAYS(Integer lastActivityYearInDays) {
        this.lastDrVisitYear = lastActivityYearInDays;
    } 

    public Integer getAGE_AT_LAST_MSK_CHEMO_YEAR_IN_DAYS() {
        return ageAtLastMskChemoYearInDays;
    }
    
    public void setAGE_AT_LAST_MSK_CHEMO_YEAR_IN_DAYS(Integer ageAtLastMskChemoYearInDays) {
        this.ageAtLastMskChemoYearInDays = ageAtLastMskChemoYearInDays;
    } 

    public Integer getAGE_AT_LAST_MSK_RADIATION_THERAPY() {
        return ageAtLastMskRadiationTherapy;
    }
    
    public void setAGE_AT_LAST_MSK_RADIATION_THERAPY(Integer ageAtLastMskRadiationTherapy) {
        this.ageAtLastMskRadiationTherapy = ageAtLastMskRadiationTherapy;
    } 

    public Integer getAGE_AT_LAST_MSK_SURGERY_YEAR_IN_DAYS() {
        return ageAtLastMskSurgeryYearInDays;
    }
    
    public void setAGE_AT_LAST_MSK_SURGERY_YEAR_IN_DAYS(Integer ageAtLastMskSurgeryYearInDays) {
        this.ageAtLastMskSurgeryYearInDays = ageAtLastMskSurgeryYearInDays;
    } 

    public Integer getAGE_AT_LAST_MSK_TREATMENT_YEAR() {
        return ageAtLastMskTreatmentYear;
    }
    
    public void setAGE_AT_LAST_MSK_TREATMENT_YEAR(Integer ageAtLastMskTreatmentYear) {
        this.ageAtLastMskTreatmentYear = ageAtLastMskTreatmentYear;
    }  
    
    public Integer getAGE_AT_LAST_KNOWN_ALIVE_YEAR_IN_DAYS() {
        return ageAtLastKnownAliveYearInDays;
    }
    
    public void setAGE_AT_LAST_KNOWN_ALIVE_YEAR_IN_DAYS(Integer ageAtLastKnownAliveYearInDays) {
        this.ageAtLastKnownAliveYearInDays = ageAtLastKnownAliveYearInDays;
    }
}

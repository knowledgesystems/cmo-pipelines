/*
 * Copyright (c) 2018-2019 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.ddp.source.composite;

import org.mskcc.cmo.ks.ddp.source.model.*;

import com.google.common.base.Strings;
import java.util.*;

/**
 *
 * @author ochoaa
 */
public class DDPCompositeRecord {
    private String ddpDeidentifiedPid;
    private String dmpPatientId;
    private List<String> dmpSampleIds;
    private CohortPatient cohortPatient;
    private PatientDemographics patientDemographics;
    private List<PatientDiagnosis> patientDiagnosis;
    private List<Radiation> radiationProcedures;
    private List<Chemotherapy> chemoProcedures;
    private List<Surgery> surgicalProcedures;
    private Boolean pediatricPatientStatus;

    public DDPCompositeRecord(){}

    public DDPCompositeRecord(String dmpPatientId) {
        this.dmpPatientId = dmpPatientId;
    }

    public DDPCompositeRecord(String ddpDeidentifiedPid, String dmpPatientId, List<String> dmpSampleIds) {
        this.ddpDeidentifiedPid = ddpDeidentifiedPid;
        this.dmpPatientId = dmpPatientId;
        this.dmpSampleIds = (dmpSampleIds != null) ? dmpSampleIds : new ArrayList();
    }

    public DDPCompositeRecord(String dmpPatientId, List<String> dmpSampleIds, CohortPatient cohortPatient) {
        this.ddpDeidentifiedPid = String.valueOf(cohortPatient.getPID());
        this.dmpPatientId = dmpPatientId;
        this.dmpSampleIds = (dmpSampleIds != null) ? dmpSampleIds : new ArrayList();
        this.cohortPatient = cohortPatient;
    }

    /**
     * @return the ddpDeidentifiedPid
     */
    public String getDdpDeidentifiedPid() {
        return ddpDeidentifiedPid;
    }

    /**
     * @param ddpDeidentifiedPid the ddpDeidentifiedPid to set
     */
    public void setDdpDeidentifiedPid(String ddpDeidentifiedPid) {
        this.ddpDeidentifiedPid = ddpDeidentifiedPid;
    }

    /**
     * @return the dmpPatientId
     */
    public String getDmpPatientId() {
        return dmpPatientId;
    }

    /**
     * @param dmpPatientId the dmpPatientId to set
     */
    public void setDmpPatientId(String dmpPatientId) {
        this.dmpPatientId = dmpPatientId;
    }

    /**
     * @return the dmpSampleIds
     */
    public List<String> getDmpSampleIds() {
        return dmpSampleIds;
    }

    /**
     * @param dmpSampleIds the dmpSampleIds to set
     */
    public void setDmpSampleIds(List<String> dmpSampleIds) {
        this.dmpSampleIds = dmpSampleIds;
    }

    /**
     * @return the cohortPatient
     */
    public CohortPatient getCohortPatient() {
        return cohortPatient;
    }

    /**
     * @param cohortPatient the cohortPatient to set
     */
    public void setCohortPatient(CohortPatient cohortPatient) {
        this.cohortPatient = cohortPatient;
    }

    /**
     * @return the patientDemographics
     */
    public PatientDemographics getPatientDemographics() {
        return patientDemographics;
    }

    /**
     * @param patientDemographics the patientDemographics to set
     */
    public void setPatientDemographics(PatientDemographics patientDemographics) {
        this.patientDemographics = patientDemographics;
    }

    /**
     * @return the patientDiagnosis
     */
    public List<PatientDiagnosis> getPatientDiagnosis() {
        return patientDiagnosis;
    }

    /**
     * @param patientDiagnosis the patientDiagnosis to set
     */
    public void setPatientDiagnosis(List<PatientDiagnosis> patientDiagnosis) {
        this.patientDiagnosis = patientDiagnosis;
    }

    /**
     * @return the radiationProcedures
     */
    public List<Radiation> getRadiationProcedures() {
        return radiationProcedures;
    }

    /**
     * @param radiationProcedures the radiationProcedures to set
     */
    public void setRadiationProcedures(List<Radiation> radiationProcedures) {
        this.radiationProcedures = radiationProcedures;
    }

    /**
     * @return the chemoProcedures
     */
    public List<Chemotherapy> getChemoProcedures() {
        return chemoProcedures;
    }

    /**
     * @param chemoProcedures the chemoProcedures to set
     */
    public void setChemoProcedures(List<Chemotherapy> chemoProcedures) {
        this.chemoProcedures = chemoProcedures;
    }

    /**
     * @return the surgicalProcedures
     */
    public List<Surgery> getSurgicalProcedures() {
        return surgicalProcedures;
    }

    /**
     * @param surgicalProcedures the surgicalProcedures to set
     */
    public void setSurgicalProcedures(List<Surgery> surgicalProcedures) {
        this.surgicalProcedures = surgicalProcedures;
    }

    public Integer getPatientAge() {
        return (patientDemographics.getCurrentAge() != null) ? patientDemographics.getCurrentAge() :
                cohortPatient.getAGE();
    }

    public String getPatientSex() {
        return (!Strings.isNullOrEmpty(patientDemographics.getGender())) ? patientDemographics.getGender() :
                cohortPatient.getPTSEX();
    }

    public String getPatientBirthDate() {
        return (!Strings.isNullOrEmpty(patientDemographics.getPTBIRTHDTE())) ?
                patientDemographics.getPTBIRTHDTE() : patientDemographics.getDateOfBirth();
    }

    public String getPatientDeathDate() {
        return (!Strings.isNullOrEmpty(patientDemographics.getPTDEATHDTE())) ?
                patientDemographics.getPTDEATHDTE() : patientDemographics.getDeceasedDate();
    }

    public String getLastContactDate() {
        // if patient is not deceased then return last contact date
        // otherwise return date of death
        String patientDeathDate = getPatientDeathDate();
        if (!Strings.isNullOrEmpty(patientDeathDate)) {
            return patientDeathDate;
        }
        return (!Strings.isNullOrEmpty(patientDemographics.getPLALASTCONTACTDTE()) ?
                patientDemographics.getPLALASTCONTACTDTE() : patientDemographics.getLastContactDate());
    }

    public String getPatientRace() {
        return patientDemographics.getRace();
    }

    public String getPatientReligion() {
        return patientDemographics.getReligion();
    }

    public String getPatientEthnicity() {
        return patientDemographics.getEthnicity();
    }

    public Boolean hasReceivedRadiation() {
        return (radiationProcedures != null && !radiationProcedures.isEmpty());
    }

    public Boolean hasReceivedRadiation(String radiationType) {
        if (radiationProcedures != null && !radiationProcedures.isEmpty()) {
            for (Radiation procedure : radiationProcedures) {
                if (procedure.getPlanName().equalsIgnoreCase(radiationType)) {
                    return Boolean.TRUE;
                }
            }
        }
        return Boolean.FALSE;
    }

    public Boolean hasReceivedChemo() {
        return (chemoProcedures != null && !chemoProcedures.isEmpty());
    }

    public Boolean hasReceivedChemo(String chemoType) {
        if (chemoProcedures != null && !chemoProcedures.isEmpty()) {
            for (Chemotherapy procedure : chemoProcedures) {
                if (procedure.getORDNAME().equalsIgnoreCase(chemoType)) {
                    return Boolean.TRUE;
                }
            }
        }
        return Boolean.FALSE;
    }

    public Boolean hasReceivedSurgery() {
        return (surgicalProcedures != null && !surgicalProcedures.isEmpty());
    }

    public Boolean hasReceivedSurgery(String surgeryType) {
        if (surgicalProcedures != null && !surgicalProcedures.isEmpty()) {
            for (Surgery procedure : surgicalProcedures) {
                if (procedure.getProcedureDescription().equalsIgnoreCase(surgeryType)) {
                    return Boolean.TRUE;
                }
            }
        }
        return Boolean.FALSE;
    }

    /**
     * @return the pediatricPatientStatus
     */
    public Boolean getPediatricPatientStatus() {
        return pediatricPatientStatus;
    }

    /**
     * @param pediatricPatientStatus the pediatricPatientStatus to set
     */
    public void setPediatricPatientStatus(Boolean pediatricPatientStatus) {
        this.pediatricPatientStatus = pediatricPatientStatus;
    }
}

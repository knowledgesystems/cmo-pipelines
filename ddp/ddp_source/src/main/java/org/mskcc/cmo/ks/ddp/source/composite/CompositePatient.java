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

package org.mskcc.cmo.ks.ddp.source.composite;

import org.mskcc.cmo.ks.ddp.source.model.CohortPatient;
import org.mskcc.cmo.ks.ddp.source.model.PatientDemographics;
import org.mskcc.cmo.ks.ddp.source.model.PatientDiagnosis;

import com.google.common.base.Strings;
import java.util.*;

/**
 *
 * @author ochoaa
 */
public class CompositePatient {
    private String ddpDeidentifiedPid;
    private String dmpPatientId;
    private List<String> dmpSampleIds;
    private CohortPatient cohortPatient;
    private PatientDemographics patientDemographics;
    private List<PatientDiagnosis> patientDiagnosis;

    public CompositePatient(){}

    public CompositePatient(String dmpPatientId, List<String> dmpSampleIds, CohortPatient cohortPatient) {
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

    public Integer getPatientAge() {
        return (patientDemographics.getCurrentAge()!= null) ? patientDemographics.getCurrentAge() :
                cohortPatient.getAGE();
    }

    public String getPatientSex() {
        return (!Strings.isNullOrEmpty(patientDemographics.getGender())) ? patientDemographics.getGender() :
                cohortPatient.getPTSEX();
    }
}

/*
 * Copyright (c) 2015 Memorial Sloan-Kettering Cancer Center.
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
 * This file is part of cBioPortal.
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

package org.cbioportal.cmo.pipelines.importer.config.composite;

import org.cbioportal.cmo.pipelines.importer.model.*;

import java.util.*;

/**
 *
 * @author ochoaa
 */
public class CompositeClinicalData {
    private Patient patient;
    private Sample sample;
    private List<ClinicalData> patientClinicalData;
    private List<ClinicalData> sampleClinicalData;

    public CompositeClinicalData(){}
    
    public CompositeClinicalData(Patient patient, Sample sample) {
        this.patient = patient;
        this.sample = sample;
        this.patientClinicalData = new ArrayList();
        this.sampleClinicalData = new ArrayList();
    }
    
    public CompositeClinicalData(Patient patient, Sample sample, List<ClinicalData> patientClinicalData, List<ClinicalData> sampleClinicalData) {
        this.patient = patient;
        this.sample = sample;
        this.patientClinicalData = patientClinicalData;
        this.sampleClinicalData = sampleClinicalData;
    }    

    /**
     * @return the patient
     */
    public Patient getPatient() {
        return patient;
    }

    /**
     * @param patient the patient to set
     */
    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    /**
     * @return the sample
     */
    public Sample getSample() {
        return sample;
    }

    /**
     * @param sample the sample to set
     */
    public void setSample(Sample sample) {
        this.sample = sample;
    }

    /**
     * @return the patientClinicalData
     */
    public List<ClinicalData> getPatientClinicalData() {
        return patientClinicalData;
    }

    /**
     * @param patientClinicalData the patientClinicalData to set
     */
    public void setPatientClinicalData(List<ClinicalData> patientClinicalData) {
        this.patientClinicalData = patientClinicalData;
    }

    /**
     * @return the sampleClinicalData
     */
    public List<ClinicalData> getSampleClinicalData() {
        return sampleClinicalData;
    }

    /**
     * @param sampleClinicalData the sampleClinicalData to set
     */
    public void setSampleClinicalData(List<ClinicalData> sampleClinicalData) {
        this.sampleClinicalData = sampleClinicalData;
    }

    /**
     * propagate internal id update to patient clinical data
     * @param internalId 
     */
    public void updatePatientInternalId(int internalId) {
        this.patient.setINTERNAL_ID(internalId);
        if (this.sample.getPATIENT_ID() == -1) {
            this.sample.setPATIENT_ID(internalId);
        }
        if (!this.patientClinicalData.isEmpty()) {
            List<ClinicalData> updatedClinicalData = new ArrayList();
            for (ClinicalData cd : this.patientClinicalData) {
                updatedClinicalData.add(new ClinicalData(internalId, cd.getATTR_ID(), cd.getATTR_VALUE()));
            }
            this.patientClinicalData.clear();
            this.patientClinicalData.addAll(updatedClinicalData);
        }        
    }

    /**
     * propagate internal id update to sample clinical data
     * @param internalId 
     */    
    public void updateSampleInternalId(int internalId) {
        this.sample.setINTERNAL_ID(internalId);
        if (this.patient.getINTERNAL_ID() == -1) {
            this.patient.setINTERNAL_ID(this.sample.getPATIENT_ID());
        }
        if (!this.sampleClinicalData.isEmpty()) {
            List<ClinicalData> updatedClinicalData = new ArrayList();
            for (ClinicalData cd : this.sampleClinicalData) {
                updatedClinicalData.add(new ClinicalData(internalId, cd.getATTR_ID(), cd.getATTR_VALUE()));
            }
            this.sampleClinicalData.clear();
            this.sampleClinicalData.addAll(updatedClinicalData);
        }        
    }    
}
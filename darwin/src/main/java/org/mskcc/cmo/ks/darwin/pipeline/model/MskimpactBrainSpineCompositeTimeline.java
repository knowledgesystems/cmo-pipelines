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

import java.util.*;
//import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactTimelineBrainSpine;

/**
 *
 * @author jake
 */

public class MskimpactBrainSpineCompositeTimeline {
    
    private String statusResult;
    private String specimenResult;
    private String treatmentResult;
    private String imagingResult;
    private String surgeryResult;
    private MskimpactBrainSpineTimeline record;
    
    public static final String NO_RESULT = "NO RESULT";

    public MskimpactBrainSpineCompositeTimeline(MskimpactBrainSpineTimeline record){
        this.statusResult = NO_RESULT;
        this.specimenResult = NO_RESULT;
        this.treatmentResult = NO_RESULT;
        this.imagingResult = NO_RESULT;
        this.surgeryResult = NO_RESULT;
        this.record = record;
    }
    
    public String getSurgeryResult(){
        return surgeryResult;
    }
    public void setSurgeryResult(String s){
        this.surgeryResult = s;
    }
    public String getImagingResult(){
        return imagingResult;
    }
    public void setImagingResult(String s){
        this.imagingResult = s;
    }
    
    public String getStatusResult(){
        return statusResult;
    }
    public void setStatusResult(String s){
        this.statusResult = s;
    }
    
    public String getTreatmentResult(){
        return treatmentResult;
    }
    public void setTreatmentResult(String s){
        this.treatmentResult = s;
    }
    
    public String getSpecimenResult(){
        return specimenResult;
    }
    public void setSpecimenResult(String s){
        this.specimenResult = s;
    }
    
    public MskimpactBrainSpineTimeline getRecord(){
        return this.record;
    }
    public void setRecord(MskimpactBrainSpineTimeline record){
        this.record = record;
    }
    
    public List<String> getJointRecord(){
        List<String> jointRecord = new ArrayList();
        jointRecord.add(this.statusResult);
        jointRecord.add(this.specimenResult);
        jointRecord.add(this.treatmentResult);
        jointRecord.add(this.imagingResult);
        jointRecord.add(this.surgeryResult);
        return jointRecord;
    }
    
    
    
}

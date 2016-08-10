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

package org.cbioportal.cmo.pipelines.foundation.model.staging;

import org.cbioportal.cmo.pipelines.foundation.model.CaseType;
import org.cbioportal.cmo.pipelines.foundation.model.MetricType;

import java.util.*;

/**
 *
 * @author Prithi Chakrapani, ochoaa
 */
public class ClinicalData /*extends DataClinicalModel*/ {
     
    private String sampleId;
    private String gender;
    private String studyId;
    private String pipelineVersion;
    private String tumorNucleiPercent;
    private String medianCoverage;
    private String x100Cov;   
    private String errorPercent;
    private String diseaseOntology;
    private String purityAssessment;
    private String genePanel;

    public ClinicalData(){}
    
    public ClinicalData(CaseType caseType) {
        this.sampleId = caseType.getCase();
        this.gender = caseType.getVariantReport().getGender();
        this.studyId = caseType.getFmiCase();
        this.pipelineVersion = caseType.getVariantReport().getPipelineVersion();
        
        this.tumorNucleiPercent = "";
        this.medianCoverage = "";
        this.x100Cov = "";
        this.errorPercent = "";
        try {
            List<MetricType> metricData = caseType.getVariantReport().getQualityControl().getMetrics().getMetric();
            for (MetricType m : metricData) {
                if (m.getName().equals("Tumor Nuclei Percent") && this.tumorNucleiPercent.isEmpty()) {
                    this.tumorNucleiPercent = m.getValue();
                }
                else if (m.getName().equals("Median coverage") && this.medianCoverage.isEmpty()) {
                    this.medianCoverage = m.getValue();
                }
                else if (m.getName().equals("Coverage >100X") && this.x100Cov.isEmpty()) {
                    this.x100Cov = m.getValue();
                }
                else if (m.getName().equals("Error") && this.errorPercent.isEmpty()) {
                    this.errorPercent = m.getValue();
                }
            }            
        }
        catch (NullPointerException ex) {}

        this.diseaseOntology = caseType.getVariantReport().getDiseaseOntology()!=null?
                caseType.getVariantReport().getDiseaseOntology():"";
        this.gender = caseType.getVariantReport().getGender()!=null?
                caseType.getVariantReport().getGender():"";
        this.purityAssessment = caseType.getVariantReport().getPurityAssessment()!=null?
                caseType.getVariantReport().getPurityAssessment():"";
        this.genePanel = caseType.getVariantReport().getStudy()!=null?
                caseType.getVariantReport().getStudy():"";
    }
    
    /** 
     * @return the sampleId
     */
    public String getSampleId() {
        return sampleId;
    }

    /**
     * @param sampleId the sampleId to set
     */
    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    /**
     * @return the gender
     */
    public String getGender() {
        return gender;
    }

    /**
     * @param gender the gender to set
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * @return the studyId
     */
    public String getStudyId() {
        return studyId;
    }

    /**
     * @param studyId the studyId to set
     */
    public void setStudyId(String studyId) {
        this.studyId = studyId;
    }

    /**
     * @return the pipelineVersion
     */
    public String getPipelineVersion() {
        return pipelineVersion;
    }

    /**
     * @param pipelineVersion the pipelineVersion to set
     */
    public void setPipelineVersion(String pipelineVersion) {
        this.pipelineVersion = pipelineVersion;
    }

    /**
     * @return the tumorNucleiPercent
     */
    public String getTumorNucleiPercent() {
        return tumorNucleiPercent;
    }

    /**
     * @param tumorNucleiPercent the tumorNucleiPercent to set
     */
    public void setTumorNucleiPercent(String tumorNucleiPercent) {
        this.tumorNucleiPercent = tumorNucleiPercent;
    }

    /**
     * @return the medianCoverage
     */
    public String getMedianCoverage() {
        return medianCoverage;
    }

    /**
     * @param medianCoverage the medianCoverage to set
     */
    public void setMedianCoverage(String medianCoverage) {
        this.medianCoverage = medianCoverage;
    }
    /**
     * @return the x100Cov
     */
    public String getX100Cov() {
        return x100Cov;
    }

    /**
     * @param x100Cov the x100Cov to set
     */
    public void setX100Cov(String x100Cov) {
        this.x100Cov = x100Cov;
    }
    /**
     * @return the diseaseOntology
     */
    public String getDiseaseOntology() {
        return diseaseOntology;
    }

    /**
     * @param diseaseOntology the diseaseOntology to set
     */
    public void setDiseaseOntology(String diseaseOntology) {
        this.diseaseOntology = diseaseOntology;
    }
    
    /**
     * @return the purityAssessment
     */
    public String getPurityAssessment() {
        return purityAssessment;
    }

    /**
     * @param purityAssessment the purityAssessment to set
     */
    public void setPurityAssessment(String purityAssessment) {
        this.purityAssessment = purityAssessment;
    }

    /**
     * @return the genePanel
     */
    public String getGenePanel() {
        return genePanel;
    }

    /**
     * @param genePanel the genePanel to set
     */
    public void setGenePanel(String genePanel) {
        this.genePanel = genePanel;
    }

    /**
     * @return the errorPercent
     */
    public String getErrorPercent() {
        return errorPercent;
    }

    /**
     * @param errorPercent the errorPercent to set
     */
    public void setErrorPercent(String errorPercent) {
        this.errorPercent = errorPercent;
    }

    /**
     * Returns a map linking the staging file column name to the appropriate getter method.
     * @return 
     */
    public Map<String, String> getStagingMap(){
        Map<String, String> map = new LinkedHashMap<>();
        map.put("PATIENT_ID","getSampleId");
        map.put("SAMPLE_ID","getSampleId");
        map.put("SEX","getGender");
        map.put("STUDY_ID","getStudyId");
        map.put("PIPELINE_VERSION","getPipelineVersion");
        map.put("PERCENT_TUMOR_NUCLEI","getTumorNucleiPercent");
        map.put("MEDIAN_COVERAGE","getMedianCoverage");
        map.put("COVERAGE_GREATER_THAN_100X","getX100Cov");
        map.put("ERROR_PERCENT","getErrorPercent");
        map.put("DISEASE_ONTOLOGY","getDiseaseOntology");
        map.put("PURITY_ASSESSMENT","getPurityAssessment");
        map.put("GENE_PANEL","getGenePanel");

        return map;
    }        

}
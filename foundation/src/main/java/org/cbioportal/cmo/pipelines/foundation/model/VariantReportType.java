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

package org.cbioportal.cmo.pipelines.foundation.model;

import javax.xml.bind.annotation.*;

/**
 *
 * @author Prithi Chakrapani, ochoaa
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VariantReportType", propOrder = {
    "samples",
    "qualityControl",
    "shortVariants",
    "copyNumberAlterations",
    "rearrangements"
})
public class VariantReportType {

   @XmlElement(required = true)
   protected SamplesType samples;
   
   @XmlElement(name = "quality-control", required = true)
   protected QualityControlType qualityControl;
   
   @XmlElement(name = "short-variants", required = true)
   protected ShortVariantsType shortVariants;
   
   @XmlElement(name = "copy-number-alterations", required = true)
    protected CopyNumberAlterationsType copyNumberAlterations;
   
   @XmlElement(name = "rearrangements", required = true)
    protected RearrangementsType rearrangements;

    @XmlAttribute(name = "disease-ontology")
    protected String diseaseOntology;
    
    @XmlAttribute(name = "gender")
    protected String gender;
    
    @XmlAttribute(name = "pipeline-version")
    protected String pipelineVersion;
   
    @XmlAttribute(name = "study")
    protected String study;
    
    @XmlAttribute(name = "test-request")
    protected String testRequest;
    
    @XmlAttribute(name = "purity-assessment")
    protected String purityAssessment;

    /**
     * @return the samples
     */
    public SamplesType getSamples() {
        return samples;
    }

    /**
     * @param samples the samples to set
     */
    public void setSamples(SamplesType samples) {
        this.samples = samples;
    }

    /**
     * @return the qualityControl
     */
    public QualityControlType getQualityControl() {
        return qualityControl;
    }

    /**
     * @param qualityControl the qualityControl to set
     */
    public void setQualityControl(QualityControlType qualityControl) {
        this.qualityControl = qualityControl;
    }

    /**
     * @return the shortVariants
     */
    public ShortVariantsType getShortVariants() {
        return shortVariants;
    }

    /**
     * @param shortVariants the shortVariants to set
     */
    public void setShortVariants(ShortVariantsType shortVariants) {
        this.shortVariants = shortVariants;
    }

    /**
     * @return the copyNumberAlterations
     */
    public CopyNumberAlterationsType getCopyNumberAlterations() {
        return copyNumberAlterations;
    }

    /**
     * @param copyNumberAlterations the copyNumberAlterations to set
     */
    public void setCopyNumberAlterations(CopyNumberAlterationsType copyNumberAlterations) {
        this.copyNumberAlterations = copyNumberAlterations;
    }

    /**
     * @return the rearrangements
     */
    public RearrangementsType getRearrangements() {
        return rearrangements;
    }

    /**
     * @param rearrangements the rearrangements to set
     */
    public void setRearrangements(RearrangementsType rearrangements) {
        this.rearrangements = rearrangements;
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
     * @return the study
     */
    public String getStudy() {
        return study;
    }

    /**
     * @param study the study to set
     */
    public void setStudy(String study) {
        this.study = study;
    }

    /**
     * @return the testRequest
     */
    public String getTestRequest() {
        return testRequest;
    }

    /**
     * @param testRequest the testRequest to set
     */
    public void setTestRequest(String testRequest) {
        this.testRequest = testRequest;
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
    
}
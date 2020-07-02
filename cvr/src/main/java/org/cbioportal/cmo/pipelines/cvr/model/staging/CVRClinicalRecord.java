/*
 * Copyright (c) 2016 - 2017 Memorial Sloan-Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr.model.staging;

import com.mysql.jdbc.StringUtils;
import java.util.*;
import org.cbioportal.cmo.pipelines.cvr.model.CVRMetaData;
import org.cbioportal.cmo.pipelines.cvr.model.GMLMetaData;

/**
 *
 * @author heinsz
 */
public class CVRClinicalRecord {

    private String sampleId;
    private String patientId;
    private String cancerType;
    private String sampleType;
    private String sampleClass;
    private String metastaticSite;
    private String primarySite;
    private String cancerTypeDetailed;
    private String genePanel;
    private String otherPatientId;
    private String soComments;
    private String sampleCoverage;
    private String tumorPurity;
    private String oncotreeCode;
    private String partCConsented;
    private String partAConsented;
    private String msiComment;
    private String msiScore;
    private String msiType;
    private String institute;
    private String somaticStatus;
    private String seqDate;
    private String ageAtSeqReportedYears;
    private String archer;
    private String cvrTmbCohortPercentile;
    private String cvrTmbScore;
    private String cvrTmbTtPercentile;
    private String pathSlideExists;
    private String mskSlideID;

    private final String DEFAULT_SAMPLE_CLASS = "Tumor";
    private final String MSKACCESS_SAMPLE_CLASS = "cfDNA";

    public CVRClinicalRecord(CVRMetaData metaData, String wholeSlideViewerBaseURL, String studyId) {
        this.sampleId = metaData.getDmpSampleId();
        this.patientId = metaData.getDmpPatientId();
        this.cancerType = metaData.getTumorTypeName();
        this.sampleType = resolveSampleType(metaData.getIsMetastasis());
        this.sampleClass = (studyId.equals("mskaccess")) ? MSKACCESS_SAMPLE_CLASS : DEFAULT_SAMPLE_CLASS;
        this.metastaticSite = metaData.getMetastasisSite();
        this.primarySite = metaData.getPrimarySite();
        this.cancerTypeDetailed = metaData.getTumorTypeName();
        this.genePanel = metaData.getGenePanel();
        this.otherPatientId = metaData.getLegacyPatientId();
        this.soComments = metaData.getSoComments();
        this.sampleCoverage = String.valueOf(metaData.getSampleCoverage());
        this.tumorPurity = metaData.getTumorPurity();
        this.oncotreeCode = metaData.getTumorTypeCode();
        this.partAConsented = metaData.getConsentPartA();
        this.partCConsented = metaData.getConsentPartC();
        this.msiComment = metaData.getMsiComment();
        this.msiScore = metaData.getMsiScore();
        this.msiType = metaData.getMsiType();
        this.institute = metaData.getOutsideInstitute();
        this.somaticStatus = metaData.getSomaticStatus();
        this.seqDate = metaData.getDateTumorSequencing();
        this.ageAtSeqReportedYears = "NA";
        this.archer = "NO";
        this.cvrTmbCohortPercentile = (metaData.getTmbCohortPercentile()!= null) ? String.valueOf(metaData.getTmbCohortPercentile()) : "NA";
        this.cvrTmbScore = (metaData.getTmbScore()!= null) ? String.valueOf(metaData.getTmbScore()) : "NA";
        this.cvrTmbTtPercentile = (metaData.getTmbTtPercentile()!= null) ? String.valueOf(metaData.getTmbTtPercentile()) : "NA";
        this.pathSlideExists = (wholeSlideViewerIdIsValid(metaData.getWholeSlideViewerId())) ? "YES" : "NO";
        this.mskSlideID = (wholeSlideViewerIdIsValid(metaData.getWholeSlideViewerId())) ? metaData.getWholeSlideViewerId() : "NA";
    }

    public CVRClinicalRecord(GMLMetaData metaData) {
        this.sampleId = metaData.getDmpSampleId();
        this.patientId = metaData.getDmpPatientId();
        this.cancerType = "";
        this.sampleType = "";
        this.sampleClass = DEFAULT_SAMPLE_CLASS;
        this.metastaticSite = "";
        this.primarySite = "";
        this.cancerTypeDetailed = "";
        this.genePanel = metaData.getGenePanel();
        this.otherPatientId = metaData.getLegacyPatientId();
        this.soComments = metaData.getSoComments();
        this.sampleCoverage = String.valueOf(metaData.getSampleCoverage());
        this.tumorPurity = "";
        this.oncotreeCode = "";
        this.partCConsented = "YES";
        this.msiComment = "";
        this.msiScore = "";
        this.msiType = "";
        this.institute = "";
        this.archer = "NO";
    }

    public CVRClinicalRecord() {
    }

    public String getSAMPLE_ID() {
        return this.sampleId != null ? this.sampleId : "";
    }

    public void setSAMPLE_ID(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getPATIENT_ID() {
        return this.patientId != null ? this.patientId : "";
    }

    public void setPATIENT_ID(String patientId) {
        this.patientId = patientId;
    }

    public String getCANCER_TYPE() {
        return this.cancerType != null ? this.cancerType : "";
    }

    public void setCANCER_TYPE(String cancerType) {
        this.cancerType = cancerType;
    }

    public String getSAMPLE_TYPE() {
        return this.sampleType != null ? this.sampleType : "";
    }

    public void setSAMPLE_TYPE(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getSAMPLE_CLASS() {
        return this.sampleClass != null ? this.sampleClass : "";
    }

    public void setSAMPLE_CLASS(String sampleClass) {
        this.sampleClass = sampleClass;
    }

    public String getMETASTATIC_SITE() {
        return this.metastaticSite != null ? this.metastaticSite : "";
    }

    public void setMETASTATIC_SITE(String metastaticSite) {
        this.metastaticSite = metastaticSite;
    }

    public String getPRIMARY_SITE() {
        return this.primarySite != null ? this.primarySite : "";
    }

    public void setPRIMARY_SITE(String primarySite) {
        this.primarySite = primarySite;
    }

    public String getCANCER_TYPE_DETAILED() {
        return this.cancerTypeDetailed != null ? this.cancerTypeDetailed : "";
    }

    public void setCANCER_TYPE_DETAILED(String cancerTypeDetailed) {
        this.cancerTypeDetailed = cancerTypeDetailed;
    }

    public String getGENE_PANEL() {
        return this.genePanel != null ? this.genePanel : "";
    }

    public void setGENE_PANEL(String genePanel) {
        this.genePanel = genePanel;
    }

    public String getOTHER_PATIENT_ID() {
        return this.otherPatientId != null ? this.otherPatientId : "";
    }

    public void setOTHER_PATIENT_ID(String otherPatientId) {
        this.otherPatientId = otherPatientId;
    }

    public String getSO_COMMENTS() {
        return this.soComments != null ? this.soComments : "";
    }

    public void setSO_COMMENTS(String soComments) {
        this.soComments = soComments;
    }

    public String getSAMPLE_COVERAGE() {
        return this.sampleCoverage != null ? this.sampleCoverage : "";
    }

    public void setSAMPLE_COVERAGE(String sampleCoverage) {
        this.sampleCoverage = sampleCoverage;
    }

    public String getTUMOR_PURITY() {
        return this.tumorPurity != null ? this.tumorPurity : "";
    }

    public void setTUMOR_PURITY(String tumorPurity) {
        this.tumorPurity = tumorPurity;
    }

    public String getONCOTREE_CODE() {
        return this.oncotreeCode != null ? this.oncotreeCode : "";
    }

    public void setONCOTREE_CODE(String oncotreeCode) {
        this.oncotreeCode = oncotreeCode;
    }

    public String getPARTA_CONSENTED_12_245() {
        return this.partAConsented != null ? this.partAConsented : "";
    }

    public void setPARTA_CONSENTED_12_245(String partAConsented) {
        this.partAConsented = partAConsented;
    }

    public String getPARTC_CONSENTED_12_245() {
        return this.partCConsented != null ? this.partCConsented : "";
    }

    public void setPARTC_CONSENTED_12_245(String partCConsented) {
        this.partCConsented = partCConsented;
    }

    public String getMSI_COMMENT() {
        return this.msiComment != null ? this.msiComment : "";
    }

    public void setMSI_COMMENT(String msiComment) {
        this.msiComment = msiComment;
    }

    public String getMSI_SCORE() {
        return this.msiScore != null ? this.msiScore : "";
    }

    public void setMSI_SCORE(String msiScore) {
        this.msiScore = msiScore;
    }

    public String getMSI_TYPE() {
        return this.msiType != null ? this.msiType : "";
    }

    public void setMSI_TYPE(String msiType) {
        this.msiType = msiType;
    }

    public String getINSTITUTE() {
        // Assume MSKCC if not specified or -, the field from CVR is 'outside_institute'
        if (this.institute != null && !this.institute.isEmpty()) {
            return this.institute.equals("-") ? "MSKCC" : this.institute;
        }
        return "MSKCC";
    }

    public void setINSTITUTE(String institute) {
        this.institute = institute;
    }

    public String getSOMATIC_STATUS() {
        return somaticStatus != null ? somaticStatus : "";
    }

    public void setSOMATIC_STATUS(String somaticStatus) {
        this.somaticStatus = somaticStatus;
    }

    public String getSEQ_DATE() {
        return seqDate != null ? seqDate : "";
    }

    public void setSEQ_DATE(String seqDate) {
        this.seqDate = seqDate;
    }

    public String getAGE_AT_SEQ_REPORTED_YEARS() {
        return ageAtSeqReportedYears != null ? ageAtSeqReportedYears : "NA";
    }

    public void setAGE_AT_SEQ_REPORTED_YEARS(String ageAtSeqReportedYears) {
        this.ageAtSeqReportedYears = ageAtSeqReportedYears;
    }

    public String getARCHER() {
        return archer != null ? archer : "NO";
    }

    public void setARCHER(String archer) {
        this.archer = archer;
    }

    public String getCVR_TMB_COHORT_PERCENTILE() {
        return cvrTmbCohortPercentile;
    }

    public void setCVR_TMB_COHORT_PERCENTILE(String tmbCohortPercentile) {
        this.cvrTmbCohortPercentile = tmbCohortPercentile;
    }

    public String getCVR_TMB_SCORE() {
        return cvrTmbScore;
    }

    public void setCVR_TMB_SCORE(String tmbScore) {
        this.cvrTmbScore = tmbScore;
    }

    public String getCVR_TMB_TT_COHORT_PERCENTILE() {
        return cvrTmbTtPercentile;
    }

    public void setCVR_TMB_TT_COHORT_PERCENTILE(String tmbTtPercentile) {
        this.cvrTmbTtPercentile = tmbTtPercentile;
    }

    public String getPATH_SLIDE_EXISTS() {
        return !StringUtils.isNullOrEmpty(this.pathSlideExists) ? this.pathSlideExists : "NO";
    }

    public void setPATH_SLIDE_EXISTS(String pathSlideExists) {
        this.pathSlideExists = pathSlideExists;
    }

    public String getMSK_SLIDE_ID() {
        return !StringUtils.isNullOrEmpty(this.mskSlideID) ? this.mskSlideID : "NA";
    }

    public void setMSK_SLIDE_ID(String mskSlideID) {
        this.mskSlideID = mskSlideID;
    }

    public String resolveSampleType(Integer isMetastasis) {
        if (isMetastasis != null) {
            switch(isMetastasis) {
                case 0:
                    return "Primary";
                case 1:
                    return "Metastasis";
                case 2:
                    return "Local Recurrence";
                case 127:
                    return "Unknown";
                default:
                    return "";
            }
        }
        return "";
    }

    private boolean wholeSlideViewerIdIsValid(String wholeSlideViewerId) {
            return (!StringUtils.isNullOrEmpty(wholeSlideViewerId) && !wholeSlideViewerId.equalsIgnoreCase("NA"));
    }

    public static List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<String>();
        fieldNames.add("SAMPLE_ID");
        fieldNames.add("PATIENT_ID");
        fieldNames.add("CANCER_TYPE");
        fieldNames.add("SAMPLE_TYPE");
        fieldNames.add("SAMPLE_CLASS");
        fieldNames.add("METASTATIC_SITE");
        fieldNames.add("PRIMARY_SITE");
        fieldNames.add("CANCER_TYPE_DETAILED");
        fieldNames.add("GENE_PANEL");
        fieldNames.add("OTHER_PATIENT_ID");
        fieldNames.add("SO_COMMENTS");
        fieldNames.add("SAMPLE_COVERAGE");
        fieldNames.add("TUMOR_PURITY");
        fieldNames.add("ONCOTREE_CODE");
        fieldNames.add("PARTA_CONSENTED_12_245");
        fieldNames.add("PARTC_CONSENTED_12_245");
        fieldNames.add("MSI_COMMENT");
        fieldNames.add("MSI_SCORE");
        fieldNames.add("MSI_TYPE");
        fieldNames.add("INSTITUTE");
        fieldNames.add("SOMATIC_STATUS");
        fieldNames.add("AGE_AT_SEQ_REPORTED_YEARS");
        fieldNames.add("ARCHER");
        fieldNames.add("CVR_TMB_COHORT_PERCENTILE");
        fieldNames.add("CVR_TMB_SCORE");
        fieldNames.add("CVR_TMB_TT_COHORT_PERCENTILE");
        fieldNames.add("PATH_SLIDE_EXISTS");
        fieldNames.add("MSK_SLIDE_ID");
        return fieldNames;
    }

    public static List<String> getSeqDateFieldNames() {
        List<String> fieldNames = new ArrayList<String>();
        fieldNames.add("SAMPLE_ID");
        fieldNames.add("PATIENT_ID");
        fieldNames.add("SEQ_DATE");
        return fieldNames;
    }
}

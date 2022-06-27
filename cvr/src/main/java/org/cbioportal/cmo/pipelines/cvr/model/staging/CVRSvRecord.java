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

import java.util.*;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.cbioportal.cmo.pipelines.cvr.model.CVRSvVariant;
import org.cbioportal.cmo.pipelines.cvr.model.GMLCnvIntragenicVariant;
import org.springframework.batch.core.configuration.annotation.StepScope;
import com.google.common.base.Strings;

/**
 *
 * @author heinsz
 * This models what the CVR pipeline is going to write out into a data_sv.txt file
 * It can construct a CVRSvRecord by
 * 1) Reading from a sv file (SvReader + CVRSvFieldSetMapper)
 * 2) Converting a CVRSvVariant (this is the response from CVR)
 * 3) Converting a CVRGmlVariant (this is the gml response from CVR)
 */

@StepScope
public class CVRSvRecord {

    private String sampleId;
    private String svStatus;
    private String site1HugoSymbol;
    private String site2HugoSymbol;
    private String site1EnsemblTranscriptId;
    private String site2EnsemblTranscriptId;
    private String site1EntrezGeneId;
    private String site2EntrezGeneId;
    private String site1RegionNumber;
    private String site2RegionNumber;
    private String site1Region;
    private String site2Region;
    private String site1Chromosome;
    private String site2Chromosome;
    private String site1Contig;
    private String site2Contig;
    private String site1Position;
    private String site2Position;
    private String site1Description;
    private String site2Description;
    private String site2EffectOnFrame;
    private String ncbiBuild;
    private String svClass; // sv_class_name
    private String tumorSplitReadCount;
    private String tumorPairedEndReadCount;
    private String eventInfo;
    private String breakpointType;
    private String connectionType; // conn_type
    private String annotation;
    private String dnaSupport; // Paired_End_Read_Support
    private String rnaSupport; // Split_Read_Support
    private String svLength;
    private String normalReadCount;
    private String tumorReadCount;
    private String normalVariantCount;
    private String tumorVariantCount;
    private String normalPairedEndReadCount;
    private String normalSplitEndReadCount;
    private String comments;

    public CVRSvRecord() {
    }

    public CVRSvRecord(CVRSvVariant variant, String sampleId) {
        this.sampleId = sampleId;
        this.annotation = variant.getAnnotation();
        this.breakpointType = variant.getBreakpoint_Type();
        this.comments = variant.getComments();
        // CVR confirmed Site1_GENE/Gene1 is a NOT_NULL field
        // Use Gene1 to test whether v1 or v2 schema
        if (variant.getSite1_Gene() != null && !variant.getSite1_Gene().isEmpty()) {
            this.site1HugoSymbol = variant.getSite1_Gene();
            this.site2HugoSymbol = variant.getSite2_Gene();
            this.site1RegionNumber = variant.getSite1_Exon();
            this.site2RegionNumber = variant.getSite2_Exon();
        } else {
            this.site1HugoSymbol = variant.getGene1();
            this.site2HugoSymbol = variant.getGene2();
            this.site1RegionNumber = variant.getExon1();
            this.site2RegionNumber = variant.getExon2();
        }
        this.connectionType = variant.getConnection_Type();
	this.eventInfo = variant.getEvent_Info();
        this.ncbiBuild = "GRCh37"; // default, not provided by CVR
        this.normalReadCount = variant.getNormal_Read_Count();
        this.normalVariantCount = variant.getNormal_Variant_Count();
        this.site1Chromosome = variant.getSite1_Chrom();
        this.site1Description = variant.getSite1_Desc();
        this.site1Position = variant.getSite1_Pos();
        this.site2Chromosome = variant.getSite2_Chrom();
        this.site2Description = variant.getSite2_Desc();
        this.site2Position = variant.getSite2_Pos();
        this.svClass = variant.getSv_Class_Name();
        this.svLength = variant.getSv_Length();
        this.tumorReadCount = variant.getTumor_Read_Count();
        this.tumorVariantCount = variant.getTumor_Variant_Count();
        this.svStatus = "SOMATIC"; // default, not provided by CVR
       
	// cover cases where event info is blank (this is the logic used to set the Fusion column in now deprecated data_fusion file) 
	if (variant.getEvent_Info().equals("-")) {
		String site1GeneTrimmed = variant.getSite1_Gene().trim();
		String site2GeneTrimmed = variant.getSite2_Gene().trim();
		this.eventInfo = site1GeneTrimmed.equals(site2GeneTrimmed) ? site1GeneTrimmed + "-intragenic" : site2GeneTrimmed + "-" + site1GeneTrimmed + " fusion";
	}
    }

    public CVRSvRecord(GMLCnvIntragenicVariant variant, String sampleId) {
        this.sampleId = sampleId;
        this.comments = (!Strings.isNullOrEmpty(variant.getInterpretation())) ? variant.getInterpretation().replaceAll("[\\t\\n\\r]+"," ") : "";
        // set event_info and sv_class
        this.site1HugoSymbol = variant.getGeneId().trim();
        this.site1Chromosome = variant.getChromosome();
        this.svStatus = "GERMLINE";
        if (!Strings.isNullOrEmpty(variant.getCnvClassName())) {
            String svClass = variant.getCnvClassName();
            String eventInfo = variant.getCnvClassName().trim().replace("INTRAGENIC_", "");
            this.eventInfo += " " + eventInfo.toLowerCase();
            this.svClass = variant.getCnvClassName();
        }
    }

    public String getSampleId(){
        return sampleId != null ? this.sampleId : "";
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getSvStatus() {
        return svStatus != null ? this.svStatus : "";
    }

    public void setSvStatus(String svStatus) {
        this.svStatus = svStatus;
    }

    public String getSite1HugoSymbol() {
        return site1HugoSymbol != null ? this.site1HugoSymbol : "";
    }

    public void setSite1HugoSymbol(String site1HugoSymbol) {
        this.site1HugoSymbol = site1HugoSymbol;
    }

    public String getSite2HugoSymbol() {
        return site2HugoSymbol != null ? this.site2HugoSymbol : "";
    }

    public void setSite2HugoSymbol(String site2HugoSymbol) {
        this.site2HugoSymbol = site2HugoSymbol;
    }

    public String getSite1EnsemblTranscriptId() {
        return site1EnsemblTranscriptId != null ? this.site1EnsemblTranscriptId : "";
    }

    public void setSite1EnsemblTranscriptId(String site1EnsemblTranscriptId) {
        this.site1EnsemblTranscriptId = site1EnsemblTranscriptId;
    }

    public String getSite2EnsemblTranscriptId() {
        return site2EnsemblTranscriptId != null ? this.site2EnsemblTranscriptId : "";
    }

    public void setSite2EnsemblTranscriptId(String site2EnsemblTranscriptId) {
        this.site2EnsemblTranscriptId = site2EnsemblTranscriptId;
    }

    public String getSite1EntrezGeneId() {
        return site1EntrezGeneId != null ? this.site1EntrezGeneId : "";
    }

    public void setSite1EntrezGeneId(String site1EntrezGeneId) {
        this.site1EntrezGeneId = site1EntrezGeneId;
    }

    public String getSite2EntrezGeneId() {
        return site2EntrezGeneId != null ? this.site2EntrezGeneId : "";
    }

    public void setSite2EntrezGeneId(String site2EntrezGeneId) {
        this.site2EntrezGeneId = site2EntrezGeneId;
    }

    public String getSite1RegionNumber() {
        return site1RegionNumber != null ? this.site1RegionNumber : "";
    }

    public void setSite1RegionNumber(String site1RegionNumber) {
        this.site1RegionNumber = site1RegionNumber;
    }

    public String getSite2RegionNumber() {
        return site2RegionNumber != null ? this.site2RegionNumber : "";
    }

    public void setSite2RegionNumber(String site2RegionNumber) {
        this.site2RegionNumber = site2RegionNumber;
    }

    public String getSite1Region() {
        return site1Region != null ? this.site1Region : "";
    }

    public void setSite1Region(String site1Region) {
        this.site1Region = site1Region;
    }

    public String getSite2Region() {
        return site2Region != null ? this.site2Region : "";
    }

    public void setSite2Region(String site2Region) {
        this.site2Region = site2Region;
    }

    public String getSite1Chromosome() {
        return site1Chromosome != null ? this.site1Chromosome : "";
    }

    public void setSite1Chromosome(String site1Chromosome) {
        this.site1Chromosome = site1Chromosome;
    }

    public String getSite2Chromosome() {
        return site2Chromosome != null ? this.site2Chromosome : "";
    }

    public void setSite2Chromosome(String site2Chromosome) {
        this.site2Chromosome = site2Chromosome;
    }

    public String getSite1Contig() {
        return site1Contig != null ? this.site1Contig : "";
    }

    public void setSite1Contig(String site1Contig) {
        this.site1Contig = site1Contig;
    }

    public String getSite2Contig() {
        return site2Contig != null ? this.site2Contig : "";
    }

    public void setSite2Contig(String site2Contig) {
        this.site2Contig = site2Contig;
    }

    public String getSite1Position() {
        return site1Position != null ? this.site1Position : "";
    }

    public void setSite1Position(String site1Position) {
        this.site1Position = site1Position;
    }

    public String getSite2Position() {
        return site2Position != null ? this.site2Position : "";
    }

    public void setSite2Position(String site2Position) {
        this.site2Position = site2Position;
    }

    public String getSite1Description() {
        return site1Description != null ? this.site1Description : "";
    }

    public void setSite1Description(String site1Description) {
        this.site1Description = site1Description;
    }

    public String getSite2Description() {
        return site2Description != null ? this.site2Description : "";
    }

    public void setSite2Description(String site2Description) {
        this.site2Description = site2Description;
    }

    public String getSite2EffectOnFrame() {
        return site2EffectOnFrame != null ? this.site2EffectOnFrame : "";
    }

    public void setSite2EffectOnFrame(String site2EffectOnFrame) {
        this.site2EffectOnFrame = site2EffectOnFrame;
    }

    public String getNcbiBuild() {
        return ncbiBuild != null ? this.ncbiBuild : "";
    }

    public void setNcbiBuild(String ncbiBuild) {
        this.ncbiBuild = ncbiBuild;
    }

    public String getSvClass() {
        return svClass != null ? this.svClass : "";
    }

    public void setSvClass(String svClass) {
        this.svClass = svClass;
    }

    public String getTumorSplitReadCount() {
        return tumorSplitReadCount != null ? this.tumorSplitReadCount : "";
    }

    public void setTumorSplitReadCount(String tumorSplitReadCount) {
        this.tumorSplitReadCount = tumorSplitReadCount;
    }

    public String getTumorPairedEndReadCount() {
        return tumorPairedEndReadCount != null ? this.tumorPairedEndReadCount : "";
    }

    public void setTumorPairedEndReadCount(String tumorPairedEndReadCount) {
        this.tumorPairedEndReadCount = tumorPairedEndReadCount;
    }

    public String getEventInfo() {
        return eventInfo != null ? this.eventInfo : "";
    }

    public void setEventInfo(String eventInfo) {
        this.eventInfo = eventInfo;
    }

    public String getBreakpointType() {
        return breakpointType != null ? this.breakpointType : "";
    }

    public void setBreakpointType(String breakpointType) {
        this.breakpointType = breakpointType;
    }

    public String getConnectionType() {
        return connectionType != null ? this.connectionType : "";
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getAnnotation() {
        return annotation != null ? this.annotation : "";
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public String getDnaSupport() {
        return dnaSupport != null ? this.dnaSupport : "";
    }

    public void setDnaSupport(String dnaSupport) {
        this.dnaSupport = dnaSupport;
    }

    public String getRnaSupport() {
        return rnaSupport != null ? this.rnaSupport : "";
    }

    public void setRnaSupport(String rnaSupport) {
        this.rnaSupport = rnaSupport;
    }

    public String getSvLength() {
        return svLength != null ? this.svLength : "";
    }

    public void setSvLength(String svLength) {
        this.svLength = svLength;
    }

    public String getNormalReadCount() {
        return normalReadCount != null ? this.normalReadCount : "";
    }

    public void setNormalReadCount(String normalReadCount) {
        this.normalReadCount = normalReadCount;
    }

    public String getTumorReadCount() {
        return tumorReadCount != null ? this.tumorReadCount : "";
    }

    public void setTumorReadCount(String tumorReadCount) {
        this.tumorReadCount = tumorReadCount;
    }

    public String getNormalVariantCount() {
        return normalVariantCount != null ? this.normalVariantCount : "";
    }

    public void setNormalVariantCount(String normalVariantCount) {
        this.normalVariantCount = normalVariantCount;
    }

    public String getTumorVariantCount() {
        return tumorVariantCount != null ? this.tumorVariantCount : "";
    }

    public void setTumorVariantCount(String tumorVariantCount) {
        this.tumorVariantCount = tumorVariantCount;
    }

    public String getNormalPairedEndReadCount() {
        return normalPairedEndReadCount != null ? this.normalPairedEndReadCount : "";
    }

    public void setNormalPairedEndReadCount(String normalPairedEndReadCount) {
        this.normalPairedEndReadCount = normalPairedEndReadCount;
    }

    public String getNormalSplitEndReadCount() {
        return normalSplitEndReadCount != null ? this.normalSplitEndReadCount : "";
    }

    public void setNormalSplitEndReadCount(String normalSplitEndReadCount) {
        this.normalSplitEndReadCount = normalSplitEndReadCount;
    }

    public String getComments() {
        return comments != null ? this.comments : "";
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public static String getStandardSvHeader() {
        List<String> standardSvHeader = new ArrayList<String>();
        for (String fieldName : getFieldNames()) {
            if (fieldName.equals("SVClass")) {
                standardSvHeader.add("Class");
            } else if (fieldName.equals("NcbiBuild")) {
                standardSvHeader.add("NCBIBuild");
            } else {
                standardSvHeader.add(fieldName);
            }
        }
        return StringUtils.join(standardSvHeader, "\t");
    }

    public static List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<String>();
        fieldNames.add("SampleId");
        fieldNames.add("SvStatus");
        fieldNames.add("Site1HugoSymbol");
        fieldNames.add("Site2HugoSymbol");
        fieldNames.add("Site1EnsemblTranscriptId");
        fieldNames.add("Site2EnsemblTranscriptId");
        fieldNames.add("Site1EntrezGeneId");
        fieldNames.add("Site2EntrezGeneId");
        fieldNames.add("Site1RegionNumber");
        fieldNames.add("Site2RegionNumber");
        fieldNames.add("Site1Region");
        fieldNames.add("Site2Region");
        fieldNames.add("Site1Chromosome");
        fieldNames.add("Site2Chromosome");
        fieldNames.add("Site1Contig");
        fieldNames.add("Site2Contig");
        fieldNames.add("Site1Position");
        fieldNames.add("Site2Position");
        fieldNames.add("Site1Description");
        fieldNames.add("Site2Description");
        fieldNames.add("Site2EffectOnFrame");
        fieldNames.add("NcbiBuild");
        fieldNames.add("SvClass");
        fieldNames.add("TumorSplitReadCount");
        fieldNames.add("TumorPairedEndReadCount");
        fieldNames.add("EventInfo");
        fieldNames.add("BreakpointType");
        fieldNames.add("ConnectionType");
        fieldNames.add("Annotation");
        fieldNames.add("DnaSupport");
        fieldNames.add("RnaSupport");
        fieldNames.add("SvLength");
        fieldNames.add("NormalReadCount");
        fieldNames.add("TumorReadCount");
        fieldNames.add("NormalVariantCount");
        fieldNames.add("TumorVariantCount");
        fieldNames.add("NormalPairedEndReadCount");
        fieldNames.add("NormalSplitEndReadCount");
        fieldNames.add("Comments");
        return fieldNames;
    }
}

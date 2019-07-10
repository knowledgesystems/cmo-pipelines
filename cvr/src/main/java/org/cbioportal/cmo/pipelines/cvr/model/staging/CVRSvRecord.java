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
import org.cbioportal.cmo.pipelines.cvr.model.CVRSvVariant;
import org.springframework.batch.core.configuration.annotation.StepScope;

/**
 *
 * @author heinsz
 */

@StepScope
public class CVRSvRecord {

    private String sampleId;
    private String annotation;
    private String breakpoint_type;
    private String comments;
    private String confidence_class;
    private String conn_type;
    private String connection_type;
    private String event_info;
    private String mapq;
    private String normal_read_count;
    private String normal_variant_count;
    private String paired_end_read_support;
    private String site1_chrom;
    private String site1_desc;
    private String site1_gene;
    private String site1_pos;
    private String site2_chrom;
    private String site2_desc;
    private String site2_gene;
    private String site2_pos;
    private String split_read_support;
    private String sv_class_name;
    private String sv_desc;
    private String sv_length;
    private String sv_variant_id;
    private String tumor_read_count;
    private String tumor_variant_count;
    private String variant_status_name;

    public CVRSvRecord() {
    }

    public CVRSvRecord(CVRSvVariant variant, String sampleId) {
        this.sampleId = sampleId;
        this.annotation = variant.getAnnotation();
        this.breakpoint_type = variant.getBreakpoint_Type();
        this.comments = variant.getComments();
        this.confidence_class = variant.getConfidence_Class();
        this.conn_type = variant.getConn_Type();
        this.connection_type = variant.getConnection_Type();
        this.event_info = variant.getEvent_Info();
        this.mapq = variant.getMapq();
        this.normal_read_count = variant.getNormal_Read_Count();
        this.normal_variant_count = variant.getNormal_Variant_Count();
        this.paired_end_read_support = variant.getPaired_End_Read_Support();
        this.site1_chrom = variant.getSite1_Chrom();
        this.site1_desc = variant.getSite1_Desc();
        this.site1_gene = variant.getSite1_Gene();
        this.site1_pos = variant.getSite1_Pos();
        this.site2_chrom = variant.getSite2_Chrom();
        this.site2_desc = variant.getSite2_Desc();
        this.site2_gene = variant.getSite2_Gene();
        this.site2_pos = variant.getSite2_Pos();
        this.split_read_support = variant.getSplit_Read_Support();
        this.sv_class_name = variant.getSv_Class_Name();
        this.sv_desc = variant.getSv_Desc();
        this.sv_length = variant.getSv_Length();
        this.sv_variant_id = variant.getSv_VariantId();
        this.tumor_read_count = variant.getTumor_Read_Count();
        this.tumor_variant_count = variant.getTumor_Variant_Count();
        this.variant_status_name = variant.getVariant_Status_Name();
    }
    public String getSampleId(){
        return sampleId != null ? this.sampleId : "";
    }

    public void setSampleId(String sampleId){
        this.sampleId = sampleId;
    }

    public String getAnnotation(){
        return annotation != null ? this.annotation : "";
    }

    public void setAnnotation(String annotation){
        this.annotation = annotation;
    }

    public String getBreakpoint_Type(){
        return this.breakpoint_type != null ? this.breakpoint_type : "";
    }

    public void setBreakpoint_Type(String breakpoint_type){
        this.breakpoint_type = breakpoint_type;
    }

    public String getComments(){
        return this.comments != null ? this.comments : "";
    }

    public void setComments(String comments){
        this.comments = comments;
    }

    public String getConfidence_Class(){
        return this.confidence_class != null ? this.confidence_class : "";
    }

    public void setConfidence_Class(String confidence_class){
        this.confidence_class = confidence_class;
    }

    public String getConn_Type(){
        return this.conn_type != null ? this.conn_type : "";
    }

    public void setConn_Type(String conn_type){
        this.conn_type = conn_type;
    }

    public String getConnection_Type(){
        return this.connection_type != null ? this.connection_type : "";
    }

    public void setConnection_Type(String connection_type){
        this.connection_type = connection_type;
    }

    public String getEvent_Info(){
        return this.event_info != null ? this.event_info : "";
    }

    public void setEvent_Info(String event_info){
        this.event_info = event_info;
    }

    public String getMapq(){
        return this.mapq != null ? this.mapq : "";
    }

    public void setMapq(String mapq){
        this.mapq = mapq;
    }

    public String getNormal_Read_Count(){
        return this.normal_read_count != null ? this.normal_read_count : "";
    }

    public void setNormal_Read_Count(String normal_read_count){
        this.normal_read_count = normal_read_count;
    }

    public String getNormal_Variant_Count(){
        return this.normal_variant_count != null ? normal_variant_count : "";
    }

    public void setNormal_Variant_Count(String normal_variant_count){
        this.normal_variant_count = normal_variant_count;
    }

    public String getPaired_End_Read_Support(){
        return this.paired_end_read_support != null ? this.paired_end_read_support : "";
    }

    public void setPaired_End_Read_Support(String pairedEndReadSupport){
        this.paired_end_read_support = pairedEndReadSupport;
    }

    public String getSite1_Chrom(){
        return this.site1_chrom != null ? this.site1_chrom : "";
    }

    public void setSite1_Chrom(String site1Chrom){
        this.site1_chrom = site1Chrom;
    }

    public String getSite1_Desc(){
        return this.site1_desc != null ? this.site1_desc : "";
    }

    public void setSite1_Desc(String site1Desc){
        this.site1_desc = site1Desc;
    }

    public String getSite1_Gene(){
        return this.site1_gene != null ? this.site1_gene : "";
    }

    public void setSite1_Gene(String site1Gene){
        this.site1_gene = site1Gene;
    }

    public String getSite1_Pos(){
        return this.site1_pos != null ? this.site1_pos : "";
    }

    public void setSite1_Pos(String site1Pos){
        this.site1_pos = site1Pos;
    }

    public String getSite2_Chrom(){
        return this.site2_chrom != null ? this.site2_chrom : "";
    }

    public void setSite2_Chrom(String site2Chrom){
        this.site2_chrom = site2Chrom;
    }

    public String getSite2_Desc(){
        return this.site2_desc != null ? this.site2_desc : "";
    }

    public void setSite2_Desc(String site2Desc){
        this.site2_desc = site2Desc;
    }

    public String getSite2_Gene(){
        return this.site2_gene != null ? this.site2_gene : "";
    }

    public void setSite2_Gene(String site2Gene){
        this.site2_gene = site2Gene;
    }

    public String getSite2_Pos(){
        return this.site2_pos != null ? this.site2_pos : "";
    }

    public void setSite2_Pos(String site2Pos){
        this.site2_pos = site2Pos;
    }

    public String getSplit_Read_Support(){
        return this.split_read_support != null ? this.split_read_support : "";
    }

    public void setSplit_Read_Support(String splitReadSupport){
        this.split_read_support = splitReadSupport;
    }

    public String getSv_Class_Name(){
        return this.sv_class_name != null ? this.sv_class_name : "";
    }

    public void setSv_Class_Name(String svClassName){
        this.sv_class_name = svClassName;
    }

    public String getSv_Desc(){
        return this.sv_desc != null ? this.sv_desc : "";
    }

    public void setSv_Desc(String svDesc){
        this.sv_desc = svDesc;
    }

    public String getSv_Length(){
        return this.sv_length != null ? this.sv_length : "";
    }

    public void setSv_Length(String svLength){
        this.sv_length = svLength;
    }

    public String getSv_VariantId(){
        return this.sv_variant_id != null ? this.sv_variant_id : "";
    }

    public void setSv_VariantId(String svVariantId){
        this.sv_variant_id = svVariantId;
    }

    public String getTumor_Read_Count(){
        return this.tumor_read_count != null ? this.tumor_read_count : "";
    }

    public void setTumor_Read_Count(String tumorReadCount){
        this.tumor_read_count = tumorReadCount;
    }

    public String getTumor_Variant_Count(){
        return this.tumor_variant_count != null ? this.tumor_variant_count : "";
    }

    public void setTumor_Variant_Count(String tumorVariantCount){
        this.tumor_variant_count = tumorVariantCount;
    }

    public String getVariant_Status_Name(){
        return variant_status_name != null ? this.variant_status_name : "";
    }

    public void setVariant_Status_Name(String variantStatusName){
        this.variant_status_name = variantStatusName;
    }
    
    public static List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<String>();
        fieldNames.add("SampleId");
        fieldNames.add("Annotation");
        fieldNames.add("Breakpoint_Type");
        fieldNames.add("Comments");
        fieldNames.add("Confidence_Class");
        fieldNames.add("Conn_Type");
        fieldNames.add("Connection_Type");
        fieldNames.add("Event_Info");
        fieldNames.add("Mapq");
        fieldNames.add("Normal_Read_Count");
        fieldNames.add("Normal_Variant_Count");
        fieldNames.add("Paired_End_Read_Support");
        fieldNames.add("Site1_Chrom");
        fieldNames.add("Site1_Desc");
        fieldNames.add("Site1_Gene");
        fieldNames.add("Site1_Pos");
        fieldNames.add("Site2_Chrom");
        fieldNames.add("Site2_Desc");
        fieldNames.add("Site2_Gene");
        fieldNames.add("Site2_Pos");
        fieldNames.add("Split_Read_Support");
        fieldNames.add("Sv_Class_Name");
        fieldNames.add("Sv_Desc");
        fieldNames.add("Sv_Length");
        fieldNames.add("Sv_VariantId");
        fieldNames.add("Tumor_Read_Count");
        fieldNames.add("Tumor_Variant_Count");
        fieldNames.add("Variant_Status_Name");
        return fieldNames;
    }
}

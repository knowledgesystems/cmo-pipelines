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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author heinsz
 */
public class Skcm_mskcc_2015_chantClinicalRecord {
    
    private String patientId;
    private String sampleId;
    private String melspcPtid;
    private String  melspcStageYear;
    private String melspcStgGrpName;
    private String melgPtid;
    private String melgStsDesc;
    private String melgStsSrcDesc;
    private String melgActvStsDesc;
    private String melgDermagrphxDesc;
    private String melgPresStgYear;
    private String melgFamilyHxDesc;
    private String melg1stRecurYear;
    private String melgLocalDesc;
    private String melgNodalDesc;
    private String melgIntransitDesc;
    private String melgSysDesc;
    private String melgRecurNdszDesc;
    private String melgRecurNodalNo;
    private String melgLdh;
    private String melgLdhYear;
    private String melgMetsDesc;
    private String melgAdjvntTxDesc;
    private String melgSysTxDesc;
    private String melgRadTxDesc;
    private String melgSurgDesc;
    private String melgTissueBankAvail;
    private String melpPtid;
    private String melpPrimSeq;
    private String melpDxYear;
    private String melpMskReviewDesc;
    private String melpThicknessMm;
    private String melpClarkLvlDesc;
    private String melpUlcerationDesc;
    private String melpSiteDesc;
    private String melpSubSiteDesc;
    private String melpTilsDesc;
    private String melpRegressionDesc;
    private String melpMarginsDesc;
    private String melpMitidxUnkDesc;
    private String melpHistTypeDesc;
    private String melpSatellitesDesc;
    private String melpExtSlidesDesc;
    private String melpLnorgDxDesc;
    private String melpLnclinStsDesc;
    private String melpLnsentinbxDesx;
    private String melpLnsentinbxYear;
    private String melpLnprolysctDesc;
    private String melpLnprosuccDesc;
    private String melpLndsctCmpDesc;
    private String melpLndsctYear;
    private String melpLnmattedDesc;
    private String LnextnodstDesc;
    private String LnintrmetsDesc;
    private String melpLnsize;
    private String melpLnsizeUnkDesc;
    private String melpLnslnlargSize;
    private String melpLnihcDesc;
    private String melpLnimmS100Desc;
    private String melpLnimmhmb45Desc;
    private String melpLnimmMelaDesc;
    private String meliPtid;
    private String meliDmpPatientId;
    private String meliDmpSampleId;
    private String meliReportYear;
    private String meliProcedureYear;
    private String meliTumorType;
    private String meliPrimarySite;
    private String meliMetSite;
    private String melmsPtid;
    private String melmsSiteTypeDesc;
    private String melmsSiteDesc;
    private String melmsSiteYear;
    
    
    private Map<String, Object> additionalProperties;    
    
    public Skcm_mskcc_2015_chantClinicalRecord() {}
    
    public Skcm_mskcc_2015_chantClinicalRecord(String melspcPtid,
            String  melspcStageYear,
            String melspcStgGrpName,
            String melgPtid,
            String melgStsDesc,
            String melgStsSrcDesc,
            String melgActvStsDesc,
            String melgDermagrphxDesc,
            String melgPresStgYear,
            String melgFamilyHxDesc,
            String melg1stRecurYear,
            String melgLocalDesc,
            String melgNodalDesc,
            String melgIntransitDesc,
            String melgSysDesc,
            String melgRecurNdszDesc,
            String melgRecurNodalNo,
            String melgLdh,
            String melgLdhYear,
            String melgMetsDesc,
            String melgAdjvntTxDesc,
            String melgSysTxDesc,
            String melgRadTxDesc,
            String melgSurgDesc,
            String melgTissueBankAvail,
            String melpPtid,
            String melpPrimSeq,
            String melpDxYear,
            String melpMskReviewDesc,
            String melpThicknessMm,
            String melpClarkLvlDesc,
            String melpUlcerationDesc,
            String melpSiteDesc,
            String melpSubSiteDesc,
            String melpTilsDesc,
            String melpRegressionDesc,
            String melpMarginsDesc,
            String melpMitidxUnkDesc,
            String melpHistTypeDesc,
            String melpSatellitesDesc,
            String melpExtSlidesDesc,
            String melpLnorgDxDesc,
            String melpLnclinStsDesc,
            String melpLnsentinbxDesx,
            String melpLnsentinbxYear,
            String melpLnprolysctDesc,
            String melpLnprosuccDesc,
            String melpLndsctCmpDesc,
            String melpLndsctYear,
            String melpLnmattedDesc,
            String LnextnodstDesc,
            String LnintrmetsDesc,
            String melpLnsize,
            String melpLnsizeUnkDesc,
            String melpLnslnlargSize,
            String melpLnihcDesc,
            String melpLnimmS100Desc,
            String melpLnimmhmb45Desc,
            String melpLnimmMelaDesc,
            String meliPtid,
            String meliDmpPatientId,
            String meliDmpSampleId,
            String meliReportYear,
            String meliProcedureYear,
            String meliTumorType,
            String meliPrimarySite,
            String meliMetSite,
            String melmsPtid,
            String melmsSiteTypeDesc,
            String melmsSiteDesc,
            String melmsSiteYear) {
this.melspcPtid = StringUtils.isNotEmpty(melspcPtid) ? melspcPtid : "NA";
this.melspcStageYear = StringUtils.isNotEmpty(melspcStageYear) ? melspcStageYear : "NA";
this.melspcStgGrpName = StringUtils.isNotEmpty(melspcStgGrpName) ? melspcStgGrpName : "NA";
this.melgPtid = StringUtils.isNotEmpty(melgPtid) ? melgPtid : "NA";
this.melgStsDesc = StringUtils.isNotEmpty(melgStsDesc) ? melgStsDesc : "NA";
this.melgStsSrcDesc = StringUtils.isNotEmpty(melgStsSrcDesc) ? melgStsSrcDesc : "NA";
this.melgActvStsDesc = StringUtils.isNotEmpty(melgActvStsDesc) ? melgActvStsDesc : "NA";
this.melgDermagrphxDesc = StringUtils.isNotEmpty(melgDermagrphxDesc) ? melgDermagrphxDesc : "NA";
this.melgPresStgYear = StringUtils.isNotEmpty(melgPresStgYear) ? melgPresStgYear : "NA";
this.melgFamilyHxDesc = StringUtils.isNotEmpty(melgFamilyHxDesc) ? melgFamilyHxDesc : "NA";
this.melg1stRecurYear = StringUtils.isNotEmpty(melg1stRecurYear) ? melg1stRecurYear : "NA";
this.melgLocalDesc = StringUtils.isNotEmpty(melgLocalDesc) ? melgLocalDesc : "NA";
this.melgNodalDesc = StringUtils.isNotEmpty(melgNodalDesc) ? melgNodalDesc : "NA";
this.melgIntransitDesc = StringUtils.isNotEmpty(melgIntransitDesc) ? melgIntransitDesc : "NA";
this.melgSysDesc = StringUtils.isNotEmpty(melgSysDesc) ? melgSysDesc : "NA";
this.melgRecurNdszDesc = StringUtils.isNotEmpty(melgRecurNdszDesc) ? melgRecurNdszDesc : "NA";
this.melgRecurNodalNo = StringUtils.isNotEmpty(melgRecurNodalNo) ? melgRecurNodalNo : "NA";
this.melgLdh = StringUtils.isNotEmpty(melgLdh) ? melgLdh : "NA";
this.melgLdhYear = StringUtils.isNotEmpty(melgLdhYear) ? melgLdhYear : "NA";
this.melgMetsDesc = StringUtils.isNotEmpty(melgMetsDesc) ? melgMetsDesc : "NA";
this.melgAdjvntTxDesc = StringUtils.isNotEmpty(melgAdjvntTxDesc) ? melgAdjvntTxDesc : "NA";
this.melgSysTxDesc = StringUtils.isNotEmpty(melgSysTxDesc) ? melgSysTxDesc : "NA";
this.melgRadTxDesc = StringUtils.isNotEmpty(melgRadTxDesc) ? melgRadTxDesc : "NA";
this.melgSurgDesc = StringUtils.isNotEmpty(melgSurgDesc) ? melgSurgDesc : "NA";
this.melgTissueBankAvail = StringUtils.isNotEmpty(melgTissueBankAvail) ? melgTissueBankAvail : "NA";
this.melpPtid = StringUtils.isNotEmpty(melpPtid) ? melpPtid : "NA";
this.melpPrimSeq = StringUtils.isNotEmpty(melpPrimSeq) ? melpPrimSeq : "NA";
this.melpDxYear = StringUtils.isNotEmpty(melpDxYear) ? melpDxYear : "NA";
this.melpMskReviewDesc = StringUtils.isNotEmpty(melpMskReviewDesc) ? melpMskReviewDesc : "NA";
this.melpThicknessMm = StringUtils.isNotEmpty(melpThicknessMm) ? melpThicknessMm : "NA";
this.melpClarkLvlDesc = StringUtils.isNotEmpty(melpClarkLvlDesc) ? melpClarkLvlDesc : "NA";
this.melpUlcerationDesc = StringUtils.isNotEmpty(melpUlcerationDesc) ? melpUlcerationDesc : "NA";
this.melpSiteDesc = StringUtils.isNotEmpty(melpSiteDesc) ? melpSiteDesc : "NA";
this.melpSubSiteDesc = StringUtils.isNotEmpty(melpSubSiteDesc) ? melpSubSiteDesc : "NA";
this.melpTilsDesc = StringUtils.isNotEmpty(melpTilsDesc) ? melpTilsDesc : "NA";
this.melpRegressionDesc = StringUtils.isNotEmpty(melpRegressionDesc) ? melpRegressionDesc : "NA";
this.melpMarginsDesc = StringUtils.isNotEmpty(melpMarginsDesc) ? melpMarginsDesc : "NA";
this.melpMitidxUnkDesc = StringUtils.isNotEmpty(melpMitidxUnkDesc) ? melpMitidxUnkDesc : "NA";
this.melpHistTypeDesc = StringUtils.isNotEmpty(melpHistTypeDesc) ? melpHistTypeDesc : "NA";
this.melpSatellitesDesc = StringUtils.isNotEmpty(melpSatellitesDesc) ? melpSatellitesDesc : "NA";
this.melpExtSlidesDesc = StringUtils.isNotEmpty(melpExtSlidesDesc) ? melpExtSlidesDesc : "NA";
this.melpLnorgDxDesc = StringUtils.isNotEmpty(melpLnorgDxDesc) ? melpLnorgDxDesc : "NA";
this.melpLnclinStsDesc = StringUtils.isNotEmpty(melpLnclinStsDesc) ? melpLnclinStsDesc : "NA";
this.melpLnsentinbxDesx = StringUtils.isNotEmpty(melpLnsentinbxDesx) ? melpLnsentinbxDesx : "NA";
this.melpLnsentinbxYear = StringUtils.isNotEmpty(melpLnsentinbxYear) ? melpLnsentinbxYear : "NA";
this.melpLnprolysctDesc = StringUtils.isNotEmpty(melpLnprolysctDesc) ? melpLnprolysctDesc : "NA";
this.melpLnprosuccDesc = StringUtils.isNotEmpty(melpLnprosuccDesc) ? melpLnprosuccDesc : "NA";
this.melpLndsctCmpDesc = StringUtils.isNotEmpty(melpLndsctCmpDesc) ? melpLndsctCmpDesc : "NA";
this.melpLndsctYear = StringUtils.isNotEmpty(melpLndsctYear) ? melpLndsctYear : "NA";
this.melpLnmattedDesc = StringUtils.isNotEmpty(melpLnmattedDesc) ? melpLnmattedDesc : "NA";
this.LnextnodstDesc = StringUtils.isNotEmpty(LnextnodstDesc) ? LnextnodstDesc : "NA";
this.LnintrmetsDesc = StringUtils.isNotEmpty(LnintrmetsDesc) ? LnintrmetsDesc : "NA";
this.melpLnsize = StringUtils.isNotEmpty(melpLnsize) ? melpLnsize : "NA";
this.melpLnsizeUnkDesc = StringUtils.isNotEmpty(melpLnsizeUnkDesc) ? melpLnsizeUnkDesc : "NA";
this.melpLnslnlargSize = StringUtils.isNotEmpty(melpLnslnlargSize) ? melpLnslnlargSize : "NA";
this.melpLnihcDesc = StringUtils.isNotEmpty(melpLnihcDesc) ? melpLnihcDesc : "NA";
this.melpLnimmS100Desc = StringUtils.isNotEmpty(melpLnimmS100Desc) ? melpLnimmS100Desc : "NA";
this.melpLnimmhmb45Desc = StringUtils.isNotEmpty(melpLnimmhmb45Desc) ? melpLnimmhmb45Desc : "NA";
this.melpLnimmMelaDesc = StringUtils.isNotEmpty(melpLnimmMelaDesc) ? melpLnimmMelaDesc : "NA";
this.meliPtid = StringUtils.isNotEmpty(meliPtid) ? meliPtid : "NA";
this.meliDmpPatientId = StringUtils.isNotEmpty(meliDmpPatientId) ? meliDmpPatientId : "NA";
this.meliDmpSampleId = StringUtils.isNotEmpty(meliDmpSampleId) ? meliDmpSampleId : "NA";
this.meliReportYear = StringUtils.isNotEmpty(meliReportYear)  ? meliReportYear : "NA";
this.meliProcedureYear = StringUtils.isNotEmpty(meliProcedureYear) ? meliProcedureYear : "NA";
this.meliTumorType = StringUtils.isNotEmpty(meliTumorType) ? meliTumorType : "NA";
this.meliPrimarySite = StringUtils.isNotEmpty(meliPrimarySite) ? meliPrimarySite : "NA";
this.meliMetSite = StringUtils.isNotEmpty(meliMetSite) ? meliMetSite : "NA";
this.melmsPtid = StringUtils.isNotEmpty(melmsPtid) ? melmsPtid : "NA";
this.melmsSiteTypeDesc = StringUtils.isNotEmpty(melmsSiteTypeDesc) ? melmsSiteTypeDesc : "NA";
this.melmsSiteDesc = StringUtils.isNotEmpty(melmsSiteDesc) ? melmsSiteDesc : "NA";
this.melmsSiteYear = StringUtils.isNotEmpty(melmsSiteYear) ? melmsSiteYear : "NA";
    }
    
    public String getPATIENT_ID() {
        if (patientId != null) {
            return patientId;
        }
        else if (!melspcPtid.equals("NA")) {
            return melspcPtid;
        }
        else if (!melgPtid.equals("NA")) {
            return melgPtid;
        }        
        else if (!melpPtid.equals("NA")) {
            return melpPtid;
        }
        else {
            return melmsPtid;
        }
    }
    
    public void setPATIENT_ID(String patientId) {
        this.patientId = patientId;
    }
    
    public String getSAMPLE_ID() {
        if (sampleId != null) {
            return sampleId;
        }
        else if (!meliDmpSampleId.equals("NA")) {
            return meliDmpSampleId;
        }        
        else if (!melpPrimSeq.equals("NA")) {
            return melpPtid + "_" + melpPrimSeq;
        }        
        else if (!melspcPtid.equals("NA")) {
            return melspcPtid;
        }
        else if (!melgPtid.equals("NA")) {
            return melgPtid;
        }
        else {
            return melmsPtid;
        }
    }
    
    public void setSAMPLE_ID(String sampleId) {
        this.sampleId = sampleId;
    }
    
    public String getMELSPC_PTID() {
        return melspcPtid;
    }
    
    public void setMELSPC_PTID(String melspcPtid) {
        this.melspcPtid = melspcPtid;
    }    
    
    public String getMELSPC_STAGE_YEAR() {
        return melspcStageYear;
    }
    
    public void setMELSPC_STAGE_YEAR(String melspcStageYear) {
        this.melspcStageYear = melspcStageYear;
    }  
    
    public String getMELSPC_STG_GRP_NAME() {
        return melspcStgGrpName;
    }
    
    public void setMELSPC_STG_GRP_NAME(String melspcStgGrpName) {
        this.melspcStgGrpName = melspcStgGrpName;
    }      
    
    public String getMELG_PTID() {
        return melgPtid;
    }
    
    public void setMELG_PTID(String melgPtid) {
        this.melgPtid = melgPtid;
    }        
    
    public String getMELG_STS_DESC() {
        return melgStsDesc;
    }
    
    public void setMELG_STS_DESC(String melgStsDesc) {
        this.melgStsDesc = melgStsDesc;
    }      
    
    public String getMELG_STS_SRCE_DESC() {
        return melgStsSrcDesc;
    }
    
    public void setMELG_STS_SRCE_DESC(String melgStsSrcDesc) {
        this.melgStsSrcDesc = melgStsSrcDesc;
    }  

    public String getMELG_ACTV_STS_DESC() {
        return melgActvStsDesc;
    }
    
    public void setMELG_ACTV_STS_DESC(String melgActvStsDesc) {
        this.melgActvStsDesc = melgActvStsDesc;
    }  

    public String getMELG_DERMAGRPHX_DESC() {
        return melgDermagrphxDesc;
    }
    
    public void setMELG_DERMAGRPHX_DESC(String melgDermagrphxDesc) {
        this.melgDermagrphxDesc = melgDermagrphxDesc;
    }  

    public String getMELG_PRES_STG_YEAR() {
        return melgPresStgYear;
    }
    
    public void setMELG_PRES_STG_YEAR(String melgPresStgYear) {
        this.melgPresStgYear = melgPresStgYear;
    }  

    public String getMELG_FAMILY_HX_DESC() {
        return melgFamilyHxDesc;
    }
    
    public void setMELG_FAMILY_HX_DESC(String melgFamilyHxDesc) {
        this.melgFamilyHxDesc = melgFamilyHxDesc;
    }  

    public String getMELG_1ST_RECUR_YEAR() {
        return melg1stRecurYear;
    }
    
    public void setMELG_1ST_RECUR_YEAR(String melg1stRecurYear) {
        this.melg1stRecurYear = melg1stRecurYear;
    }  

    public String getMELG_LOCAL_DESC() {
        return melgLocalDesc;
    }
    
    public void setMELG_LOCAL_DESC(String melgLocalDesc) {
        this.melgLocalDesc = melgLocalDesc;
    }  

    public String getMELG_NODAL_DESC() {
        return melgNodalDesc;
    }
    
    public void setMELG_NODAL_DESC(String melgNodalDesc) {
        this.melgNodalDesc = melgNodalDesc;
    }  

    public String getMELG_INTRANSIT_DESC() {
        return melgIntransitDesc;
    }
    
    public void setMELG_INTRANSIT_DESC(String melgIntransitDesc) {
        this.melgIntransitDesc = melgIntransitDesc;
    }  

    public String getMELG_SYS_DESC() {
        return melgSysDesc;
    }
    
    public void setMELG_SYS_DESC(String melgSysDesc) {
        this.melgSysDesc = melgSysDesc;
    }  

    public String getMELG_RECUR_NDSZ_DESC() {
        return melgRecurNdszDesc;
    }
    
    public void setMELG_RECUR_NDSZ_DESC(String melgRecurNdszDesc) {
        this.melgRecurNdszDesc = melgRecurNdszDesc;
    }  

    public String getMELG_RECUR_NODAL_NO() {
        return melgRecurNodalNo;
    }
    
    public void setMELG_RECUR_NODAL_NO(String melgRecurNodalNo) {
        this.melgRecurNodalNo = melgRecurNodalNo;
    }  

    public String getMELG_LDH() {
        return melgLdh;
    }
    
    public void setMELG_LDH(String melgLdh) {
        this.melgLdh = melgLdh;
    }

    public String getMELG_LDH_YEAR() {
        return melgLdhYear;
    }
    
    public void setMELG_LDH_YEAR(String melgLdhYear) {
        this.melgLdhYear = melgLdhYear;
    }

    public String getMELG_METS_DESC() {
        return melgMetsDesc;
    }
    
    public void setMELG_METS_DESC(String melgMetsDesc) {
        this.melgMetsDesc = melgMetsDesc;
    }

    public String getMELG_ADJVNT_TX_DESC() {
        return melgAdjvntTxDesc;
    }
    
    public void setMELG_ADJVNT_TX_DESC(String melgAdjvntTxDesc) {
        this.melgAdjvntTxDesc = melgAdjvntTxDesc;
    }

    public String getMELG_SYS_TX_DESC() {
        return melgSysTxDesc;
    }
    
    public void setMELG_SYS_TX_DESC(String melgSysTxDesc) {
        this.melgSysTxDesc = melgSysTxDesc;
    }

    public String getMELG_RAD_TX_DESC() {
        return melgRadTxDesc;
    }
    
    public void setMELG_RAD_TX_DESC(String melgRadTxDesc) {
        this.melgRadTxDesc = melgRadTxDesc;
    }

    public String getMELG_SURG_DESC() {
        return melgSurgDesc;
    }
    
    public void setMELG_SURG_DESC(String melgSurgDesc) {
        this.melgSurgDesc = melgSurgDesc;
    }    
    
    public String getMELG_TISSUE_BANK_AVAIL() {
        return melgTissueBankAvail;
    }
    
    public void setMELG_TISSUE_BANK_AVAIL(String melgTissueBankAvail) {
        this.melgTissueBankAvail = melgTissueBankAvail;
    }
    
    public String getMELP_PTID() {
        return melpPtid;
    }
    
    public void setMELP_PTID(String melpPtid) {
        this.melpPtid = melpPtid;
    }        
    
    public String getMELP_PRIM_SEQ() {
        return melpPrimSeq;
    }
    
    public void setMELP_PRIM_SEQ(String melpPrimSeq) {
        this.melpPrimSeq = melpPrimSeq;
    }
    
    public String getMELP_DX_YEAR() {
        return melpDxYear;
    }
    
    public void setMELP_DX_YEAR(String melpDxYear) {
        this.melpDxYear = melpDxYear;
    }

    public String getMELP_MSK_REVIEW_DESC() {
        return melpMskReviewDesc;
    }
    
    public void setMELP_MSK_REVIEW_DESC(String melpMskReviewDesc) {
        this.melpMskReviewDesc = melpMskReviewDesc;
    }

    public String getMELP_THICKNESS_MM() {
        return melpThicknessMm;
    }
    
    public void setMELP_THICKNESS_MM(String melpThicknessMm) {
        this.melpThicknessMm = melpThicknessMm;
    }

    public String getMELP_CLARK_LVL_DESC() {
        return melpClarkLvlDesc;
    }
    
    public void setMELP_CLARK_LVL_DESC(String melpClarkLvlDesc) {
        this.melpClarkLvlDesc = melpClarkLvlDesc;
    }

    public String getMELP_ULCERATION_DESC() {
        return melpUlcerationDesc;
    }
    
    public void setMELP_ULCERATION_DESC(String melpUlcerationDesc) {
        this.melpUlcerationDesc = melpUlcerationDesc;
    }

    public String getMELP_SITE_DESC() {
        return melpSiteDesc;
    }
    
    public void setMELP_SITE_DESC(String melpSiteDesc) {
        this.melpSiteDesc = melpSiteDesc;
    }

    public String getMELP_SUB_SITE_DESC() {
        return melpSubSiteDesc;
    }
    
    public void setMELP_SUB_SITE_DESC(String melpSubSiteDesc) {
        this.melpSubSiteDesc = melpSubSiteDesc;
    }

    public String getMELP_TILS_DESC() {
        return melpTilsDesc;
    }
    
    public void setMELP_TILS_DESC(String melpTilsDesc) {
        this.melpTilsDesc = melpTilsDesc;
    }

    public String getMELP_REGRESSION_DESC() {
        return melpRegressionDesc;
    }
    
    public void setMELP_REGRESSION_DESC(String melpRegressionDesc) {
        this.melpRegressionDesc = melpRegressionDesc;
    }

    public String getMELP_MARGINS_DESC() {
        return melpMarginsDesc;
    }
    
    public void setMELP_MARGINS_DESC(String melpMarginsDesc) {
        this.melpMarginsDesc = melpMarginsDesc;
    }

    public String getMELP_MITIDX_UNK_DESC() {
        return melpMitidxUnkDesc;
    }
    
    public void setMELP_MITIDX_UNK_DESC(String melpMitidxUnkDesc) {
        this.melpMitidxUnkDesc = melpMitidxUnkDesc;
    }

    public String getMELP_HIST_TYPE_DESC() {
        return melpHistTypeDesc;
    }
    
    public void setMELP_HIST_TYPE_DESC(String melpHistTypeDesc) {
        this.melpHistTypeDesc = melpHistTypeDesc;
    }

    public String getMELP_SATELLITES_DESC() {
        return melpSatellitesDesc;
    }
    
    public void setMELP_SATELLITES_DESC(String melpSatellitesDesc) {
        this.melpSatellitesDesc = melpSatellitesDesc;
    }

    public String getMELP_EXT_SLIDES_DESC() {
        return melpExtSlidesDesc;
    }
    
    public void setMELP_EXT_SLIDES_DESC(String melpExtSlidesDesc) {
        this.melpExtSlidesDesc = melpExtSlidesDesc;
    }

    public String getMELP_LNORG_DX_DESC() {
        return melpLnorgDxDesc;
    }
    
    public void setMELP_LNORG_DX_DESC(String melpLnorgDxDesc) {
        this.melpLnorgDxDesc = melpLnorgDxDesc;
    }

    public String getMELP_LNCLIN_STS_DESC() {
        return melpLnclinStsDesc;
    }
    
    public void setMELP_LNCLIN_STS_DESC(String melpLnclinStsDesc) {
        this.melpLnclinStsDesc = melpLnclinStsDesc;
    }

    public String getMELP_LNSENTINBX_DESC() {
        return melpLnsentinbxDesx;
    }
    
    public void setMELP_LNSENTINBX_DESC(String melpLnsentinbxDesx) {
        this.melpLnsentinbxDesx = melpLnsentinbxDesx;
    }

    public String getMELP_LNSENTINBX_YEAR() {
        return melpLnsentinbxYear;
    }
    
    public void setMELP_LNSENTINBX_YEAR(String melpLnsentinbxYear) {
        this.melpLnsentinbxYear = melpLnsentinbxYear;
    }

    public String getMELP_LNPROLYSCT_DESC() {
        return melpLnprolysctDesc;
    }
    
    public void setMELP_LNPROLYSCT_DESC(String melpLnprolysctDesc) {
        this.melpLnprolysctDesc = melpLnprolysctDesc;
    }

    public String getMELP_LNPROSUCC_DESC() {
        return melpLnprosuccDesc;
    }
    
    public void setMELP_LNPROSUCC_DESC(String melpLnprosuccDesc) {
        this.melpLnprosuccDesc = melpLnprosuccDesc;
    }

    public String getMELP_LNDSCT_CMP_DESC() {
        return melpLndsctCmpDesc;
    }
    
    public void setMELP_LNDSCT_CMP_DESC(String melpLndsctCmpDesc) {
        this.melpLndsctCmpDesc = melpLndsctCmpDesc;
    }

    public String getMELP_LNDSCT_YEAR() {
        return melpLndsctYear;
    }
    
    public void setMELP_LNDSCT_YEAR(String melpLndsctYear) {
        this.melpLndsctYear = melpLndsctYear;
    }    
    
    public String getMELP_LNMATTED_DESC() {
        return melpLnmattedDesc;
    }
    
    public void setMELP_LNMATTED_DESC(String melpLnmattedDesc) {
        this.melpLnmattedDesc = melpLnmattedDesc;
    }

    public String getMELP_LNEXTNODST_DESC() {
        return LnextnodstDesc;
    }
    
    public void setMELP_LNEXTNODST_DESC(String LnextnodstDesc) {
        this.LnextnodstDesc = LnextnodstDesc;
    }

    public String getMELP_LNINTRMETS_DESC() {
        return LnintrmetsDesc;
    }
    
    public void setMELP_LNINTRMETS_DESC(String LnintrmetsDesc) {
        this.LnintrmetsDesc = LnintrmetsDesc;
    }

    public String getMELP_LNSIZE() {
        return melpLnsize;
    }
    
    public void setMELP_LNSIZE(String melpLnsize) {
        this.melpLnsize = melpLnsize;
    }

    public String getMELP_LNSIZE_UNK_DESC() {
        return melpLnsizeUnkDesc;
    }
    
    public void setMELP_LNSIZE_UNK_DESC(String melpLnsizeUnkDesc) {
        this.melpLnsizeUnkDesc = melpLnsizeUnkDesc;
    }

    public String getMELP_LNSLNLARG_SIZE() {
        return melpLnslnlargSize;
    }
    
    public void setMELP_LNSLNLARG_SIZE(String melpLnslnlargSize) {
        this.melpLnslnlargSize = melpLnslnlargSize;
    }

    public String getMELP_LNIHC_DESC() {
        return melpLnihcDesc;
    }
    
    public void setMELP_LNIHC_DESC(String melpLnihcDesc) {
        this.melpLnihcDesc = melpLnihcDesc;
    }   
    
    public String getMELP_LNIMM_S100_DESC() {
        return melpLnimmS100Desc;
    }
    
    public void setMELP_LNIMM_S100_DESC(String melpLnimmS100Desc) {
        this.melpLnimmS100Desc = melpLnimmS100Desc;
    }

    public String getMELP_LNIMMHMB45_DESC() {
        return melpLnimmhmb45Desc;
    }
    
    public void setMELP_LNIMMHMB45_DESC(String melpLnimmhmb45Desc) {
        this.melpLnimmhmb45Desc = melpLnimmhmb45Desc;
    }

    public String getMELP_LNIMM_MELA_DESC() {
        return melpLnimmMelaDesc;
    }
    
    public void setMELP_LNIMM_MELA_DESC(String melpLnimmMelaDesc) {
        this.melpLnimmMelaDesc = melpLnimmMelaDesc;
    }    
    
    public String getMELI_PTID() {
        return meliPtid;
    }
    
    public void setMELI_PTID(String meliPtid) {
        this.meliPtid = meliPtid;
    }  

    public String getMELI_DMP_PATIENT_ID() {
        return meliDmpPatientId;
    }
    
    public void setMELI_DMP_PATIENT_ID(String meliDmpPatientId) {
        this.meliDmpPatientId = meliDmpPatientId;
    }  

    public String getMELI_DMP_SAMPLE_ID() {
        return meliDmpSampleId;
    }
    
    public void setMELI_DMP_SAMPLE_ID(String meliDmpSampleId) {
        this.meliDmpSampleId = meliDmpSampleId;
    }   
    
    public String getMELI_REPORT_YEAR() {
        return meliReportYear;
    }
    
    public void setMELI_REPORT_YEAR(String meliReportYear) {
        this.meliReportYear = meliReportYear;
    }    

    public String getMELI_PROCEDURE_YEAR() {
        return meliProcedureYear;
    }
    
    public void setMELI_PROCEDURE_YEAR(String meliProcedureYear) {
        this.meliProcedureYear = meliProcedureYear;
    }        
    
    public String getMELI_TUMOR_TYPE() {
        return meliTumorType;
    }
    
    public void setMELI_TUMOR_TYPE(String meliTumorType) {
        this.meliTumorType = meliTumorType;
    }

    public String getMELI_PRIMARY_SITE() {
        return meliPrimarySite;
    }
    
    public void setMELI_PRIMARY_SITE(String meliPrimarySite) {
        this.meliPrimarySite = meliPrimarySite;
    }

    public String getMELI_MET_SITE() {
        return meliMetSite;
    }
    
    public void setMELI_MET_SITE(String meliMetSite) {
        this.meliMetSite = meliMetSite;
    }
    
    public String getMELMS_PTID() {
        return melmsPtid;
    }
    
    public void setMELMS_PTID(String melmsPtid) {
        this.melmsPtid = melmsPtid;
    }
    
    public String getMELMS_SITE_TYPE_DESC() {
        return melmsSiteTypeDesc;
    }
    
    public void setMELMS_SITE_TYPE_DESC(String melmsSiteTypeDesc) {
        this.melmsSiteTypeDesc = melmsSiteTypeDesc;
    }
    
    public String getMELMS_SITE_DESC() {
        return melmsSiteDesc;
    }
    
    public void setMELMS_SITE_DESC(String melmsSiteDesc) {
        this.melmsSiteDesc = melmsSiteDesc;
    }

    public String getMELMS_SITE_YEAR() {
        return melmsSiteYear;
    }
    
    public void setMELMS_SITE_YEAR(String melmsSiteYear) {
        this.melmsSiteYear = melmsSiteYear;
    }    
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("SAMPLE_ID");
        fieldNames.add("MELSPC_STAGE_YEAR");
        fieldNames.add("MELSPC_STG_GRP_NAME");
        fieldNames.add("MELG_STS_DESC");
        fieldNames.add("MELG_STS_SRCE_DESC");
        fieldNames.add("MELG_ACTV_STS_DESC");
        fieldNames.add("MELG_DERMAGRPHX_DESC");
        fieldNames.add("MELG_PRES_STG_YEAR");
        fieldNames.add("MELG_FAMILY_HX_DESC");
        fieldNames.add("MELG_1ST_RECUR_YEAR");
        fieldNames.add("MELG_LOCAL_DESC");
        fieldNames.add("MELG_NODAL_DESC");
        fieldNames.add("MELG_INTRANSIT_DESC");
        fieldNames.add("MELG_SYS_DESC");
        fieldNames.add("MELG_RECUR_NDSZ_DESC");
        fieldNames.add("MELG_RECUR_NODAL_NO");
        fieldNames.add("MELG_LDH");
        fieldNames.add("MELG_LDH_YEAR");
        fieldNames.add("MELG_METS_DESC");
        fieldNames.add("MELG_ADJVNT_TX_DESC");
        fieldNames.add("MELG_SYS_TX_DESC");
        fieldNames.add("MELG_RAD_TX_DESC");
        fieldNames.add("MELG_SURG_DESC");
        fieldNames.add("MELG_TISSUE_BANK_AVAIL");
        fieldNames.add("MELP_PRIM_SEQ");
        fieldNames.add("MELP_DX_YEAR");
        fieldNames.add("MELP_MSK_REVIEW_DESC");
        fieldNames.add("MELP_THICKNESS_MM");
        fieldNames.add("MELP_CLARK_LVL_DESC");
        fieldNames.add("MELP_ULCERATION_DESC");
        fieldNames.add("MELP_SITE_DESC");
        fieldNames.add("MELP_SUB_SITE_DESC");
        fieldNames.add("MELP_TILS_DESC");
        fieldNames.add("MELP_REGRESSION_DESC");
        fieldNames.add("MELP_MARGINS_DESC");
        fieldNames.add("MELP_MITIDX_UNK_DESC");
        fieldNames.add("MELP_HIST_TYPE_DESC");        
        fieldNames.add("MELP_SATELLITES_DESC");
        fieldNames.add("MELP_EXT_SLIDES_DESC");
        fieldNames.add("MELP_LNORG_DX_DESC");
        fieldNames.add("MELP_LNCLIN_STS_DESC");
        fieldNames.add("MELP_LNSENTINBX_DESC");
        fieldNames.add("MELP_LNSENTINBX_YEAR"); 
        fieldNames.add("MELP_LNPROLYSCT_DESC");
        fieldNames.add("MELP_LNPROSUCC_DESC");
        fieldNames.add("MELP_LNDSCT_CMP_DESC");
        fieldNames.add("MELP_LNDSCT_YEAR");
        fieldNames.add("MELP_LNMATTED_DESC");
        fieldNames.add("MELP_LNEXTNODST_DESC");
        fieldNames.add("MELP_LNINTRMETS_DESC");
        fieldNames.add("MELP_LNSIZE");
        fieldNames.add("MELP_LNSIZE_UNK_DESC");
        fieldNames.add("MELP_LNSLNLARG_SIZE");
        fieldNames.add("MELP_LNIHC_DESC");
        fieldNames.add("MELP_LNIMM_S100_DESC");
        fieldNames.add("MELP_LNIMMHMB45_DESC");
        fieldNames.add("MELP_LNIMM_MELA_DESC");
        fieldNames.add("MELI_REPORT_YEAR");
        fieldNames.add("MELI_PROCEDURE_YEAR");
        fieldNames.add("MELI_TUMOR_TYPE");
        fieldNames.add("MELI_PRIMARY_SITE");
        fieldNames.add("MELI_MET_SITE");
        fieldNames.add("MELMS_SITE_TYPE_DESC");
        fieldNames.add("MELMS_SITE_DESC");
        fieldNames.add("MELMS_SITE_YEAR");

        return fieldNames;
    }
    
    public List<String> getAllVariables() {
        List<String> fieldNames = getFieldNames();
        fieldNames.add("MELSPC_PTID");
        fieldNames.add("MELP_PTID");
        fieldNames.add("MELG_PTID");
        fieldNames.add("MELI_PTID");
        fieldNames.add("MELMS_PTID");
        
        return fieldNames;
    }
    
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }    
    
}

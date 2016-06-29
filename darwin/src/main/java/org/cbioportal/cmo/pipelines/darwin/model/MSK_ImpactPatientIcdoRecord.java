/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin.model;

import java.util.*;
import org.apache.commons.lang.builder.ToStringBuilder;
/**
 *
 * @author jake
 */
public class MSK_ImpactPatientIcdoRecord {
    private Integer TUMOR_YEAR;
    private String PT_ID_ICDO;
    private String DMP_ID_ICDO;
    private String TM_TUMOR_SEQ_DESC;
    private String TM_ACC_YEAR;
    private String TM_FIRST_MSK_YEAR;
    private String AGE_AT_TM_FIRST_MSK_DATE_IN_DAYS;
    private String TM_CASE_STS;
    private String TM_CASE_STS_DESC;
    private String TM_CASE_EFF_YEAR;
    private String AGE_AT_TM_CASE_EFF_DATE_IN_DAYS;
    private String TM_STATE_AT_DX;
    private String TM_SMOKING_HX;
    private String TM_SMOKING_HX_DESC;
    private String TM_OCCUPATION;
    private String TM_OCCUPATION_DESC;
    private String TM_FACILITY_FROM;
    private String FACILITY_FROM_DESC;
    private String TM_FACILITY_TO;
    private String FACILITY_TO_DESC;
    private String TM_DX_YEAR;
    private String AGE_AT_TM_DX_DATE_IN_DAYS;
    private String TM_SITE_CD;
    private String TM_SITE_DESC;
    private String TM_LATERALITY_CD;
    private String TM_LATERALITY_DESC;
    private String TM_HIST_CD;
    private String TM_HIST_DESC;
    private String TM_DX_CONFRM_CD;
    private String TM_DX_CONFRM_DESC;
    private String TM_REGNODE_EXM_NO;
    private String TM_REGNODE_POS_NO;
    private String TM_TUMOR_SIZE;
    private String TM_RESID_TUMOR_CD;
    private String TM_RESID_TUMOR_DESC;
    private String TM_GENERAL_STG;
    private String TM_GENERAL_STG_DESC;
    private String TM_TNM_EDITION;
    private String TM_TNM_EDITION_DESC;
    private String TM_CLIN_TNM_T;
    private String TM_CLIN_TNM_T_DESC;
    private String TM_CLIN_TNM_N;
    private String TM_CLIN_TNM_N_DESC;
    private String TM_CLIN_TNM_M;
    private String TM_CLIN_TNM_M_DESC;
    private String TM_CLIN_STG_GRP;
    private String TM_PATH_TNM_T;
    private String TM_PATH_TNM_T_DESC;
    private String TM_PATH_TNM_N;
    private String TM_PATH_TNM_N_DESC;
    private String TM_PATH_TNM_M;
    private String TM_PATH_TNM_M_DESC;
    private String TM_PATH_STG_GRP;
    private String TM_PATH_RPT_AV;
    private String TM_CA_STS_AT_ACC;
    private String TM_FIRST_RECUR_YEAR;
    private String AGE_AT_TM_FIRST_RECUR_DATE_IN_DAYS;
    private String TM_FIRST_RECUR_TYP;
    private String TM_ADM_YEAR;
    private String AGE_AT_TM_ADM_DATE_IN_DAYS;
    private String TM_DSCH_YEAR;
    private String AGE_AT_TM_DSCH_DATE_IN_DAYS;
    private String TM_SURG_DSCH_YEAR;
    private String AGE_AT_TM_SURG_DSCH_DATE_IN_DAYS;
    private String TM_READM_WTHN_30D;
    private String TM_FIRST_TX_YEAR;
    private String AGE_AT_TM_FIRST_TX_DATE_IN_DAYS;
    private String TM_NON_CA_SURG_SUM;
    private String TM_NON_CA_SURG_SUM_DESC;
    private String TM_NON_CA_SURG_MSK;
    private String TM_NON_CA_SURG_YEAR;
    private String AGE_AT_TM_NON_CA_SURG_DATE_IN_DAYS;
    private String TM_CA_SURG_98;
    private String TM_CA_SURG_98_MSK;
    private String TM_CA_SURG_03;
    private String TM_CA_SURG_03_MSK;
    private String TM_OTH_SURG_98;
    private String TM_OTH_SURG_98_MSK;
    private String TM_OTH_SURG_03;
    private String TM_OTH_SURG_03_MSK;
    private String TM_OTH_SURG_CD;
    private String TM_OTH_SURG_CD_DESC;
    private String TM_RGN_SCOP_98;
    private String TM_RGN_SCOP_98_MSK;
    private String TM_RGN_SCOP_03;
    private String TM_RGN_SCOP_03_MSK;
    private String TM_REGNODE_SCOP_CD;
    private String TM_REGNODE_SCOP_DESC;
    private String TM_RECON_SURG;
    private String TM_RECON_SURG_DESC;
    private String TM_CA_SURG_YEAR;
    private String AGE_AT_TM_CA_SURG_DATE_IN_DAYS;
    private String TM_SURG_DEF_YEAR;
    private String AGE_AT_TM_SURG_DEF_DATE_IN_DAYS;
    private String TM_REASON_NO_SURG;
    private String REASON_NO_SURG_DESC;
    private String TM_PRIM_SURGEON;
    private String TM_PRIM_SURGEON_NAME;
    private String TM_ATN_DR_NO; 
    private String TM_ATN_DR_NAME; 
    private String TM_REASON_NO_RAD; 
    private String TM_REASON_NO_RAD_DESC; 
    private String TM_RAD_STRT_YEAR; 
    private String AGE_AT_TM_RAD_STRT_DATE_IN_DAYS; 
    private String TM_RAD_END_YEAR; 
    private String AGE_AT_TM_RAD_END_DATE_IN_DAYS; 
    private String TM_RAD_TX_MOD; 
    private String TM_RAD_TX_MOD_DESC; 
    private String TM_BOOST_RAD_MOD; 
    private String TM_BOOST_RAD_MOD_DESC; 
    private String TM_LOC_RAD_TX; 
    private String TM_LOC_RAD_TX_DESC; 
    private String TM_RAD_TX_VOL; 
    private String TM_RAD_TX_VOL_DESC; 
    private String TM_NUM_TX_THIS_VOL; 
    private String TM_NUM_TX_THIS_VOL_DESC; 
    private String TM_REG_RAD_DOSE; 
    private String TM_REG_RAD_DOSE_DESC; 
    private String TM_BOOST_RAD_DOSE; 
    private String TM_BOOST_RAD_DOSE_DESC; 
    private String TM_RAD_SURG_SEQ; 
    private String TM_RAD_SURG_SEQ_DESC; 
    private String TM_RAD_MD; 
    private String TM_RAD_MD_NAME; 
    private String TM_SYST_STRT_YEAR; 
    private String AGE_AT_TM_SYST_STRT_DATE_IN_DAYS; 
    private String TM_OTH_STRT_YEAR; 
    private String AGE_AT_TM_OTH_STRT_DATE_IN_DAYS; 
    private String TM_CHEM_SUM; 
    private String TM_CHEM_SUM_DESC; 
    private String TM_CHEM_SUM_MSK; 
    private String TM_CHEM_SUM_MSK_DESC; 
    private String TM_TUMOR_SEQ; 
    private String TM_HORM_SUM; 
    private String TM_HORM_SUM_DESC; 
    private String TM_HORM_SUM_MSK; 
    private String TM_HORM_SUM_MSK_DESC; 
    private String TM_BRM_SUM; 
    private String TM_BRM_SUM_DESC; 
    private String TM_BRM_SUM_MSK; 
    private String TM_BRM_SUM_MSK_DESC; 
    private String TM_OTH_SUM; 
    private String TM_OTH_SUM_DESC; 
    private String TM_OTH_SUM_MSK; 
    private String TM_OTH_SUM_MSK_DESC; 
    private String TM_PALLIA_PROC; 
    private String TM_PALLIA_PROC_DESC; 
    private String TM_PALLIA_PROC_MSK; 
    private String TM_PALLIA_PROC_MSK_DESC; 
    private String TM_ONCOLOGY_MD; 
    private String TM_ONCOLOGY_MD_NAME; 
    private String TM_PRCS_YEAR; 
    private String AGE_AT_TM_PRCS_DATE_IN_DAYS; 
    private String TM_PATH_TEXT; 
    private String TM_SURG_TEXT; 
    private String TM_OVERRIDE_COM; 
    private String TM_CSSIZE; 
    private String TM_CSEXT; 
    private String TM_CSEXTEV; 
    private String TM_CSLMND; 
    private String TM_CSRGNEV; 
    private String TM_CSMETDX; 
    private String TM_CSMETEV; 
    private String TM_TSTAGE; 
    private String TM_TSTAGE_DESC; 
    private String TM_NSTAGE; 
    private String TM_NSTAGE_DESC; 
    private String TM_MSTAGE; 
    private String TM_MSTAGE_DESC; 
    private String TM_TBASIS; 
    private String TM_TBASIS_DESC; 
    private String TM_NBASIS; 
    private String TM_NBASIS_DESC; 
    private String TM_MBASIS; 
    private String TM_MBASIS_DESC; 
    private String TM_AJCC; 
    private String TM_AJCC_DESC;
    private String TM_MSK_STG;
    
    public MSK_ImpactPatientIcdoRecord(){}
    
    public MSK_ImpactPatientIcdoRecord(
            Integer TUMOR_YEAR,
            String PT_ID_ICDO,
            String DMP_ID_ICDO,
            String TM_TUMOR_SEQ_DESC,
            String TM_ACC_YEAR,
            String TM_FIRST_MSK_YEAR,
            String AGE_AT_TM_FIRST_MSK_DATE_IN_DAYS,
            String TM_CASE_STS,
            String TM_CASE_STS_DESC,
            String TM_CASE_EFF_YEAR,
            String AGE_AT_TM_CASE_EFF_DATE_IN_DAYS,
            String TM_STATE_AT_DX,
            String TM_SMOKING_HX,
            String TM_SMOKING_HX_DESC,
            String TM_OCCUPATION,
            String TM_OCCUPATION_DESC,
            String TM_FACILITY_FROM,
            String FACILITY_FROM_DESC,
            String TM_FACILITY_TO,
            String FACILITY_TO_DESC,
            String TM_DX_YEAR,
            String AGE_AT_TM_DX_DATE_IN_DAYS,
            String TM_SITE_CD,
            String TM_SITE_DESC,
            String TM_LATERALITY_CD,
            String TM_LATERALITY_DESC,
            String TM_HIST_CD,
            String TM_HIST_DESC,
            String TM_DX_CONFRM_CD,
            String TM_DX_CONFRM_DESC,
            String TM_REGNODE_EXM_NO,
            String TM_REGNODE_POS_NO,
            String TM_TUMOR_SIZE,
            String TM_RESID_TUMOR_CD,
            String TM_RESID_TUMOR_DESC,
            String TM_GENERAL_STG,
            String TM_GENERAL_STG_DESC,
            String TM_TNM_EDITION,
            String TM_TNM_EDITION_DESC,
            String TM_CLIN_TNM_T,
            String TM_CLIN_TNM_T_DESC,
            String TM_CLIN_TNM_N,
            String TM_CLIN_TNM_N_DESC,
            String TM_CLIN_TNM_M,
            String TM_CLIN_TNM_M_DESC,
            String TM_CLIN_STG_GRP,
            String TM_PATH_TNM_T,
            String TM_PATH_TNM_T_DESC,
            String TM_PATH_TNM_N,
            String TM_PATH_TNM_N_DESC,
            String TM_PATH_TNM_M,
            String TM_PATH_TNM_M_DESC,
            String TM_PATH_STG_GRP,
            String TM_PATH_RPT_AV,
            String TM_CA_STS_AT_ACC,
            String TM_FIRST_RECUR_YEAR,
            String AGE_AT_TM_FIRST_RECUR_DATE_IN_DAYS,
            String TM_FIRST_RECUR_TYP,
            String TM_ADM_YEAR,
            String AGE_AT_TM_ADM_DATE_IN_DAYS,
            String TM_DSCH_YEAR,
            String AGE_AT_TM_DSCH_DATE_IN_DAYS,
            String TM_SURG_DSCH_YEAR,
            String AGE_AT_TM_SURG_DSCH_DATE_IN_DAYS,
            String TM_READM_WTHN_30D,
            String TM_FIRST_TX_YEAR,
            String AGE_AT_TM_FIRST_TX_DATE_IN_DAYS,
            String TM_NON_CA_SURG_SUM,
            String TM_NON_CA_SURG_SUM_DESC,
            String TM_NON_CA_SURG_MSK,
            String TM_NON_CA_SURG_YEAR,
            String AGE_AT_TM_NON_CA_SURG_DATE_IN_DAYS,
            String TM_CA_SURG_98,
            String TM_CA_SURG_98_MSK,
            String TM_CA_SURG_03,
            String TM_CA_SURG_03_MSK,
            String TM_OTH_SURG_98,
            String TM_OTH_SURG_98_MSK,
            String TM_OTH_SURG_03,
            String TM_OTH_SURG_03_MSK,
            String TM_OTH_SURG_CD,
            String TM_OTH_SURG_CD_DESC,
            String TM_RGN_SCOP_98,
            String TM_RGN_SCOP_98_MSK,
            String TM_RGN_SCOP_03,
            String TM_RGN_SCOP_03_MSK,
            String TM_REGNODE_SCOP_CD,
            String TM_REGNODE_SCOP_DESC,
            String TM_RECON_SURG,
            String TM_RECON_SURG_DESC,
            String TM_CA_SURG_YEAR,
            String AGE_AT_TM_CA_SURG_DATE_IN_DAYS,
            String TM_SURG_DEF_YEAR,
            String AGE_AT_TM_SURG_DEF_DATE_IN_DAYS,
            String TM_REASON_NO_SURG,
            String REASON_NO_SURG_DESC,
            String TM_PRIM_SURGEON,
            String TM_PRIM_SURGEON_NAME,
            String TM_ATN_DR_NO,
            String TM_ATN_DR_NAME,
            String TM_REASON_NO_RAD,
            String TM_REASON_NO_RAD_DESC,
            String TM_RAD_STRT_YEAR,
            String AGE_AT_TM_RAD_STRT_DATE_IN_DAYS,
            String TM_RAD_END_YEAR,
            String AGE_AT_TM_RAD_END_DATE_IN_DAYS,
            String TM_RAD_TX_MOD,
            String TM_RAD_TX_MOD_DESC,
            String TM_BOOST_RAD_MOD,
            String TM_BOOST_RAD_MOD_DESC,
            String TM_LOC_RAD_TX,
            String TM_LOC_RAD_TX_DESC,
            String TM_RAD_TX_VOL,
            String TM_RAD_TX_VOL_DESC,
            String TM_NUM_TX_THIS_VOL,
            String TM_NUM_TX_THIS_VOL_DESC,
            String TM_REG_RAD_DOSE,
            String TM_REG_RAD_DOSE_DESC,
            String TM_BOOST_RAD_DOSE,
            String TM_BOOST_RAD_DOSE_DESC,
            String TM_RAD_SURG_SEQ,
            String TM_RAD_SURG_SEQ_DESC,
            String TM_RAD_MD,
            String TM_RAD_MD_NAME,
            String TM_SYST_STRT_YEAR,
            String AGE_AT_TM_SYST_STRT_DATE_IN_DAYS,
            String TM_OTH_STRT_YEAR,
            String AGE_AT_TM_OTH_STRT_DATE_IN_DAYS,
            String TM_CHEM_SUM,
            String TM_CHEM_SUM_DESC,
            String TM_CHEM_SUM_MSK,
            String TM_CHEM_SUM_MSK_DESC,
            String TM_TUMOR_SEQ,
            String TM_HORM_SUM,
            String TM_HORM_SUM_DESC,
            String TM_HORM_SUM_MSK,
            String TM_HORM_SUM_MSK_DESC,
            String TM_BRM_SUM,
            String TM_BRM_SUM_DESC,
            String TM_BRM_SUM_MSK,
            String TM_BRM_SUM_MSK_DESC,
            String TM_OTH_SUM,
            String TM_OTH_SUM_DESC,
            String TM_OTH_SUM_MSK,
            String TM_OTH_SUM_MSK_DESC,
            String TM_PALLIA_PROC,
            String TM_PALLIA_PROC_DESC,
            String TM_PALLIA_PROC_MSK,
            String TM_PALLIA_PROC_MSK_DESC,
            String TM_ONCOLOGY_MD,
            String TM_ONCOLOGY_MD_NAME,
            String TM_PRCS_YEAR,
            String AGE_AT_TM_PRCS_DATE_IN_DAYS,
            String TM_PATH_TEXT,
            String TM_SURG_TEXT,
            String TM_OVERRIDE_COM,
            String TM_CSSIZE,
            String TM_CSEXT,
            String TM_CSEXTEV,
            String TM_CSLMND,
            String TM_CSRGNEV,
            String TM_CSMETDX,
            String TM_CSMETEV,
            String TM_TSTAGE,
            String TM_TSTAGE_DESC,
            String TM_NSTAGE,
            String TM_NSTAGE_DESC,
            String TM_MSTAGE,
            String TM_MSTAGE_DESC,
            String TM_TBASIS,
            String TM_TBASIS_DESC,
            String TM_NBASIS,
            String TM_NBASIS_DESC,
            String TM_MBASIS,
            String TM_MBASIS_DESC,
            String TM_AJCC,
            String TM_AJCC_DESC,
            String TM_MSK_STG){
        this.TUMOR_YEAR = TUMOR_YEAR != null ? TUMOR_YEAR : -1;
        this.PT_ID_ICDO = PT_ID_ICDO != null ? PT_ID_ICDO : "N/A";
        this.DMP_ID_ICDO = DMP_ID_ICDO != null ? DMP_ID_ICDO : "N/A";
        this.TM_TUMOR_SEQ_DESC = TM_TUMOR_SEQ_DESC != null ? TM_TUMOR_SEQ_DESC : "N/A";
        this.TM_ACC_YEAR = TM_ACC_YEAR != null ? TM_ACC_YEAR : "N/A";
        this.TM_FIRST_MSK_YEAR = TM_FIRST_MSK_YEAR != null ? TM_FIRST_MSK_YEAR : "N/A";
        this.AGE_AT_TM_FIRST_MSK_DATE_IN_DAYS = AGE_AT_TM_FIRST_MSK_DATE_IN_DAYS != null ? AGE_AT_TM_FIRST_MSK_DATE_IN_DAYS : "N/A";
        this.TM_CASE_STS = TM_CASE_STS != null ? TM_CASE_STS : "N/A";
        this.TM_CASE_STS_DESC = TM_CASE_STS_DESC != null ? TM_CASE_STS_DESC : "N/A";
        this.TM_CASE_EFF_YEAR = TM_CASE_EFF_YEAR != null ? TM_CASE_EFF_YEAR : "N/A";
        this.AGE_AT_TM_CASE_EFF_DATE_IN_DAYS = AGE_AT_TM_CASE_EFF_DATE_IN_DAYS != null ? AGE_AT_TM_CASE_EFF_DATE_IN_DAYS : "N/A";
        this.TM_STATE_AT_DX = TM_STATE_AT_DX != null ? TM_STATE_AT_DX : "N/A";
        this.TM_SMOKING_HX = TM_SMOKING_HX != null ? TM_SMOKING_HX : "N/A";
        this.TM_SMOKING_HX_DESC = TM_SMOKING_HX_DESC != null ? TM_SMOKING_HX_DESC : "N/A";
        this.TM_OCCUPATION = TM_OCCUPATION != null ? TM_OCCUPATION : "N/A";
        this.TM_OCCUPATION_DESC = TM_OCCUPATION_DESC != null ? TM_OCCUPATION_DESC : "N/A";
        this.TM_FACILITY_FROM = TM_FACILITY_FROM != null ? TM_FACILITY_FROM : "N/A";
        this.FACILITY_FROM_DESC = FACILITY_FROM_DESC != null ? FACILITY_FROM_DESC : "N/A";
        this.TM_FACILITY_TO = TM_FACILITY_TO != null ? TM_FACILITY_TO : "N/A";
        this.FACILITY_TO_DESC = FACILITY_TO_DESC != null ? FACILITY_TO_DESC : "N/A";
        this.TM_DX_YEAR = TM_DX_YEAR != null ? TM_DX_YEAR : "N/A";
        this.AGE_AT_TM_DX_DATE_IN_DAYS = AGE_AT_TM_DX_DATE_IN_DAYS != null ? AGE_AT_TM_DX_DATE_IN_DAYS : "N/A";
        this.TM_SITE_CD = TM_SITE_CD != null ? TM_SITE_CD : "N/A";
        this.TM_SITE_DESC = TM_SITE_DESC != null ? TM_SITE_DESC : "N/A";
        this.TM_LATERALITY_CD = TM_LATERALITY_CD != null ? TM_LATERALITY_CD : "N/A";
        this.TM_LATERALITY_DESC = TM_LATERALITY_DESC != null ? TM_LATERALITY_DESC : "N/A";
        this.TM_HIST_CD = TM_HIST_CD != null ? TM_HIST_CD : "N/A";
        this.TM_HIST_DESC = TM_HIST_DESC != null ? TM_HIST_DESC : "N/A";
        this.TM_DX_CONFRM_CD = TM_DX_CONFRM_CD != null ? TM_DX_CONFRM_CD : "N/A";
        this.TM_DX_CONFRM_DESC = TM_DX_CONFRM_DESC != null ? TM_DX_CONFRM_DESC : "N/A";
        this.TM_REGNODE_EXM_NO = TM_REGNODE_EXM_NO != null ? TM_REGNODE_EXM_NO : "N/A";
        this.TM_REGNODE_POS_NO = TM_REGNODE_POS_NO != null ? TM_REGNODE_POS_NO : "N/A";
        this.TM_TUMOR_SIZE = TM_TUMOR_SIZE != null ? TM_TUMOR_SIZE : "N/A";
        this.TM_RESID_TUMOR_CD = TM_RESID_TUMOR_CD != null ? TM_RESID_TUMOR_CD : "N/A";
        this.TM_RESID_TUMOR_DESC = TM_RESID_TUMOR_DESC != null ? TM_RESID_TUMOR_DESC : "N/A";
        this.TM_GENERAL_STG = TM_GENERAL_STG != null ? TM_GENERAL_STG : "N/A";
        this.TM_GENERAL_STG_DESC = TM_GENERAL_STG_DESC != null ? TM_GENERAL_STG_DESC : "N/A";
        this.TM_TNM_EDITION = TM_TNM_EDITION != null ? TM_TNM_EDITION : "N/A";
        this.TM_TNM_EDITION_DESC = TM_TNM_EDITION_DESC != null ? TM_TNM_EDITION_DESC : "N/A";
        this.TM_CLIN_TNM_T = TM_CLIN_TNM_T != null ? TM_CLIN_TNM_T : "N/A";
        this.TM_CLIN_TNM_T_DESC = TM_CLIN_TNM_T_DESC != null ? TM_CLIN_TNM_T_DESC : "N/A";
        this.TM_CLIN_TNM_N = TM_CLIN_TNM_N != null ? TM_CLIN_TNM_N : "N/A";
        this.TM_CLIN_TNM_N_DESC = TM_CLIN_TNM_N_DESC != null ? TM_CLIN_TNM_N_DESC : "N/A";
        this.TM_CLIN_TNM_M = TM_CLIN_TNM_M != null ? TM_CLIN_TNM_M : "N/A";
        this.TM_CLIN_TNM_M_DESC = TM_CLIN_TNM_M_DESC != null ? TM_CLIN_TNM_M_DESC : "N/A";
        this.TM_CLIN_STG_GRP = TM_CLIN_STG_GRP != null ? TM_CLIN_STG_GRP : "N/A";
        this.TM_PATH_TNM_T = TM_PATH_TNM_T != null ? TM_PATH_TNM_T : "N/A";
        this.TM_PATH_TNM_T_DESC = TM_PATH_TNM_T_DESC != null ? TM_PATH_TNM_T_DESC : "N/A";
        this.TM_PATH_TNM_N = TM_PATH_TNM_N != null ? TM_PATH_TNM_N : "N/A";
        this.TM_PATH_TNM_N_DESC = TM_PATH_TNM_N_DESC != null ? TM_PATH_TNM_N_DESC : "N/A";
        this.TM_PATH_TNM_M = TM_PATH_TNM_M != null ? TM_PATH_TNM_M : "N/A";
        this.TM_PATH_TNM_M_DESC = TM_PATH_TNM_M_DESC != null ? TM_PATH_TNM_M_DESC : "N/A";
        this.TM_PATH_STG_GRP = TM_PATH_STG_GRP != null ? TM_PATH_STG_GRP : "N/A";
        this.TM_PATH_RPT_AV = TM_PATH_RPT_AV != null ? TM_PATH_RPT_AV : "N/A";
        this.TM_CA_STS_AT_ACC = TM_CA_STS_AT_ACC != null ? TM_CA_STS_AT_ACC : "N/A";
        this.TM_FIRST_RECUR_YEAR = TM_FIRST_RECUR_YEAR != null ? TM_FIRST_RECUR_YEAR : "N/A";
        this.AGE_AT_TM_FIRST_RECUR_DATE_IN_DAYS = AGE_AT_TM_FIRST_RECUR_DATE_IN_DAYS != null ? AGE_AT_TM_FIRST_RECUR_DATE_IN_DAYS : "N/A";
        this.TM_FIRST_RECUR_TYP = TM_FIRST_RECUR_TYP != null ? TM_FIRST_RECUR_TYP : "N/A";
        this.TM_ADM_YEAR = TM_ADM_YEAR != null ? TM_ADM_YEAR : "N/A";
        this.AGE_AT_TM_ADM_DATE_IN_DAYS = AGE_AT_TM_ADM_DATE_IN_DAYS != null ? AGE_AT_TM_ADM_DATE_IN_DAYS : "N/A";
        this.TM_DSCH_YEAR = TM_DSCH_YEAR != null ? TM_DSCH_YEAR : "N/A";
        this.AGE_AT_TM_DSCH_DATE_IN_DAYS = AGE_AT_TM_DSCH_DATE_IN_DAYS != null ? AGE_AT_TM_DSCH_DATE_IN_DAYS : "N/A";
        this.TM_SURG_DSCH_YEAR = TM_SURG_DSCH_YEAR != null ? TM_SURG_DSCH_YEAR : "N/A";
        this.AGE_AT_TM_SURG_DSCH_DATE_IN_DAYS = AGE_AT_TM_SURG_DSCH_DATE_IN_DAYS != null ? AGE_AT_TM_SURG_DSCH_DATE_IN_DAYS : "N/A";
        this.TM_READM_WTHN_30D = TM_READM_WTHN_30D != null ? TM_READM_WTHN_30D : "N/A";
        this.TM_FIRST_TX_YEAR = TM_FIRST_TX_YEAR != null ? TM_FIRST_TX_YEAR : "N/A";
        this.AGE_AT_TM_FIRST_TX_DATE_IN_DAYS = AGE_AT_TM_FIRST_TX_DATE_IN_DAYS != null ? AGE_AT_TM_FIRST_TX_DATE_IN_DAYS : "N/A";
        this.TM_NON_CA_SURG_SUM = TM_NON_CA_SURG_SUM != null ? TM_NON_CA_SURG_SUM : "N/A";
        this.TM_NON_CA_SURG_SUM_DESC = TM_NON_CA_SURG_SUM_DESC != null ? TM_NON_CA_SURG_SUM_DESC : "N/A";
        this.TM_NON_CA_SURG_MSK = TM_NON_CA_SURG_MSK != null ? TM_NON_CA_SURG_MSK : "N/A";
        this.TM_NON_CA_SURG_YEAR = TM_NON_CA_SURG_YEAR != null ? TM_NON_CA_SURG_YEAR : "N/A";
        this.AGE_AT_TM_NON_CA_SURG_DATE_IN_DAYS = AGE_AT_TM_NON_CA_SURG_DATE_IN_DAYS != null ? AGE_AT_TM_NON_CA_SURG_DATE_IN_DAYS : "N/A";
        this.TM_CA_SURG_98 = TM_CA_SURG_98 != null ? TM_CA_SURG_98 : "N/A";
        this.TM_CA_SURG_98_MSK = TM_CA_SURG_98_MSK != null ? TM_CA_SURG_98_MSK : "N/A";
        this.TM_CA_SURG_03 = TM_CA_SURG_03 != null ? TM_CA_SURG_03 : "N/A";
        this.TM_CA_SURG_03_MSK = TM_CA_SURG_03_MSK != null ? TM_CA_SURG_03_MSK : "N/A";
        this.TM_OTH_SURG_98 = TM_OTH_SURG_98 != null ? TM_OTH_SURG_98 : "N/A";
        this.TM_OTH_SURG_98_MSK = TM_OTH_SURG_98_MSK != null ? TM_OTH_SURG_98_MSK : "N/A";
        this.TM_OTH_SURG_03 = TM_OTH_SURG_03 != null ? TM_OTH_SURG_03 : "N/A";
        this.TM_OTH_SURG_03_MSK = TM_OTH_SURG_03_MSK != null ? TM_OTH_SURG_03_MSK : "N/A";
        this.TM_OTH_SURG_CD = TM_OTH_SURG_CD != null ? TM_OTH_SURG_CD : "N/A";
        this.TM_OTH_SURG_CD_DESC = TM_OTH_SURG_CD_DESC != null ? TM_OTH_SURG_CD_DESC : "N/A";
        this.TM_RGN_SCOP_98 = TM_RGN_SCOP_98 != null ? TM_RGN_SCOP_98 : "N/A";
        this.TM_RGN_SCOP_98_MSK = TM_RGN_SCOP_98_MSK != null ? TM_RGN_SCOP_98_MSK : "N/A";
        this.TM_RGN_SCOP_03 = TM_RGN_SCOP_03 != null ? TM_RGN_SCOP_03 : "N/A";
        this.TM_RGN_SCOP_03_MSK = TM_RGN_SCOP_03_MSK != null ? TM_RGN_SCOP_03_MSK : "N/A";
        this.TM_REGNODE_SCOP_CD = TM_REGNODE_SCOP_CD != null ? TM_REGNODE_SCOP_CD : "N/A";
        this.TM_REGNODE_SCOP_DESC = TM_REGNODE_SCOP_DESC != null ? TM_REGNODE_SCOP_DESC : "N/A";
        this.TM_RECON_SURG = TM_RECON_SURG != null ? TM_RECON_SURG : "N/A";
        this.TM_RECON_SURG_DESC = TM_RECON_SURG_DESC != null ? TM_RECON_SURG_DESC : "N/A";
        this.TM_CA_SURG_YEAR = TM_CA_SURG_YEAR != null ? TM_CA_SURG_YEAR : "N/A";
        this.AGE_AT_TM_CA_SURG_DATE_IN_DAYS = AGE_AT_TM_CA_SURG_DATE_IN_DAYS != null ? AGE_AT_TM_CA_SURG_DATE_IN_DAYS : "N/A";
        this.TM_SURG_DEF_YEAR = TM_SURG_DEF_YEAR != null ? TM_SURG_DEF_YEAR : "N/A";
        this.AGE_AT_TM_SURG_DEF_DATE_IN_DAYS = AGE_AT_TM_SURG_DEF_DATE_IN_DAYS != null ? AGE_AT_TM_SURG_DEF_DATE_IN_DAYS : "N/A";
        this.TM_REASON_NO_SURG = TM_REASON_NO_SURG != null ? TM_REASON_NO_SURG : "N/A";
        this.REASON_NO_SURG_DESC = REASON_NO_SURG_DESC != null ? REASON_NO_SURG_DESC : "N/A";
        this.TM_PRIM_SURGEON = TM_PRIM_SURGEON != null ? TM_PRIM_SURGEON : "N/A";
        this.TM_PRIM_SURGEON_NAME = TM_PRIM_SURGEON_NAME != null ? TM_PRIM_SURGEON_NAME : "N/A";
        this.TM_ATN_DR_NO = TM_ATN_DR_NO != null ? TM_ATN_DR_NO : "N/A";
        this.TM_ATN_DR_NAME = TM_ATN_DR_NAME != null ? TM_ATN_DR_NAME : "N/A";
        this.TM_REASON_NO_RAD = TM_REASON_NO_RAD != null ? TM_REASON_NO_RAD : "N/A";
        this.TM_REASON_NO_RAD_DESC = TM_REASON_NO_RAD_DESC != null ? TM_REASON_NO_RAD_DESC : "N/A";
        this.TM_RAD_STRT_YEAR = TM_RAD_STRT_YEAR != null ? TM_RAD_STRT_YEAR : "N/A";
        this.AGE_AT_TM_RAD_STRT_DATE_IN_DAYS = AGE_AT_TM_RAD_STRT_DATE_IN_DAYS != null ? AGE_AT_TM_RAD_STRT_DATE_IN_DAYS : "N/A";
        this.TM_RAD_END_YEAR = TM_RAD_END_YEAR != null ? TM_RAD_END_YEAR : "N/A";
        this.AGE_AT_TM_RAD_END_DATE_IN_DAYS = AGE_AT_TM_RAD_END_DATE_IN_DAYS != null ? AGE_AT_TM_RAD_END_DATE_IN_DAYS : "N/A";
        this.TM_RAD_TX_MOD = TM_RAD_TX_MOD != null ? TM_RAD_TX_MOD : "N/A";
        this.TM_RAD_TX_MOD_DESC = TM_RAD_TX_MOD_DESC != null ? TM_RAD_TX_MOD_DESC : "N/A";
        this.TM_BOOST_RAD_MOD = TM_BOOST_RAD_MOD != null ? TM_BOOST_RAD_MOD : "N/A";
        this.TM_BOOST_RAD_MOD_DESC = TM_BOOST_RAD_MOD_DESC != null ? TM_BOOST_RAD_MOD_DESC : "N/A";
        this.TM_LOC_RAD_TX = TM_LOC_RAD_TX != null ? TM_LOC_RAD_TX : "N/A";
        this.TM_LOC_RAD_TX_DESC = TM_LOC_RAD_TX_DESC != null ? TM_LOC_RAD_TX_DESC : "N/A";
        this.TM_RAD_TX_VOL = TM_RAD_TX_VOL != null ? TM_RAD_TX_VOL : "N/A";
        this.TM_RAD_TX_VOL_DESC = TM_RAD_TX_VOL_DESC != null ? TM_RAD_TX_VOL_DESC : "N/A";
        this.TM_NUM_TX_THIS_VOL = TM_NUM_TX_THIS_VOL != null ? TM_NUM_TX_THIS_VOL : "N/A";
        this.TM_NUM_TX_THIS_VOL_DESC = TM_NUM_TX_THIS_VOL_DESC != null ? TM_NUM_TX_THIS_VOL_DESC : "N/A";
        this.TM_REG_RAD_DOSE = TM_REG_RAD_DOSE != null ? TM_REG_RAD_DOSE : "N/A";
        this.TM_REG_RAD_DOSE_DESC = TM_REG_RAD_DOSE_DESC != null ? TM_REG_RAD_DOSE_DESC : "N/A";
        this.TM_BOOST_RAD_DOSE = TM_BOOST_RAD_DOSE != null ? TM_BOOST_RAD_DOSE : "N/A";
        this.TM_BOOST_RAD_DOSE_DESC = TM_BOOST_RAD_DOSE_DESC != null ? TM_BOOST_RAD_DOSE_DESC : "N/A";
        this.TM_RAD_SURG_SEQ = TM_RAD_SURG_SEQ != null ? TM_RAD_SURG_SEQ : "N/A";
        this.TM_RAD_SURG_SEQ_DESC = TM_RAD_SURG_SEQ_DESC != null ? TM_RAD_SURG_SEQ_DESC : "N/A";
        this.TM_RAD_MD = TM_RAD_MD != null ? TM_RAD_MD : "N/A";
        this.TM_RAD_MD_NAME = TM_RAD_MD_NAME != null ? TM_RAD_MD_NAME : "N/A";
        this.TM_SYST_STRT_YEAR = TM_SYST_STRT_YEAR != null ? TM_SYST_STRT_YEAR : "N/A";
        this.AGE_AT_TM_SYST_STRT_DATE_IN_DAYS = AGE_AT_TM_SYST_STRT_DATE_IN_DAYS != null ? AGE_AT_TM_SYST_STRT_DATE_IN_DAYS : "N/A";
        this.TM_OTH_STRT_YEAR = TM_OTH_STRT_YEAR != null ? TM_OTH_STRT_YEAR : "N/A";
        this.AGE_AT_TM_OTH_STRT_DATE_IN_DAYS = AGE_AT_TM_OTH_STRT_DATE_IN_DAYS != null ? AGE_AT_TM_OTH_STRT_DATE_IN_DAYS : "N/A";
        this.TM_CHEM_SUM = TM_CHEM_SUM != null ? TM_CHEM_SUM : "N/A";
        this.TM_CHEM_SUM_DESC = TM_CHEM_SUM_DESC != null ? TM_CHEM_SUM_DESC : "N/A";
        this.TM_CHEM_SUM_MSK = TM_CHEM_SUM_MSK != null ? TM_CHEM_SUM_MSK : "N/A";
        this.TM_CHEM_SUM_MSK_DESC = TM_CHEM_SUM_MSK_DESC != null ? TM_CHEM_SUM_MSK_DESC : "N/A";
        this.TM_TUMOR_SEQ = TM_TUMOR_SEQ != null ? TM_TUMOR_SEQ : "N/A";
        this.TM_HORM_SUM = TM_HORM_SUM != null ? TM_HORM_SUM : "N/A";
        this.TM_HORM_SUM_DESC = TM_HORM_SUM_DESC != null ? TM_HORM_SUM_DESC : "N/A";
        this.TM_HORM_SUM_MSK = TM_HORM_SUM_MSK != null ? TM_HORM_SUM_MSK : "N/A";
        this.TM_HORM_SUM_MSK_DESC = TM_HORM_SUM_MSK_DESC != null ? TM_HORM_SUM_MSK_DESC : "N/A";
        this.TM_BRM_SUM = TM_BRM_SUM != null ? TM_BRM_SUM : "N/A";
        this.TM_BRM_SUM_DESC = TM_BRM_SUM_DESC != null ? TM_BRM_SUM_DESC : "N/A";
        this.TM_BRM_SUM_MSK = TM_BRM_SUM_MSK != null ? TM_BRM_SUM_MSK : "N/A";
        this.TM_BRM_SUM_MSK_DESC = TM_BRM_SUM_MSK_DESC != null ? TM_BRM_SUM_MSK_DESC : "N/A";
        this.TM_OTH_SUM = TM_OTH_SUM != null ? TM_OTH_SUM : "N/A";
        this.TM_OTH_SUM_DESC = TM_OTH_SUM_DESC != null ? TM_OTH_SUM_DESC : "N/A";
        this.TM_OTH_SUM_MSK = TM_OTH_SUM_MSK != null ? TM_OTH_SUM_MSK : "N/A";
        this.TM_OTH_SUM_MSK_DESC = TM_OTH_SUM_MSK_DESC != null ? TM_OTH_SUM_MSK_DESC : "N/A";
        this.TM_PALLIA_PROC = TM_PALLIA_PROC != null ? TM_PALLIA_PROC : "N/A";
        this.TM_PALLIA_PROC_DESC = TM_PALLIA_PROC_DESC != null ? TM_PALLIA_PROC_DESC : "N/A";
        this.TM_PALLIA_PROC_MSK = TM_PALLIA_PROC_MSK != null ? TM_PALLIA_PROC_MSK : "N/A";
        this.TM_PALLIA_PROC_MSK_DESC = TM_PALLIA_PROC_MSK_DESC != null ? TM_PALLIA_PROC_MSK_DESC : "N/A";
        this.TM_ONCOLOGY_MD = TM_ONCOLOGY_MD != null ? TM_ONCOLOGY_MD : "N/A";
        this.TM_ONCOLOGY_MD_NAME = TM_ONCOLOGY_MD_NAME != null ? TM_ONCOLOGY_MD_NAME : "N/A";
        this.TM_PRCS_YEAR = TM_PRCS_YEAR != null ? TM_PRCS_YEAR : "N/A";
        this.AGE_AT_TM_PRCS_DATE_IN_DAYS = AGE_AT_TM_PRCS_DATE_IN_DAYS != null ? AGE_AT_TM_PRCS_DATE_IN_DAYS : "N/A";
        this.TM_PATH_TEXT = TM_PATH_TEXT != null ? TM_PATH_TEXT : "N/A";
        this.TM_SURG_TEXT = TM_SURG_TEXT != null ? TM_SURG_TEXT : "N/A";
        this.TM_OVERRIDE_COM = TM_OVERRIDE_COM != null ? TM_OVERRIDE_COM : "N/A";
        this.TM_CSSIZE = TM_CSSIZE != null ? TM_CSSIZE : "N/A";
        this.TM_CSEXT = TM_CSEXT != null ? TM_CSEXT : "N/A";
        this.TM_CSEXTEV = TM_CSEXTEV != null ? TM_CSEXTEV : "N/A";
        this.TM_CSLMND = TM_CSLMND != null ? TM_CSLMND : "N/A";
        this.TM_CSRGNEV = TM_CSRGNEV != null ? TM_CSRGNEV : "N/A";
        this.TM_CSMETDX = TM_CSMETDX != null ? TM_CSMETDX : "N/A";
        this.TM_CSMETEV = TM_CSMETEV != null ? TM_CSMETEV : "N/A";
        this.TM_TSTAGE = TM_TSTAGE != null ? TM_TSTAGE : "N/A";
        this.TM_TSTAGE_DESC = TM_TSTAGE_DESC != null ? TM_TSTAGE_DESC : "N/A";
        this.TM_NSTAGE = TM_NSTAGE != null ? TM_NSTAGE : "N/A";
        this.TM_NSTAGE_DESC = TM_NSTAGE_DESC != null ? TM_NSTAGE_DESC : "N/A";
        this.TM_MSTAGE = TM_MSTAGE != null ? TM_MSTAGE : "N/A";
        this.TM_MSTAGE_DESC = TM_MSTAGE_DESC != null ? TM_MSTAGE_DESC : "N/A";
        this.TM_TBASIS = TM_TBASIS != null ? TM_TBASIS : "N/A";
        this.TM_TBASIS_DESC = TM_TBASIS_DESC != null ? TM_TBASIS_DESC : "N/A";
        this.TM_NBASIS = TM_NBASIS != null ? TM_NBASIS : "N/A";
        this.TM_NBASIS_DESC = TM_NBASIS_DESC != null ? TM_NBASIS_DESC : "N/A";
        this.TM_MBASIS = TM_MBASIS != null ? TM_MBASIS : "N/A";
        this.TM_MBASIS_DESC = TM_MBASIS_DESC != null ? TM_MBASIS_DESC : "N/A";
        this.TM_AJCC = TM_AJCC != null ? TM_AJCC : "N/A";
        this.TM_AJCC_DESC = TM_AJCC_DESC != null ? TM_AJCC_DESC : "N/A";
        this.TM_MSK_STG = TM_MSK_STG != null ? TM_MSK_STG : "N/A";
    }
    
    public String getPT_ID_ICDO(){
	return PT_ID_ICDO;
    }

    public void setPT_ID_ICDO(String PT_ID_ICDO) {
        this.PT_ID_ICDO = PT_ID_ICDO != null ? PT_ID_ICDO : "N/A";
    }
    
    public Integer getTUMOR_YEAR(){
	return TUMOR_YEAR;
    }

    public void setTUMOR_YEAR(Integer TUMOR_YEAR) {
        this.TUMOR_YEAR = TUMOR_YEAR != null ? TUMOR_YEAR : -1;
    }

    public String getDMP_ID_ICDO() {
        return DMP_ID_ICDO;
    }

    public void setDMP_ID_ICDO(String DMP_ID_ICDO) {
        this.DMP_ID_ICDO = DMP_ID_ICDO != null ? DMP_ID_ICDO : "N/A";
    }

    public String getTM_TUMOR_SEQ_DESC() {
        return TM_TUMOR_SEQ_DESC;
    }

    public void setTM_TUMOR_SEQ_DESC(String TM_TUMOR_SEQ_DESC) {
        this.TM_TUMOR_SEQ_DESC = TM_TUMOR_SEQ_DESC != null ? TM_TUMOR_SEQ_DESC : "N/A";
    }

    public String getTM_ACC_YEAR() {
        return TM_ACC_YEAR;
    }

    public void setTM_ACC_YEAR(String TM_ACC_YEAR) {
        this.TM_ACC_YEAR = TM_ACC_YEAR != null ? TM_ACC_YEAR : "N/A";
    }

    public String getTM_FIRST_MSK_YEAR() {
        return TM_FIRST_MSK_YEAR;
    }

    public void setTM_FIRST_MSK_YEAR(String TM_FIRST_MSK_YEAR) {
        this.TM_FIRST_MSK_YEAR = TM_FIRST_MSK_YEAR != null ? TM_FIRST_MSK_YEAR : "N/A";
    }

    public String getAGE_AT_TM_FIRST_MSK_DATE_IN_DAYS() {
        return AGE_AT_TM_FIRST_MSK_DATE_IN_DAYS;
    }

    public void setAGE_AT_TM_FIRST_MSK_DATE_IN_DAYS(String AGE_AT_TM_FIRST_MSK_DATE_IN_DAYS) {
        this.AGE_AT_TM_FIRST_MSK_DATE_IN_DAYS = AGE_AT_TM_FIRST_MSK_DATE_IN_DAYS != null ? AGE_AT_TM_FIRST_MSK_DATE_IN_DAYS : "N/A";
    }

    public String getTM_CASE_STS() {
        return TM_CASE_STS;
    }

    public void setTM_CASE_STS(String TM_CASE_STS) {
        this.TM_CASE_STS = TM_CASE_STS != null ? TM_CASE_STS : "N/A";
    }

    public String getTM_CASE_STS_DESC() {
        return TM_CASE_STS_DESC;
    }

    public void setTM_CASE_STS_DESC(String TM_CASE_STS_DESC) {
        this.TM_CASE_STS_DESC = TM_CASE_STS_DESC != null ? TM_CASE_STS_DESC : "N/A";
    }

    public String getTM_CASE_EFF_YEAR() {
        return TM_CASE_EFF_YEAR;
    }

    public void setTM_CASE_EFF_YEAR(String TM_CASE_EFF_YEAR) {
        this.TM_CASE_EFF_YEAR = TM_CASE_EFF_YEAR != null ? TM_CASE_EFF_YEAR : "N/A";
    }

    public String getAGE_AT_TM_CASE_EFF_DATE_IN_DAYS() {
        return AGE_AT_TM_CASE_EFF_DATE_IN_DAYS;
    }

    public void setAGE_AT_TM_CASE_EFF_DATE_IN_DAYS(String AGE_AT_TM_CASE_EFF_DATE_IN_DAYS) {
        this.AGE_AT_TM_CASE_EFF_DATE_IN_DAYS = AGE_AT_TM_CASE_EFF_DATE_IN_DAYS != null ? AGE_AT_TM_CASE_EFF_DATE_IN_DAYS : "N/A";
    }

    public String getTM_STATE_AT_DX() {
        return TM_STATE_AT_DX;
    }

    public void setTM_STATE_AT_DX(String TM_STATE_AT_DX) {
        this.TM_STATE_AT_DX = TM_STATE_AT_DX != null ? TM_STATE_AT_DX : "N/A";
    }

    public String getTM_SMOKING_HX() {
        return TM_SMOKING_HX;
    }

    public void setTM_SMOKING_HX(String TM_SMOKING_HX) {
        this.TM_SMOKING_HX = TM_SMOKING_HX != null ? TM_SMOKING_HX : "N/A";
    }

    public String getTM_SMOKING_HX_DESC() {
        return TM_SMOKING_HX_DESC;
    }

    public void setTM_SMOKING_HX_DESC(String TM_SMOKING_HX_DESC) {
        this.TM_SMOKING_HX_DESC = TM_SMOKING_HX_DESC != null ? TM_SMOKING_HX_DESC : "N/A";
    }

    public String getTM_OCCUPATION() {
        return TM_OCCUPATION;
    }

    public void setTM_OCCUPATION(String TM_OCCUPATION) {
        this.TM_OCCUPATION = TM_OCCUPATION != null ? TM_OCCUPATION : "N/A";
    }

    public String getTM_OCCUPATION_DESC() {
        return TM_OCCUPATION_DESC;
    }

    public void setTM_OCCUPATION_DESC(String TM_OCCUPATION_DESC) {
        this.TM_OCCUPATION_DESC = TM_OCCUPATION_DESC != null ? TM_OCCUPATION_DESC : "N/A";
    }

    public String getTM_FACILITY_FROM() {
        return TM_FACILITY_FROM;
    }

    public void setTM_FACILITY_FROM(String TM_FACILITY_FROM) {
        this.TM_FACILITY_FROM = TM_FACILITY_FROM != null ? TM_FACILITY_FROM : "N/A";
    }

    public String getFACILITY_FROM_DESC() {
        return FACILITY_FROM_DESC;
    }

    public void setFACILITY_FROM_DESC(String FACILITY_FROM_DESC) {
        this.FACILITY_FROM_DESC = FACILITY_FROM_DESC != null ? FACILITY_FROM_DESC : "N/A";
    }

    public String getTM_FACILITY_TO() {
        return TM_FACILITY_TO;
    }

    public void setTM_FACILITY_TO(String TM_FACILITY_TO) {
        this.TM_FACILITY_TO = TM_FACILITY_TO != null ? TM_FACILITY_TO : "N/A";
    }

    public String getFACILITY_TO_DESC() {
        return FACILITY_TO_DESC;
    }

    public void setFACILITY_TO_DESC(String FACILITY_TO_DESC) {
        this.FACILITY_TO_DESC = FACILITY_TO_DESC != null ? FACILITY_TO_DESC : "N/A";
    }

    public String getTM_DX_YEAR() {
        return TM_DX_YEAR;
    }

    public void setTM_DX_YEAR(String TM_DX_YEAR) {
        this.TM_DX_YEAR = TM_DX_YEAR != null ? TM_DX_YEAR : "N/A";
    }

    public String getAGE_AT_TM_DX_DATE_IN_DAYS() {
        return AGE_AT_TM_DX_DATE_IN_DAYS;
    }

    public void setAGE_AT_TM_DX_DATE_IN_DAYS(String AGE_AT_TM_DX_DATE_IN_DAYS) {
        this.AGE_AT_TM_DX_DATE_IN_DAYS = AGE_AT_TM_DX_DATE_IN_DAYS != null ? AGE_AT_TM_DX_DATE_IN_DAYS : "N/A";
    }

    public String getTM_SITE_CD() {
        return TM_SITE_CD;
    }

    public void setTM_SITE_CD(String TM_SITE_CD) {
        this.TM_SITE_CD = TM_SITE_CD != null ? TM_SITE_CD : "N/A";
    }

    public String getTM_SITE_DESC() {
        return TM_SITE_DESC;
    }

    public void setTM_SITE_DESC(String TM_SITE_DESC) {
        this.TM_SITE_DESC = TM_SITE_DESC != null ? TM_SITE_DESC : "N/A";
    }

    public String getTM_LATERALITY_CD() {
        return TM_LATERALITY_CD;
    }

    public void setTM_LATERALITY_CD(String TM_LATERALITY_CD) {
        this.TM_LATERALITY_CD = TM_LATERALITY_CD != null ? TM_LATERALITY_CD : "N/A";
    }

    public String getTM_LATERALITY_DESC() {
        return TM_LATERALITY_DESC;
    }

    public void setTM_LATERALITY_DESC(String TM_LATERALITY_DESC) {
        this.TM_LATERALITY_DESC = TM_LATERALITY_DESC != null ? TM_LATERALITY_DESC : "N/A";
    }

    public String getTM_HIST_CD() {
        return TM_HIST_CD;
    }

    public void setTM_HIST_CD(String TM_HIST_CD) {
        this.TM_HIST_CD = TM_HIST_CD != null ? TM_HIST_CD : "N/A";
    }

    public String getTM_HIST_DESC() {
        return TM_HIST_DESC;
    }

    public void setTM_HIST_DESC(String TM_HIST_DESC) {
        this.TM_HIST_DESC = TM_HIST_DESC != null ? TM_HIST_DESC : "N/A";
    }

    public String getTM_DX_CONFRM_CD() {
        return TM_DX_CONFRM_CD;
    }

    public void setTM_DX_CONFRM_CD(String TM_DX_CONFRM_CD) {
        this.TM_DX_CONFRM_CD = TM_DX_CONFRM_CD != null ? TM_DX_CONFRM_CD : "N/A";
    }

    public String getTM_DX_CONFRM_DESC() {
        return TM_DX_CONFRM_DESC;
    }

    public void setTM_DX_CONFRM_DESC(String TM_DX_CONFRM_DESC) {
        this.TM_DX_CONFRM_DESC = TM_DX_CONFRM_DESC != null ? TM_DX_CONFRM_DESC : "N/A";
    }

    public String getTM_REGNODE_EXM_NO() {
        return TM_REGNODE_EXM_NO;
    }

    public void setTM_REGNODE_EXM_NO(String TM_REGNODE_EXM_NO) {
        this.TM_REGNODE_EXM_NO = TM_REGNODE_EXM_NO != null ? TM_REGNODE_EXM_NO : "N/A";
    }

    public String getTM_REGNODE_POS_NO() {
        return TM_REGNODE_POS_NO;
    }

    public void setTM_REGNODE_POS_NO(String TM_REGNODE_POS_NO) {
        this.TM_REGNODE_POS_NO = TM_REGNODE_POS_NO != null ? TM_REGNODE_POS_NO : "N/A";
    }

    public String getTM_TUMOR_SIZE() {
        return TM_TUMOR_SIZE;
    }

    public void setTM_TUMOR_SIZE(String TM_TUMOR_SIZE) {
        this.TM_TUMOR_SIZE = TM_TUMOR_SIZE != null ? TM_TUMOR_SIZE : "N/A";
    }

    public String getTM_RESID_TUMOR_CD() {
        return TM_RESID_TUMOR_CD;
    }

    public void setTM_RESID_TUMOR_CD(String TM_RESID_TUMOR_CD) {
        this.TM_RESID_TUMOR_CD = TM_RESID_TUMOR_CD != null ? TM_RESID_TUMOR_CD : "N/A";
    }

    public String getTM_RESID_TUMOR_DESC() {
        return TM_RESID_TUMOR_DESC;
    }

    public void setTM_RESID_TUMOR_DESC(String TM_RESID_TUMOR_DESC) {
        this.TM_RESID_TUMOR_DESC = TM_RESID_TUMOR_DESC != null ? TM_RESID_TUMOR_DESC : "N/A";
    }

    public String getTM_GENERAL_STG() {
        return TM_GENERAL_STG;
    }

    public void setTM_GENERAL_STG(String TM_GENERAL_STG) {
        this.TM_GENERAL_STG = TM_GENERAL_STG != null ? TM_GENERAL_STG : "N/A";
    }

    public String getTM_GENERAL_STG_DESC() {
        return TM_GENERAL_STG_DESC;
    }

    public void setTM_GENERAL_STG_DESC(String TM_GENERAL_STG_DESC) {
        this.TM_GENERAL_STG_DESC = TM_GENERAL_STG_DESC != null ? TM_GENERAL_STG_DESC : "N/A";
    }

    public String getTM_TNM_EDITION() {
        return TM_TNM_EDITION;
    }

    public void setTM_TNM_EDITION(String TM_TNM_EDITION) {
        this.TM_TNM_EDITION = TM_TNM_EDITION != null ? TM_TNM_EDITION : "N/A";
    }

    public String getTM_TNM_EDITION_DESC() {
        return TM_TNM_EDITION_DESC;
    }

    public void setTM_TNM_EDITION_DESC(String TM_TNM_EDITION_DESC) {
        this.TM_TNM_EDITION_DESC = TM_TNM_EDITION_DESC != null ? TM_TNM_EDITION_DESC : "N/A";
    }

    public String getTM_CLIN_TNM_T() {
        return TM_CLIN_TNM_T;
    }

    public void setTM_CLIN_TNM_T(String TM_CLIN_TNM_T) {
        this.TM_CLIN_TNM_T = TM_CLIN_TNM_T != null ? TM_CLIN_TNM_T : "N/A";
    }

    public String getTM_CLIN_TNM_T_DESC() {
        return TM_CLIN_TNM_T_DESC;
    }

    public void setTM_CLIN_TNM_T_DESC(String TM_CLIN_TNM_T_DESC) {
        this.TM_CLIN_TNM_T_DESC = TM_CLIN_TNM_T_DESC != null ? TM_CLIN_TNM_T_DESC : "N/A";
    }

    public String getTM_CLIN_TNM_N() {
        return TM_CLIN_TNM_N;
    }

    public void setTM_CLIN_TNM_N(String TM_CLIN_TNM_N) {
        this.TM_CLIN_TNM_N = TM_CLIN_TNM_N != null ? TM_CLIN_TNM_N : "N/A";
    }

    public String getTM_CLIN_TNM_N_DESC() {
        return TM_CLIN_TNM_N_DESC;
    }

    public void setTM_CLIN_TNM_N_DESC(String TM_CLIN_TNM_N_DESC) {
        this.TM_CLIN_TNM_N_DESC = TM_CLIN_TNM_N_DESC != null ? TM_CLIN_TNM_N_DESC : "N/A";
    }

    public String getTM_CLIN_TNM_M() {
        return TM_CLIN_TNM_M;
    }

    public void setTM_CLIN_TNM_M(String TM_CLIN_TNM_M) {
        this.TM_CLIN_TNM_M = TM_CLIN_TNM_M != null ? TM_CLIN_TNM_M : "N/A";
    }

    public String getTM_CLIN_TNM_M_DESC() {
        return TM_CLIN_TNM_M_DESC;
    }

    public void setTM_CLIN_TNM_M_DESC(String TM_CLIN_TNM_M_DESC) {
        this.TM_CLIN_TNM_M_DESC = TM_CLIN_TNM_M_DESC != null ? TM_CLIN_TNM_M_DESC : "N/A";
    }

    public String getTM_CLIN_STG_GRP() {
        return TM_CLIN_STG_GRP;
    }

    public void setTM_CLIN_STG_GRP(String TM_CLIN_STG_GRP) {
        this.TM_CLIN_STG_GRP = TM_CLIN_STG_GRP != null ? TM_CLIN_STG_GRP : "N/A";
    }

    public String getTM_PATH_TNM_T() {
        return TM_PATH_TNM_T;
    }

    public void setTM_PATH_TNM_T(String TM_PATH_TNM_T) {
        this.TM_PATH_TNM_T = TM_PATH_TNM_T != null ? TM_PATH_TNM_T : "N/A";
    }

    public String getTM_PATH_TNM_T_DESC() {
        return TM_PATH_TNM_T_DESC;
    }

    public void setTM_PATH_TNM_T_DESC(String TM_PATH_TNM_T_DESC) {
        this.TM_PATH_TNM_T_DESC = TM_PATH_TNM_T_DESC != null ? TM_PATH_TNM_T_DESC : "N/A";
    }

    public String getTM_PATH_TNM_N() {
        return TM_PATH_TNM_N;
    }

    public void setTM_PATH_TNM_N(String TM_PATH_TNM_N) {
        this.TM_PATH_TNM_N = TM_PATH_TNM_N != null ? TM_PATH_TNM_N : "N/A";
    }

    public String getTM_PATH_TNM_N_DESC() {
        return TM_PATH_TNM_N_DESC;
    }

    public void setTM_PATH_TNM_N_DESC(String TM_PATH_TNM_N_DESC) {
        this.TM_PATH_TNM_N_DESC = TM_PATH_TNM_N_DESC != null ? TM_PATH_TNM_N_DESC : "N/A";
    }

    public String getTM_PATH_TNM_M() {
        return TM_PATH_TNM_M;
    }

    public void setTM_PATH_TNM_M(String TM_PATH_TNM_M) {
        this.TM_PATH_TNM_M = TM_PATH_TNM_M != null ? TM_PATH_TNM_M : "N/A";
    }

    public String getTM_PATH_TNM_M_DESC() {
        return TM_PATH_TNM_M_DESC;
    }

    public void setTM_PATH_TNM_M_DESC(String TM_PATH_TNM_M_DESC) {
        this.TM_PATH_TNM_M_DESC = TM_PATH_TNM_M_DESC != null ? TM_PATH_TNM_M_DESC : "N/A";
    }

    public String getTM_PATH_STG_GRP() {
        return TM_PATH_STG_GRP;
    }

    public void setTM_PATH_STG_GRP(String TM_PATH_STG_GRP) {
        this.TM_PATH_STG_GRP = TM_PATH_STG_GRP != null ? TM_PATH_STG_GRP : "N/A";
    }

    public String getTM_PATH_RPT_AV() {
        return TM_PATH_RPT_AV;
    }

    public void setTM_PATH_RPT_AV(String TM_PATH_RPT_AV) {
        this.TM_PATH_RPT_AV = TM_PATH_RPT_AV != null ? TM_PATH_RPT_AV : "N/A";
    }

    public String getTM_CA_STS_AT_ACC() {
        return TM_CA_STS_AT_ACC;
    }

    public void setTM_CA_STS_AT_ACC(String TM_CA_STS_AT_ACC) {
        this.TM_CA_STS_AT_ACC = TM_CA_STS_AT_ACC != null ? TM_CA_STS_AT_ACC : "N/A";
    }

    public String getTM_FIRST_RECUR_YEAR() {
        return TM_FIRST_RECUR_YEAR;
    }

    public void setTM_FIRST_RECUR_YEAR(String TM_FIRST_RECUR_YEAR) {
        this.TM_FIRST_RECUR_YEAR = TM_FIRST_RECUR_YEAR != null ? TM_FIRST_RECUR_YEAR : "N/A";
    }

    public String getAGE_AT_TM_FIRST_RECUR_DATE_IN_DAYS() {
        return AGE_AT_TM_FIRST_RECUR_DATE_IN_DAYS;
    }

    public void setAGE_AT_TM_FIRST_RECUR_DATE_IN_DAYS(String AGE_AT_TM_FIRST_RECUR_DATE_IN_DAYS) {
        this.AGE_AT_TM_FIRST_RECUR_DATE_IN_DAYS = AGE_AT_TM_FIRST_RECUR_DATE_IN_DAYS != null ? AGE_AT_TM_FIRST_RECUR_DATE_IN_DAYS : "N/A";
    }

    public String getTM_FIRST_RECUR_TYP() {
        return TM_FIRST_RECUR_TYP;
    }

    public void setTM_FIRST_RECUR_TYP(String TM_FIRST_RECUR_TYP) {
        this.TM_FIRST_RECUR_TYP = TM_FIRST_RECUR_TYP != null ? TM_FIRST_RECUR_TYP : "N/A";
    }

    public String getTM_ADM_YEAR() {
        return TM_ADM_YEAR;
    }

    public void setTM_ADM_YEAR(String TM_ADM_YEAR) {
        this.TM_ADM_YEAR = TM_ADM_YEAR != null ? TM_ADM_YEAR : "N/A";
    }

    public String getAGE_AT_TM_ADM_DATE_IN_DAYS() {
        return AGE_AT_TM_ADM_DATE_IN_DAYS;
    }

    public void setAGE_AT_TM_ADM_DATE_IN_DAYS(String AGE_AT_TM_ADM_DATE_IN_DAYS) {
        this.AGE_AT_TM_ADM_DATE_IN_DAYS = AGE_AT_TM_ADM_DATE_IN_DAYS != null ? AGE_AT_TM_ADM_DATE_IN_DAYS : "N/A";
    }

    public String getTM_DSCH_YEAR() {
        return TM_DSCH_YEAR;
    }

    public void setTM_DSCH_YEAR(String TM_DSCH_YEAR) {
        this.TM_DSCH_YEAR = TM_DSCH_YEAR != null ? TM_DSCH_YEAR : "N/A";
    }

    public String getAGE_AT_TM_DSCH_DATE_IN_DAYS() {
        return AGE_AT_TM_DSCH_DATE_IN_DAYS;
    }

    public void setAGE_AT_TM_DSCH_DATE_IN_DAYS(String AGE_AT_TM_DSCH_DATE_IN_DAYS) {
        this.AGE_AT_TM_DSCH_DATE_IN_DAYS = AGE_AT_TM_DSCH_DATE_IN_DAYS != null ? AGE_AT_TM_DSCH_DATE_IN_DAYS : "N/A";
    }

    public String getTM_SURG_DSCH_YEAR() {
        return TM_SURG_DSCH_YEAR;
    }

    public void setTM_SURG_DSCH_YEAR(String TM_SURG_DSCH_YEAR) {
        this.TM_SURG_DSCH_YEAR = TM_SURG_DSCH_YEAR != null ? TM_SURG_DSCH_YEAR : "N/A";
    }

    public String getAGE_AT_TM_SURG_DSCH_DATE_IN_DAYS() {
        return AGE_AT_TM_SURG_DSCH_DATE_IN_DAYS;
    }

    public void setAGE_AT_TM_SURG_DSCH_DATE_IN_DAYS(String AGE_AT_TM_SURG_DSCH_DATE_IN_DAYS) {
        this.AGE_AT_TM_SURG_DSCH_DATE_IN_DAYS = AGE_AT_TM_SURG_DSCH_DATE_IN_DAYS != null ? AGE_AT_TM_SURG_DSCH_DATE_IN_DAYS : "N/A";
    }

    public String getTM_READM_WTHN_30D() {
        return TM_READM_WTHN_30D;
    }

    public void setTM_READM_WTHN_30D(String TM_READM_WTHN_30D) {
        this.TM_READM_WTHN_30D = TM_READM_WTHN_30D != null ? TM_READM_WTHN_30D : "N/A";
    }

    public String getTM_FIRST_TX_YEAR() {
        return TM_FIRST_TX_YEAR;
    }

    public void setTM_FIRST_TX_YEAR(String TM_FIRST_TX_YEAR) {
        this.TM_FIRST_TX_YEAR = TM_FIRST_TX_YEAR != null ? TM_FIRST_TX_YEAR : "N/A";
    }

    public String getAGE_AT_TM_FIRST_TX_DATE_IN_DAYS() {
        return AGE_AT_TM_FIRST_TX_DATE_IN_DAYS;
    }

    public void setAGE_AT_TM_FIRST_TX_DATE_IN_DAYS(String AGE_AT_TM_FIRST_TX_DATE_IN_DAYS) {
        this.AGE_AT_TM_FIRST_TX_DATE_IN_DAYS = AGE_AT_TM_FIRST_TX_DATE_IN_DAYS != null ? AGE_AT_TM_FIRST_TX_DATE_IN_DAYS : "N/A";
    }

    public String getTM_NON_CA_SURG_SUM() {
        return TM_NON_CA_SURG_SUM;
    }

    public void setTM_NON_CA_SURG_SUM(String TM_NON_CA_SURG_SUM) {
        this.TM_NON_CA_SURG_SUM = TM_NON_CA_SURG_SUM != null ? TM_NON_CA_SURG_SUM : "N/A";
    }

    public String getTM_NON_CA_SURG_SUM_DESC() {
        return TM_NON_CA_SURG_SUM_DESC;
    }

    public void setTM_NON_CA_SURG_SUM_DESC(String TM_NON_CA_SURG_SUM_DESC) {
        this.TM_NON_CA_SURG_SUM_DESC = TM_NON_CA_SURG_SUM_DESC != null ? TM_NON_CA_SURG_SUM_DESC : "N/A";
    }

    public String getTM_NON_CA_SURG_MSK() {
        return TM_NON_CA_SURG_MSK;
    }

    public void setTM_NON_CA_SURG_MSK(String TM_NON_CA_SURG_MSK) {
        this.TM_NON_CA_SURG_MSK = TM_NON_CA_SURG_MSK != null ? TM_NON_CA_SURG_MSK : "N/A";
    }

    public String getTM_NON_CA_SURG_YEAR() {
        return TM_NON_CA_SURG_YEAR;
    }

    public void setTM_NON_CA_SURG_YEAR(String TM_NON_CA_SURG_YEAR) {
        this.TM_NON_CA_SURG_YEAR = TM_NON_CA_SURG_YEAR != null ? TM_NON_CA_SURG_YEAR : "N/A";
    }

    public String getAGE_AT_TM_NON_CA_SURG_DATE_IN_DAYS() {
        return AGE_AT_TM_NON_CA_SURG_DATE_IN_DAYS;
    }

    public void setAGE_AT_TM_NON_CA_SURG_DATE_IN_DAYS(String AGE_AT_TM_NON_CA_SURG_DATE_IN_DAYS) {
        this.AGE_AT_TM_NON_CA_SURG_DATE_IN_DAYS = AGE_AT_TM_NON_CA_SURG_DATE_IN_DAYS != null ? AGE_AT_TM_NON_CA_SURG_DATE_IN_DAYS : "N/A";
    }

    public String getTM_CA_SURG_98() {
        return TM_CA_SURG_98;
    }

    public void setTM_CA_SURG_98(String TM_CA_SURG_98) {
        this.TM_CA_SURG_98 = TM_CA_SURG_98 != null ? TM_CA_SURG_98 : "N/A";
    }

    public String getTM_CA_SURG_98_MSK() {
        return TM_CA_SURG_98_MSK;
    }

    public void setTM_CA_SURG_98_MSK(String TM_CA_SURG_98_MSK) {
        this.TM_CA_SURG_98_MSK = TM_CA_SURG_98_MSK != null ? TM_CA_SURG_98_MSK : "N/A";
    }

    public String getTM_CA_SURG_03() {
        return TM_CA_SURG_03;
    }

    public void setTM_CA_SURG_03(String TM_CA_SURG_03) {
        this.TM_CA_SURG_03 = TM_CA_SURG_03 != null ? TM_CA_SURG_03 : "N/A";
    }

    public String getTM_CA_SURG_03_MSK() {
        return TM_CA_SURG_03_MSK;
    }

    public void setTM_CA_SURG_03_MSK(String TM_CA_SURG_03_MSK) {
        this.TM_CA_SURG_03_MSK = TM_CA_SURG_03_MSK != null ? TM_CA_SURG_03_MSK : "N/A";
    }

    public String getTM_OTH_SURG_98() {
        return TM_OTH_SURG_98;
    }

    public void setTM_OTH_SURG_98(String TM_OTH_SURG_98) {
        this.TM_OTH_SURG_98 = TM_OTH_SURG_98 != null ? TM_OTH_SURG_98 : "N/A";
    }

    public String getTM_OTH_SURG_98_MSK() {
        return TM_OTH_SURG_98_MSK;
    }

    public void setTM_OTH_SURG_98_MSK(String TM_OTH_SURG_98_MSK) {
        this.TM_OTH_SURG_98_MSK = TM_OTH_SURG_98_MSK != null ? TM_OTH_SURG_98_MSK : "N/A";
    }

    public String getTM_OTH_SURG_03() {
        return TM_OTH_SURG_03;
    }

    public void setTM_OTH_SURG_03(String TM_OTH_SURG_03) {
        this.TM_OTH_SURG_03 = TM_OTH_SURG_03 != null ? TM_OTH_SURG_03 : "N/A";
    }

    public String getTM_OTH_SURG_03_MSK() {
        return TM_OTH_SURG_03_MSK;
    }

    public void setTM_OTH_SURG_03_MSK(String TM_OTH_SURG_03_MSK) {
        this.TM_OTH_SURG_03_MSK = TM_OTH_SURG_03_MSK != null ? TM_OTH_SURG_03_MSK : "N/A";
    }

    public String getTM_OTH_SURG_CD() {
        return TM_OTH_SURG_CD;
    }

    public void setTM_OTH_SURG_CD(String TM_OTH_SURG_CD) {
        this.TM_OTH_SURG_CD = TM_OTH_SURG_CD != null ? TM_OTH_SURG_CD : "N/A";
    }

    public String getTM_OTH_SURG_CD_DESC() {
        return TM_OTH_SURG_CD_DESC;
    }

    public void setTM_OTH_SURG_CD_DESC(String TM_OTH_SURG_CD_DESC) {
        this.TM_OTH_SURG_CD_DESC = TM_OTH_SURG_CD_DESC != null ? TM_OTH_SURG_CD_DESC : "N/A";
    }

    public String getTM_RGN_SCOP_98() {
        return TM_RGN_SCOP_98;
    }

    public void setTM_RGN_SCOP_98(String TM_RGN_SCOP_98) {
        this.TM_RGN_SCOP_98 = TM_RGN_SCOP_98 != null ? TM_RGN_SCOP_98 : "N/A";
    }

    public String getTM_RGN_SCOP_98_MSK() {
        return TM_RGN_SCOP_98_MSK;
    }

    public void setTM_RGN_SCOP_98_MSK(String TM_RGN_SCOP_98_MSK) {
        this.TM_RGN_SCOP_98_MSK = TM_RGN_SCOP_98_MSK != null ? TM_RGN_SCOP_98_MSK : "N/A";
    }

    public String getTM_RGN_SCOP_03() {
        return TM_RGN_SCOP_03;
    }

    public void setTM_RGN_SCOP_03(String TM_RGN_SCOP_03) {
        this.TM_RGN_SCOP_03 = TM_RGN_SCOP_03 != null ? TM_RGN_SCOP_03 : "N/A";
    }

    public String getTM_RGN_SCOP_03_MSK() {
        return TM_RGN_SCOP_03_MSK;
    }

    public void setTM_RGN_SCOP_03_MSK(String TM_RGN_SCOP_03_MSK) {
        this.TM_RGN_SCOP_03_MSK = TM_RGN_SCOP_03_MSK != null ? TM_RGN_SCOP_03_MSK : "N/A";
    }

    public String getTM_REGNODE_SCOP_CD() {
        return TM_REGNODE_SCOP_CD;
    }

    public void setTM_REGNODE_SCOP_CD(String TM_REGNODE_SCOP_CD) {
        this.TM_REGNODE_SCOP_CD = TM_REGNODE_SCOP_CD != null ? TM_REGNODE_SCOP_CD : "N/A";
    }

    public String getTM_REGNODE_SCOP_DESC() {
        return TM_REGNODE_SCOP_DESC;
    }

    public void setTM_REGNODE_SCOP_DESC(String TM_REGNODE_SCOP_DESC) {
        this.TM_REGNODE_SCOP_DESC = TM_REGNODE_SCOP_DESC != null ? TM_REGNODE_SCOP_DESC : "N/A";
    }

    public String getTM_RECON_SURG() {
        return TM_RECON_SURG;
    }

    public void setTM_RECON_SURG(String TM_RECON_SURG) {
        this.TM_RECON_SURG = TM_RECON_SURG != null ? TM_RECON_SURG : "N/A";
    }

    public String getTM_RECON_SURG_DESC() {
        return TM_RECON_SURG_DESC;
    }

    public void setTM_RECON_SURG_DESC(String TM_RECON_SURG_DESC) {
        this.TM_RECON_SURG_DESC = TM_RECON_SURG_DESC != null ? TM_RECON_SURG_DESC : "N/A";
    }

    public String getTM_CA_SURG_YEAR() {
        return TM_CA_SURG_YEAR;
    }

    public void setTM_CA_SURG_YEAR(String TM_CA_SURG_YEAR) {
        this.TM_CA_SURG_YEAR = TM_CA_SURG_YEAR != null ? TM_CA_SURG_YEAR : "N/A";
    }

    public String getAGE_AT_TM_CA_SURG_DATE_IN_DAYS() {
        return AGE_AT_TM_CA_SURG_DATE_IN_DAYS;
    }

    public void setAGE_AT_TM_CA_SURG_DATE_IN_DAYS(String AGE_AT_TM_CA_SURG_DATE_IN_DAYS) {
        this.AGE_AT_TM_CA_SURG_DATE_IN_DAYS = AGE_AT_TM_CA_SURG_DATE_IN_DAYS != null ? AGE_AT_TM_CA_SURG_DATE_IN_DAYS : "N/A";
    }

    public String getTM_SURG_DEF_YEAR() {
        return TM_SURG_DEF_YEAR;
    }

    public void setTM_SURG_DEF_YEAR(String TM_SURG_DEF_YEAR) {
        this.TM_SURG_DEF_YEAR = TM_SURG_DEF_YEAR != null ? TM_SURG_DEF_YEAR : "N/A";
    }

    public String getAGE_AT_TM_SURG_DEF_DATE_IN_DAYS() {
        return AGE_AT_TM_SURG_DEF_DATE_IN_DAYS;
    }

    public void setAGE_AT_TM_SURG_DEF_DATE_IN_DAYS(String AGE_AT_TM_SURG_DEF_DATE_IN_DAYS) {
        this.AGE_AT_TM_SURG_DEF_DATE_IN_DAYS = AGE_AT_TM_SURG_DEF_DATE_IN_DAYS != null ? AGE_AT_TM_SURG_DEF_DATE_IN_DAYS : "N/A";
    }

    public String getTM_REASON_NO_SURG() {
        return TM_REASON_NO_SURG;
    }

    public void setTM_REASON_NO_SURG(String TM_REASON_NO_SURG) {
        this.TM_REASON_NO_SURG = TM_REASON_NO_SURG != null ? TM_REASON_NO_SURG : "N/A";
    }

    public String getREASON_NO_SURG_DESC() {
        return REASON_NO_SURG_DESC;
    }

    public void setREASON_NO_SURG_DESC(String REASON_NO_SURG_DESC) {
        this.REASON_NO_SURG_DESC = REASON_NO_SURG_DESC != null ? REASON_NO_SURG_DESC : "N/A";
    }

    public String getTM_PRIM_SURGEON() {
        return TM_PRIM_SURGEON;
    }

    public void setTM_PRIM_SURGEON(String TM_PRIM_SURGEON) {
        this.TM_PRIM_SURGEON = TM_PRIM_SURGEON != null ? TM_PRIM_SURGEON : "N/A";
    }

    public String getTM_PRIM_SURGEON_NAME() {
        return TM_PRIM_SURGEON_NAME;
    }

    public void setTM_PRIM_SURGEON_NAME(String TM_PRIM_SURGEON_NAME) {
        this.TM_PRIM_SURGEON_NAME = TM_PRIM_SURGEON_NAME != null ? TM_PRIM_SURGEON_NAME : "N/A";
    }

    public String getTM_ATN_DR_NO() {
        return TM_ATN_DR_NO;
    }

    public void setTM_ATN_DR_NO(String TM_ATN_DR_NO) {
        this.TM_ATN_DR_NO = TM_ATN_DR_NO != null ? TM_ATN_DR_NO : "N/A";
    }

    public String getTM_ATN_DR_NAME() {
        return TM_ATN_DR_NAME;
    }

    public void setTM_ATN_DR_NAME(String TM_ATN_DR_NAME) {
        this.TM_ATN_DR_NAME = TM_ATN_DR_NAME != null ? TM_ATN_DR_NAME : "N/A";
    }

    public String getTM_REASON_NO_RAD() {
        return TM_REASON_NO_RAD;
    }

    public void setTM_REASON_NO_RAD(String TM_REASON_NO_RAD) {
        this.TM_REASON_NO_RAD = TM_REASON_NO_RAD != null ? TM_REASON_NO_RAD : "N/A";
    }

    public String getTM_REASON_NO_RAD_DESC() {
        return TM_REASON_NO_RAD_DESC;
    }

    public void setTM_REASON_NO_RAD_DESC(String TM_REASON_NO_RAD_DESC) {
        this.TM_REASON_NO_RAD_DESC = TM_REASON_NO_RAD_DESC != null ? TM_REASON_NO_RAD_DESC : "N/A";
    }

    public String getTM_RAD_STRT_YEAR() {
        return TM_RAD_STRT_YEAR;
    }

    public void setTM_RAD_STRT_YEAR(String TM_RAD_STRT_YEAR) {
        this.TM_RAD_STRT_YEAR = TM_RAD_STRT_YEAR != null ? TM_RAD_STRT_YEAR : "N/A";
    }

    public String getAGE_AT_TM_RAD_STRT_DATE_IN_DAYS() {
        return AGE_AT_TM_RAD_STRT_DATE_IN_DAYS;
    }

    public void setAGE_AT_TM_RAD_STRT_DATE_IN_DAYS(String AGE_AT_TM_RAD_STRT_DATE_IN_DAYS) {
        this.AGE_AT_TM_RAD_STRT_DATE_IN_DAYS = AGE_AT_TM_RAD_STRT_DATE_IN_DAYS != null ? AGE_AT_TM_RAD_STRT_DATE_IN_DAYS : "N/A";
    }

    public String getTM_RAD_END_YEAR() {
        return TM_RAD_END_YEAR;
    }

    public void setTM_RAD_END_YEAR(String TM_RAD_END_YEAR) {
        this.TM_RAD_END_YEAR = TM_RAD_END_YEAR != null ? TM_RAD_END_YEAR : "N/A";
    }

    public String getAGE_AT_TM_RAD_END_DATE_IN_DAYS() {
        return AGE_AT_TM_RAD_END_DATE_IN_DAYS;
    }

    public void setAGE_AT_TM_RAD_END_DATE_IN_DAYS(String AGE_AT_TM_RAD_END_DATE_IN_DAYS) {
        this.AGE_AT_TM_RAD_END_DATE_IN_DAYS = AGE_AT_TM_RAD_END_DATE_IN_DAYS != null ? AGE_AT_TM_RAD_END_DATE_IN_DAYS : "N/A";
    }

    public String getTM_RAD_TX_MOD() {
        return TM_RAD_TX_MOD;
    }

    public void setTM_RAD_TX_MOD(String TM_RAD_TX_MOD) {
        this.TM_RAD_TX_MOD = TM_RAD_TX_MOD != null ? TM_RAD_TX_MOD : "N/A";
    }

    public String getTM_RAD_TX_MOD_DESC() {
        return TM_RAD_TX_MOD_DESC;
    }

    public void setTM_RAD_TX_MOD_DESC(String TM_RAD_TX_MOD_DESC) {
        this.TM_RAD_TX_MOD_DESC = TM_RAD_TX_MOD_DESC != null ? TM_RAD_TX_MOD_DESC : "N/A";
    }

    public String getTM_BOOST_RAD_MOD() {
        return TM_BOOST_RAD_MOD;
    }

    public void setTM_BOOST_RAD_MOD(String TM_BOOST_RAD_MOD) {
        this.TM_BOOST_RAD_MOD = TM_BOOST_RAD_MOD != null ? TM_BOOST_RAD_MOD : "N/A";
    }

    public String getTM_BOOST_RAD_MOD_DESC() {
        return TM_BOOST_RAD_MOD_DESC;
    }

    public void setTM_BOOST_RAD_MOD_DESC(String TM_BOOST_RAD_MOD_DESC) {
        this.TM_BOOST_RAD_MOD_DESC = TM_BOOST_RAD_MOD_DESC != null ? TM_BOOST_RAD_MOD_DESC : "N/A";
    }

    public String getTM_LOC_RAD_TX() {
        return TM_LOC_RAD_TX;
    }

    public void setTM_LOC_RAD_TX(String TM_LOC_RAD_TX) {
        this.TM_LOC_RAD_TX = TM_LOC_RAD_TX != null ? TM_LOC_RAD_TX : "N/A";
    }

    public String getTM_LOC_RAD_TX_DESC() {
        return TM_LOC_RAD_TX_DESC;
    }

    public void setTM_LOC_RAD_TX_DESC(String TM_LOC_RAD_TX_DESC) {
        this.TM_LOC_RAD_TX_DESC = TM_LOC_RAD_TX_DESC != null ? TM_LOC_RAD_TX_DESC : "N/A";
    }

    public String getTM_RAD_TX_VOL() {
        return TM_RAD_TX_VOL;
    }

    public void setTM_RAD_TX_VOL(String TM_RAD_TX_VOL) {
        this.TM_RAD_TX_VOL = TM_RAD_TX_VOL != null ? TM_RAD_TX_VOL : "N/A";
    }

    public String getTM_RAD_TX_VOL_DESC() {
        return TM_RAD_TX_VOL_DESC;
    }

    public void setTM_RAD_TX_VOL_DESC(String TM_RAD_TX_VOL_DESC) {
        this.TM_RAD_TX_VOL_DESC = TM_RAD_TX_VOL_DESC != null ? TM_RAD_TX_VOL_DESC : "N/A";
    }

    public String getTM_NUM_TX_THIS_VOL() {
        return TM_NUM_TX_THIS_VOL;
    }

    public void setTM_NUM_TX_THIS_VOL(String TM_NUM_TX_THIS_VOL) {
        this.TM_NUM_TX_THIS_VOL = TM_NUM_TX_THIS_VOL != null ? TM_NUM_TX_THIS_VOL : "N/A";
    }

    public String getTM_NUM_TX_THIS_VOL_DESC() {
        return TM_NUM_TX_THIS_VOL_DESC;
    }

    public void setTM_NUM_TX_THIS_VOL_DESC(String TM_NUM_TX_THIS_VOL_DESC) {
        this.TM_NUM_TX_THIS_VOL_DESC = TM_NUM_TX_THIS_VOL_DESC != null ? TM_NUM_TX_THIS_VOL_DESC : "N/A";
    }

    public String getTM_REG_RAD_DOSE() {
        return TM_REG_RAD_DOSE;
    }

    public void setTM_REG_RAD_DOSE(String TM_REG_RAD_DOSE) {
        this.TM_REG_RAD_DOSE = TM_REG_RAD_DOSE != null ? TM_REG_RAD_DOSE : "N/A";
    }

    public String getTM_REG_RAD_DOSE_DESC() {
        return TM_REG_RAD_DOSE_DESC;
    }

    public void setTM_REG_RAD_DOSE_DESC(String TM_REG_RAD_DOSE_DESC) {
        this.TM_REG_RAD_DOSE_DESC = TM_REG_RAD_DOSE_DESC != null ? TM_REG_RAD_DOSE_DESC : "N/A";
    }

    public String getTM_BOOST_RAD_DOSE() {
        return TM_BOOST_RAD_DOSE;
    }

    public void setTM_BOOST_RAD_DOSE(String TM_BOOST_RAD_DOSE) {
        this.TM_BOOST_RAD_DOSE = TM_BOOST_RAD_DOSE != null ? TM_BOOST_RAD_DOSE : "N/A";
    }

    public String getTM_BOOST_RAD_DOSE_DESC() {
        return TM_BOOST_RAD_DOSE_DESC;
    }

    public void setTM_BOOST_RAD_DOSE_DESC(String TM_BOOST_RAD_DOSE_DESC) {
        this.TM_BOOST_RAD_DOSE_DESC = TM_BOOST_RAD_DOSE_DESC != null ? TM_BOOST_RAD_DOSE_DESC : "N/A";
    }

    public String getTM_RAD_SURG_SEQ() {
        return TM_RAD_SURG_SEQ;
    }

    public void setTM_RAD_SURG_SEQ(String TM_RAD_SURG_SEQ) {
        this.TM_RAD_SURG_SEQ = TM_RAD_SURG_SEQ != null ? TM_RAD_SURG_SEQ : "N/A";
    }

    public String getTM_RAD_SURG_SEQ_DESC() {
        return TM_RAD_SURG_SEQ_DESC;
    }

    public void setTM_RAD_SURG_SEQ_DESC(String TM_RAD_SURG_SEQ_DESC) {
        this.TM_RAD_SURG_SEQ_DESC = TM_RAD_SURG_SEQ_DESC != null ? TM_RAD_SURG_SEQ_DESC : "N/A";
    }

    public String getTM_RAD_MD() {
        return TM_RAD_MD;
    }

    public void setTM_RAD_MD(String TM_RAD_MD) {
        this.TM_RAD_MD = TM_RAD_MD != null ? TM_RAD_MD : "N/A";
    }

    public String getTM_RAD_MD_NAME() {
        return TM_RAD_MD_NAME;
    }

    public void setTM_RAD_MD_NAME(String TM_RAD_MD_NAME) {
        this.TM_RAD_MD_NAME = TM_RAD_MD_NAME != null ? TM_RAD_MD_NAME : "N/A";
    }

    public String getTM_SYST_STRT_YEAR() {
        return TM_SYST_STRT_YEAR;
    }

    public void setTM_SYST_STRT_YEAR(String TM_SYST_STRT_YEAR) {
        this.TM_SYST_STRT_YEAR = TM_SYST_STRT_YEAR != null ? TM_SYST_STRT_YEAR : "N/A";
    }

    public String getAGE_AT_TM_SYST_STRT_DATE_IN_DAYS() {
        return AGE_AT_TM_SYST_STRT_DATE_IN_DAYS;
    }

    public void setAGE_AT_TM_SYST_STRT_DATE_IN_DAYS(String AGE_AT_TM_SYST_STRT_DATE_IN_DAYS) {
        this.AGE_AT_TM_SYST_STRT_DATE_IN_DAYS = AGE_AT_TM_SYST_STRT_DATE_IN_DAYS != null ? AGE_AT_TM_SYST_STRT_DATE_IN_DAYS : "N/A";
    }

    public String getTM_OTH_STRT_YEAR() {
        return TM_OTH_STRT_YEAR;
    }

    public void setTM_OTH_STRT_YEAR(String TM_OTH_STRT_YEAR) {
        this.TM_OTH_STRT_YEAR = TM_OTH_STRT_YEAR != null ? TM_OTH_STRT_YEAR : "N/A";
    }

    public String getAGE_AT_TM_OTH_STRT_DATE_IN_DAYS() {
        return AGE_AT_TM_OTH_STRT_DATE_IN_DAYS;
    }

    public void setAGE_AT_TM_OTH_STRT_DATE_IN_DAYS(String AGE_AT_TM_OTH_STRT_DATE_IN_DAYS) {
        this.AGE_AT_TM_OTH_STRT_DATE_IN_DAYS = AGE_AT_TM_OTH_STRT_DATE_IN_DAYS != null ? AGE_AT_TM_OTH_STRT_DATE_IN_DAYS : "N/A";
    }

    public String getTM_CHEM_SUM() {
        return TM_CHEM_SUM;
    }

    public void setTM_CHEM_SUM(String TM_CHEM_SUM) {
        this.TM_CHEM_SUM = TM_CHEM_SUM != null ? TM_CHEM_SUM : "N/A";
    }

    public String getTM_CHEM_SUM_DESC() {
        return TM_CHEM_SUM_DESC;
    }

    public void setTM_CHEM_SUM_DESC(String TM_CHEM_SUM_DESC) {
        this.TM_CHEM_SUM_DESC = TM_CHEM_SUM_DESC != null ? TM_CHEM_SUM_DESC : "N/A";
    }

    public String getTM_CHEM_SUM_MSK() {
        return TM_CHEM_SUM_MSK;
    }

    public void setTM_CHEM_SUM_MSK(String TM_CHEM_SUM_MSK) {
        this.TM_CHEM_SUM_MSK = TM_CHEM_SUM_MSK != null ? TM_CHEM_SUM_MSK : "N/A";
    }

    public String getTM_CHEM_SUM_MSK_DESC() {
        return TM_CHEM_SUM_MSK_DESC;
    }

    public void setTM_CHEM_SUM_MSK_DESC(String TM_CHEM_SUM_MSK_DESC) {
        this.TM_CHEM_SUM_MSK_DESC = TM_CHEM_SUM_MSK_DESC != null ? TM_CHEM_SUM_MSK_DESC : "N/A";
    }

    public String getTM_TUMOR_SEQ() {
        return TM_TUMOR_SEQ;
    }

    public void setTM_TUMOR_SEQ(String TM_TUMOR_SEQ) {
        this.TM_TUMOR_SEQ = TM_TUMOR_SEQ != null ? TM_TUMOR_SEQ : "N/A";
    }

    public String getTM_HORM_SUM() {
        return TM_HORM_SUM;
    }

    public void setTM_HORM_SUM(String TM_HORM_SUM) {
        this.TM_HORM_SUM = TM_HORM_SUM != null ? TM_HORM_SUM : "N/A";
    }

    public String getTM_HORM_SUM_DESC() {
        return TM_HORM_SUM_DESC;
    }

    public void setTM_HORM_SUM_DESC(String TM_HORM_SUM_DESC) {
        this.TM_HORM_SUM_DESC = TM_HORM_SUM_DESC != null ? TM_HORM_SUM_DESC : "N/A";
    }

    public String getTM_HORM_SUM_MSK() {
        return TM_HORM_SUM_MSK;
    }

    public void setTM_HORM_SUM_MSK(String TM_HORM_SUM_MSK) {
        this.TM_HORM_SUM_MSK = TM_HORM_SUM_MSK != null ? TM_HORM_SUM_MSK : "N/A";
    }

    public String getTM_HORM_SUM_MSK_DESC() {
        return TM_HORM_SUM_MSK_DESC;
    }

    public void setTM_HORM_SUM_MSK_DESC(String TM_HORM_SUM_MSK_DESC) {
        this.TM_HORM_SUM_MSK_DESC = TM_HORM_SUM_MSK_DESC != null ? TM_HORM_SUM_MSK_DESC : "N/A";
    }

    public String getTM_BRM_SUM() {
        return TM_BRM_SUM;
    }

    public void setTM_BRM_SUM(String TM_BRM_SUM) {
        this.TM_BRM_SUM = TM_BRM_SUM != null ? TM_BRM_SUM : "N/A";
    }

    public String getTM_BRM_SUM_DESC() {
        return TM_BRM_SUM_DESC;
    }

    public void setTM_BRM_SUM_DESC(String TM_BRM_SUM_DESC) {
        this.TM_BRM_SUM_DESC = TM_BRM_SUM_DESC != null ? TM_BRM_SUM_DESC : "N/A";
    }

    public String getTM_BRM_SUM_MSK() {
        return TM_BRM_SUM_MSK;
    }

    public void setTM_BRM_SUM_MSK(String TM_BRM_SUM_MSK) {
        this.TM_BRM_SUM_MSK = TM_BRM_SUM_MSK != null ? TM_BRM_SUM_MSK : "N/A";
    }

    public String getTM_BRM_SUM_MSK_DESC() {
        return TM_BRM_SUM_MSK_DESC;
    }

    public void setTM_BRM_SUM_MSK_DESC(String TM_BRM_SUM_MSK_DESC) {
        this.TM_BRM_SUM_MSK_DESC = TM_BRM_SUM_MSK_DESC != null ? TM_BRM_SUM_MSK_DESC : "N/A";
    }

    public String getTM_OTH_SUM() {
        return TM_OTH_SUM;
    }

    public void setTM_OTH_SUM(String TM_OTH_SUM) {
        this.TM_OTH_SUM = TM_OTH_SUM != null ? TM_OTH_SUM : "N/A";
    }

    public String getTM_OTH_SUM_DESC() {
        return TM_OTH_SUM_DESC;
    }

    public void setTM_OTH_SUM_DESC(String TM_OTH_SUM_DESC) {
        this.TM_OTH_SUM_DESC = TM_OTH_SUM_DESC != null ? TM_OTH_SUM_DESC : "N/A";
    }

    public String getTM_OTH_SUM_MSK() {
        return TM_OTH_SUM_MSK;
    }

    public void setTM_OTH_SUM_MSK(String TM_OTH_SUM_MSK) {
        this.TM_OTH_SUM_MSK = TM_OTH_SUM_MSK != null ? TM_OTH_SUM_MSK : "N/A";
    }

    public String getTM_OTH_SUM_MSK_DESC() {
        return TM_OTH_SUM_MSK_DESC;
    }

    public void setTM_OTH_SUM_MSK_DESC(String TM_OTH_SUM_MSK_DESC) {
        this.TM_OTH_SUM_MSK_DESC = TM_OTH_SUM_MSK_DESC != null ? TM_OTH_SUM_MSK_DESC : "N/A";
    }

    public String getTM_PALLIA_PROC() {
        return TM_PALLIA_PROC;
    }

    public void setTM_PALLIA_PROC(String TM_PALLIA_PROC) {
        this.TM_PALLIA_PROC = TM_PALLIA_PROC != null ? TM_PALLIA_PROC : "N/A";
    }

    public String getTM_PALLIA_PROC_DESC() {
        return TM_PALLIA_PROC_DESC;
    }

    public void setTM_PALLIA_PROC_DESC(String TM_PALLIA_PROC_DESC) {
        this.TM_PALLIA_PROC_DESC = TM_PALLIA_PROC_DESC != null ? TM_PALLIA_PROC_DESC : "N/A";
    }

    public String getTM_PALLIA_PROC_MSK() {
        return TM_PALLIA_PROC_MSK;
    }

    public void setTM_PALLIA_PROC_MSK(String TM_PALLIA_PROC_MSK) {
        this.TM_PALLIA_PROC_MSK = TM_PALLIA_PROC_MSK != null ? TM_PALLIA_PROC_MSK : "N/A";
    }

    public String getTM_PALLIA_PROC_MSK_DESC() {
        return TM_PALLIA_PROC_MSK_DESC;
    }

    public void setTM_PALLIA_PROC_MSK_DESC(String TM_PALLIA_PROC_MSK_DESC) {
        this.TM_PALLIA_PROC_MSK_DESC = TM_PALLIA_PROC_MSK_DESC != null ? TM_PALLIA_PROC_MSK_DESC : "N/A";
    }

    public String getTM_ONCOLOGY_MD() {
        return TM_ONCOLOGY_MD;
    }

    public void setTM_ONCOLOGY_MD(String TM_ONCOLOGY_MD) {
        this.TM_ONCOLOGY_MD = TM_ONCOLOGY_MD != null ? TM_ONCOLOGY_MD : "N/A";
    }

    public String getTM_ONCOLOGY_MD_NAME() {
        return TM_ONCOLOGY_MD_NAME;
    }

    public void setTM_ONCOLOGY_MD_NAME(String TM_ONCOLOGY_MD_NAME) {
        this.TM_ONCOLOGY_MD_NAME = TM_ONCOLOGY_MD_NAME != null ? TM_ONCOLOGY_MD_NAME : "N/A";
    }

    public String getTM_PRCS_YEAR() {
        return TM_PRCS_YEAR;
    }

    public void setTM_PRCS_YEAR(String TM_PRCS_YEAR) {
        this.TM_PRCS_YEAR = TM_PRCS_YEAR != null ? TM_PRCS_YEAR : "N/A";
    }

    public String getAGE_AT_TM_PRCS_DATE_IN_DAYS() {
        return AGE_AT_TM_PRCS_DATE_IN_DAYS;
    }

    public void setAGE_AT_TM_PRCS_DATE_IN_DAYS(String AGE_AT_TM_PRCS_DATE_IN_DAYS) {
        this.AGE_AT_TM_PRCS_DATE_IN_DAYS = AGE_AT_TM_PRCS_DATE_IN_DAYS != null ? AGE_AT_TM_PRCS_DATE_IN_DAYS : "N/A";
    }

    public String getTM_PATH_TEXT() {
        return TM_PATH_TEXT;
    }

    public void setTM_PATH_TEXT(String TM_PATH_TEXT) {
        this.TM_PATH_TEXT = TM_PATH_TEXT != null ? TM_PATH_TEXT : "N/A";
    }

    public String getTM_SURG_TEXT() {
        return TM_SURG_TEXT;
    }

    public void setTM_SURG_TEXT(String TM_SURG_TEXT) {
        this.TM_SURG_TEXT = TM_SURG_TEXT != null ? TM_SURG_TEXT : "N/A";
    }

    public String getTM_OVERRIDE_COM() {
        return TM_OVERRIDE_COM;
    }

    public void setTM_OVERRIDE_COM(String TM_OVERRIDE_COM) {
        this.TM_OVERRIDE_COM = TM_OVERRIDE_COM != null ? TM_OVERRIDE_COM : "N/A";
    }

    public String getTM_CSSIZE() {
        return TM_CSSIZE;
    }

    public void setTM_CSSIZE(String TM_CSSIZE) {
        this.TM_CSSIZE = TM_CSSIZE != null ? TM_CSSIZE : "N/A";
    }

    public String getTM_CSEXT() {
        return TM_CSEXT;
    }

    public void setTM_CSEXT(String TM_CSEXT) {
        this.TM_CSEXT = TM_CSEXT != null ? TM_CSEXT : "N/A";
    }

    public String getTM_CSEXTEV() {
        return TM_CSEXTEV;
    }

    public void setTM_CSEXTEV(String TM_CSEXTEV) {
        this.TM_CSEXTEV = TM_CSEXTEV != null ? TM_CSEXTEV : "N/A";
    }

    public String getTM_CSLMND() {
        return TM_CSLMND;
    }

    public void setTM_CSLMND(String TM_CSLMND) {
        this.TM_CSLMND = TM_CSLMND != null ? TM_CSLMND : "N/A";
    }

    public String getTM_CSRGNEV() {
        return TM_CSRGNEV;
    }

    public void setTM_CSRGNEV(String TM_CSRGNEV) {
        this.TM_CSRGNEV = TM_CSRGNEV != null ? TM_CSRGNEV : "N/A";
    }

    public String getTM_CSMETDX() {
        return TM_CSMETDX;
    }

    public void setTM_CSMETDX(String TM_CSMETDX) {
        this.TM_CSMETDX = TM_CSMETDX != null ? TM_CSMETDX : "N/A";
    }

    public String getTM_CSMETEV() {
        return TM_CSMETEV;
    }

    public void setTM_CSMETEV(String TM_CSMETEV) {
        this.TM_CSMETEV = TM_CSMETEV != null ? TM_CSMETEV : "N/A";
    }

    public String getTM_TSTAGE() {
        return TM_TSTAGE;
    }

    public void setTM_TSTAGE(String TM_TSTAGE) {
        this.TM_TSTAGE = TM_TSTAGE != null ? TM_TSTAGE : "N/A";
    }

    public String getTM_TSTAGE_DESC() {
        return TM_TSTAGE_DESC;
    }

    public void setTM_TSTAGE_DESC(String TM_TSTAGE_DESC) {
        this.TM_TSTAGE_DESC = TM_TSTAGE_DESC != null ? TM_TSTAGE_DESC : "N/A";
    }

    public String getTM_NSTAGE() {
        return TM_NSTAGE;
    }

    public void setTM_NSTAGE(String TM_NSTAGE) {
        this.TM_NSTAGE = TM_NSTAGE != null ? TM_NSTAGE : "N/A";
    }

    public String getTM_NSTAGE_DESC() {
        return TM_NSTAGE_DESC;
    }

    public void setTM_NSTAGE_DESC(String TM_NSTAGE_DESC) {
        this.TM_NSTAGE_DESC = TM_NSTAGE_DESC != null ? TM_NSTAGE_DESC : "N/A";
    }

    public String getTM_MSTAGE() {
        return TM_MSTAGE;
    }

    public void setTM_MSTAGE(String TM_MSTAGE) {
        this.TM_MSTAGE = TM_MSTAGE != null ? TM_MSTAGE : "N/A";
    }

    public String getTM_MSTAGE_DESC() {
        return TM_MSTAGE_DESC;
    }

    public void setTM_MSTAGE_DESC(String TM_MSTAGE_DESC) {
        this.TM_MSTAGE_DESC = TM_MSTAGE_DESC != null ? TM_MSTAGE_DESC : "N/A";
    }

    public String getTM_TBASIS() {
        return TM_TBASIS;
    }

    public void setTM_TBASIS(String TM_TBASIS) {
        this.TM_TBASIS = TM_TBASIS != null ? TM_TBASIS : "N/A";
    }

    public String getTM_TBASIS_DESC() {
        return TM_TBASIS_DESC;
    }

    public void setTM_TBASIS_DESC(String TM_TBASIS_DESC) {
        this.TM_TBASIS_DESC = TM_TBASIS_DESC != null ? TM_TBASIS_DESC : "N/A";
    }

    public String getTM_NBASIS() {
        return TM_NBASIS;
    }

    public void setTM_NBASIS(String TM_NBASIS) {
        this.TM_NBASIS = TM_NBASIS != null ? TM_NBASIS : "N/A";
    }

    public String getTM_NBASIS_DESC() {
        return TM_NBASIS_DESC;
    }

    public void setTM_NBASIS_DESC(String TM_NBASIS_DESC) {
        this.TM_NBASIS_DESC = TM_NBASIS_DESC != null ? TM_NBASIS_DESC : "N/A";
    }

    public String getTM_MBASIS() {
        return TM_MBASIS;
    }

    public void setTM_MBASIS(String TM_MBASIS) {
        this.TM_MBASIS = TM_MBASIS != null ? TM_MBASIS : "N/A";
    }

    public String getTM_MBASIS_DESC() {
        return TM_MBASIS_DESC;
    }

    public void setTM_MBASIS_DESC(String TM_MBASIS_DESC) {
        this.TM_MBASIS_DESC = TM_MBASIS_DESC != null ? TM_MBASIS_DESC : "N/A";
    }

    public String getTM_AJCC() {
        return TM_AJCC;
    }

    public void setTM_AJCC(String TM_AJCC) {
        this.TM_AJCC = TM_AJCC != null ? TM_AJCC : "N/A";
    }

    public String getTM_AJCC_DESC() {
        return TM_AJCC_DESC;
    }

    public void setTM_AJCC_DESC(String TM_AJCC_DESC) {
        this.TM_AJCC_DESC = TM_AJCC_DESC != null ? TM_AJCC_DESC : "N/A";
    }

    public String getTM_MSK_STG() {
        return TM_MSK_STG;
    }

    public void setTM_MSK_STG(String TM_MSK_STG) {
        this.TM_MSK_STG = TM_MSK_STG != null ? TM_MSK_STG : "N/A";
    }
    
    @Override
    public String toString(){
        return ToStringBuilder.reflectionToString(this);
    }

    public List<String> getFieldNames(){
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PT_ID_ICDO");
        fieldNames.add("DMP_ID_ICDO");
        fieldNames.add("TM_TUMOR_SEQ_DESC");
        fieldNames.add("TM_ACC_YEAR");
        fieldNames.add("TM_FIRST_MSK_YEAR");
        fieldNames.add("AGE_AT_TM_FIRST_MSK_DATE_IN_DAYS");
        fieldNames.add("TM_CASE_STS");
        fieldNames.add("TM_CASE_STS_DESC");
        fieldNames.add("TM_CASE_EFF_YEAR");
        fieldNames.add("AGE_AT_TM_CASE_EFF_DATE_IN_DAYS");
        fieldNames.add("TM_STATE_AT_DX");
        fieldNames.add("TM_SMOKING_HX");
        fieldNames.add("TM_SMOKING_HX_DESC");
        fieldNames.add("TM_OCCUPATION");
        fieldNames.add("TM_OCCUPATION_DESC");
        fieldNames.add("TM_FACILITY_FROM");
        fieldNames.add("FACILITY_FROM_DESC");
        fieldNames.add("TM_FACILITY_TO");
        fieldNames.add("FACILITY_TO_DESC");
        fieldNames.add("TM_DX_YEAR");
        fieldNames.add("AGE_AT_TM_DX_DATE_IN_DAYS");
        fieldNames.add("TM_SITE_CD");
        fieldNames.add("TM_SITE_DESC");
        fieldNames.add("TM_LATERALITY_CD");
        fieldNames.add("TM_LATERALITY_DESC");
        fieldNames.add("TM_HIST_CD");
        fieldNames.add("TM_HIST_DESC");
        fieldNames.add("TM_DX_CONFRM_CD");
        fieldNames.add("TM_DX_CONFRM_DESC");
        fieldNames.add("TM_REGNODE_EXM_NO");
        fieldNames.add("TM_REGNODE_POS_NO");
        fieldNames.add("TM_TUMOR_SIZE");
        fieldNames.add("TM_RESID_TUMOR_CD");
        fieldNames.add("TM_RESID_TUMOR_DESC");
        fieldNames.add("TM_GENERAL_STG");
        fieldNames.add("TM_GENERAL_STG_DESC");
        fieldNames.add("TM_TNM_EDITION");
        fieldNames.add("TM_TNM_EDITION_DESC");
        fieldNames.add("TM_CLIN_TNM_T");
        fieldNames.add("TM_CLIN_TNM_T_DESC");
        fieldNames.add("TM_CLIN_TNM_N");
        fieldNames.add("TM_CLIN_TNM_N_DESC");
        fieldNames.add("TM_CLIN_TNM_M");
        fieldNames.add("TM_CLIN_TNM_M_DESC");
        fieldNames.add("TM_CLIN_STG_GRP");
        fieldNames.add("TM_PATH_TNM_T");
        fieldNames.add("TM_PATH_TNM_T_DESC");
        fieldNames.add("TM_PATH_TNM_N");
        fieldNames.add("TM_PATH_TNM_N_DESC");
        fieldNames.add("TM_PATH_TNM_M");
        fieldNames.add("TM_PATH_TNM_M_DESC");
        fieldNames.add("TM_PATH_STG_GRP");
        fieldNames.add("TM_PATH_RPT_AV");
        fieldNames.add("TM_CA_STS_AT_ACC");
        fieldNames.add("TM_FIRST_RECUR_YEAR");
        fieldNames.add("AGE_AT_TM_FIRST_RECUR_DATE_IN_DAYS");
        fieldNames.add("TM_FIRST_RECUR_TYP");
        fieldNames.add("TM_ADM_YEAR");
        fieldNames.add("AGE_AT_TM_ADM_DATE_IN_DAYS");
        fieldNames.add("TM_DSCH_YEAR");
        fieldNames.add("AGE_AT_TM_DSCH_DATE_IN_DAYS");
        fieldNames.add("TM_SURG_DSCH_YEAR");
        fieldNames.add("AGE_AT_TM_SURG_DSCH_DATE_IN_DAYS");
        fieldNames.add("TM_READM_WTHN_30D");
        fieldNames.add("TM_FIRST_TX_YEAR");
        fieldNames.add("AGE_AT_TM_FIRST_TX_DATE_IN_DAYS");
        fieldNames.add("TM_NON_CA_SURG_SUM");
        fieldNames.add("TM_NON_CA_SURG_SUM_DESC");
        fieldNames.add("TM_NON_CA_SURG_MSK");
        fieldNames.add("TM_NON_CA_SURG_YEAR");
        fieldNames.add("AGE_AT_TM_NON_CA_SURG_DATE_IN_DAYS");
        fieldNames.add("TM_CA_SURG_98");
        fieldNames.add("TM_CA_SURG_98_MSK");
        fieldNames.add("TM_CA_SURG_03");
        fieldNames.add("TM_CA_SURG_03_MSK");
        fieldNames.add("TM_OTH_SURG_98");
        fieldNames.add("TM_OTH_SURG_98_MSK");
        fieldNames.add("TM_OTH_SURG_03");
        fieldNames.add("TM_OTH_SURG_03_MSK");
        fieldNames.add("TM_OTH_SURG_CD");
        fieldNames.add("TM_OTH_SURG_CD_DESC");
        fieldNames.add("TM_RGN_SCOP_98");
        fieldNames.add("TM_RGN_SCOP_98_MSK");
        fieldNames.add("TM_RGN_SCOP_03");
        fieldNames.add("TM_RGN_SCOP_03_MSK");
        fieldNames.add("TM_REGNODE_SCOP_CD");
        fieldNames.add("TM_REGNODE_SCOP_DESC");
        fieldNames.add("TM_RECON_SURG");
        fieldNames.add("TM_RECON_SURG_DESC");
        fieldNames.add("TM_CA_SURG_YEAR");
        fieldNames.add("AGE_AT_TM_CA_SURG_DATE_IN_DAYS");
        fieldNames.add("TM_SURG_DEF_YEAR");
        fieldNames.add("AGE_AT_TM_SURG_DEF_DATE_IN_DAYS");
        fieldNames.add("TM_REASON_NO_SURG");
        fieldNames.add("REASON_NO_SURG_DESC");
        fieldNames.add("TM_PRIM_SURGEON");
        fieldNames.add("TM_PRIM_SURGEON_NAME");
        fieldNames.add("TM_ATN_DR_NO");
        fieldNames.add("TM_ATN_DR_NAME");
        fieldNames.add("TM_REASON_NO_RAD");
        fieldNames.add("TM_REASON_NO_RAD_DESC");
        fieldNames.add("TM_RAD_STRT_YEAR");
        fieldNames.add("AGE_AT_TM_RAD_STRT_DATE_IN_DAYS");
        fieldNames.add("TM_RAD_END_YEAR");
        fieldNames.add("AGE_AT_TM_RAD_END_DATE_IN_DAYS");
        fieldNames.add("TM_RAD_TX_MOD");
        fieldNames.add("TM_RAD_TX_MOD_DESC");
        fieldNames.add("TM_BOOST_RAD_MOD");
        fieldNames.add("TM_BOOST_RAD_MOD_DESC");
        fieldNames.add("TM_LOC_RAD_TX");
        fieldNames.add("TM_LOC_RAD_TX_DESC");
        fieldNames.add("TM_RAD_TX_VOL");
        fieldNames.add("TM_RAD_TX_VOL_DESC");
        fieldNames.add("TM_NUM_TX_THIS_VOL");
        fieldNames.add("TM_NUM_TX_THIS_VOL_DESC");
        fieldNames.add("TM_REG_RAD_DOSE");
        fieldNames.add("TM_REG_RAD_DOSE_DESC");
        fieldNames.add("TM_BOOST_RAD_DOSE");
        fieldNames.add("TM_BOOST_RAD_DOSE_DESC");
        fieldNames.add("TM_RAD_SURG_SEQ");
        fieldNames.add("TM_RAD_SURG_SEQ_DESC");
        fieldNames.add("TM_RAD_MD");
        fieldNames.add("TM_RAD_MD_NAME");
        fieldNames.add("TM_SYST_STRT_YEAR");
        fieldNames.add("AGE_AT_TM_SYST_STRT_DATE_IN_DAYS");
        fieldNames.add("TM_OTH_STRT_YEAR");
        fieldNames.add("AGE_AT_TM_OTH_STRT_DATE_IN_DAYS");
        fieldNames.add("TM_CHEM_SUM");
        fieldNames.add("TM_CHEM_SUM_DESC");
        fieldNames.add("TM_CHEM_SUM_MSK");
        fieldNames.add("TM_CHEM_SUM_MSK_DESC");
        fieldNames.add("TM_TUMOR_SEQ");
        fieldNames.add("TM_HORM_SUM");
        fieldNames.add("TM_HORM_SUM_DESC");
        fieldNames.add("TM_HORM_SUM_MSK");
        fieldNames.add("TM_HORM_SUM_MSK_DESC");
        fieldNames.add("TM_BRM_SUM");
        fieldNames.add("TM_BRM_SUM_DESC");
        fieldNames.add("TM_BRM_SUM_MSK");
        fieldNames.add("TM_BRM_SUM_MSK_DESC");
        fieldNames.add("TM_OTH_SUM");
        fieldNames.add("TM_OTH_SUM_DESC");
        fieldNames.add("TM_OTH_SUM_MSK");
        fieldNames.add("TM_OTH_SUM_MSK_DESC");
        fieldNames.add("TM_PALLIA_PROC");
        fieldNames.add("TM_PALLIA_PROC_DESC");
        fieldNames.add("TM_PALLIA_PROC_MSK");
        fieldNames.add("TM_PALLIA_PROC_MSK_DESC");
        fieldNames.add("TM_ONCOLOGY_MD");
        fieldNames.add("TM_ONCOLOGY_MD_NAME");
        fieldNames.add("TM_PRCS_YEAR");
        fieldNames.add("AGE_AT_TM_PRCS_DATE_IN_DAYS");
        fieldNames.add("TM_PATH_TEXT");
        fieldNames.add("TM_SURG_TEXT");
        fieldNames.add("TM_OVERRIDE_COM");
        fieldNames.add("TM_CSSIZE");
        fieldNames.add("TM_CSEXT");
        fieldNames.add("TM_CSEXTEV");
        fieldNames.add("TM_CSLMND");
        fieldNames.add("TM_CSRGNEV");
        fieldNames.add("TM_CSMETDX");
        fieldNames.add("TM_CSMETEV");
        fieldNames.add("TM_TSTAGE");
        fieldNames.add("TM_TSTAGE_DESC");
        fieldNames.add("TM_NSTAGE");
        fieldNames.add("TM_NSTAGE_DESC");
        fieldNames.add("TM_MSTAGE");
        fieldNames.add("TM_MSTAGE_DESC");
        fieldNames.add("TM_TBASIS");
        fieldNames.add("TM_TBASIS_DESC");
        fieldNames.add("TM_NBASIS");
        fieldNames.add("TM_NBASIS_DESC");
        fieldNames.add("TM_MBASIS");
        fieldNames.add("TM_MBASIS_DESC");
        fieldNames.add("TM_AJCC");
        fieldNames.add("TM_AJCC_DESC");
        fieldNames.add("TM_MSK_STG");

        return fieldNames;
    }
    
    
    
}

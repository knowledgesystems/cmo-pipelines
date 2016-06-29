/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;

import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactPatientIcdoRecord;

import static com.querydsl.core.alias.Alias.$;
import static com.querydsl.core.alias.Alias.alias;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;

import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.apache.log4j.Logger;

import java.util.*;
/**
 *
 * @author jake
 */
public class MSK_ImpactPatientIcdoReader implements ItemStreamReader<MSK_ImpactPatientIcdoRecord>{
    @Value("${darwin.icdo_view}")
    private String patientIcdoView;
    
    @Autowired
    SQLQueryFactory darwinQueryFactory;
    
    private List<MSK_ImpactPatientIcdoRecord> darwinIcdoResults;
    
    Logger log = Logger.getLogger(MSK_ImpactPatientIcdoReader.class);
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException{
        this.darwinIcdoResults = getDarwinIcdoResults();
    }
    
    @Transactional
    private List<MSK_ImpactPatientIcdoRecord> getDarwinIcdoResults(){
        log.info("Start of Darwin Patient ICDO Record View Import...");
        MSK_ImpactPatientIcdoRecord qDICDO = alias(MSK_ImpactPatientIcdoRecord.class, patientIcdoView);
        List<MSK_ImpactPatientIcdoRecord> darwinIcdoResults = darwinQueryFactory.select(Projections.constructor(MSK_ImpactPatientIcdoRecord.class, 
                        $(qDICDO.getTUMOR_YEAR()),
                        $(qDICDO.getPT_ID_ICDO()),
                        $(qDICDO.getDMP_ID_ICDO()),
                        $(qDICDO.getTM_TUMOR_SEQ_DESC()),
                        $(qDICDO.getTM_ACC_YEAR()),
                        $(qDICDO.getTM_FIRST_MSK_YEAR()),
                        $(qDICDO.getAGE_AT_TM_FIRST_MSK_DATE_IN_DAYS()),
                        $(qDICDO.getTM_CASE_STS()),
                        $(qDICDO.getTM_CASE_STS_DESC()),
                        $(qDICDO.getTM_CASE_EFF_YEAR()),
                        $(qDICDO.getAGE_AT_TM_CASE_EFF_DATE_IN_DAYS()),
                        $(qDICDO.getTM_STATE_AT_DX()),
                        $(qDICDO.getTM_SMOKING_HX()),
                        $(qDICDO.getTM_SMOKING_HX_DESC()),
                        $(qDICDO.getTM_OCCUPATION()),
                        $(qDICDO.getTM_OCCUPATION_DESC()),
                        $(qDICDO.getTM_FACILITY_FROM()),
                        $(qDICDO.getFACILITY_FROM_DESC()),
                        $(qDICDO.getTM_FACILITY_TO()),
                        $(qDICDO.getFACILITY_TO_DESC()),
                        $(qDICDO.getTM_DX_YEAR()),
                        $(qDICDO.getAGE_AT_TM_DX_DATE_IN_DAYS()),
                        $(qDICDO.getTM_SITE_CD()),
                        $(qDICDO.getTM_SITE_DESC()),
                        $(qDICDO.getTM_LATERALITY_CD()),
                        $(qDICDO.getTM_LATERALITY_DESC()),
                        $(qDICDO.getTM_HIST_CD()),
                        $(qDICDO.getTM_HIST_DESC()),
                        $(qDICDO.getTM_DX_CONFRM_CD()),
                        $(qDICDO.getTM_DX_CONFRM_DESC()),
                        $(qDICDO.getTM_REGNODE_EXM_NO()),
                        $(qDICDO.getTM_REGNODE_POS_NO()),
                        $(qDICDO.getTM_TUMOR_SIZE()),
                        $(qDICDO.getTM_RESID_TUMOR_CD()),
                        $(qDICDO.getTM_RESID_TUMOR_DESC()),
                        $(qDICDO.getTM_GENERAL_STG()),
                        $(qDICDO.getTM_GENERAL_STG_DESC()),
                        $(qDICDO.getTM_TNM_EDITION()),
                        $(qDICDO.getTM_TNM_EDITION_DESC()),
                        $(qDICDO.getTM_CLIN_TNM_T()),
                        $(qDICDO.getTM_CLIN_TNM_T_DESC()),
                        $(qDICDO.getTM_CLIN_TNM_N()),
                        $(qDICDO.getTM_CLIN_TNM_N_DESC()),
                        $(qDICDO.getTM_CLIN_TNM_M()),
                        $(qDICDO.getTM_CLIN_TNM_M_DESC()),
                        $(qDICDO.getTM_CLIN_STG_GRP()),
                        $(qDICDO.getTM_PATH_TNM_T()),
                        $(qDICDO.getTM_PATH_TNM_T_DESC()),
                        $(qDICDO.getTM_PATH_TNM_N()),
                        $(qDICDO.getTM_PATH_TNM_N_DESC()),
                        $(qDICDO.getTM_PATH_TNM_M()),
                        $(qDICDO.getTM_PATH_TNM_M_DESC()),
                        $(qDICDO.getTM_PATH_STG_GRP()),
                        $(qDICDO.getTM_PATH_RPT_AV()),
                        $(qDICDO.getTM_CA_STS_AT_ACC()),
                        $(qDICDO.getTM_FIRST_RECUR_YEAR()),
                        $(qDICDO.getAGE_AT_TM_FIRST_RECUR_DATE_IN_DAYS()),
                        $(qDICDO.getTM_FIRST_RECUR_TYP()),
                        $(qDICDO.getTM_ADM_YEAR()),
                        $(qDICDO.getAGE_AT_TM_ADM_DATE_IN_DAYS()),
                        $(qDICDO.getTM_DSCH_YEAR()),
                        $(qDICDO.getAGE_AT_TM_DSCH_DATE_IN_DAYS()),
                        $(qDICDO.getTM_SURG_DSCH_YEAR()),
                        $(qDICDO.getAGE_AT_TM_SURG_DSCH_DATE_IN_DAYS()),
                        $(qDICDO.getTM_READM_WTHN_30D()),
                        $(qDICDO.getTM_FIRST_TX_YEAR()),
                        $(qDICDO.getAGE_AT_TM_FIRST_TX_DATE_IN_DAYS()),
                        $(qDICDO.getTM_NON_CA_SURG_SUM()),
                        $(qDICDO.getTM_NON_CA_SURG_SUM_DESC()),
                        $(qDICDO.getTM_NON_CA_SURG_MSK()),
                        $(qDICDO.getTM_NON_CA_SURG_YEAR()),
                        $(qDICDO.getAGE_AT_TM_NON_CA_SURG_DATE_IN_DAYS()),
                        $(qDICDO.getTM_CA_SURG_98()),
                        $(qDICDO.getTM_CA_SURG_98_MSK()),
                        $(qDICDO.getTM_CA_SURG_03()),
                        $(qDICDO.getTM_CA_SURG_03_MSK()),
                        $(qDICDO.getTM_OTH_SURG_98()),
                        $(qDICDO.getTM_OTH_SURG_98_MSK()),
                        $(qDICDO.getTM_OTH_SURG_03()),
                        $(qDICDO.getTM_OTH_SURG_03_MSK()),
                        $(qDICDO.getTM_OTH_SURG_CD()),
                        $(qDICDO.getTM_OTH_SURG_CD_DESC()),
                        $(qDICDO.getTM_RGN_SCOP_98()),
                        $(qDICDO.getTM_RGN_SCOP_98_MSK()),
                        $(qDICDO.getTM_RGN_SCOP_03()),
                        $(qDICDO.getTM_RGN_SCOP_03_MSK()),
                        $(qDICDO.getTM_REGNODE_SCOP_CD()),
                        $(qDICDO.getTM_REGNODE_SCOP_DESC()),
                        $(qDICDO.getTM_RECON_SURG()),
                        $(qDICDO.getTM_RECON_SURG_DESC()),
                        $(qDICDO.getTM_CA_SURG_YEAR()),
                        $(qDICDO.getAGE_AT_TM_CA_SURG_DATE_IN_DAYS()),
                        $(qDICDO.getTM_SURG_DEF_YEAR()),
                        $(qDICDO.getAGE_AT_TM_SURG_DEF_DATE_IN_DAYS()),
                        $(qDICDO.getTM_REASON_NO_SURG()),
                        $(qDICDO.getREASON_NO_SURG_DESC()),
                        $(qDICDO.getTM_PRIM_SURGEON()),
                        $(qDICDO.getTM_PRIM_SURGEON_NAME()),
                        $(qDICDO.getTM_ATN_DR_NO()),
                        $(qDICDO.getTM_ATN_DR_NAME()),
                        $(qDICDO.getTM_REASON_NO_RAD()),
                        $(qDICDO.getTM_REASON_NO_RAD_DESC()),
                        $(qDICDO.getTM_RAD_STRT_YEAR()),
                        $(qDICDO.getAGE_AT_TM_RAD_STRT_DATE_IN_DAYS()),
                        $(qDICDO.getTM_RAD_END_YEAR()),
                        $(qDICDO.getAGE_AT_TM_RAD_END_DATE_IN_DAYS()),
                        $(qDICDO.getTM_RAD_TX_MOD()),
                        $(qDICDO.getTM_RAD_TX_MOD_DESC()),
                        $(qDICDO.getTM_BOOST_RAD_MOD()),
                        $(qDICDO.getTM_BOOST_RAD_MOD_DESC()),
                        $(qDICDO.getTM_LOC_RAD_TX()),
                        $(qDICDO.getTM_LOC_RAD_TX_DESC()),
                        $(qDICDO.getTM_RAD_TX_VOL()),
                        $(qDICDO.getTM_RAD_TX_VOL_DESC()),
                        $(qDICDO.getTM_NUM_TX_THIS_VOL()),
                        $(qDICDO.getTM_NUM_TX_THIS_VOL_DESC()),
                        $(qDICDO.getTM_REG_RAD_DOSE()),
                        $(qDICDO.getTM_REG_RAD_DOSE_DESC()),
                        $(qDICDO.getTM_BOOST_RAD_DOSE()),
                        $(qDICDO.getTM_BOOST_RAD_DOSE_DESC()),
                        $(qDICDO.getTM_RAD_SURG_SEQ()),
                        $(qDICDO.getTM_RAD_SURG_SEQ_DESC()),
                        $(qDICDO.getTM_RAD_MD()),
                        $(qDICDO.getTM_RAD_MD_NAME()),
                        $(qDICDO.getTM_SYST_STRT_YEAR()),
                        $(qDICDO.getAGE_AT_TM_SYST_STRT_DATE_IN_DAYS()),
                        $(qDICDO.getTM_OTH_STRT_YEAR()),
                        $(qDICDO.getAGE_AT_TM_OTH_STRT_DATE_IN_DAYS()),
                        $(qDICDO.getTM_CHEM_SUM()),
                        $(qDICDO.getTM_CHEM_SUM_DESC()),
                        $(qDICDO.getTM_CHEM_SUM_MSK()),
                        $(qDICDO.getTM_CHEM_SUM_MSK_DESC()),
                        $(qDICDO.getTM_TUMOR_SEQ()),
                        $(qDICDO.getTM_HORM_SUM()),
                        $(qDICDO.getTM_HORM_SUM_DESC()),
                        $(qDICDO.getTM_HORM_SUM_MSK()),
                        $(qDICDO.getTM_HORM_SUM_MSK_DESC()),
                        $(qDICDO.getTM_BRM_SUM()),
                        $(qDICDO.getTM_BRM_SUM_DESC()),
                        $(qDICDO.getTM_BRM_SUM_MSK()),
                        $(qDICDO.getTM_BRM_SUM_MSK_DESC()),
                        $(qDICDO.getTM_OTH_SUM()),
                        $(qDICDO.getTM_OTH_SUM_DESC()),
                        $(qDICDO.getTM_OTH_SUM_MSK()),
                        $(qDICDO.getTM_OTH_SUM_MSK_DESC()),
                        $(qDICDO.getTM_PALLIA_PROC()),
                        $(qDICDO.getTM_PALLIA_PROC_DESC()),
                        $(qDICDO.getTM_PALLIA_PROC_MSK()),
                        $(qDICDO.getTM_PALLIA_PROC_MSK_DESC()),
                        $(qDICDO.getTM_ONCOLOGY_MD()),
                        $(qDICDO.getTM_ONCOLOGY_MD_NAME()),
                        $(qDICDO.getTM_PRCS_YEAR()),
                        $(qDICDO.getAGE_AT_TM_PRCS_DATE_IN_DAYS()),
                        $(qDICDO.getTM_PATH_TEXT()),
                        $(qDICDO.getTM_SURG_TEXT()),
                        $(qDICDO.getTM_OVERRIDE_COM()),
                        $(qDICDO.getTM_CSSIZE()),
                        $(qDICDO.getTM_CSEXT()),
                        $(qDICDO.getTM_CSEXTEV()),
                        $(qDICDO.getTM_CSLMND()),
                        $(qDICDO.getTM_CSRGNEV()),
                        $(qDICDO.getTM_CSMETDX()),
                        $(qDICDO.getTM_CSMETEV()),
                        $(qDICDO.getTM_TSTAGE()),
                        $(qDICDO.getTM_TSTAGE_DESC()),
                        $(qDICDO.getTM_NSTAGE()),
                        $(qDICDO.getTM_NSTAGE_DESC()),
                        $(qDICDO.getTM_MSTAGE()),
                        $(qDICDO.getTM_MSTAGE_DESC()),
                        $(qDICDO.getTM_TBASIS()),
                        $(qDICDO.getTM_TBASIS_DESC()),
                        $(qDICDO.getTM_NBASIS()),
                        $(qDICDO.getTM_NBASIS_DESC()),
                        $(qDICDO.getTM_MBASIS()),
                        $(qDICDO.getTM_MBASIS_DESC()),
                        $(qDICDO.getTM_AJCC()),
                        $(qDICDO.getTM_AJCC_DESC()),
                        $(qDICDO.getTM_MSK_STG())))
                .where($(qDICDO.getPT_ID_ICDO()).isNotEmpty())
                .from($(qDICDO))
                .fetch();
        
        log.info("Imported " + darwinIcdoResults.size() + " records from Darwin Patient ICDO Record View.");
        return darwinIcdoResults;
    }
    
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException{}
    
    @Override
    public void close() throws ItemStreamException{}
    
    @Override
    public MSK_ImpactPatientIcdoRecord read() throws Exception{
        if(!darwinIcdoResults.isEmpty()){
            return darwinIcdoResults.remove(0);
        }
        return null;
    }
}

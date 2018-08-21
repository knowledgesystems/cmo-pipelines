/*
 * Copyright (c) 2016-2018 Memorial Sloan-Kettering Cancer Center.
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
package org.mskcc.cmo.ks.crdb.model;

import java.util.*;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Model for CRDBPDXClinicalPatientDataset results.
 *
 * @author averyniceday
 */

public class CRDBPDXTimelineDataset {

    private String PATIENT_ID;
    private String SAMPLE_ID;
    private String PDX_ID;
    private String DESTINATION_STUDY_ID;
    private String START_DATE;
    private String STOP_DATE;
    private String EVENT_TYPE;
    private String PASSAGE_ID;
    private String TREATMENT_TYPE;
    private String SUBTYPE;
    private String AGENT;
    private String RESPONSE;
    private String RESPONSE_DURATION_MONTHS;
    private String REASON_FOR_TX_DISCONTINUATION;
    private String SURGERY_DETAILS;
    private String EVENT_TYPE_DETAILED;
    private String PROCEDURE_LOCATION;
    private String PROCEDURE_LOCATION_SPECIFY;
    private String DIAGNOSTIC_TYPE;
    private String DIAGNOSTIC_TYPE_SITE;
    private String IMAGING;
    private String SPECIMEN_TYPE;
    private String SPECIMEN_SITE;
    private String AGE_AT_PROCEDURE;
    private String LATERALITY;
    private String DISEASE_STATUS;
    private String METASTATIC_SITE;
    private String TIME_TO_METASTASIS_MONTHS;
    private String SAMPLE_TYPE;
    private String SITE_OF_RECURRENCE;
    private String TIME_TO_RECURRENCE;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public CRDBPDXTimelineDataset() {
    }
    public CRDBPDXTimelineDataset(String PATIENT_ID, String SAMPLE_ID, String PDX_ID, String DESTINATION_STUDY_ID,
                                  String START_DATE, String STOP_DATE, String EVENT_TYPE, String PASSAGE_ID,
                                  String TREATMENT_TYPE, String SUBTYPE, String AGENT, String RESPONSE,
                                  String RESPONSE_DURATION_MONTHS, String REASON_FOR_TX_DISCONTINUATION,
                                  String SURGERY_DETAILS, String EVENT_TYPE_DETAILED, String PROCEDURE_LOCATION,
                                  String PROCEDURE_LOCATION_SPECIFY, String DIAGNOSTIC_TYPE, String DIAGNOSTIC_TYPE_SITE,
                                  String IMAGING, String SPECIMEN_TYPE, String SPECIMEN_SITE, String AGE_AT_PROCEDURE,
                                  String LATERALITY, String DISEASE_STATUS, String METASTATIC_SITE,
                                  String TIME_TO_METASTASIS_MONTHS, String SAMPLE_TYPE, String SITE_OF_RECURRENCE,
                                  String TIME_TO_RECURRENCE) {
        this.PATIENT_ID = PATIENT_ID == null ? "NA" : PATIENT_ID;
        this.SAMPLE_ID = SAMPLE_ID == null ? "NA" : SAMPLE_ID;
        this.PDX_ID = PDX_ID == null ? "NA" : PDX_ID;
        this.DESTINATION_STUDY_ID = DESTINATION_STUDY_ID == null ? "NA" : DESTINATION_STUDY_ID;
        this.START_DATE = START_DATE == null ? "NA" : START_DATE;
        this.STOP_DATE = STOP_DATE == null ? "NA" : STOP_DATE;
        this.EVENT_TYPE = EVENT_TYPE == null ? "NA" : EVENT_TYPE;
        this.PASSAGE_ID = PASSAGE_ID == null ? "NA" : PASSAGE_ID;
        this.TREATMENT_TYPE = TREATMENT_TYPE == null ? "NA" : TREATMENT_TYPE;
        this.SUBTYPE = SUBTYPE == null ? "NA" : SUBTYPE;
        this.AGENT = AGENT == null ? "NA" : AGENT;
        this.RESPONSE = RESPONSE == null ? "NA" : RESPONSE;
        this.RESPONSE_DURATION_MONTHS = RESPONSE_DURATION_MONTHS == null ? "NA" : RESPONSE_DURATION_MONTHS;
        this.REASON_FOR_TX_DISCONTINUATION = REASON_FOR_TX_DISCONTINUATION == null ? "NA" : REASON_FOR_TX_DISCONTINUATION;
        this.SURGERY_DETAILS = SURGERY_DETAILS == null ? "NA" : SURGERY_DETAILS;
        this.EVENT_TYPE_DETAILED = EVENT_TYPE_DETAILED == null ? "NA" : EVENT_TYPE_DETAILED;
        this.PROCEDURE_LOCATION = PROCEDURE_LOCATION == null ? "NA" : PROCEDURE_LOCATION;
        this.PROCEDURE_LOCATION_SPECIFY = PROCEDURE_LOCATION_SPECIFY == null ? "NA" : PROCEDURE_LOCATION_SPECIFY;
        this.DIAGNOSTIC_TYPE = DIAGNOSTIC_TYPE == null ? "NA" : DIAGNOSTIC_TYPE;
        this.DIAGNOSTIC_TYPE_SITE = DIAGNOSTIC_TYPE_SITE == null ? "NA" : DIAGNOSTIC_TYPE_SITE;
        this.IMAGING = IMAGING == null ? "NA" : IMAGING;
        this.SPECIMEN_TYPE = SPECIMEN_TYPE == null ? "NA" : SPECIMEN_TYPE;
        this.SPECIMEN_SITE = SPECIMEN_SITE == null ? "NA" : SPECIMEN_SITE;
        this.AGE_AT_PROCEDURE = AGE_AT_PROCEDURE == null ? "NA" : AGE_AT_PROCEDURE;
        this.LATERALITY = LATERALITY == null ? "NA" : LATERALITY;
        this.DISEASE_STATUS = DISEASE_STATUS == null ? "NA" : DISEASE_STATUS;
        this.METASTATIC_SITE = METASTATIC_SITE == null ? "NA" : METASTATIC_SITE;
        this.TIME_TO_METASTASIS_MONTHS = TIME_TO_METASTASIS_MONTHS == null ? "NA" : TIME_TO_METASTASIS_MONTHS;
        this.SAMPLE_TYPE = SAMPLE_TYPE == null ? "NA" : SAMPLE_TYPE;
        this.SITE_OF_RECURRENCE = SITE_OF_RECURRENCE == null ? "NA" : SITE_OF_RECURRENCE;
        this.TIME_TO_RECURRENCE = TIME_TO_RECURRENCE == null ? "NA" : TIME_TO_RECURRENCE;
    }
    
    
    public String getPATIENT_ID() {
        return PATIENT_ID;
    }

    /**
     * @param PATIENT_ID
     */
    public void setPATIENT_ID(String PATIENT_ID) {
        this.PATIENT_ID = PATIENT_ID;
    }

    /**
     * 
     * @return SAMPLE_ID
     */
    public String getSAMPLE_ID() {
        return SAMPLE_ID;
    }

    /**
     * @param SAMPLE_ID
     */
    public void setSAMPLE_ID(String SAMPLE_ID) {
        this.SAMPLE_ID = SAMPLE_ID;
    }

    /**
     *
     * @return PDX_ID
     */
    public String getPDX_ID() {
        return PDX_ID;
    }

    /**
     * @param PDX_ID
     */
    public void setPDX_ID(String PDX_ID) {
        this.PDX_ID = PDX_ID;
    }

    /**
     *
     * @return getDESTINATION_STUDY_ID
     */
    public String getDESTINATION_STUDY_ID() {
        return DESTINATION_STUDY_ID;
    }

    /**
     * @param DESTINATION_STUDY_ID
     */
    public void setDESTINATION_STUDY_ID(String DESTINATION_STUDY_ID) {
        this.DESTINATION_STUDY_ID = DESTINATION_STUDY_ID;
    }

    /**
     * @return START_DATE
     */
    public String getSTART_DATE() {
        return START_DATE;
    }

    /**
     * @param START_DATE
     */
    public void setSTART_DATE(String START_DATE) {
        this.START_DATE = START_DATE;
    }

    /**
     * @return STOP_DATE
     */
    public String getSTOP_DATE() {
        return STOP_DATE;
    }

    /**
     * @param STOP_DATE
     */
    public void setSTOP_DATE(String STOP_DATE) {
        this.STOP_DATE = STOP_DATE;
    }

    /**
     * @return EVENT_TYPE
     */
    public String getEVENT_TYPE() {
        return EVENT_TYPE;
    }

    /**
     * @param EVENT_TYPE
     */
    public void setEVENT_TYPE(String EVENT_TYPE) {
        this.EVENT_TYPE = EVENT_TYPE;
    }

    /**
     * @return PASSAGE_ID
     */
    public String getPASSAGE_ID() {
        return PASSAGE_ID;
    }

    /**
     * @param PASSAGE_ID
     */
    public void setPASSAGE_ID(String PASSAGE_ID) {
        this.PASSAGE_ID = PASSAGE_ID;
    }

    /**
     * @return TREATMENT_TYPE
     */
    public String getTREATMENT_TYPE() {
        return TREATMENT_TYPE;
    }

    /**
     * @param TREATMENT_TYPE
     */
    public void setTREATMENT_TYPE(String TREATMENT_TYPE) {
        this.TREATMENT_TYPE = TREATMENT_TYPE;
    }

    /**
     * @return SUBTYPE
     */
    public String getSUBTYPE() {
        return SUBTYPE;
    }

    /**
     * @param SUBTYPE
     */
    public void setSUBTYPE(String SUBTYPE) {
        this.SUBTYPE = SUBTYPE;
    }

    /**
     * @return AGENT
     */
    public String getAGENT() {
        return AGENT;
    }

    /**
     * @param AGENT
     */
    public void setAGENT(String AGENT) {
        this.AGENT = AGENT;
    }

    /**
     * @return RESPONSE
     */
    public String getRESPONSE() {
        return RESPONSE;
    }

    /**
     * @param RESPONSE
     */
    public void setRESPONSE(String RESPONSE) {
        this.RESPONSE = RESPONSE;
    }

    /**
     * @return RESPONSE_DURATION_MONTHS
     */
    public String getRESPONSE_DURATION_MONTHS() {
        return RESPONSE_DURATION_MONTHS;
    }

    /**
     * @param RESPONSE_DURATION_MONTHS
     */
    public void setRESPONSE_DURATION_MONTHS(String RESPONSE_DURATION_MONTHS) {
        this.RESPONSE_DURATION_MONTHS = RESPONSE_DURATION_MONTHS;
    }

    /**
     * @return REASON_FOR_TX_DISCONTINUATION
     */
    public String getREASON_FOR_TX_DISCONTINUATION() {
        return REASON_FOR_TX_DISCONTINUATION;
    }

    /**
     * @param REASON_FOR_TX_DISCONTINUATION
     */
    public void setREASON_FOR_TX_DISCONTINUATION(String REASON_FOR_TX_DISCONTINUATION) {
        this.REASON_FOR_TX_DISCONTINUATION = REASON_FOR_TX_DISCONTINUATION;
    }

    /**
     * @return SURGERY_DETAILS
     */
    public String getSURGERY_DETAILS() {
        return SURGERY_DETAILS;
    }

    /**
     * @param SURGERY_DETAILS
     */
    public void setSURGERY_DETAILS(String SURGERY_DETAILS) {
        this.SURGERY_DETAILS = SURGERY_DETAILS;
    }

    /**
     * @return EVENT_TYPE_DETAILED
     */
    public String getEVENT_TYPE_DETAILED() {
        return EVENT_TYPE_DETAILED;
    }

    /**
     * @param EVENT_TYPE_DETAILED
     */
    public void setEVENT_TYPE_DETAILED(String EVENT_TYPE_DETAILED) {
        this.EVENT_TYPE_DETAILED = EVENT_TYPE_DETAILED;
    }

    /**
     * @return PROCEDURE_LOCATION
     */
    public String getPROCEDURE_LOCATION() {
        return PROCEDURE_LOCATION;
    }

    /**
     * @param PROCEDURE_LOCATION
     */
    public void setPROCEDURE_LOCATION(String PROCEDURE_LOCATION) {
        this.PROCEDURE_LOCATION = PROCEDURE_LOCATION;
    }

    /**
     * @return PROCEDURE_LOCATION_SPECIFY
     */
    public String getPROCEDURE_LOCATION_SPECIFY() {
        return PROCEDURE_LOCATION_SPECIFY;
    }

    /**
     * @param PROCEDURE_LOCATION_SPECIFY
     */
    public void setPROCEDURE_LOCATION_SPECIFY(String PROCEDURE_LOCATION_SPECIFY) {
        this.PROCEDURE_LOCATION_SPECIFY = PROCEDURE_LOCATION_SPECIFY;
    }

    /**
     * @return DIAGNOSTIC_TYPE
     */
    public String getDIAGNOSTIC_TYPE() {
        return DIAGNOSTIC_TYPE;
    }

    /**
     * @param DIAGNOSTIC_TYPE
     */
    public void setDIAGNOSTIC_TYPE(String DIAGNOSTIC_TYPE) {
        this.DIAGNOSTIC_TYPE = DIAGNOSTIC_TYPE;
    }


    /**
     * @return DIAGNOSTIC_TYPE_SITE
     */
    public String getDIAGNOSTIC_TYPE_SITE() {
        return DIAGNOSTIC_TYPE_SITE;
    }

    /**
     * @param DIAGNOSTIC_TYPE_SITE
     */
    public void setDIAGNOSTIC_TYPE_SITE(String DIAGNOSTIC_TYPE_SITE) {
        this.DIAGNOSTIC_TYPE_SITE = DIAGNOSTIC_TYPE_SITE;
    }

    /**
     * @return IMAGING
     */
    public String getIMAGING() {
        return IMAGING;
    }

    /**
     * @param IMAGING
     */
    public void setIMAGING(String IMAGING) {
        this.IMAGING = IMAGING;
    }

    /**
     * @return SPECIMEN_TYPE
     */
    public String getSPECIMEN_TYPE() {
        return SPECIMEN_TYPE;
    }

    /**
     * @param SPECIMEN_TYPE
     */
    public void setSPECIMEN_TYPE(String SPECIMEN_TYPE) {
        this.SPECIMEN_TYPE = SPECIMEN_TYPE;
    }

    /**
     * @return SPECIMEN_SITE
     */
    public String getSPECIMEN_SITE() {
        return SPECIMEN_SITE;
    }

    /**
     * @param SPECIMEN_SITE
     */
    public void setSPECIMEN_SITE(String SPECIMEN_SITE) {
        this.SPECIMEN_SITE = SPECIMEN_SITE;
    }

    /**
     * @return AGE_AT_PROCEDURE
     */
    public String getAGE_AT_PROCEDURE() {
        return AGE_AT_PROCEDURE;
    }

    /**
     * @param AGE_AT_PROCEDURE
     */
    public void setAGE_AT_PROCEDURE(String AGE_AT_PROCEDURE) {
        this.AGE_AT_PROCEDURE = AGE_AT_PROCEDURE;
    }


    /**
     * @return LATERALITY
     */
    public String getLATERALITY() {
        return LATERALITY;
    }

    /**
     * @param LATERALITY
     */
    public void setLATERALITY(String LATERALITY) {
        this.LATERALITY = LATERALITY;
    }

    /**
     * @return DISEASE_STATUS
     */
    public String getDISEASE_STATUS() {
        return DISEASE_STATUS;
    }

    /**
     * @param DISEASE_STATUS
     */
    public void setDISEASE_STATUS(String DISEASE_STATUS) {
        this.DISEASE_STATUS = DISEASE_STATUS;
    }

    /**
     * @return METASTATIC_SITE
     */
    public String getMETASTATIC_SITE() {
        return METASTATIC_SITE;
    }

    /**
     * @param METASTATIC_SITE
     */
    public void setMETASTATIC_SITE(String METASTATIC_SITE) {
        this.METASTATIC_SITE = METASTATIC_SITE;
    }

    /**
     * @return TIME_TO_METASTATIS_MONTHS
     */
    public String getTIME_TO_METASTASIS_MONTHS() {
        return TIME_TO_METASTASIS_MONTHS;
    }

    /**
     * @param TIME_TO_METASTASIS_MONTHS
     */
    public void setTIME_TO_METASTASIS_MONTHS(String TIME_TO_METASTASIS_MONTHS) {
        this.TIME_TO_METASTASIS_MONTHS = TIME_TO_METASTASIS_MONTHS;
    }

    /**
     * @return SAMPLE_TYPE
     */
    public String getSAMPLE_TYPE() {
        return SAMPLE_TYPE;
    }

    /**
     * @param SAMPLE_TYPE
     */
    public void setSAMPLE_TYPE(String SAMPLE_TYPE) {
        this.SAMPLE_TYPE = SAMPLE_TYPE;
    }

    /**
     * @return SITE_OF_RECURRENCE
     */
    public String getSITE_OF_RECURRENCE() {
        return SITE_OF_RECURRENCE;
    }

    /**
     * @param SITE_OF_RECURRENCE
     */
    public void setSITE_OF_RECURRENCE(String SITE_OF_RECURRENCE) {
        this.SITE_OF_RECURRENCE = SITE_OF_RECURRENCE;
    }

    /**
     * @return TIME_OF_RECURRENCE
     */
    public String getTIME_TO_RECURRENCE() {
        return TIME_TO_RECURRENCE;
    }

    /**
     * @param TIME_TO_RECURRENCE
     */
    public void setTIME_TO_RECURRENCE(String TIME_TO_RECURRENCE) {
        this.TIME_TO_RECURRENCE = TIME_TO_RECURRENCE;
    }
    
    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("SAMPLE_ID");
        fieldNames.add("PDX_ID");
        //fieldNames.add("DESTINATION_STUDY_ID"); // This field is not a true clinical attribute -- do not output
        fieldNames.add("START_DATE");
        fieldNames.add("STOP_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("PASSAGE_ID");
        fieldNames.add("TREATMENT_TYPE");
        fieldNames.add("SUBTYPE");
        fieldNames.add("AGENT");
        fieldNames.add("RESPONSE");
        fieldNames.add("RESPONSE_DURATION_MONTHS");
        fieldNames.add("REASON_FOR_TX_DISCONTINUATION");
        fieldNames.add("SURGERY_DETAILS");
        fieldNames.add("EVENT_TYPE_DETAILED");
        fieldNames.add("PROCEDURE_LOCATION");
        fieldNames.add("PROCEDURE_LOCATION_SPECIFY");
        fieldNames.add("DIAGNOSTIC_TYPE");
        fieldNames.add("DIAGNOSTIC_TYPE_SITE");
        fieldNames.add("IMAGING");
        fieldNames.add("SPECIMEN_TYPE");
        fieldNames.add("SPECIMEN_SITE");
        fieldNames.add("AGE_AT_PROCEDURE");
        fieldNames.add("LATERALITY");
        fieldNames.add("DISEASE_STATUS");
        fieldNames.add("METASTATIC_SITE");
        fieldNames.add("TIME_TO_METASTASIS_MONTHS");
        fieldNames.add("SAMPLE_TYPE");
        fieldNames.add("SITE_OF_RECURRENCE");
        fieldNames.add("TIME_TO_RECURRENCE");
        return fieldNames;
    }
    
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}

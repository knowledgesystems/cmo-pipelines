/*
 * Copyright (c) 2018 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.ddp.source.model;

import java.util.*;
import com.fasterxml.jackson.annotation.*;
import java.io.Serializable;

/**
 *
 * @author ochoaa
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "ACTIVE",
    "ACTIVE_PATIENT_COUNT",
    "CHT_COHORT_ID",
    "CHT_COHORT_IS_IDENTIFIED_IND",
    "COHORT_TEMPLATE_SOURCE_ID",
    "CREATEDT",
    "DATALINEPROJECT",
    "DESC",
    "INACTIVE_PATIENT_COUNT",
    "IRBNO",
    "IRBWAIVER",
    "IS_DDP_PATIENT_ID_DRIVEN_COHORT",
    "IS_PERSONAL_COHORT",
    "IS_USER_ALLOWED_TO_ADD_PT",
    "IS_USER_ALLOWED_TO_DELETE_COHORT",
    "IS_USER_ALLOWED_TO_DROP_PT",
    "IS_USER_ALLOWED_TO_EDIT_COHORT_DESCRIPTION",
    "IS_USER_ALLOWED_TO_EDIT_COHORT_TITLE",
    "IS_USER_ALLOWED_TO_SET_PT_ACTIVE_STATUS",
    "IS_USER_ALLOWED_TO_SET_PT_NOTES",
    "IS_USER_ALLOWED_TO_SET_PT_NOTIFICATIONS",
    "PATIENT_COUNT",
    "TITLE"
})
public class Cohort implements Serializable {
    @JsonProperty("ACTIVE")
    private String aCTIVE;
    @JsonProperty("ACTIVE_PATIENT_COUNT")
    private Integer aCTIVEPATIENTCOUNT;
    @JsonProperty("CHT_COHORT_ID")
    private Integer cHTCOHORTID;
    @JsonProperty("CHT_COHORT_IS_IDENTIFIED_IND")
    private String cHTCOHORTISIDENTIFIEDIND;
    @JsonProperty("COHORT_TEMPLATE_SOURCE_ID")
    private String cOHORTTEMPLATESOURCEID;
    @JsonProperty("CREATEDT")
    private String cREATEDT;
    @JsonProperty("DATALINEPROJECT")
    private String dATALINEPROJECT;
    @JsonProperty("DESC")
    private String dESC;
    @JsonProperty("INACTIVE_PATIENT_COUNT")
    private Integer iNACTIVEPATIENTCOUNT;
    @JsonProperty("IRBNO")
    private String iRBNO;
    @JsonProperty("IRBWAIVER")
    private String iRBWAIVER;
    @JsonProperty("IS_DDP_PATIENT_ID_DRIVEN_COHORT")
    private String iSDDPPATIENTIDDRIVENCOHORT;
    @JsonProperty("IS_PERSONAL_COHORT")
    private String iSPERSONALCOHORT;
    @JsonProperty("IS_USER_ALLOWED_TO_ADD_PT")
    private String iSUSERALLOWEDTOADDPT;
    @JsonProperty("IS_USER_ALLOWED_TO_DELETE_COHORT")
    private String iSUSERALLOWEDTODELETECOHORT;
    @JsonProperty("IS_USER_ALLOWED_TO_DROP_PT")
    private String iSUSERALLOWEDTODROPPT;
    @JsonProperty("IS_USER_ALLOWED_TO_EDIT_COHORT_DESCRIPTION")
    private String iSUSERALLOWEDTOEDITCOHORTDESCRIPTION;
    @JsonProperty("IS_USER_ALLOWED_TO_EDIT_COHORT_TITLE")
    private String iSUSERALLOWEDTOEDITCOHORTTITLE;
    @JsonProperty("IS_USER_ALLOWED_TO_SET_PT_ACTIVE_STATUS")
    private String iSUSERALLOWEDTOSETPTACTIVESTATUS;
    @JsonProperty("IS_USER_ALLOWED_TO_SET_PT_NOTES")
    private String iSUSERALLOWEDTOSETPTNOTES;
    @JsonProperty("IS_USER_ALLOWED_TO_SET_PT_NOTIFICATIONS")
    private String iSUSERALLOWEDTOSETPTNOTIFICATIONS;
    @JsonProperty("PATIENT_COUNT")
    private Integer pATIENTCOUNT;
    @JsonProperty("TITLE")
    private String tITLE;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Cohort(){}

    @JsonProperty("ACTIVE")
    public String getACTIVE() {
        return aCTIVE;
    }

    @JsonProperty("ACTIVE")
    public void setACTIVE(String aCTIVE) {
        this.aCTIVE = aCTIVE;
    }

    @JsonProperty("ACTIVE_PATIENT_COUNT")
    public Integer getACTIVEPATIENTCOUNT() {
        return aCTIVEPATIENTCOUNT;
    }

    @JsonProperty("ACTIVE_PATIENT_COUNT")
    public void setACTIVEPATIENTCOUNT(Integer aCTIVEPATIENTCOUNT) {
        this.aCTIVEPATIENTCOUNT = aCTIVEPATIENTCOUNT;
    }

    @JsonProperty("CHT_COHORT_ID")
    public Integer getCHTCOHORTID() {
        return cHTCOHORTID;
    }

    @JsonProperty("CHT_COHORT_ID")
    public void setCHTCOHORTID(Integer cHTCOHORTID) {
        this.cHTCOHORTID = cHTCOHORTID;
    }

    @JsonProperty("CHT_COHORT_IS_IDENTIFIED_IND")
    public String getCHTCOHORTISIDENTIFIEDIND() {
        return cHTCOHORTISIDENTIFIEDIND;
    }

    @JsonProperty("CHT_COHORT_IS_IDENTIFIED_IND")
    public void setCHTCOHORTISIDENTIFIEDIND(String cHTCOHORTISIDENTIFIEDIND) {
        this.cHTCOHORTISIDENTIFIEDIND = cHTCOHORTISIDENTIFIEDIND;
    }

    @JsonProperty("COHORT_TEMPLATE_SOURCE_ID")
    public String getCOHORTTEMPLATESOURCEID() {
        return cOHORTTEMPLATESOURCEID;
    }

    @JsonProperty("COHORT_TEMPLATE_SOURCE_ID")
    public void setCOHORTTEMPLATESOURCEID(String cOHORTTEMPLATESOURCEID) {
        this.cOHORTTEMPLATESOURCEID = cOHORTTEMPLATESOURCEID;
    }

    @JsonProperty("CREATEDT")
    public String getCREATEDT() {
        return cREATEDT;
    }

    @JsonProperty("CREATEDT")
    public void setCREATEDT(String cREATEDT) {
        this.cREATEDT = cREATEDT;
    }

    @JsonProperty("DATALINEPROJECT")
    public String getDATALINEPROJECT() {
        return dATALINEPROJECT;
    }

    @JsonProperty("DATALINEPROJECT")
    public void setDATALINEPROJECT(String dATALINEPROJECT) {
        this.dATALINEPROJECT = dATALINEPROJECT;
    }

    @JsonProperty("DESC")
    public String getDESC() {
        return dESC;
    }

    @JsonProperty("DESC")
    public void setDESC(String dESC) {
        this.dESC = dESC;
    }

    @JsonProperty("INACTIVE_PATIENT_COUNT")
    public Integer getINACTIVEPATIENTCOUNT() {
        return iNACTIVEPATIENTCOUNT;
    }

    @JsonProperty("INACTIVE_PATIENT_COUNT")
    public void setINACTIVEPATIENTCOUNT(Integer iNACTIVEPATIENTCOUNT) {
        this.iNACTIVEPATIENTCOUNT = iNACTIVEPATIENTCOUNT;
    }

    @JsonProperty("IRBNO")
    public String getIRBNO() {
        return iRBNO;
    }

    @JsonProperty("IRBNO")
    public void setIRBNO(String iRBNO) {
        this.iRBNO = iRBNO;
    }

    @JsonProperty("IRBWAIVER")
    public String getIRBWAIVER() {
        return iRBWAIVER;
    }

    @JsonProperty("IRBWAIVER")
    public void setIRBWAIVER(String iRBWAIVER) {
        this.iRBWAIVER = iRBWAIVER;
    }

    @JsonProperty("IS_DDP_PATIENT_ID_DRIVEN_COHORT")
    public String getISDDPPATIENTIDDRIVENCOHORT() {
        return iSDDPPATIENTIDDRIVENCOHORT;
    }

    @JsonProperty("IS_DDP_PATIENT_ID_DRIVEN_COHORT")
    public void setISDDPPATIENTIDDRIVENCOHORT(String iSDDPPATIENTIDDRIVENCOHORT) {
        this.iSDDPPATIENTIDDRIVENCOHORT = iSDDPPATIENTIDDRIVENCOHORT;
    }

    @JsonProperty("IS_PERSONAL_COHORT")
    public String getISPERSONALCOHORT() {
        return iSPERSONALCOHORT;
    }

    @JsonProperty("IS_PERSONAL_COHORT")
    public void setISPERSONALCOHORT(String iSPERSONALCOHORT) {
        this.iSPERSONALCOHORT = iSPERSONALCOHORT;
    }

    @JsonProperty("IS_USER_ALLOWED_TO_ADD_PT")
    public String getISUSERALLOWEDTOADDPT() {
        return iSUSERALLOWEDTOADDPT;
    }

    @JsonProperty("IS_USER_ALLOWED_TO_ADD_PT")
    public void setISUSERALLOWEDTOADDPT(String iSUSERALLOWEDTOADDPT) {
        this.iSUSERALLOWEDTOADDPT = iSUSERALLOWEDTOADDPT;
    }

    @JsonProperty("IS_USER_ALLOWED_TO_DELETE_COHORT")
    public String getISUSERALLOWEDTODELETECOHORT() {
        return iSUSERALLOWEDTODELETECOHORT;
    }

    @JsonProperty("IS_USER_ALLOWED_TO_DELETE_COHORT")
    public void setISUSERALLOWEDTODELETECOHORT(String iSUSERALLOWEDTODELETECOHORT) {
        this.iSUSERALLOWEDTODELETECOHORT = iSUSERALLOWEDTODELETECOHORT;
    }

    @JsonProperty("IS_USER_ALLOWED_TO_DROP_PT")
    public String getISUSERALLOWEDTODROPPT() {
        return iSUSERALLOWEDTODROPPT;
    }

    @JsonProperty("IS_USER_ALLOWED_TO_DROP_PT")
    public void setISUSERALLOWEDTODROPPT(String iSUSERALLOWEDTODROPPT) {
        this.iSUSERALLOWEDTODROPPT = iSUSERALLOWEDTODROPPT;
    }

    @JsonProperty("IS_USER_ALLOWED_TO_EDIT_COHORT_DESCRIPTION")
    public String getISUSERALLOWEDTOEDITCOHORTDESCRIPTION() {
        return iSUSERALLOWEDTOEDITCOHORTDESCRIPTION;
    }

    @JsonProperty("IS_USER_ALLOWED_TO_EDIT_COHORT_DESCRIPTION")
    public void setISUSERALLOWEDTOEDITCOHORTDESCRIPTION(String iSUSERALLOWEDTOEDITCOHORTDESCRIPTION) {
        this.iSUSERALLOWEDTOEDITCOHORTDESCRIPTION = iSUSERALLOWEDTOEDITCOHORTDESCRIPTION;
    }

    @JsonProperty("IS_USER_ALLOWED_TO_EDIT_COHORT_TITLE")
    public String getISUSERALLOWEDTOEDITCOHORTTITLE() {
        return iSUSERALLOWEDTOEDITCOHORTTITLE;
    }

    @JsonProperty("IS_USER_ALLOWED_TO_EDIT_COHORT_TITLE")
    public void setISUSERALLOWEDTOEDITCOHORTTITLE(String iSUSERALLOWEDTOEDITCOHORTTITLE) {
        this.iSUSERALLOWEDTOEDITCOHORTTITLE = iSUSERALLOWEDTOEDITCOHORTTITLE;
    }

    @JsonProperty("IS_USER_ALLOWED_TO_SET_PT_ACTIVE_STATUS")
    public String getISUSERALLOWEDTOSETPTACTIVESTATUS() {
        return iSUSERALLOWEDTOSETPTACTIVESTATUS;
    }

    @JsonProperty("IS_USER_ALLOWED_TO_SET_PT_ACTIVE_STATUS")
    public void setISUSERALLOWEDTOSETPTACTIVESTATUS(String iSUSERALLOWEDTOSETPTACTIVESTATUS) {
        this.iSUSERALLOWEDTOSETPTACTIVESTATUS = iSUSERALLOWEDTOSETPTACTIVESTATUS;
    }

    @JsonProperty("IS_USER_ALLOWED_TO_SET_PT_NOTES")
    public String getISUSERALLOWEDTOSETPTNOTES() {
        return iSUSERALLOWEDTOSETPTNOTES;
    }

    @JsonProperty("IS_USER_ALLOWED_TO_SET_PT_NOTES")
    public void setISUSERALLOWEDTOSETPTNOTES(String iSUSERALLOWEDTOSETPTNOTES) {
        this.iSUSERALLOWEDTOSETPTNOTES = iSUSERALLOWEDTOSETPTNOTES;
    }

    @JsonProperty("IS_USER_ALLOWED_TO_SET_PT_NOTIFICATIONS")
    public String getISUSERALLOWEDTOSETPTNOTIFICATIONS() {
        return iSUSERALLOWEDTOSETPTNOTIFICATIONS;
    }

    @JsonProperty("IS_USER_ALLOWED_TO_SET_PT_NOTIFICATIONS")
    public void setISUSERALLOWEDTOSETPTNOTIFICATIONS(String iSUSERALLOWEDTOSETPTNOTIFICATIONS) {
        this.iSUSERALLOWEDTOSETPTNOTIFICATIONS = iSUSERALLOWEDTOSETPTNOTIFICATIONS;
    }

    @JsonProperty("PATIENT_COUNT")
    public Integer getPATIENTCOUNT() {
        return pATIENTCOUNT;
    }

    @JsonProperty("PATIENT_COUNT")
    public void setPATIENTCOUNT(Integer pATIENTCOUNT) {
        this.pATIENTCOUNT = pATIENTCOUNT;
    }

    @JsonProperty("TITLE")
    public String getTITLE() {
        return tITLE;
    }

    @JsonProperty("TITLE")
    public void setTITLE(String tITLE) {
        this.tITLE = tITLE;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}

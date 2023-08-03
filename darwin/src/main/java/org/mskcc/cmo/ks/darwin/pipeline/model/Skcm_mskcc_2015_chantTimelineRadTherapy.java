/*
 * Copyright (c) 2016, 2023 Memorial Sloan Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan Kettering Cancer
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
import org.cbioportal.cmo.pipelines.common.util.ClinicalValueUtil;

/**
 *
 * @author heinsz
 */
public class Skcm_mskcc_2015_chantTimelineRadTherapy implements Skcm_mskcc_2015_chantTimelineRecord {

    private String melrtPtid;
    private String melrtRttxTypeDesc;
    private String melrtRttxAdjDesc;
    private Integer melrtRttxStrtYear;
    private Integer melrtRttxEndDt;
    private Integer melatRttxDaysDuration;

    public Skcm_mskcc_2015_chantTimelineRadTherapy() {}

    public Skcm_mskcc_2015_chantTimelineRadTherapy(String melrtPtid,
            String melrtRttxTypeDesc,
            String melrtRttxAdjDesc,
            Integer melrtRttxStrtYear,
            Integer melrtRttxEndDt,
            Integer melatRttxDaysDuration
            ) {
        this.melrtPtid = ClinicalValueUtil.defaultWithNA(melrtPtid);
        this.melrtRttxTypeDesc = ClinicalValueUtil.defaultWithNA(melrtRttxTypeDesc);
        this.melrtRttxAdjDesc = ClinicalValueUtil.defaultWithNA(melrtRttxAdjDesc);
        this.melrtRttxStrtYear = melrtRttxStrtYear != null ? melrtRttxStrtYear : -1;
        this.melrtRttxEndDt = melrtRttxEndDt != null ? melrtRttxEndDt : -1;
        this.melatRttxDaysDuration = melatRttxDaysDuration != null ? melatRttxDaysDuration : -1;
    }

    public String getMELRT_PTID() {
        return melrtPtid;
    }

    public void setMELRT_PTID(String melrtPtid) {
        this.melrtPtid = melrtPtid;
    }

    public String getMELRT_RTTX_TYPE_DESC() {
        return melrtRttxTypeDesc;
    }

    public void setMELRT_RTTX_TYPE_DESC(String melrtRttxTypeDesc) {
        this.melrtRttxTypeDesc = melrtRttxTypeDesc;
    }

    public String getMELRT_RTTX_ADJ_DESC() {
        return melrtRttxAdjDesc;
    }

    public void setMELRT_RTTX_ADJ_DESC(String melrtRttxAdjDesc) {
        this.melrtRttxAdjDesc = melrtRttxAdjDesc;
    }

    public Integer getMELRT_RTTX_STRT_YEAR() {
        return melrtRttxStrtYear;
    }

    public void setMELRT_RTTX_STRT_YEAR(Integer melrtRttxStrtYear) {
        this.melrtRttxStrtYear = melrtRttxStrtYear;
    }

    public Integer getMELRT_RTTX_END_DT() {
        return melrtRttxEndDt;
    }

    public void setMELRT_RTTX_END_DT(Integer melrtRttxEndDt) {
        this.melrtRttxEndDt = melrtRttxEndDt;
    }

    public Integer getMELAT_RTTX_DAYS_DURATION() {
        return melatRttxDaysDuration;
    }

    public void setMELAT_RTTX_DAYS_DURATION(Integer melatRttxDaysDuration) {
        this.melatRttxDaysDuration = melatRttxDaysDuration;
    }

    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();

        fieldNames.add("PATIENT_ID");
        fieldNames.add("START_DATE");
        fieldNames.add("STOP_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("TREATMENT_TYPE");
        fieldNames.add("SUBTYPE");

        return fieldNames;
    }

    @Override
    public String getPATIENT_ID() {
        return melrtPtid;
    }

    // due to insufficient data in darwin, start date is 0
    @Override
    public String getSTART_DATE() {
        return "0";
    }

    @Override
    public String getSTOP_DATE() {
        return String.valueOf(melatRttxDaysDuration);
    }

    @Override
    public String getEVENT_TYPE() {
        return "TREATMENT";
    }

    @Override
    public String getTREATMENT_TYPE() {
        return melrtRttxTypeDesc;

    }

    @Override
    public String getSUBTYPE() {
        return melrtRttxAdjDesc;
    }

}

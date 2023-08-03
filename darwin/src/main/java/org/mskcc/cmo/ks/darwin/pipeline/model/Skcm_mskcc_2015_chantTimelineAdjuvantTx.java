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
public class Skcm_mskcc_2015_chantTimelineAdjuvantTx implements Skcm_mskcc_2015_chantTimelineRecord {

    private String melatPtid;
    private Integer melatAdjtxTypCd;
    private String melatAdjtxTypDesc;
    private String melatAdjtxTypeOth;
    private Integer melatAdjtxStrtYear;
    private Integer melatAdjtxEndYear;
    private Integer melatAdjtxDaysDuration;

    public Skcm_mskcc_2015_chantTimelineAdjuvantTx() {}

    public Skcm_mskcc_2015_chantTimelineAdjuvantTx(String melatPtid,
            Integer melatAdjtxTypCd,
            String melatAdjtxTypDesc,
            String melatAdjtxTypeOth,
            Integer melatAdjtxStrtYear,
            Integer melatAdjtxEndYear,
            Integer melatAdjtxDaysDuration) {
        this.melatPtid = ClinicalValueUtil.defaultWithNA(melatPtid);
        this.melatAdjtxTypCd = melatAdjtxTypCd != null ? melatAdjtxTypCd : -1;
        this.melatAdjtxTypDesc = ClinicalValueUtil.defaultWithNA(melatAdjtxTypDesc);
        this.melatAdjtxTypeOth = ClinicalValueUtil.defaultWithNA(melatAdjtxTypeOth);
        this.melatAdjtxStrtYear = melatAdjtxStrtYear != null ? melatAdjtxStrtYear : -1;
        this.melatAdjtxEndYear = melatAdjtxEndYear != null ? melatAdjtxEndYear : -1;
        this.melatAdjtxDaysDuration = melatAdjtxDaysDuration != null ? melatAdjtxDaysDuration : -1;
    }

    public String getMELAT_PTID() {
        return melatPtid;
    }

    public void setMELAT_PTID(String melatPtid) {
        this.melatPtid = melatPtid;
    }

    public Integer getMELAT_ADJTX_TYP_CD() {
        return melatAdjtxTypCd;
    }

    public void setMELAT_ADJTX_TYP_CD(Integer melatAdjtxTypCd) {
        this.melatAdjtxTypCd = melatAdjtxTypCd;
    }

    public String getMELAT_ADJTX_TYP_DESC() {
        return melatAdjtxTypDesc;
    }

    public void setMELAT_ADJTX_TYP_DESC(String melatAdjtxTypDesc) {
        this.melatAdjtxTypDesc = melatAdjtxTypDesc;
    }

    public String getMELAT_ADJTX_TYPE_OTH() {
        return melatAdjtxTypeOth;
    }

    public void setMELAT_ADJTX_TYPE_OTH(String melatAdjtxTypeOth) {
        this.melatAdjtxTypeOth = melatAdjtxTypeOth;
    }

    public Integer getMELAT_ADJTX_STRT_YEAR() {
        return melatAdjtxStrtYear;
    }

    public void setMELAT_ADJTX_STRT_YEAR(Integer melatAdjtxStrtYear) {
        this.melatAdjtxStrtYear = melatAdjtxStrtYear;
    }

    public Integer getMELAT_ADJTX_END_YEAR() {
        return melatAdjtxEndYear;
    }

    public void setMELAT_ADJTX_END_YEAR(Integer melatAdjtxEndYear) {
        this.melatAdjtxEndYear = melatAdjtxEndYear;
    }

    public Integer getMELAT_ADJTX_DAYS_DURATION() {
        return melatAdjtxDaysDuration;
    }

    public void setMELAT_ADJTX_DAYS_DURATION(Integer melatAdjtxDaysDuration) {
        this.melatAdjtxDaysDuration = melatAdjtxDaysDuration;
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
        return melatPtid;
    }

    // due to insufficient data in darwin, start date is 0
    @Override
    public String getSTART_DATE() {
        return "0";
    }

    @Override
    public String getSTOP_DATE() {
        return String.valueOf(melatAdjtxDaysDuration);
    }

    @Override
    public String getEVENT_TYPE() {
        return "TREATMENT";
    }

    @Override
    public String getTREATMENT_TYPE() {
        return melatAdjtxTypDesc;

    }

    @Override
    public String getSUBTYPE() {
        return melatAdjtxTypeOth;
    }
}

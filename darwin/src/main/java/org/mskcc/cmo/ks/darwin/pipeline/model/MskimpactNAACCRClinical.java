/*
 * Copyright (c) 2017, 2023 Memorial Sloan Kettering Cancer Center.
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
 * published by the Free Software Foundation, either version 3 2of the
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

import java.util.*;
import org.cbioportal.cmo.pipelines.common.util.ClinicalValueUtil;

/**
 *
 * @author heinsz
 */
public class MskimpactNAACCRClinical {
    private String DMP_ID_DEMO;
    private String PT_NAACCR_SEX_CODE;
    private String PT_NAACCR_RACE_CODE_PRIMARY;
    private String PT_NAACCR_RACE_CODE_SECONDARY;
    private String PT_NAACCR_RACE_CODE_TERTIARY;
    private String PT_NAACCR_ETHNICITY_CODE;
    private String PT_BIRTH_YEAR;

    public MskimpactNAACCRClinical() {}

    public MskimpactNAACCRClinical(String DMP_ID_DEMO, String PT_NAACCR_SEX_CODE, String PT_NAACCR_RACE_CODE_PRIMARY,
            String PT_NAACCR_RACE_CODE_SECONDARY, String PT_NAACCR_RACE_CODE_TERTIARY, String PT_NAACCR_ETHNICITY_CODE,
            String PT_BIRTH_YEAR) {
        this.DMP_ID_DEMO = ClinicalValueUtil.defaultWithNA(DMP_ID_DEMO);
        this.PT_NAACCR_SEX_CODE = ClinicalValueUtil.defaultWithNA(PT_NAACCR_SEX_CODE);
        this.PT_NAACCR_RACE_CODE_PRIMARY = ClinicalValueUtil.defaultWithNA(PT_NAACCR_RACE_CODE_PRIMARY);
        this.PT_NAACCR_RACE_CODE_SECONDARY = ClinicalValueUtil.defaultWithNA(PT_NAACCR_RACE_CODE_SECONDARY);
        this.PT_NAACCR_RACE_CODE_TERTIARY = ClinicalValueUtil.defaultWithNA(PT_NAACCR_RACE_CODE_TERTIARY);
        this.PT_NAACCR_ETHNICITY_CODE = ClinicalValueUtil.defaultWithNA(PT_NAACCR_ETHNICITY_CODE);
        this.PT_BIRTH_YEAR = ClinicalValueUtil.defaultWithNA(PT_BIRTH_YEAR);
    }


    public String getDMP_ID_DEMO() {
        return DMP_ID_DEMO;
    }

    public void setDMP_ID_DEMO(String DMP_ID_DEMO) {
        this.DMP_ID_DEMO = ClinicalValueUtil.defaultWithNA(DMP_ID_DEMO);
    }

    public String getPT_NAACCR_SEX_CODE() {
        return PT_NAACCR_SEX_CODE;
    }

    public void setPT_NAACCR_SEX_CODE(String PT_NAACCR_SEX_CODE) {
        this.PT_NAACCR_SEX_CODE = ClinicalValueUtil.defaultWithNA(PT_NAACCR_SEX_CODE);
    }

    public String getPT_NAACCR_RACE_CODE_PRIMARY() {
        return PT_NAACCR_RACE_CODE_PRIMARY;
    }

    public void setPT_NAACCR_RACE_CODE_PRIMARY(String PT_NAACCR_RACE_CODE_PRIMARY) {
        this.PT_NAACCR_RACE_CODE_PRIMARY = ClinicalValueUtil.defaultWithNA(PT_NAACCR_RACE_CODE_PRIMARY);
    }

    public String getPT_NAACCR_RACE_CODE_SECONDARY() {
        return PT_NAACCR_RACE_CODE_SECONDARY;
    }

    public void setPT_NAACCR_RACE_CODE_SECONDARY(String PT_NAACCR_RACE_CODE_SECONDARY) {
        this.PT_NAACCR_RACE_CODE_SECONDARY = ClinicalValueUtil.defaultWithNA(PT_NAACCR_RACE_CODE_SECONDARY);
    }

    public String getPT_NAACCR_RACE_CODE_TERTIARY() {
        return PT_NAACCR_RACE_CODE_TERTIARY;
    }

    public void setPT_NAACCR_RACE_CODE_TERTIARY(String PT_NAACCR_RACE_CODE_TERTIARY) {
        this.PT_NAACCR_RACE_CODE_TERTIARY = ClinicalValueUtil.defaultWithNA(PT_NAACCR_RACE_CODE_TERTIARY);
    }

    public String getPT_NAACCR_ETHNICITY_CODE() {
        return PT_NAACCR_ETHNICITY_CODE;
    }

    public void setPT_NAACCR_ETHNICITY_CODE(String PT_NAACCR_ETHNICITY_CODE) {
        this.PT_NAACCR_ETHNICITY_CODE = ClinicalValueUtil.defaultWithNA(PT_NAACCR_ETHNICITY_CODE);
    }

    public String getPT_BIRTH_YEAR() {
        return PT_BIRTH_YEAR;
    }

    public void setPT_BIRTH_YEAR(String PT_BIRTH_YEAR) {
        this.PT_BIRTH_YEAR = ClinicalValueUtil.defaultWithNA(PT_BIRTH_YEAR);
    }

    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("DMP_ID_DEMO");
        fieldNames.add("PT_NAACCR_SEX_CODE");
        fieldNames.add("PT_NAACCR_RACE_CODE_PRIMARY");
        fieldNames.add("PT_NAACCR_RACE_CODE_SECONDARY");
        fieldNames.add("PT_NAACCR_RACE_CODE_TERTIARY");
        fieldNames.add("PT_NAACCR_ETHNICITY_CODE");
        fieldNames.add("PT_BIRTH_YEAR");
        return fieldNames;
    }

    public List<String> getHeaders() {
        List<String> headerNames = new ArrayList<>();
        headerNames.add("PATIENT_ID");
        headerNames.add("NAACCR_SEX_CODE");
        headerNames.add("NAACCR_RACE_CODE_PRIMARY");
        headerNames.add("NAACCR_RACE_CODE_SECONDARY");
        headerNames.add("NAACCR_RACE_CODE_TERTIARY");
        headerNames.add("NAACCR_ETHNICITY_CODE");
        headerNames.add("BIRTH_YEAR");
        return headerNames;
    }
}

/*
 * Copyright (c) 2019, 2024 Memorial Sloan Kettering Cancer Center.
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

package org.mskcc.cmo.ks.ddp.pipeline.model;

import org.mskcc.cmo.ks.ddp.pipeline.util.DDPUtils;
import org.mskcc.cmo.ks.ddp.source.composite.DDPCompositeRecord;

import java.text.ParseException;
import java.util.*;

/**
 *
 * @author ochoaa
 */
public class SuppNaaccrMappingsRecord {

    private String PATIENT_ID;
    private String NAACCR_SEX_CODE;
    private String NAACCR_RACE_CODE_PRIMARY;
    private String NAACCR_RACE_CODE_SECONDARY;
    private String NAACCR_RACE_CODE_TERTIARY;
    private String NAACCR_ETHNICITY_CODE;
    private String BIRTH_YEAR;

    public SuppNaaccrMappingsRecord(){}

    public SuppNaaccrMappingsRecord(DDPCompositeRecord compositeRecord) throws ParseException {
        this.PATIENT_ID = compositeRecord.getDmpPatientId();
        this.NAACCR_SEX_CODE = DDPUtils.resolveNaaccrSexCode(compositeRecord.getPatientSex());
        this.NAACCR_RACE_CODE_PRIMARY = DDPUtils.resolveNaaccrRaceCode(compositeRecord.getPatientRace());
        this.NAACCR_RACE_CODE_SECONDARY = "NA";
        this.NAACCR_RACE_CODE_TERTIARY = "NA";
        this.NAACCR_ETHNICITY_CODE = DDPUtils.resolveNaaccrEthnicityCode(compositeRecord.getPatientEthnicity());
        this.BIRTH_YEAR = DDPUtils.parseYearFromDateAsString(compositeRecord.getPatientBirthDate());
    }

    /**
     * @return the PATIENT_ID
     */
    public String getPATIENT_ID() {
        return PATIENT_ID;
    }

    /**
     * @param PATIENT_ID the PATIENT_ID to set
     */
    public void setPATIENT_ID(String PATIENT_ID) {
        this.PATIENT_ID = PATIENT_ID;
    }

    /**
     * @return the NAACCR_SEX_CODE
     */
    public String getNAACCR_SEX_CODE() {
        return NAACCR_SEX_CODE;
    }

    /**
     * @param NAACCR_SEX_CODE the NAACCR_SEX_CODE to set
     */
    public void setNAACCR_SEX_CODE(String NAACCR_SEX_CODE) {
        this.NAACCR_SEX_CODE = NAACCR_SEX_CODE;
    }

    /**
     * @return the NAACCR_RACE_CODE_PRIMARY
     */
    public String getNAACCR_RACE_CODE_PRIMARY() {
        return NAACCR_RACE_CODE_PRIMARY;
    }

    /**
     * @param NAACCR_RACE_CODE_PRIMARY the NAACCR_RACE_CODE_PRIMARY to set
     */
    public void setNAACCR_RACE_CODE_PRIMARY(String NAACCR_RACE_CODE_PRIMARY) {
        this.NAACCR_RACE_CODE_PRIMARY = NAACCR_RACE_CODE_PRIMARY;
    }

    /**
     * @return the NAACCR_RACE_CODE_SECONDARY
     */
    public String getNAACCR_RACE_CODE_SECONDARY() {
        return NAACCR_RACE_CODE_SECONDARY;
    }

    /**
     * @param NAACCR_RACE_CODE_SECONDARY the NAACCR_RACE_CODE_SECONDARY to set
     */
    public void setNAACCR_RACE_CODE_SECONDARY(String NAACCR_RACE_CODE_SECONDARY) {
        this.NAACCR_RACE_CODE_SECONDARY = NAACCR_RACE_CODE_SECONDARY;
    }

    /**
     * @return the NAACCR_RACE_CODE_TERTIARY
     */
    public String getNAACCR_RACE_CODE_TERTIARY() {
        return NAACCR_RACE_CODE_TERTIARY;
    }

    /**
     * @param NAACCR_RACE_CODE_TERTIARY the NAACCR_RACE_CODE_TERTIARY to set
     */
    public void setNAACCR_RACE_CODE_TERTIARY(String NAACCR_RACE_CODE_TERTIARY) {
        this.NAACCR_RACE_CODE_TERTIARY = NAACCR_RACE_CODE_TERTIARY;
    }

    /**
     * @return the NAACCR_ETHNICITY_CODE
     */
    public String getNAACCR_ETHNICITY_CODE() {
        return NAACCR_ETHNICITY_CODE;
    }

    /**
     * @param NAACCR_ETHNICITY_CODE the NAACCR_ETHNICITY_CODE to set
     */
    public void setNAACCR_ETHNICITY_CODE(String NAACCR_ETHNICITY_CODE) {
        this.NAACCR_ETHNICITY_CODE = NAACCR_ETHNICITY_CODE;
    }

    /**
     * @return the BIRTH_YEAR
     */
    public String getBIRTH_YEAR() {
        return BIRTH_YEAR;
    }

    /**
     * @param BIRTH_YEAR the BIRTH_YEAR to set
     */
    public void setBIRTH_YEAR(String BIRTH_YEAR) {
        this.BIRTH_YEAR = BIRTH_YEAR;
    }

    /**
     * Returns field names as list of strings.
     *
     * @return
     */
    public static List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("NAACCR_SEX_CODE");
        fieldNames.add("NAACCR_RACE_CODE_PRIMARY");
        fieldNames.add("NAACCR_RACE_CODE_SECONDARY");
        fieldNames.add("NAACCR_RACE_CODE_TERTIARY");
        fieldNames.add("NAACCR_ETHNICITY_CODE");
        fieldNames.add("BIRTH_YEAR");
        return fieldNames;
    }
}

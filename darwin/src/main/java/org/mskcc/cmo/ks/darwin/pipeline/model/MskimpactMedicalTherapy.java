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
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Class to model relevant data from record within DVCBIO.PHARMACY_V.
 *
 * @author Benjamin Gross
 */
public class MskimpactMedicalTherapy implements Comparable<MskimpactMedicalTherapy>
{
    private String PT_ID_PHARMACY;
    private String DMP_ID_PHARMACY;
    private Integer AGE_AT_DISPENSE_DATE_IN_DAYS;
    private String GENERIC_DRUG_NAME;
    private Float DOSAGE;
    private String DOSE_UNIT;
    private Float DISPENSED_QUANTITY;
    private Integer START_DATE;
    private Integer STOP_DATE;
    private String SAMPLE_ID_PATH_DMP;

    public MskimpactMedicalTherapy() {}

    /**
     * All fields are coming directly from DVCBIO.PHARMACY_V
     */
    public MskimpactMedicalTherapy(String PT_ID_PHARMACY, String DMP_ID_PHARMACY,
                                   Integer AGE_AT_DISPENSE_DATE_IN_DAYS, String GENERIC_DRUG_NAME,
                                   Float DOSAGE, String DOSE_UNIT, Float DISPENSED_QUANTITY, String SAMPLE_ID_PATH_DMP) {
        this(PT_ID_PHARMACY, DMP_ID_PHARMACY, AGE_AT_DISPENSE_DATE_IN_DAYS,
             GENERIC_DRUG_NAME, DOSAGE, DOSE_UNIT, DISPENSED_QUANTITY,
             AGE_AT_DISPENSE_DATE_IN_DAYS, AGE_AT_DISPENSE_DATE_IN_DAYS,SAMPLE_ID_PATH_DMP);
    }

    /**
     * All fields are coming from DVCBIO.PHARMACY_V with the exception of
     * START_DATE, STOP_DATE  which are chosen "AGE_AT_DISPENSE_DATE_IN_DAYS" of patient
     * - see MksimpactMedicalTherapyProcessor for more information.
     */
    public MskimpactMedicalTherapy(String PT_ID_PHARMACY, String DMP_ID_PHARMACY,
                                   Integer AGE_AT_DISPENSE_DATE_IN_DAYS, String GENERIC_DRUG_NAME,
                                   Float DOSAGE, String DOSE_UNIT, Float DISPENSED_QUANTITY,
                                   Integer START_DATE, Integer STOP_DATE, String SAMPLE_ID_PATH_DMP) {
        this.PT_ID_PHARMACY = PT_ID_PHARMACY.trim();
        this.DMP_ID_PHARMACY = DMP_ID_PHARMACY.trim();
        this.AGE_AT_DISPENSE_DATE_IN_DAYS = AGE_AT_DISPENSE_DATE_IN_DAYS;
        this.GENERIC_DRUG_NAME = GENERIC_DRUG_NAME.trim();
        this.DOSAGE = DOSAGE;
        this.DOSE_UNIT = DOSE_UNIT.trim();
        this.DISPENSED_QUANTITY = DISPENSED_QUANTITY;
        this.START_DATE = START_DATE;
        this.STOP_DATE = STOP_DATE;
        this.SAMPLE_ID_PATH_DMP = SAMPLE_ID_PATH_DMP;
    }

    public String getPT_ID_PHARMACY() {
        return PT_ID_PHARMACY;
    }

    public String getDMP_ID_PHARMACY() {
        return DMP_ID_PHARMACY;
    }

    public Integer getAGE_AT_DISPENSE_DATE_IN_DAYS() {
        return AGE_AT_DISPENSE_DATE_IN_DAYS;
    }

    public String getGENERIC_DRUG_NAME() {
        return GENERIC_DRUG_NAME;
    }

    public Float getDOSAGE() {
        return DOSAGE;
    }

    public String getDOSE_UNIT() {
        return DOSE_UNIT;
    }

    public Float getDISPENSED_QUANTITY() {
        return DISPENSED_QUANTITY;
    }

    public Integer getSTART_DATE() {
        return START_DATE;
    }

    public Integer getSTOP_DATE() {
        return STOP_DATE;
    }

    /**
     * @return the SAMPLE_ID_PATH_DMP
     */
    public String getSAMPLE_ID_PATH_DMP() {
        return SAMPLE_ID_PATH_DMP;
    }

    /**
     * @param SAMPLE_ID_PATH_DMP the SAMPLE_ID_PATH_DMP to set
     */
    public void setSAMPLE_ID_PATH_DMP(String SAMPLE_ID_PATH_DMP) {
        this.SAMPLE_ID_PATH_DMP = SAMPLE_ID_PATH_DMP;
    }

    public static List<String> getHeaders(){
        return Arrays.asList("PATIENT_ID", "START_DATE", "STOP_DATE", "EVENT_TYPE",
                             "TREATMENT_TYPE", "SUBTYPE", "AGENT", "DOSAGE");
    }

    /**
     * MskimpactMedicalTherapyProcessor will sort a collection of these records
     * by AGE_AT_DISPENSE_DATE_IN_DAYS in order to create a START_DATE-STOP_DATE range.
     */
    @Override
    public int compareTo(MskimpactMedicalTherapy that) {
        if (this.AGE_AT_DISPENSE_DATE_IN_DAYS < that.AGE_AT_DISPENSE_DATE_IN_DAYS) return -1;
        if (this.AGE_AT_DISPENSE_DATE_IN_DAYS > that.AGE_AT_DISPENSE_DATE_IN_DAYS) return 1;
        return 0;
    }

    /**
     * This is called to create a record with within the timeline file
     */
    @Override
    public String toString() {
        return (DMP_ID_PHARMACY + "\t" + START_DATE + "\t" + STOP_DATE + "\t" +
                "TREATMENT\tMedical Therapy\tImmunotherapy\t" +
                GENERIC_DRUG_NAME + "\t" + Math.round(DOSAGE) + " " + DOSE_UNIT);
    }
}

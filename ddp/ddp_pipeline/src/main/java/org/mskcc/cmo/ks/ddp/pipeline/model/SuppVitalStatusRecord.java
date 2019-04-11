/*
 * Copyright (c) 2019 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.ddp.pipeline.model;

import org.mskcc.cmo.ks.ddp.pipeline.util.DDPUtils;
import org.mskcc.cmo.ks.ddp.source.composite.DDPCompositeRecord;

import com.google.common.base.Strings;
import java.text.ParseException;
import java.util.*;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author ochoaa
 */
public class SuppVitalStatusRecord {

    private String PATIENT_ID;
    private String YEAR_CONTACT;
    private String YEAR_DEATH;
    private String INT_CONTACT;
    private String INT_DOD;
    private String DEAD;

    public SuppVitalStatusRecord(){}

    public SuppVitalStatusRecord(DDPCompositeRecord compositeRecord) throws ParseException {
        this.PATIENT_ID = compositeRecord.getDmpPatientId();
        this.YEAR_CONTACT = DDPUtils.parseYearFromDateAsString(compositeRecord.getLastContactDate());
        this.YEAR_DEATH = DDPUtils.parseYearFromDateAsString(compositeRecord.getPatientDeathDate());
        this.INT_CONTACT = DDPUtils.getDateInDaysAsString(compositeRecord.getLastContactDate());
        this.INT_DOD = DDPUtils.getDateInDaysAsString(compositeRecord.getPatientDeathDate());
        this.DEAD = (!Strings.isNullOrEmpty(compositeRecord.getPatientDeathDate())) ? "True" : "False";
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
     * @return the YEAR_CONTACT
     */
    public String getYEAR_CONTACT() {
        return YEAR_CONTACT;
    }

    /**
     * @param YEAR_CONTACT the YEAR_CONTACT to set
     */
    public void setYEAR_CONTACT(String YEAR_CONTACT) {
        this.YEAR_CONTACT = YEAR_CONTACT;
    }

    /**
     * @return the YEAR_DEATH
     */
    public String getYEAR_DEATH() {
        return YEAR_DEATH;
    }

    /**
     * @param YEAR_DEATH the YEAR_DEATH to set
     */
    public void setYEAR_DEATH(String YEAR_DEATH) {
        this.YEAR_DEATH = YEAR_DEATH;
    }

    /**
     * @return the INT_CONTACT
     */
    public String getINT_CONTACT() {
        return INT_CONTACT;
    }

    /**
     * @param INT_CONTACT the INT_CONTACT to set
     */
    public void setINT_CONTACT(String INT_CONTACT) {
        this.INT_CONTACT = INT_CONTACT;
    }

    /**
     * @return the INT_DOD
     */
    public String getINT_DOD() {
        return INT_DOD;
    }

    /**
     * @param INT_DOD the INT_DOD to set
     */
    public void setINT_DOD(String INT_DOD) {
        this.INT_DOD = INT_DOD;
    }

    /**
     * @return the DEAD
     */
    public String getDEAD() {
        return DEAD;
    }

    /**
     * @param DEAD the DEAD to set
     */
    public void setDEAD(String DEAD) {
        this.DEAD = DEAD;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     * Returns field names as list of strings.
     *
     * @return
     */
    public static List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("YEAR_CONTACT");
        fieldNames.add("YEAR_DEATH");
        fieldNames.add("INT_CONTACT");
        fieldNames.add("INT_DOD");
        fieldNames.add("DEAD");
        return fieldNames;
    }
}

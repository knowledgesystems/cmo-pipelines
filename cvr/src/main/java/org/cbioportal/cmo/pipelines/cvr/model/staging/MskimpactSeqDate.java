/*
 * Copyright (c) 2016 - 2017 Memorial Sloan-Kettering Cancer Center.
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
package org.cbioportal.cmo.pipelines.cvr.model.staging;

import java.util.*;

/**
 *
 * @author heinsz
 */
public class MskimpactSeqDate {
    private String SAMPLE_ID;
    private String PATIENT_ID;
    private String SEQ_DATE;

    /**
     * @return the SAMPLE_ID
     */
    public String getSAMPLE_ID() {
        return SAMPLE_ID != null ? SAMPLE_ID : "";
    }

    /**
     * @param SAMPLE_ID the SAMPLE_ID to set
     */
    public void setSAMPLE_ID(String SAMPLE_ID) {
        this.SAMPLE_ID = SAMPLE_ID;
    }

    /**
     * @return the PATIENT_ID
     */
    public String getPATIENT_ID() {
        return PATIENT_ID != null ? PATIENT_ID : "";
    }

    /**
     * @param PATIENT_ID the PATIENT_ID to set
     */
    public void setPATIENT_ID(String PATIENT_ID) {
        this.PATIENT_ID = PATIENT_ID;
    }

    /**
     * @return the SEQ_DATE
     */
    public String getSEQ_DATE() {
        return SEQ_DATE != null ? SEQ_DATE : "";
    }

    /**
     * @param SEQ_DATE the SEQ_DATE to set
     */
    public void setSEQ_DATE(String SEQ_DATE) {
        this.SEQ_DATE = SEQ_DATE;
    }
    
    public static List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("SAMPLE_ID");
        fieldNames.add("PATIENT_ID");
        fieldNames.add("SEQ_DATE");
        return fieldNames;
    }
}

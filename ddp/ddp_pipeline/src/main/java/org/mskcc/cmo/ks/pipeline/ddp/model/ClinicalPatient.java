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

package org.mskcc.cmo.ks.pipeline.ddp.model;

import org.mskcc.cmo.ks.ddp.source.composite.CompositePatient;
import org.mskcc.cmo.ks.pipeline.ddp.util.DDPUtils;

import java.text.ParseException;

/**
 *
 * @author ochoaa
 */
public class ClinicalPatient {
    private String patientId;
    private String age;
    private String sex;
    private String osStatus;
    private String osMonths;

    public ClinicalPatient(){}

    public ClinicalPatient(CompositePatient compositePatient) throws ParseException {
        this.patientId = compositePatient.getDmpPatientId();
        this.age = DDPUtils.resolvePatientAge(compositePatient);
        this.sex = DDPUtils.resolvePatientSex(compositePatient);
        this.osStatus = DDPUtils.resolveOsStatus(compositePatient);
        this.osMonths = DDPUtils.resolveOsMonths(osStatus, compositePatient);
    }

    /**
     * @return the patientId
     */
    public String getPatientId() {
        return patientId;
    }

    /**
     * @param patientId the patientId to set
     */
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    /**
     * @return the age
     */
    public String getAge() {
        return age;
    }

    /**
     * @param age the age to set
     */
    public void setAge(String age) {
        this.age = age;
    }

    /**
     * @return the sex
     */
    public String getSex() {
        return sex;
    }

    /**
     * @param sex the sex to set
     */
    public void setSex(String sex) {
        this.sex = sex;
    }

    /**
     * @return the osStatus
     */
    public String getOsStatus() {
        return osStatus;
    }

    /**
     * @param osStatus the osStatus to set
     */
    public void setOsStatus(String osStatus) {
        this.osStatus = osStatus;
    }

    /**
     * @return the osMonths
     */
    public String getOsMonths() {
        return osMonths;
    }

    /**
     * @param osMonths the osMonths to set
     */
    public void setOsMonths(String osMonths) {
        this.osMonths = osMonths;
    }

}

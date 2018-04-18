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

package org.mskcc.cmo.ks.ddp.pipeline.util;

import org.mskcc.cmo.ks.ddp.source.composite.CompositePatient;
import org.mskcc.cmo.ks.ddp.source.model.PatientDiagnosis;

import com.google.common.base.Strings;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @author ochoaa
 */
public class DDPUtils {
    private static final Double DAYS_TO_YEARS_CONVERSION = 365.2422;
    private static final Double DAYS_TO_MONTHS_CONVERSION = 30.4167;

    /**
     * Resolve and anonymize patient age.
     *
     * If birth year is null/empty then return anonymized current age.
     * Otherwise calculate from difference between reference date and birth date,
     * where reference date is either date of death if pt is deceased or current date.
     *
     * @param compositePatient
     * @return
     * @throws ParseException
     */
    public static String resolvePatientAge(CompositePatient compositePatient) throws ParseException {
        // if patient birth year is null/empty then use current age value
        if (Strings.isNullOrEmpty(compositePatient.getPatientDemographics().getPTBIRTHDTE())) {
            return anonymizePatientAge(compositePatient.getPatientAge());
        }

        Long birthDateInDays = getDateInDays(compositePatient.getPatientDemographics().getPTBIRTHDTE());
        Long referenceDateInDays;
        // use current date as reference date if patient not deceased, otherwise use date of death
        if (!Strings.isNullOrEmpty(compositePatient.getPatientDemographics().getPTDEATHDTE())) {
            referenceDateInDays = getDateInDays(compositePatient.getPatientDemographics().getPTDEATHDTE());
        }
        else {
            referenceDateInDays = getDateInDays(new Date());
        }
        Double age = (referenceDateInDays - birthDateInDays) / (DAYS_TO_YEARS_CONVERSION);
        return anonymizePatientAge(age.intValue());
    }

    /**
     * Returns anonymized patient age as string.
     * @param age
     * @return
     */
    private static String anonymizePatientAge(Integer age) {
        if (age >= 90) {
            age = 90;
        }
        else if (age <= 18) {
            age = 18;
        }
        return String.valueOf(age);
    }

    /**
     * Standardize patient sex value.
     *
     * @param compositePatient
     * @return
     */
    public static String resolvePatientSex(CompositePatient compositePatient) {
        String sex = "NA";
        if (!Strings.isNullOrEmpty(compositePatient.getPatientSex())) {
            if (compositePatient.getPatientSex().equalsIgnoreCase("M") ||
                    compositePatient.getPatientSex().equalsIgnoreCase("MALE")) {
                sex = "Male";
            }
            else {
                sex = "Female";
            }
        }
        return sex;
    }

    /**
     * Standardize OS_STATUS value.
     *
     * @param compositePatient
     * @return
     */
    public static String resolveOsStatus(CompositePatient compositePatient) {
        if (compositePatient.getCohortPatientData().getPTVITALSTATUS().equalsIgnoreCase("ALIVE")) {
            return "LIVING";
        }
        else {
            return "DECEASED";
        }
    }

    /**
     * Calculate OS_MONTHS.
     *
     * Note: In some cases, patients may not have any tumor diagnoses in the system yet. These are NA.
     *
     * @param osStatus
     * @param compositePatient
     * @return
     * @throws ParseException
     */
    public static String resolveOsMonths(String osStatus, CompositePatient compositePatient) throws ParseException {
        String osMonths = "NA";
        Long referenceAgeInDays = (osStatus.equals("LIVING")) ?
                getDateInDays(compositePatient.getPatientDemographics().getPLALASTACTVDTE()) :
                getDateInDays(compositePatient.getPatientDemographics().getDeceasedDate());
        Long firsTumorDiagnosisDateInDays = getFirstTumorDiagnosisDateInDays(compositePatient.getPatientDiagnosis());
        if (referenceAgeInDays != null && firsTumorDiagnosisDateInDays != null) {
            osMonths = String.format("%.3f", (referenceAgeInDays - firsTumorDiagnosisDateInDays) / 30.4167);
        }
        return osMonths;
    }

    /**
     * Find and return the earliest patient tumor diagnosis date in days.
     *
     * @param patientDiagnosis
     * @return
     * @throws ParseException
     */
    private static Long getFirstTumorDiagnosisDateInDays(List<PatientDiagnosis> patientDiagnosis) throws ParseException {
        SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd");
        Date firstTumorDiagnosisDate = null;
        if (patientDiagnosis == null || patientDiagnosis.isEmpty()) {
            return null;
        }

        for (PatientDiagnosis diagnosis : patientDiagnosis) {
            if (Strings.isNullOrEmpty(diagnosis.getTumorDiagnosisDate())) {
                continue;
            }
            if (firstTumorDiagnosisDate == null) {
                firstTumorDiagnosisDate = sfd.parse(diagnosis.getTumorDiagnosisDate());
            }
            else {
                Date currentTumorDiagnosisDate = sfd.parse(diagnosis.getTumorDiagnosisDate());
                if (currentTumorDiagnosisDate.before(firstTumorDiagnosisDate)) {
                    firstTumorDiagnosisDate = currentTumorDiagnosisDate;
                }
            }
        }
        return getDateInDays(firstTumorDiagnosisDate);
    }

    /**
     * Calculates the date in days.
     *
     * @param date
     * @return
     * @throws ParseException
     */
    private static Long getDateInDays(Date date) throws ParseException {
        if (date != null) {
            return date.getTime() / (1000 * 60 * 60 * 24); // milliseconds -> minutes -> hours -> days
        }
        return null;
    }

    /**
     * Calculates the date in days.
     *
     * @param dateValue
     * @return
     * @throws ParseException
     */
    private static Long getDateInDays(String dateValue) throws ParseException {
        if (!Strings.isNullOrEmpty(dateValue)) {
            SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sfd.parse(dateValue);
            return date.getTime() / (1000 * 60 * 60 * 24); // milliseconds -> minutes -> hours -> days
        }
        return null;
    }
}

/*
 * Copyright (c) 2018 - 2021 Memorial Sloan-Kettering Cancer Center.
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

import org.mskcc.cmo.ks.ddp.source.composite.DDPCompositeRecord;
import org.mskcc.cmo.ks.ddp.source.model.PatientDiagnosis;

import com.google.common.base.Strings;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.io.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author ochoaa
 */
public class DDPUtils {

    public static final String MSKIMPACT_STUDY_ID = "mskimpact";
    public static final Double DAYS_TO_YEARS_CONVERSION = 365.2422;
    public static final Double DAYS_TO_MONTHS_CONVERSION = 30.4167;
    public static final List<String> NULL_EMPTY_VALUES = Arrays.asList(new String[]{"NA", "N/A"});

    private static Map<String, String> naaccrEthnicityMap;
    private static Map<String, String> naaccrRaceMap;
    private static Map<String, String> naaccrSexMap;
    private static Map<String, Date> patientFirstSeqDateMap = new HashMap<String, Date>();
    private static Set<String> patientsMissingSurvival = new HashSet<>();
    private static Boolean useSeqDateOsMonthsMethod = Boolean.FALSE;
    private static Set<String> patientsWithNegativeOsMonths = new HashSet<>();

    private static final Logger LOG = Logger.getLogger(DDPUtils.class);

    private static Long offsetMilliSecondsForCurrentTimeZone = null;
    private static Long millisecondsPerDay = 1000L * 60L * 60L * 24L;

    private static Long getOffsetMilliSecondsForCurrentTimeZone() {
        if (offsetMilliSecondsForCurrentTimeZone == null) {
            offsetMilliSecondsForCurrentTimeZone = ZonedDateTime.now().getOffset().getTotalSeconds() * 1000L;
        }
        return offsetMilliSecondsForCurrentTimeZone;
    }

    public static void setNaaccrEthnicityMap(Map<String, String> naaccrEthnicityMap) {
        DDPUtils.naaccrEthnicityMap = naaccrEthnicityMap;
    }

    public static Map<String, String> getNaaccrEthnicityMap() {
        return DDPUtils.naaccrEthnicityMap;
    }

    public static void setNaaccrRaceMap(Map<String, String> naaccrRaceMap) {
        DDPUtils.naaccrRaceMap = naaccrRaceMap;
    }

    public static Map<String, String> getNaaccrRaceMap() {
        return DDPUtils.naaccrRaceMap;
    }

    public static void setNaaccrSexMap(Map<String, String> naaccrSexMap) {
        DDPUtils.naaccrSexMap = naaccrSexMap;
    }

    public static Map<String, String> getNaaccrSexMap() {
        return DDPUtils.naaccrSexMap;
    }

    public static void setPatientFirstSeqDateMap(Map<String, Date> patientFirstSeqDateMap) {
        DDPUtils.patientFirstSeqDateMap = patientFirstSeqDateMap;
    }

    public static Map<String, Date> getPatientFirstSeqDateMap() {
        return DDPUtils.patientFirstSeqDateMap;
    }

    public static Set<String> getPatientsMissingSurvival() {
        return DDPUtils.patientsMissingSurvival;
    }

    public static void setUseSeqDateOsMonthsMethod(Boolean useSeqDateOsMonthsMethod) {
        DDPUtils.useSeqDateOsMonthsMethod = useSeqDateOsMonthsMethod;
    }

    public static Boolean getUseSeqDateOsMonthsMethod() {
        return DDPUtils.useSeqDateOsMonthsMethod;
    }

    public static Set<String> getPatientsWithNegativeOsMonths() {
        return DDPUtils.patientsWithNegativeOsMonths;
    }

    public static Boolean isMskimpactCohort(String cohortName) {
        return (!Strings.isNullOrEmpty(cohortName) && DDPUtils.MSKIMPACT_STUDY_ID.equalsIgnoreCase(cohortName));
    }

    /**
     * Resolve and anonymize patient current age.
     *
     * If birth year is null/empty then return anonymized patient age.
     * Otherwise calculate from difference between reference date and birth date,
     * where reference date is either date of death if pt is deceased or current date.
     *
     * @param compositeRecord
     * @return
     * @throws ParseException
     */
    public static String resolvePatientCurrentAge(DDPCompositeRecord compositeRecord) throws ParseException {
        // if patient birth date is null/empty then use current age value
        if (Strings.isNullOrEmpty(compositeRecord.getPatientBirthDate())) {
            return anonymizePatientAge(compositeRecord.getPatientAge());
        }
        Long birthDateInDays = getDateInDays(compositeRecord.getPatientBirthDate());
        Long referenceDateInDays;
        // use current date as reference date if patient not deceased, otherwise use date of death
        if (!Strings.isNullOrEmpty(compositeRecord.getPatientDeathDate())) {
            referenceDateInDays = getDateInDays(compositeRecord.getPatientDeathDate());
        }
        else {
            referenceDateInDays = getDateInDays(new Date());
        }
        Double age = (referenceDateInDays - birthDateInDays) / (DAYS_TO_YEARS_CONVERSION);
        return anonymizePatientAge(age.intValue());
    }

    /**
     * Resolve and anonymize patient age at diagnosis.
     *
     * If birth year is null/empty then return anonymized patient age.
     * Otherwise calculate from difference between current date and birth date.
     *
     * @param compositeRecord
     * @return
     * @throws ParseException
     */
    public static String resolvePatientAgeAtDiagnosis(DDPCompositeRecord compositeRecord) throws ParseException {
        // if patient birth date is null/empty then use current age value
        if (Strings.isNullOrEmpty(compositeRecord.getPatientBirthDate())) {
            return anonymizePatientAge(compositeRecord.getPatientAge());
        }
        Long birthDateInDays = getDateInDays(compositeRecord.getPatientBirthDate());
        Long currentDateInDays = getDateInDays(new Date());
        Double age = (currentDateInDays - birthDateInDays) / (DAYS_TO_YEARS_CONVERSION);
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
     * @param compositeRecord
     * @return
     */
    public static String resolvePatientSex(DDPCompositeRecord compositeRecord) {
        String sex = "NA";
        if (!isNullEmptyValue(compositeRecord.getPatientSex())) {
            if (compositeRecord.getPatientSex().equalsIgnoreCase("M") ||
                    compositeRecord.getPatientSex().equalsIgnoreCase("MALE")) {
                sex = "Male";
            }
            else if (compositeRecord.getPatientSex().equalsIgnoreCase("F") ||
                    compositeRecord.getPatientSex().equalsIgnoreCase("FEMALE")){
                sex = "Female";
            }
        }
        return sex;
    }

    /**
     * Standardize OS_STATUS value.
     * If CohortPatient is null then resolve from PatientDemographics,
     * which should never be null
     *
     * @param compositeRecord
     * @return
     */
    public static String resolveOsStatus(DDPCompositeRecord compositeRecord) {
        String osStatus = "NA";
        if (compositeRecord.getCohortPatient() != null &&
                !Strings.isNullOrEmpty(compositeRecord.getCohortPatient().getPTVITALSTATUS())) {
            if (compositeRecord.getCohortPatient().getPTVITALSTATUS().equalsIgnoreCase("ALIVE")) {
                osStatus = "LIVING";
            }
            else {
                osStatus = "DECEASED";
            }
        }
        else if (compositeRecord.getPatientDemographics() != null) {
            if (!Strings.isNullOrEmpty(compositeRecord.getPatientDemographics().getDeceasedDate()) ||
                !Strings.isNullOrEmpty(compositeRecord.getPatientDemographics().getPTDEATHDTE())) {
                osStatus = "DECEASED";
            }
            else {
                osStatus = "LIVING";
            }
        }
        return osStatus;
    }

    public static String getOsMonthsLogging(String dmpPatientId, String osStatus, String osMonths, Long referenceInDays, Long firstDateInDays) {
        StringBuilder builder = new StringBuilder();
        builder.append("Patient '").append(dmpPatientId).append("' resolves OS_MONTHS to 'NA':  ")
                .append("( OS_STATUS=").append(osStatus)
                .append(", OS_MONTHS=").append(osMonths)
                .append(", REFERENCE_DATE_DAYS=").append(referenceInDays)
                .append(", FIRST_DATE_days=").append(firstDateInDays)
                .append(" )");
        return builder.toString();
    }

    /**
     * Calculate OS_MONTHS.
     *
     * There are some special cases to handle if we are calculating OS_MONTHS from the first date of sequencing:
     *
     * 1. If a patient dies before the sequencing date, return 0
     * 2. If a living patient has a last follow up date occurring before the date of sequencing, return NA
     *
     * If the calculated OS_MONTHS is negative after handling these special cases then log the operands
     * and resulting OS_MONTHS for reference.
     *
     * Note: In some cases, patients may not have any sequence date or tumor diagnoses in the system yet. These are NA.
     *
     * @param osStatus
     * @param compositeRecord
     * @return
     * @throws ParseException
     */
    public static String resolveOsMonths(String osStatus, DDPCompositeRecord compositeRecord) throws ParseException {
        String osMonths = "NA";
        Long referenceInDays = (osStatus.equals("LIVING")) ?
                getDateInDays(compositeRecord.getPatientDemographics().getPLALASTACTVDTE()) :
                getDateInDays(compositeRecord.getPatientDemographics().getDeceasedDate());
        List<PatientDiagnosis> patientDiagnosis = compositeRecord.getPatientDiagnosis();
        Long firstDateInDays = null;
        // if we were given a seq_date.txt file, use first tumor sequencing
        // date in days, otherwise use first tumor diagnosis date
        if (DDPUtils.useSeqDateOsMonthsMethod) {
            firstDateInDays = getFirstTumorSeqDateInDays(compositeRecord.getDmpPatientId());
            // handle special cases when calculating OS_MONTHS from date of sequencing
            // return 0 if patient dies before sequencing date or NA if patient is living
            // and has a follow up date before sequencing date, otherwise proceed as normal
            if (patientDiedBeforeFirstTumorSequencingDate(osStatus, referenceInDays, firstDateInDays)) {
                return String.valueOf(0);
            } else if (lastFollowUpBeforeFirstTumorSequencingDate(osStatus, referenceInDays, firstDateInDays)) {
                // log cases where OS_MONTHS is 'NA' because patient is living
                // and has a follow up date before sequencing date
                String osMonthsLogMessage = getOsMonthsLogging(compositeRecord.getDmpPatientId(), osStatus, osMonths, referenceInDays, firstDateInDays);
                LOG.debug(osMonthsLogMessage);
                return "NA";
            }
        } else if (patientDiagnosis != null && patientDiagnosis.size() > 0) {
            firstDateInDays = getFirstTumorDiagnosisDateInDays(patientDiagnosis);
        }
        if (referenceInDays != null && firstDateInDays != null) {
            double osMonthsValue = (referenceInDays - firstDateInDays) / DAYS_TO_MONTHS_CONVERSION;
            if (osMonthsValue < 0) {
                // log cases where OS_MONTHS is 'NA' because it calculates to negative
                String osMonthsLogMessage = getOsMonthsLogging(compositeRecord.getDmpPatientId(), osStatus, osMonths, referenceInDays, firstDateInDays);
                LOG.warn(osMonthsLogMessage);
                patientsWithNegativeOsMonths.add(compositeRecord.getDmpPatientId());
                return "NA";
            } else {
                osMonths = String.format("%.3f", osMonthsValue);
            }
        }
        if (osMonths.equals("NA")) {
            // log cases where OS_MONTHS is 'NA' because required values are  null
            String osMonthsLogMessage = getOsMonthsLogging(compositeRecord.getDmpPatientId(), osStatus, osMonths, referenceInDays, firstDateInDays);
            LOG.debug(osMonthsLogMessage);
            patientsMissingSurvival.add(compositeRecord.getDmpPatientId());
        }
        return osMonths;
    }

    /**
     * Determines whether patient died before their first date of tumor sequencing date.
     *
     * @param osStatus
     * @param deceasedDate
     * @param firstTumorSequencingDate
     * @return
     */
    public static Boolean patientDiedBeforeFirstTumorSequencingDate(String osStatus, Long deceasedDate, Long firstTumorSequencingDate) {
        return (!osStatus.equals("LIVING") && deceasedDate != null  && firstTumorSequencingDate != null && deceasedDate < firstTumorSequencingDate);
    }

    /**
     * Determines whether living patient had their last follow up before their first tumor sequencing date.
     *
     * @param osStatus
     * @param dateLastActive
     * @param firstTumorSequencingDate
     * @return
     */
    public static Boolean lastFollowUpBeforeFirstTumorSequencingDate(String osStatus, Long dateLastActive, Long firstTumorSequencingDate) {
        return (osStatus.equals("LIVING") && dateLastActive != null && firstTumorSequencingDate != null && dateLastActive < firstTumorSequencingDate);
    }

    /**
     * Return the earliest patient tumor sequence date in days.
     *
     * @param dmpPatientId
     * @return
     */
    private static Long getFirstTumorSeqDateInDays(String dmpPatientId) {
        if (DDPUtils.patientFirstSeqDateMap.containsKey(dmpPatientId)) {
            return getDateInDays(DDPUtils.patientFirstSeqDateMap.get(dmpPatientId));
        }
        return null;
    }

    /**
     * Find and return the earliest patient tumor diagnosis date in days.
     *
     * @param patientDiagnosis
     * @return
     * @throws ParseException
     */
    private static Long getFirstTumorDiagnosisDateInDays(List<PatientDiagnosis> patientDiagnosis) throws ParseException {
        return getDateInDays(getFirstTumorDiagnosisDate(patientDiagnosis));
    }

    /**
     * Find and return the earliest patient tumor diagnosis date as a string.
     *
     * @param patientDiagnosis
     * @return
     * @throws ParseException
     */
    public static String getFirstTumorDiagnosisDate(List<PatientDiagnosis> patientDiagnosis) throws ParseException {
        if (patientDiagnosis == null || patientDiagnosis.isEmpty()) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String firstTumorDiagnosisDateValue = null;
        Date firstTumorDiagnosisDate = null;
        for (PatientDiagnosis diagnosis : patientDiagnosis) {
            if (Strings.isNullOrEmpty(diagnosis.getTumorDiagnosisDate())) {
                continue;
            }
            if (firstTumorDiagnosisDate == null) {
                firstTumorDiagnosisDate = sdf.parse(diagnosis.getTumorDiagnosisDate());
                firstTumorDiagnosisDateValue = diagnosis.getTumorDiagnosisDate();
            }
            else {
                Date currentTumorDiagnosisDate = sdf.parse(diagnosis.getTumorDiagnosisDate());
                if (currentTumorDiagnosisDate.before(firstTumorDiagnosisDate)) {
                    firstTumorDiagnosisDate = currentTumorDiagnosisDate;
                    firstTumorDiagnosisDateValue = diagnosis.getTumorDiagnosisDate();
                }
            }
        }
        return firstTumorDiagnosisDateValue;
    }

    /**
     * Resolves the date offset in days between the timeline event and reference date (date of birth or first tumor diagnosis date).
     * @param referenceDate
     * @param eventDate
     * @return
     * @throws ParseException
     */
    public static String resolveTimelineEventDateInDays(String referenceDate, String eventDate) throws ParseException {
        Long birthDateInDays = getDateInDays(referenceDate);
        Long eventDateInDays = getDateInDays(eventDate);
        Long timelineEventInDays = (eventDateInDays - birthDateInDays);
        return timelineEventInDays.toString();
    }

    /**
     * Calculates the date in days using local midnight to increment day count.
     *
     * @param date
     * @return
     */
    private static Long computeDaysFromDateUsingLocalMidnight(Date date) {
        Long millisecondsFromUtcEpoc = date.getTime();
        Long millisecondsIfLocalZoneDefinedEpoc = millisecondsFromUtcEpoc + getOffsetMilliSecondsForCurrentTimeZone();
        return millisecondsIfLocalZoneDefinedEpoc / millisecondsPerDay;
    }

    /**
     * Calculates the date in days.
     *
     * @param date
     * @return
     */
    private static Long getDateInDays(Date date) {
        if (date != null) {
            return computeDaysFromDateUsingLocalMidnight(date);
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
        if (!Strings.isNullOrEmpty(dateValue) && !NULL_EMPTY_VALUES.contains(dateValue)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(dateValue);
            return computeDaysFromDateUsingLocalMidnight(date);
        }
        return null;
    }

    /**
     * Returns the date in days as a string.
     *
     * If date value is null/NA or calculated date in days
     * is null then 'NA' is returned.
     * @param dateValue
     * @return
     * @throws ParseException
     */
    public static String getDateInDaysAsString(String dateValue) throws ParseException {
        Long dateInDays = getDateInDays(dateValue);
        if (dateInDays != null) {
            return dateInDays.toString();
        }
        return "NA";
    }

    /**
     * Parses the year from the given date.
     *
     * @param date
     * @return
     * @throws ParseException
     */
    public static Long parseYearFromDate(Date date) throws ParseException {
        if (date != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return new Long(calendar.get(Calendar.YEAR));
        }
        return null;
    }

    /**
     * Parses the year from the given date.
     *
     * @param dateValue
     * @return
     * @throws ParseException
     */
    public static Long parseYearFromDate(String dateValue) throws ParseException {
        if (!Strings.isNullOrEmpty(dateValue) && !NULL_EMPTY_VALUES.contains(dateValue)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(dateValue));
            return new Long(calendar.get(Calendar.YEAR));
        }
        return null;
    }

    /**
     * Parses the year from the given date.
     *
     * Date format expected: yyyy-MM-dd.
     *
     * @param dateValue
     * @return
     * @throws ParseException
     */
    public static String parseYearFromDateAsString(String dateValue) throws ParseException {
        Long year = parseYearFromDate(dateValue);
        if (year != null) {
            return year.toString();
        }
        return "NA";
    }

    /**
     * Calculates the number of years since birth.
     *
     * @param birthDate
     * @return
     * @throws ParseException
     */
    public static String resolveYearsSinceBirth(String birthDate) throws ParseException {
        if (!Strings.isNullOrEmpty(birthDate) && !NULL_EMPTY_VALUES.contains(birthDate)) {
            Long currentYear = parseYearFromDate(new Date());
            Long birthYear = parseYearFromDate(birthDate);
            if (currentYear != null && birthYear != null && birthYear > -1) {
                return String.valueOf(currentYear - birthYear);
            }
        }
        return "NA";
    }

    /**
     * Returns the NAACCR Ethnicity code for the given ethnicity or 'NA' if not
     * NAACCR Ethnicity code mappings.
     *
     * @param ethnicity
     * @return
     */
    public static String resolveNaaccrEthnicityCode(String ethnicity) {
        return (Strings.isNullOrEmpty(ethnicity) ? "NA" :
                naaccrEthnicityMap.getOrDefault(ethnicity.toUpperCase(), "NA"));
    }

    /**
     * Returns the NAACCR Race code for the given ethnicity or 'NA' if not
     * NAACCR Race code mappings.
     *
     * @param race
     * @return
     */
    public static String resolveNaaccrRaceCode(String race) {
        return (Strings.isNullOrEmpty(race) ? "NA" :
                naaccrRaceMap.getOrDefault(race.toUpperCase(), "NA"));
    }

    /**
     * Returns the NAACCR Sex code for the given ethnicity or 'NA' if not
     * NAACCR Sex code mappings.
     *
     * @param sex
     * @return
     */
    public static String resolveNaaccrSexCode(String sex) {
        return (Strings.isNullOrEmpty(sex) ? "NA" :
                naaccrSexMap.getOrDefault(sex.toUpperCase(), "NA"));
    }

    public static String resolvePediatricCohortPatientStatus(Boolean pediatricCohortPatientStatus) {
        if (pediatricCohortPatientStatus == null) {
            return "NA";
        }
        return (pediatricCohortPatientStatus ? "Yes" : "No");
    }

    /**
     * Converts record as tab-delimited string of values.
     *
     * @param object
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static String constructRecord(Object object) throws Exception {
        List<String> fields = (List<String>) object.getClass().getMethod("getFieldNames").invoke(object); // unchecked cast
        List<String> record = new ArrayList<>();
        for (String field : fields) {
            String value = object.getClass().getMethod("get" + field).invoke(object).toString();
            record.add(value.trim());
        }
        return StringUtils.join(record, "\t");
    }

    /**
     * Converts record as tab-delimited string of values.
     *
     * @param object
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static String constructRecord(Object object, Boolean firstInclude, Boolean secondInclude, Boolean thirdInclude, Boolean fourthInclude) throws Exception {
        // we probably should just call this method constructClinicalRecord, not sure keeping it general makes sense
        List<String> fields = (List<String>) object
                                    .getClass()
                                    .getMethod("getFieldNames", Boolean.class, Boolean.class, Boolean.class, Boolean.class)
                                    .invoke(object, firstInclude, secondInclude, thirdInclude, fourthInclude); // unchecked cast
        List<String> record = new ArrayList<>();
        for (String field : fields) {
            String value = object.getClass().getMethod("get" + field).invoke(object).toString();
            record.add(value.trim());
        }
        return StringUtils.join(record, "\t");
    }

    /**
     * Returns whether value is null/empty string.
     * @param value
     * @return
     */
    public static Boolean isNullEmptyValue(String value) {
        return (Strings.isNullOrEmpty(value) || NULL_EMPTY_VALUES.contains(value.trim()));
    }

    /**
     * Reads a file into a list, sorts, and overwrites original file with sorted records
     */
    public static void sortAndWrite(String filename, String header) throws IOException {
        List<String> recordsInFile = readRecordsFromFile(filename);
        Collections.sort(recordsInFile);
        writeRecordsToFile(filename, header, recordsInFile);
    }

    /**
     * Returns a list where each index contains a row from a given file
     * first line is skipped under the assumption it is a header
     * @param filename
     */
    public static List<String> readRecordsFromFile(String filename) throws IOException {
        List<String> recordsInFile = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        // skip first line (header)
        reader.readLine();
        String line;
        while ((line = reader.readLine()) != null) {
            recordsInFile.add(line);
        }
        reader.close();
        return recordsInFile;
    }

    public static void writeRecordsToFile(String filename, String header, List<String> records) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write(header);
        writer.newLine();
        for (String record : records) {
            writer.write(record);
            writer.newLine();
        }
        writer.close();
    }
}

/*
 * Copyright (c) 2023 Memorial Sloan Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.common.util;

import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.apache.log4j.Logger;

/* parses and converts strings to required format for java.time.Instant.parse(), which requires java.time.format.DateTimeFormatter.ISO_INSTANT compatible format.
 * Several formats are attempted for parsing, based on easily output values from the unix 'date' program (run with LC_TIME="en_US.UTF-8"):
 *  date -Iseconds --date='Sep 1 2023 12:00' (this is iso-8601)
 *      2023-09-01T12:00:00-0400
 *  date -Iminutes --date='Sep 1 2023 12:00'
 *      2023-09-01T12:00-0400
 *  date -Ins --date='Sep 1 2023 12:00:00,000000004'
 *      2023-09-01T12:00:00,000000000-0400
 *      (the 3 formats above are parsible after standardization via DateTimeFormatter.ISO_DATE_TIME)
 *  date -R --date='Sep 1 2023 12:00' (this is rfc 2822)
 *      Fri, 01 Sep 2023 12:00:00 -0400
 *      (this is parsible via DateTimeFormatter.RFC_1123_DATE_TIME)
 *  date --rfc-3339=seconds --date='Sep 1 2023 12:00' (this is rfc 3339)
 *      2023-09-01 12:00:00-04:00
 *  date --rfc-3339=ns --date='Sep 1 2023 12:00'
 *      2023-09-01 12:00:00.000000000-04:00
 *      (the two formats above are parsible via DateTimeFormatter.ISO_DATE_TIME)
 *  date --date='Sep 1 2023 12:00'
 *      Fri Sep  1 12:00:00 EDT 2023
 *      Fri Sep  1 16:00:00 UTC 2023
 *      (this is a default locale "human readible" output for the date command.
 *          a translation of this format (assuming the ordering shown here, and then reformatting for input into DateTimeFormatter.RFC_1123_DATE_TIME is provided)
 * Unsupported formats (date only, no time)
 *  date -Idate --date='Sep 1 2023 12:00'
 *      2023-09-01
 *  date --rfc-3339=date --date='Sep 1 2023 12:00'
 *      2023-09-01
 *      (the two formats above are parsible via DateTimeFormatter.ISO_DATE, but contain insufficient information to determine an Instant)
 *  date -Ihours --date='Sep 1 2023 12:00'
 *      2023-09-01T12-0400
 *      (this format contains the hour but not minutes or seconds, so insufficient information to determine an Instant)
 */

public class InstantStringUtil {

    private static Logger log = Logger.getLogger(HttpClientWithTimeoutAndRetry.class);

    private static Map<String, String> tzlabel2offset;
    static {
        // values taken from easily parsible linux TZInfo files
        // WARNING : label "CST" is ambiguous and can refer to both Central Time [America] and China Standard Time [Asia]. e.g. see zoneinfo/America/Chicago and zoneinfo/Asia/Shanghai
        // for unambiguous resolution in these zones, use a datetime string in ino/rfc format with an offset component.
        tzlabel2offset = new HashMap<String, String>();
        tzlabel2offset.put("UTC", "+0000");
        tzlabel2offset.put("Z", "+0000");
        tzlabel2offset.put("GMT", "+0000");
        tzlabel2offset.put("WET", "+0000");
        tzlabel2offset.put("WAT", "+0100");
        tzlabel2offset.put("WEST", "+0100");
        tzlabel2offset.put("CET", "+0100");
        tzlabel2offset.put("MET", "+0100");
        tzlabel2offset.put("CAT", "+0200");
        tzlabel2offset.put("CEST", "+0200");
        tzlabel2offset.put("EET", "+0200");
        tzlabel2offset.put("MEST", "+0200");
        tzlabel2offset.put("SAST", "+0200");
        tzlabel2offset.put("IST", "+0200");
        tzlabel2offset.put("IDT", "+0300");
        tzlabel2offset.put("EAT", "+0300");
        tzlabel2offset.put("EEST", "+0300");
        tzlabel2offset.put("MSK", "+0300");
        tzlabel2offset.put("PKT", "+0500");
        tzlabel2offset.put("IST", "+0530");
        tzlabel2offset.put("WIB", "+0700");
        tzlabel2offset.put("AWST", "+0800");
        tzlabel2offset.put("CST", "+0800"); // Note conflict below
        tzlabel2offset.put("HKT", "+0800");
        tzlabel2offset.put("PST", "+0800");
        tzlabel2offset.put("WITA", "+0800");
        tzlabel2offset.put("JST", "+0900");
        tzlabel2offset.put("KST", "+0900");
        tzlabel2offset.put("WIT", "+0900");
        tzlabel2offset.put("ACST", "+0930");
        tzlabel2offset.put("ChST", "+1000");
        tzlabel2offset.put("AEST", "+1000");
        tzlabel2offset.put("ACDT", "+1030");
        tzlabel2offset.put("AEDT", "+1100");
        tzlabel2offset.put("NZST", "+1200");
        tzlabel2offset.put("NZDT", "+1300");
        tzlabel2offset.put("NDT", "-0230");
        tzlabel2offset.put("ADT", "-0300");
        tzlabel2offset.put("NST", "-0330");
        tzlabel2offset.put("AST", "-0400");
        tzlabel2offset.put("CDT", "-0400");
        tzlabel2offset.put("EDT", "-0400");
        tzlabel2offset.put("CDT", "-0500");
        tzlabel2offset.put("CST", "-0600"); // Note conflict above
        tzlabel2offset.put("MDT", "-0600");
        tzlabel2offset.put("MST", "-0700");
        tzlabel2offset.put("PDT", "-0700");
        tzlabel2offset.put("EST", "-0500");
        tzlabel2offset.put("AKDT", "-0800");
        tzlabel2offset.put("PST", "-0800");
        tzlabel2offset.put("AKST", "-0900");
        tzlabel2offset.put("HDT", "-0900");
        tzlabel2offset.put("HST", "-1000");
        tzlabel2offset.put("SST", "-1100");
    }

    public InstantStringUtil() {
    }

    private static String standardize(String dateTimeString) {
        String returnValue = dateTimeString.trim();
        // convert nanoseconds field to use period delimiter rather than comma
        if (returnValue.matches(".*,(\\d\\d\\d\\d\\d\\d\\d\\d\\d).*")) {
            returnValue = returnValue.replaceFirst(".(\\d\\d\\d\\d\\d\\d\\d\\d\\d)", ".$1");
        }
        return returnValue;
    }

    private static String standardizeIsoDateTime(String dateTimeString) {
        String returnValue = standardize(dateTimeString);
        // convert tz offset string in the form '-0500' to '-05:00'
        if (returnValue.length() >= 5) {
            String zoneOffsetCandidate = returnValue.substring(returnValue.length() - 5);
            if (zoneOffsetCandidate.matches("[+-]\\d\\d\\d\\d")) {
                // insert the required colon
                returnValue = returnValue.substring(0, returnValue.length() - 2) + ":" + returnValue.substring(returnValue.length() - 2);
            }
        }
        return returnValue;
    }

    private static String standardizeRfc2822(String dateTimeString) {
        String returnValue = standardize(dateTimeString);
        return returnValue;
    }

    private static String standardizeRfc3339(String dateTimeString) {
        String returnValue = standardizeIsoDateTime(dateTimeString);
        // replace space between date and time with 'T' delimiter
        returnValue = returnValue.replace(' ', 'T');
        return returnValue;
    }

    /* Conversion of the default date format (human readable, based on locale)
     * from:
     *      Fri Sep  1 12:00:00 EDT 2023
     *      Fri Sep  1 16:00:00 UTC 2023
     * to:
     *      Fri, 01 Sep 2023 12:00:00 -0400
     *      Fri, 01 Sep 2023 16:00:00 +0000
     */
    private static String standardizeDateDefault(String dateTimeString) {
        String returnValue = dateTimeString.trim();
        String[] words = returnValue.split("\\s+");
        if (words.length != 6) {
            return "";
        }
        if (! words[0].matches("Mon|Tue|Wed|Thu|Fri|Sat|Sun")) {
            return "";
        }
        if (! words[1].matches("Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec")) {
            return "";
        }
        if (! words[2].matches("\\d+")) {
            return "";
        }
        if (! words[3].matches("\\d+:\\d+:\\d+")) {
            return "";
        }
        if (words[4].length() == 0) {
            return "";
        }
        String zoneOffsetString = tzlabel2offset.get(words[4]);
        if (zoneOffsetString == null) {
            return "";
        }
        if (! words[5].matches("\\d\\d\\d\\d")) {
            return "";
        }
        int dayOfMonth = 0;
        try {
            dayOfMonth = Integer.parseInt(words[2]);
        } catch (NumberFormatException e) {
            return "";
        }
        returnValue = String.format("%s, %02d %s %s %s %s", words[0], dayOfMonth, words[1], words[5], words[3], zoneOffsetString);
        return returnValue;
    }

    public static String convertToIsoInstantFormat(String dateTimeString) {
        if (dateTimeString == null) {
            return null;
        }
        // Try to parse iso-8601 formats
        try {
            String standardizedDateTimeString = standardizeIsoDateTime(dateTimeString);
            String isoDateTimeString = DateTimeFormatter.ISO_INSTANT.format(DateTimeFormatter.ISO_DATE_TIME.parse(standardizedDateTimeString));
            return isoDateTimeString;
        } catch (DateTimeParseException e) {
        }
        // Try to parse rfc-2822 formats
        try {
            String standardizedDateTimeString = standardizeRfc2822(dateTimeString);
            String isoDateTimeString = DateTimeFormatter.ISO_INSTANT.format(DateTimeFormatter.RFC_1123_DATE_TIME.parse(standardizedDateTimeString));
            return isoDateTimeString ;
        } catch (DateTimeParseException e) {
        }
        // Try to parse rfc-3339 formats
        try {
            String standardizedDateTimeString = standardizeRfc3339(dateTimeString);
            String isoDateTimeString = DateTimeFormatter.ISO_INSTANT.format(DateTimeFormatter.ISO_DATE_TIME.parse(standardizedDateTimeString));
            return isoDateTimeString ;
        } catch (DateTimeParseException e) {
        }
        // Try to parse default date format (human readable)
        try {
            String standardizedDateTimeString = standardizeDateDefault(dateTimeString);
            if (standardizedDateTimeString != null) {
                String isoDateTimeString = DateTimeFormatter.ISO_INSTANT.format(DateTimeFormatter.RFC_1123_DATE_TIME.parse(standardizedDateTimeString));
                return isoDateTimeString ;
            }
        } catch (DateTimeParseException e) {
        }
        return null;
    }

    public static Instant createInstant(String isoInstantFormatString) {
        if (isoInstantFormatString == null || isoInstantFormatString.length() == 0) {
            Instant defaultValue = Instant.EPOCH.plusMillis(Long.MAX_VALUE);
            log.info(String.format("Using default dropDeadInstantString: %s", DateTimeFormatter.ISO_INSTANT.format(defaultValue)));
            return defaultValue;
        }
        return Instant.parse(isoInstantFormatString);
    }
}

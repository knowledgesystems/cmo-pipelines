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

package org.mskcc.cmo.ks.redcap.util;

import java.util.*;
import java.util.regex.*;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class ValueNormalizer {

    private final Logger log = Logger.getLogger(ValueNormalizer.class);
    private static final String QUOTE = "\"";
    private static final String DOUBLEQUOTE = "\"\"";
    private static final String COMMA = ",";
    private static final String REGEX_CONTROL_BLOCK = "[\\u0000-\\u001f\\u007f]+";
    private static final String REGEX_WHITESPACE_MINUS_SPACE_BLOCK = "[\\n\\r\\f\\u000B\\u0085\\u2028\\u2029\\t\\u00A0\\u1680\\u180e\\u2000-\\u200a\\u202f\\u205f\\u3000]+";
    private static final Pattern PATTERN_SPACE_OR_CONTROL = Pattern.compile("[ \\u0000-\\u001f\\u007f\\u0085\\u2028\\u2029\\u00A0\\u1680\\u180e\\u2000-\\u200a\\u202f\\u205f\\u3000]");

    /** Removes troublesome whitespace from strings intended for use as redcap values.
     * The supplied value is normalized by removing flanking whitespace
     * and then replacing all blocks of one or more {newline,carriage return,tab,vertical tab,form feed,...} with a single space if no flanking spaces are present.
     * All remaining illegal (control) characters are stripped.
     * If the supplied value is null, the empty string is returned.
     * @param value The value to be normalized
     * @return The normalized version of the supplied value.
     */
    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        if (!PATTERN_SPACE_OR_CONTROL.matcher(value).find()) {
            return value;
        }
        String[] valueTrimmedAndBlocked = value.trim().split(REGEX_WHITESPACE_MINUS_SPACE_BLOCK);
        // purge any other control characters
        for (int index = 0; index < valueTrimmedAndBlocked.length; index++) {
            valueTrimmedAndBlocked[index] = valueTrimmedAndBlocked[index].replaceAll(REGEX_CONTROL_BLOCK, "");
        }
        // join printable elements
        StringBuilder builder = new StringBuilder();
        boolean considerPrependingASpace = false; // don't prepend a space while builder is empty
        for (int index = 0; index < valueTrimmedAndBlocked.length; index++) {
            String nextPrintableBlock = valueTrimmedAndBlocked[index];
            if (nextPrintableBlock.length() == 0) {
                continue; // skip empty elements
            }
            if (considerPrependingASpace && ' ' != nextPrintableBlock.charAt(0)) {
                // only prepend a space when the next element does not start with a space
                builder.append(' ');
            }
            builder.append(nextPrintableBlock);
            char lastCharacterAdded = nextPrintableBlock.charAt(nextPrintableBlock.length() - 1);
            considerPrependingASpace = (lastCharacterAdded != ' ');
        }
        return builder.toString();
    }

    /** Detects embedded quotation marks and commas, escaping as necessary for use in a CSV format field.
     * If the normalized field value contains quotation marks or commas, the field is escaped appropriately and returned as a quote delimited string.
     * @param value The value to be normalized
     * @return The normalized version of the supplied value.
     */
    public String formatForCSV(String value) {
        if (value == null) {
            return "";
        }
        boolean valueContainsQuote = value.indexOf(QUOTE) != -1;
        boolean valueContainsComma = value.indexOf(COMMA) != -1;
        if (!valueContainsQuote && !valueContainsComma) {
            return value; // escaping or quoting not necessary
        }
        return QUOTE + String.join(DOUBLEQUOTE, value.split(QUOTE, -1)) + QUOTE;
    }

    /** Converts a string matrix from tab delimited format to comma separated format
     * If the normalized field value contains quotation marks or commas, the field is escaped appropriately and returned as a quote delimited string.
     * If tsvLines is empty or null, returns an empty string array.
     * @param tsvLines an array of tab delimited records
     * @param dropDuplicatedKeys determines whether to drop records that repeat the content in the first field
     * @return e
     */
    public List<String> convertTSVtoCSV(List<String> tsvLines, boolean dropDuplicatedKeys) {
        LinkedList<String> csvLines = new LinkedList<String>();
        if (tsvLines == null) {
            return csvLines;
        }
        HashSet<String> seen = new HashSet<String>();
        for (String tsvLine : tsvLines) {
            String[] tsvFields = tsvLine.split("\t", -1);
            String firstValue = tsvFields[0].trim();
            if (!dropDuplicatedKeys || !seen.contains(firstValue)) {
                seen.add(firstValue);
                String[] csvFields = new String[tsvFields.length];
                for (int i = 0; i < tsvFields.length; i++) {
                        csvFields[i] = formatForCSV(tsvFields[i]);
                }
                csvLines.add(String.join(",", csvFields));
            }
        }
        return csvLines;
    }

}

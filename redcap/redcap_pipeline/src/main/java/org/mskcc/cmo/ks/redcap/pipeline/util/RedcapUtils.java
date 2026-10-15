/*
 * Copyright (c) 2017 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.redcap.pipeline.util;

import java.util.*;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.source.*;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedcapUtils {

    private final Logger log = Logger.getLogger(RedcapUtils.class);

    private Set<String> notApplicableValueSet = initializeNotApplicableValueSet();

    /** This function takes an existing value for an attribute and a newly read value for the attribute and chooses the more informative attribute.
     *  Null arguments are lowest in precedence. Whitespace only strings are next lowest. Values which are equivalent to "Not Applicable" are next lowest. Highest precedence for any other string.
     *  If both arguments have equal precedence, the original value is chosen ... however, an exception is thrown when at the highest level of precedence the two values are not equal.
     *  If both arguemnts are whitespace only strings, the return value is the empty string.
     *  @param existingValue The previously known value for an attribute
     *  @param newValue The newly read value for the same attribute
     *  @return If the original attribute is chosen, the function returns null ... indicating that no changes are needed. Otherwise, a string containing the chosen value is returned.
     */
    public String getReplacementValueForAttribute(String existingValue, String newValue) throws ConflictingAttributeValuesException {
        if (newValue == null) {
            newValue = "";
        }
        //existing value is null
        if (existingValue == null) {
            return newValue.trim(); //prefer anything over nothing
        }
        //existing value is emptystring
        if (existingValue.isEmpty()) {
            if (newValue.trim().isEmpty()) {
                return null; //don't overwrite empty with empty / whitespace
            } else {
                return newValue; //prefer any non-whitepace to empty
            }
        }
        //existing value is whitespce only
        if (existingValue.trim().isEmpty()) {
            if (newValue.trim().isEmpty()) {
                return ""; //prefer empty over whitespace
            } else {
                return newValue; //prefer any non-empty to whitespace
            }
        }
        //existing value is NA
        if (valueIsEmptyOrNotApplicable(existingValue)) {
            if (!valueIsEmptyOrNotApplicable(newValue)) {
                return newValue; //prefer any substantial (not "NA" or similar) value to an insubstantial value
            }
            return null;
        }
        // existing value is actual [not NA, not whitespace]
        if (!valueIsEmptyOrNotApplicable(newValue) && !existingValue.equals(newValue)) {
            throw new ConflictingAttributeValuesException(); //signal an exception when a substantial new value differs from a substantial existing value
        }
        return null;
    }

    private Set<String> initializeNotApplicableValueSet() {
        String[] basisList = new String[] {
                "NOTAPPLICABLE",
                "NOTAVAILABLE",
                "PENDING",
                "DISCREPANCY",
                "COMPLETED",
                "NULL",
                "NA"};
        Set<String> constructedSet = new HashSet<String>(Arrays.asList(basisList));
        return constructedSet;
    }

    private boolean valueIsEmptyOrNotApplicable(String value) {
        if (value == null) {
            return true;
        }
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) return true;
        try {
            trimmedValue = trimmedValue.replaceAll("[\\[|\\]| |\\/]", "");
            return notApplicableValueSet.contains(trimmedValue.toUpperCase());
        } catch (IllegalArgumentException x) {
            return false;
        }
    }
}

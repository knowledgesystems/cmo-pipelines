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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package org.mskcc.cmo.ks.redcap.util;

import java.io.*;
import java.lang.StringBuilder;
import java.util.*;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mskcc.cmo.ks.redcap.util.ValueNormalizerTestConfiguration;
import org.springframework.beans.factory.annotation.*;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(classes=ValueNormalizerTestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class ValueNormalizerTest {

    @Autowired
    private ValueNormalizer valueNormalizer;

    @Test
    public void testNormalize() {
        StringBuilder errorMessage = new StringBuilder();
        runNormalizeTest(null, "", errorMessage);
        Map<String, String> normalizeTestMap = makeNormalizeTestMap(); // maps values to expected return values
        for (String query : normalizeTestMap.keySet()) {
            String expectedValue = normalizeTestMap.get(query);
            runNormalizeTest(query, expectedValue, errorMessage);
        }
        if (errorMessage.length() > 0) {
            Assert.fail("\nFailures:\n" + errorMessage.toString());
        }
    }

    private void runNormalizeTest(String query, String expectedValue, StringBuilder errorMessage) {
        String returnedValue = valueNormalizer.normalize(query);
        if (returnedValue == null || !returnedValue.equals(expectedValue)) {
            errorMessage.append("ValueNormalizer.normalize() returned '" + returnedValue +
                    "' for value '" + query + "' but the expected value was '" + expectedValue + "'\n");
        }
    }

    @Test
    public void testFormatForCSV() {
        StringBuilder errorMessage = new StringBuilder();
        runFormatForCSVTest(null, "", errorMessage);
        Map<String, String> formatForCSVTestMap = makeFormatForCSVTestMap(); // maps values to expected return values
        for (String query : formatForCSVTestMap.keySet()) {
            String expectedValue = formatForCSVTestMap.get(query);
            runFormatForCSVTest(query, expectedValue, errorMessage);
        }
        if (errorMessage.length() > 0) {
            Assert.fail("\nFailures:\n" + errorMessage.toString());
        }
    }

    private void runFormatForCSVTest(String query, String expectedValue, StringBuilder errorMessage) {
        String returnedValue = valueNormalizer.formatForCSV(query);
        if (returnedValue == null || !returnedValue.equals(expectedValue)) {
            errorMessage.append("ValueNormalizer.formatForCSV() returned '" + returnedValue +
                    "' for value '" + query + "' but the expected value was '" + expectedValue + "'\n");
        }
    }

    @Test
    public void testConvertTSVtoCSV() {
        StringBuilder errorMessage = new StringBuilder();
        List<String> query = Arrays.asList(
            "SAMPLE_ID\tPATIENT_ID\tCOMMENT",
            "P-0000001-T01-IM3\tP-0000001\t",
            "P-0000002-T01-IM3\tP-0000002\tNA",
            "P-0000002-T01-IM3\tP-0000002\tduplicated sample record",
            "P-0000003-T01-IM3\tP-0000003\ttest with comma, and spaces",
            "P-0000004-T01-IM3\tP-0000004\ttest with \"quotes\" and spaces");
        List<String> expectedValue = Arrays.asList(
            "SAMPLE_ID,PATIENT_ID,COMMENT",
            "P-0000001-T01-IM3,P-0000001,",
            "P-0000002-T01-IM3,P-0000002,NA",
            "P-0000003-T01-IM3,P-0000003,\"test with comma, and spaces\"",
            "P-0000004-T01-IM3,P-0000004,\"test with \"\"quotes\"\" and spaces\"");
        List<String> returnedValue = valueNormalizer.convertTSVtoCSV(query, true);
        if (returnedValue == null) {
            Assert.fail("null value returned");
        }
        if (returnedValue.size() != expectedValue.size()) {
            Assert.fail("returned array had length " + Integer.toString(returnedValue.size()) + ", but expected array of length " + Integer.toString(expectedValue.size())); 
        }
        for (int index = 0; index < returnedValue.size(); index++) {
            String returnedRecord = returnedValue.get(index);
            String expectedRecord = returnedValue.get(index);
            if (!returnedRecord.equals(expectedRecord)) {
                Assert.fail("returned array record had value '" + returnedRecord + "', but expected array had record '" + expectedRecord + "' at position " + Integer.toString(index)); 
            }
        }
    }

    @Test
    public void testConvertTSVtoCSVNoFiltering() {
        StringBuilder errorMessage = new StringBuilder();
        List<String> query = Arrays.asList(
            "SAMPLE_ID\tPATIENT_ID\tCOMMENT",
            "P-0000001-T01-IM3\tP-0000001\t",
            "P-0000002-T01-IM3\tP-0000002\tNA",
            "P-0000002-T01-IM3\tP-0000002\tduplicated sample record",
            "P-0000003-T01-IM3\tP-0000003\ttest with comma, and spaces",
            "P-0000004-T01-IM3\tP-0000004\ttest with \"quotes\" and spaces");
        List<String> expectedValue = Arrays.asList(
            "SAMPLE_ID,PATIENT_ID,COMMENT",
            "P-0000001-T01-IM3,P-0000001,",
            "P-0000002-T01-IM3,P-0000002,NA",
            "P-0000002-T01-IM3,P-0000002,duplicated sample record",
            "P-0000003-T01-IM3,P-0000003,\"test with comma, and spaces\"",
            "P-0000004-T01-IM3,P-0000004,\"test with \"\"quotes\"\" and spaces\"");
        List<String> returnedValue = valueNormalizer.convertTSVtoCSV(query, false);
        if (returnedValue == null) {
            Assert.fail("null value returned");
        }
        if (returnedValue.size() != expectedValue.size()) {
            Assert.fail("returned array had length " + Integer.toString(returnedValue.size()) + ", but expected array of length " + Integer.toString(expectedValue.size())); 
        }
        for (int index = 0; index < returnedValue.size(); index++) {
            String returnedRecord = returnedValue.get(index);
            String expectedRecord = returnedValue.get(index);
            if (!returnedRecord.equals(expectedRecord)) {
                Assert.fail("returned array record had value '" + returnedRecord + "', but expected array had record '" + expectedRecord + "' at position " + Integer.toString(index)); 
            }
        }
    }

    private Map<String, String> makeNormalizeTestMap() {
        Map<String, String> normalizeTestMap = new LinkedHashMap<>();
        normalizeTestMap.put("", "");
        normalizeTestMap.put("X", "X");
        normalizeTestMap.put(" X", "X");
        normalizeTestMap.put("X ", "X");
        normalizeTestMap.put(" X ", "X");
        normalizeTestMap.put("  X  ", "X");
        normalizeTestMap.put("\tX\t", "X");
        normalizeTestMap.put("\nX\n", "X");
        normalizeTestMap.put("\rX\r", "X");
        normalizeTestMap.put("\fX\f", "X");
        normalizeTestMap.put("\u00A0X\u00A0", "X");
        normalizeTestMap.put("\u1680X\u1680", "X");
        normalizeTestMap.put("\u180eX\u180e", "X");
        normalizeTestMap.put("\u2000X\u2001", "X");
        normalizeTestMap.put("\u2002X\u2003", "X");
        normalizeTestMap.put("\u2004X\u2005", "X");
        normalizeTestMap.put("\u2006X\u2007", "X");
        normalizeTestMap.put("\u2008X\u2009", "X");
        normalizeTestMap.put("\u200aX\u200a", "X");
        normalizeTestMap.put("\u202fX\u202f", "X");
        normalizeTestMap.put("\u205fX\u205f", "X");
        normalizeTestMap.put("\u3000X\u3000", "X");
        normalizeTestMap.put("\u000BX\u000B", "X");
        normalizeTestMap.put("\u0085X\u0085", "X");
        normalizeTestMap.put("\u2028X\u2028", "X");
        normalizeTestMap.put("\u2029X\u2029", "X");
        normalizeTestMap.put("\u0001X\u0001", "X");
        normalizeTestMap.put("\u007fX\u007f", "X");
        normalizeTestMap.put("\u001fX\u001f", "X");
        normalizeTestMap.put("\t\t\tX\t\t\t", "X");
        normalizeTestMap.put(" \t\t\tX\t\t\t ", "X");
        normalizeTestMap.put("\t\t\t X \t\t\t", "X");
        normalizeTestMap.put("\t\tX\t\tX\t\t", "X X");
        normalizeTestMap.put("\t\tX \t\tX\t\t", "X X");
        normalizeTestMap.put("\t\tX\t \tX\t\t", "X X");
        normalizeTestMap.put("\t\tX\t\t X\t\t", "X X");
        normalizeTestMap.put("\t\tX\t\t X\t\t", "X X");
        normalizeTestMap.put("\t\tX \t\t X\t\t", "X  X");
        normalizeTestMap.put("\t\tX \t \t X\t\t", "X   X");
        normalizeTestMap.put("\t\tX       X\t\t", "X       X");
        normalizeTestMap.put("\t\tX   \u0000    X\t\t", "X       X");
        return normalizeTestMap;
    }

    private Map<String, String> makeFormatForCSVTestMap() {
        Map<String, String> formatForCSVTestMap = new LinkedHashMap<>();
        formatForCSVTestMap.put("", "");
        formatForCSVTestMap.put("X", "X");
        formatForCSVTestMap.put("X X", "X X");
        formatForCSVTestMap.put(",", "\",\"");
        formatForCSVTestMap.put("\"", "\"\"\"\"");
        formatForCSVTestMap.put("X,X", "\"X,X\"");
        formatForCSVTestMap.put("X,X,X", "\"X,X,X\"");
        formatForCSVTestMap.put("\"X\"", "\"\"\"X\"\"\"");
        formatForCSVTestMap.put("\"X\"", "\"\"\"X\"\"\"");
        formatForCSVTestMap.put("\"\"\"", "\"\"\"\"\"\"\"\"");
        formatForCSVTestMap.put("\"X\" \"X\"", "\"\"\"X\"\" \"\"X\"\"\"");
        return formatForCSVTestMap;
    }

}

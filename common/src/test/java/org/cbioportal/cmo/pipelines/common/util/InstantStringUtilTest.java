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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package org.cbioportal.cmo.pipelines.common.util;

import java.time.Instant;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.cbioportal.cmo.pipelines.common.util.InstantStringUtil;


@ContextConfiguration(classes=Object.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class InstantStringUtilTest {

    @Test
    public void testParseIsoDateInvalid() throws Exception {
        String inputstring = "2023-09-01";
        String instantString = InstantStringUtil.convertToIsoInstantFormat(inputstring);
        Assert.assertNull("Unexpected string returned for invalid date only format", instantString);
    }

    @Test
    public void testParseIsoDateTimeMicroSecondsValid() throws Exception {
        // microseconds are trimmed/ignored by output format
        String inputstring = "2023-09-01T12:00:00,000000004-0400";
        String expectedOutput = "2023-09-01T16:00:00.000000004Z";
        String instantString = InstantStringUtil.convertToIsoInstantFormat(inputstring);
        Assert.assertNotNull("Null return value for valid test string", instantString);
        Assert.assertEquals("Unexpected Value", expectedOutput, instantString);
    }

    @Test
    public void testParseIsoDateTimeSecondsValid() throws Exception {
        String inputstring = "2023-09-01T12:00:00-0400";
        String expectedOutput = "2023-09-01T16:00:00Z";
        String instantString = InstantStringUtil.convertToIsoInstantFormat(inputstring);
        Assert.assertNotNull("Null return value for valid test string", instantString);
        Assert.assertEquals("Unexpected Value", expectedOutput, instantString);
    }

    @Test
    public void testParseIsoDateTimeMinutesValid() throws Exception {
        // omitting seconds is all right - when omitting seonds, the instant is assumed to occur at the start of the referenced minute
        String inputstring = "2023-09-01T12:00-0400";
        String expectedOutput = "2023-09-01T16:00:00Z";
        String instantString = InstantStringUtil.convertToIsoInstantFormat(inputstring);
        Assert.assertNotNull("Null return value for valid test string", instantString);
        Assert.assertEquals("Unexpected Value", expectedOutput, instantString);
    }

    @Test
    public void testParseRfc2822_1Valid() throws Exception {
        String inputstring = "Fri, 01 Sep 2023 12:00:00 -0400";
        String expectedOutput = "2023-09-01T16:00:00Z";
        String instantString = InstantStringUtil.convertToIsoInstantFormat(inputstring);
        Assert.assertNotNull("Null return value for valid test string", instantString);
        Assert.assertEquals("Unexpected Value", expectedOutput, instantString);
    }

    @Test
    public void testParseRfc2822_2Valid() throws Exception {
        String inputstring = "Fri, 01 Sep 2023 12:00:00 +0000";
        String expectedOutput = "2023-09-01T12:00:00Z";
        String instantString = InstantStringUtil.convertToIsoInstantFormat(inputstring);
        Assert.assertNotNull("Null return value for valid test string", instantString);
        Assert.assertEquals("Unexpected Value", expectedOutput, instantString);
    }

    @Test
    public void testParseRfc3339_1Valid() throws Exception {
        String inputstring = "2023-09-01 12:00:00-04:00";
        String expectedOutput = "2023-09-01T16:00:00Z";
        String instantString = InstantStringUtil.convertToIsoInstantFormat(inputstring);
        Assert.assertNotNull("Null return value for valid test string", instantString);
        Assert.assertEquals("Unexpected Value", expectedOutput, instantString);
    }

    @Test
    public void testParseRfc3339_2Valid() throws Exception {
        String inputstring = "2023-09-01 12:00:00.000000004-04:00";
        String expectedOutput = "2023-09-01T16:00:00.000000004Z";
        String instantString = InstantStringUtil.convertToIsoInstantFormat(inputstring);
        Assert.assertNotNull("Null return value for valid test string", instantString);
        Assert.assertEquals("Unexpected Value", expectedOutput, instantString);
    }

    @Test
    public void testParseDateDefault_1Valid() throws Exception {
        String inputstring = "Fri Sep  1 16:00:00 UTC 2023";
        String expectedOutput = "2023-09-01T16:00:00Z";
        String instantString = InstantStringUtil.convertToIsoInstantFormat(inputstring);
        Assert.assertNotNull("Null return value for valid test string", instantString);
        Assert.assertEquals("Unexpected Value", expectedOutput, instantString);
    }

    @Test
    public void testParseDateDefault_2Valid() throws Exception {
        String inputstring = "Fri Sep  1 12:00:00 EDT 2023";
        String expectedOutput = "2023-09-01T16:00:00Z";
        String instantString = InstantStringUtil.convertToIsoInstantFormat(inputstring);
        Assert.assertNotNull("Null return value for valid test string", instantString);
        Assert.assertEquals("Unexpected Value", expectedOutput, instantString);
    }

    @Test
    public void testParseDateDefault_3Valid() throws Exception {
        String inputstring = "Fri Dec  1 12:00:00 EST 2023";
        String expectedOutput = "2023-12-01T17:00:00Z";
        String instantString = InstantStringUtil.convertToIsoInstantFormat(inputstring);
        Assert.assertNotNull("Null return value for valid test string", instantString);
        Assert.assertEquals("Unexpected Value", expectedOutput, instantString);
    }

}

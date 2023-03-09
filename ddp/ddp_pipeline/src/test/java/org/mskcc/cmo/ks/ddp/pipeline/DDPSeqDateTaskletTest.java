/*
 * Copyright (c) 2019 - 2023 Memorial Sloan Kettering Cancer Center.
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

package org.mskcc.cmo.ks.ddp.pipeline;

import java.io.BufferedReader;
import java.text.SimpleDateFormat;
import java.util.*;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mskcc.cmo.ks.ddp.pipeline.util.DDPUtils;
import org.mockito.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
/**
 *
 * @author Manda Wilson
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={DDPSeqDateTaskletTestConfiguration.class})
public class DDPSeqDateTaskletTest {

    @Test
    public void testGetFirstSeqDatePerPatientFromFileInvalidHeader() throws Exception {
        BufferedReader mockBufferedReader = Mockito.mock(BufferedReader.class);
        Mockito.when(mockBufferedReader.readLine()).thenReturn("BLAH").thenReturn("SAMPLE_1\tPATIENT_1\tMon, 01 Oct 2018 15:09:02 GMT").thenReturn(null);
        DDPSeqDateTasklet tasklet = new DDPSeqDateTasklet();
        tasklet.setSeqDateMaps("filename", mockBufferedReader);
        Map<String, Date> actualPatientFirstSeqDateMap = DDPUtils.getPatientFirstSeqDateMap();
        Assert.assertTrue("Expected an empty map because of an invalid header", actualPatientFirstSeqDateMap.size() == 0);
    }

    @Test
    public void testGetFirstSeqDatePerPatientFromFile() throws Exception {
        BufferedReader mockBufferedReader = Mockito.mock(BufferedReader.class);
        Mockito.when(mockBufferedReader.readLine())
            .thenReturn("SAMPLE_ID\tPATIENT_ID\tSEQ_DATE")
            .thenReturn("SAMPLE_1\tPATIENT_1\tMon, 01 Oct 2018 15:09:02 GMT")
            .thenReturn("SAMPLE_2\tPATIENT_2\tMon, 11 Jun 2018 15:20:33 GMT")
            .thenReturn("SAMPLE_3\tPATIENT_3\tFri, 23 Nov 2018 15:01:19 GMT")
            .thenReturn("SAMPLE_4\tPATIENT_1\tSat, 20 Apr 2019 22:01:22 GMT")
            .thenReturn("SAMPLE_5\tPATIENT_2\tMon, 30 Mar 2015 17:21:08 GMT")
            .thenReturn("SAMPLE_6\tPATIENT_3\tMon, 22 Oct 2018 15:33:15 GMT")
            .thenReturn("SAMPLE_7\tPATIENT_1\tFri, 23 Nov 2018 15:01:19 GMT")
            .thenReturn("SAMPLE_8\tPATIENT_2\tMon, 01 Oct 2018 15:09:02 GMT")
            .thenReturn("SAMPLE_9\tPATIENT_4\tTes, 14 Feb 2014 17:21:03 GMT") // invalid date, this patient should not have a seq date in map
            .thenReturn("SAMPLE_9\tPATIENT_3\tFri, 14 Feb 2014 17:21:03 GMT")
            .thenReturn("SAMPLE_10\tPATIENT_5\t") // no date provided, this patient should not have a seq date in map
            .thenReturn(null);
        DDPSeqDateTasklet tasklet = new DDPSeqDateTasklet();
        tasklet.setSeqDateMaps("filename", mockBufferedReader);
        Map<String, Date> actualPatientFirstSeqDateMap = DDPUtils.getPatientFirstSeqDateMap();
        Map<String, Date> expectedPatientFirstSeqDateMap = new HashMap<String, Date>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        expectedPatientFirstSeqDateMap.put("PATIENT_1", simpleDateFormat.parse("Mon, 01 Oct 2018 15:09:02 GMT")); 
        expectedPatientFirstSeqDateMap.put("PATIENT_2", simpleDateFormat.parse("Mon, 30 Mar 2015 17:21:08 GMT"));
        expectedPatientFirstSeqDateMap.put("PATIENT_3", simpleDateFormat.parse("Fri, 14 Feb 2014 17:21:03 GMT"));
        Assert.assertEquals(expectedPatientFirstSeqDateMap, actualPatientFirstSeqDateMap);
    }

    @Test
    public void testGetSeqDatePerSampleFromFile() throws Exception {
        BufferedReader mockBufferedReader = Mockito.mock(BufferedReader.class);
        Mockito.when(mockBufferedReader.readLine())
            .thenReturn("SAMPLE_ID\tPATIENT_ID\tSEQ_DATE")
            .thenReturn("PATIENT_1-SAMPLE_1\tPATIENT_1\tMon, 01 Oct 2018 15:09:02 GMT")
            .thenReturn("PATIENT_2-SAMPLE_2\tPATIENT_2\tMon, 11 Jun 2018 15:20:33 GMT")
            .thenReturn("PATIENT_3-SAMPLE_3\tPATIENT_3\tFri, 23 Nov 2018 15:01:19 GMT")
            .thenReturn("PATIENT_1-SAMPLE_4\tPATIENT_1\tSat, 20 Apr 2019 22:01:22 GMT")
            .thenReturn("PATIENT_2-SAMPLE_5\tPATIENT_2\tMon, 30 Mar 2015 17:21:08 GMT")
            .thenReturn("PATIENT_3-SAMPLE_6\tPATIENT_3\tMon, 22 Oct 2018 15:33:15 GMT")
            .thenReturn("PATIENT_1-SAMPLE_7\tPATIENT_1\tFri, 23 Nov 2018 15:01:19 GMT")
            .thenReturn("PATIENT_2-SAMPLE_8\tPATIENT_2\tMon, 01 Oct 2018 15:09:02 GMT")
            .thenReturn("PATIENT_4-SAMPLE_9\tPATIENT_4\tTes, 14 Feb 2014 17:21:03 GMT") // invalid date, this patient should not have a seq date in map
            .thenReturn("PATIENT_3-SAMPLE_9\tPATIENT_3\tFri, 14 Feb 2014 17:21:03 GMT")
            .thenReturn("PATIENT_5-SAMPLE_10\tPATIENT_5\t") // no date provided, this patient should not have a seq date in map
            .thenReturn(null);
        DDPSeqDateTasklet tasklet = new DDPSeqDateTasklet();
        tasklet.setSeqDateMaps("filename", mockBufferedReader);
        Map<String, Date> actualSampleSeqDateMap = DDPUtils.getSampleSeqDateMap();
        Map<String, Date> expectedSampleSeqDateMap = new HashMap<String, Date>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        expectedSampleSeqDateMap.put("PATIENT_1-SAMPLE_1", simpleDateFormat.parse("Mon, 01 Oct 2018 15:09:02 GMT"));
        expectedSampleSeqDateMap.put("PATIENT_2-SAMPLE_2", simpleDateFormat.parse("Mon, 11 Jun 2018 15:20:33 GMT"));
        expectedSampleSeqDateMap.put("PATIENT_3-SAMPLE_3", simpleDateFormat.parse("Fri, 23 Nov 2018 15:01:19 GMT"));
        expectedSampleSeqDateMap.put("PATIENT_1-SAMPLE_4", simpleDateFormat.parse("Sat, 20 Apr 2019 22:01:22 GMT"));
        expectedSampleSeqDateMap.put("PATIENT_2-SAMPLE_5", simpleDateFormat.parse("Mon, 30 Mar 2015 17:21:08 GMT"));
        expectedSampleSeqDateMap.put("PATIENT_3-SAMPLE_6", simpleDateFormat.parse("Mon, 22 Oct 2018 15:33:15 GMT"));
        expectedSampleSeqDateMap.put("PATIENT_1-SAMPLE_7", simpleDateFormat.parse("Fri, 23 Nov 2018 15:01:19 GMT"));
        expectedSampleSeqDateMap.put("PATIENT_2-SAMPLE_8", simpleDateFormat.parse("Mon, 01 Oct 2018 15:09:02 GMT"));
        expectedSampleSeqDateMap.put("PATIENT_3-SAMPLE_9", simpleDateFormat.parse("Fri, 14 Feb 2014 17:21:03 GMT"));
        Assert.assertEquals(expectedSampleSeqDateMap, actualSampleSeqDateMap);
    }
}

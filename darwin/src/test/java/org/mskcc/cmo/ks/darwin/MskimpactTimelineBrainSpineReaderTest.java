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

package org.mskcc.cmo.ks.darwin;

import java.util.ArrayList;
import java.util.Set;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mskcc.cmo.ks.darwin.pipeline.model.MskimpactBrainSpineTimeline;
import org.mskcc.cmo.ks.darwin.pipeline.mskimpactbrainspinetimeline.MskimpactTimelineBrainSpineReader;
import org.mskcc.cmo.ks.darwin.pipeline.util.DarwinSampleListUtil;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author Manda Wilson
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=DarwinTestConfiguration.class)
public class MskimpactTimelineBrainSpineReaderTest {

    @Autowired
    private MskimpactTimelineBrainSpineReader mskimpactTimelineBrainSpineReader;

    @Autowired
    private DarwinSampleListUtil darwinSampleListUtil;

    @Test
    public void testReadFiltersNAStartDate() {
        mskimpactTimelineBrainSpineReader.openForTestingAndSetDarwinTimelineResults(makeMockMskimpactBrainSpineTimelineResults());
        try {
            // confirm DarwinSampleListUtil.filteredMskimpactBrainSpineTimelineSet is empty before we start reading
            Set<MskimpactBrainSpineTimeline> filteredMskimpactBrainSpineTimelineSet = darwinSampleListUtil.getFilteredMskimpactBrainSpineTimelineSet();
            Assert.assertTrue("filteredMskimpactBrainSpineTimelineSet should be empty but contains " +
                filteredMskimpactBrainSpineTimelineSet.size() + " records", filteredMskimpactBrainSpineTimelineSet.isEmpty());

            // read first unfiltered result
            MskimpactBrainSpineTimeline actualUnfilteredResult = null;
            try {
                actualUnfilteredResult = mskimpactTimelineBrainSpineReader.read();
            } catch (Exception e) {
                Assert.fail(e.toString());
            }

            // set up expected result
            MskimpactBrainSpineTimeline expectedUnfilteredResult = new MskimpactBrainSpineTimeline();
            expectedUnfilteredResult.setDMP_PATIENT_ID_ALL_BRAINSPINETMLN("P-0000003");
            expectedUnfilteredResult.setSTART_DATE("757");

            // compare expected result and actual result
            Assert.assertEquals(expectedUnfilteredResult.getDMP_PATIENT_ID_ALL_BRAINSPINETMLN(), actualUnfilteredResult.getDMP_PATIENT_ID_ALL_BRAINSPINETMLN());
            Assert.assertEquals(expectedUnfilteredResult.getSTART_DATE(), actualUnfilteredResult.getSTART_DATE());

            // there should be no more records (all others were filtered)
            try {
                Assert.assertNull("mskimpactTimelineBrainSpineReader should be done reading but found another record", mskimpactTimelineBrainSpineReader.read());
            } catch (Exception e) {
                Assert.fail(e.toString());
            }

            // confirm that the filtered records were stored in DarwinSampleListUtil.filteredMskimpactBrainSpineTimelineSet
            filteredMskimpactBrainSpineTimelineSet = darwinSampleListUtil.getFilteredMskimpactBrainSpineTimelineSet();
            Assert.assertEquals(2, filteredMskimpactBrainSpineTimelineSet.size());
            boolean foundNAStartDate = false;
            boolean foundNullStartDate = false;
            for (MskimpactBrainSpineTimeline mskimpactBrainSpineTimeline : filteredMskimpactBrainSpineTimelineSet) {
                if (mskimpactBrainSpineTimeline.getDMP_PATIENT_ID_ALL_BRAINSPINETMLN().equals("P-0000001")) {
                    foundNAStartDate = true;
                } else if (mskimpactBrainSpineTimeline.getDMP_PATIENT_ID_ALL_BRAINSPINETMLN().equals("P-0000002")) {
                    foundNullStartDate = true;
                }
                Assert.assertTrue(mskimpactBrainSpineTimeline.getDMP_PATIENT_ID_ALL_BRAINSPINETMLN() +
                    " has unexpected START_DATE '" + mskimpactBrainSpineTimeline.getSTART_DATE() +
                    "', it should be 'NA'", mskimpactBrainSpineTimeline.getSTART_DATE().equals("NA")); // both "NA" and null should be converted to "NA" in model
            }
            Assert.assertTrue("Did not find patient initialized with NA start date in darwinSampleListUtil.getFilteredMskimpactBrainSpineTimelineSet()", foundNAStartDate);
            Assert.assertTrue("Did not find patient initialized with null start date in darwinSampleListUtil.getFilteredMskimpactBrainSpineTimelineSet()", foundNullStartDate);

        } finally {
            mskimpactTimelineBrainSpineReader.close();
        }
    }

    private ArrayList<MskimpactBrainSpineTimeline> makeMockMskimpactBrainSpineTimelineResults() {
        ArrayList<MskimpactBrainSpineTimeline> mskimpactBrainSpineTimelineResults = new ArrayList<MskimpactBrainSpineTimeline>();

        MskimpactBrainSpineTimeline mskimpactBrainSpineTimelineNAStartDate = new MskimpactBrainSpineTimeline();
        mskimpactBrainSpineTimelineNAStartDate.setDMP_PATIENT_ID_ALL_BRAINSPINETMLN("P-0000001");
        mskimpactBrainSpineTimelineNAStartDate.setSTART_DATE("NA");

        MskimpactBrainSpineTimeline mskimpactBrainSpineTimelineNullStartDate = new MskimpactBrainSpineTimeline();
        mskimpactBrainSpineTimelineNullStartDate.setDMP_PATIENT_ID_ALL_BRAINSPINETMLN("P-0000002");
        mskimpactBrainSpineTimelineNullStartDate.setSTART_DATE(null);

        MskimpactBrainSpineTimeline mskimpactBrainSpineTimelineValidStartDate = new MskimpactBrainSpineTimeline();
        mskimpactBrainSpineTimelineValidStartDate.setDMP_PATIENT_ID_ALL_BRAINSPINETMLN("P-0000003");
        mskimpactBrainSpineTimelineValidStartDate.setSTART_DATE("757");

        mskimpactBrainSpineTimelineResults.add(mskimpactBrainSpineTimelineNAStartDate); // should be filtered by MskimpactTimelineBrainSpineReader.read()
        mskimpactBrainSpineTimelineResults.add(mskimpactBrainSpineTimelineNullStartDate); // should be filtered by MskimpactTimelineBrainSpineReader.read()
        mskimpactBrainSpineTimelineResults.add(mskimpactBrainSpineTimelineValidStartDate); // should not be filtered by MskimpactTimelineBrainSpineReader.read()

        return mskimpactBrainSpineTimelineResults;
    }

}

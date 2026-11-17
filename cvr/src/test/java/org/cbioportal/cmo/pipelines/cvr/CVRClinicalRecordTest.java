/*
 * Copyright (c) 2020 Memorial Sloan-Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr;

import java.util.List;
import org.cbioportal.cmo.pipelines.cvr.clinical.CVRClinicalFieldSetMapper;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRClinicalRecord;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.infrastructure.item.file.transform.DefaultFieldSet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
/**
 *
 * @author ochoaa
 */
@ContextConfiguration(classes=CvrTestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class CVRClinicalRecordTest {

    @Test
    public void testSampleTypeNull() throws Exception {
        CVRClinicalRecord record = new CVRClinicalRecord();
        String sampleType = record.resolveSampleType(null);
        Assert.assertTrue(sampleType.isEmpty());
    }

    @Test
    public void testSampleTypeNotNull() throws Exception {
        CVRClinicalRecord record = new CVRClinicalRecord();
        String primarySampleType = record.resolveSampleType(0);
        String metastasisSampleType = record.resolveSampleType(1);
        String localRecurrenceSampleType = record.resolveSampleType(2);
        String unknownSampleType = record.resolveSampleType(127);
        String defaultSampleType = record.resolveSampleType(128);

        Assert.assertTrue(primarySampleType.equalsIgnoreCase("primary"));
        Assert.assertTrue(metastasisSampleType.equalsIgnoreCase("metastasis"));
        Assert.assertTrue(localRecurrenceSampleType.equalsIgnoreCase("local recurrence"));
        Assert.assertTrue(unknownSampleType.equalsIgnoreCase("unknown"));
        Assert.assertTrue(defaultSampleType.isEmpty());
    }

    @Test
    public void testFieldNamesExcludePathSlideExists() throws Exception {
        List<String> fieldNames = CVRClinicalRecord.getFieldNames();
        Assert.assertFalse(fieldNames.contains("PATH_SLIDE_EXISTS"));
        Assert.assertTrue(fieldNames.contains("MSK_SLIDE_ID"));
    }

    @Test
    public void testFieldSetMapperIgnoresPathSlideExistsColumn() throws Exception {
        String[] names = {"SAMPLE_ID", "PATH_SLIDE_EXISTS", "MSK_SLIDE_ID"};
        String[] values = {"P-0000001-T01-IM3", "YES", "123456"};
        CVRClinicalRecord record = new CVRClinicalFieldSetMapper().mapFieldSet(new DefaultFieldSet(values, names));
        Assert.assertEquals("P-0000001-T01-IM3", record.getSAMPLE_ID());
        Assert.assertEquals("123456", record.getMSK_SLIDE_ID());
    }

}

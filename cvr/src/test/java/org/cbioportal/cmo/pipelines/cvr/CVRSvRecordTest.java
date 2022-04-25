/*
 * Copyright (c) 2020-2022 Memorial Sloan-Kettering Cancer Center.
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

import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRSvRecord;
import org.cbioportal.cmo.pipelines.cvr.model.CVRSvVariant;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
/**
 *
 * @author averyniceday
 */
@ContextConfiguration(classes=CvrTestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class CVRSvRecordTest {

    public CVRSvVariant mockCvrSvVariant(String annotation, String oncokbType) {
        CVRSvVariant mockCVRSvVariant = new CVRSvVariant();
        mockCVRSvVariant.setAnnotation(annotation);
        mockCVRSvVariant.setOncokbType(oncokbType);
        return mockCVRSvVariant;
    }

    @Test
    public void testValidAnnotationAndOncokbType() throws Exception {
        CVRSvVariant cvrSvVariant = mockCvrSvVariant("Annotation values go here", "VII");
        CVRSvRecord cvrSvRecord = new CVRSvRecord(cvrSvVariant, "S-001");
        Assert.assertEquals(cvrSvRecord.getAnnotation(), "Annotation values go here;ONCOKB_TYPE:VII");
    }

    @Test
    public void testNullAnnotation() throws Exception {
        CVRSvVariant cvrSvVariant = mockCvrSvVariant(null, "VII");
        CVRSvRecord cvrSvRecord = new CVRSvRecord(cvrSvVariant, "S-001");
        Assert.assertEquals(cvrSvRecord.getAnnotation(), "ONCOKB_TYPE:VII");
    }

    @Test
    public void testEmptyAnnotation() throws Exception {
        CVRSvVariant cvrSvVariant = mockCvrSvVariant("", "VII");
        CVRSvRecord cvrSvRecord = new CVRSvRecord(cvrSvVariant, "S-001");
        Assert.assertEquals(cvrSvRecord.getAnnotation(), "ONCOKB_TYPE:VII");
    }

    @Test
    public void testNullOncokbType() throws Exception {
        CVRSvVariant cvrSvVariant = mockCvrSvVariant("Annotation values go here", null);
        CVRSvRecord cvrSvRecord = new CVRSvRecord(cvrSvVariant, "S-001");
        Assert.assertEquals(cvrSvRecord.getAnnotation(), "Annotation values go here");
    }

    @Test
    public void testEmptyOncokbType() throws Exception {
        CVRSvVariant cvrSvVariant = mockCvrSvVariant("Annotation values go here", "");
        CVRSvRecord cvrSvRecord = new CVRSvRecord(cvrSvVariant, "S-001");
        Assert.assertEquals(cvrSvRecord.getAnnotation(), "Annotation values go here");
    }

    @Test
    public void testNullAnnotationAndOncokbType() throws Exception {
        CVRSvVariant cvrSvVariant = mockCvrSvVariant(null, null);
        CVRSvRecord cvrSvRecord = new CVRSvRecord(cvrSvVariant, "S-001");
        Assert.assertEquals(cvrSvRecord.getAnnotation(), "");
    }

    @Test
    public void testEmptyAnnotationAndOncokbType() throws Exception {
        CVRSvVariant cvrSvVariant = mockCvrSvVariant("", "");
        CVRSvRecord cvrSvRecord = new CVRSvRecord(cvrSvVariant, "S-001");
        Assert.assertEquals(cvrSvRecord.getAnnotation(), "");
    }

}

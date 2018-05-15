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

package org.mskcc.cmo.ks.ddp.source.composite;

import java.util.*;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mskcc.cmo.ks.ddp.source.composite.DDPCompositeRecord;
import org.mskcc.cmo.ks.ddp.source.model.Radiation;
import org.mskcc.cmo.ks.ddp.source.model.Chemotherapy;
import org.mskcc.cmo.ks.ddp.source.model.Surgery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.mskcc.cmo.ks.ddp.source.composite.DDPCompositeRecordTestConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=DDPCompositeRecordTestConfiguration.class)
public class DDPCompositeRecordTest {

    /* should be true if radiationProcedures member is present and non-empty
    */
    @Test
    public void hasReceivedRadiationTest() {
        makeRadiationProceduresAndAssert(null, null, Boolean.FALSE);
        makeRadiationProceduresAndAssert("", "", Boolean.FALSE);
        makeRadiationProceduresAndAssert("2016-12-10", "plan_1", Boolean.TRUE);
        makeRadiationProceduresAndAssert("2016-12-10,2016-12-20", "plan_1,plan_1", Boolean.TRUE);
        makeRadiationProceduresAndAssert("2016-12-10,2016-12-20", "plan_1,plan_2", Boolean.TRUE);
    }

    /* should be true if radiationProcedures member is present and contains radiation therapy of matching plan
    */
    @Test
    public void hasReceivedRadiationSubTypeTest() {
        makeRadiationProceduresAndAssertSubtype(null, null, "plan_1", Boolean.FALSE);
        makeRadiationProceduresAndAssertSubtype("", "", "plan_1", Boolean.FALSE);
        makeRadiationProceduresAndAssertSubtype("2016-12-10", "plan_1", "plan_1", Boolean.TRUE);
        makeRadiationProceduresAndAssertSubtype("2016-12-10,2016-12-20", "plan_1,plan_1", "plan_1", Boolean.TRUE);
        makeRadiationProceduresAndAssertSubtype("2016-12-10,2016-12-20", "plan_1,plan_2", "plan_1", Boolean.TRUE);
        makeRadiationProceduresAndAssertSubtype("2016-12-10,2016-12-20", "plan_1,plan_2", "plan_2", Boolean.TRUE);
        makeRadiationProceduresAndAssertSubtype("2016-12-10,2016-12-20", "plan_1,plan_2", "PLAN_2", Boolean.TRUE);
        makeRadiationProceduresAndAssertSubtype("2016-12-10,2016-12-20", "plan_1,plan_2", "plan_3", Boolean.FALSE);
    }

    /* should be true if chemoProcedures member is present and non-empty
    */
    @Test
    public void hasReceivedChemotherapyTest() {
        makeChemotherapyProceduresAndAssert(null, null, Boolean.FALSE);
        makeChemotherapyProceduresAndAssert("", "", Boolean.FALSE);
        makeChemotherapyProceduresAndAssert("2016-12-10", "name_1", Boolean.TRUE);
        makeChemotherapyProceduresAndAssert("2016-12-10,2016-12-20", "name_1,name_1", Boolean.TRUE);
        makeChemotherapyProceduresAndAssert("2016-12-10,2016-12-20", "name_1,name_2", Boolean.TRUE);
    }

    /* should be true if chemoProcedures member is present and contains chemotherapy of matching ordname
    */
    @Test
    public void hasReceivedChemotherapySubTypeTest() {
        makeRadiationProceduresAndAssertSubtype(null, null, "name_1", Boolean.FALSE);
        makeRadiationProceduresAndAssertSubtype("", "", "name_1", Boolean.FALSE);
        makeRadiationProceduresAndAssertSubtype("2016-12-10", "name_1", "name_1", Boolean.TRUE);
        makeRadiationProceduresAndAssertSubtype("2016-12-10,2016-12-20", "name_1,name_1", "name_1", Boolean.TRUE);
        makeRadiationProceduresAndAssertSubtype("2016-12-10,2016-12-20", "name_1,name_2", "name_1", Boolean.TRUE);
        makeRadiationProceduresAndAssertSubtype("2016-12-10,2016-12-20", "name_1,name_2", "name_2", Boolean.TRUE);
        makeRadiationProceduresAndAssertSubtype("2016-12-10,2016-12-20", "name_1,name_2", "NAME_2", Boolean.TRUE);
        makeRadiationProceduresAndAssertSubtype("2016-12-10,2016-12-20", "name_1,name_2", "name_3", Boolean.FALSE);
    }

    /* should be true if surgicalProcedures member is present and non-empty
    */
    @Test
    public void hasReceivedSurgeryTest() {
        makeSurgicalProceduresAndAssert(null, null, Boolean.FALSE);
        makeSurgicalProceduresAndAssert("", "", Boolean.FALSE);
        makeSurgicalProceduresAndAssert("2016-12-10", "description1", Boolean.TRUE);
        makeSurgicalProceduresAndAssert("2016-12-10,2016-12-20", "description_1,description_1", Boolean.TRUE);
        makeSurgicalProceduresAndAssert("2016-12-10,2016-12-20", "description_1,description_2", Boolean.TRUE);
    }

    /* should be true if surgicalProcedures member is present and contains any Surgeries of matching description
    */
    @Test
    public void hasReceivedSurgicalProceduresSubTypeTest() {
        makeSurgicalProceduresAndAssertSubtype(null, null, "description_1", Boolean.FALSE);
        makeSurgicalProceduresAndAssertSubtype("", "", "description_1", Boolean.FALSE);
        makeSurgicalProceduresAndAssertSubtype("2016-12-10", "description_1", "description_1", Boolean.TRUE);
        makeSurgicalProceduresAndAssertSubtype("2016-12-10,2016-12-20", "description_1,description_1", "description_1", Boolean.TRUE);
        makeSurgicalProceduresAndAssertSubtype("2016-12-10,2016-12-20", "description_1,description_2", "description_1", Boolean.TRUE);
        makeSurgicalProceduresAndAssertSubtype("2016-12-10,2016-12-20", "description_1,description_2", "description_2", Boolean.TRUE);
        makeSurgicalProceduresAndAssertSubtype("2016-12-10,2016-12-20", "description_1,description_2", "DESCRIPTION_2", Boolean.TRUE);
        makeSurgicalProceduresAndAssertSubtype("2016-12-10,2016-12-20", "description_1,description_2", "description_3", Boolean.FALSE);
    }

    private void makeRadiationProceduresAndAssert(String dateListString, String subTypeListString, Boolean expectedValue) {
        DDPCompositeRecord ddpCompositeRecord = makeRadiationProcedures(dateListString, subTypeListString);
        Boolean actualResult = ddpCompositeRecord.hasReceivedRadiation();
        Assert.assertTrue(expectedValue.equals(actualResult));
    }

    private void makeRadiationProceduresAndAssertSubtype(String dateListString, String subTypeListString, String subTypeQueryString, Boolean expectedValue) {
        DDPCompositeRecord ddpCompositeRecord = makeRadiationProcedures(dateListString, subTypeListString);
        Boolean actualResult = ddpCompositeRecord.hasReceivedRadiation(subTypeQueryString);
        Assert.assertTrue(expectedValue.equals(actualResult));
    }

    private DDPCompositeRecord makeRadiationProcedures(String dateListString, String subTypeListString) {
        DDPCompositeRecord ddpCompositeRecord = new DDPCompositeRecord();
        if (dateListString != null || subTypeListString != null) {
            String[] date = parseCommaSeparatedList(dateListString);
            String[] subType = parseCommaSeparatedList(subTypeListString);
            if (date.length != subType.length) {
                Assert.fail("bad test case : date and subType lists do not have matching length");
            }
            List<Radiation> radiationList = new ArrayList<Radiation>();
            for (int pos = 0; pos < date.length; pos = pos + 1) {
                Radiation radiation = new Radiation();
                radiation.setPlanName(subType[pos]);
                radiation.setRadOncTreatmentCourseStartDate(date[pos]);
                radiation.setRadOncTreatmentCourseStopDate(date[pos]);
                radiationList.add(radiation);
            }
            ddpCompositeRecord.setRadiationProcedures(radiationList);
        }
        return ddpCompositeRecord;
    }

    private void makeChemotherapyProceduresAndAssert(String dateListString, String subTypeListString, Boolean expectedValue) {
        DDPCompositeRecord ddpCompositeRecord = makeChemotherapyProcedures(dateListString, subTypeListString);
        Boolean actualResult = ddpCompositeRecord.hasReceivedChemo();
        Assert.assertTrue(expectedValue.equals(actualResult));
    }

    private void makeChemotherapyProceduresAndAssertSubtype(String dateListString, String subTypeListString, String subTypeQueryString, Boolean expectedValue) {
        DDPCompositeRecord ddpCompositeRecord = makeChemotherapyProcedures(dateListString, subTypeListString);
        Boolean actualResult = ddpCompositeRecord.hasReceivedChemo(subTypeQueryString);
        Assert.assertTrue(expectedValue.equals(actualResult));
    }

    private DDPCompositeRecord makeChemotherapyProcedures(String dateListString, String subTypeListString) {
        DDPCompositeRecord ddpCompositeRecord = new DDPCompositeRecord();
        if (dateListString != null || subTypeListString != null) {
            String[] date = parseCommaSeparatedList(dateListString);
            String[] subType = parseCommaSeparatedList(subTypeListString);
            if (date.length != subType.length) {
                Assert.fail("bad test case : date and subType lists do not have matching length");
            }
            List<Chemotherapy> chemotherapyList = new ArrayList<Chemotherapy>();
            for (int pos = 0; pos < date.length; pos = pos + 1) {
                Chemotherapy chemotherapy = new Chemotherapy();
                chemotherapy.setORDNAME(subType[pos]);
                chemotherapy.setSTARTDATE(date[pos]);
                chemotherapy.setSTOPDATE(date[pos]);
                chemotherapyList.add(chemotherapy);
            }
            ddpCompositeRecord.setChemoProcedures(chemotherapyList);
        }
        return ddpCompositeRecord;
    }

    private void makeSurgicalProceduresAndAssert(String dateListString, String subTypeListString, Boolean expectedValue) {
        DDPCompositeRecord ddpCompositeRecord = makeSurgicalProcedures(dateListString, subTypeListString);
        Boolean actualResult = ddpCompositeRecord.hasReceivedSurgery();
        Assert.assertTrue(expectedValue.equals(actualResult));
    }

    private void makeSurgicalProceduresAndAssertSubtype(String dateListString, String subTypeListString, String subTypeQueryString, Boolean expectedValue) {
        DDPCompositeRecord ddpCompositeRecord = makeSurgicalProcedures(dateListString, subTypeListString);
        Boolean actualResult = ddpCompositeRecord.hasReceivedSurgery(subTypeQueryString);
        Assert.assertTrue(expectedValue.equals(actualResult));
    }

    private DDPCompositeRecord makeSurgicalProcedures(String dateListString, String subTypeListString) {
        DDPCompositeRecord ddpCompositeRecord = new DDPCompositeRecord();
        if (dateListString != null || subTypeListString != null) {
            String[] date = parseCommaSeparatedList(dateListString);
            String[] subType = parseCommaSeparatedList(subTypeListString);
            if (date.length != subType.length) {
                Assert.fail("bad test case : date and subType lists do not have matching length");
            }
            List<Surgery> surgeryList = new ArrayList<Surgery>();
            for (int pos = 0; pos < date.length; pos = pos + 1) {
                Surgery surgery = new Surgery();
                surgery.setProcedureDescription(subType[pos]);
                surgery.setProcedureDate(date[pos]);
                surgeryList.add(surgery);
            }
            ddpCompositeRecord.setSurgicalProcedures(surgeryList);
        }
        return ddpCompositeRecord;
    }

    private String[] parseCommaSeparatedList(String listString) {
        if (listString == null || listString.trim().length() == 0) {
            return new String[0];
        }
        return listString.split(",", -1);
    }

}

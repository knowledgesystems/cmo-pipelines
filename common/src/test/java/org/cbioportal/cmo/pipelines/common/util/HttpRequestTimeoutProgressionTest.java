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

@ContextConfiguration(classes=Object.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class HttpRequestTimeoutProgressionTest {

    @Test
    public void testClockStartsAtInitial() throws Exception {
        int initialTimeout = 1000;
        int maximumTimeout = 10000;
        HttpRequestTimeoutProgression timeoutProgression = new HttpRequestTimeoutProgression(initialTimeout, maximumTimeout, Instant.now().plusSeconds(4 * maximumTimeout));
        Assert.assertEquals("timeout progression failed to start at initial timeout", initialTimeout, timeoutProgression.getNextTimeoutForRequest());
    }

    @Test
    public void testClockHitsMaximum() throws Exception {
        int initialTimeout = 1000;
        int maximumTimeout = 10000;
        HttpRequestTimeoutProgression timeoutProgression = new HttpRequestTimeoutProgression(initialTimeout, maximumTimeout, Instant.now().plusSeconds(4 * maximumTimeout));
        // this test assumes that the timeout growth rate per retry cycle is at least 1%
        for (int i = 0; i < 256; i++) {
            timeoutProgression.getNextTimeoutForRequest();
        }
        Assert.assertEquals("timeout progression failed to hit maximum after numerous iterations", maximumTimeout, timeoutProgression.getNextTimeoutForRequest());
    }

    @Test
    public void testClockHitsDropDead() throws Exception {
        int initialTimeout = 1000;
        int maximumTimeout = 10000;
        int dropDeadReturnValue = 0;
        HttpRequestTimeoutProgression timeoutProgression = new HttpRequestTimeoutProgression(initialTimeout, maximumTimeout, Instant.now());
        Assert.assertEquals("timeout progression failed to hit maximum after numerous iterations", dropDeadReturnValue, timeoutProgression.getNextTimeoutForRequest());
    }

}

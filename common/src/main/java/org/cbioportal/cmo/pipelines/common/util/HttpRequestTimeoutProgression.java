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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.cbioportal.cmo.pipelines.common.util;

import java.util.*;
import java.util.Properties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.context.annotation.Bean;

public class HttpRequestTimeoutProgression {

    private int initialTimeout; // the first configured timeout to be used before retry (in milliseconds)
    private int maximumTimeout; // the upper limit for subsequent configured timeouts before retry (in milliseconds)
    private Instant dropDeadMoment; // a fixed time at which any current attempt and any further attempt will be abondoned, returning with failure
    private int timeoutForNextQuery;

    public HttpRequestTimeoutProgression() {
        initialTimeout = 1000;
        maximumTimeout = 60000;
        dropDeadMoment = Instant.now().plusSeconds(4 * maximumTimeout); // default alows 19 timeout loops to complete, ranging from 1 second to 60 seconds (the last three iterations have 60 second timeouts.)
        timeoutForNextQuery = initialTimeout;
    }

/* if dropDeadMoment is null, by default the dropDeadMoment will be set to a point in the future 4 times the maximum timeout from now
*/
    public HttpRequestTimeoutProgression(int initialTimeout, int maximumTimeout, Instant dropDeadMoment) {
        this.initialTimeout = initialTimeout;
        this.maximumTimeout = maximumTimeout;
        if (dropDeadMoment != null) {
            this.dropDeadMoment = dropDeadMoment;
        } else {
            this.dropDeadMoment = Instant.now().plusSeconds(4 * maximumTimeout);
        }
        this.timeoutForNextQuery = initialTimeout;
    }

/* a zero response means we have hit the drop dead limit
*/
    public int getNextTimeoutForRequest() {
        Instant now = Instant.now();
        long timeUntilDropDead = now.until(dropDeadMoment, ChronoUnit.MILLIS);
        if (timeUntilDropDead <= 0) {
            return 0;
        }
        if (timeUntilDropDead <= timeoutForNextQuery) {
            return Long.valueOf(timeUntilDropDead).intValue();
        }
        if (maximumTimeout <= timeoutForNextQuery) {
            return maximumTimeout;
        }
        int timeout = timeoutForNextQuery;
        timeoutForNextQuery = timeoutForNextQuery * 128 / 100; // increase timeout by 28% : timeout doubles every 3 iterations
        return timeout;
    }

}

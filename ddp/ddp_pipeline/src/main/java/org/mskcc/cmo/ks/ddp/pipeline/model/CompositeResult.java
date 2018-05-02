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

package org.mskcc.cmo.ks.ddp.pipeline.model;

import java.util.List;

/**
 *
 * @author ochoaa
 */
public class CompositeResult {
    private String clinicalRecord;
    private List<String> timelineRadiationRecords;
    private List<String> timelineChemoRecords;
    private List<String> timelineSurgeryRecords;

    public CompositeResult(){}

    /**
     * @return the clinicalRecord
     */
    public String getClinicalRecord() {
        return clinicalRecord;
    }

    /**
     * @param clinicalRecord the clinicalRecord to set
     */
    public void setClinicalRecord(String clinicalRecord) {
        this.clinicalRecord = clinicalRecord;
    }

    /**
     * @return the timelineRadiationRecords
     */
    public List<String> getTimelineRadiationRecords() {
        return timelineRadiationRecords;
    }

    /**
     * @param timelineRadiationRecords the timelineRadiationRecords to set
     */
    public void setTimelineRadiationRecords(List<String> timelineRadiationRecords) {
        this.timelineRadiationRecords = timelineRadiationRecords;
    }

    /**
     * @return the timelineChemoRecords
     */
    public List<String> getTimelineChemoRecords() {
        return timelineChemoRecords;
    }

    /**
     * @param timelineChemoRecords the timelineChemoRecords to set
     */
    public void setTimelineChemoRecords(List<String> timelineChemoRecords) {
        this.timelineChemoRecords = timelineChemoRecords;
    }

    /**
     * @return the timelineSurgeryRecords
     */
    public List<String> getTimelineSurgeryRecords() {
        return timelineSurgeryRecords;
    }

    /**
     * @param timelineSurgeryRecords the timelineSurgeryRecords to set
     */
    public void setTimelineSurgeryRecords(List<String> timelineSurgeryRecords) {
        this.timelineSurgeryRecords = timelineSurgeryRecords;
    }
}

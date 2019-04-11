/*
 * Copyright (c) 2018-2019 Memorial Sloan-Kettering Cancer Center.
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
    private String clinicalResult;
    private List<String> timelineRadiationResults;
    private List<String> timelineChemoResults;
    private List<String> timelineSurgeryResults;
    private String suppVitalStatusResult;
    private String suppAgeResult;
    private String suppNaccrMappingsResult;

    public CompositeResult(){}

    /**
     * @return the clinicalResult
     */
    public String getClinicalResult() {
        return clinicalResult;
    }

    /**
     * @param clinicalResult the clinicalResult to set
     */
    public void setClinicalResult(String clinicalResult) {
        this.clinicalResult = clinicalResult;
    }

    /**
     * @return the timelineRadiationResults
     */
    public List<String> getTimelineRadiationResults() {
        return timelineRadiationResults;
    }

    /**
     * @param timelineRadiationResults the timelineRadiationResults to set
     */
    public void setTimelineRadiationResults(List<String> timelineRadiationResults) {
        this.timelineRadiationResults = timelineRadiationResults;
    }

    /**
     * @return the timelineChemoResults
     */
    public List<String> getTimelineChemoResults() {
        return timelineChemoResults;
    }

    /**
     * @param timelineChemoResults the timelineChemoResults to set
     */
    public void setTimelineChemoResults(List<String> timelineChemoResults) {
        this.timelineChemoResults = timelineChemoResults;
    }

    /**
     * @return the timelineSurgeryResults
     */
    public List<String> getTimelineSurgeryResults() {
        return timelineSurgeryResults;
    }

    /**
     * @param timelineSurgeryResults the timelineSurgeryResults to set
     */
    public void setTimelineSurgeryResults(List<String> timelineSurgeryResults) {
        this.timelineSurgeryResults = timelineSurgeryResults;
    }

    /**
     * @return the suppVitalStatusResult
     */
    public String getSuppVitalStatusResult() {
        return suppVitalStatusResult;
    }

    /**
     * @param suppVitalStatusResult the suppVitalStatusResult to set
     */
    public void setSuppVitalStatusResult(String suppVitalStatusResult) {
        this.suppVitalStatusResult = suppVitalStatusResult;
    }

    /**
     * @return the suppAgeResult
     */
    public String getSuppAgeResult() {
        return suppAgeResult;
    }

    /**
     * @param suppAgeResult the suppAgeResult to set
     */
    public void setSuppAgeResult(String suppAgeResult) {
        this.suppAgeResult = suppAgeResult;
    }

    /**
     * @return the suppNaccrMappingsResult
     */
    public String getSuppNaccrMappingsResult() {
        return suppNaccrMappingsResult;
    }

    /**
     * @param suppNaccrMappingsResult the suppNaccrMappingsResult to set
     */
    public void setSuppNaccrMappingsResult(String suppNaccrMappingsResult) {
        this.suppNaccrMappingsResult = suppNaccrMappingsResult;
    }
}

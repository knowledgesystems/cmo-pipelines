/*
 * Copyright (c) 2016 - 2017 Memorial Sloan-Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr.model;

/**
 *
 * @author jake
 */
public class CVRConsumeSample {
    private Integer affectedRows;
    private String dmp_sample_id;
    private String aly2sample_id;
    private String disclaimer;

    public CVRConsumeSample() {
    }

    public CVRConsumeSample(Integer affectedRows, String dmp_sample_id, String aly2sample_id, String disclaimer) {
        this.affectedRows = affectedRows;
        this.dmp_sample_id = dmp_sample_id;
        this.aly2sample_id = aly2sample_id;
        this.disclaimer = disclaimer;
    }

    public Integer getaffectedRows() {
        return affectedRows;
    }

    public void setaffectedRows(Integer affectedRows) {
        this.affectedRows = affectedRows;
    }

    public String getdmp_sample_id() {
        return dmp_sample_id;
    }

    public void setdmp_sample_id(String dmp_sample_id) {
        this.dmp_sample_id = dmp_sample_id;
    }

    public String getaly2sample_id() {
        return aly2sample_id;
    }

    public void setaly2sample_id(String aly2sample_id) {
        this.aly2sample_id = aly2sample_id;
    }

    public String getdisclaimer() {
        return disclaimer;
    }

    public void setdisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}

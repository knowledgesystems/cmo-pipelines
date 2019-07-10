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

package org.cbioportal.cmo.pipelines.cvr.model.composite;

public class CompositeClinicalRecord {
    private String newClinicalRecord;
    private String oldClinicalRecord;
    private String seqDateRecord;

    public CompositeClinicalRecord() {
        this.newClinicalRecord = "";
        this.oldClinicalRecord = "";
        this.seqDateRecord = "";
    }

    public CompositeClinicalRecord(String newClinicalRecord, String oldClinicalRecord, String seqDateRecord) {
        this.newClinicalRecord = newClinicalRecord;
        this.oldClinicalRecord = oldClinicalRecord;
        this.seqDateRecord = seqDateRecord;
    }

    public void setNewClinicalRecord(String newClinicalRecord) {
        this.newClinicalRecord = newClinicalRecord;
    }

    public String getNewClinicalRecord() {
        return newClinicalRecord.isEmpty() ? null : newClinicalRecord;
    }

    public void setOldClinicalRecord(String oldClinicalRecord) {
        this.oldClinicalRecord = oldClinicalRecord;
    }

    public String getOldClinicalRecord() {
        return oldClinicalRecord.isEmpty() ? null : oldClinicalRecord;
    }
    
    public void setSeqDateRecord(String seqDateRecord) {
        this.seqDateRecord = seqDateRecord;
    }
    
    public String getSeqDateRecord() {
        return seqDateRecord.isEmpty() ? null : seqDateRecord;
    }
}

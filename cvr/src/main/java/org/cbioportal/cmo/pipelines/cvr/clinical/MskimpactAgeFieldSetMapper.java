/*
 * Copyright (c) 2016 - 2017, 2025 Memorial Sloan-Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr.clinical;

import java.util.List;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.model.staging.MskimpactAge;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

/**
 *
 * @author heinsz
 */
public class MskimpactAgeFieldSetMapper implements  FieldSetMapper<MskimpactAge>{

    Logger log = Logger.getLogger(CVRClinicalFieldSetMapper.class);

    @Override
    public MskimpactAge mapFieldSet(FieldSet fs) throws BindException {
        MskimpactAge record = new MskimpactAge();
        List<String> fields = MskimpactAge.getFieldNames();

        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i);
            String value = (i < fs.getFieldCount()) ? fs.readString(i) : "";  // default to empty string if out of bounds
            setFieldValue(record, field, value);
        }
        return record;
    }

    private void setFieldValue(MskimpactAge record, String field, String value) {
        switch (field) {
            case "PATIENT_ID":
                record.setPATIENT_ID(value);
                break;
            case "AGE":
                record.setAGE(value);
                break;
            default:
                log.info("No set method exists for " + field);
                break;
        }
    }
}

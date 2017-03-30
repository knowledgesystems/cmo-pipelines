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

import java.util.*;

/**
 *
 * @author jake
 */
public class CVRSegRecord {
    private String chromosome;
    private String start;
    private String end;
    private String num_mark;
    private String seg_mean;
    private String id;
    private String isNew;

    public CVRSegRecord() {
    }

    public void setIsNew(String isNew) {
        this.isNew = isNew;
    }

    public String getIsNew() {
        return this.isNew != null ? this.isNew : "";
    }

    public String getchrom() {
        return this.chromosome != null ? this.chromosome : "";
    }

    public void setchrom(String chromosome) {
        this.chromosome = chromosome;
    }

    public void setloc_start(String start) {
        this.start = start;
    }

    public String getloc_start() {
        return this.start != null ? this.start : "";
    }

    public void setloc_end(String end) {
        this.end = end;
    }

    public String getloc_end() {
        return this.end != null ? this.end : "";
    }

    public void setnum_mark(String num_mark) {
        this.num_mark = num_mark;
    }

    public String getnum_mark() {
        return this.num_mark != null ? this.num_mark : "";
    }

    public void setseg_mean(String seg_mean) {
        this.seg_mean = seg_mean;
    }

    public String getseg_mean() {
        return this.seg_mean != null ? this.seg_mean : "";
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getID() {
        return this.id != null ? this.id : "";
    }

    public static List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("ID");
        fieldNames.add("chrom");
        fieldNames.add("loc_start");
        fieldNames.add("loc_end");
        fieldNames.add("num_mark");
        fieldNames.add("seg_mean");
        return fieldNames;
    }

    //reformatted field names as original fields contained "."
    //getHeaderNames reverts change to original format
    public static List<String> getHeaderNames() {
        List<String> headerNames = getFieldNames();
        for (int i=0; i<headerNames.size();i++) {
            headerNames.set(i, headerNames.get(i).replace("_", "."));
        }
        return headerNames;
    }

}

/*
 * Copyright (c) 2016 Memorial Sloan-Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.foundation.model;

import java.io.Serializable;
import java.util.*;
import javax.xml.bind.annotation.*;

/**
 *
 * @author Prithi Chakrapani, ochoaa
 */
 @XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SampleType", propOrder = {
    "comment", 
    "content",
    "baitSet",
    "meanExonDepth",
    "percentTumorNuclei"        
})
public class SampleType {
    
   @XmlElement(name = "comment", required = false)
   protected String comment;
   
   @XmlMixed
   protected List<Serializable> content;
   
   @XmlAttribute(name = "bait-set")
   protected String baitSet;
   
   @XmlAttribute(name = "mean-exon-depth")
   protected Float meanExonDepth;
   
   @XmlAttribute(name = "name" )
   protected String name;
   
   @XmlAttribute(name = "percent-tumor-nuclei")
   protected Integer percentTumorNuclei;
   
    /**
     * @return the content
     */
    public List<Serializable> getContent() {
        if (content == null) {
            content = new ArrayList<Serializable>();
        }
        return this.content;
    }   

    /**
     * @return the baitSet
     */
    public String getBaitSet() {
        return baitSet;
    }

    /**
     * @param baitSet the baitSet to set
     */
    public void setBaitSet(String baitSet) {
        this.baitSet = baitSet;
    }

    /**
     * @return the meanExonDepth
     */
    public Float getMeanExonDepth() {
        return meanExonDepth;
    }

    /**
     * @param meanExonDepth the meanExonDepth to set
     */
    public void setMeanExonDepth(Float meanExonDepth) {
        this.meanExonDepth = meanExonDepth;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the percentTumorNuclei
     */
    public Integer getPercentTumorNuclei() {
        return percentTumorNuclei;
    }

    /**
     * @param percentTumorNuclei the percentTumorNuclei to set
     */
    public void setPercentTumorNuclei(Integer percentTumorNuclei) {
        this.percentTumorNuclei = percentTumorNuclei;
    }
    
}
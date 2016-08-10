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

import javax.xml.bind.annotation.*;

/**
 * @author Prithi Chakrapani, ochoaa
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ShortVariantType", propOrder = { 
    "value"
})
public class ShortVariantType {

    @XmlValue
    protected String value;
    
    @XmlAttribute(name = "cds-effect")
    protected String cdsEffect;
    
    @XmlAttribute(name = "depth")
    protected Short depth; 
    
    @XmlAttribute(name = "functional-effect")
    protected String functionalEffect;
    
    @XmlAttribute(name = "gene")
    protected String gene; 
    
    @XmlAttribute(name = "percent-reads")
    protected Float percentReads;
    
    @XmlAttribute(name = "position")
    protected String position;
 
    @XmlAttribute(name = "protein-effect")
    protected String proteinEffect;
    
    @XmlAttribute(name = "status")
    protected String status;
          
    @XmlAttribute(name = "strand")
    protected String strand;
    
    @XmlAttribute(name = "transcript")
    protected String transcript;        

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the cdsEffect
     */
    public String getCdsEffect() {
        return cdsEffect;
    }

    /**
     * @param cdsEffect the cdsEffect to set
     */
    public void setCdsEffect(String cdsEffect) {
        this.cdsEffect = cdsEffect;
    }

    /**
     * @return the depth
     */
    public Short getDepth() {
        return depth;
    }

    /**
     * @param depth the depth to set
     */
    public void setDepth(Short depth) {
        this.depth = depth;
    }

    /**
     * @return the functionalEffect
     */
    public String getFunctionalEffect() {
        return functionalEffect;
    }

    /**
     * @param functionalEffect the functionalEffect to set
     */
    public void setFunctionalEffect(String functionalEffect) {
        this.functionalEffect = functionalEffect;
    }

    /**
     * @return the gene
     */
    public String getGene() {
        return gene;
    }

    /**
     * @param gene the gene to set
     */ 
    public void setGene(String gene) {
        this.gene = gene;
    }

    /**
     * @return the percentReads
     */
    public Float getPercentReads() {
        return percentReads;
    }

    /**
     * @param percentReads the percentReads to set
     */
    public void setPercentReads(Float percentReads) {
        this.percentReads = percentReads;
    }

    /**
     * @return the position
     */
    public String getPosition() {
        return position;
    }

    /**
     * @param position the position to set
     */
    public void setPosition(String position) {
        this.position = position;
    }

    /**
     * @return the proteinEffect
     */
    public String getProteinEffect() {
        return proteinEffect;
    }

    /**
     * @param proteinEffect the proteinEffect to set
     */
    public void setProteinEffect(String proteinEffect) {
        this.proteinEffect = proteinEffect;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the strand
     */
    public String getStrand() {
        return strand;
    }

    /**
     * @param strand the strand to set
     */
    public void setStrand(String strand) {
        this.strand = strand;
    }

    /**
     * @return the transcript
     */
    public String getTranscript() {
        return transcript;
    }

    /**
     * @param transcript the transcript to set
     */
    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

}
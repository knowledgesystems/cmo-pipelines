/*
 * Copyright (c) 2015 Memorial Sloan-Kettering Cancer Center.
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
 * This file is part of cBioPortal.
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

package org.cbioportal.cmo.pipelines.importer.util;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 *
 * @author ochoaa
 */
public class DataFileUtils {
    
    public static String DELIMITER = "\t";
    public static String METADATA_PREFIX = "#";
    public static String DEFAULT_DESCRIPTION = "MISSING";    
    public static String DEFAULT_DATATYPE = "STRING";
    public static String DEFAULT_ATTRIBUTE_TYPE = "SAMPLE";
    public static String DEFAULT_PRIORITY = "1";

    /**
     * list data files in directory by file pattern
     * @param directory
     * @param filePattern
     * @return
     * @throws IOException 
     */
    public static List<File> listDataFiles(String directory, String filePattern) throws IOException {
     List<File> dataFiles = new ArrayList();
     
     for (Path file : Files.newDirectoryStream(Paths.get(directory), filePattern.trim())) {
         dataFiles.add(file.toFile());
     }
     
     return dataFiles;
    }
    
    /**
     * light string processing for tab delimited files
     * @param line
     * @return 
     */
    public static String[] splitDataFields(String line) {
        line = line.replaceAll("^" + METADATA_PREFIX + "+", "");
        String[] fields = line.split(DELIMITER, -1);

        return fields;                   
    }
}
/*
 * Copyright (c) 2025 Memorial Sloan-Kettering Cancer Center.
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

import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRClinicalRecord;
import org.cbioportal.cmo.pipelines.cvr.model.*;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.core.io.FileSystemResource;

/**
 *
 * @author Manda Wilson
 */
public class ClinicalFileReaderUtil {

    public static FlatFileItemReader<CVRClinicalRecord> createReader(File mskimpactClinicalFile) throws IOException {
        // Read the first line (header) from the file
        BufferedReader br = new BufferedReader(new FileReader(mskimpactClinicalFile));
        String headerLine = br.readLine();
        br.close();  // Close after reading the first line

        if (headerLine == null || headerLine.isEmpty()) {
            throw new IllegalStateException("The file is empty or has no header.");
        }

        // Extract column names from the header
        String[] columnNames = headerLine.split("\t");

        // Configure the tokenizer with dynamic column names
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB);
        tokenizer.setNames(columnNames);  // Dynamically set field names
        tokenizer.setQuoteCharacter('\0'); // Use the null character to effectively disable quotes 
                                           // The default quote character is a " which was causing issues in a record where there was a single unclosed "

        // Create the reader
        FlatFileItemReader<CVRClinicalRecord> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(mskimpactClinicalFile));
        reader.setLinesToSkip(1);  // Skip the header row after reading it

        // Set up the line mapper
        DefaultLineMapper<CVRClinicalRecord> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(new CVRClinicalFieldSetMapper());

        reader.setLineMapper(lineMapper);
        return reader;
    }
}

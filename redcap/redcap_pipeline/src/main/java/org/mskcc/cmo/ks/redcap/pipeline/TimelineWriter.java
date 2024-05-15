/*
 * Copyright (c) 2016, 2017, 2023, 2024 Memorial Sloan Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan Kettering Cancer
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

package org.mskcc.cmo.ks.redcap.pipeline;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;

/**
 *
 * @author heinsz
 */
public class TimelineWriter  implements ItemStreamWriter<String> {
    @Value("#{jobParameters[directory]}")
    private String directory;

    @Value("#{jobParameters[rawData]}")
    private boolean rawData;

    @Value("#{stepExecutionContext['timelineHeader']}")
    private List<String> timelineHeader;

    @Value("#{stepExecutionContext['projectTitle']}")
    private String projectTitle;

    @Value("#{stepExecutionContext['writeTimelineData']}")
    private boolean writeTimelineData;

    @Value("#stepExecutionContext['standardTimelineDataFields']")
    private List<String> standardTimelineDataFields;

    private static final String OUTPUT_FILENAME_PREFIX = "data_timeline";
    private File stagingFile;
    private FlatFileItemWriter<String> flatFileItemWriter = new FlatFileItemWriter<String>();

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        if (rawData) {
            this.stagingFile = new File(directory, OUTPUT_FILENAME_PREFIX + "_" + projectTitle + ".txt");
        }
        else {
            this.stagingFile = new File(directory, OUTPUT_FILENAME_PREFIX + ".txt");
        }
        if (writeTimelineData) {
            PassThroughLineAggregator<String> aggr = new PassThroughLineAggregator<>();
            flatFileItemWriter.setLineAggregator(aggr);
            flatFileItemWriter.setResource(new FileSystemResource(stagingFile));
            flatFileItemWriter.setHeaderCallback(new FlatFileHeaderCallback() {
                @Override
                public void writeHeader(Writer writer) throws IOException {
                    writer.write(getHeaderLine(timelineHeader));
                }
            });
            flatFileItemWriter.open(ec);
        }
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
        if (writeTimelineData) {
            ec.put("timelineFile", stagingFile.getName());
        }
    }

    @Override
    public void close() throws ItemStreamException {
        flatFileItemWriter.close();
    }

    @Override
    public void write(Chunk<? extends String> items) throws Exception {
        if (writeTimelineData) {
            flatFileItemWriter.write(items);
        }
    }

    private String getHeaderLine(List<String> metaData) {
        List<String> header = new ArrayList<>();
        for (String column : standardTimelineDataFields) {
            if (metaData.contains(column)) {
                header.add(column);
            }
        }
        for (String column : metaData) {
            if (!standardTimelineDataFields.contains(column) && !column.equals("RECORD_ID")) {
                header.add(column);
            }
        }
        return String.join("\t", header);
    }

}

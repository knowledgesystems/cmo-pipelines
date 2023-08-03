/*
 * Copyright (c) 2016, 2023 Memorial Sloan Kettering Cancer Center.
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

package org.mskcc.cmo.ks.darwin.pipeline.mskimpactbrainspinetimeline;

import com.google.common.base.Strings;
import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.darwin.pipeline.model.MskimpactBrainSpineCompositeTimeline;
import org.mskcc.cmo.ks.darwin.pipeline.model.MskimpactBrainSpineTimeline;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.*;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;

/**
 *
 * @author jake
 */
public class MskimpactTimelineBrainSpineWriter implements ItemStreamWriter<MskimpactBrainSpineCompositeTimeline>{
    private BrainSpineTimelineType type;

    public MskimpactTimelineBrainSpineWriter(BrainSpineTimelineType type) {
        this.type = type;
    }

    Logger log = Logger.getLogger(MskimpactTimelineBrainSpineWriter.class);
    @Value("#{jobParameters[outputDirectory]}")
    private String outputDirectory;

    private int recordsWritten;
    private List<String> writeList = new ArrayList<>();
    private final FlatFileItemWriter<String> flatFileItemWriter = new FlatFileItemWriter<>();
    private File stagingFile;

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException{
        stagingFile = new File(outputDirectory, "data_timeline_" + type.toString().toLowerCase() + "_caisis_gbm.txt");
        PassThroughLineAggregator aggr = new PassThroughLineAggregator();
        flatFileItemWriter.setLineAggregator(aggr);
        flatFileItemWriter.setHeaderCallback(new FlatFileHeaderCallback(){
            @Override
            public void writeHeader(Writer writer) throws IOException{
                MskimpactBrainSpineTimeline mskImpactBrainSpineTimeline = new MskimpactBrainSpineTimeline();
                try {
                    writer.write(String.join("\t", (List<String>)mskImpactBrainSpineTimeline.getClass().getMethod("get" + type.toString() + "Headers").invoke(mskImpactBrainSpineTimeline)));
                }
                catch(Exception e) {
                    throw new ItemStreamException(e);
                }
            }
        });

        flatFileItemWriter.setResource(new FileSystemResource(this.stagingFile));
        flatFileItemWriter.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException{}

    @Override
    public void close() throws ItemStreamException{
        if (recordsWritten == 0) {
            throw new RuntimeException("No records were written to output file: " + stagingFile.getName() + " - exiting...");
        }
        flatFileItemWriter.close();
    }

    @Override
    public void write(List<? extends MskimpactBrainSpineCompositeTimeline> items) throws Exception{
        writeList.clear();
        for (MskimpactBrainSpineCompositeTimeline result : items) {
            String record = (String) result.getClass().getMethod("get" + type.toString() + "Result").invoke(result);
            if (!Strings.isNullOrEmpty(record) && !record.equals(MskimpactBrainSpineCompositeTimeline.NO_RESULT)) {
                writeList.add(record);
                recordsWritten++;
            }
        }
        if(!writeList.isEmpty()){
            flatFileItemWriter.write(writeList);
        }
    }
}

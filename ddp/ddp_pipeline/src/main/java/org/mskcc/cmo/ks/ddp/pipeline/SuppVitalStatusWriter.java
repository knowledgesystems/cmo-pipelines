/*
 * Copyright (c) 2019, 2023 Memorial Sloan Kettering Cancer Center.
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

package org.mskcc.cmo.ks.ddp.pipeline;

import com.google.common.base.Strings;
import java.io.*;
import java.util.*;
import org.mskcc.cmo.ks.ddp.pipeline.model.CompositeResult;
import org.mskcc.cmo.ks.ddp.pipeline.model.SuppVitalStatusRecord;
import org.mskcc.cmo.ks.ddp.pipeline.util.DDPUtils;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.*;
import org.springframework.batch.item.file.transform.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;

/**
 *
 * @author ochoaa
 */
public class SuppVitalStatusWriter implements ItemStreamWriter<CompositeResult> {

    @Value("#{jobParameters[outputDirectory]}")
    private String outputDirectory;

    @Value("${ddp.supp.dirname}")
    private String ddpSuppDirname;

    @Value("${ddp.supp.vital_status_filename}")
    private String ddpSuppVitalStatusFilename;

    @Value("#{jobParameters[cohortName]}")
    private String cohortName;

    private FlatFileItemWriter<String> flatFileItemWriter = new FlatFileItemWriter<>();

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        if (DDPUtils.isMskimpactCohort(cohortName) || DDPUtils.isHemepactCohort(cohortName) || DDPUtils.isMskaccessCohort(cohortName)) {
            File stagingFile = new File(outputDirectory, ddpSuppDirname + File.separator + ddpSuppVitalStatusFilename);
            LineAggregator<String> aggr = new PassThroughLineAggregator<>();
            flatFileItemWriter.setLineAggregator(aggr);
            flatFileItemWriter.setHeaderCallback(new FlatFileHeaderCallback() {
                @Override
                public void writeHeader(Writer writer) throws IOException {
                    writer.write(String.join("\t", SuppVitalStatusRecord.getFieldNames()));
                }
            });
            flatFileItemWriter.setResource(new FileSystemResource(stagingFile));
            flatFileItemWriter.open(ec);
        }
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {
        if (DDPUtils.isMskimpactCohort(cohortName) || DDPUtils.isHemepactCohort(cohortName) || DDPUtils.isMskaccessCohort(cohortName)) {
            flatFileItemWriter.close();
        }
    }

    @Override
    public void write(List<? extends CompositeResult> compositeResults) throws Exception {
        if (DDPUtils.isMskimpactCohort(cohortName) || DDPUtils.isHemepactCohort(cohortName) || DDPUtils.isMskaccessCohort(cohortName)) {
            List<String> records = new ArrayList<>();
            for (CompositeResult result : compositeResults) {
                if (Strings.isNullOrEmpty(result.getSuppVitalStatusResult())) {
                    continue;
                }
                records.add(result.getSuppVitalStatusResult());
            }
            flatFileItemWriter.write(records);
        }
    }
}

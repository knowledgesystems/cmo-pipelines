/*
 * Copyright (c) 2018 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.redcap.source.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gdata.client.spreadsheet.*;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.common.base.Preconditions;
import java.io.*;
import java.net.*;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.logging.*;
import org.mskcc.cmo.ks.redcap.models.ProjectInfoResponse;
import org.mskcc.cmo.ks.redcap.models.RedcapAttributeMetadata;
import org.mskcc.cmo.ks.redcap.models.RedcapProjectAttribute;
import org.mskcc.cmo.ks.redcap.models.RedcapToken;
import org.mskcc.cmo.ks.redcap.source.ClinicalDataSource;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Repository
public class GoogleSessionManager {

    @Value("${google.id}")
    private String googleId;
    @Value("${google.pw}")
    private String googlePw;
    @Value("${importer.spreadsheet_service_appname}")
    private String appName;
    @Value("${importer.spreadsheet}")
    private String gdataSpreadsheet;
    @Value("${importer.google.service.email}")
    private String googleServiceEmail;
    @Value("${importer.google.service.private.key.file}")
    private String googleServicePrivateKeyFile;
    @Value ("${importer.clinical_attributes_worksheet}")
    private String clinicalAttributesWorksheet;

    private SpreadsheetService spreadsheetService;
    private ArrayList<ArrayList<String>> clinicalAttributesMatrix;

    private static Log LOG = LogFactory.getLog(GoogleSessionManager.class);

    public SpreadsheetService getService() {
	if (spreadsheetService == null) {
	    initService();
	}
	return spreadsheetService;
    }

    private void initService() {
        try {
	    HttpTransport httpTransport = new NetHttpTransport();
	    JacksonFactory jsonFactory = new JacksonFactory();
	    String [] SCOPESArray= {"https://spreadsheets.google.com/feeds", "https://docs.google.com/feeds"};
	    final List<String> SCOPES = Arrays.asList(SCOPESArray);
	    GoogleCredential credential = new GoogleCredential.Builder()
		.setTransport(httpTransport)
	        .setJsonFactory(jsonFactory)
		.setServiceAccountId(googleServiceEmail)
		.setServiceAccountScopes(SCOPES)
		.setServiceAccountPrivateKeyFromP12File(new File(googleServicePrivateKeyFile)).build();
	    spreadsheetService = new SpreadsheetService("data");
	    spreadsheetService.setOAuth2Credentials(credential);
	}
	catch (IOException | GeneralSecurityException e) {
	    e.printStackTrace();
	}
    }

    /**
     * Gets an array of RedcapAttributeMetadata.
     * @return RedcapAttributeMetadata[] array
     */
    public RedcapAttributeMetadata[] getRedcapMetadata() {
        // generate matrix representing each record in metadata worksheet
        if (clinicalAttributesMatrix == null) {
            clinicalAttributesMatrix = getWorksheetData(gdataSpreadsheet, clinicalAttributesWorksheet);
        }
        // initialize array to store all metadata bojects
        RedcapAttributeMetadata[] redcapAttributeMetadataList = getRedcapAttributeMetadataFromMatrix(clinicalAttributesMatrix);
        return redcapAttributeMetadataList; // if user wants all, we're done
    }

    /**
     * Constructs a collection of objects of the given classname from the given matrix.
     *
     * @param metadataMatrix ArrayList<ArrayList<String>>
     * @return RedcapAttributeMetadata[] array
     */
    private RedcapAttributeMetadata[] getRedcapAttributeMetadataFromMatrix(ArrayList<ArrayList<String>> metadataMatrix) {
        RedcapAttributeMetadata[] redcapAttributeMetadataList = new RedcapAttributeMetadata[metadataMatrix.size() - 1];
        // we start at one and subtract 1 from metadataMatrix size because row 0 is the column headers
        for (int row = 1; row < metadataMatrix.size(); row++) {
            ArrayList<String> record = metadataMatrix.get(row);
            RedcapAttributeMetadata redcapAttributeMetadata = new RedcapAttributeMetadata(record.get(0),
                record.get(1),
                record.get(2),
                record.get(3),
                record.get(4),
                record.get(5));
            redcapAttributeMetadataList[row - 1] = redcapAttributeMetadata;
        }
        return redcapAttributeMetadataList;
    }

    /**
     * Gets the spreadsheet.
     *
     * @param spreadsheetName String
     * @returns SpreadsheetEntry
     * @throws Exception
     */
    private SpreadsheetEntry getSpreadsheet(String spreadsheetName) throws Exception {
        FeedURLFactory factory = FeedURLFactory.getDefault();
        SpreadsheetFeed feed = null;
        // error happens here
        feed = spreadsheetService.getFeed(factory.getSpreadsheetsFeedUrl(), SpreadsheetFeed.class);
        for (SpreadsheetEntry entry : feed.getEntries()) {
            System.out.println(entry.getTitle().getPlainText());
            if (entry.getTitle().getPlainText().equals(spreadsheetName)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Gets the worksheet feed.
     *
     * @param spreadsheetName String
     * @param worksheetName String
     * @returns WorksheetFeed
     * @throws Exception
     */
    private WorksheetEntry getWorksheet(String spreadsheetName, String worksheetName) throws Exception {
        // first get the spreadsheet
        SpreadsheetEntry spreadsheet = getSpreadsheet(spreadsheetName);
        if (spreadsheet != null) {
            WorksheetFeed worksheetFeed = spreadsheetService.getFeed(spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
            for (WorksheetEntry worksheet : worksheetFeed.getEntries()) {
                if (worksheet.getTitle().getPlainText().equals(worksheetName)) {
                    return worksheet;
                }
            }
        }
        return null;
    }

    /**
     *
     * @param worksheetName
     * @param columnName
     * @return A List of String values from a specified column in a specified worksheet
     *
     */
    private List<String> getWorksheetDataByColumnName(String worksheetName, String columnName) {
        com.google.common.base.Preconditions.checkState(!Strings.isNullOrEmpty(this.gdataSpreadsheet),
                "The Google spreadsheet has not been defined.");
        com.google.common.base.Preconditions.checkArgument(!Strings.isNullOrEmpty(worksheetName),
                "A worksheet name is required");
        com.google.common.base.Preconditions.checkArgument(!Strings.isNullOrEmpty(columnName),
                "A worksheet column name is required");
        return null;
    }

    /**
     * Helper function to retrieve the given google worksheet data matrix. as a
     * list of string lists.
     *
     * @param spreadsheetName String
     * @param worksheetName String
     * @return ArrayList<ArrayList<String>>
     */
    private ArrayList<ArrayList<String>> getWorksheetData(String spreadsheetName, String worksheetName) {
        this.spreadsheetService = getService();
        ArrayList<ArrayList<String>> toReturn = new ArrayList<ArrayList<String>>();
        if (LOG.isInfoEnabled()) {
            LOG.info("getWorksheetData(): " + spreadsheetName + ", " + worksheetName);
        }
        try {
            //login();
            WorksheetEntry worksheet = getWorksheet(spreadsheetName, worksheetName);
            if (worksheet != null) {
                ListFeed feed = spreadsheetService.getFeed(worksheet.getListFeedUrl(), ListFeed.class);
                if (feed != null && feed.getEntries().size() > 0) {
                    boolean needHeaders = true;
                    for (ListEntry entry : feed.getEntries()) {
                        if (needHeaders) {
                            ArrayList<String> headers = new ArrayList<String>(entry.getCustomElements().getTags());
                            toReturn.add(headers);
                            needHeaders = false;
                        }
                        ArrayList<String> customElements = new ArrayList<String>();
                        for (String tag : toReturn.get(0)) {
                            String value = entry.getCustomElements().getValue(tag);
                            if (value == null) {
                                value = "";
                            }
                            customElements.add(value);
                        }
                        toReturn.add(customElements);
                    }
                } else {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Worksheet contains no entries!");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Problem connecting to " + spreadsheetName + ":" + worksheetName);
            throw new RuntimeException(e);
        }
        return toReturn;
    }
}

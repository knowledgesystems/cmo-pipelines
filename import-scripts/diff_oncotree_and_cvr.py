# Checks that the CVR Oncotree system is up to date
# with Oncotree release oncotree_candidate_release
# New codes are added to CVR with an API,
# removed/changed codes are reported.

# Author(s): Manda Wilson
# Date: February 13, 2019

import requests
from collections import defaultdict
import json

import sys
reload(sys)
sys.setdefaultencoding('utf8')

ONCOTREE_VERSION = "oncotree_candidate_release"
ONCOTREE_BASE_URI = "http://oncotree.mskcc.org/api/tumorTypes?version=%s"
ONCOTREE_URI = ONCOTREE_BASE_URI % (ONCOTREE_VERSION)
CVR_ONCOTREE_BASE_URI = "http://crater.mskcc.org:9010/"
CVR_ONCOTREE_GET_URI = CVR_ONCOTREE_BASE_URI + "get_tumor_types_CVR"
CVR_ONCOTREE_ADD_URI = CVR_ONCOTREE_BASE_URI + "set_tumor_types/"

redundant_cvr_codes = set([])

# ONCOTREE => CVR
# "name" => "tumor_type" and "tumor_type_name"
# "mainType" => "generic_tumor_type"
# "tissue" => "tissue_type"
# parent["mainType"] => "parent_tumor_type"
CVR_TO_ONCOTREE_LABELS = {
    "tumor_type" : "name",
    "tumor_type_name" : "name",
    "tissue_type" : "tissue",
    "generic_tumor_type" : "mainType"}

def add_code_to_cvr(oncotree_codes, oncotree_code):
    oncotree_code_data = {
        "code": oncotree_codes[oncotree_code]["code"],
        "maintype": oncotree_codes[oncotree_code]["mainType"],
        "name": oncotree_codes[oncotree_code]["name"],
        "tissue": oncotree_codes[oncotree_code]["tissue"]
    }
    parent_tumor_type = get_parent_tumor_type(oncotree_codes, oncotree_code)
    if parent_tumor_type:
        oncotree_code_data["parent"] = parent_tumor_type

    print oncotree_code_data
    response = requests.post(CVR_ONCOTREE_ADD_URI, data=json.dumps(oncotree_code_data))
    if response.status_code != 200:
        print >> sys.stderr, "ERROR: %d status code adding oncotree code '%s' to CVR with URI '%s'" % (response.status_code, oncotree_code, CVR_ONCOTREE_ADD_URI)
        sys.exit(1)

def get_parent_tumor_type(oncotree_codes, oncotree_code):
    if oncotree_codes[oncotree_code]["parent"] and oncotree_codes[oncotree_code]["parent"] in oncotree_codes:
        return oncotree_codes[oncotree_codes[oncotree_code]["parent"]]["mainType"]
    return None

def get_cvr_oncotree_codes():
    cvr_oncotree_codes = {}
    response = requests.get(CVR_ONCOTREE_GET_URI)
    if response.status_code != 200:
        print >> sys.stderr, "ERROR: %d status code getting CVR oncotree codes with URI '%s'" % (response.status_code, CVR_ONCOTREE_GET_URI)
        sys.exit(1)

    if response.json():
        for oncotree_item in response.json()["result"]:
            oncotree_code = oncotree_item["code"]
            # check for duplicate codes
            if oncotree_code in cvr_oncotree_codes:
                redundant_cvr_codes.add(oncotree_code)

            cvr_oncotree_codes[oncotree_code] = {
                "generic_tumor_type" : oncotree_item["generic_tumor_type"],
                "parent_tumor_type" : oncotree_item["parent_tumor_type"],
                "tissue_type" : oncotree_item["tissue_type"],
                "tumor_type" : oncotree_item["tumor_type"],
                "tumor_type_cv_id" : oncotree_item["tumor_type_cv_id"],
                "tumor_type_name" : oncotree_item["tumor_type_name"]}
    else:
        print >> sys.stderr, "ERROR: no oncotree codes found in JSON response to '%s'" % (ONCOTREE_URI)
    return cvr_oncotree_codes

def get_oncotree_codes():
    oncotree_codes = {}
    response = requests.get(ONCOTREE_URI)
    if response.status_code != 200:
        print >> sys.stderr, "ERROR: %d status code getting oncotree codes with URI '%s'" % (response.status_code, ONCOTREE_URI)
        sys.exit(1)

    if response.json():
        for oncotree_item in response.json():
            oncotree_code = oncotree_item["code"]
            # skip nodes at leavel 0 and 1, they are not currently included in CVR
            if oncotree_item["level"] != 0 and oncotree_item["level"] != 1:
                # note we are assuming there are no duplicate codes
                oncotree_codes[oncotree_code] = {
                    "code" : oncotree_item["code"],
                    "name" : oncotree_item["name"],
                    "mainType" : oncotree_item["mainType"],
                    "tissue" : oncotree_item["tissue"],
                    "parent" : oncotree_item["parent"],
                    "level" : oncotree_item["level"]}
    else:
        print >> sys.stderr, "ERROR: no oncotree codes found in JSON response to '%s'" % (ONCOTREE_URI)

    return oncotree_codes

def add_codes_and_report_diff(oncotree_codes, cvr_oncotree_codes):
    oncotree_code_set = set(oncotree_codes.keys())
    cvr_oncotree_code_set = set(cvr_oncotree_codes.keys())

    # find new codes
    new_codes = oncotree_code_set - cvr_oncotree_code_set
    for new_code in new_codes:
        add_code_to_cvr(oncotree_codes, new_code)

    if new_codes:
        print "Added to CVR:"
        print "\t%s\n" % ("\n\t".join(sorted(new_codes)))

    # find deleted nodes
    deleted_codes = cvr_oncotree_code_set - oncotree_code_set
    if deleted_codes:
        print "In CVR but not in the latest Oncotree:"
        print "\t%s\n" % ("\n\t".join(sorted(deleted_codes)))

    # duplicated codes in CVR
    if redundant_cvr_codes:
        print "Redundant CVR codes:"
        print "\t%s\n" % ("\n\t".join(sorted(redundant_cvr_codes)))

    # find changes
    codes_in_both = oncotree_code_set & cvr_oncotree_code_set
    changes = defaultdict(dict) # oncotree code points to dict where label points to [cvr value, oncotree value]
    for oncotree_code in codes_in_both:
        for cvr_label, oncotree_label in CVR_TO_ONCOTREE_LABELS.iteritems():
            if cvr_oncotree_codes[oncotree_code][cvr_label] != oncotree_codes[oncotree_code][oncotree_label]:
                changes[oncotree_code][cvr_label] = [
                    cvr_oncotree_codes[oncotree_code][cvr_label],
                    oncotree_codes[oncotree_code][oncotree_label]]

        oncotree_parent_tumor_type = get_parent_tumor_type(oncotree_codes, oncotree_code)
        if oncotree_parent_tumor_type != cvr_oncotree_codes[oncotree_code]["parent_tumor_type"]:
                changes[oncotree_code]["parent_tumor_type"] = [
                    cvr_oncotree_codes[oncotree_code]["parent_tumor_type"],
                    oncotree_parent_tumor_type]

    if changes:
        print "Nodes with attributes that differ between CVR and the latest Oncotree:"
        print "%s\t%s\t%s\t%s\t%s" % ("OncotreeCode", "CvrLabel", "OncotreeLabel", "CvrValue", "OncotreeValue")
        for oncotree_code in sorted(changes.keys()):
            for cvr_label in changes[oncotree_code]:
                oncotree_label = "[parent]mainType" if cvr_label == "parent_tumor_type" else CVR_TO_ONCOTREE_LABELS[cvr_label]
                print "%s\t%s\t%s\t%s\t%s" % (oncotree_code, cvr_label, oncotree_label, changes[oncotree_code][cvr_label][0], changes[oncotree_code][cvr_label][1])

def main():
    oncotree_codes = get_oncotree_codes()
    cvr_oncotree_codes = get_cvr_oncotree_codes()

    add_codes_and_report_diff(oncotree_codes, cvr_oncotree_codes)

if __name__ == '__main__':
    main()

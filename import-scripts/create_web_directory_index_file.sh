#!/usr/bin/env bash

# This script takes a source directory and a base URL as input and will create an index.html file containing a link to each filename to allow web-based download

SOURCE_DIR=$1
TMP_DIR=$2
BASE_URL=$3
TITLE_STRING=$4

if [ -z $SOURCE_DIR ] || [ -z $TMP_DIR ] || [ -z $BASE_URL ] || [ -z $TITLE_STRING ] ; then
    echo "usage : create_web_directory_index_file.sh '<SOURCE_DIR>' '<TMP_DIR>' '<BASE_URL>' '<TITLE_STRING>'" 1>&2
    exit 1;
fi

INDEX_FILENAME="$TMP_DIR/index.html"
FILELIST_FILENAME="$TMP_DIR/filenames.txt"

function create_tmp_dir_if_necessary() {
    if [[ ! -d "$TMP_DIR" ]] ; then
        rm -rf "TMP_DIR"
        mkdir -p "TMP_DIR"
    fi
}

function output_document_head() {
    echo "<head>" >> $INDEX_FILENAME
    echo "<title>$TITLE_STRING</title>" >> $INDEX_FILENAME
    echo "</head>" >> $INDEX_FILENAME
}

function output_data_usage_agreement() {
    echo '<div style="width:700;border-style:double;border-width:3px;border-color:DodgerBlue">' >> $INDEX_FILENAME
    echo '<div style="background-color:Ivory;font-family:Helvetica,sans-serif;padding-top:4px;padding-right:4px;padding-left:4px;padding-bottom:4px">' >> $INDEX_FILENAME
    echo '<div style="font-size:20;color:DodgerBlue;margin-bottom:5px">MSK Clinical Sequencing Cohort (MSKCC)</div>' >> $INDEX_FILENAME
    echo '<div style="font-size:14;color:DarkSlateGray">' >> $INDEX_FILENAME
    echo '<span>Please follow the <a href="http://cmo.mskcc.org/cmo/initiatives/msk-impact/">publication guidelines</a> when using these data in abstracts or journal articles. Manuscripts involving pan-cancer and other large analyses will undergo a biostatistical review prior to submission. If you would like assistance identifying a collaborator in Biostatistics, please contact <a href="mailto:Mithat Gonen <gonenm@mskcc.org>"</a>Mithat Gonen (gonenm@mskcc.org)</a>. For questions regarding the results shown here, please contact <a href="mailto:Mike Berger <bergerm1@mskcc.org>">Mike Berger (bergerm1@mskcc.org)</a>, <a href="mailto:Ahmet Zehir <zehira@mskcc.org>">Ahmet Zehir (zehira@mskcc.org)</a>, or <a href="mailto:Nikolaus Schultz <schultzn@mskcc.org>">Nikolaus Schultz (schultzn@mskcc.org)</a>.' >> $INDEX_FILENAME
    echo '<div style="font-size:15;color:OrangeRed;margin-top:7px">' >> $INDEX_FILENAME
    echo '<span>These data are available to MSK investigators only and are not to be published or shared with anyone outside of MSK without permission.</span>' >> $INDEX_FILENAME
    echo '</span>' >> $INDEX_FILENAME
    echo '</div>' >> $INDEX_FILENAME
    echo '</div>' >> $INDEX_FILENAME
    echo '</div>' >> $INDEX_FILENAME
    echo '</div>' >> $INDEX_FILENAME
    echo '</div>' >> $INDEX_FILENAME
}

function output_download_section() {
    echo "<h3>Available Downloads</h3>" >> $INDEX_FILENAME
    echo "<p>Control-click links and choose \"Save Link As...\"" >> $INDEX_FILENAME
    echo "<ul>" >> $INDEX_FILENAME
    ls $SOURCE_DIR > $FILELIST_FILENAME
    while read filename ; do
        if [ $filename != "index.html" ] ; then
            echo "<li><a href=\"$BASE_URL/$filename\">$filename</a>" >> $INDEX_FILENAME
        fi
    done < $FILELIST_FILENAME
    echo "</ul>" >> $INDEX_FILENAME
}

function output_timestamp() {
    timestamp=$(date '+%Y_%m_%d %H:%M %Z')
    echo "<p style=\"color:Teal;font-size:7px;\">(last updated: $timestamp)" >> $INDEX_FILENAME
}

function output_document_body() {
    echo "<body>" >> $INDEX_FILENAME
    echo "<h2>OncoKB Annotated MSK-IMPACT</h2>" >> $INDEX_FILENAME
    output_data_usage_agreement
    output_download_section
    output_timestamp
    echo "</body>" >> $INDEX_FILENAME
}

function make_directory_file() {
    echo "<html>" > $INDEX_FILENAME
    output_document_head
    output_document_body
    echo "</html>" >> $INDEX_FILENAME
}

create_tmp_dir_if_necessary
make_directory_file
rm -f $FILELIST_FILENAME

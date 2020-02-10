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

function make_directory_file() {
    echo "<html>" > $INDEX_FILENAME
    echo "<head>" >> $INDEX_FILENAME
    echo "<title>$TITLE_STRING</title>" >> $INDEX_FILENAME
    echo "</head>" >> $INDEX_FILENAME
    echo "<body>" >> $INDEX_FILENAME
    echo "<h2>Available Downloads</h2>" >> $INDEX_FILENAME
    echo "<p>Control-click links and choose \"Save Link As...\"" >> $INDEX_FILENAME
    echo "<ul>" >> $INDEX_FILENAME
    ls $SOURCE_DIR > $FILELIST_FILENAME
    while read filename ; do
        if [ $filename != "index.html" ] ; then
            echo "<li><a href=\"$BASE_URL/$filename\">$filename</a>" >> $INDEX_FILENAME
        fi
    done < $FILELIST_FILENAME
    echo "</ul>" >> $INDEX_FILENAME
    echo "</body>" >> $INDEX_FILENAME
    echo "</html>" >> $INDEX_FILENAME
}

create_tmp_dir_if_necessary
make_directory_file
rm -f $FILELIST_FILENAME

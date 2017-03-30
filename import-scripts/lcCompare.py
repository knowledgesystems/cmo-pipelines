#! /usr/bin/env python

# ---------------------------------------------------------------
# Script to check if any files were wiped out during import
# of CRV/Darwin data. If an error occured, do not want to push 
# these erroneous/incomplete files to the repository.
# Checks the output from a bash script which calculates the 
# number of lines in each file before CRV/Darwin code runs,
# and compares it to after the script run
# ---------------------------------------------------------------

# ---------------------------------------------------------------
# imports
import os
import sys
import getopt

# ---------------------------------------------------------------
# globals
ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

# ---------------------------------------------------------------
# functions

# ---------------------------------------------------------------
# create dictionary of linecounts pre java processing
def processlinecounts(linecountsFile):
	linecounts = {}
	for line in linecountsFile:
		if line.strip() != '':
			line = line.strip()
			splitline = line.split(' ')
			linecounts[splitline[1]] = int(splitline[0])

	return linecounts

def processRepoFiles(repoFilenames, linecounts, output_filename):
    ioerror = False
    to_return = 0
    try:
        output_file = open(output_filename, 'w')
    except IOError:
        print "IOError"
        ioerror = True
    for filename in repoFilenames:
        if 'data_clinical' in filename  or 'data_timeline' in filename:
            repoFile = open(filename,'rU')

            # quick lambda for file line counts
            filelen = lambda f: sum(1 for line in f) 

            if filename in linecounts:
                lines_in_file = filelen(repoFile)
                if int(lines_in_file) < int(linecounts[filename]):
                    if not ioerror:
                        output_file.write('Filename: ' + os.path.basename(filename) + '\n' + \
                            'Lines before run: ' + str(linecounts[filename]) + '\n' + \
                            'Lines after run: ' + str(lines_in_file)+ '\n')
                    to_return = 1
    return to_return

# ---------------------------------------------------------------
# displays usage of program
def usage():
	print >> OUTPUT_FILE, 'lcCompare.py --line-counts <path/to/linecounts/file> --repo-path <path/to/repository/> --fail-output <path/to/desired/output>'

# ---------------------------------------------------------------
# the main
def main():
    # parse command line
    try:
        opts,args = getopt.getopt(sys.argv[1:],'',['line-counts=','repo-path=', 'fail-output='])
    except getopt.error,msg:
        print >> ERROR_FILE,msg
        usage()
        sys.exit(2)

    linecountsFilename = ''
    repoPath = ''
    output_filename = ''

    # process options
    for o, a in opts:
        if o == '--line-counts':
            linecountsFilename = a
        if o == '--repo-path':
            repoPath = a
        if o == '--fail-output':
            output_filename = a
	
    if linecountsFilename == '' or repoPath == '' or output_filename == '':
        usage()
        sys.exit(2)

    # check existence of file
    if not os.path.exists(os.path.abspath(linecountsFilename)):
        print >> ERROR_FILE, 'linecounts file cannot be read: ' + linecountsFilename
        sys.exit(2)

    if not os.path.isdir(os.path.abspath(repoPath)):
        print >> ERROR_FILE, 'Repository cannot be found: ' + repoPath
        sys.exit(2)

    exitcode = 0

    linecountsFile = open(linecountsFilename,'rU')
    repoFilenames = [os.path.join(dp,f) for dp,dn,fn in os.walk(repoPath) for f in fn]

    linecounts = processlinecounts(linecountsFile)
    exitcode = processRepoFiles(repoFilenames, linecounts, output_filename)

    sys.exit(exitcode)

# ---------------------------------------------------------------
# do a main
if __name__ == '__main__':
	main()

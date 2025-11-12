#!/usr/bin/python

""" remove-duplicate-maf-variants.py
Script to remove duplicate maf records based on the 8 key columns.
Calculates VAF for each record and picks the record with high VAF
Formula for VAF = t_alt_count / (t_ref_count + t_alt_count)
"""

import sys
import optparse

ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

KEY_COLUMNS_INDEX = []
KEY_COLUMNS = ['Entrez_Gene_Id','Chromosome','Start_Position','End_Position','Variant_Classification','Tumor_Seq_Allele2','Tumor_Sample_Barcode','HGVSp_Short']
MAF_DATA = {}

def remove_duplicate_variants(out_filename, comments, header, t_refc_index, t_altc_index):
	outfile = []
	outfile.append(comments)
	outfile.append(header)
	for key in MAF_DATA:
		if len(MAF_DATA[key]) > 1:
			vaf_ind = 0
			vaf_value = 0
			for val in MAF_DATA[key]:
				#calculate VAF for each duplicate record.
				columns = val.rstrip('\n').split('\t')
				try:
					VAF = int(columns[t_altc_index])/(int(columns[t_altc_index])+int(columns[t_refc_index]))
					if VAF > vaf_value:
						vaf_value = VAF
						vaf_ind = MAF_DATA[key].index(val)
						outfile.append(MAF_DATA[key][vaf_ind])
				except:
					print >> ERROR_FILE, 'ERROR: VAF cannot be calculated for the variant : ' + key
					print >> ERROR_FILE, 'The t_ref_count is: '+ columns[t_refc_index]+ ' and t_alt_count is: '+ columns[t_altc_index]
					outfile.append(val)
		else:
			outfile.append(MAF_DATA[key][0])
			
	datafile = open(out_filename, 'w')
	for line in outfile:
		datafile.write(line)
	datafile.close()
	print >> OUTPUT_FILE, 'MAF file with duplicate variants removed is written to: ' + out_filename +'\n'
		

def main():
	# get command line arguments
	parser = optparse.OptionParser()
	parser.add_option('-i', '--input-maf-file', action = 'store', dest = 'input_maf_file')
	parser.add_option('-o', '--output-maf-file', action = 'store', dest = 'output_maf_file')

	(options, args) = parser.parse_args()
	maf_filename = options.input_maf_file
	out_filename = options.output_maf_file

	comments = ""
	header = ""
	
	with open(maf_filename,'r') as maf_file:
		for line in maf_file:
			if line.startswith('#'):
				comments += line
			elif line.startswith('Hugo_Symbol'):
				header += line
				header_cols = line.rstrip('\n').split('\t')
				#get the positions of the 8 key maf columns
				for value in KEY_COLUMNS:
					KEY_COLUMNS_INDEX.append(header_cols.index(value))
				t_refc_index = header_cols.index('t_ref_count')
				t_altc_index = header_cols.index('t_alt_count')
			else:
				reference_key = ""
				data = line.rstrip('\n').split('\t')
				for index in KEY_COLUMNS_INDEX:
					reference_key += data[index]+'\t'
				reference_key = reference_key.rstrip('\t')
				if reference_key not in MAF_DATA:
					MAF_DATA[reference_key] = [line]
				else:
					MAF_DATA[reference_key].append(line)
	
	remove_duplicate_variants(out_filename, comments, header, t_refc_index, t_altc_index)

if __name__ == '__main__':
	main()

#!/usr/bin/env python3

""" merge-duplicate-maf-variants.py
Script to merge duplicate maf records based on the 8 key columns.
Within each group, apply ImportExtendedMutationData.mergeMutationData preferences
and merge sequencing centers. Read counts do not affect selection.
"""

import sys
import optparse
import re

ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

KEY_COLUMNS_INDEX = []
KEY_COLUMNS = ['Entrez_Gene_Id','Chromosome','Start_Position','End_Position','Variant_Classification','Tumor_Seq_Allele2','Tumor_Sample_Barcode','HGVSp_Short']
MAF_DATA = {}

def merge_duplicate_records(records, header_columns):
	"""Match Java mergeMutationData within the existing eight-column groups."""
	indexes = {name: i for i, name in enumerate(header_columns)}
	def value(row, name):
		return row[indexes[name]] if name in indexes else ''
	selected = records[0].rstrip('\n').split('\t')
	for record in records[1:]:
		candidate = record.rstrip('\n').split('\t')
		choose_candidate = False
		if value(selected, 'Matched_Norm_Sample_Barcode').lower() != value(candidate, 'Matched_Norm_Sample_Barcode').lower():
			choose_candidate = re.fullmatch(r'TCGA-..-....-10.*', value(candidate, 'Matched_Norm_Sample_Barcode')) is not None
		elif value(selected, 'Validation_Status').lower() != value(candidate, 'Validation_Status').lower():
			choose_candidate = value(candidate, 'Validation_Status').lower() in ('valid', 'validated')
		elif value(selected, 'Mutation_Status').lower() != value(candidate, 'Mutation_Status').lower():
			status = value(candidate, 'Mutation_Status').lower()
			choose_candidate = status == 'germline' or (status == 'somatic' and value(selected, 'Mutation_Status').lower() != 'germline')
		merged = candidate if choose_candidate else selected
		if 'Center' in indexes:
			centers = set(value(selected, 'Center').split(';'))
			additional = set(value(candidate, 'Center').split(';')) - centers
			if additional:
				centers.update(additional)
				if len(centers) > 1:
					centers.discard('NA')
				merged[indexes['Center']] = ';'.join(sorted(centers))
		selected = merged
	return '\t'.join(selected) + '\n'


def merge_duplicate_variants(out_filename, comments, header):
	header_columns = header.rstrip('\n').split('\t')
	with open(out_filename, 'w') as datafile:
		datafile.write(comments)
		datafile.write(header)
		for records in MAF_DATA.values():
			datafile.write(records[0] if len(records) == 1 else merge_duplicate_records(records, header_columns))
	print('MAF file with duplicate variants merged is written to: ' + out_filename +'\n', file=OUTPUT_FILE)


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
			elif not header:
				header += line
				header_cols = line.rstrip('\n').split('\t')
				#get the positions of the 8 key maf columns
				for value in KEY_COLUMNS:
					KEY_COLUMNS_INDEX.append(header_cols.index(value))
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

	merge_duplicate_variants(out_filename, comments, header)

if __name__ == '__main__':
	main()

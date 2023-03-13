import sys
import os
import optparse

ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

HEADER_KEYWORDS = ['hugo_symbol','entrez_gene_id']
GENE_MERGE_LIST = {'CDKN2Ap16INK4A': 'CDKN2A',
'CDKN2Ap14ARF': 'CDKN2A',
'MLL': 'KMT2A',
'MLL2': 'KMT2D',
'MLL3': 'KMT2C',
'MLL4': 'KMT2B',
'FAM123B': 'AMER1',
'MYCL1': 'MYCL'}

def get_file_header(filename):
	""" Returns the file header. """
	data_file = open(filename, 'rU')
	filedata = [x for x in data_file.readlines() if not x.startswith('#')]
	header = map(str.strip, filedata[0].split('\t'))
	data_file.close()
	return header

def merge_duplicate_cna_records(data):
	for gene in data:
		if len(data[gene]) > 1:
			merge_status = 0
			cna_data = map(lambda v: set(v), zip(*data[gene]))
			merged_cna_data = []
			for value in cna_data:
				if len(value) > 1:
					value = value - set(['NA'])
					if len(value) > 1: value = value - set([''])
					if len(value) > 1: value = value - set(['0'])
					if len(value) > 1:
						if len(value) == 2 and '-1.5' in value and '-2' in value: value.remove('-1.5')
						else: merge_status = 1; break
					merged_cna_data.append(map(str,value))
				else:
					merged_cna_data.append(map(str,value))
			if merge_status == 1:
				print >> ERROR_FILE, "The copy number values for gene", gene, "cannot be merged"
			else:
				merged_cna_data = [value[0] for value in merged_cna_data]
				data[gene] = [merged_cna_data]
	return(data)			

def write_merged_cna_data(data,header,out_cna_filepath):
	unmerged_data = ""
	merged_data = ""
	
	for gene_symbol in data:
		if len(data[gene_symbol]) > 1:
			for record in data[gene_symbol]:
				unmerged_data += gene_symbol+'\t'+'\t'.join(record)+'\n'
		else:
			merged_data += gene_symbol+'\t'+'\t'.join(data[gene_symbol][0])+'\n'
	
	if unmerged_data != "":
		unmerged_file = open(out_cna_filepath+'data_CNA_unmerged.txt','w')
		unmerged_file.write('\t'.join(header)+'\n')
		unmerged_file.write(unmerged_data)
		print >> OUTPUT_FILE, "The unmerged CNA records are written to :", out_cna_filepath+'data_CNA_unmerged.txt'
	if merged_data != "":
		merged_file = open(out_cna_filepath+'data_CNA_merged.txt','w')
		merged_file.write('\t'.join(header)+'\n')
		merged_file.write(merged_data)
		print >> OUTPUT_FILE, "The merged CNA records are written to :", out_cna_filepath+'data_CNA_merged.txt'

def main():
	# get command line arguments
	parser = optparse.OptionParser()
	parser.add_option('-i', '--input-cnafile', action = 'store', dest = 'cnafile')
	parser.add_option('-o', '--output-cna-filepath', action = 'store', dest = 'out_cna_filepath')

	(options, args) = parser.parse_args()
	cna_filename = options.cnafile
	out_cna_filepath = options.out_cna_filepath
	
	header = get_file_header(cna_filename)

	# load data from clinical_filename and write data to output directory
	data_file = open(cna_filename, 'rU')
	data_reader = [line for line in data_file.readlines() if not line.startswith('#')][1:]
	
	COPY_NUMBER_DATA = {}
	for line in data_reader:
		line = line.strip('\n').split('\t')
		if line[0] in GENE_MERGE_LIST: line[0] = GENE_MERGE_LIST[line[0]]
		if line[0] not in COPY_NUMBER_DATA: COPY_NUMBER_DATA[line[0]] = [line[1:]]
		else: COPY_NUMBER_DATA[line[0]].append(line[1:])
	
	data = merge_duplicate_cna_records(COPY_NUMBER_DATA)
	
	write_merged_cna_data(data,header,out_cna_filepath)
	
if __name__ == '__main__':
	main()
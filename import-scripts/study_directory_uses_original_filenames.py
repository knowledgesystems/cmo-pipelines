#!/usr/bin/env python3

import argparse
import os
import sys

"""
DATATYPE_SHEET_DOWNLOAD_TAB_SEPARATED is hardcoded data - an exact copy of the contents of the current datatypes sheet from the portal_importer_configuration spreadsheet.
When updates are made to the datatypes sheet, this can be updated by using the "File - Download - Tab Separated Values" option in the google sheets interface.
- make sure to copy the exact contents of that download (maintaining the literal TAB characters) into the lines below, between the assignment line and the final triple-quote line
"""
DATATYPE_SHEET_DOWNLOAD_TAB_SEPARATED = """
datatype	DOWNLOAD	PROCESS	DEPENDENCIES	TCGA_DOWNLOAD_ARCHIVE	STAGING_FILENAME	CONVERTER_CLASSNAME	IMPORTER_CLASSNAME	REQUIRES_METAFILE	META_FILENAME	META_STABLE_ID	META_GENETIC_ALTERATION_TYPE	META_DATATYPE	META_SHOW_PROFILE_IN_ANALYSIS_TAB	META_PROFILE_NAME	META_PROFILE_DESCRIPTION	META_REFERENCE_GENOME_ID
bcr-clinical	TRUE	TRUE		nationwidechildrens.org_<TUMOR_TYPE>.bio.Level_2.0.<REVISION>.0:nationwidechildrens.org_clinical_patient_<TUMOR_TYPE>.txt;nationwidechildrens.org_<TUMOR_TYPE>.bio.Level_2.0.<REVISION>.0:nationwidechildrens.org_clinical_follow_up_v<FOLLOWUP_VERSION>_<TUMOR_TYPE>.txt;nationwidechildrens.org_<TUMOR_TYPE>.bio.Level_2.0.<REVISION>.0:nationwidechildrens.org_clinical_nte_<TUMOR_TYPE>.txt;nationwidechildrens.org_<TUMOR_TYPE>.bio.Level_2.0.<REVISION>.0:nationwidechildrens.org_clinical_follow_up_v<FOLLOWUP_VERSION>_nte_<TUMOR_TYPE>.txt;nationwidechildrens.org_<TUMOR_TYPE>.bio.Level_2.0.<REVISION>.0:nationwidechildrens.org_biospecimen_sample_<TUMOR_TYPE>.txt;nationwidechildrens.org_:nationwidechildrens.org_	data_bcr_clinical_data.txt 	org.mskcc.cbio.importer.converter.internal.ClinicalDataConverterImpl	org.mskcc.cbio.portal.scripts.ImportClinicalData	TRUE	meta_bcr_clinical.txt	bcr_clinical	CLINICAL	PATIENT_ATTRIBUTES				
bcr-clinical-other	FALSE	FALSE			data_bcr_clinical_data_*.txt 		org.mskcc.cbio.portal.scripts.ImportClinicalData	TRUE	meta_bcr_clinical_other.txt	bcr_clinical_other	CLINICAL	CLINICAL	FALSE	Clinical Data	Clinical Data	
clinical	FALSE	FALSE			data_clinical.txt		org.mskcc.cbio.portal.scripts.ImportClinicalData	TRUE	meta_clinical.txt	clinical	CLINICAL	CLINICAL	FALSE	Clinical Data	Clinical Data	
clinical-sample	FALSE	FALSE			data_clinical_sample.txt		org.mskcc.cbio.portal.scripts.ImportClinicalData	TRUE	meta_clinical_sample.txt	clinical_sample	CLINICAL	SAMPLE_ATTRIBUTES	FALSE	Clinical Data	Clinical Data.	
clinical-patient	FALSE	FALSE			data_clinical_patient.txt		org.mskcc.cbio.portal.scripts.ImportClinicalData	TRUE	meta_clinical_patient.txt	clinical_patient	CLINICAL	PATIENT_ATTRIBUTES	FALSE	Clinical Data	Clinical Data.	
clinical-supp	FALSE	FALSE			data_clinical_supp.txt 		org.mskcc.cbio.portal.scripts.ImportClinicalData	TRUE	meta_clinical.txt			CLINICAL				
clinical-supp-other	FALSE	FALSE			data_clinical_supp_*.txt 		org.mskcc.cbio.portal.scripts.ImportClinicalData	TRUE	meta_clinical.txt			CLINICAL				
clinical-caises	FALSE	FALSE			data_clinical_caises.xml		org.mskcc.cbio.portal.scripts.ImportCaisesClinicalXML	FALSE	meta_clinical_caises.txt			CLINICAL				
time-line-data	FALSE	FALSE			data_timeline.txt		org.mskcc.cbio.portal.scripts.ImportTimelineData	TRUE	meta_timeline.txt			CLINICAL				
time-line-data-other	FALSE	FALSE			data_timeline_*.txt		org.mskcc.cbio.portal.scripts.ImportTimelineData	TRUE	meta_timeline.txt			CLINICAL				
rppa	TRUE	TRUE		RPPA_AnnotateWithGene.Level_3:<TUMOR_TYPE>.rppa.txt;	data_rppa.txt	org.mskcc.cbio.importer.converter.internal.PassThroughConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_rppa.txt	rppa	PROTEIN_LEVEL	LOG2-VALUE	FALSE	Protein expression (RPPA)	Protein expression measured by reverse-phase protein array	
rppa-zscores	TRUE	TRUE	rppa	RPPA_AnnotateWithGene.Level_3:<TUMOR_TYPE>.rppa.txt;	data_rppa_Zscores.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_rppa_Zscores.txt	rppa_Zscores	PROTEIN_LEVEL	Z-SCORE	TRUE	Protein expression z-scores (RPPA)	Protein expression, measured by reverse-phase protein array, z-scores	
protein-quantification	FALSE	TRUE			data_protein_quantification.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_protein_quantification.txt	protein_quantification	PROTEIN_LEVEL	CONTINUOUS	FALSE	Protein levels (mass spectrometry)	Protein levels (mass spectrometry)	
protein-quantification-zscores	FALSE	TRUE	protein-quantification		data_protein_quantification_Zscores.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_protein_quantification_Zscores.txt	protein_quantification_zscores	PROTEIN_LEVEL	Z-SCORE	TRUE	Protein levels z-scores (mass spectrometry) 	Protein levels z-scores (mass spectrometry)	
cna-foundation	FALSE	TRUE			data_CNA_foundation.txt	org.mskcc.cbio.importer.converter.internal.CNAConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_CNA_foundation.txt	cna	COPY_NUMBER_ALTERATION	DISCRETE	TRUE	Putative copy-number alterations from Foundation	Putative copy-number calls on <NUM_CASES> cases determined by Foundation Medicine.  Values: -2 = homozygous deletion; 2 = high level amplification.	
cna-rae	FALSE	TRUE			data_CNA_RAE.txt	org.mskcc.cbio.importer.converter.internal.CNAConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_CNA_RAE.txt	cna_rae	COPY_NUMBER_ALTERATION	DISCRETE	TRUE	Putative copy-number alterations from RAE	Putative copy-number calls on <NUM_CASES> cases determined using the RAE algorithm by Barry Taylor @MSKCC. Values: -2 = homozygous deletion; -1 = hemizygous deletion; 0 = neutral / no change; 1 = gain; 2 = high level amplification.        	
cna-consensus	FALSE	TRUE			data_CNA_consensus.txt	org.mskcc.cbio.importer.converter.internal.CNAConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_CNA_consensus.txt	cna_consensus	COPY_NUMBER_ALTERATION	DISCRETE	TRUE	Putative copy-number alterations (Consensus)       	Putative copy-number calls for genes which are the consensus of a combination of the Agilent 244k and Affymetrix SNP6 platforms and three different methods (RAE, GISTIC, GTS).        	
cna-gistic	TRUE	TRUE		CopyNumber_Gistic2.Level_4:all_thresholded.by_genes.txt;	data_CNA.txt	org.mskcc.cbio.importer.converter.internal.CNAConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_CNA.txt	gistic	COPY_NUMBER_ALTERATION	DISCRETE	TRUE	Putative copy-number alterations from GISTIC	Putative copy-number calls on <NUM_CASES> cases determined using GISTIC 2.0. Values: -2 = homozygous deletion; -1 = hemizygous deletion; 0 = neutral / no change; 1 = gain; 2 = high level amplification.	
affymetrix-gene-expression	TRUE	TRUE		Merge_transcriptome__ht_hg_u133a__broad_mit_edu__Level_3__gene_rma__data.Level_3:<TUMOR_TYPE>.transcriptome__ht_hg_u133a__broad_mit_edu__Level_3__gene_rma__data.data.txt	data_expression.txt	org.mskcc.cbio.importer.converter.internal.MRNAMedianConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_expression.txt	mrna_U133	MRNA_EXPRESSION	CONTINUOUS	FALSE	mRNA expression (U133 microarray only)	mRNA expression data from the Affymetrix U133 microarray.	
gene-expression-merged	TRUE	TRUE			data_expression_merged.txt	org.mskcc.cbio.importer.converter.internal.MRNAMedianConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_expression_merged.txt	mrna_U133	MRNA_EXPRESSION	CONTINUOUS	FALSE	mRNA expression (U133 microarray only)	mRNA expression data from the Affymetrix U133 microarray.	
affymetrix-gene-expression-zscores	TRUE	TRUE	cna-gistic:affymetrix-gene-expression	CopyNumber_Gistic2.Level_4:all_thresholded.by_genes.txt;Merge_transcriptome__ht_hg_u133a__broad_mit_edu__Level_3__gene_rma__data.Level_3:<TUMOR_TYPE>.transcriptome__ht_hg_u133a__broad_mit_edu__Level_3__gene_rma__data.data.txt	data_expression_Zscores.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_expression_Zscores.txt	mrna_U133_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores (U133 microarray only)	mRNA z-scores  (U133 microarray only) compared to the expression distribution of each gene tumors that are diploid for this gene.	
gene-expression-merged-zscores	TRUE	TRUE	cna-gistic:gene-expression-merged	CopyNumber_Gistic2.Level_4:all_thresholded.by_genes.txt;Merge_transcriptome__ht_hg_u133a__broad_mit_edu__Level_3__gene_rma__data.Level_3:<TUMOR_TYPE>.transcriptome__ht_hg_u133a__broad_mit_edu__Level_3__gene_rma__data.data.txt	data_expression_merged_Zscores.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_expression_merged_Zscores.txt	mrna_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores	mRNA z-scores compared to the expression distribution of each gene tumors that are diploid for this gene.	
rnaseq-gene-expression	TRUE	TRUE		Merge_rnaseq__illuminaga_rnaseq__bcgsc_ca__Level_3__gene_expression__data.Level_3:<TUMOR_TYPE>.rnaseq__illuminaga_rnaseq__bcgsc_ca__Level_3__gene_expression__data.data.txt;	data_RNA_Seq_expression_median.txt	org.mskcc.cbio.importer.converter.internal.RNASEQMRNAMedianConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_expression_median.txt	rna_seq_mrna	MRNA_EXPRESSION	CONTINUOUS	FALSE	mRNA expression (RNA Seq RPKM)	Expression levels for <NUM_GENES> genes in <NUM_CASES> <TUMOR_TYPE> cases (RNA Seq RPKM).	
rnaseq-gene-expression-zscores	TRUE	TRUE	cna-gistic:rnaseq-gene-expression	CopyNumber_Gistic2.Level_4:all_thresholded.by_genes.txt;Merge_rnaseq__illuminaga_rnaseq__bcgsc_ca__Level_3__gene_expression__data.Level_3:<TUMOR_TYPE>.rnaseq__illuminaga_rnaseq__bcgsc_ca__Level_3__gene_expression__data.data.txt;	data_RNA_Seq_mRNA_median_Zscores.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_mRNA_median_Zscores.txt	rna_seq_mrna_median_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores (RNA Seq RPKM)	mRNA z-scores (RNA Seq RPKM) compared to the expression distribution of each gene tumors that are diploid for this gene.	
agilent-gene-expression	TRUE	TRUE		Merge_transcriptome__agilentg4502a_07_2__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.Level_3:<TUMOR_TYPE>.transcriptome__agilentg4502a_07_2__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.data.txt;Merge_transcriptome__agilentg4502a_07_3__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.Level_3:<TUMOR_TYPE>.transcriptome__agilentg4502a_07_3__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.data.txt;	data_expression_median.txt	org.mskcc.cbio.importer.converter.internal.MRNAMedianConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_expression_median.txt	mrna	MRNA_EXPRESSION	CONTINUOUS	FALSE	mRNA expression (microarray)	Expression levels for <NUM_GENES> genes in <NUM_CASES> <TUMOR_TYPE> cases (Agilent microarray).	
agilent-gene-expression-zscores	TRUE	TRUE	cna-gistic:agilent-gene-expression	CopyNumber_Gistic2.Level_4:all_thresholded.by_genes.txt;Merge_transcriptome__agilentg4502a_07_2__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.Level_3:<TUMOR_TYPE>.transcriptome__agilentg4502a_07_2__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.data.txt;Merge_transcriptome__agilentg4502a_07_3__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.Level_3:<TUMOR_TYPE>.transcriptome__agilentg4502a_07_3__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.data.txt;	data_mRNA_median_Zscores.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mRNA_median_Zscores.txt	mrna_median_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores (microarray)	mRNA z-scores (Agilent microarray) compared to the expression distribution of each gene tumors that are diploid for this gene.	
rnaseq-v2-gene-expression	TRUE	TRUE		Merge_rnaseqv2__illuminahiseq_rnaseqv2__unc_edu__Level_3__RSEM_genes_normalized__data.Level_3:<TUMOR_TYPE>.rnaseqv2__illuminahiseq_rnaseqv2__unc_edu__Level_3__RSEM_genes_normalized__data.data.txt;	data_RNA_Seq_v2_expression_median.txt	org.mskcc.cbio.importer.converter.internal.RNASEQV2MRNAMedianConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_v2_expression_median.txt	rna_seq_v2_mrna	MRNA_EXPRESSION	CONTINUOUS	FALSE	mRNA expression (RNA Seq V2 RSEM)	Expression levels for <NUM_GENES> genes in <NUM_CASES> <TUMOR_TYPE> cases (RNA Seq V2 RSEM).	
rnaseq-v2-gene-expression-zscores	TRUE	TRUE	cna-gistic:rnaseq-v2-gene-expression	CopyNumber_Gistic2.Level_4:all_thresholded.by_genes.txt;Merge_rnaseqv2__illuminahiseq_rnaseqv2__unc_edu__Level_3__RSEM_genes_normalized__data.Level_3:<TUMOR_TYPE>.rnaseqv2__illuminahiseq_rnaseqv2__unc_edu__Level_3__RSEM_genes_normalized__data.data.txt;	data_RNA_Seq_v2_mRNA_median_Zscores.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_v2_mRNA_median_Zscores.txt	rna_seq_v2_mrna_median_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores (RNA Seq V2 RSEM)	mRNA z-scores (RNA Seq V2 RSEM) compared to the expression distribution of each gene tumors that are diploid for this gene.	
rnaseq-gene-expression-cpm	FALSE	FALSE			data_RNA_Seq_expression_cpm.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_expression_cpm.txt	mrna_seq_cpm	MRNA_EXPRESSION	CONTINUOUS	FALSE	mRNA expression (RNA Seq CPM)	Expression levels for genes in <NUM_CASES> <TUMOR_TYPE> cases (RNA Seq CPM).	
rnaseq-gene-expression-cpm-zscores	FALSE	FALSE	cna-gistic:rnaseq-gene-expression-cpm		data_RNA_Seq_expression_cpm_Zscores.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_expression_cpm_Zscores.txt	mrna_seq_cpm_Zscores	MRNA_EXPRESSION	Z-SCORE	FALSE	mRNA expression z-scores (RNA Seq CPM)	Expression level z-scores for genes in <NUM_CASES> <TUMOR_TYPE> cases (RNA Seq CPM).	
rnaseq-gene-expression-tpm	FALSE	FALSE			data_RNA_Seq_expression_tpm.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_expression_tpm.txt	mrna_seq_tpm	MRNA_EXPRESSION	CONTINUOUS	FALSE	mRNA expression (RNA Seq TPM)	Expression levels for genes in <NUM_CASES> <TUMOR_TYPE> cases (RNA Seq TPM).	
rnaseq-gene-expression-tpm-zscores	FALSE	FALSE	cna-gistic:rnaseq-gene-expression-tpm		data_RNA_Seq_expression_tpm_Zscores.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_expression_tpm_Zscores.txt	mrna_seq_tpm_Zscores	MRNA_EXPRESSION	Z-SCORE	FALSE	mRNA expression z-scores (RNA Seq TPM)	Expression level z-scores for genes in <NUM_CASES> <TUMOR_TYPE> cases (RNA Seq TPM).	
cna-hg19-seg	TRUE	TRUE		Merge_snp__genome_wide_snp_6__broad_mit_edu__Level_3__segmented_scna_minus_germline_cnv_hg19__seg.Level_3:<TUMOR_TYPE>.snp__genome_wide_snp_6__broad_mit_edu__Level_3__segmented_scna_minus_germline_cnv_hg19__seg.seg.txt;	<CANCER_STUDY>_data_cna_hg19.seg	org.mskcc.cbio.importer.converter.internal.CopyNumberSegmentConverterImpl	org.mskcc.cbio.portal.scripts.ImportCopyNumberSegmentData	TRUE	<CANCER_STUDY>_meta_cna_hg19_seg.txt		COPY_NUMBER_ALTERATION	SEG			Somatic CNA data (copy number ratio from tumor samples minus ratio from matched normals) from TCGA.	hg19
cna-hg18-seg	TRUE	TRUE		Merge_snp__genome_wide_snp_6__broad_mit_edu__Level_3__segmented_scna_minus_germline_cnv_hg18__seg.Level_3:<TUMOR_TYPE>.snp__genome_wide_snp_6__broad_mit_edu__Level_3__segmented_scna_minus_germline_cnv_hg18__seg.seg.txt;	<CANCER_STUDY>_data_cna_hg18.seg	org.mskcc.cbio.importer.converter.internal.CopyNumberSegmentConverterImpl	org.mskcc.cbio.portal.scripts.ImportCopyNumberSegmentData	TRUE	<CANCER_STUDY>_meta_cna_hg18_seg.txt		COPY_NUMBER_ALTERATION	SEG			Somatic CNA data (copy number ratio from tumor samples minus ratio from matched normals) from TCGA.	hg18
cna-hg19-seg-std	TRUE	TRUE		Merge_snp__genome_wide_snp_6__broad_mit_edu__Level_3__segmented_scna_minus_germline_cnv_hg19__seg.Level_3:<TUMOR_TYPE>.snp__genome_wide_snp_6__broad_mit_edu__Level_3__segmented_scna_minus_germline_cnv_hg19__seg.seg.txt;	data_cna_hg19.seg	org.mskcc.cbio.importer.converter.internal.CopyNumberSegmentConverterImpl	org.mskcc.cbio.portal.scripts.ImportCopyNumberSegmentData	TRUE	meta_cna_hg19_seg.txt		COPY_NUMBER_ALTERATION	SEG			Somatic CNA data (copy number ratio from tumor samples minus ratio from matched normals) from TCGA.	hg19
cna-hg18-seg-std	TRUE	TRUE		Merge_snp__genome_wide_snp_6__broad_mit_edu__Level_3__segmented_scna_minus_germline_cnv_hg18__seg.Level_3:<TUMOR_TYPE>.snp__genome_wide_snp_6__broad_mit_edu__Level_3__segmented_scna_minus_germline_cnv_hg18__seg.seg.txt;	data_cna_hg18.seg	org.mskcc.cbio.importer.converter.internal.CopyNumberSegmentConverterImpl	org.mskcc.cbio.portal.scripts.ImportCopyNumberSegmentData	TRUE	meta_cna_hg18_seg.txt		COPY_NUMBER_ALTERATION	SEG			Somatic CNA data (copy number ratio from tumor samples minus ratio from matched normals) from TCGA.	hg18
gistic-genes-amp	TRUE	TRUE		CopyNumber_Gistic2.Level_4:amp_genes.conf_99.txt;CopyNumber_Gistic2.Level_4:table_amp.conf_99.txt;	data_gistic_genes_amp.txt	org.mskcc.cbio.importer.converter.internal.GisticGenesConverterImpl	org.mskcc.cbio.portal.scripts.ImportGisticData	FALSE	meta_gistic_genes_amp.txt	gistic_genes_amp	GISTIC_GENES_AMP	Q-VALUE	FALSE	Putative copy-number alterations (amplifications) from GISTIC	Putative copy-number amplifications determined using GISTIC 2.0.	hg19
gistic-genes-del	TRUE	TRUE		CopyNumber_Gistic2.Level_4:del_genes.conf_99.txt;CopyNumber_Gistic2.Level_4:table_del.conf_99.txt;	data_gistic_genes_del.txt	org.mskcc.cbio.importer.converter.internal.GisticGenesConverterImpl	org.mskcc.cbio.portal.scripts.ImportGisticData	FALSE	meta_gistic_genes_del.txt	gistic_genes_del	GISTIC_GENES_DEL	Q-VALUE	FALSE	Putative copy-number alterations (deletions) from GISTIC	Putative copy-number deletions determined using GISTIC 2.0.	hg19
linear-cna-gistic	TRUE	TRUE		CopyNumber_Gistic2.Level_4:all_data_by_genes.txt;	data_linear_CNA.txt	org.mskcc.cbio.importer.converter.internal.CNAConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_linear_CNA.txt	linear_CNA	COPY_NUMBER_ALTERATION	CONTINUOUS	FALSE	Relative linear copy-number values	Relative linear copy-number values for each gene (from Affymetrix SNP6).	
log2-cna	TRUE	TRUE			data_log2CNA.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_log2CNA.txt	log2CNA	COPY_NUMBER_ALTERATION	LOG2-VALUE	FALSE	Log2 copy-number values	Log2 copy-number values for each gene.	
armlevel_cna	FALSE	FALSE			data_armlevel_CNA.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_armlevel_CNA.txt	armlevel_cna	GENERIC_ASSAY	CATEGORICAL	TRUE	Putative arm-level copy-number from GISTIC	Putative arm-level copy-number from GISTIC 2.0.	
methylation-hm27	TRUE	TRUE		Correlate_Methylation_vs_mRNA.Level_4:Correlate_Methylation_vs_mRNA_<TUMOR_TYPE>_matrix.txt;Merge_methylation__humanmethylation27__jhu_usc_edu__Level_3__within_bioassay_data_set_function__data.Level_3:<TUMOR_TYPE>.methylation__humanmethylation27__jhu_usc_edu__Level_3__within_bioassay_data_set_function__data.data.txt;	data_methylation_hm27.txt	org.mskcc.cbio.importer.converter.internal.MethylationConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_methylation_hm27.txt	methylation_hm27	METHYLATION	CONTINUOUS	FALSE	Methylation (HM27)	Methylation (HM27) beta-values for genes in <NUM_CASES> cases. For genes with multiple methylation probes, the probe most anti-correlated with expression.	
methylation-hm450	TRUE	TRUE		Correlate_Methylation_vs_mRNA.Level_4:Correlate_Methylation_vs_mRNA_<TUMOR_TYPE>_matrix.txt;Merge_methylation__humanmethylation450__jhu_usc_edu__Level_3__within_bioassay_data_set_function__data.Level_3:<TUMOR_TYPE>.methylation__humanmethylation450__jhu_usc_edu__Level_3__within_bioassay_data_set_function__data.data.txt;	data_methylation_hm450.txt	org.mskcc.cbio.importer.converter.internal.MethylationConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_methylation_hm450.txt	methylation_hm450	METHYLATION	CONTINUOUS	FALSE	Methylation (HM450)	Methylation (HM450) beta-values for genes in <NUM_CASES> cases. For genes with multiple methylation probes, the probe most anti-correlated with expression.	
methylation-promoters-hmepic	TRUE	TRUE			data_methylation_promoters_hmEPIC.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_methylation_promoters_hmEPIC.txt	methylation_promoters_hmEPIC	METHYLATION	CONTINUOUS	TRUE	Methylation (HMEPIC)	Methylation beta-values (Infinium MethylationEPIC platform). This matrix is collapsed to include only promoters.	
methylation-promoters-wgbs	TRUE	TRUE			data_methylation_promoters_wgbs.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_methylation_promoters_wgbs.txt	methylation_promoters_wgbs	METHYLATION	CONTINUOUS	TRUE	Methylation (WGBS)	Methylation beta-values by whole-genome bisulfite sequencing. Values are the average fractional methylation value across promoter CpGs for the labeled gene.	
methylation-genebodies-hmepic	TRUE	TRUE			data_methylation_genebodies_hmEPIC.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_methylation_genebodies_hmEPIC.txt	methylation_genebodies_hmEPIC	METHYLATION	CONTINUOUS	TRUE	Methylation (HMEPIC)	Methylation beta-values (HMEPIC platform). Values are the average fractional methylation value across exons for the labeled gene.	
methylation-genebodies-wgbs	TRUE	TRUE			data_methylation_genebodies_wgbs.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_methylation_genebodies_wgbs.txt	methylation_genebodies_wgbs	METHYLATION	CONTINUOUS	TRUE	Methylation (WGBS)	Methylation beta-values by whole-genome bisulfite sequencing. Values are the average fractional methylation value across exons of the labeled gene.	
mutation-foundation	FALSE	FALSE			data_mutations_extended_foundation.txt	 	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mutations_extended.txt	mutations	MUTATION_EXTENDED	MAF	TRUE	Mutations from Foundation	Putative mutation calls on <NUM_CASES> cases determined by Foundation Medicine.	
mutation	TRUE	TRUE		Mutation_Packager_Calls.Level_3:<BARCODE>.maf.txt;Mutation_Packager_Raw_Calls.Level_3:<BARCODE>.maf.txt	data_mutations_extended.txt	org.mskcc.cbio.importer.converter.internal.MutationConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mutations_extended.txt	mutations	MUTATION_EXTENDED	MAF	TRUE	Mutations	Mutation data from whole exome sequencing.	
mutation-other	FALSE	TRUE			data_mutations_extended_*.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mutations_extended.txt	mutations	MUTATION_EXTENDED	MAF	TRUE	Mutations	Mutation data from whole exome sequencing.	
mutation_germline	FALSE	FALSE			data_mutations_germline.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mutations_extended.txt	mutations	MUTATION_EXTENDED	MAF	TRUE	Mutations	Mutation data from whole exome sequencing.	
mutation_nonsignedout	FALSE	FALSE			data_mutations_nonsignedout.txt			TRUE	meta_mutations_extended.txt	mutations	MUTATION_EXTENDED	MAF	TRUE	Mutations	Mutation data from whole exome sequencing.	
mutation-uncalled	FALSE	FALSE			data_mutations_uncalled.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mutations_uncalled.txt	mutations_uncalled	MUTATION_UNCALLED	MAF	FALSE	Mutations Uncalled	Mutation data from whole exome sequencing. (Uncalled)	
mutation-manual	FALSE	TRUE			data_mutations_manual.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mutations_extended.txt	mutations	MUTATION_EXTENDED	MAF	TRUE	Mutations	Mutation data that are manually curated.	
gene-panel	FALSE	TRUE			data_gene_panel.txt		org.mskcc.cbio.portal.scripts.ImportGenePanel	FALSE								
gene-panel	FALSE	TRUE			data_gene_panel_*.txt		org.mskcc.cbio.portal.scripts.ImportGenePanel	FALSE								
mutation-significance-v2	TRUE	TRUE		MutSigNozzleReport2.0.Level_4:<TUMOR_TYPE>.sig_genes.txt;	data_mutsig.txt	org.mskcc.cbio.importer.converter.internal.PassThroughConverterImpl	org.mskcc.cbio.portal.scripts.ImportMutSigData	FALSE	meta_mutsig.txt	mutsig	MUTSIG	Q-VALUE	FALSE		Mutation analysis via MutSig from whole exome sequencing.	
fusion	FALSE	FALSE			data_fusions.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_fusions.txt	mutations	FUSION	FUSION	FALSE	Fusions	Fusion data derived from mutation file.	
mirna-expression	TRUE	TRUE		miRseq_Mature_Preprocess.Level_3:<TUMOR_TYPE>.miRseq_mature_RPM.txt	data_expression_miRNA.txt	org.mskcc.cbio.importer.converter.internal.MIRNAConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_expression_miRNA.txt	mirna	MRNA_EXPRESSION	CONTINUOUS	FALSE	microRNA expression	Expression levels for microRNAs.	
mirna-median-zscores	FALSE	TRUE		miRseq_Mature_Preprocess.Level_3:<TUMOR_TYPE>.miRseq_mature_RPM.txt	data_miRNA_median_Zscores.txt	org.mskcc.cbio.importer.converter.internal.MIRNAZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_miRNA_median_Zscores.txt	mirna_median_Zscores	MRNA_EXPRESSION	Z-SCORE	FALSE	microRNA expression z-scores	microRNA expression z-scores compared to all tumors.	
mirna-merged-median-zscores	FALSE	TRUE	mirna-median-zscores:rnaseq-v2-gene-expression	miRseq_Mature_Preprocess.Level_3:<TUMOR_TYPE>.miRseq_mature_RPM.txt;Merge_rnaseqv2__illuminahiseq_rnaseqv2__unc_edu__Level_3__RSEM_genes_normalized__data.Level_3:<TUMOR_TYPE>.rnaseqv2__illuminahiseq_rnaseqv2__unc_edu__Level_3__RSEM_genes_normalized__data.data.txt;	data_expression_merged_median_Zscores.txt	org.mskcc.cbio.importer.converter.internal.MIRNAMergedMedianZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_expression_merged_median_Zscores.txt	mrna_merged_median_Zscores	MRNA_EXPRESSION	Z-SCORE	FALSE	mRNA/miRNA expression z-scores (all genes)	 mRNA and microRNA z-scores merged: mRNA expression z-scores compared to diploid tumors (diploid for each gene), median values from all three mRNA expression platforms; and miRNA z-scores compared to all tumours.	
mrna-outliers	FALSE	TRUE			data_mRNA_outliers.txt	org.mskcc.cbio.importer.converter.internal.PassThroughConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mRNA_outliers.txt	mrna_outliers	MRNA_EXPRESSION	DISCRETE	FALSE	mRNA Expression Outliers	mRNA expression outliers compared to normals (-1 = underexpressed, 1 = overexpressed).	
capture-gene-expression	FALSE	FALSE			data_RNA_Seq_expression_capture.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_expression_capture.txt	rna_seq_mrna_capture	MRNA_EXPRESSION	CONTINUOUS	FALSE	mRNA expression / capture (RNA Seq capture)	mRNA expression from capture (RNA Seq RPKM)	
capture-gene-expression-zscores	FALSE	FALSE	cna-gistic:capture-gene-expression		data_RNA_Seq_expression_capture_Zscores.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_expression_capture_Zscores.txt	rna_seq_mrna_capture_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression / capture z-scores (RNA Seq capture)	mRNA expression from capture z-scores (RNA Seq RPKM)	
other-gene-expression-zscores	FALSE	FALSE			data_expression_other_Zscores.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_expression_other_Zscores.txt							
methylation-binary	FALSE	FALSE			data_methylation_binary.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_methylation_binary.txt							
mrna-seq-fpkm	FALSE	FALSE			data_mRNA_seq_fpkm.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mRNA_seq_fpkm.txt	mrna_seq_fpkm	MRNA_EXPRESSION	CONTINUOUS		mRNA expression (FPKM)	mRNA expression from capture (RNA Seq FPKM)	
mrna-seq-rsem					data_mrnaseq_rsem.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrnaseq_rsem.txt							
mrna-seq-fcount					data_mrnaseq_fcount.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrnaseq_fcount.txt							
mrna-seq-fpkm-capture	FALSE	FALSE			data_mRNA_seq_fpkm_capture.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mRNA_seq_fpkm_capture.txt	mrna_seq_fpkm_capture	MRNA_EXPRESSION	CONTINUOUS		mRNA expression (FPKM capture)	mRNA expression from capture (RNA Seq FPKM)	
mrna-seq-fpkm-polya	FALSE	FALSE			data_mRNA_seq_fpkm_polya.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mRNA_seq_fpkm_polya.txt	mrna_seq_fpkm_polya	MRNA_EXPRESSION	CONTINUOUS		mRNA expression (FPKM polyA)	mRNA expression from polyA (RNA Seq FPKM)	
mrna-seq-fpkm-capture-zscores	FALSE	FALSE	cna-gistic:mrna-seq-fpkm-capture		data_mRNA_seq_fpkm_capture_Zscores.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mRNA_seq_fpkm_capture_Zscores.txt	mrna_seq_fpkm_capture_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores (FPKM capture)	mRNA expression from capture z-scores (RNA Seq FPKM)	
mrna-seq-fpkm-polya-zscores	FALSE	FALSE	cna-gistic:mrna-seq-fpkm-polya		data_mRNA_seq_fpkm_polya_Zscores.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mRNA_seq_fpkm_polya_Zscores.txt	mrna_seq_fpkm_polya_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores (FPKM polyA)	mRNA expression from polyA z-scores (RNA Seq FPKM)	
fusion-gml	FALSE	FALSE			data_fusions_gml.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_fusions_gml.txt	mutations	FUSION	FUSION_GML	FALSE	Fusions (GML)	Fusion data derived from mutation file. (GML)	
gsva-scores	FALSE	TRUE			data_gsva_scores.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_gsva_scores.txt	gsva_scores	GENESET_SCORE	GSVA-SCORE	FALSE	GSVA scores		
gsva-pvalues	FALSE	TRUE			data_gsva_pvalues.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_gsva_pvalues.txt	gsva_pvalues	GENESET_SCORE	P-VALUE	FALSE	GSVA p-values		
drug-treatment-ic50					data_drug_treatment_IC50.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_drug_treatment_IC50.txt	CCLE_drug_treatment_IC50	GENERIC_ASSAY	LIMIT-VALUE	TRUE	Treatment response: IC50	Treatment response: IC50	
drug-treatment-ic50-zscores					data_drug_treatment_ZSCORE.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_drug_treatment_ZSCORE.txt	CCLE_drug_treatment_zscore	GENERIC_ASSAY	LIMIT-VALUE	TRUE	Treament response: z-score of IC50	Treament response: z-score of IC50	
drug-treatment-auc					data_drug_treatment_AUC.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_drug_treatment_AUC.txt	CCLE_drug_treatment_AUC	GENERIC_ASSAY	LIMIT-VALUE	TRUE	Treatment response: AUC	Treatment response: AUC	
rnaseq-v2-gene-expression-all-sample-zscores	FALSE	FALSE	rnaseq-v2-gene-expression		data_RNA_Seq_v2_mRNA_median_all_sample_Zscores.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_v2_mRNA_median_all_sample_Zscores.txt	rna_seq_v2_mrna_median_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to all samples (log RNA Seq V2 RSEM)	Log-transformed mRNA z-scores compared to the expression distribution of all samples (RNA Seq V2 RSEM).	
agilent-gene-expression-all-sample-zscores	FALSE	FALSE	agilent-gene-expression		data_mRNA_median_all_sample_Zscores.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mRNA_median_all_sample_Zscores.txt	mrna_median_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to all samples (log microarray)	Log-transformed mRNA z-scores compared to the expression distribution of all samples  (Agilent microarray).	
affymetrix-gene-expression-all-sample-zscores	FALSE	FALSE	affymetrix-gene-expression		data_expression_all_sample_Zscores.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_expression_all_sample_Zscores.txt	mrna_U133_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to all samples (log U133 microarray only)	Log-transformed mRNA z-scores compared to the expression distribution of all samples (U133 microarray only).	
rnaseq-gene-expression-all-sample-zscores	FALSE	FALSE	rnaseq-gene-expression		data_RNA_Seq_mRNA_median_all_sample_Zscores.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_mRNA_median_all_sample_Zscores.txt	rna_seq_mrna_median_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to all samples (log RNA Seq RPKM)	Log-transformed mRNA z-scores compared to the expression distribution of all samples (RNA Seq RPKM).	
rnaseq-gene-expression-cpm-all-sample-zscores	FALSE	FALSE	rnaseq-gene-expression-cpm		data_RNA_Seq_expression_cpm_all_sample_Zscores.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_expression_cpm_all_sample_Zscores.txt	mrna_seq_cpm_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to all samples (log RNA Seq CPM)	Log-transformed mRNA z-scores compared to the expression distribution of all samples (RNA Seq CPM).	
rnaseq-gene-expression-tpm-all-sample-zscores	FALSE	FALSE	rnaseq-gene-expression-tpm		data_RNA_Seq_expression_tpm_all_sample_Zscores.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_expression_tpm_all_sample_Zscores.txt	mrna_seq_tpm_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to all samples (log RNA Seq TPM)	Log-transformed mRNA z-scores compared to the expression distribution of all samples (RNA Seq TPM).	
capture-gene-expression-all-sample-zscores	FALSE	FALSE	capture-gene-expression		data_RNA_Seq_expression_capture_all_sample_Zscores.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_expression_capture_all_sample_Zscores.txt	rna_seq_mrna_capture_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression / capture z-scores relative to all samples (log RNA Seq capture)	Log-transformed mRNA z-scores compared to the expression distribution across all samples (RNA Seq RPKM capture)	
mrna-seq-fpkm-capture-all-sample-zscores	FALSE	FALSE	mrna-seq-fpkm-capture		data_mRNA_seq_fpkm_capture_all_sample_Zscores.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mRNA_seq_fpkm_capture_all_sample_Zscores.txt	mrna_seq_fpkm_capture_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to all samples (log FPKM capture)	Log-transformed mRNA z-scores compared to expression distribution of all samples (RNA Seq FPKM capture)	
mrna-seq-fpkm-polya-all-sample-zscores	FALSE	FALSE	mrna-seq-fpkm-polya		data_mRNA_seq_fpkm_polya_all_sample_Zscores.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mRNA_seq_fpkm_polya_all_sample_Zscores.txt	mrna_seq_fpkm_polya_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to all samples (log FPKM polyA)	Log-transformed mRNA z-scores compared to expression distribution of all samples (RNA Seq FPKM polyA)	
microbiome-signatures	FALSE	FALSE			data_microbiome.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_microbiome.txt	microbiome_signature	GENERIC_ASSAY	LIMIT-VALUE	TRUE	Cancer Microbiome Data (log2-CPM Normalized)	Microbial Signatures (log-cpm) from whole genome and whole-transcriptome sequencing studies of TCGA.	
rnaseq-v2-gene-expression-normal	FALSE	FALSE			data_RNA_Seq_v2_mRNA_median_normals.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_v2_mRNA_median_normals.txt	rna_seq_v2_mrna_median_normals	MRNA_EXPRESSION	CONTINUOUS	TRUE	mRNA expression of adjacent normal samples, RSEM (Batch normalized from Illumina HiSeq_RNASeqV2)	mRNA expression of adjacent normal samples, RSEM (Batch normalized from Illumina HiSeq_RNASeqV2)	
rnaseq-v2-gene-expression-normal-zscores	FALSE	FALSE			data_RNA_Seq_v2_mRNA_median_Zscores_normals.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_v2_mRNA_median_Zscores_normals.txt	rna_seq_v2_mrna_median_normals_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression of adjacent normal samples, z-scores relative to all normal samples (log RNA Seq V2 RSEM)	Expression z-scores of adjacent normal samples compared to the expression distribution of all log-transformed mRNA expression of adjacent normal samples in the cohort.	
rnaseq-v2-gene-expression-all-sample-ref-normal-zscores	FALSE	FALSE			data_RNA_Seq_v2_mRNA_median_all_sample_ref_normal_Zscores.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_RNA_Seq_v2_mRNA_median_all_sample_ref_normal_Zscores.txt	rna_seq_v2_mrna_median_all_sample_ref_normal_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to normal samples (log RNA Seq V2 RSEM)	Expression z-scores of tumor samples compared to the expression distribution of all log-transformed mRNA expression of adjacent normal samples in the cohort.	
mutational-signature-contribution-v2	FALSE	FALSE			data_mutational_signature_contribution_v2.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mutational_signature_contribution_v2.txt	mutational_signature_contribution_v2	GENERIC_ASSAY	LIMIT-VALUE	TRUE	mutational signature contribution v2	profile for contribution value of mutational signatures (COSMIC version 2)	
mutational-signature-pvalue-v2	FALSE	FALSE			data_mutational_signature_pvalue_v2.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mutational_signature_pvalue_v2.txt	mutational_signature_pvalue_v2	GENERIC_ASSAY	LIMIT-VALUE	FALSE	mutational signature pvalue v2	profile for pvalue of mutational signatures (COSMIC version 2)	
mutational-signature-contribution-v3	FALSE	FALSE			data_mutational_signature_contribution_v3.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mutational_signature_contribution_v3.txt	mutational_signature_contribution_v3	GENERIC_ASSAY	LIMIT-VALUE	TRUE	mutational signature contribution v3	profile for contribution value of mutational signatures (COSMIC version 3)	
mutational-signature-pvalue-v3	FALSE	FALSE			data_mutational_signature_pvalue_v3.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mutational_signature_pvalue_v3.txt	mutational_signature_pvalue_v3	GENERIC_ASSAY	LIMIT-VALUE	FALSE	mutational signature pvalue v3	profile for pvalue of mutational signatures (COSMIC version 3)	
resource-definition		FALSE			data_resource_definition.txt		org.mskcc.cbio.portal.scripts.ImportResourceDefinition	TRUE	meta_resource_definition.txt							
resource-study		FALSE			data_resource_study.txt		org.mskcc.cbio.portal.scripts.ImportResourceData	TRUE	meta_resource_study.txt							
resource-sample		FALSE			data_resource_sample.txt		org.mskcc.cbio.portal.scripts.ImportResourceData	TRUE	meta_resource_sample.txt							
phosphoprotein_quantification		FALSE			data_phosphoprotein_quantification.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_phosphoprotein_quantification.txt	phosphoprotein_quantification	GENERIC_ASSAY	LIMIT-VALUE	TRUE	Phosphoprotein site level expression data by CPTAC (TMT, Log2ratio)	Phosphoprotein site level expression data given in log-ratio (normalized) by CPTAC.	
mirna-expression	TRUE	TRUE		miRseq_Mature_Preprocess.Level_3:<TUMOR_TYPE>.miRseq_mature_RPM.txt	data_mirna.txt	org.mskcc.cbio.importer.converter.internal.MIRNAConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mirna.txt	mirna	MRNA_EXPRESSION	CONTINUOUS	FALSE	microRNA expression	Expression levels for microRNAs.	
mirna-median-zscores	FALSE	TRUE		miRseq_Mature_Preprocess.Level_3:<TUMOR_TYPE>.miRseq_mature_RPM.txt	data_mirna_zscores.txt	org.mskcc.cbio.importer.converter.internal.MIRNAZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mirna_zscores.txt	mirna_median_Zscores	MRNA_EXPRESSION	Z-SCORE	FALSE	microRNA expression z-scores	microRNA expression z-scores compared to all tumors.	
affymetrix-gene-expression	TRUE	TRUE		Merge_transcriptome__ht_hg_u133a__broad_mit_edu__Level_3__gene_rma__data.Level_3:<TUMOR_TYPE>.transcriptome__ht_hg_u133a__broad_mit_edu__Level_3__gene_rma__data.data.txt	data_mrna_affymetrix_microarray.txt	org.mskcc.cbio.importer.converter.internal.MRNAMedianConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_affymetrix_microarray.txt	mrna_U133	MRNA_EXPRESSION	CONTINUOUS	FALSE	mRNA expression (U133 microarray only)	mRNA expression data from the Affymetrix U133 microarray.	
affymetrix-gene-expression-zscores	TRUE	TRUE	cna-gistic:affymetrix-gene-expression	CopyNumber_Gistic2.Level_4:all_thresholded.by_genes.txt;Merge_transcriptome__ht_hg_u133a__broad_mit_edu__Level_3__gene_rma__data.Level_3:<TUMOR_TYPE>.transcriptome__ht_hg_u133a__broad_mit_edu__Level_3__gene_rma__data.data.txt	data_mrna_affymetrix_microarray_zscores_ref_diploid_samples.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_affymetrix_microarray_zscores_ref_diploid_samples.txt	mrna_U133_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores (U133 microarray only)	mRNA z-scores  (U133 microarray only) compared to the expression distribution of each gene tumors that are diploid for this gene.	
affymetrix-gene-expression-all-sample-zscores	FALSE	FALSE	affymetrix-gene-expression		data_mrna_affymetrix_microarray_zscores_ref_all_samples.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_affymetrix_microarray_zscores_ref_all_samples.txt	mrna_U133_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to all samples (log U133 microarray only)	Log-transformed mRNA z-scores compared to the expression distribution of all samples (U133 microarray only).	
agilent-gene-expression	TRUE	TRUE		Merge_transcriptome__agilentg4502a_07_2__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.Level_3:<TUMOR_TYPE>.transcriptome__agilentg4502a_07_2__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.data.txt;Merge_transcriptome__agilentg4502a_07_3__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.Level_3:<TUMOR_TYPE>.transcriptome__agilentg4502a_07_3__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.data.txt;	data_mrna_agilent_microarray.txt	org.mskcc.cbio.importer.converter.internal.MRNAMedianConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_agilent_microarray.txt	mrna	MRNA_EXPRESSION	CONTINUOUS	FALSE	mRNA expression (microarray)	Expression levels for <NUM_GENES> genes in <NUM_CASES> <TUMOR_TYPE> cases (Agilent microarray).	
agilent-gene-expression-zscores	TRUE	TRUE	cna-gistic:agilent-gene-expression	CopyNumber_Gistic2.Level_4:all_thresholded.by_genes.txt;Merge_transcriptome__agilentg4502a_07_2__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.Level_3:<TUMOR_TYPE>.transcriptome__agilentg4502a_07_2__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.data.txt;Merge_transcriptome__agilentg4502a_07_3__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.Level_3:<TUMOR_TYPE>.transcriptome__agilentg4502a_07_3__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.data.txt;	data_mrna_agilent_microarray_zscores_ref_diploid_samples.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_agilent_microarray_zscores_ref_diploid_samples.txt	mrna_median_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores (microarray)	mRNA z-scores (Agilent microarray) compared to the expression distribution of each gene tumors that are diploid for this gene.	
agilent-gene-expression-all-sample-zscores	FALSE	FALSE	agilent-gene-expression		data_mrna_agilent_microarray_zscores_ref_all_samples.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_agilent_microarray_zscores_ref_all_samples.txt	mrna_median_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to all samples (log microarray)	Log-transformed mRNA z-scores compared to the expression distribution of all samples  (Agilent microarray).	
rnaseq-gene-expression	TRUE	TRUE		Merge_rnaseq__illuminaga_rnaseq__bcgsc_ca__Level_3__gene_expression__data.Level_3:<TUMOR_TYPE>.rnaseq__illuminaga_rnaseq__bcgsc_ca__Level_3__gene_expression__data.data.txt;	data_mrna_seq_rpkm.txt	org.mskcc.cbio.importer.converter.internal.RNASEQMRNAMedianConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_rpkm.txt	rna_seq_mrna	MRNA_EXPRESSION	CONTINUOUS	FALSE	mRNA expression (RNA Seq RPKM)	Expression levels for <NUM_GENES> genes in <NUM_CASES> <TUMOR_TYPE> cases (RNA Seq RPKM).	
rnaseq-gene-expression-zscores	TRUE	TRUE	cna-gistic:rnaseq-gene-expression	CopyNumber_Gistic2.Level_4:all_thresholded.by_genes.txt;Merge_rnaseq__illuminaga_rnaseq__bcgsc_ca__Level_3__gene_expression__data.Level_3:<TUMOR_TYPE>.rnaseq__illuminaga_rnaseq__bcgsc_ca__Level_3__gene_expression__data.data.txt;	data_mrna_seq_rpkm_zscores_ref_diploid_samples.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_rpkm_zscores_ref_diploid_samples.txt	rna_seq_mrna_median_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores (RNA Seq RPKM)	mRNA z-scores (RNA Seq RPKM) compared to the expression distribution of each gene tumors that are diploid for this gene.	
rnaseq-gene-expression-all-sample-zscores	FALSE	FALSE	rnaseq-gene-expression		data_mrna_seq_rpkm_zscores_ref_all_samples.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_rpkm_zscores_ref_all_samples.txt	rna_seq_mrna_median_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to all samples (log RNA Seq RPKM)	Log-transformed mRNA z-scores compared to the expression distribution of all samples (RNA Seq RPKM).	
rnaseq-v2-gene-expression	TRUE	TRUE		Merge_rnaseqv2__illuminahiseq_rnaseqv2__unc_edu__Level_3__RSEM_genes_normalized__data.Level_3:<TUMOR_TYPE>.rnaseqv2__illuminahiseq_rnaseqv2__unc_edu__Level_3__RSEM_genes_normalized__data.data.txt;	data_mrna_seq_v2_rsem.txt	org.mskcc.cbio.importer.converter.internal.RNASEQV2MRNAMedianConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_v2_rsem.txt	rna_seq_v2_mrna	MRNA_EXPRESSION	CONTINUOUS	FALSE	mRNA expression (RNA Seq V2 RSEM)	Expression levels for <NUM_GENES> genes in <NUM_CASES> <TUMOR_TYPE> cases (RNA Seq V2 RSEM).	
rnaseq-v2-gene-expression-zscores	TRUE	TRUE	cna-gistic:rnaseq-v2-gene-expression	CopyNumber_Gistic2.Level_4:all_thresholded.by_genes.txt;Merge_rnaseqv2__illuminahiseq_rnaseqv2__unc_edu__Level_3__RSEM_genes_normalized__data.Level_3:<TUMOR_TYPE>.rnaseqv2__illuminahiseq_rnaseqv2__unc_edu__Level_3__RSEM_genes_normalized__data.data.txt;	data_mrna_seq_v2_rsem_zscores_ref_diploid_samples.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_v2_rsem_zscores_ref_diploid_samples.txt	rna_seq_v2_mrna_median_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores (RNA Seq V2 RSEM)	mRNA z-scores (RNA Seq V2 RSEM) compared to the expression distribution of each gene tumors that are diploid for this gene.	
rnaseq-v2-gene-expression-all-sample-zscores	FALSE	FALSE	rnaseq-v2-gene-expression		data_mrna_seq_v2_rsem_zscores_ref_all_samples.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_v2_rsem_zscores_ref_all_samples.txt	rna_seq_v2_mrna_median_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to all samples (log RNA Seq V2 RSEM)	Log-transformed mRNA z-scores compared to the expression distribution of all samples (RNA Seq V2 RSEM).	
rnaseq-v2-gene-expression-normal	FALSE	FALSE			data_mrna_seq_v2_rsem_normal_samples.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_v2_rsem_normal_samples.txt	rna_seq_v2_mrna_median_normals	MRNA_EXPRESSION	CONTINUOUS	TRUE	mRNA expression of adjacent normal samples, RSEM (Batch normalized from Illumina HiSeq_RNASeqV2)	mRNA expression of adjacent normal samples, RSEM (Batch normalized from Illumina HiSeq_RNASeqV2)	
rnaseq-v2-gene-expression-normal-zscores	FALSE	FALSE			data_mrna_seq_v2_rsem_normal_samples_zscores_ref_normal_samples.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_v2_rsem_normal_samples_zscores_ref_normal_samples.txt	rna_seq_v2_mrna_median_normals_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression of adjacent normal samples, z-scores relative to all normal samples (log RNA Seq V2 RSEM)	Expression z-scores of adjacent normal samples compared to the expression distribution of all log-transformed mRNA expression of adjacent normal samples in the cohort.	
rnaseq-v2-gene-expression-all-sample-ref-normal-zscores	FALSE	FALSE			data_mrna_seq_v2_rsem_zscores_ref_normal_samples.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_v2_rsem_zscores_ref_normal_samples.txt	rna_seq_v2_mrna_median_all_sample_ref_normal_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to normal samples (log RNA Seq V2 RSEM)	Expression z-scores of tumor samples compared to the expression distribution of all log-transformed mRNA expression of adjacent normal samples in the cohort.	
rnaseq-gene-expression-cpm	FALSE	FALSE			data_mrna_seq_cpm.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_cpm.txt	mrna_seq_cpm	MRNA_EXPRESSION	CONTINUOUS	FALSE	mRNA expression (RNA Seq CPM)	Expression levels for genes in <NUM_CASES> <TUMOR_TYPE> cases (RNA Seq CPM).	
rnaseq-gene-expression-cpm-zscores	FALSE	FALSE	cna-gistic:rnaseq-gene-expression-cpm		data_mrna_seq_cpm_zscores_ref_diploid_samples.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_cpm_zscores_ref_diploid_samples.txt	mrna_seq_cpm_Zscores	MRNA_EXPRESSION	Z-SCORE	FALSE	mRNA expression z-scores (RNA Seq CPM)	Expression level z-scores for genes in <NUM_CASES> <TUMOR_TYPE> cases (RNA Seq CPM).	
rnaseq-gene-expression-cpm-all-sample-zscores	FALSE	FALSE	rnaseq-gene-expression-cpm		data_mrna_seq_cpm_zscores_ref_all_samples.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_cpm_zscores_ref_all_samples.txt	mrna_seq_cpm_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to all samples (log RNA Seq CPM)	Log-transformed mRNA z-scores compared to the expression distribution of all samples (RNA Seq CPM).	
rnaseq-gene-expression-tpm	FALSE	FALSE			data_mrna_seq_tpm.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_tpm.txt	mrna_seq_tpm	MRNA_EXPRESSION	CONTINUOUS	FALSE	mRNA expression (RNA Seq TPM)	Expression levels for genes in <NUM_CASES> <TUMOR_TYPE> cases (RNA Seq TPM).	
rnaseq-gene-expression-tpm-zscores	FALSE	FALSE	cna-gistic:rnaseq-gene-expression-tpm		data_mrna_seq_tpm_zscores_ref_diploid_samples.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_tpm_zscores_ref_diploid_samples.txt	mrna_seq_tpm_Zscores	MRNA_EXPRESSION	Z-SCORE	FALSE	mRNA expression z-scores (RNA Seq TPM)	Expression level z-scores for genes in <NUM_CASES> <TUMOR_TYPE> cases (RNA Seq TPM).	
rnaseq-gene-expression-tpm-all-sample-zscores	FALSE	FALSE	rnaseq-gene-expression-tpm		data_mrna_seq_tpm_zscores_ref_all_samples.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_tpm_zscores_ref_all_samples.txt	mrna_seq_tpm_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to all samples (log RNA Seq TPM)	Log-transformed mRNA z-scores compared to the expression distribution of all samples (RNA Seq TPM).	
mrna-seq-fpkm-capture	FALSE	FALSE			data_mrna_seq_fpkm_capture.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_fpkm_capture.txt	mrna_seq_fpkm_capture	MRNA_EXPRESSION	CONTINUOUS		mRNA expression (FPKM capture)	mRNA expression from capture (RNA Seq FPKM)	
mrna-seq-fpkm-capture-zscores	FALSE	FALSE	cna-gistic:mrna-seq-fpkm-capture		data_mrna_seq_fpkm_capture_zscores_ref_diploid_samples.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_fpkm_capture_zscores_ref_diploid_samples.txt	mrna_seq_fpkm_capture_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores (FPKM capture)	mRNA expression from capture z-scores (RNA Seq FPKM)	
mrna-seq-fpkm-capture-all-sample-zscores	FALSE	FALSE	mrna-seq-fpkm-capture		data_mrna_seq_fpkm_capture_zscores_ref_all_samples.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_fpkm_capture_zscores_ref_all_samples.txt	mrna_seq_fpkm_capture_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to all samples (log FPKM capture)	Log-transformed mRNA z-scores compared to expression distribution of all samples (RNA Seq FPKM capture)	
mrna-seq-fpkm-polya	FALSE	FALSE			data_mrna_seq_fpkm_polya.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_fpkm_polya.txt	mrna_seq_fpkm_polya	MRNA_EXPRESSION	CONTINUOUS		mRNA expression (FPKM polyA)	mRNA expression from polyA (RNA Seq FPKM)	
mrna-seq-fpkm-polya-zscores	FALSE	FALSE	cna-gistic:mrna-seq-fpkm-polya		data_mrna_seq_fpkm_polya_zscores_ref_diploid_samples.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_fpkm_polya_zscores_ref_diploid_samples.txt	mrna_seq_fpkm_polya_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores (FPKM polyA)	mRNA expression from polyA z-scores (RNA Seq FPKM)	
mrna-seq-fpkm-polya-all-sample-zscores	FALSE	FALSE	mrna-seq-fpkm-polya		data_mrna_seq_fpkm_polya_zscores_ref_all_samples.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_seq_fpkm_polya_zscores_ref_all_samples.txt	mrna_seq_fpkm_polya_all_sample_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores relative to all samples (log FPKM polyA)	Log-transformed mRNA z-scores compared to expression distribution of all samples (RNA Seq FPKM polyA)	
mirna-merged-median-zscores	FALSE	TRUE	mirna-median-zscores:rnaseq-v2-gene-expression	miRseq_Mature_Preprocess.Level_3:<TUMOR_TYPE>.miRseq_mature_RPM.txt;Merge_rnaseqv2__illuminahiseq_rnaseqv2__unc_edu__Level_3__RSEM_genes_normalized__data.Level_3:<TUMOR_TYPE>.rnaseqv2__illuminahiseq_rnaseqv2__unc_edu__Level_3__RSEM_genes_normalized__data.data.txt;	data_mrna_mirna_merged_zscores.txt	org.mskcc.cbio.importer.converter.internal.MIRNAMergedMedianZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_mirna_merged_zscores.txt	mrna_merged_median_Zscores	MRNA_EXPRESSION	Z-SCORE	FALSE	mRNA/miRNA expression z-scores (all genes)	mRNA and microRNA z-scores merged: mRNA expression z-scores compared to diploid tumors (diploid for each gene), median values from all three mRNA expression platforms; and miRNA z-scores compared to all tumours.	
gene-expression-merged	TRUE	TRUE			data_mrna_affymetrix_microarray_merged.txt	org.mskcc.cbio.importer.converter.internal.MRNAMedianConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_affymetrix_microarray_merged.txt	mrna_U133	MRNA_EXPRESSION	CONTINUOUS	FALSE	mRNA expression (U133 microarray only)	mRNA expression data from the Affymetrix U133 microarray.	
gene-expression-merged-zscores	TRUE	TRUE	cna-gistic:gene-expression-merged	CopyNumber_Gistic2.Level_4:all_thresholded.by_genes.txt;Merge_transcriptome__ht_hg_u133a__broad_mit_edu__Level_3__gene_rma__data.Level_3:<TUMOR_TYPE>.transcriptome__ht_hg_u133a__broad_mit_edu__Level_3__gene_rma__data.data.txt	data_mrna_affymetrix_microarray_merged_zscores_ref_diploid_samples.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_affymetrix_microarray_merged_zscores_ref_diploid_samples.txt	mrna_Zscores	MRNA_EXPRESSION	Z-SCORE	TRUE	mRNA expression z-scores	mRNA z-scores compared to the expression distribution of each gene tumors that are diploid for this gene.	
mrna-outliers	FALSE	TRUE			data_mrna_outliers.txt	org.mskcc.cbio.importer.converter.internal.PassThroughConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mrna_outliers.txt	mrna_outliers	MRNA_EXPRESSION	DISCRETE	FALSE	mRNA Expression Outliers	mRNA expression outliers compared to normals (-1 = underexpressed, 1 = overexpressed).	
rppa-zscores	TRUE	TRUE	rppa	RPPA_AnnotateWithGene.Level_3:<TUMOR_TYPE>.rppa.txt;	data_rppa_zscores.txt	org.mskcc.cbio.importer.converter.internal.ZScoresConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_rppa_zscores.txt	rppa_Zscores	PROTEIN_LEVEL	Z-SCORE	TRUE	Protein expression z-scores (RPPA)	Protein expression, measured by reverse-phase protein array, z-scores	
protein-quantification-zscores	FALSE	TRUE	protein-quantification		data_protein_quantification_zscores.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_protein_quantification_zscores.txt	protein_quantification_zscores	PROTEIN_LEVEL	Z-SCORE	TRUE	Protein levels z-scores (mass spectrometry)	Protein levels z-scores (mass spectrometry)	
cna-foundation	FALSE	TRUE			data_cna_foundation.txt	org.mskcc.cbio.importer.converter.internal.CNAConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_cna_foundation.txt	cna	COPY_NUMBER_ALTERATION	DISCRETE	TRUE	Putative copy-number alterations from Foundation	Putative copy-number calls on <NUM_CASES> cases determined by Foundation Medicine.  Values: -2 = homozygous deletion; 2 = high level amplification.	
cna-rae	FALSE	TRUE			data_cna_rae.txt	org.mskcc.cbio.importer.converter.internal.CNAConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_cna_rae.txt	cna_rae	COPY_NUMBER_ALTERATION	DISCRETE	TRUE	Putative copy-number alterations from RAE	Putative copy-number calls on <NUM_CASES> cases determined using the RAE algorithm by Barry Taylor @MSKCC. Values: -2 = homozygous deletion; -1 = hemizygous deletion; 0 = neutral / no change; 1 = gain; 2 = high level amplification.	
cna-consensus	FALSE	TRUE			data_cna_consensus.txt	org.mskcc.cbio.importer.converter.internal.CNAConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_cna_consensus.txt	cna_consensus	COPY_NUMBER_ALTERATION	DISCRETE	TRUE	Putative copy-number alterations (Consensus)	Putative copy-number calls for genes which are the consensus of a combination of the Agilent 244k and Affymetrix SNP6 platforms and three different methods (RAE, GISTIC, GTS).	
cna-gistic	TRUE	TRUE		CopyNumber_Gistic2.Level_4:all_thresholded.by_genes.txt;	data_cna.txt	org.mskcc.cbio.importer.converter.internal.CNAConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_cna.txt	gistic	COPY_NUMBER_ALTERATION	DISCRETE	TRUE	Putative copy-number alterations from GISTIC	Putative copy-number calls on <NUM_CASES> cases determined using GISTIC 2.0. Values: -2 = homozygous deletion; -1 = hemizygous deletion; 0 = neutral / no change; 1 = gain; 2 = high level amplification.	
linear-cna-gistic	TRUE	TRUE		CopyNumber_Gistic2.Level_4:all_data_by_genes.txt;	data_linear_cna.txt	org.mskcc.cbio.importer.converter.internal.CNAConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_linear_cna.txt	linear_CNA	COPY_NUMBER_ALTERATION	CONTINUOUS	FALSE	Relative linear copy-number values	Relative linear copy-number values for each gene (from Affymetrix SNP6).	
log2-cna	TRUE	TRUE			data_log2_cna.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_log2_cna.txt	log2CNA	COPY_NUMBER_ALTERATION	LOG2-VALUE	FALSE	Log2 copy-number values	Log2 copy-number values for each gene.	
armlevel_cna	FALSE	FALSE			data_armlevel_cna.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_armlevel_cna.txt	armlevel_cna	GENERIC_ASSAY	CATEGORICAL	TRUE	Putative arm-level copy-number from GISTIC	Putative arm-level copy-number from GISTIC 2.0.	
drug-treatment-auc					data_drug_treatment_auc.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_drug_treatment_auc.txt	CCLE_drug_treatment_AUC	GENERIC_ASSAY	LIMIT-VALUE	TRUE	Treatment response: AUC	Treatment response: AUC	
drug-treatment-ic50					data_drug_treatment_ic50.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_drug_treatment_ic50.txt	CCLE_drug_treatment_IC50	GENERIC_ASSAY	LIMIT-VALUE	TRUE	Treatment response: IC50	Treatment response: IC50	
drug-treatment-ic50-zscores					data_drug_treatment_zscore.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_drug_treatment_zscore.txt	CCLE_drug_treatment_zscore	GENERIC_ASSAY	LIMIT-VALUE	TRUE	Treament response: z-score of IC50	Treament response: z-score of IC50	
mutation	TRUE	TRUE		Mutation_Packager_Calls.Level_3:<BARCODE>.maf.txt;Mutation_Packager_Raw_Calls.Level_3:<BARCODE>.maf.txt	data_mutations_uniprot_canonical_transcripts.txt	org.mskcc.cbio.importer.converter.internal.MutationConverterImpl	org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mutations_uniprot_canonical_transcripts.txt	mutations	MUTATION_EXTENDED	MAF	TRUE	Mutations	Mutation data from whole exome sequencing.	
mutation-other	FALSE	TRUE			data_mutations_uniprot_canonical_transcripts_*.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mutations_uniprot_canonical_transcripts.txt	mutations	MUTATION_EXTENDED	MAF	TRUE	Mutations	Mutation data from whole exome sequencing.	
mutation-foundation	FALSE	FALSE			data_mutations_uniprot_canonical_transcripts_foundation.txt		org.mskcc.cbio.portal.scripts.ImportProfileData	TRUE	meta_mutations_uniprot_canonical_transcripts.txt							
gene-panel-matrix	FALSE	TRUE			data_gene_matrix.txt		org.mskcc.cbio.portal.scripts.ImportGenePanelProfileMap	TRUE	meta_gene_matrix.txt							
gene-panel-matrix	FALSE	TRUE			data_gene_panel_matrix.txt		org.mskcc.cbio.portal.scripts.ImportGenePanelProfileMap	TRUE	meta_gene_panel_matrix.txt							
"""

"""
FILENAMES_IN_USE_BEFORE_UPDATE_LIST is a hardcoded list of old filenames which were recognized before the file renaming effort of mid-2021.
The merge.py and subset-dmp-data.sh scripts were working with studies limited to these filenames.
We have decided to allow running of the merge/subset scripts regardless of whether the prefix on seg file filenames matches the cancer study. Determining the actual cancer study in all use cases was not feasible.
"""
FILENAMES_IN_USE_BEFORE_UPDATE_LIST = [
    "*_data_cna_hg18.seg",
    "*_data_cna_hg19.seg",
    "*_meta_cna_hg18_seg.txt",
    "*_meta_cna_hg19_seg.txt",
    "data_CNA.txt",
    "data_CNA_RAE.txt",
    "data_CNA_consensus.txt",
    "data_CNA_foundation.txt",
    "data_RNA_Seq_expression_capture.txt",
    "data_RNA_Seq_expression_capture_Zscores.txt",
    "data_RNA_Seq_expression_capture_all_sample_Zscores.txt",
    "data_RNA_Seq_expression_cpm.txt",
    "data_RNA_Seq_expression_cpm_Zscores.txt",
    "data_RNA_Seq_expression_cpm_all_sample_Zscores.txt",
    "data_RNA_Seq_expression_median.txt",
    "data_RNA_Seq_expression_tpm.txt",
    "data_RNA_Seq_expression_tpm_Zscores.txt",
    "data_RNA_Seq_expression_tpm_all_sample_Zscores.txt",
    "data_RNA_Seq_mRNA_median_Zscores.txt",
    "data_RNA_Seq_mRNA_median_all_sample_Zscores.txt",
    "data_RNA_Seq_v2_expression_median.txt",
    "data_RNA_Seq_v2_mRNA_median_Zscores.txt",
    "data_RNA_Seq_v2_mRNA_median_Zscores_normals.txt",
    "data_RNA_Seq_v2_mRNA_median_all_sample_Zscores.txt",
    "data_RNA_Seq_v2_mRNA_median_all_sample_ref_normal_Zscores.txt",
    "data_RNA_Seq_v2_mRNA_median_normals.txt",
    "data_armlevel_CNA.txt",
    "data_bcr_clinical_data.txt",
    "data_bcr_clinical_data_*.txt",
    "data_clinical.txt",
    "data_clinical_caises.xml",
    "data_clinical_patient.txt",
    "data_clinical_sample.txt",
    "data_clinical_supp.txt",
    "data_clinical_supp_*.txt",
    "data_cna_hg18.seg",
    "data_cna_hg19.seg",
    "data_drug_treatment_AUC.txt",
    "data_drug_treatment_IC50.txt",
    "data_drug_treatment_ZSCORE.txt",
    "data_expression.txt",
    "data_expression_Zscores.txt",
    "data_expression_all_sample_Zscores.txt",
    "data_expression_median.txt",
    "data_expression_merged.txt",
    "data_expression_merged_Zscores.txt",
    "data_expression_merged_median_Zscores.txt",
    "data_expression_miRNA.txt",
    "data_expression_other_Zscores.txt",
    "data_fusions.txt",
    "data_fusions_gml.txt",
    "data_gene_matrix.txt",
    "data_gene_panel.txt",
    "data_gene_panel_*.txt",
    "data_gistic_genes_amp.txt",
    "data_gistic_genes_del.txt",
    "data_gsva_pvalues.txt",
    "data_gsva_scores.txt",
    "data_linear_CNA.txt",
    "data_log2CNA.txt",
    "data_mRNA_median_Zscores.txt",
    "data_mRNA_median_all_sample_Zscores.txt",
    "data_mRNA_outliers.txt",
    "data_mRNA_seq_fpkm.txt",
    "data_mRNA_seq_fpkm_capture.txt",
    "data_mRNA_seq_fpkm_capture_Zscores.txt",
    "data_mRNA_seq_fpkm_capture_all_sample_Zscores.txt",
    "data_mRNA_seq_fpkm_polya.txt",
    "data_mRNA_seq_fpkm_polya_Zscores.txt",
    "data_mRNA_seq_fpkm_polya_all_sample_Zscores.txt",
    "data_methylation_binary.txt",
    "data_methylation_genebodies_hmEPIC.txt",
    "data_methylation_genebodies_wgbs.txt",
    "data_methylation_hm27.txt",
    "data_methylation_hm450.txt",
    "data_methylation_promoters_hmEPIC.txt",
    "data_methylation_promoters_wgbs.txt",
    "data_miRNA_median_Zscores.txt",
    "data_microbiome.txt",
    "data_mrnaseq_fcount.txt",
    "data_mrnaseq_rsem.txt",
    "data_mutational_signature_contribution_v2.txt",
    "data_mutational_signature_contribution_v3.txt",
    "data_mutational_signature_pvalue_v2.txt",
    "data_mutational_signature_pvalue_v3.txt",
    "data_mutations_extended.txt",
    "data_mutations_extended_*.txt",
    "data_mutations_extended_foundation.txt",
    "data_mutations_germline.txt",
    "data_mutations_manual.txt",
    "data_mutations_nonsignedout.txt",
    "data_mutations_uncalled.txt",
    "data_mutsig.txt",
    "data_phosphoprotein_quantification.txt",
    "data_protein_quantification.txt",
    "data_protein_quantification_Zscores.txt",
    "data_protein_quantification_zscores.txt",
    "data_resource_definition.txt",
    "data_resource_sample.txt",
    "data_resource_study.txt",
    "data_rppa.txt",
    "data_rppa_Zscores.txt",
    "data_timeline.txt",
    "data_timeline_*.txt",
    "meta_CNA.txt",
    "meta_CNA_RAE.txt",
    "meta_CNA_consensus.txt",
    "meta_CNA_foundation.txt",
    "meta_RNA_Seq_expression_capture.txt",
    "meta_RNA_Seq_expression_capture_Zscores.txt",
    "meta_RNA_Seq_expression_capture_all_sample_Zscores.txt",
    "meta_RNA_Seq_expression_cpm.txt",
    "meta_RNA_Seq_expression_cpm_Zscores.txt",
    "meta_RNA_Seq_expression_cpm_all_sample_Zscores.txt",
    "meta_RNA_Seq_expression_median.txt",
    "meta_RNA_Seq_expression_tpm.txt",
    "meta_RNA_Seq_expression_tpm_Zscores.txt",
    "meta_RNA_Seq_expression_tpm_all_sample_Zscores.txt",
    "meta_RNA_Seq_mRNA_median_Zscores.txt",
    "meta_RNA_Seq_mRNA_median_all_sample_Zscores.txt",
    "meta_RNA_Seq_v2_expression_median.txt",
    "meta_RNA_Seq_v2_mRNA_median_Zscores.txt",
    "meta_RNA_Seq_v2_mRNA_median_Zscores_normals.txt",
    "meta_RNA_Seq_v2_mRNA_median_all_sample_Zscores.txt",
    "meta_RNA_Seq_v2_mRNA_median_all_sample_ref_normal_Zscores.txt",
    "meta_RNA_Seq_v2_mRNA_median_normals.txt",
    "meta_armlevel_CNA.txt",
    "meta_bcr_clinical.txt",
    "meta_bcr_clinical_other.txt",
    "meta_clinical.txt",
    "meta_clinical_caises.txt",
    "meta_clinical_patient.txt",
    "meta_clinical_sample.txt",
    "meta_cna_hg18_seg.txt",
    "meta_cna_hg19_seg.txt",
    "meta_drug_treatment_AUC.txt",
    "meta_drug_treatment_IC50.txt",
    "meta_drug_treatment_ZSCORE.txt",
    "meta_expression.txt",
    "meta_expression_Zscores.txt",
    "meta_expression_all_sample_Zscores.txt",
    "meta_expression_median.txt",
    "meta_expression_merged.txt",
    "meta_expression_merged_Zscores.txt",
    "meta_expression_merged_median_Zscores.txt",
    "meta_expression_miRNA.txt",
    "meta_expression_other_Zscores.txt",
    "meta_fusions.txt",
    "meta_fusions_gml.txt",
    "meta_gene_matrix.txt",
    "meta_gistic_genes_amp.txt",
    "meta_gistic_genes_del.txt",
    "meta_gsva_pvalues.txt",
    "meta_gsva_scores.txt",
    "meta_linear_CNA.txt",
    "meta_log2CNA.txt",
    "meta_mRNA_median_Zscores.txt",
    "meta_mRNA_median_all_sample_Zscores.txt",
    "meta_mRNA_outliers.txt",
    "meta_mRNA_seq_fpkm.txt",
    "meta_mRNA_seq_fpkm_capture.txt",
    "meta_mRNA_seq_fpkm_capture_Zscores.txt",
    "meta_mRNA_seq_fpkm_capture_all_sample_Zscores.txt",
    "meta_mRNA_seq_fpkm_polya.txt",
    "meta_mRNA_seq_fpkm_polya_Zscores.txt",
    "meta_mRNA_seq_fpkm_polya_all_sample_Zscores.txt",
    "meta_methylation_binary.txt",
    "meta_methylation_genebodies_hmEPIC.txt",
    "meta_methylation_genebodies_wgbs.txt",
    "meta_methylation_hm27.txt",
    "meta_methylation_hm450.txt",
    "meta_methylation_promoters_hmEPIC.txt",
    "meta_methylation_promoters_wgbs.txt",
    "meta_miRNA_median_Zscores.txt",
    "meta_microbiome.txt",
    "meta_mrnaseq_fcount.txt",
    "meta_mrnaseq_rsem.txt",
    "meta_mutational_signature_contribution_v2.txt",
    "meta_mutational_signature_contribution_v3.txt",
    "meta_mutational_signature_pvalue_v2.txt",
    "meta_mutational_signature_pvalue_v3.txt",
    "meta_mutations_extended.txt",
    "meta_mutations_uncalled.txt",
    "meta_mutsig.txt",
    "meta_phosphoprotein_quantification.txt",
    "meta_protein_quantification.txt",
    "meta_protein_quantification_Zscores.txt",
    "meta_protein_quantification_zscores.txt",
    "meta_resource_definition.txt",
    "meta_resource_sample.txt",
    "meta_resource_study.txt",
    "meta_rppa.txt",
    "meta_rppa_Zscores.txt",
    "meta_timeline.txt"
    ]

def filenames_in_use_before_update():
    """
    this processes the items in FILENAMES_IN_USE_BEFORE_UPDATE_LIST, separating the simple filenames from the wildcard patterns, and returning two results.
    """
    filenames_in_use_before_update_set = set()
    wildcard_patterns_in_use_before_update_set = set()
    for filename in FILENAMES_IN_USE_BEFORE_UPDATE_LIST:
        if "*" in filename:
            wildcard_patterns_in_use_before_update_set.add(filename)
        else:
            filenames_in_use_before_update_set.add(filename)
    return filenames_in_use_before_update_set, wildcard_patterns_in_use_before_update_set

def parse_current_datatypes():
    """
    the contents of the datatype sheet (in tab separated format) are parsed and all filenames (both meta and staging) or wildcard patterns are mapped to their associated datatypes. The maps are returned.

    wildcard parterns are returned in a separate dictionary, so that matching can be done using wildcard substitution (rather than exact string match) example:
        - data_mutations_uniprot_canonical_transcripts_*.txt is a newly introduced wildcard pattern. In this function, it will be added to the map wildcard_pattern2datatype
            wildcard_pattern2datatype['data_mutations_uniprot_canonical_transcripts_*.txt'] = 'mutation-other'
        - later, while scanning files, this wildcard pattern will be "split" into two parts (see split_wildcard_patterns()) .. the prefix before the asterisk and the suffix after:
            {'prefix' : 'data_mutations_uniprot_canonical_transcripts_', 'suffix' : '.txt'}
        - testing for matching files (from the cancer study source directories) will be done using comparisons to these prefix/suffix pairs
    """
    datatype_sheet_lines = DATATYPE_SHEET_DOWNLOAD_TAB_SEPARATED.splitlines()
    filename2datatype = {}
    wildcard_pattern2datatype = {}
    header_skipped = False
    for line in datatype_sheet_lines:
        if not header_skipped:
            if line.startswith("datatype"):
                header_skipped = True
            continue
        fields = line.split("\t")
        datatype = fields[0].strip()
        staging_filename = fields[5].strip()
        meta_filename = fields[9].strip()
        if "*" in staging_filename:
            wildcard_pattern2datatype[staging_filename] = datatype
        else:
            filename2datatype[staging_filename] = datatype
        if "*" in meta_filename:
            wildcard_pattern2datatype[meta_filename] = datatype
        else:
            filename2datatype[meta_filename] = datatype
    return filename2datatype, wildcard_pattern2datatype

def is_normal_sample_filename(filename):
    """
    examines filename to see if the importer would skip it as a "normal sample" file.
    """
    normal_sample_suffixes = ["_normals.txt", "data_mrna_seq_v2_rsem_normal_samples.txt", "data_mrna_seq_v2_rsem_normal_samples_zscores_ref_normal_samples.txt"]
    for suffix in normal_sample_suffixes:
        if filename.endswith(suffix):
            return True;
    return False;

def split_wildcard_patterns(wildcard_patterns):
    """
    breaks a list of wildcard_patterns (each containing a single asterisk) into two parts, returning a list of dictionaries with prefix/suffix.
    """
    split_wildcard_pattern_list = []
    for wildcard_pattern in wildcard_patterns:
        pattern_segments = wildcard_pattern.strip().split("*")
        if len(pattern_segments) != 2:
            sys.stderr.write("Error : wildcard filename pattern '" + wildcard_pattern + "' does not have exactly one asterisk\n")
            sys.exit(1)
        split_wildcard_pattern = {"prefix" : pattern_segments[0], "suffix" : pattern_segments[1]}
        split_wildcard_pattern_list.append(split_wildcard_pattern)
    return split_wildcard_pattern_list

def matches_wildcard_pattern(filename, split_wildcard_pattern):
    """
    returns True if filename starts with the prefix from, and ends with the suffix from, the split_wildcard_pattern dictionary.

    example : when split_wildcard_pattern is {'prefix' : 'data_mutations_uniprot_canonical_transcripts_', 'suffix' : '.txt'} this function will return:
        - True for a file such as "data_mutations_uniprot_canonical_transcripts_uniprot.txt", and
        - False for a file such as "meta_mutations_uniprot_canonical_transcripts_uniprot.txt"
    """
    return filename.startswith(split_wildcard_pattern["prefix"]) and filename.endswith(split_wildcard_pattern["suffix"])

def matches_any_wildcard_pattern(filename, split_wildcard_patterns):
    """
    iterates through all pattern dictionaries, looking for any match for filename.
    """
    for split_wildcard_pattern in split_wildcard_patterns:
        if matches_wildcard_pattern(filename, split_wildcard_pattern):
            return True
    return False

def study_directory_uses_original_filenames(path):
    """
    determines if unsupported filename patterns (those added June 2021) are present or not in a study directory

    search directory at <path> - return False if any unsupported filenames are present, which:
        - are not of the original set of filenames we recognized before the renaming effort, and
        - would be imported according to the current datatypes sheet, and
        - are not "normal sample" files (which are ignored by the importer)

    arguments:
    path -- the (absolute or relative) filename path to the directory which should be examined
    """
    new_filenames_present = False
    if os.path.isdir(path):
        path = path + os.path.sep
    basedir = os.path.dirname(path)
    filenames = os.listdir(path)
    original_datatype_filenames, original_wildcard_patterns = filenames_in_use_before_update()
    current_datatype_filenames, current_wildcard_patterns = parse_current_datatypes()
    split_original_wildcard_patterns = split_wildcard_patterns(original_wildcard_patterns)
    split_current_wildcard_patterns = split_wildcard_patterns(current_wildcard_patterns)
    for filename in filenames:
        filepath = os.path.join(basedir, filename)
        if not os.path.isfile(filepath):
            # ignore subdirectories
            continue
        if filename in original_datatype_filenames or matches_any_wildcard_pattern(filename, split_original_wildcard_patterns):
            # ignore original filenames and matches to any original wildcard pattern
            continue
        if not filename in current_datatype_filenames and not matches_any_wildcard_pattern(filename, split_current_wildcard_patterns):
            # filename/pattern was not found in current list - so would be skipped by importer
            continue
        if is_normal_sample_filename(filename):
            # normal sample files are skipped in importer
            continue
        print("file '%s' is an updated filename for the datatype" % filename)
        new_filenames_present = True
    return not new_filenames_present

def main(args):
    """
    scans a study directory for any novel filenames which are not yet supported by the merge scripts
    returns non-zero if novel/updated/unsupported filenames are preset, 0 otherwise
    """
    if study_directory_uses_original_filenames(args.cancer_study_path):
        sys.exit(0);
    else:
        sys.exit(1);

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description = 'scans a study directory for any novel filenames which are not yet supported by the merge scripts - exits with non-0 if any are found')
    parser.add_argument("-d", "--directory_of_study", action="store", required=True, dest="cancer_study_path")
    args = parser.parse_args()
    main(args)

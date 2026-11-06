# MAF preprocessing scope

Sophia (`update-sophia-mskimpact.sh`) and AZ (`update-az-mskimpact.sh`) call
`remove-duplicate-maf-variants.py` before Java import. This PR ports that script
to Python 3 while preserving its existing selection algorithm. It does not fix
VAF selection or change either caller to a different deduplication policy.

The duplicate key is an exact, case-sensitive match of eight raw columns:
`Entrez_Gene_Id`, `Chromosome`, `Start_Position`, `End_Position`,
`Variant_Classification`, `Tumor_Seq_Allele2`, `Tumor_Sample_Barcode`, and
`HGVSp_Short`. Rows are grouped in memory. Centers are never merged.

| Input within a group | Current output |
| --- | --- |
| One row, including invalid counts | That row unchanged |
| Duplicate rows with alt/ref counts 11/10 and 12/10 | Neither row: integer quotients are both zero |
| Duplicate rows with alt/ref counts 1/0 and 5/0 | First row: both quotients are one |
| Invalid alternate or reference count | That row, with an error diagnostic |
| Zero alternate and reference counts | That row, with an error diagnostic |
| A selected row followed by an invalid-count row | Both rows |

For duplicates, the algorithm starts the maximum at zero, computes
`alt // (alt + ref)`, and immediately emits each row that raises that maximum.
It does not reliably select one row with the largest VAF. The regression tests
characterize this behavior so a Python migration cannot silently change it.

The proposed `merge-duplicate-maf-variants.py` has been removed from this PR.
It had no caller or agreed preprocessing requirement. Although its selection
preferences borrowed from Java, its eight raw key columns did not reproduce
Java mutation equality. It therefore could not establish parity with the Java
importer. Java continues to perform its own mutation merging downstream.

Before changing Sophia's selection behavior, coordinate with Calla and define
the desired duplicate key, treatment of invalid counts, and expected retained
rows. Any replacement or VAF correction belongs in that separately agreed work.

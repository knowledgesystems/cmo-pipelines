# run all python3 unit tests with:
#     import-scripts> python3 -m unittest discover test-py3
import os
import shutil
import tempfile
import unittest

import remove_duplicate_maf_variants as dedup

HEADER = "Hugo_Symbol\tEntrez_Gene_Id\tChromosome\tStart_Position\tEnd_Position\tVariant_Classification\tTumor_Seq_Allele2\tTumor_Sample_Barcode\tHGVSp_Short\tt_ref_count\tt_alt_count\n"
# same variant in S1 three times (VAF 0.1, 0.5, 0.3), one distinct variant, and an unparseable-count duplicate pair
RECORDS = [
    "TP53\t7157\t17\t100\t100\tMissense_Mutation\tA\tS1\tp.R1C\t90\t10\n",
    "KRAS\t3845\t12\t200\t200\tMissense_Mutation\tT\tS1\tp.G12D\t50\t50\n",
    "TP53\t7157\t17\t100\t100\tMissense_Mutation\tA\tS1\tp.R1C\t50\t50\n",
    "TP53\t7157\t17\t100\t100\tMissense_Mutation\tA\tS1\tp.R1C\t70\t30\n",
    "BRAF\t673\t7\t300\t300\tMissense_Mutation\tG\tS2\tp.V600E\tNA\tNA\n",
    "BRAF\t673\t7\t300\t300\tMissense_Mutation\tG\tS2 \tp.V600E\t.\t.\n",
]


class TestRemoveDuplicateMafVariants(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.mkdtemp()
        self.input_filename = os.path.join(self.temp_dir, "in.maf")
        self.output_filename = os.path.join(self.temp_dir, "out.maf")
        with open(self.input_filename, "w") as maf_file:
            maf_file.write("#version 2.4\n")
            maf_file.write(HEADER)
            maf_file.writelines(RECORDS)

    def tearDown(self):
        shutil.rmtree(self.temp_dir)

    def read_output(self, filename=None):
        with open(filename or self.output_filename) as maf_file:
            return maf_file.read().split("\n")

    def test_strategy_vaf_keeps_highest_vaf_once_in_input_order(self):
        dropped = dedup.process_maf_file(self.input_filename, self.output_filename, dedup.STRATEGY_VAF)
        self.assertEqual(3, dropped)
        lines = self.read_output()
        self.assertEqual("#version 2.4", lines[0])
        self.assertEqual(HEADER.rstrip("\n"), lines[1])
        self.assertEqual([RECORDS[2].rstrip("\n"), RECORDS[1].rstrip("\n"), RECORDS[4].rstrip("\n")], lines[2:5])

    def test_strategy_first_keeps_first_record(self):
        dropped = dedup.process_maf_file(self.input_filename, self.output_filename, dedup.STRATEGY_FIRST)
        self.assertEqual(3, dropped)
        self.assertEqual([RECORDS[0].rstrip("\n"), RECORDS[1].rstrip("\n"), RECORDS[4].rstrip("\n")], self.read_output()[2:5])

    def test_in_place_rewrite(self):
        dedup.process_maf_file(self.input_filename, self.input_filename, dedup.STRATEGY_FIRST)
        self.assertEqual(3, len([line for line in self.read_output(self.input_filename)[2:] if line]))

    def test_strategy_first_does_not_need_count_columns(self):
        with open(self.input_filename, "w") as maf_file:
            maf_file.write(HEADER.replace("\tt_ref_count\tt_alt_count", ""))
            for record in RECORDS:
                maf_file.write("\t".join(record.split("\t")[:9]) + "\n")
        self.assertEqual(3, dedup.process_maf_file(self.input_filename, self.output_filename, dedup.STRATEGY_FIRST))

    def test_strategy_vaf_needs_count_columns(self):
        with open(self.input_filename, "w") as maf_file:
            maf_file.write(HEADER.replace("\tt_ref_count\tt_alt_count", ""))
        with self.assertRaises(SystemExit):
            dedup.process_maf_file(self.input_filename, self.output_filename, dedup.STRATEGY_VAF)


if __name__ == "__main__":
    unittest.main()

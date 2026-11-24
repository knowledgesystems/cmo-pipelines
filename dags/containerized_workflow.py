"""Ordering shared by the public and HGNC worker configurations."""


def wire_import(ready, clone, load, derive, transfer, complete):
    ready >> clone >> load >> derive >> transfer >> complete

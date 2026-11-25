"""Stage ordering and skip options for the containerized import DAG."""


# Task IDs that may be listed in the skip_tasks param (dry-run support).
SKIPPABLE_TASK_IDS = (
    "clone_live_database_into_standby",
    "import_into_standby_database",
    "create_derived_tables_in_standby_database",
    "transfer_deployment_color",
)


def wire_import(ready, clone, load, derive, transfer, complete):
    ready >> clone >> load >> derive >> transfer >> complete

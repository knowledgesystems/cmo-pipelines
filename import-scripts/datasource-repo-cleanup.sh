#!/bin/bash

# Datasource Repo Cleanup input argument
# (1) space-delmited list of repository names to cleanup

# user input repository list
REPOSITORIES="$@"

source /data/portal-cron/scripts/automation-environment.sh

# -----------------------------------------------------------------------------------------------------------
# GLOBALS

EMAIL_LIST="cbioportal-pipelines@cbioportal.org"
GITHUB_MANAGER="github"
DEFAULT_REPOSITORY_MANAGER="UNKNOWN"

# keep track of invalid repositories for notification email
REPOSITORY_CLEANUP_ERRORS=""

# -----------------------------------------------------------------------------------------------------------
## FUNCTIONS

# Function for cleaning up github repository
function cleanupGithubRepository {
    # (1) GITHUB_REPO_PATH : github repository path
    GITHUB_REPO_PATH=$1

    cd $GITHUB_REPO_PATH
    current_github_branch=$(git rev-parse --abbrev-ref HEAD)

    echo "git checkout -- ."
    git checkout -- . ; return_value=$?
    if [ $return_value -gt 0 ] ; then
        return $return_value
    fi
    echo "git clean -fd"
    git clean -fd ; return_value=$?
    if [ $return_value -gt 0 ] ; then
        return $return_value
    fi
    echo "git reset --hard origin/$current_github_branch"
    git reset --hard origin/$current_github_branch ; return_value=$?
    if [ $return_value -gt 0 ] ; then
        return $return_value
    fi
    echo "git lfs prune"
    git lfs prune ; return_value=$?
    if [ $return_value -gt 0 ] ; then
        return $return_value
    fi
    return $return_value
}

# Function for determining repository manager type
function determineRepositoryManager {
    # (1) REPO_PATH : repository path

    REPO_PATH=$1
    cd $REPO_PATH
    git rev-parse --is-inside-work-tree > /dev/null 2>&1
    if [ $? -eq 0 ] ; then
        repository_manager="$GITHUB_MANAGER"
        return
    fi
}

# Function to print usage statement and exit
function printUsageAndExit {
    echo "bash datasource-repo-cleanup.sh repository1 [repository2] ... "
    exit 1
}

# -----------------------------------------------------------------------------------------------------------
## CHECK ENVIRONMENT VARIABLES
if [ -z $HG_BINARY ] | [ -z $PORTAL_DATA_HOME ] ; then
    MESSAGE="Could not run datasource-repo-cleanup.sh: automation-environment.sh script must be run in order to set needed environment variables (like PORTAL_DATA_HOME, ...)"
    echo $MESSAGE
    echo -e "$MESSAGE" |  mail -s "datasource-repo-cleanup.sh failed to run." $EMAIL_LIST
    exit 2
fi

# -----------------------------------------------------------------------------------------------------------
## CLEAN UP REPOSITORIES
echo $(date)
if [ -z "$REPOSITORIES" ] ; then
    echo "ERROR: must provide at least one repository to clean up!" ; printUsageAndExit
else
    # loop through repositories and run repository cleanup function based on repo type
    for repository in $REPOSITORIES ; do
        repository_manager="$DEFAULT_REPOSITORY_MANAGER"
        return_value=0

        echo "Cleaning up repository: $repository..."
        if [ -d $repository ] ; then
            # determine repository manager and run corresponding cleanup commands
            determineRepositoryManager $repository
            echo -e "\t---> Repository manager = $repository_manager"
            case "$repository_manager" in
                $GITHUB_MANAGER)
                    cleanupGithubRepository $repository ; return_value=$?
                    ;;
                *)
                    echo -e "ERROR: cannot determine the repository type!"
                    REPOSITORY_CLEANUP_ERRORS="$REPOSITORY_CLEANUP_ERRORS\n\t$repository (unknown repository type)"
                    ;;
            esac
            # log any issues with cleanup of current repository
            if [ $return_value -gt 0 ] ; then
                echo -e "ERROR: Something went wrong during cleanup!"
                REPOSITORY_CLEANUP_ERRORS="$REPOSITORY_CLEANUP_ERRORS\n\t$repository (error during cleanup attempt)"
            fi
        else
            echo "Repository does not exist! Skipping cleanup of $repository..."
            REPOSITORY_CLEANUP_ERRORS="$REPOSITORY_CLEANUP_ERRORS\n\t$repository (does not exist)"
        fi
        echo
    done

    if [ ! -z "$REPOSITORY_CLEANUP_ERRORS" ] ; then
        MESSAGE="datasource-repo-cleanup.sh errors:\n$REPOSITORY_CLEANUP_ERRORS"
        echo -e "Sending email:\n$MESSAGE"
        echo -e "$MESSAGE" | mail -s "[ERROR] datasource-repo-cleanup.sh" $EMAIL_LIST
    fi
    exit 0
fi

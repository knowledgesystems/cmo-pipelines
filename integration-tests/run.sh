#!/bin/bash

echo "Running integration tests"
echo "GIT_BRANCH is '$GIT_BRANCH'"
if [[ -n "$GIT_BRANCH" ]]
then
    if [[ "$GIT_BRANCH" =~ ^origin/pull/([0-9]+)/head$ ]]
    then
        echo "Run tests on PR '${BASH_REMATCH[1]}'!"
    else
        # TODO what?
        echo "What do we want to do with branch '$GIT_BRANCH'?"
    fi
else
    # TODO do we fail?
    echo "Do not have GIT_BRANCH, nothing to do"
fi


#!/bin/bash

# set -euo pipefail

# Prepare mkdocs
python3 -m pip install -r requirements.txt

# Generate CDF Doc Pages
cd cdf
./cdf generate --target mkdocs ../cdp_events --output ../generated --usage usage.json

cd ../generated/mkdocs
python3 -m mkdocs build --clean --strict

COMMIT_HASH=$(git rev-parse --short HEAD)
export GIT_CREDENTIALS_TOKEN_NAME=write

# This is an adaptation of the git commands used here: https://github.com/JamesIves/github-pages-deploy-action
# We keep the gh-pages-event-browser branch orphaned and limit it to only a single commit to keep the repo size down
git worktree add --no-checkout --detach tmp
cd tmp # ./generated/mkdocs/tmp

# Set up git user
git config user.email "marketing-automation@squareup.com"
git config user.name "CDP Github Bot"

git checkout origin/gh-pages-event-browser --orphan gh-pages-event-browser
git reset --hard
cp -r ../site/* .

git add .
git commit -m "Deploying docs from @ $COMMIT_HASH"
git push -u origin gh-pages-event-browser:gh-pages-event-browser --force

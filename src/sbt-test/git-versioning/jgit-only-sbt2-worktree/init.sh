#!/usr/bin/env bash

set -eux

# Build this scripted layout:
#
#   .
#   |-- main/     primary Git repository
#   `-- project/  linked worktree containing the sbt build under test
#
# The directory name "project" is intentional. In sbt, `project/` is the
# meta-build of the scripted root. The scripted test runs `reload plugins`
# so subsequent commands execute from inside this linked worktree.
mkdir -p main

cd main
git init
git config user.email "test@jsuereth.com"
git config user.name "Tester"
git commit --allow-empty --no-gpg-sign -m "Initial commit"
rm -rf ../project
git worktree add -b wt ../project
cd -

cp -r template-build/. project/
cd project
git add .gitignore build.sbt project/plugins.sbt
git commit --no-gpg-sign -m "Add scripted project"
echo "# dirty" >> .gitignore

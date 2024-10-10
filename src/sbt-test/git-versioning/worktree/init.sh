#!/usr/bin/env bash

set -eux

mkdir -p main

cd main
git init
git commit --allow-empty -m "Initial commit"
git worktree add ../project
cd -

cp -r .project/project project

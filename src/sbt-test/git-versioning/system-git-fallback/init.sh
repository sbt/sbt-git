#!/usr/bin/env bash

set -eux

git init
git config user.email "test@jsuereth.com"
git config user.name "Tester"
git add build.sbt project/plugins.sbt
git commit --no-gpg-sign -m "Add scripted project"

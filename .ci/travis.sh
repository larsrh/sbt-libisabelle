#!/bin/bash

set -ex

curl -s https://raw.githubusercontent.com/paulp/sbt-extras/master/sbt > sbt
chmod 0755 sbt

./sbt scripted

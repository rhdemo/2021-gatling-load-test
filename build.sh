#!/usr/bin/env bash
set -e

SBT_VERSION="1.4.7"
# GATLING_KAFKA_COMMIT="2deebc5c8a30d94283f0120eb7fd0fe4802aaeda"

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

mkdir -p $DIR/target/gatling/reports

pushd $DIR/target > /dev/null
if [ ! -d sbt ]; then
  echo "> Downloading sbt"
  wget https://github.com/sbt/sbt/releases/download/v1.4.7/sbt-1.4.7.tgz
  tar xzf sbt-1.4.7.tgz
  rm sbt-1.4.7.tgz
fi
if [ ! -d gatling-kafka ]; then
  echo "> Cloning gatling-kafka"
  git clone https://github.com/mnogu/gatling-kafka.git
fi

pushd gatling-kafka > /dev/null
echo $(git rev-parse HEAD)
# if [ "$(git rev-parse HEAD)" != "$GATLING_KAFKA_COMMIT" ]; then
#   rm -rf target
#   git checkout $GATLING_KAFKA_COMMIT
# fi
if [ ! -d target ]; then
  echo "> Compiling gatling-kafka"
  ../sbt/bin/sbt assembly
fi
popd > /dev/null #gatling-kafka

popd > /dev/null #target


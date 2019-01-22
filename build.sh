#!/bin/sh

NEED_CLEAN=${1:-yes}
if [ "X$NEED_CLEAN" == "Xyes" ]; then
	./gradlew clean || exit -1
fi

./gradlew jar

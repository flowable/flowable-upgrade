#!/bin/bash
# When running this script make sure that the JDBC properties in the pom.xml file are set to the correct database
# Or do export MAVEN_CONFIG="-Djdbc.url=<jdbcUrl> -Djdbc.driver=<jdbcDriver> -Djdbc.username=flowable -Djdbc.password=flowable" before running the script
if [[ "$2" == "" ]] ; then

   	echo "Missing argument: correct usage <./run-upgrade-test.sh oldVersion newVersion>"
   	exit 1
fi

OLD_VERSION=$1
NEW_VERSION=$2

echo "Running upgrade test for Flowable version $OLD_VERSION to $NEW_VERSION"

OLD_VERSION_MODULE="flowable-upgrade-$OLD_VERSION"
NEW_VERSION_MODULE="flowable-upgrade-$NEW_VERSION"

echo "Installing upgrade project for modules ${OLD_VERSION_MODULE} and ${NEW_VERSION_MODULE}"
./mvnw -T 1C clean install -DskipTests -pl $OLD_VERSION_MODULE,$NEW_VERSION_NEW_VERSION_MODULE -am

if [ $? -ne 0 ]; then
    exit 1
fi

echo "Generate Data for version $OLD_VERSION"

./mvnw -DoldVersion=$OLD_VERSION -Dmaven.test.skip=true -DgenerateData=true clean test -f flowable-upgrade-${OLD_VERSION}/pom.xml

if [ $? -ne 0 ]; then
    exit 1
fi

echo "Running $NEW_VERSION unit tests against $OLD_VERSION"

./mvnw -DoldVersion=$OLD_VERSION clean test -f flowable-upgrade-${NEW_VERSION}/pom.xml

if [ $? -ne 0 ]; then
    exit 1
fi

#!/bin/bash

mvn jar:jar

mvn install:install-file -Dfile=target/femtozip-1.0.jar -DgroupId=org.toubassi -DartifactId=femtozip -Dversion=1.0 -D packaging=jar
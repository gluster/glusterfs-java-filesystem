#!/bin/bash
for i in `find . -path '**/surefire-reports/**'`; do
    echo
    echo
    echo $i
    cat $i
done
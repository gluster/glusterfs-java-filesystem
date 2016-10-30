#!/bin/bash
echo
echo '*** LIBGFAPI Client Log:'
cat /tmp/glfsjni.log
echo '***'

for i in `find . -path '**/surefire-reports/**.txt'`; do
    echo
    echo "*** MVN LOG: $i"
    cat $i
    echo '***'
done
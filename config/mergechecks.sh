#!/bin/bash

count=$(cat sun_checks.xml | wc -l)
head -n "$(($count-3))" sun_checks.xml > checkstyle-merged.xml

printf "\n    <!-- End sun_checks.xml -->\n\n" >> checkstyle-merged.xml

count=$(cat google_checks.xml | wc -l)
tail -n "$(($count-49))" google_checks.xml | head -n "$(($count-51))" >> checkstyle-merged.xml

printf "\n    <!-- End google_checks.xml -->\n\n" >> checkstyle-merged.xml

count=$(cat custom_checks.xml | wc -l)
tail -n "$(($count-5))" custom_checks.xml >> checkstyle-merged.xml
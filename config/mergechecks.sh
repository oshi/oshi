#!/bin/bash
#
# MIT License
#
# Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#


count=$(cat sun_checks.xml | wc -l)
head -n "$(($count-3))" sun_checks.xml > checkstyle-merged.xml

printf "\n    <!-- End sun_checks.xml -->\n\n" >> checkstyle-merged.xml

count=$(cat google_checks.xml | wc -l)
tail -n "$(($count-49))" google_checks.xml | head -n "$(($count-51))" >> checkstyle-merged.xml

printf "\n    <!-- End google_checks.xml -->\n\n" >> checkstyle-merged.xml

count=$(cat custom_checks.xml | wc -l)
tail -n "$(($count-6))" custom_checks.xml >> checkstyle-merged.xml
#!/bin/bash -xe

################################################################################
# This script is used as a method of adding UTF-8 characters to the Info.plist #
# file of the Mac application. It is required because ant doesn't support      #
# setting the LC_CTYPE before running sed                                      #
################################################################################
env

export LC_CTYPE=UTF-8

sed -i "" -f "$1/fixAsciiInfoPlist.txt" "$2/$3.app/Contents/Info.plist"


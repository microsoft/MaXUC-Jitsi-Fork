# Copyright (c) Microsoft Corporation. All rights reserved.

# Note this contains untested changes.  The previous version of the Makefile needed a number of changes just to get the
# library built. The current version creates a .jnilib on Mac6933 as of 27/01/22, but the resultant library hasn't been tested.
CC = cc -O2
TARGET_BASENAME = jmacosxaddrbook

JAVA_HOME = /System/Library/Frameworks/JavaVM.framework/Versions/Current
JDK_HOME=/Library/Java/JavaVirtualMachines/jdk-11.0.11+9/Contents/Home/

CC := $(CC) -arch x86_64 -mmacosx-version-min=10.4
CPPFLAGS = -Wall -Wreturn-type -DJNI_IMPLEMENTATION -I$(JAVA_HOME)/Headers -I$(JDK_HOME)/include/  -I$(JDK_HOME)/include/darwin/
LDFLAGS = -dynamiclib
LIBS = -framework AddressBook -framework Foundation
TARGET = ../../../../lib/native/mac/lib$(TARGET_BASENAME).jnilib

$(TARGET): \
		AddrBookContactQuery.c \
		net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils.m
	$(CC) $(CPPFLAGS) $^ $(LDFLAGS) -o $@ $(LIBS)
	-strip $(TARGET)

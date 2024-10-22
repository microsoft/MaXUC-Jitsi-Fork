# Author: Romain KUNTZ
# Portions (c) Microsoft Corporation. All rights reserved.
# Requires the Sparkle.framework installed in $(SYSTEM_ARTIFACTSDIRECTORY)/Library/Frameworks
# export MACOSX_DEPLOYMENT_TARGET=10.7

CC=gcc -arch x86_64 -mmacosx-version-min=10.13
TARGET=libsparkle_init.dylib
JAVA_HOME=`/usr/libexec/java_home`
JNI_INCLUDE_PATH=$(JAVA_HOME)/include
JNI_INCLUDE_PATH_DARWIN=$(JNI_INCLUDE_PATH)/darwin
CFLAGS=-I$(JNI_INCLUDE_PATH) -I$(JNI_INCLUDE_PATH_DARWIN) -I$(SYSTEM_ARTIFACTSDIRECTORY)/Library/Frameworks/Sparkle.framework/Versions/Current/Headers/ -F $(SYSTEM_ARTIFACTSDIRECTORY)/Library/Frameworks
LIBS=-framework AppKit -framework Foundation -framework Sparkle
OBJS=net_java_sip_communicator_impl_sparkle_SparkleActivator.o

all:$(TARGET)

clean:
	rm -rf $(TARGET) $(OBJS) *~

install:$(TARGET)
	cp $(TARGET) ../../../../lib/native/mac/
	rm $(TARGET)

libsparkle_init.dylib:$(OBJS)
	$(CC) -dynamiclib -o $(TARGET) $(LIBS) $(CFLAGS) $<
	install_name_tool -add_rpath "@loader_path/../Frameworks" libsparkle_init.dylib


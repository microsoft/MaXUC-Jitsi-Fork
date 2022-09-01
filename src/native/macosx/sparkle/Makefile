# Author: Romain KUNTZ
# Requires the Sparkle.framework installed in /Library/Frameworks
# The Framework is available at http://sparkle.andymatuschak.org/
# export MACOSX_DEPLOYMENT_TARGET=10.7

CC=gcc -arch x86_64 -mmacosx-version-min=10.7
TARGET=libsparkle_init.dylib
JAVA_HOME=`/usr/libexec/java_home`
JNI_INCLUDE_PATH=$(JAVA_HOME)/include
JNI_INCLUDE_PATH_DARWIN=$(JNI_INCLUDE_PATH)/darwin
CFLAGS=-I$(JNI_INCLUDE_PATH) -I$(JNI_INCLUDE_PATH_DARWIN) -I/Library/Frameworks/Sparkle.framework/Versions/A/Headers/ -F /Library/Frameworks
LIBS=-framework AppKit -framework Foundation -framework Sparkle
OBJS=net_java_sip_communicator_impl_sparkle_SparkleActivator.o

all:$(TARGET)

clean:
	rm -rf $(TARGET) $(OBJS) *~

install:$(TARGET)
	cp $(TARGET) ../../../../lib/native/mac/

libsparkle_init.dylib:$(OBJS)
	$(CC) -dynamiclib -o $(TARGET) $(LIBS) $(CFLAGS) $<
	install_name_tool -add_rpath "@loader_path/../Frameworks" libsparkle_init.dylib


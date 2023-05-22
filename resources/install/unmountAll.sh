# Copyright (c) Microsoft Corporation. All rights reserved.

hdiutil info | grep Accession | awk '{print $1}' | grep dev  > mounts.txt

while read mount; do
 echo Unmounting $mount
 hdiutil unmount $mount
done < mounts.txt


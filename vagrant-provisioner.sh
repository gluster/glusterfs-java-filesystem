#!/bin/bash

add-apt-repository -y ppa:gluster/glusterfs-3.8
apt-get update
# apt-get -y dist-upgrade
apt-get -y install glusterfs-server
sleep 2

sed -i 's/\(end-volume\)/    option rpc-auth-allow-insecure on\n\1/' /etc/glusterfs/glusterd.vol
service glusterfs-server restart
sleep 2

gluster volume create foo ${1}:/var/tmp/foo force
gluster volume set foo server.allow-insecure on
gluster volume start foo
sleep 2

mkdir -v /mnt/foo
mount -t glusterfs localhost:foo /mnt/foo && echo Mounted glusterfs volume at /mnt/foo.
chmod -v ugo+w /mnt/foo

echo Provision complete.

#!/bin/bash

add-apt-repository -y ppa:gluster/glusterfs-3.4
apt-get update
# apt-get -y dist-upgrade
apt-get -y install glusterfs-server

sed -i 's/\(end-volume\)/    option rpc-auth-allow-insecure on\n\1/' /etc/glusterfs/glusterd.vol
service glusterfs-server restart
gluster volume create foo ${1}:/var/tmp/foo force
gluster volume set foo server.allow-insecure on
gluster volume start foo
mkdir /mnt/foo
mount -t glusterfs localhost:foo /mnt/foo
chmod ugo+w /mnt/foo


dnl ---------------------------------------------------------------------------
dnl  Copyright (c) 2013 Louis Zuckerman All rights reserved.
dnl  
dnl  Redistribution and use in source and binary forms, with or without
dnl  modification, are permitted provided that the following conditions are
dnl  met:
dnl  
dnl     * Redistributions of source code must retain the above copyright
dnl  notice, this list of conditions and the following disclaimer.
dnl     * Redistributions in binary form must reproduce the above
dnl  copyright notice, this list of conditions and the following disclaimer
dnl  in the documentation and/or other materials provided with the
dnl  distribution.
dnl     * Neither the names of the authors nor the names of 
dnl  contributors may be used to endorse or promote products derived from
dnl  this software without specific prior written permission.
dnl  
dnl  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
dnl  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
dnl  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
dnl  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
dnl  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
dnl  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
dnl  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
dnl  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
dnl  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
dnl  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
dnl  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
dnl  
dnl  =======
dnl  
dnl  This file, was derived from the leveldbjni project with significant help from 
dnl  Hiram Chirino (thanks, Hiram!).  The leveldbjni project is hosted on github:
dnl  https://github.com/fusesource/leveldbjni
dnl ---------------------------------------------------------------------------

AC_DEFUN([CUSTOM_M4_SETUP],
[
  AC_ARG_WITH([glfs],
    [AS_HELP_STRING([--with-glfs@<:@=PATH@:>@],
    [Directory where glfs was built. Example: --with-glfs=/opt/glfs])],
    [
      if test "$withval" = "no" || test "$withval" = "yes"; then
        AC_MSG_ERROR([--with-glfs: PATH to Gluster installation not supplied])
      fi
      
      CFLAGS="$CFLAGS -I${withval}/include"
      CXXFLAGS="$CXXFLAGS -I${withval}/include"
      AC_SUBST(CXXFLAGS)
      LDFLAGS="$LDFLAGS -lgfapi -L${withval}/lib"
      AC_SUBST(LDFLAGS)
    ],[
    ]
  )
  
  dnl 
  dnl Lets validate that the headers and libs can be used and built against.
  dnl
  AC_CHECK_HEADER([glusterfs/api/glfs.h],,AC_MSG_ERROR([cannot find headers for glfs.h]))
  AC_CHECK_LIB([gfapi],[glfs_new],,AC_MSG_ERROR([cannot find the glfs library]))
  
])

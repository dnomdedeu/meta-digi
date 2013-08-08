# Copyright (C) 2013 Digi International.

PRINC := "${@int(PRINC) + 1}"
PR_append = "+${DISTRO}"

FILESEXTRAPATHS_prepend := "${THISDIR}/${PN}:"

SRC_URI += " \
    file://0001-v4l2-fix-compilation-against-newer-kernel-headers-as.patch \
"

PACKAGECONFIG = "${@base_contains('DISTRO_FEATURES', 'x11', 'x11', '', d)}"
PACKAGECONFIG[x11] = ",--disable-gconf --disable-x --disable-xshm --disable-xvideo,"

DEPENDS_no_X := "${@oe_filter_out('gconf', '${DEPENDS}', d)}"
DEPENDS := "${@base_contains('DISTRO_FEATURES', 'x11', '${DEPENDS}', '${DEPENDS_no_X}', d)}"

EXTRA_OECONF += " \
    --disable-aalibtest \
    --disable-audiofx \
    --disable-cairo \
    --disable-cutter \
    --disable-debug \
    --disable-debugutils \
    --disable-directsound \
    --disable-dv1394 \
    --disable-effectv \
    --disable-esdtest \
    --disable-examples \
    --disable-goom \
    --disable-goom2k1 \
    --disable-gtk-doc \
    --disable-libdv \
    --disable-libpng \
    --disable-osx_audio \
    --disable-osx_video \
    --disable-rpath \
    --disable-shout2test \
    --disable-spectrum \
    --disable-speex \
    --disable-sunaudio \
    --disable-valgrind \
"

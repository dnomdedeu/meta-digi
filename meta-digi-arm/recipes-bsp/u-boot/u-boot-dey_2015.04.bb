# Copyright (C) 2012-2015 Digi International

require recipes-bsp/u-boot/u-boot.inc

DESCRIPTION = "Bootloader for Digi platforms"
LICENSE = "GPLv2+"
LIC_FILES_CHKSUM = "file://Licenses/README;md5=c7383a594871c03da76b3707929d2919"

DEPENDS += "dtc-native u-boot-mkimage-native"

PROVIDES += "u-boot"

SRCBRANCH = "v2015.04/master"
SRCBRANCH_ccimx6ul = "v2015.04/master"
SRCREV = "${AUTOREV}"

# Select internal or Github U-Boot repo
UBOOT_GIT_URI = "${@base_conditional('DIGI_INTERNAL_GIT', '1' , '${DIGI_GIT}u-boot-denx.git', '${DIGI_GITHUB_GIT}/u-boot.git', d)}"

SRC_URI = " \
    ${UBOOT_GIT_URI};branch=${SRCBRANCH} \
"

SRC_URI_append_ccimx6 = " \
    file://boot.txt \
    file://install_linux_fw_sd.txt \
"

LOCALVERSION ?= ""
inherit fsl-u-boot-localversion

EXTRA_OEMAKE_append = " KCFLAGS=-fgnu89-inline"

UBOOT_EXTRA_CONF ?= ""

do_compile () {
	if [ "${@bb.utils.contains('DISTRO_FEATURES', 'ld-is-gold', 'ld-is-gold', '', d)}" = "ld-is-gold" ] ; then
		sed -i 's/$(CROSS_COMPILE)ld$/$(CROSS_COMPILE)ld.bfd/g' config.mk
	fi

	unset LDFLAGS
	unset CFLAGS
	unset CPPFLAGS

	if [ ! -e ${B}/.scmversion -a ! -e ${S}/.scmversion ]
	then
		echo ${UBOOT_LOCALVERSION} > ${B}/.scmversion
		echo ${UBOOT_LOCALVERSION} > ${S}/.scmversion
	fi

    if [ "x${UBOOT_CONFIG}" != "x" ]
    then
        for config in ${UBOOT_MACHINE}; do
            i=`expr $i + 1`;
            for type in ${UBOOT_CONFIG}; do
                j=`expr $j + 1`;
                if [ $j -eq $i ]
                then
                    oe_runmake O=${config} ${config}
                    for var in ${UBOOT_EXTRA_CONF}; do
                        echo "${var}" >> ${config}/.config
                    done
                    oe_runmake O=${config} oldconfig
                    oe_runmake O=${config} ${UBOOT_MAKE_TARGET}
                    cp  ${S}/${config}/${UBOOT_BINARY}  ${S}/${config}/u-boot-${type}.${UBOOT_SUFFIX}

                    # Secure boot artifacts
                    if [ -n "${TRUSTFENCE_UBOOT_SIGN}" ]
                    then
                        cp ${S}/${config}/u-boot-signed.imx ${S}/${config}/u-boot-signed-${type}.${UBOOT_SUFFIX}
                    fi
                fi
            done
            unset  j
        done
        unset  i
    else
        oe_runmake ${UBOOT_MACHINE}
        for var in ${UBOOT_EXTRA_CONF}; do
            echo "${var}" >> .config
        done
        oe_runmake O=${config} oldconfig
        oe_runmake ${UBOOT_MAKE_TARGET}
    fi

}

do_deploy_append() {
	# Remove canonical U-Boot symlinks for ${UBOOT_CONFIG} currently in the form:
	#    u-boot-<platform>.imx-<type>
	#    u-boot-<type>
	# and add a more suitable symlink in the form:
	#    u-boot-<platform>-<config>.imx
	if [ "x${UBOOT_CONFIG}" != "x" ]; then
		for config in ${UBOOT_MACHINE}; do
			i=`expr $i + 1`
			for type in ${UBOOT_CONFIG}; do
				j=`expr $j + 1`
				if [ $j -eq $i ]; then
					cd ${DEPLOYDIR}
					rm -r ${UBOOT_BINARY}-${type} ${UBOOT_SYMLINK}-${type}
					ln -sf u-boot-${type}-${PV}-${PR}.${UBOOT_SUFFIX} u-boot-${type}.${UBOOT_SUFFIX}
					if [ -n "${TRUSTFENCE_UBOOT_SIGN}" ]
					then
						install ${S}/${config}/SRK_efuses.bin SRK_efuses-${PV}-${PR}.bin
						install ${S}/${config}/u-boot-signed-${type}.${UBOOT_SUFFIX} u-boot-signed-${type}-${PV}-${PR}.${UBOOT_SUFFIX}
						ln -sf u-boot-signed-${type}-${PV}-${PR}.${UBOOT_SUFFIX} u-boot-signed-${type}.${UBOOT_SUFFIX}
						ln -sf SRK_efuses-${PV}-${PR}.bin SRK_efuses.bin
						if [ -n "${TRUSTFENCE_UBOOT_ENCRYPT}" ]
						then
							# Move the data encryption key in plain text directly to the deployment directory.
							# Do not leave any other copies in the machine.
							mv ${S}/${config}/dek.bin ${DEPLOYDIR}/dek-${type}.bin
						fi
					fi
				fi
			done
			unset  j
		done
		unset  i
	fi
}

do_deploy_append_ccimx6() {
	# DEY firmware install script
	sed -i -e 's,##GRAPHICAL_BACKEND##,${GRAPHICAL_BACKEND},g' ${WORKDIR}/install_linux_fw_sd.txt
	mkimage -T script -n "DEY firmware install script" -C none -d ${WORKDIR}/install_linux_fw_sd.txt ${DEPLOYDIR}/install_linux_fw_sd.scr

	# Boot script for DEY images
	mkimage -T script -n bootscript -C none -d ${WORKDIR}/boot.txt ${DEPLOYDIR}/boot.scr
}

COMPATIBLE_MACHINE = "(ccimx6$|ccimx6ul)"

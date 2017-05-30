package com.epam.petclinic.plugin.extensions

/**
 * Extension for {@link com.epam.petclinic.plugin.QualityAwareJavaPlugin}.
 *
 * Date: 5/17/2017
 *
 * @author Stanislau Halauniou
 */
class QualityAwareJavaExtension {
    public static final String NAME = 'javaQuality'

    String javaVersion = '1.8'
    String checkstyleToolVersion = '7.1'
    String pmdToolVersion = '5.5.1'
    String findbugToolVersion = '3.0.1'
    String checkstyleSupressionPath
}

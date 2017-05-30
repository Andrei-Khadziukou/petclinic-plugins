package com.epam.petclinic.plugin.extensions

/**
 * Extension for {@link com.epam.petclinic.plugin.CodeCoveragePlugin}.
 *
 * Date: 5/29/2017
 *
 * @author Stanislau Halauniou
 */
class CodeCoverageExtension {
    public static final String NAME = "codeCoverage"

    Map metrics = [
            instruction: 0,
            branch     : 25,
            line       : 50,
            complexity : 0,
            method     : 40,
            //set this to 0 if you want to switch off minimum coverage check
            class      : 50
    ]
}

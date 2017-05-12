package com.epam.petclinic.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Plugin generates Restfull documentation based on Rest Docs tests.
 *
 * Date: 5/12/2017
 *
 * @author Stanislau Halauniou
 */
class AsciiDoctorPlugin implements Plugin<Project> {

    private static final String REST_DOCS_PLUGIN = 'org.asciidoctor.convert'

    @Override
    void apply(Project project) {
        project.plugins.apply(REST_DOCS_PLUGIN)
        project.tasks['jar'].dependsOn(project.tasks['asciidoctor'])
        File snippetsDir = project.file('build/generated-snippets')

        project.asciidoctor {
            attributes 'snippets': snippetsDir
            inputs.dir snippetsDir
            outputDir "build/asciidoc"
            dependsOn project.tasks['test']
            sourceDir 'src/main/resources/asciidoc'
        }
    }
}

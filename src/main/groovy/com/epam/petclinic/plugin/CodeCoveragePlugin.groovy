package com.epam.petclinic.plugin

import com.epam.petclinic.plugin.extensions.CodeCoverageExtension
import com.epam.petclinic.plugin.util.CoverageReportUtil
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

/**
 * Code Coverage Report plugin.
 *
 * Date: 5/10/2017
 *
 * @author Stanislau Halauniou
 */
class CodeCoveragePlugin implements Plugin<Project> {
    private static final String JACOCO_TOOL_VERSION = '0.7.8'
    private static final String CODE_COVERAGE_TASK_GROUP = 'Petclinic Code Coverage'
    private static final String JACOCO_PLUGIN_ID = 'jacoco'
    private static final String XML_REPORT_PATH = '/reports/coverage/coverage-report.xml'

    private XmlParser parser

    @Override
    void apply(Project project) {
        parser = configureParser()
        project.extensions.create(CodeCoverageExtension.NAME, CodeCoverageExtension)

        project.plugins.withType(JavaPlugin) {
            addCheckMinimumCoverageTask(project)
            addCreateJacocoReportTask(project)
            configureJacoco(project)
        }
    }

    /**
     * Performs the configuration of JaCoCo plugin.
     *
     * @param project
     */
    private void configureJacoco(Project project) {
        project.plugins.apply(JACOCO_PLUGIN_ID)

        //JaCoCo version
        project.jacoco {
            toolVersion = JACOCO_TOOL_VERSION
        }

        //configuration of the JaCoCo specific properties of the test task
        project.test {
            jacoco {
                append = false
                destinationFile = project.file("${project.buildDir}/${CoverageReportUtil.JACOCO_TEST_PATH}")
                classDumpFile = project.file("${project.buildDir}/jacoco/classpathdumps")
            }
            finalizedBy(project.createJacocoReport)
        }

    }

    /**
     * Add task which generates the coverage report.
     *
     * @param project
     */
    private void addCreateJacocoReportTask(Project project) {
        project.tasks.create(name: 'createJacocoReport') {
            group(CODE_COVERAGE_TASK_GROUP)
            description('Create coverage report')
            onlyIf {project.sourceSets.main.output.classesDir.exists()}
            doLast {
                CoverageReportUtil.generate(project)
            }
            finalizedBy(project.checkMinimumCoverage)
        }
    }

    /**
     * Add task that responsible for checking of minimum code coverage.
     *
     * @param project
     */
    private void addCheckMinimumCoverageTask(Project project) {
        project.task(['description': 'Check minimum test coverage',
                      'group'      : CODE_COVERAGE_TASK_GROUP],
                'checkMinimumCoverage').doLast {
            List failures = []
            //check that coverage rate is not less acceptable
            parseCoverageReport(project).each { metric ->
                if (metric.value < (project.codeCoverage.metrics[metric.key] as double)) {
                    failures.add("${metric.key} coverage rate is: ${metric.value}%, " +
                            "minimum is ${project.codeCoverage.metrics[metric.key]}%")
                }
            }
            if (failures) {
                throw new GradleException("Check coverage: ${failures}")
            }
        }.onlyIf {
            //task should be executed if isSkipCoverage is false and state of jacocoTestReport is not skipped
            //but state can be skipped because of upToDate is true
            //TODO: find the better way to implement this check
            !project.createJacocoReport.state.skipped || project.createJacocoReport.state.upToDate
        }

    }

    /**
     * Performs parsing coverage report for the current project to get metrics.
     *
     * @param xmlReport coverage report to parse
     * @return percentage coverage metrics of project
     */
    private Map parseCoverageReport(Project project) {
        File xmlReport = project.file("${project.buildDir}${XML_REPORT_PATH}")
        if (xmlReport.exists()) {
            Map reportMetrics = [:]

            //closure calculate percentage coverage
            Closure percentage = { Node node ->
                //if metric is missing, consider that coverage is full
                if (node != null) {
                    //covered counter
                    double covered = node.@covered as double
                    //missed counter
                    double missed = node.@missed as double
                    //calculate percentage(with an accuracy of two decimal places) coverage
                    return ((covered / (covered + missed)) * 100).round(2)
                } else {
                    return 100
                }
            }

            Node parseResults = parser.parse(xmlReport)

            //find and calculate every metric in coverage report
            project.codeCoverage.metrics.each { metric ->
                reportMetrics << [(metric.key): (percentage(parseResults.counter.find {
                    //see structure of JaCoCo xml coverage report
                    it.@type == metric.key.toUpperCase()
                }))]
            }

            return reportMetrics
        } else {
            throw new GradleException("File: ${xmlReport.name} doesn't exist")
        }
    }

    /**
     * Configures the xml parser.
     *
     * @return configured parser.
     */
    private XmlParser configureParser() {
        XmlParser parser = new XmlParser()
        parser.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)
        parser.setFeature('http://apache.org/xml/features/disallow-doctype-decl', false)
        return parser
    }
}

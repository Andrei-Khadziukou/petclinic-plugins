package com.epam.petclinic.plugin

import com.epam.petclinic.plugin.extensions.QualityAwareJavaExtension
import com.epam.petclinic.plugin.util.CodeQualityUtil
import com.epam.petclinic.plugin.util.ModuleTree
import org.apache.commons.lang3.StringUtils
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.FindBugs
import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.resources.TextResource
import org.gradle.api.tasks.compile.JavaCompile

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * The QualityAwareJavaPlugin adds configuration that is common to all Java projects.
 * Add java, checkstyle, pmd, findbugs plugins.
 *
 * Date: 5/4/2017
 *
 * @author Stanislau Halauniou
 */
public class QualityAwareJavaPlugin implements Plugin<Project> {
    private static final String CODE_QUALITY_DIR = 'code-quality'
    private static final String JAVA_PLUGIN_ID = "java"
    private static final String CHECKSTYLE_PLUGIN_ID = "checkstyle"
    private static final String PMD_PLUGIN_ID = "pmd"
    private static final String FINDBUGS_PLUGIN_ID = "findbugs"
    private static final String CHECK_CODE_QUALITY_ERRORS_TASK = "checkCodeQualityErrors"

    @Override
    void apply(Project project) {
        project.extensions.create(QualityAwareJavaExtension.NAME, QualityAwareJavaExtension)
        ModuleTree.eachChildModule(project) { subProject ->
            subProject.plugins.apply(JAVA_PLUGIN_ID)

            subProject.tasks.withType(JavaCompile) {
                sourceCompatibility = project.javaQuality.javaVersion
                targetCompatibility = project.javaQuality.javaVersion
                options.encoding = 'UTF-8'
                options.compilerArgs = [
                        '-Xlint:deprecation',
                        '-Xlint:finally',
                        '-Xlint:overrides',
                        '-Xlint:path',
                        '-Xlint:processing',
                        '-Xlint:rawtypes',
                        '-Xlint:varargs',
                        '-Xlint:unchecked'
                ]
            }

            subProject.afterEvaluate {
                configureCheckStyle(subProject)
                configurePMD(subProject)
                configureFindBugs(subProject)
            }
        }
        //TODO: enable fail build after fixing tons of code quality errors
        //createCheckCodeQualityErrorsTask(project)
    }

    private void configureCheckStyle(Project project) {
        project.plugins.apply(CHECKSTYLE_PLUGIN_ID)

        project.checkstyle {
            toolVersion = project.rootProject.javaQuality.checkstyleToolVersion
            config = getToolResource(project, 'checkstyle/checkstyle-rules.xml')
            configProperties.suppressionsFile =
                    getFilePath(project, 'checkstyle/checkstyle-suppressions.xml',
                            project.rootProject.javaQuality.checkstyleSupressionPath)
            ignoreFailures = true
        }

        project.tasks.withType(Checkstyle) {
            reports {
                html.stylesheet(
                        getToolResource(project, 'checkstyle/checkstyle-noframes-severity-sorted.xsl'))
            }
        }
    }

    private void configurePMD(Project project) {
        project.plugins.apply(PMD_PLUGIN_ID)

        project.pmd {
            ignoreFailures = true
            toolVersion = project.rootProject.javaQuality.pmdToolVersion
            ruleSetFiles = project.rootProject.files(getFilePath(project, 'pmd/pmd-rules-general.xml'))
        }

        /*
         * We can disable auto-generated HTML because it will be overridden by ANT task
         */
        project.tasks.withType(Pmd) {
            reports {
                html.enabled(false)
            }
        }

        /*
         * PMD Gradle plugin does not have "html.stylesheet" option, so apply stylesheet through ANT XSLT task
         */
        applyXsltAntTransformation(project, PMD_PLUGIN_ID, 'pmd/pmd-nicerhtml.xsl')
    }

    private void configureFindBugs(Project project) {
        project.plugins.apply(FINDBUGS_PLUGIN_ID)

        project.findbugs {
            toolVersion = project.rootProject.javaQuality.findbugToolVersion
            sourceSets = [project.sourceSets.main, project.sourceSets.test]
            excludeFilter = project.file(getFilePath(project, 'findbugs/findbugs-exclude.xml'))
            ignoreFailures = true
        }

        /*
         * FindBugs plugin does not generate XML and HTML at the same time, so HTML will be generated by ANT
         */
        project.tasks.withType(FindBugs) {
            reports {
                xml.enabled(true)
                xml.withMessages(true)
                html.enabled(false)
            }
        }

        applyXsltAntTransformation(project, FINDBUGS_PLUGIN_ID, 'findbugs/default.xsl')
    }

    private static void applyXsltAntTransformation(Project project, String toolName, String stylesheetRelativePath) {
        project.sourceSets.each { sourceSet ->
            String sourceSetName = sourceSet.name

            Path xmlReportPath = Paths.get("${project[toolName].reportsDir}", "${sourceSetName}.xml")
            Path htmlReportPath = Paths.get("${project[toolName].reportsDir}", "${sourceSetName}.html")

            project.tasks["${toolName}${sourceSetName.capitalize()}"].doLast {
                if (Files.exists(xmlReportPath)) {
                    project.ant.xslt(
                            in: "$xmlReportPath",
                            out: "$htmlReportPath",
                            style: getFilePath(project, stylesheetRelativePath),
                            destdir: "${project[toolName].reportsDir}"
                    )
                }
            }
        }
    }

    private static TextResource getToolResource(Project project, String relativeReference, String overriddenPath) {
        return project.rootProject.resources.text.fromFile(
                getFilePath(project, relativeReference, overriddenPath)
        )
    }

    private static TextResource getToolResource(Project project, String relativeReference) {
        return getToolResource(project, relativeReference, null)
    }

    private static String getFilePath(Project project, String localPath, String overriddenPath) {
        if (overriddenPath == null || overriddenPath.isEmpty()) {
            // Gets file from plugins resources locally and copy to project
            return getAndCopyFileFromPluginsToProject(project, localPath)
        }
        return "${project.rootDir}/${overriddenPath}"
    }

    private static String getFilePath(Project project, String localPath) {
        return getFilePath(project, localPath, null)
    }


    /**
     * Copy file from plugins to root project to the code-quality directory(used for idea plugins).
     *
     * @param project from which you run plugins
     * @param path to file from plugins directories
     * @return created file path for project
     */
    private static String getAndCopyFileFromPluginsToProject(Project project, String path) {
        String codeQualityPath = "${CODE_QUALITY_DIR}/${path}"
        URL resource = QualityAwareJavaPlugin.getClassLoader().getResource(codeQualityPath)
        File targetFile = new File("${project.rootDir}/${codeQualityPath}");
        File directory = new File(targetFile.getParentFile().getAbsolutePath());
        directory.mkdirs();
        resource.openStream().withCloseable { is ->
            Files.copy(is, targetFile.toPath(),  StandardCopyOption.REPLACE_EXISTING)
        }
        return targetFile.absolutePath
    }

    /**
     * Create task, that find errors in checkstyle, findbugs or pmd reports and if there are exists  - fail build
     * and log those errors.
     *
     * @param project current project
     */
    private void createCheckCodeQualityErrorsTask(Project project) {
        project.tasks.create(name: CHECK_CODE_QUALITY_ERRORS_TASK) {
            description("Fail build if any Checkstyle, Pmd or Findbugs error found.")
            doLast {
                List reportErrors = []
                ModuleTree.eachChildModule(project) { subProject ->
                    subProject.sourceSets.each { sourceSet ->
                        String sourceName = sourceSet.name
                        reportErrors.addAll(CodeQualityUtil.getCheckstyleReportErrors(subProject, sourceName))
                        reportErrors.addAll(CodeQualityUtil.getFindbugsReportErrors(subProject, sourceName))
                        reportErrors.addAll(CodeQualityUtil.getPmdReportErrors(subProject, sourceName))
                    }
                }

                if (!reportErrors.isEmpty()) {
                    throw new GradleException("Rule violations were found at modules, Please, look at " +
                            "Checkstyle, Pmd or Findbugs reports: \n" + StringUtils.join(reportErrors, "\n"))
                }
            }
        }

        project.tasks['build'].finalizedBy(project.tasks[CHECK_CODE_QUALITY_ERRORS_TASK])
    }

}

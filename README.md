# petclinic-plugins

- From petclinic-plugins root:
Publish artifacts to maven local: gradle publishToMavenLocal

- For main project where you want to use the plugins:
Add classpath("com.epam.petclinic.plugin:petclinic-plugins:0.0.1") to your script dependencies in build.gradle
For java plugin add - apply plugin: 'petclinic-java' (remove version, sourceCompatibility ?) - it adds java, checkstyle,
pmd and findbugs plugins
For code coverage add - apply plugin: 'petclinic-code-coverage' and jacocoVersion=0.7.8(to gradle.properties)
For using idea plugin add - apply plugin: 'petclinic-idea', it applies configurations for idea for
checkstyle, formatter, pmd, findbugs plugins(BUT in addition: CheckStyle-IDEA, PMDPlugin, FindBugs-IDEA should be installed on ide)
For use document generation plugin for rest docs - apply plugin: 'petclinic-ascii-doctor'
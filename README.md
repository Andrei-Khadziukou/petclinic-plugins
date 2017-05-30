# petclinic-plugins

### How to use:
* From petclinic-plugins root publish plugins to maven local repository: `gradle clean publishToMavenLocal`
* For using it from child project add to buildscript dependencies in build.gradle:
    `classpath("com.epam.petclinic:petclinic-plugins:${petclinicPluginsVersion}")`

### Available plugins:
Add to build.gradle
* `apply plugin: 'com.epam.petclinic.java'` - applies checkstyle, formatter, pmd, findbugs code quality checkers for gradlew build
* `apply plugin: 'com.epam.petclinic.idea'` - applies idea configurations for checkstyle, formatter, pmd, findbugs plugins(BUT in addition: CheckStyle-IDEA, PMDPlugin, FindBugs-IDEA should be installed on ide)
* `apply plugin: 'com.epam.petclinic.code-coverage'` - applies code coverage checker (for disabling use in child's build gradle - `checkMinimumCoverage.enabled=false`)
* `apply plugin: 'com.epam.petclinic.ascii-doctor'` - applies rest docs generation plugin based on mock mvc integration tests
* `apply plugin: 'com.epam.petclinic.database'` - applies database plugin that includes createDatabase, dropDatabase, liquibase tasks
* `apply plugin: 'com.epam.petclinic.petcliinc-artifactory'` - don't use, still in progress

### Extensions (plugin's configuration)
Configured in `plugins` project and could be overridden in child project
* javaQuality extension - used for com.epam.petclinic.java plugin
* codeCoverage extension - used for com.epam.petclinic.code-coverage plugin

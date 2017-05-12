package com.epam.petclinic.plugin

import groovy.sql.Sql
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Database plugin for liquibase tasks, creation and deletion database.
 *
 * Date: 5/16/2017
 *
 * @author Stanislau Halauniou
 */
class DatabasePlugin implements Plugin<Project> {

    private static final String LIQUIBASE_PLUGIN = 'liquibase'
    private static final String POSTGRESQL = 'postgresql'
    private static final String DATABASE_TASK_GROUP = 'Database Applier'
    private static final String POSTGRES_DRIVER = 'org.postgresql.Driver'
    private static final String JDBC_URL = 'jdbc:postgresql://localhost:5432/'
    private static final String CREATE_DATABASE = 'createDatabase'
    private static final String DROP_DATABASE = 'dropDatabase'

    @Override
    void apply(Project project) {
        project.plugins.apply(LIQUIBASE_PLUGIN)

        initJdbcDriver(project)
        configureLiquibase(project)
        addDatabaseTasks(project)
    }

    /**
     * Initiates JDBC driver.
     *
     * @param project project to which plugin applied
     */
    private void initJdbcDriver(Project project) {
        // Finds a jdbc driver
        File driver = project.rootProject.buildscript.configurations.classpath.find { File file ->
            file.name.contains(POSTGRESQL)
        }
        // Loads and registers a JDBC driver for database access
        URLClassLoader loader = GroovyObject.class.classLoader
        loader.addURL(driver.toURI().toURL())
    }

    /**
     * Configures Liquibase plugin.
     *
     * @param project project to which plugin applied
     * @param cp classpath where Liquibase's scripts are located
     */
    private void configureLiquibase(Project project) {
        project.liquibase {
            activities {
                main {
                    changeLogFile("src/main/resources/${project.changeLogPath}")
                    url("${JDBC_URL}${project.databaseName}")
                    defaultSchemaName(project.schemaName)
                    username(project.username)
                    password(project.password)
                }
            }
        }
    }

    /**
    * Adds tasks to work with database.
    *
    * @param project project to which plugin applied
    */
    private void addDatabaseTasks(Project project) {
        project.task([description: 'Creates the database',
                      group      : DATABASE_TASK_GROUP],
                CREATE_DATABASE) {
            doLast {
                String createUserQuery = "create user ${project.username}"
                Sql.withInstance(JDBC_URL, project.admin, project.password, POSTGRES_DRIVER, { Sql sql ->
                    sql.execute(createUserQuery)
                })

                String createUserPasswordQuery = "alter user ${project.username} password \'${project.password}\'"
                Sql.withInstance(JDBC_URL, project.admin, project.password, POSTGRES_DRIVER, { Sql sql ->
                    sql.execute(createUserPasswordQuery)
                })

                String createDbQuery = "create database ${project.databaseName} owner ${project.username}"
                Sql.withInstance(JDBC_URL, project.admin, project.password, POSTGRES_DRIVER, { Sql sql ->
                    sql.execute(createDbQuery)
                })

                String createSchemaQuery = "create schema if not exists " +
                        "${project.schemaName} authorization ${project.username}"
                Sql.withInstance("${JDBC_URL}${project.databaseName}", project.admin, project.password, POSTGRES_DRIVER,
                        { Sql sql ->
                            sql.execute(createSchemaQuery)
                        })
            }
        }

        project.task([description: 'Drops the database',
                      group      : DATABASE_TASK_GROUP],
                DROP_DATABASE) {
            doLast {
                String dropDatabaseQuery = "drop database if exists ${project.databaseName}"
                Sql.withInstance(JDBC_URL, project.admin, project.password, POSTGRES_DRIVER, { Sql sql ->
                    sql.execute(dropDatabaseQuery)
                })

                String dropUserQuery = "drop user if exists ${project.username}"
                Sql.withInstance(JDBC_URL, project.admin, project.password, POSTGRES_DRIVER, { Sql sql ->
                    sql.execute(dropUserQuery)
                })
            }
        }
    }

}

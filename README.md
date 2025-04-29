# Project Documentation: Roster Generator

## 1. Overview

This document provides an analysis of the Roster Generator web application. The application consists of a Java Spring Boot backend providing a REST API and serving a static frontend, interacting with a PostgreSQL database. Its primary function is to generate employee schedules (rosters) based on various inputs like tasks, employee availability, skills, and constraints.

## How to Run on new machine
*   **Prerequisites:**
    *   Java 17 to install java ```sudo apt install openjdk-17-jdk openjdk-17-jre``` 
    * ```java -version```
    *   Maven to install maven ```sudo apt install maven```
    * ``` mvn -v```
    *   PostgreSQL to install postgresql ```sudo apt install postgresql postgresql-contrib```
    * ```psql --version```
    *  run the following command to create a user and the database:
    *  ```sudo -u postgres psql```
    * ```CREATE DATABASE simpleroster-v2;```
    *  others useful commands:
    *  list databases: ```\l```
    * list users: ```\du```
    * connect to a database: ```\c simpleroster-v2```
    * insert the sql file ```\i data_refined.sql```
    * check the port that psql is running 
    * ```sudo netstat -nlpt | grep postgres```
    * or before stop the service psql and running psql:
    * ```sudo systemctl stop postgresql``` 
    * to change the port of psql to : 
    * ```sudo nvim /etc/postgresql/16/main/postgresql.conf```
    * change the port to 5433
    * ```sudo systemctl start postgresql```
    * before run the application:
    * set password for the postgres user:
    * Connect to the database: sudo -u postgres psql 
    * Enter the command: \password postgres
    * Enter the password on the application.properties file

    * import the jar file
* import the sql file

* to first populate the database:
* -u means username
* -d means database_name
* -a means all the output will be shown in the terminal
* -f means the file name
```bash
 psql -U postgres -d  -a -f data_refined.sql
 ```
*   **To run the application:**
* - Make sure PostgreSQL is running.
- Run the Spring Boot application using your IDE or command line:
```bash
mvn spring-boot:run
```
*   **To access the application:**
    *   Open a web browser and navigate to `http://localhost:8090/` for the main page.




## 2. Architecture

*   **Backend:** Java 17, Spring Boot 3.4.4, Maven
    *   **Framework:** Spring Boot (Web, Data JPA, Security, Actuator)
    *   **Database:** PostgreSQL
    *   **ORM:** Hibernate (via Spring Data JPA)
    *   **Authentication:** JWT (JSON Web Tokens) using `jjwt` library and Spring Security.
    *   **API:** RESTful API using Spring MVC controllers.
*   **Frontend:** Static HTML, CSS, JavaScript served directly by the Spring Boot application from the `src/main/resources/static` directory.
*   **Database Schema:** Defined by JPA entities (`Employee`, `Task`, `Skill`, `User`, `Shift`). Schema management currently relies on `spring.jpa.hibernate.ddl-auto=update`.

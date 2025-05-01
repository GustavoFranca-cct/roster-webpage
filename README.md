# Website create as a prottype for Project Skil: Simple Roster(roster generator)

Architecture

    Backend:
        Language/Platform: Java 17, Spring Boot 3.2.4 (Note: Version 3.4.4 seemed unlikely, adjusted to a plausible recent version, please verify), Maven
        Framework Modules: Spring Boot (Web, Data JPA, Security, Actuator)
        Database: PostgreSQL
        Authentication: JWT (JSON Web Tokens) via jjwt library and Spring Security
        API: RESTful (Spring MVC)
    Frontend: Static HTML, CSS, JavaScript (Served by Spring Boot from src/main/resources/static)
    Database Schema: Defined by JPA Entities (e.g., Employee, Task, Skill, User, Shift)

1. Overview

This document provides step-by-step instructions on how to set up and run the Roster Generator web application. Its primary function is to generate employee schedules (rosters) based on various inputs such as tasks, employee availability, skills, and constraints.

Disclaimer: This web application was developed as a prototype during my second semester of college for the 'Project Skill' subject. Please be aware that it contains numerous bugs, unfinished functions, and lacks robust programming logic, reflecting its origin as an early student project. I may rebuild this project in the future once my programming skills are more advanced.
2. Setting Up and Running the Application

Follow these steps to get the application running on a new machine.
2.1. Prerequisites

Ensure the following software is installed:

    Java Development Kit (JDK) 17:
        Verify installation:
        Bash

java -version

Example Installation (Debian/Ubuntu Linux):
Bash

    sudo apt update
    sudo apt install openjdk-17-jdk openjdk-17-jre

    For other operating systems, download from Oracle, Adoptium, or use a package manager like brew (macOS) or choco (Windows).

PostgreSQL Database:

    Verify installation:
    Bash

psql --version

Example Installation (Debian/Ubuntu Linux):
Bash

sudo apt update
sudo apt install postgresql postgresql-contrib

Ensure the PostgreSQL server service is running after installation.
Bash

    sudo systemctl status postgresql

Apache Maven (Required only if building from source):

    Needed if you plan to run using mvn spring-boot:run.
    Verify installation:
    Bash

        mvn -version

        Installation instructions vary by OS. Check the official Maven website.

2.2. Database Setup

    Connect to PostgreSQL as the admin user:
        This command connects you to the database server using the default postgres superuser account.
        Bash

    sudo -u postgres psql

Create the Application Database:

    Inside the psql shell, run:
    SQL

    CREATE DATABASE "simpleroster-v2";

    (Optional) Useful psql commands:
        List all databases: \l
        List all users/roles: \du
        Connect to a specific database: \c database_name
        Quit psql: \q

Set Password for postgres User:

    While still in the psql shell connected as the postgres user:
    SQL

        \password postgres

        Enter and confirm a secure password when prompted. Remember this password, you will need it for the application configuration.

2.3. Database Configuration (Port)

The application needs to know which port PostgreSQL is running on. By default, PostgreSQL uses port 5432.

    Check PostgreSQL Port:
    Bash

sudo netstat -nlpt | grep postgres

    Look for the port number (commonly 5432).

Option A (Recommended): Configure the Application

    Modify the application.properties file (see Step 2.4) and ensure the spring.datasource.url property points to the correct port (e.g., jdbc:postgresql://localhost:5432/simpleroster-v2). This is the most flexible approach.

Option B: Change PostgreSQL Port (Only if necessary)

    If you absolutely must run PostgreSQL on port 5433 (as mentioned in the original notes perhaps due to a hardcoded value in an old JAR build), follow these steps:
        Stop PostgreSQL: sudo systemctl stop postgresql
        Edit the configuration file (path might vary slightly based on version/OS):
        Bash

            sudo nano /etc/postgresql/16/main/postgresql.conf
            # Or use vim: sudo vim /etc/postgresql/16/main/postgresql.conf

            Find the line #port = 5432 (or similar), uncomment it (remove the #), and change the value to 5433.
            Save the file and exit the editor.
            Start PostgreSQL: sudo systemctl start postgresql
            Verify it's running on the new port using the netstat command again.

2.4. Application Configuration (application.properties)

Before running the application, you need to configure its database connection settings. Locate or create the application.properties file.

    If running from source code: It's usually located in src/main/resources/application.properties.
    If running a JAR: It might be packaged inside, or you might place an external application.properties file in the same directory as the JAR, or use environment variables.

Ensure the following properties are set correctly:
Properties

# Server Port (Example: 8090)
server.port=8090

# Database Connection
spring.datasource.url=jdbc:postgresql://localhost:5432/simpleroster-v2
# ^^ Adjust localhost and port (e.g., 5433 if you changed it) if needed.
spring.datasource.username=postgres
spring.datasource.password=YOUR_POSTGRES_PASSWORD_HERE
# ^^ Use the password you set in step 2.2.3

# JPA/Hibernate Settings (Example)
spring.jpa.hibernate.ddl-auto=update # Or 'validate', 'none' depending on your setup
spring.jpa.show-sql=true

# JWT Secret Key (Example - Use a strong, private key!)
# You might set this via environment variable for better security
jwt.secret=YourVerySecretKeyNeedsToBeLongAndSecure

Important: Replace YOUR_POSTGRES_PASSWORD_HERE with the actual password you set. Also, adjust the database URL (localhost:5432) if your database host or port is different.
2.5. Populate the Database

You need to import the initial data from the data_refined.sql file.

    Method 1 (Using psql command line):
        Navigate to the directory containing data_refined.sql in your terminal.
        Run the following command, replacing YOUR_POSTGRES_PASSWORD_HERE if required (it might prompt you if not provided).
        Bash

    psql -U postgres -d "simpleroster-v2" -f data_refined.sql

    Flag Explanations:
        -U postgres: Specifies the PostgreSQL username.
        -d simpleroster-v2: Specifies the database name.
        -f data_refined.sql: Specifies the SQL file to execute.

Method 2 (Inside psql shell):

    Connect to the database: sudo -u postgres psql -d "simpleroster-v2"
    Use the \i command to import the file (provide the full path to the file):
    SQL

        \i /path/to/your/data_refined.sql

2.6. Run the Application

You have two main options:

    Option A: Run the pre-compiled JAR file
        Place the route-generator-0.0.1-SNAPSHOT.jar file in a directory.
        Ensure your application.properties file is correctly configured (either inside the JAR, or placed in the same directory as the JAR, or settings provided via environment variables).
        Open a terminal in that directory and run:
        Bash

    java -jar route-generator-0.0.1-SNAPSHOT.jar

Option B: Build and Run from Source using Maven

    Make sure you have the application's source code and Maven is installed.
    Ensure the src/main/resources/application.properties file is correctly configured.
    Open a terminal in the root directory of the project (where the pom.xml file is).
    Run the application using the Spring Boot Maven plugin:
    Bash

        mvn spring-boot:run

        Maven will download dependencies, compile the code, and start the application.

2.7. Access the Application

Once the application starts successfully (check the terminal logs for messages like "Tomcat started on port(s): 8090"), open a web browser and navigate to:

http://localhost:8090/

(Adjust the port 8090 if you configured a different server.port in application.properties)
3. (Optional) Configure Nginx as a Reverse Proxy

If you want to deploy this application more formally (e.g., make it accessible on standard HTTP port 80, handle HTTPS, or serve multiple applications from one server), you can use Nginx as a reverse proxy.

    Install Nginx:
        Example (Debian/Ubuntu): sudo apt update && sudo apt install nginx

    Create an Nginx Configuration File:

        Create a new file in /etc/nginx/sites-available/, for example, roster-generator:
        Bash

sudo nano /etc/nginx/sites-available/roster-generator

Add the following configuration, replacing <your_server_name_or_ip> and adjusting the proxy_pass port if your Spring Boot app runs on a port other than 8090:
Nginx

    server {
        listen 80;
        listen [::]:80;

        # Replace with your server's domain name or IP address
        server_name <your_server_name_or_ip>;

        location / {
            # Forward requests to the running Spring Boot application
            # Ensure the port matches your application's server.port
            proxy_pass http://localhost:8090/;

            # Set headers to pass along original request info
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_set_header X-Forwarded-Port $server_port;
        }
    }

Enable the Site and Test Configuration:

    Create a symbolic link to enable the site:
    Bash

sudo ln -s /etc/nginx/sites-available/roster-generator /etc/nginx/sites-enabled/

Test the Nginx configuration for errors:
Bash

    sudo nginx -t

Restart Nginx:

    If the test is successful, apply the changes:
    Bash

    sudo systemctl restart nginx

Access via Nginx: You should now be able to access the application via http://<your_server_name_or_ip>/ (on port 80).

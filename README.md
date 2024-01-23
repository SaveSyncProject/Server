# Server

## General Project Information

- Java application with JDK OpenJDK 21.0.1
- Maven project
- Use of SSL sockets
- LDAP directory used for user authentication
- AES encryption implemented for secure storage of backups on the server

## Quick Start

### Environment Setup
- Ensure Java is installed on your system and the JAVA_HOME environment variable is set to point to OpenJDK 21.
- Install Docker, if not already done, and start the Docker service on your machine.

### Installation

1. Clone the Git repository
```
git clone https://github.com/SaveSync-App/Server.git
```

2. Configure JavaFX

Ensure that the JavaFX SDK is installed and properly configured in your IDE or development environment.

### Launching the Server Application

The server has a graphical interface listing relevant information.
Open your IDE and launch the server application or use the .jar file

![SaveSyncServerGUI.png](src/main/resources/img/SaveSyncServerGUI.png)

### Launching the Client Application

Open your IDE and launch the client application or use the .jar file to start the SaveSync client graphical interface.

A configuration file is already present in LDAP for user authentication.
- admin / admin

## SSL Security Steps (Server Side)

### Step 1: Creating a Keystore

- Generate a self-signed certificate with the following command:

```bash
keytool -genkey -alias myServerKey -keyalg RSA -keystore mySrvKeystore.jks -keysize 2048
```

```bash
gonfiantinig@DESKTOP-PGLUUA7:/mnt/c/Users/Gaetan/OneDrive - umontpellier.fr/Cours IUT 3ème année/Semestre 5/Contuinité d
e service/TD/TD3 - PRA/Application_Sauvegarde/Server/Server_app/resources/SSL/Serveur$ keytool -genkey -alias myServerKey -keyalg RSA -keystore mySrvKeystore.jks -keysize 2048
Enter keystore password:
Re-enter new password:
They don't match. Try again
Enter keystore password:
Re-enter new password:
Enter the distinguished name. Provide a single dot (.) to leave a sub-component empty or press ENTER to use the default value in braces.
What is your first and last name?
  [Unknown]:  Gaëtan Gonfiantini
What is the name of your organizational unit?
  [Unknown]:  IUT
What is the name of your organization?
  [Unknown]:  SaveUnit
What is the name of your City or Locality?
  [Unknown]:  Montpellier
What is the name of your State or Province?
  [Unknown]:  France
What is the two-letter country code for this unit?
  [Unknown]:  FR
Is CN=Gaëtan Gonfiantini, OU=IUT, O=SaveUnit, L=Montpellier, ST=France, C=FR correct?
  [no]:  YES

Generating 2,048 bit RSA key pair and self-signed certificate (SHA384withRSA) with a validity of 90 days
        for: CN=Gaëtan Gonfiantini, OU=IUT, O=SaveUnit, L=Montpellier, ST=France, C=FR
gonfiantinig@DESKTOP-PGLUUA7:/mnt/c/Users/Gaetan/OneDrive - umontpellier.fr/Cours IUT 3ème année/Semestre 5/Contuinité d
e service/TD/TD3 - PRA/Application_Sauvegarde/Server/Server_app/resources/SSL/Serveur$
```

### Step 2: Exporting the Server's Public Certificate

This command generates a certificate:

```bash
keytool -export -alias myServerKey -file server.cer -keystore mySrvKeystore.jks
```

```bash
gonfiantinig@DESKTOP-PGLUUA7:/mnt/c/Users/Gaetan/OneDrive - umontpellier.fr/Cours IUT 3ème année/Semestre 5/Contuinité de service/TD/TD3 - PRA/Application_Sauvegarde/Server/Server_app/resources/SSL/Serveur$ keytool -export -alias myServerKey -file server.cer -keystore mySrvKeystore.jks
Enter keystore password:
Certificate stored in file <server.cer>
```

## SSL Security Steps (Client Side)

### Step 1: Creating a Keystore for the Client

```bash
gonfiantinig@DESKTOP-PGLUUA7:/mnt/c/Users/Gaetan/OneDrive - umontpellier.fr/Cours IUT 3ème année/Semestre 5/Contuinité d
e service/TD/TD3 - PRA/Application_Sauvegarde/Server/Server_app/resources/SSL/Client$ keytool -genkey -alias clientKey -keyalg RSA -keystore myClientKeystore.jks -keysize 2048
Enter keystore password:
Re-enter new password:
Enter the distinguished name. Provide a single dot (.) to leave a sub-component empty or press ENTER to use the default value in braces.
What is your first and last name?
  [Unknown]:  Gaëtan Gonfiantini
What is the name of your organizational unit?
  [Unknown]:  IUT
What is the name of your organization?
  [Unknown]:  SaveUnit
What is the name of your City or Locality?
  [Unknown]:  Montpellier
What is the name of your State or Province?
  [Unknown]:  France
What is the two-letter country code for this unit?
  [Unknown]:  FR
Is CN=Gaëtan Gonfiantini, OU=IUT, O=SaveUnit, L=Montpellier, ST=France, C=FR correct?
  [no]:  YES

Generating 2,048 bit RSA key pair and self-signed certificate (SHA384withRSA) with a validity of 90 days
        for: CN=Gaëtan Gonfiantini, OU=IUT, O=SaveUnit, L=Montpellier, ST=France, C=FR
gonfiantinig@DESKTOP-PGLUUA7:/mnt/c/Users/Gaetan/OneDrive - umontpellier.fr/Cours IUT 3ème année/Semestre 5/Contuinité d
e service/TD/TD3 - PRA/Application_Sauvegarde/Server/Server_app/resources/SSL/Client$
```

### Step 2: Importing the Certificate into the Client's Keystore

It's important that the client's keystore trusts the certificate generated by the server:

```bash
keytool -import -alias serverCert -file path/to/server.cer -keystore path/to/clientKeystore.jks
```

```bash
gonfiantinig@DESKTOP-PGLUUA7:/mnt/c/Users/Gaetan/OneDrive - umontpellier.fr/Cours IUT 3ème année/Semestre 5/Contuinité de service/TD/TD3 - PRA/Application_Sauvegarde/Server/Server_app/resources/SSL/Client$ keytool -import -alias serverCert -file server.cer -keystore myClientKeystore.jks
Enter keystore password:
Owner: CN=Gaëtan Gonfiantini, OU=IUT, O=SaveUnit, L=Montpellier, ST=France, C=FR
Issuer: CN=Gaëtan Gonfiantini, OU=IUT, O=SaveUnit, L=Montpellier, ST=France, C=FR
Serial number: 1694f4a077a64948
Valid from: Thu Dec 14 11:56:30 CET 2023 until: Wed Mar 13 11:56:30 CET 2024
Certificate fingerprints:
         SHA1: A9:F4:7A:32:90:A1:29:3E:8A:C3:5D:45:28:78:3C:D7:83:C7:6F:BE
         SHA256: 30:D1:E7:FD:B3:2C:F4:5C:3D:22:C3:A1:4A:1A:35:35:3B:F2:2C:53:00:18:D4:7A:04:B7:8B:F7:41:23:3E:4E
Signature algorithm name: SHA384withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.14 Criticality=false
SubjectKeyIdentifier [
KeyIdentifier [
0000: 4D D8 FC 81 32 44 F0 CC   A1 57 2C F5 D2 F5 4B EC  M...2D...W,...K.
0010: 22 4A 82 20                                        "J.
]
]

Trust this certificate? [no]:  YES
Certificate was added to keystore
gonfiantinig@DESKTOP-PGLUUA7:/mnt/c/Users/Gaetan/OneDrive - umontpellier.fr/Cours IUT 3ème année/Semestre 5/Contuinité de service/TD/TD3 - PRA/Application_Sauvegarde/Server/Server_app/resources/SSL/Client$
```

## Integrating SSL into the Code

Once these steps are completed, the keystores are ready to be used in the code.





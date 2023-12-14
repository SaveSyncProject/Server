# Server

## Informations générales du projet

- Application Java avec le JDK OpenJDK 21.0.1
- Projet Maven
- Utilisation de socket classiquent

## Docker

- Docker dans ce projet sera utilisé pour virtualisé le serveur est contient
également un serveur LDAP pour l'authentification des utilisateurs.








## Etapes suivies pour la sécurisation SSL (Côté serveur)

Temporairement mis de côté car pas encore fonctionnel.

### Etapne n°1 : Génération du certificat

- Générer un certificat auto-signé avec la commande suivante :

```bash
openssl req -newkey rsa:2048 -nodes -keyout key.pem -x509 -days 365 -out certificate.pem
```
Certificat auto-signé donc à ne pas utiliser en production.

### Etape n°2 : Importation du certificat dans le keystore

```bash
keytool -import -alias server -file certificate.pem -keystore keystore.jks
```

## Etapes suivies pour le client

### Etape n°1 : Création d'un Trustore

```bash
keytool -import -file server.crt -alias server -keystore truststore.jks
```



<div align="center">

```text
    _             _  ____                      _
   / \   __ _ _ __(_)/ ___| _ __ ___   __ _ _ __| |_
  / _ \ / _` | '__| |\___ \| '_ ` _ \ / _` | '__| __|
 / ___ \ (_| | |  | | ___) | | | | | | (_| | |  | |_
/_/   \_\__, |_|  |_| |____/|_| |_| |_|\__,_|_|   \__|
        |___/
```

### AgriSmart - Plateforme intelligente de gestion agricole (Desktop Client)

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net)
[![JavaFX](https://img.shields.io/badge/JavaFX-17-2D6A4F?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io)
[![Maven](https://img.shields.io/badge/Maven-3.x-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://mysql.com)
[![ESPRIT](https://img.shields.io/badge/ESPRIT-PIDEV_3A-D4A853?style=for-the-badge)](https://esprit.tn)

</div>

---

## A propos du projet

**AgriSmart Desktop** est le client Java/JavaFX d'une plateforme agricole intelligente developpee dans le cadre du **PIDEV 3A** a ESPRIT School of Engineering.

L'application JavaFX partage la meme base MySQL que le backend Symfony pour assurer une integration complete.

```text
Symfony Web App  ----------------------------.
                                              \
                                               >  MySQL (agrismart)
                                              /
JavaFX Desktop App --------------------------'
```

---

## Fonctionnalites

| Module | Description | Roles |
|--------|-------------|-------|
| Marketplace | Catalogue produits, commandes, wishlist, messagerie | Tous |
| Culture | Gestion des cultures et parcelles | Agriculteur |
| Taches | Suivi des taches et assignations | Employe, Admin |
| Utilisateurs | CRUD utilisateurs, roles et acces | Admin |
| Candidature | Gestion des candidatures et offres | Admin, Employe |

---

## Base de donnees

> Attention: la base est partagee avec le projet Symfony. Ne pas renommer les tables existantes.

Connexion JDBC:

```text
jdbc:mysql://localhost:3306/agrismart
```

Tables principales:

| Table | Description |
|-------|-------------|
| produit | Produits marketplace |
| commande | Commandes clients |
| commande_item | Lignes de commande |
| wishlist_item | Favoris utilisateurs |
| marketplace_conversation | Conversations acheteur/vendeur |
| marketplace_message | Messages des conversations |
| users | Utilisateurs |
| culture | Cultures |
| task | Taches |
| task_assignment | Assignations |
| offre | Offres candidature |
| demande | Demandes candidature |

---

## Installation et lancement

### Prerequis

- JDK 17 (Temurin recommande)
- IntelliJ IDEA (Maven integre)
- MySQL (XAMPP ou serveur local)

### Etapes

1. Cloner le repo

```bash
git clone https://github.com/aya284/agriSmartJava.git
cd agriSmartJava
```

2. Demarrer MySQL
3. Verifier la base `agrismart` dans phpMyAdmin
4. Ouvrir le projet dans IntelliJ
5. Lancer `MainApp.java`

Option Maven (si Maven est installe globalement):

```bash
mvn clean javafx:run
```

---
## Equipe et modules

| Developpeur | Module | Tables DB |
|-------------|--------|-----------|
| Aya Fdhila | Marketplace | produit, commande, wishlist_item, marketplace_* |
| Amine Arfaoui | Gestion des taches | task, task_assignment, suivi_tache |
| Abir Ben Khlifa | Utilisateurs | users, login_history, reset_password_request |
| Soumaya Dridi | Culture | culture, parcelle, consommation |
| Akrem Zaied | Candidature | offre, demande, ressource |

---

## Convention de code

Toujours reutiliser la connexion centralisee:

```java
Connection conn = MyConnection.getInstance().getConn();
```

Pattern service:

```java
public class ProduitService implements IService<Produit> {
    Connection conn = MyConnection.getInstance().getConn();

    @Override
    public void ajouter(Produit p) throws SQLException {
        String req = "INSERT INTO produit (...) VALUES (?, ?, ...)";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            // set params...
            ps.executeUpdate();
        }
    }
}
```

---

## Planning PIDEV

```text
Sprint 0   - Analyse & conception                  [OK]
Sprint 1   - Java Desktop (ce repo)                [IN PROGRESS]
Sprint 2   - Symfony Web                           [OK]
Integration - Java + Web (base partagee)           [IN PROGRESS]
```

---

<div align="center">

Made with love by ESPRIT PIDEV 3A - 2025/2026

Aya Fdhila - Amine Arfaoui - Abir Ben Khlifa - Soumaya Dridi - Akrem Zaied

Java 17 - JavaFX - Maven - MySQL - JDBC - MVC

</div>

# 🌿 AgriSmart - Système Intégré de Gestion Agricole (Desktop)

<div align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/JavaFX-17-2D6A4F?style=for-the-badge&logo=java&logoColor=white" />
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" />
  <img src="https://img.shields.io/badge/Maven-3.x-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" />
</div>

## 📖 Vue d'ensemble
**AgriSmart** est une application Desktop robuste conçue pour moderniser le secteur agricole. Ce projet se distingue par son architecture **hybride**, partageant une base de données MySQL avec un écosystème Web (Symfony), garantissant une continuité de service entre les bureaux administratifs et les utilisateurs web.

---

## 🚀 Modules & Détails Techniques

### 📂 Gestion RH & Recrutement (Candidatures)
* **Cycle de vie complet** : CRUD complet des offres d'emploi et des demandes.
* **Validation de données** : Contrôle strict du format des champs (ex: **CIN à 8 chiffres**).
* **Reporting** : Génération automatique de contrats ou de récapitulatifs au format **PDF** via `PDFBox`.
* **Interface Responsive** : Design optimisé avec `AtlantaFX` pour une expérience utilisateur moderne.

### 🤖 Intelligence Artificielle & Marketplace
* **Génération de contenu** : Intégration de l'API **Hugging Face** (Modèle Qwen/Llama) pour suggérer automatiquement des descriptions de produits agricoles.
* **Système de Paiement** : Intégration de l'API **Stripe** pour les transactions sécurisées par carte bancaire directement dans l'interface JavaFX.
* **Messagerie** : Système de communication entre acheteurs et vendeurs.

### 🛠️ Gestion des Opérations
* **Suivi des Cultures** : Monitoring des parcelles, suivi de la croissance et consommation des ressources.
* **Gestion des Tâches** : Attribution dynamique des travaux aux employés avec suivi d'avancement.
* **Sécurité** : Authentification centralisée avec chiffrement des mots de passe via **BCrypt**.

---

## 🛠️ Architecture & Stack Technique

* **Langage** : Java 17 (LTS)
* **Interface Graphique** : JavaFX avec le thème **AtlantaFX** (Thème sombre/clair).
* **Gestionnaire de dépendances** : Maven.
* **Base de données** : MySQL 8.0 (Connecteur JDBC).
* **Bibliothèques Clés** :
    * `jbcrypt` : Sécurité des accès.
    * `Stripe-java` : Flux financier.
    * `Jackson` / `JSON` : Parsing des données API.
    * `SikuliLibrary` : Automatisation des tests UI via Robot Framework.

---

## 🔧 Installation & Configuration

### 1. Configuration de la Base de Données
Assurez-vous que votre serveur MySQL est actif et exécutez le script suivant pour mettre à jour la structure :
```sql
-- Ajout de la colonne CIN indispensable au module Candidature
ALTER TABLE users ADD cin_number VARCHAR(8) AFTER email;

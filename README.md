# 🌿 AgriSmart - Client Desktop (PIDEV 3A)

<div align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/JavaFX-17-2D6A4F?style=for-the-badge&logo=java&logoColor=white" />
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" />
  <img src="https://img.shields.io/badge/Maven-3.x-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" />
</div>

---

## 📝 Présentation du Projet
**AgriSmart** est une plateforme desktop intelligente dédiée à la modernisation de la gestion agricole. Développée en **JavaFX**, elle s'intègre avec un écosystème web via une base de données **MySQL** centralisée, permettant une synchronisation en temps réel des ressources, des cultures et du capital humain.

---

## 🚀 Fonctionnalités par Module

### 💼 RH, Offres d'emploi & Candidatures
* **Cycle de vie complet** : CRUD des offres d'emploi et des demandes de candidature.
* **Validation Intelligente** : Formulaires sécurisés (ex: CIN à 8 chiffres) et analyse de CV par **IA**.
* **Reporting & Email** : Génération de documents **PDF** (PDFBox/OpenPDF) et notifications via **Jakarta Mail**.

### 🛒 Marketplace & Intelligence Artificielle
* **Commerce Connecté** : Panier, Wishlist et messagerie instantanée via **WebSocket**.
* **IA Générative** : Assistant basé sur **Google Gemini** et **Hugging Face** pour les descriptions de produits.
* **Paiement Sécurisé** : Tunnel d'achat intégré avec l'API **Stripe**.

### 🚜 Gestion Technique & RH
* **Cultures** : Monitoring des parcelles, diagnostic agricole et prédiction de rendement.
* **Tâches & Planning** : Affectation dynamique et synchronisation avec **Google Calendar**.
* **Sécurité** : Authentification **BCrypt**, Double facteur (**OTP**) et vérification faciale.

---

## 🛠️ Stack Technique
* **Interface** : JavaFX 17 (FXML, CSS), AtlantaFX.
* **Moteur** : Java 17, Maven.
* **Données** : MySQL 8.0, JDBC.
* **APIs Tierces** : Stripe, Google Gemini, Hugging Face, Google Calendar.
* **Outils Avancés** : Tess4J (OCR), WebSockets, Jakarta Mail.

---

## 📂 Structure du Projet
```text
agriSmartJava/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── controllers/   # Logique des interfaces
│   │   │   ├── entities/      # Modèles de données
│   │   │   ├── services/      # Logique métier (CRUD)
│   │   │   └── utils/         # Connexion DB & API Handlers
│   │   └── resources/
│   │       ├── Views/         # Fichiers FXML
│   │       ├── css/           # Design & Thèmes
│   │       └── config.properties # Configuration (API Keys)
└── pom.xml                    # Dépendances Maven

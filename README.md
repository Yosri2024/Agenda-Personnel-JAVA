# 📅 Agenda Personnel - JAVA

Application JavaFX de gestion d'événements personnels avec Oracle Database.

[![Java CI with Maven](https://github.com/Yosri2024/Agenda-Personnel-JAVA/actions/workflows/maven.yml/badge.svg)](https://github.com/Yosri2024/Agenda-Personnel-JAVA/actions)
![Java](https://img.shields.io/badge/Java-17%2B-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)
![Oracle](https://img.shields.io/badge/Oracle-23c-red)
![License](https://img.shields.io/badge/License-MIT-green)

## ✨ Fonctionnalités
- Gestion complète des événements (CRUD)
- Recherche par titre / catégorie / date
- Filtres : Tous, Aujourd'hui, Semaine, Importants
- Statistiques dynamiques (total, aujourd'hui, semaine, importants)
- Interface JavaFX moderne avec animations (Fade, Toast, Auto-refresh 5min)
- Raccourcis clavier : `Ctrl+N` nouveau, `Ctrl+F` recherche, `F5` actualiser, `F11` plein écran
- Persistance Oracle Database (`jdbc:oracle:thin:@localhost:1521/XEPDB1`)

## 🗂️ Structure
```
Agenda-Personnel-JAVA/
├── pom.xml                           # Maven (Java 17, JavaFX 21, ojdbc11)
├── src/main/java/personalagenda/
│   ├── PersonalAgenda.java           # Application principale (824 lignes, UI + logique)
│   ├── dao/EventDAO.java             # Oracle DAO (JDBC)
│   └── model/Event.java              # Modèle Event
├── src/main/resources/styles/
│   └── AgendaStyle.css               # Thème premium (pastel moderne)
├── sql/
│   └── schema.sql                    # Script Oracle
└── .github/workflows/maven.yml       # CI Ubuntu + JDK 17
```

## 🚀 Prérequis
- JDK 17+
- Maven 3.8+
- Oracle 21c+ / XE (`XEPDB1`)
- JavaFX 21 (géré par Maven)

## ⚙️ Installation Oracle
```sql
-- sql/schema.sql
CREATE USER agenda_user IDENTIFIED BY agenda123;
GRANT CONNECT, RESOURCE TO agenda_user;
ALTER USER agenda_user QUOTA UNLIMITED ON USERS;

CREATE TABLE events (
  event_id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  title VARCHAR2(200) NOT NULL,
  event_date DATE NOT NULL,
  event_time VARCHAR2(10),
  description VARCHAR2(1000),
  event_type VARCHAR2(50),
  importance_level NUMBER(1) CHECK (importance_level BETWEEN 1 AND 5)
);
```

## 🔧 Lancer
```bash
# Clone
git clone https://github.com/Yosri2024/Agenda-Personnel-JAVA.git
cd Agenda-Personnel-JAVA

# Configure EventDAO.java : url/user/password Oracle

# Compile
mvn clean compile

# Run (JavaFX)
mvn javafx:run

# Package jar
mvn clean package
java --module-path /path/to/javafx --add-modules javafx.controls,javafx.fxml -jar target/Agenda-Personnel-JAVA-1.0.0.jar
```

## 🎨 Thème
`AgendaStyle.css` : palette pastel moderne (violet `#6366F1`, turquoise, bleu ciel, vert menthe) + `getCategoryColor()` / `getTextColor()` dans `Event.java`

## 📄 Licence
MIT — voir `LICENSE`.

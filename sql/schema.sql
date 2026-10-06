-- Agenda Personnel - Oracle Schema
-- Compatible Oracle 21c / XE XEPDB1

-- Utilisateur
-- CREATE USER agenda_user IDENTIFIED BY agenda123;
-- GRANT CONNECT, RESOURCE TO agenda_user;
-- ALTER USER agenda_user QUOTA UNLIMITED ON USERS;
-- GRANT CREATE TABLE TO agenda_user;

-- Table events
CREATE TABLE events (
  event_id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  title VARCHAR2(200) NOT NULL,
  event_date DATE NOT NULL,
  event_time VARCHAR2(10),
  description VARCHAR2(1000),
  event_type VARCHAR2(50),
  importance_level NUMBER(1) DEFAULT 3 CHECK (importance_level BETWEEN 1 AND 5),
  created_at TIMESTAMP DEFAULT SYSTIMESTAMP
);

-- Index pour performance
CREATE INDEX idx_events_date ON events(event_date);
CREATE INDEX idx_events_type ON events(event_type);
CREATE INDEX idx_events_importance ON events(importance_level);

-- Jeu d'exemple varié : couvre Aujourd'hui, Cette semaine, À venir,
-- toutes les catégories et plusieurs niveaux d'importance.
INSERT INTO events (title, event_date, event_time, description, event_type, importance_level) VALUES ('Réunion sprint projet', DATE '2026-10-06', '09:00', 'Revue de sprint avec toute l équipe', 'travail', 4);
INSERT INTO events (title, event_date, event_time, description, event_type, importance_level) VALUES ('RDV médecin', DATE '2026-10-06', '15:30', 'Contrôle annuel, apporter le carnet de santé', 'santé', 5);
INSERT INTO events (title, event_date, event_time, description, event_type, importance_level) VALUES ('Courses supermarché', DATE '2026-10-07', '18:00', 'Liste : fruits, légumes, pain, lait', 'courses', 2);
INSERT INTO events (title, event_date, event_time, description, event_type, importance_level) VALUES ('Révisions examen Java', DATE '2026-10-08', '20:00', 'Chapitres collections et JDBC', 'étude', 4);
INSERT INTO events (title, event_date, event_time, description, event_type, importance_level) VALUES ('Anniversaire de maman', DATE '2026-10-09', NULL, 'Cadeau + gâteau, appeler la veille', 'famille', 5);
INSERT INTO events (title, event_date, event_time, description, event_type, importance_level) VALUES ('Match de foot entre amis', DATE '2026-10-10', '17:00', 'Terrain municipal, apporter les maillots', 'loisirs', 3);
INSERT INTO events (title, event_date, event_time, description, event_type, importance_level) VALUES ('Paiement des factures', DATE '2026-10-12', '10:00', 'Électricité + internet avant échéance', 'personnel', 3);
INSERT INTO events (title, event_date, event_time, description, event_type, importance_level) VALUES ('Dîner en famille', DATE '2026-10-18', '19:30', 'Couscous chez les parents', 'famille', 4);
INSERT INTO events (title, event_date, event_time, description, event_type, importance_level) VALUES ('Conférence tech', DATE '2026-10-20', '09:00', 'Sujet : cloud et IA, badge reçu par mail', 'travail', 3);
INSERT INTO events (title, event_date, event_time, description, event_type, importance_level) VALUES ('Bilan trimestriel', DATE '2026-09-28', '14:00', 'Déjà effectué, conservé pour archive', 'personnel', 2);
COMMIT;

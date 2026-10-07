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

-- Exemples
INSERT INTO events (title, event_date, event_time, description, event_type, importance_level) VALUES ('Réunion projet', DATE '2026-06-10', '09:00', 'Sprint review', 'travail', 4);
INSERT INTO events (title, event_date, event_time, description, event_type, importance_level) VALUES ('RDV médecin', DATE '2026-06-11', '15:30', 'Contrôle annuel', 'santé', 5);
INSERT INTO events (title, event_date, event_time, description, event_type, importance_level) VALUES ('Anniversaire famille', DATE '2026-06-15', NULL, 'Cadeau + gâteau', 'famille', 5);
COMMIT;

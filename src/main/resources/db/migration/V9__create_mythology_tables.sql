-- Norse mythology reference tables (deity_heroes, runes, artifacts, seasonal_events).
-- Source: norse-heathenry-pagan-api-dataset (CC0-1.0). Adapted from a Liquibase changeset to Flyway.

CREATE TABLE deity_heroes (
    id BIGSERIAL PRIMARY KEY,
    hero_key VARCHAR(50) NOT NULL UNIQUE,
    name_ru VARCHAR(50) NOT NULL,
    name_original VARCHAR(50) NOT NULL,
    pantheon VARCHAR(20) NOT NULL,
    rarity VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL,
    rune_anchor VARCHAR(30),
    skill_name VARCHAR(100) NOT NULL,
    skill_description VARCHAR(255) NOT NULL,
    lore_source VARCHAR(100),
    art_prompt TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE runes (
    id BIGSERIAL PRIMARY KEY,
    rune_key VARCHAR(50) NOT NULL UNIQUE,
    symbol VARCHAR(5) NOT NULL,
    name VARCHAR(30) NOT NULL,
    futhark VARCHAR(10) NOT NULL,
    tier INT NOT NULL,
    meaning VARCHAR(100),
    effect_type VARCHAR(30) NOT NULL,
    effect_value INT NOT NULL,
    effect_description VARCHAR(255) NOT NULL,
    flavor_text TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE artifacts (
    id BIGSERIAL PRIMARY KEY,
    artifact_key VARCHAR(50) NOT NULL UNIQUE,
    name_ru VARCHAR(50) NOT NULL,
    name_original VARCHAR(50) NOT NULL,
    rarity VARCHAR(20) NOT NULL,
    owner_hero_key VARCHAR(50),
    effect VARCHAR(255) NOT NULL,
    set_bonus VARCHAR(255),
    art_prompt TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE seasonal_events (
    id BIGSERIAL PRIMARY KEY,
    event_key VARCHAR(50) NOT NULL UNIQUE,
    name_ru VARCHAR(50) NOT NULL,
    name_original VARCHAR(80) NOT NULL,
    event_month VARCHAR(15) NOT NULL,
    duration_days INT NOT NULL DEFAULT 7,
    game_mechanics VARCHAR(255) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- TextCraft schema (MySQL 8). Run automatically by SchemaInitializer when tables are missing.
-- Tip: use a restricted DB user (SELECT, INSERT, UPDATE, DELETE only) in config/app.properties.

CREATE TABLE IF NOT EXISTS users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  google_id     VARCHAR(64) NULL UNIQUE,
  display_name  VARCHAR(255) NULL,
  avatar_url    VARCHAR(1024) NULL,
  username      VARCHAR(64) NULL UNIQUE,
  email         VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NULL,
  salt          VARCHAR(255) NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS documents (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT NOT NULL,
  title      VARCHAR(255) NOT NULL,
  doc_type   ENUM('RICH','CODE') NOT NULL DEFAULT 'RICH',
  language   VARCHAR(32) NULL,
  content    LONGTEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_doc_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_doc_user (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS document_versions (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT NOT NULL,
  content     LONGTEXT NULL,
  saved_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ver_doc FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
  INDEX idx_ver_doc (document_id, saved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_history (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  action      VARCHAR(32) NOT NULL,
  input_text  MEDIUMTEXT NULL,
  output_text MEDIUMTEXT NULL,
  model       VARCHAR(64) NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS compile_history (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  language    VARCHAR(16) NOT NULL,
  source_code MEDIUMTEXT NULL,
  stdin_text  TEXT NULL,
  output_text MEDIUMTEXT NULL,
  status      VARCHAR(64) NULL,
  run_ms      BIGINT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_cmp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_settings (
  user_id           BIGINT PRIMARY KEY,
  theme             ENUM('LIGHT','DARK','SYSTEM') NOT NULL DEFAULT 'LIGHT',
  default_font      VARCHAR(64) NOT NULL DEFAULT 'Arial',
  default_font_size INT NOT NULL DEFAULT 12,
  gemini_model      VARCHAR(64) NULL,
  voice_language    VARCHAR(16) NOT NULL DEFAULT 'en-US',
  voice_speed       DOUBLE NOT NULL DEFAULT 1.0,
  lite_mode         TINYINT(1) NOT NULL DEFAULT 0,
  CONSTRAINT fk_set_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS voice_usage (
  user_id      BIGINT NOT NULL,
  month        CHAR(7) NOT NULL,
  seconds_used INT NOT NULL DEFAULT 0,
  PRIMARY KEY (user_id, month),
  CONSTRAINT fk_voice_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS flashcard_sets (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  document_id BIGINT NULL,
  title       VARCHAR(255) NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_fs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_fs_doc FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS flashcards (
  id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  set_id   BIGINT NOT NULL,
  question TEXT NOT NULL,
  answer   TEXT NOT NULL,
  CONSTRAINT fk_fc_set FOREIGN KEY (set_id) REFERENCES flashcard_sets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quizzes (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  document_id BIGINT NULL,
  title       VARCHAR(255) NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_qz_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_qz_doc FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quiz_questions (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  quiz_id        BIGINT NOT NULL,
  question       TEXT NOT NULL,
  option_a       TEXT NOT NULL,
  option_b       TEXT NOT NULL,
  option_c       TEXT NOT NULL,
  option_d       TEXT NOT NULL,
  correct_option CHAR(1) NOT NULL,
  explanation    TEXT NULL,
  CONSTRAINT fk_qq_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quiz_attempts (
  id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  quiz_id  BIGINT NOT NULL,
  user_id  BIGINT NOT NULL,
  score    INT NOT NULL,
  total    INT NOT NULL,
  taken_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_qa_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE,
  CONSTRAINT fk_qa_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS users (
  user_id    INTEGER PRIMARY KEY AUTOINCREMENT,
  full_name  TEXT NOT NULL,
  cpf        TEXT NOT NULL UNIQUE,
  email      TEXT NOT NULL UNIQUE,
  job_title  TEXT,
  username   TEXT NOT NULL UNIQUE,
  password   TEXT NOT NULL,
  role       TEXT NOT NULL CHECK (role IN ('ADM','GER','OPER','DEV')),
  is_active  INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS projects (
  project_id  INTEGER PRIMARY KEY AUTOINCREMENT,
  name        TEXT NOT NULL,
  description TEXT,
  start_date  TEXT NOT NULL,
  due_date    TEXT,
  status      TEXT NOT NULL DEFAULT 'PLANEJADO'
              CHECK (status IN ('PLANEJADO','ANDAMENTO','CONCLUIDO','CANCELADO')),
  manager_id  INTEGER NOT NULL,
  FOREIGN KEY (manager_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS teams (
  team_id     INTEGER PRIMARY KEY AUTOINCREMENT,
  team_name   TEXT NOT NULL UNIQUE,
  description TEXT
);

CREATE TABLE IF NOT EXISTS team_members (
  team_id      INTEGER NOT NULL,
  user_id      INTEGER NOT NULL,
  role_in_team TEXT,
  PRIMARY KEY (team_id, user_id),
  FOREIGN KEY (team_id) REFERENCES teams(team_id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS project_teams (
  project_id INTEGER NOT NULL,
  team_id    INTEGER NOT NULL,
  PRIMARY KEY (project_id, team_id),
  FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
  FOREIGN KEY (team_id)    REFERENCES teams(team_id)    ON DELETE CASCADE
);

-- Tarefas
CREATE TABLE IF NOT EXISTS tasks (
  task_id      INTEGER PRIMARY KEY AUTOINCREMENT,
  project_id   INTEGER NOT NULL,
  title        TEXT NOT NULL,
  description  TEXT,
  assignee_id  INTEGER, -- deve ser membro do projeto (via view abaixo)
  priority     TEXT NOT NULL DEFAULT 'MEDIA' CHECK (priority IN ('BAIXA','MEDIA','ALTA','CRITICA')),
  status       TEXT NOT NULL DEFAULT 'FAZER'   CHECK (status IN ('FAZER','ANDAMENTO','BLOQUEADO','REVISANDO','CONCLUIDO')),
  estimate_h   REAL,
  spent_h      REAL NOT NULL DEFAULT 0,
  due_date     TEXT,
  created_at   TEXT NOT NULL DEFAULT (DATE('now')),
  FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
  FOREIGN KEY (assignee_id) REFERENCES users(user_id)
);

CREATE INDEX IF NOT EXISTS ix_tasks_project ON tasks(project_id);
CREATE INDEX IF NOT EXISTS ix_tasks_assignee ON tasks(assignee_id);
CREATE INDEX IF NOT EXISTS ix_tasks_status_due ON tasks(status, due_date);

-- View: membros que participam de um projeto por meio das equipes alocadas ao projeto
DROP VIEW IF EXISTS project_members_v;
CREATE VIEW project_members_v AS
SELECT DISTINCT pt.project_id, tm.user_id
FROM project_teams pt
JOIN team_members tm ON tm.team_id = pt.team_id;


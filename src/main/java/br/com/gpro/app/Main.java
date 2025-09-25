package br.com.gpro.app;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import br.com.gpro.controller.ProjectController;
import br.com.gpro.controller.TaskController;
import br.com.gpro.controller.TeamController;
import br.com.gpro.controller.UserController;
import br.com.gpro.dao.ProjectDao;
import br.com.gpro.dao.ProjectDaoSqlite;
import br.com.gpro.dao.TaskDao;
import br.com.gpro.dao.TaskDaoSqlite;
import br.com.gpro.dao.TeamDaoSqlite;
import br.com.gpro.dao.UserDao;
import br.com.gpro.dao.UserDaoSqlite;
import br.com.gpro.infra.DbInit;
import br.com.gpro.model.Priority;
import br.com.gpro.model.Project;
import br.com.gpro.model.ProjectStatus;
import br.com.gpro.model.Role;
import br.com.gpro.model.TaskStatus;


public class Main {
    private static final Scanner SC = new Scanner(System.in);

    public static void main(String[] args) {
        ProjectDao projectDao = new ProjectDaoSqlite();
        UserDao userDao = new UserDaoSqlite();
        TaskDao taskDao = new TaskDaoSqlite();
        // 1) Inicializa BD (cria tabelas e seed)
        DbInit.run("./db/schema.sql", null);
        verificarTabelas();

        var projDao = new ProjectDaoSqlite();
        var teamDao = new TeamDaoSqlite();
        var teamCtl = new TeamController(teamDao, userDao);
        var auth = new AuthService(userDao);
        var reports = new ReportService();
        ProjectController projCtl = new ProjectController(projectDao, userDao, taskDao);
        TaskController taskCtl = new TaskController(taskDao, projectDao, userDao);
        UserController userCtl = new UserController(userDao);

        // 2) Garante admin
        Long adminId = userCtl.criarAdminSeNaoExiste();
        System.out.println("Admin OK (id=" + adminId + ")\n");

        // 3) Loop CLI
        while (true) {
            System.out.println("Usuário: " + (auth.isLogged() ? auth.getCurrentUser().getUsername() : "<não logado>"));
            System.out.println("""
                ==== MENU ====
                L) Login
                O) Logout
                1) Listar usuários
                2) Criar projeto
                3) Listar projetos
                4) Criar equipe
                5) Listar equipes
                6) Adicionar membro à equipe
                7) Alocar equipe em projeto
                8) Criar tarefa
                9) Listar tarefas do projeto
                10) Mover status da tarefa
                11) Reatribuir tarefa
                12) Progresso do projeto
                13) Relatório: Tarefas atrasadas
                14) Relatório: Carga por usuário (est x spent)
                15) Atualizar projeto
                16) Excluir projeto
                17) Atualizar tarefa (básico)
                18) Excluir tarefa
                19) Excluir equipe
                20) Exportar CSV: Tarefas atrasadas
                21) Criar usuário
                22) Atualizar usuário (básico)
                23) Alterar senha (próprio)
                24) Alterar senha (admin)
                25) Alterar perfil do usuário (ADMIN/MANAGER/MEMBER)
                26) Ativar/Desativar usuário
                0) Sair
                Escolha: """);
            String op = SC.nextLine().trim();

            try {
                switch (op) {
                    case "L", "l" -> {
                    System.out.print("Username: "); String u = SC.nextLine().trim();
                    System.out.print("Senha: "); String p = SC.nextLine().trim();
                    if (auth.login(u, p)) System.out.println("Login OK.");
                    else System.out.println("Credenciais inválidas.");
                    }
                    case "O", "o" -> { auth.logout(); System.out.println("Logout."); }
                    case "1" -> listarUsuarios(userCtl);
                    case "2" -> {
                        criarProjeto(projCtl);
                        exigirPerfil(auth, Role.ADM, Role.GER);
                    }
                    case "3" -> listarProjetos(projCtl);
                    case "4" -> {
                        exigirPerfil(auth, Role.ADM, Role.GER);
                        System.out.print("Nome da equipe: ");
                        String tn = SC.nextLine().trim();
                        System.out.print("Descrição (opcional): ");
                        String td = SC.nextLine().trim();
                        Long id = teamCtl.criarEquipe(tn, td);
                        System.out.println("Equipe criada id=" + id);
                    }
                    case "5" -> {
                        var teams = teamCtl.listarEquipes();
                        if (teams.isEmpty()) System.out.println("Sem equipes.");
                        else teams.forEach(t -> System.out.printf("- [%d] %s%n", t.getId(), t.getName()));
                    }
                    case "6" -> {
                        exigirPerfil(auth, Role.ADM, Role.GER);
                        System.out.print("ID da equipe: ");
                        Long teamId = Long.parseLong(SC.nextLine().trim());
                        System.out.print("ID do usuário: ");
                        Long userId = Long.parseLong(SC.nextLine().trim());
                        System.out.print("Papel na equipe (opcional): ");
                        String roleInTeam = SC.nextLine().trim();
                        teamCtl.adicionarMembro(teamId, userId, roleInTeam.isBlank() ? null : roleInTeam);
                        System.out.println("Membro adicionado.");
                    }
                    case "7" -> {
                        exigirPerfil(auth, Role.ADM, Role.GER);
                        System.out.print("ID do projeto: ");
                        Long projectId = Long.parseLong(SC.nextLine().trim());
                        System.out.print("ID da equipe: ");
                        Long teamId = Long.parseLong(SC.nextLine().trim());
                        projCtl.alocarEquipeEmProjeto(projectId, teamId);
                        System.out.println("Equipe alocada ao projeto.");
                    }
                    case "8" -> {
                        exigirPerfil(auth, Role.ADM, Role.GER, Role.OPER);
                        System.out.print("ID do projeto: ");
                        Long pid = Long.parseLong(SC.nextLine().trim());
                        System.out.print("Título: ");
                        String title = SC.nextLine().trim();
                        System.out.print("Descrição (opcional): ");
                        String desc = SC.nextLine().trim();
                        System.out.print("ID do responsável (ENTER p/ vazio): ");
                        String aid = SC.nextLine().trim();
                        Long assigneeId = aid.isBlank() ? null : Long.parseLong(aid);
                        System.out.print("Prioridade [LOW/MEDIUM/HIGH/CRITICAL] (ENTER=MEDIUM): ");
                        String pr = SC.nextLine().trim();
                        Priority prio = pr.isBlank() ? Priority.MEDIA : Priority.valueOf(pr.toUpperCase());
                        System.out.print("Estimativa (horas, ENTER p/ vazio): ");
                        String est = SC.nextLine().trim();
                        Double estimateH = est.isBlank() ? null : Double.parseDouble(est);
                        System.out.print("Vencimento YYYY-MM-DD (ENTER p/ vazio): ");
                        String dd = SC.nextLine().trim();
                        java.time.LocalDate due = dd.isBlank() ? null : java.time.LocalDate.parse(dd);

                        Long id = taskCtl.criarTarefa(pid, title, desc, assigneeId, prio, estimateH, due);
                        System.out.println("Tarefa criada id=" + id);
                    }
                    case "9" -> {
                        System.out.print("ID do projeto: ");
                        Long pid = Long.parseLong(SC.nextLine().trim());
                        var ts = taskCtl.listarPorProjeto(pid);
                        if (ts.isEmpty()) System.out.println("Sem tarefas.");
                        else ts.forEach(t -> System.out.printf("- [%d] %s | %s | %s | est=%.1f | spent=%.1f | due=%s | assigneeId=%s%n",
                                t.getId(), t.getTitle(), t.getPriority(), t.getStatus(),
                                t.getEstimateH() == null ? 0.0 : t.getEstimateH(),
                                t.getSpentH() == null ? 0.0 : t.getSpentH(),
                                t.getDueDate(), t.getAssigneeId()));
                    }
                    case "10" -> {
                        exigirPerfil(auth, Role.ADM, Role.GER, Role.OPER);
                        System.out.print("ID da tarefa: ");
                        Long tid = Long.parseLong(SC.nextLine().trim());
                        System.out.print("Novo status [TODO/DOING/BLOCKED/REVIEW/DONE]: ");
                        TaskStatus st = TaskStatus.valueOf(SC.nextLine().trim().toUpperCase());
                        Double add = null;
                        if (st == TaskStatus.CONCLUIDO) {
                            System.out.print("Horas a somar para concluir (>=0): ");
                            add = Double.parseDouble(SC.nextLine().trim());
                        }
                        taskCtl.moverStatus(tid, st, add);
                        System.out.println("Status atualizado.");
                    }
                    case "11" -> {
                        exigirPerfil(auth, Role.ADM, Role.GER, Role.OPER);
                        System.out.print("ID da tarefa: ");
                        Long tid = Long.parseLong(SC.nextLine().trim());
                        System.out.print("Novo responsável (userId): ");
                        Long uid = Long.parseLong(SC.nextLine().trim());
                        taskCtl.reatribuir(tid, uid);
                        System.out.println("Tarefa reatribuída.");
                    }
                    case "12" -> {
                        System.out.print("ID do projeto: ");
                        Long pid = Long.parseLong(SC.nextLine().trim());
                        System.out.println(projCtl.progressoProjeto(pid));
                    }
                    case "13" -> {
                        var linhas = reports.tarefasAtrasadas();
                        if (linhas.isEmpty()) System.out.println("Sem tarefas atrasadas. 🎉");
                        else linhas.forEach(System.out::println);
                    }
                    case "14" -> {
                        var linhas = reports.cargaPorUsuario();
                        if (linhas.isEmpty()) System.out.println("Sem dados de carga (sem tarefas ou sem responsáveis).");
                        else linhas.forEach(System.out::println);
                    }
                    case "15" -> { // atualizar projeto
                        exigirPerfil(auth, Role.ADM, Role.GER);
                        System.out.print("ID do projeto: "); Long pid = Long.parseLong(SC.nextLine().trim());
                        var p = projCtl.listarProjetos().stream().filter(x -> x.getId().equals(pid)).findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado"));
                        System.out.print("Novo nome (ENTER mantém '" + p.getName() + "'): ");
                        String nn = SC.nextLine().trim(); if (!nn.isBlank()) p.setName(nn);
                        System.out.print("Nova descrição (ENTER mantém): ");
                        String nd = SC.nextLine().trim(); if (!nd.isBlank()) p.setDescription(nd);
                        System.out.print("Nova data início (YYYY-MM-DD, ENTER mantém " + p.getStartDate() + "): ");
                        String ns = SC.nextLine().trim(); if (!ns.isBlank()) p.setStartDate(java.time.LocalDate.parse(ns));
                        System.out.print("Nova data término (YYYY-MM-DD, ENTER mantém " + p.getDueDate() + "): ");
                        String ndue = SC.nextLine().trim(); if (!ndue.isBlank()) p.setDueDate(java.time.LocalDate.parse(ndue)); else p.setDueDate(p.getDueDate());
                        System.out.print("Novo status [PLANNED/IN_PROGRESS/DONE/CANCELLED] (ENTER mantém " + p.getStatus() + "): ");
                        String st = SC.nextLine().trim(); if (!st.isBlank()) p.setStatus(br.com.gpro.model.ProjectStatus.valueOf(st.toUpperCase()));
                        projCtl.atualizarProjeto(p);
                        System.out.println("Projeto atualizado.");
                    }
                    case "16" -> {
                        exigirPerfil(auth, Role.ADM, Role.GER);
                        System.out.print("ID do projeto a excluir: ");
                        Long pid = Long.parseLong(SC.nextLine().trim());
                        projCtl.excluirProjeto(pid);
                        System.out.println("Projeto excluído.");
                    }
                    case "17" -> {
                        exigirPerfil(auth, Role.ADM, Role.GER, Role.OPER);
                        System.out.print("ID da tarefa: "); Long tid = Long.parseLong(SC.nextLine().trim());
                        System.out.print("Novo título: "); String t = SC.nextLine().trim();
                        System.out.print("Nova descrição (opcional): "); String d = SC.nextLine().trim();
                        System.out.print("Prioridade [LOW/MEDIUM/HIGH/CRITICAL] (ENTER mantém MEDIUM se não souber): ");
                        String pr = SC.nextLine().trim();
                        var prio = pr.isBlank() ? br.com.gpro.model.Priority.MEDIA : br.com.gpro.model.Priority.valueOf(pr.toUpperCase());
                        System.out.print("Nova estimativa (horas, vazio = manter/limpar): "); String eh = SC.nextLine().trim();
                        Double est = eh.isBlank() ? null : Double.parseDouble(eh);
                        System.out.print("Novo vencimento (YYYY-MM-DD, vazio=sem): "); String dd = SC.nextLine().trim();
                        java.time.LocalDate due = dd.isBlank() ? null : java.time.LocalDate.parse(dd);
                        taskCtl.atualizarTarefaBasica(tid, t, d, prio, est, due);
                        System.out.println("Tarefa atualizada.");
                    }
                    case "18" -> {
                        exigirPerfil(auth, Role.ADM, Role.GER);
                        System.out.print("ID da tarefa a excluir: ");
                        Long tid = Long.parseLong(SC.nextLine().trim());
                        taskCtl.excluirTarefa(tid);
                        System.out.println("Tarefa excluída.");
                    }
                    case "19" -> {
                        exigirPerfil(auth, Role.ADM, Role.GER);
                        System.out.print("ID da equipe a excluir: ");
                        Long teamId = Long.parseLong(SC.nextLine().trim());
                        teamCtl.excluirEquipe(teamId);
                        System.out.println("Equipe excluída.");
                    }
                    case "20" -> {
                        var rows = reports.tarefasAtrasadasCsv();
                        var path = br.com.gpro.app.CsvExporter.write("relatorio_tarefas_atrasadas.csv", rows);
                        System.out.println("CSV gerado em: " + path.toAbsolutePath());
                    }
                    case "21" -> {
                        exigirPerfil(auth, Role.ADM);
                        System.out.print("Nome: "); String nome = SC.nextLine().trim();
                        System.out.print("CPF (apenas dígitos): "); String cpf = SC.nextLine().trim();
                        System.out.print("E-mail: "); String email = SC.nextLine().trim();
                        System.out.print("Cargo: "); String cargo = SC.nextLine().trim();
                        System.out.print("Username: "); String username = SC.nextLine().trim();
                        System.out.print("Senha: "); String senha = SC.nextLine().trim();
                        System.out.print("Perfil [ADM/GER/OPER/DEV] (ENTER=OPER): ");
                        String r = SC.nextLine().trim();
                        Role role = r.isEmpty() ? Role.OPER : Role.valueOf(r.toUpperCase());
                        Long id = userCtl.criarUsuario(nome, cpf, email, cargo, username, senha, role);
                        System.out.println("Usuário criado id=" + id);
                    }
                    case "22" -> {
                        exigirPerfil(auth, Role.ADM);
                        System.out.print("ID do usuário: "); Long id = Long.parseLong(SC.nextLine().trim());
                        System.out.print("Novo nome: "); String nome = SC.nextLine().trim();
                        System.out.print("Novo e-mail: "); String email = SC.nextLine().trim();
                        System.out.print("Novo cargo: "); String cargo = SC.nextLine().trim();
                        userCtl.atualizarBasico(id, nome, email, cargo);
                        System.out.println("Usuário atualizado.");
                    }
                    case "23" -> {
                        exigirPerfil(auth, Role.ADM, Role.GER, Role.OPER);
                        Long id = auth.getCurrentUser().getId();
                        System.out.print("Senha atual: "); String s1 = SC.nextLine().trim();
                        System.out.print("Nova senha: "); String s2 = SC.nextLine().trim();
                        userCtl.alterarSenha(id, s1, s2, false);
                        System.out.println("Senha alterada.");
                    }
                    case "24" -> {
                        exigirPerfil(auth, Role.ADM);
                        System.out.print("ID do usuário: "); Long id = Long.parseLong(SC.nextLine().trim());
                        System.out.print("Nova senha: "); String s2 = SC.nextLine().trim();
                        userCtl.alterarSenha(id, null, s2, true);
                        System.out.println("Senha alterada pelo admin.");
                    }
                    case "25" -> {
                        exigirPerfil(auth, Role.ADM);
                        System.out.print("ID do usuário: "); Long id = Long.parseLong(SC.nextLine().trim());
                        System.out.print("Novo perfil [ADMIN/MANAGER/MEMBER]: ");
                        Role role = Role.valueOf(SC.nextLine().trim().toUpperCase());
                        userCtl.alterarPerfil(id, role);
                        System.out.println("Perfil alterado.");
                    }
                    case "26" -> {
                        exigirPerfil(auth, Role.ADM);
                        System.out.print("ID do usuário: "); Long id = Long.parseLong(SC.nextLine().trim());
                        System.out.print("Ativar? (S/N): "); String s = SC.nextLine().trim().toUpperCase();
                        userCtl.setAtivo(id, s.startsWith("S"));
                        System.out.println("Status atualizado.");
                    }

                    case "0" -> { System.out.println("Até mais!"); return; }
                    default -> System.out.println("Opção inválida.");
                }
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage());
            }
            System.out.println();
        }
    }

    private static void verificarTabelas() {
        String q = """
        SELECT name FROM sqlite_master 
        WHERE type='table' AND name IN ('users','projects','teams','tasks')
        ORDER BY name
        """;
        try (var c = br.com.gpro.infra.ConnectionFactory.get();
            var st = c.createStatement();
            var rs = st.executeQuery(q)) {
            System.out.print("Tabelas encontradas: ");
            boolean any = false;
            while (rs.next()) { any = true; System.out.print(rs.getString(1) + " "); }
            System.out.println(any ? "" : "(nenhuma)");
        } catch (Exception e) {
            System.out.println("Falha ao verificar tabelas: " + e.getMessage());
        }
    }

    private static void exigirPerfil(AuthService auth, Role... perfis) {
        if (!auth.isLogged()) throw new IllegalStateException("Faça login.");
        Role r = auth.getCurrentUser().getRole();
        for (Role p : perfis) if (r == p) return;
        throw new IllegalStateException("Ação não permitida para o perfil: " + r);
    }

    private static void listarUsuarios(UserController ctl) {
        var users = ctl.listarUsuarios();
        if (users.isEmpty()) { System.out.println("Sem usuários cadastrados."); return; }
        users.forEach(u -> System.out.printf("- [%d] %s (%s)%n", u.getId(), u.getFullName(), u.getRole()));
    }

    private static void criarProjeto(ProjectController ctl) {
        System.out.print("Nome do projeto: ");
        String name = SC.nextLine().trim();

        System.out.print("Descrição (opcional): ");
        String desc = SC.nextLine().trim();

        LocalDate start = lerDataObrigatoria("Data de início (YYYY-MM-DD): ");
        LocalDate due = lerDataOpcional("Data de término (YYYY-MM-DD) [ENTER p/ vazio]: ");

        ProjectStatus status = escolherStatus();

        System.out.print("ID do gerente responsável: ");
        Long managerId = Long.parseLong(SC.nextLine().trim());

        Long id = ctl.criarProjeto(name, desc, start, due, status, managerId);
        System.out.println("Projeto criado com id=" + id);
    }

    private static ProjectStatus escolherStatus() {
        System.out.print("Status [PLANNED/IN_PROGRESS/DONE/CANCELLED] (ENTER=PLANNED): ");
        String s = SC.nextLine().trim();
        if (s.isEmpty()) return ProjectStatus.PLANEJADO;
        return ProjectStatus.valueOf(s.toUpperCase());
    }

    private static LocalDate lerDataObrigatoria(String label) {
        while (true) {
            System.out.print(label);
            String s = SC.nextLine().trim();
            try { return LocalDate.parse(s); }
            catch (Exception e) { System.out.println("Data inválida. Formato esperado: YYYY-MM-DD."); }
        }
    }

    private static LocalDate lerDataOpcional(String label) {
        System.out.print(label);
        String s = SC.nextLine().trim();
        if (s.isEmpty()) return null;
        try { return LocalDate.parse(s); }
        catch (Exception e) { System.out.println("Data inválida. Ignorando."); return null; }
    }

    private static void listarProjetos(ProjectController ctl) {
        List<Project> ps = ctl.listarProjetos();
        if (ps.isEmpty()) { System.out.println("Sem projetos."); return; }
        for (Project p : ps) {
            System.out.printf("- [%d] %s | %s | início=%s | fim=%s | gerenteId=%d%n",
                    p.getId(), p.getName(), p.getStatus(),
                    p.getStartDate(), p.getDueDate(), p.getManagerId());
        }
    }
}

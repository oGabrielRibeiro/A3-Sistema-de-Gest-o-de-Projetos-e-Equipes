package br.com.gpro.app;

import java.time.LocalDate;
import java.util.List;

import br.com.gpro.controller.ProjectController;
import br.com.gpro.controller.TaskController;
import br.com.gpro.controller.TeamController;
import br.com.gpro.controller.UserController;
import br.com.gpro.dao.ProjectDao;
import br.com.gpro.dao.ProjectDaoSqlite;
import br.com.gpro.dao.TaskDao;
import br.com.gpro.dao.TaskDaoSqlite;
import br.com.gpro.dao.TeamDao;
import br.com.gpro.dao.TeamDaoSqlite;
import br.com.gpro.dao.UserDao;
import br.com.gpro.dao.UserDaoSqlite;
import br.com.gpro.infra.DbInit;
import br.com.gpro.model.Project;
import br.com.gpro.model.ProjectStatus;
import br.com.gpro.model.Task;
import br.com.gpro.model.TaskStatus;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import br.com.gpro.app.AuthService;
import br.com.gpro.model.Role;
import br.com.gpro.model.User;
import javafx.scene.control.Control;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.util.StringConverter;
import javafx.scene.control.ListCell;

public class AppFX extends Application {

    // DAOs/Controllers
    private final UserDao userDao = new UserDaoSqlite();
    private final ProjectDao projectDao = new ProjectDaoSqlite();
    private final TeamDao teamDao = new TeamDaoSqlite();
    private final TaskDao taskDao = new TaskDaoSqlite();

    private final UserController userCtl = new UserController(userDao);
    private final ProjectController projCtl = new ProjectController(projectDao, userDao, taskDao);
    private final TeamController teamCtl = new TeamController(teamDao, userDao);
    private final TaskController taskCtl = new TaskController(taskDao, projectDao, userDao);

    // Dados UI
    private final ObservableList<Project> projetos = FXCollections.observableArrayList();
    private final ObservableList<Task> tarefas = FXCollections.observableArrayList();

    // Autenticação
    private final AuthService auth = new AuthService(userDao);

    // Nós de UI que serão bloqueados por perfil
    private Button btnProjetoCriar, btnProjetoExcluir, btnProjetoProgresso;
    private Button btnTaskCriar, btnTaskDone, btnTaskReattr;
    private Button btnEquipeCriar, btnEquipeExcluir, btnAddMember, btnAlocarEquipe;

    // Usuários – botões controlados por RBAC (ADMIN)
    private Button btnUserCriar, btnUserAtualizar, btnUserAtivar, btnUserPerfil, btnUserResetSenha;

    // (se ainda não existir)
    private final ObservableList<br.com.gpro.model.User> usuarios = FXCollections.observableArrayList();

    // Filtros (Projetos)
    private ComboBox<br.com.gpro.model.ProjectStatus> pfStatus;
    private TextField pfBuscaNome;
    private Button btnProjExportCsv;

    // Filtros (Tarefas)
    private ComboBox<br.com.gpro.model.TaskStatus> tfStatus;
    private ComboBox<br.com.gpro.model.Priority> tfPrio;
    private TextField tfAssigneeId;
    private CheckBox tfSomenteAtrasadas;
    private Button btnTaskExportCsv;           // responsável ao criar tarefa

    // Dashboard
    private Label kpiProjetos, kpiTarefas, kpiAtrasadas, kpiEsforco;
    private PieChart chartProjetosStatus;
    private BarChart<String, Number> chartTarefasStatus;
    private Button btnDashRefresh;

    // Combos e tabelas da aba Equipes
    private ComboBox<br.com.gpro.model.Team> cbTeamSelect, cbTeamForProject;
    private ComboBox<br.com.gpro.model.User> cbUserToAdd;
    private ComboBox<br.com.gpro.model.Project> cbProjectSelect;
    private TableView<br.com.gpro.model.User> tvMembers;

    // Combos e tabelas da aba Projetos
    private ComboBox<br.com.gpro.model.User> cbGerente;

    // Combos e tabelas da aba Tarefas
    private ComboBox<br.com.gpro.model.User> cbAssignee; 
    private ComboBox<br.com.gpro.model.Project> cbProjeto;
    private ComboBox<br.com.gpro.model.User> tfAssigneeCombo;  

    // ---- TAREFAS: mudança de status ----
    private ComboBox<br.com.gpro.model.TaskStatus> cbNovoStatus;
    private Button btnTaskMover;


    @Override
    public void start(Stage stage) {
        // BD + admin
        DbInit.run("./db/schema.sql", null);
        userCtl.criarAdminSeNaoExiste();

        // Janela base com MenuBar + Tabs
        TabPane tabs = new TabPane();
        Tab tabDash = tabDashboard();
        Tab tabProj = tabProjetos(1L); // default managerId qualquer (pode ajustar depois)
        Tab tabTasks = tabTarefas();
        Tab tabTeams = tabEquipes();
        Tab tabUsers = tabUsuarios();
        tabs.getTabs().addAll(tabDash, tabProj, tabTasks, tabTeams, tabUsers);

        MenuBar menuBar = buildMenuBar(stage, tabs);

        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(tabs);

        Scene scene = new Scene(root, 980, 640);
        stage.setScene(scene);
        stage.setTitle("Gestão de Projetos e Equipes - JavaFX");
        stage.show();

        // Fluxo de login obrigatório
        doLogin(stage);

        // Carrega dados iniciais
        refreshProjetos();
    }

    private Tab tabProjetos(Long defaultManagerId) {
        // Tabela
        TableView<Project> tv = new TableView<>(projetos);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Project, Long> cId = new TableColumn<>("ID");
        cId.setCellValueFactory(new PropertyValueFactory<>("id"));
        cId.setMaxWidth(80);

        TableColumn<Project, String> cNome = new TableColumn<>("Nome");
        cNome.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Project, String> cStatus = new TableColumn<>("Status");
        cStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Project, LocalDate> cIni = new TableColumn<>("Início");
        cIni.setCellValueFactory(new PropertyValueFactory<>("startDate"));

        TableColumn<Project, LocalDate> cFim = new TableColumn<>("Término");
        cFim.setCellValueFactory(new PropertyValueFactory<>("dueDate"));

        TableColumn<Project, Long> cMgr = new TableColumn<>("GerenteId");
        cMgr.setCellValueFactory(new PropertyValueFactory<>("managerId"));

        tv.getColumns().addAll(cId, cNome, cStatus, cIni, cFim, cMgr);

        // Formulário de criação
        TextField fNome = new TextField();
        fNome.setPromptText("Nome do projeto");

        TextField fDesc = new TextField();
        fDesc.setPromptText("Descrição (opcional)");

        pfStatus = new ComboBox<>();
        pfStatus.getItems().setAll(br.com.gpro.model.ProjectStatus.values());
        pfStatus.getItems().add(0, null); // opção “todos”
        pfStatus.getSelectionModel().select(0);

        pfBuscaNome = new TextField();
        pfBuscaNome.setPromptText("Buscar por nome...");

        DatePicker fInicio = new DatePicker(LocalDate.now());
        DatePicker fFim = new DatePicker();

        ComboBox<ProjectStatus> cbStatus = new ComboBox<>();
        cbStatus.getItems().setAll(ProjectStatus.values());
        cbStatus.getSelectionModel().select(ProjectStatus.PLANEJADO);

        cbGerente = new ComboBox<>();
        // sug.: listar só GER/ADM (e DEV se quiser). Aqui filtro no cliente:
        var todos = userCtl.listarUsuarios();
        var apenasGestores = todos.stream()
            .filter(u -> u.getRole() == br.com.gpro.model.Role.GER || u.getRole() == br.com.gpro.model.Role.ADM)
            .toList();
        cbGerente.getItems().setAll(apenasGestores);
        setupUserCombo(cbGerente);
        // seleção default (se existia o defaultManagerId)
        apenasGestores.stream().filter(u -> u.getId() == defaultManagerId).findFirst()
            .ifPresent(u -> cbGerente.getSelectionModel().select(u));

        btnProjetoCriar = new Button("Criar Projeto");
        btnProjetoCriar.setOnAction(e -> {
            try {
                String nome = fNome.getText().trim();
                String desc = fDesc.getText().trim();
                LocalDate ini = fInicio.getValue();
                LocalDate fim = fFim.getValue();
                ProjectStatus st = cbStatus.getValue();
                var gSel = cbGerente.getValue();
                if (gSel == null) { alertErro("Gerente", "Selecione um gerente."); return; }
                Long id = projCtl.criarProjeto(nome, desc, ini, fim, st, gSel.getId());

                alertInfo("Projeto criado", "ID = " + id);
                refreshProjetos();
                clearFields(fNome, fDesc); fFim.setValue(null);
            } catch (Exception ex) { alertErro("Erro ao criar projeto", ex.getMessage()); }
        });

        btnProjetoExcluir = new Button("Excluir Selecionado");
        btnProjetoExcluir.setOnAction(e -> {
            Project sel = tv.getSelectionModel().getSelectedItem();
            if (sel == null) { alertErro("Atenção", "Selecione um projeto para excluir."); return; }
            try {
                projCtl.excluirProjeto(sel.getId());
                refreshProjetos();
            } catch (Exception ex) { alertErro("Erro ao excluir", ex.getMessage()); }
        });
        
        btnTaskDone = new Button("Concluir Selecionada (+horas)");
        btnTaskDone.setOnAction(e -> {
            var sel = tv.getSelectionModel().getSelectedItem();
            if (sel == null) { alertErro("Atenção", "Selecione um projeto."); return; }
            try {
                projCtl.concluirProjeto(sel.getId());
                alertInfo("Projeto", "Projeto concluído com sucesso.");
                refreshProjetos();
            } catch (Exception ex) {
                alertErro("Não foi possível concluir", ex.getMessage());
            }
        });

        btnProjetoProgresso = new Button("Progresso do Selecionado");
        btnProjetoProgresso.setOnAction(e -> {
            Project sel = tv.getSelectionModel().getSelectedItem();
            if (sel == null) { alertErro("Atenção", "Selecione um projeto."); return; }
            try {
                String s = projCtl.progressoProjeto(sel.getId());
                alertInfo("Progresso", s);
            } catch (Exception ex) { alertErro("Erro no relatório", ex.getMessage()); }
        });

        Button btnAplicarFiltro = new Button("Filtrar");
        btnAplicarFiltro.setOnAction(e -> refreshProjetos()); // reaproveita método e filtra no cliente

        btnProjExportCsv = new Button("Exportar CSV (Projetos visíveis)");
        btnProjExportCsv.setOnAction(e -> exportProjetosVisiveis(tv));

        HBox filtros = new HBox(10, new Label("Status:"), pfStatus, pfBuscaNome, btnAplicarFiltro, btnProjExportCsv);
        filtros.setPadding(new Insets(10));

        HBox form = new HBox(10, fNome, fDesc, fInicio, fFim, cbStatus, cbGerente, btnProjetoCriar, btnProjetoExcluir, btnTaskDone, btnProjetoProgresso);
        form.setPadding(new Insets(10));
        form.setPrefHeight(60);

        VBox root = new VBox(10, filtros, tv, form);
        root.setPadding(new Insets(10));
        Tab tab = new Tab("Projetos", root);
        tab.setClosable(false);

        // aplica RBAC quando a aba ganhar foco
        tab.setOnSelectionChanged(ev -> {
            if (tab.isSelected()) {
                refreshProjetos();
                applyRbac();
            }
        });
        return tab;
    }

    private Tab tabTarefas() {
        // Tabela
        TableView<Task> tv = new TableView<>(tarefas);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Task, Long> cId = new TableColumn<>("ID");
        cId.setCellValueFactory(new PropertyValueFactory<>("id"));
        cId.setMaxWidth(80);

        TableColumn<Task, Long> cPid = new TableColumn<>("Projeto");
        cPid.setCellValueFactory(new PropertyValueFactory<>("projectId"));

        TableColumn<Task, String> cTitulo = new TableColumn<>("Título");
        cTitulo.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Task, String> cPrio = new TableColumn<>("Prioridade");
        cPrio.setCellValueFactory(new PropertyValueFactory<>("priority"));

        TableColumn<Task, String> cStatus = new TableColumn<>("Status");
        cStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Task, LocalDate> cDue = new TableColumn<>("Vencimento");
        cDue.setCellValueFactory(new PropertyValueFactory<>("dueDate"));

        TableColumn<Task, Long> cAssg = new TableColumn<>("Responsável");
        cAssg.setCellValueFactory(new PropertyValueFactory<>("assigneeId"));

        tv.getColumns().addAll(cId, cPid, cTitulo, cPrio, cStatus, cDue, cAssg);
        
        var cm = new javafx.scene.control.ContextMenu();
        for (var s : br.com.gpro.model.TaskStatus.values()) {
            var mi = new javafx.scene.control.MenuItem("Mover para " + s.pt());
            mi.setOnAction(ev -> {
                Task sel = tv.getSelectionModel().getSelectedItem();
                if (sel == null) return;

                double h = 0;
                if (s == br.com.gpro.model.TaskStatus.CONCLUIDO) {
                    var d = new TextInputDialog("0");
                    d.setHeaderText("Horas a somar ao concluir:");
                    d.setTitle("Concluir tarefa");
                    var r = d.showAndWait();
                    if (r.isEmpty()) return;
                    try { h = Double.parseDouble(r.get()); } catch (Exception ex) { alertErro("Horas","Valor inválido."); return; }
                }

                try {
                    taskCtl.moverStatus(sel.getId(), s, h);
                    refreshTarefas(sel.getProjectId());
                } catch (Exception ex) {
                    alertErro("Erro ao mover status", ex.getMessage());
                }
            });
            cm.getItems().add(mi);
        }
        tv.setContextMenu(cm);

        // Formulário
        cbProjeto = new ComboBox<>();
        cbProjeto.getItems().setAll(projCtl.listarProjetos());
        setupProjectCombo(cbProjeto);

        TextField fTitulo = new TextField();
        fTitulo.setPromptText("Título da tarefa");

        TextField fDesc = new TextField();
        fDesc.setPromptText("Descrição (opcional)");

        cbAssignee = new ComboBox<>();
        cbAssignee.getItems().setAll(userCtl.listarUsuarios());
        setupUserCombo(cbAssignee);

        tfStatus = new ComboBox<>();
        tfStatus.getItems().setAll(br.com.gpro.model.TaskStatus.values());
        tfStatus.getItems().add(0, null); // todos
        tfStatus.getSelectionModel().select(0);

        ComboBox<br.com.gpro.model.Priority> cbPrio = new ComboBox<>();
        cbPrio.getItems().setAll(br.com.gpro.model.Priority.values());
        cbPrio.getSelectionModel().select(br.com.gpro.model.Priority.MEDIA);

        tfPrio = new ComboBox<>();
        tfPrio.getItems().setAll(br.com.gpro.model.Priority.values());
        tfPrio.getItems().add(0, null);
        tfPrio.getSelectionModel().select(0);

        cbProjeto.valueProperty().addListener((obs, oldP, newP) -> {
            if (newP != null) {
                refreshTarefas(newP.getId());
            } else {
                tarefas.clear();
            }
        });

        DatePicker fDue = new DatePicker();

        ComboBox<br.com.gpro.model.User> tfAssigneeCombo = new ComboBox<>();
        tfAssigneeCombo.getItems().setAll(userCtl.listarUsuarios());
        setupUserCombo(tfAssigneeCombo);
        tfSomenteAtrasadas = new CheckBox("Somente atrasadas");

        btnTaskCriar = new Button("Criar Tarefa");
        btnTaskCriar.setOnAction(e -> {
            try {
                var pSel = cbProjeto.getValue();
                if (pSel == null) { alertErro("Projeto", "Selecione um projeto."); return; }
                Long pid = pSel.getId();

                String titulo = fTitulo.getText().trim();
                String desc   = fDesc.getText().trim();
                Long assignee = (cbAssignee.getValue() == null) ? null : cbAssignee.getValue().getId();
                br.com.gpro.model.Priority prio = cbPrio.getValue();
                LocalDate due = fDue.getValue();

                Long id = taskCtl.criarTarefa(pid, titulo, desc, assignee, prio, null, due);
                alertInfo("Tarefa criada", "ID = " + id);
                refreshTarefas(pid);
                fTitulo.clear(); fDesc.clear(); cbAssignee.getSelectionModel().clearSelection(); fDue.setValue(null);
            } catch (Exception ex) { alertErro("Erro ao criar tarefa", ex.getMessage()); }
        });

        Button btnCarregarProjeto = new Button("Listar do projeto");
        btnCarregarProjeto.setOnAction(e -> {
            var p = cbProjeto.getValue();
            if (p == null) { alertErro("Projeto", "Selecione um projeto."); return; }
            refreshTarefas(p.getId());
        });
        
        // Selecionar novo status
        cbNovoStatus = new ComboBox<>();
        cbNovoStatus.getItems().setAll(br.com.gpro.model.TaskStatus.values());
        cbNovoStatus.getSelectionModel().select(br.com.gpro.model.TaskStatus.ANDAMENTO);

        // Botão "Mover status"
        btnTaskMover = new Button("Mover status");
        btnTaskMover.setOnAction(e -> {
            Task sel = tv.getSelectionModel().getSelectedItem();
            if (sel == null) { alertErro("Atenção", "Selecione uma tarefa."); return; }
            br.com.gpro.model.TaskStatus novo = cbNovoStatus.getValue();
            if (novo == null) { alertErro("Status", "Escolha o novo status."); return; }

            double h = 0;
            if (novo == br.com.gpro.model.TaskStatus.CONCLUIDO) {
                var d = new TextInputDialog("0");
                d.setHeaderText("Horas a somar ao concluir:");
                d.setTitle("Concluir tarefa");
                var res = d.showAndWait();
                if (res.isEmpty()) return;
                try { h = Double.parseDouble(res.get()); }
                catch (Exception ex) { alertErro("Horas", "Valor inválido."); return; }
            }

            try {
                taskCtl.moverStatus(sel.getId(), novo, h);
                refreshTarefas(sel.getProjectId());
            } catch (Exception ex) {
                alertErro("Erro ao mover status", ex.getMessage());
            }
        });

        btnTaskDone.setOnAction(e -> {
            Task sel = tv.getSelectionModel().getSelectedItem();
            if (sel == null) { alertErro("Atenção", "Selecione uma tarefa."); return; }

            TextInputDialog d = new TextInputDialog("0");
            d.setHeaderText("Horas a somar ao concluir:");
            d.setTitle("Concluir tarefa");
            var r = d.showAndWait();
            if (r.isEmpty()) return;

            double h;
            try { h = Double.parseDouble(r.get()); }
            catch (Exception ex) { alertErro("Horas", "Valor inválido."); return; }

            try {
                taskCtl.moverStatus(sel.getId(), br.com.gpro.model.TaskStatus.CONCLUIDO, h); // <<— TAREFA, não projeto
                refreshTarefas(sel.getProjectId());
            } catch (Exception ex) {
                alertErro("Erro ao concluir tarefa", ex.getMessage());
            }
        });


        btnTaskReattr = new Button("Reatribuir Selecionada");
        btnTaskReattr.setOnAction(e -> {
            Task sel = tv.getSelectionModel().getSelectedItem();
            if (sel == null) { alertErro("Atenção", "Selecione uma tarefa."); return; }
            TextInputDialog d = new TextInputDialog();
            d.setHeaderText("Novo assigneeId:");
            d.setTitle("Reatribuir");
            d.showAndWait().ifPresent(val -> {
                try {
                    Long uid = Long.parseLong(val);
                    taskCtl.reatribuir(sel.getId(), uid);
                    refreshTarefas(sel.getProjectId());
                } catch (Exception ex) { alertErro("Erro", ex.getMessage()); }
            });
        });

        Button btnAplicarFiltroT = new Button("Filtrar");
        btnAplicarFiltroT.setOnAction(e -> {
            var p = cbProjeto.getValue();
            if (p == null) { alertErro("Projeto", "Selecione um projeto para filtrar."); return; }
            refreshTarefas(p.getId());
        });


        btnTaskExportCsv = new Button("Exportar CSV (Tarefas visíveis)");
        btnTaskExportCsv.setOnAction(e -> exportTarefasVisiveis(tv));

        HBox linhaProjeto = new HBox(10, new Label("Projeto:"), cbProjeto, btnCarregarProjeto);
        linhaProjeto.setPadding(new Insets(10));

        HBox filtrosT = new HBox(10,
            new Label("Status:"), tfStatus,
            new Label("Prioridade:"), tfPrio,
            new Label("Responsável:"), tfAssigneeCombo,
            tfSomenteAtrasadas, btnAplicarFiltroT, btnTaskExportCsv
        );
        filtrosT.setPadding(new Insets(10));

        HBox form = new HBox(10,
            new Label("Título:"), fTitulo,
            new Label("Descrição:"), fDesc,
            new Label("Responsável:"), cbAssignee,
            new Label("Prioridade:"), cbPrio,
            new Label("Vencimento:"), fDue,
            new Label("Novo status:"), cbNovoStatus, btnTaskMover,
            btnTaskCriar, btnTaskDone, btnTaskReattr
        );
        form.setPadding(new Insets(10));
        form.setPrefHeight(60);


        VBox root = new VBox(10, linhaProjeto, filtrosT, tv, form);
        root.setPadding(new Insets(10));
        Tab tab = new Tab("Tarefas", root);
        tab.setClosable(false);
        tab.setOnSelectionChanged(ev -> { if (tab.isSelected()) applyRbac(); });
        return tab;
    }

    private Tab tabEquipes() {
        // Tabela de equipes
        TableView<br.com.gpro.model.Team> tvTeams = new TableView<>();
        tvTeams.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        var cId = new TableColumn<br.com.gpro.model.Team, Long>("ID");
        cId.setCellValueFactory(new PropertyValueFactory<>("id"));
        cId.setMaxWidth(80);
        var cNome = new TableColumn<br.com.gpro.model.Team, String>("Nome");
        cNome.setCellValueFactory(new PropertyValueFactory<>("name"));
        var cDesc = new TableColumn<br.com.gpro.model.Team, String>("Descrição");
        cDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        tvTeams.getColumns().addAll(cId, cNome, cDesc);
        tvTeams.getItems().setAll(teamCtl.listarEquipes());

        // ----- tabela de Membros da equipe selecionada -----
        tvMembers = new TableView<>();
        tvMembers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        var mId = new TableColumn<br.com.gpro.model.User, Long>("UserId");
        mId.setCellValueFactory(new PropertyValueFactory<>("id"));
        mId.setMaxWidth(80);
        var mNome = new TableColumn<br.com.gpro.model.User, String>("Nome");
        mNome.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        var mUser = new TableColumn<br.com.gpro.model.User, String>("Username");
        mUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        var mRole = new TableColumn<br.com.gpro.model.User, br.com.gpro.model.Role>("Perfil");
        mRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        tvMembers.getColumns().addAll(mId, mNome, mUser, mRole);

         // quando selecionar uma equipe, recarrega membros e combos que dependem dela
        tvTeams.getSelectionModel().selectedItemProperty().addListener((obs, oldT, newT) -> {
            if (newT != null) {
                tvMembers.getItems().setAll(teamCtl.listarMembros(newT.getId()));
                cbTeamSelect.getSelectionModel().select(newT);
                cbTeamForProject.getSelectionModel().select(newT);
            } else {
                tvMembers.getItems().clear();
            }
        });

        // ----- campos de criação de equipe -----
        TextField fNome = new TextField(); fNome.setPromptText("Nome da equipe");
        TextField fDesc = new TextField(); fDesc.setPromptText("Descrição (opcional)");

        btnEquipeCriar = new Button("Criar equipe");
        btnEquipeCriar.setOnAction(e -> {
            try {
                Long id = teamCtl.criarEquipe(fNome.getText().trim(), fDesc.getText().trim());
                alertInfo("Equipe criada", "ID = " + id);
                tvTeams.getItems().setAll(teamCtl.listarEquipes());
                fNome.clear(); fDesc.clear();
            } catch (Exception ex) { alertErro("Erro", ex.getMessage()); }
        });

        btnEquipeExcluir = new Button("Excluir equipe selecionada");
        btnEquipeExcluir.setOnAction(e -> {
            var sel = tvTeams.getSelectionModel().getSelectedItem();
            if (sel == null) { alertErro("Atenção", "Selecione uma equipe."); return; }
            try {
                teamCtl.excluirEquipe(sel.getId());
                tvTeams.getItems().setAll(teamCtl.listarEquipes());
                tvMembers.getItems().clear();
            } catch (Exception ex) { alertErro("Erro", ex.getMessage()); }
        });

        // ----- Combos (sem digitar IDs) -----
        // Equipe para "Adicionar membro"
        cbTeamSelect = new ComboBox<>();
        cbTeamSelect.getItems().setAll(teamCtl.listarEquipes());
        setupTeamCombo(cbTeamSelect);

        // Usuário para adicionar
        cbUserToAdd = new ComboBox<>();
        cbUserToAdd.getItems().setAll(userCtl.listarUsuarios()); // pode filtrar ativos se quiser
        setupUserCombo(cbUserToAdd);

        TextField fRoleInTeam = new TextField(); fRoleInTeam.setPromptText("Papel na equipe (opcional)");

        btnAddMember = new Button("Adicionar membro");
        btnAddMember.setOnAction(e -> {
            var t = cbTeamSelect.getValue();
            var u = cbUserToAdd.getValue();
            if (t == null || u == null) { alertErro("Atenção", "Selecione equipe e usuário."); return; }
            try {
                String role = fRoleInTeam.getText().trim();
                teamCtl.adicionarMembro(t.getId(), u.getId(), role.isBlank() ? null : role);
                tvMembers.getItems().setAll(teamCtl.listarMembros(t.getId()));
                alertInfo("OK", "Membro adicionado.");
            } catch (Exception ex) { alertErro("Erro", ex.getMessage()); }
        });

        // Alocar equipe a projeto
        cbTeamForProject = new ComboBox<>();
        cbTeamForProject.getItems().setAll(teamCtl.listarEquipes());
        setupTeamCombo(cbTeamForProject);

        cbProjectSelect = new ComboBox<>();
        cbProjectSelect.getItems().setAll(projCtl.listarProjetos());
        setupProjectCombo(cbProjectSelect);

        btnAlocarEquipe = new Button("Alocar equipe no projeto");
        btnAlocarEquipe.setOnAction(e -> {
            var t = cbTeamForProject.getValue();
            var p = cbProjectSelect.getValue();
            if (t == null || p == null) { alertErro("Atenção","Selecione equipe e projeto."); return; }
            try {
                projCtl.alocarEquipeEmProjeto(p.getId(), t.getId());
                alertInfo("OK", "Equipe alocada no projeto.");
            } catch (Exception ex) { alertErro("Erro", ex.getMessage()); }
        });

        // ----- layouts -----
        var formCreate = new HBox(10, fNome, fDesc, btnEquipeCriar, btnEquipeExcluir);
        formCreate.setPadding(new Insets(10));

        var formAddMember = new HBox(10, new Label("Equipe:"), cbTeamSelect,
                                        new Label("Usuário:"), cbUserToAdd,
                                        fRoleInTeam, btnAddMember);
        formAddMember.setPadding(new Insets(10));

        var formAlloc = new HBox(10, new Label("Projeto:"), cbProjectSelect,
                                    new Label("Equipe:"), cbTeamForProject,
                                    btnAlocarEquipe);
        formAlloc.setPadding(new Insets(10));

        var grids = new HBox(10, tvTeams, tvMembers);
        HBox.setHgrow(tvTeams, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(tvMembers, javafx.scene.layout.Priority.ALWAYS);

        var root = new VBox(10, grids, formCreate, formAddMember, formAlloc);
        root.setPadding(new Insets(10));

        var tab = new Tab("Equipes", root);
        tab.setClosable(false);
        tab.setOnSelectionChanged(ev -> {
            if (tab.isSelected()) {
                // refresh combos e grids
                var teams = teamCtl.listarEquipes();
                cbTeamSelect.getItems().setAll(teams);
                cbTeamForProject.getItems().setAll(teams);
                cbProjectSelect.getItems().setAll(projCtl.listarProjetos());
                tvTeams.getItems().setAll(teams);
                // se houver seleção, recarrega membros
                var sel = tvTeams.getSelectionModel().getSelectedItem();
                tvMembers.getItems().setAll(sel == null ? java.util.Collections.emptyList()
                                                        : teamCtl.listarMembros(sel.getId()));
                applyRbac();
            }
        });
        return tab;
    }

    private Tab tabDashboard() {
    kpiProjetos = new Label("Projetos: 0");
    kpiTarefas = new Label("Tarefas: 0");
    kpiAtrasadas = new Label("Atrasadas: 0");
    kpiEsforco = new Label("Esforço: 0/0h (0%)");

    kpiProjetos.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
    kpiTarefas.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
    kpiAtrasadas.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
    kpiEsforco.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

    HBox kpis = new HBox(20, kpiProjetos, kpiTarefas, kpiAtrasadas, kpiEsforco);
    kpis.setPadding(new Insets(10));

    chartProjetosStatus = new PieChart();
    chartProjetosStatus.setTitle("Projetos por status");

    CategoryAxis x = new CategoryAxis();
    NumberAxis y = new NumberAxis();
    chartTarefasStatus = new BarChart<>(x, y);
    chartTarefasStatus.setTitle("Tarefas por status (todos os projetos)");
    x.setLabel("Status");
    y.setLabel("Quantidade");

    btnDashRefresh = new Button("Atualizar dashboard");
    btnDashRefresh.setOnAction(e -> refreshDashboard());
    HBox topo = new HBox(10, btnDashRefresh);
    topo.setPadding(new Insets(10));

    VBox root = new VBox(10, topo, kpis, chartProjetosStatus, chartTarefasStatus);
    root.setPadding(new Insets(10));

    Tab tab = new Tab("Dashboard", root);
    tab.setClosable(false);
    tab.setOnSelectionChanged(ev -> { if (tab.isSelected()) refreshDashboard(); });
    return tab;
}

    private Tab tabUsuarios() {
        // Tabela
        TableView<br.com.gpro.model.User> tv = new TableView<>(usuarios);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<br.com.gpro.model.User, Long> cId = new TableColumn<>("ID");
        cId.setCellValueFactory(new PropertyValueFactory<>("id"));
        cId.setMaxWidth(80);

        TableColumn<br.com.gpro.model.User, String> cNome = new TableColumn<>("Nome");
        cNome.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        TableColumn<br.com.gpro.model.User, String> cEmail = new TableColumn<>("E-mail");
        cEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<br.com.gpro.model.User, String> cUser = new TableColumn<>("Username");
        cUser.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<br.com.gpro.model.User, br.com.gpro.model.Role> cRole = new TableColumn<>("Perfil");
        cRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        TableColumn<br.com.gpro.model.User, Boolean> cAtivo = new TableColumn<>("Ativo");
        cAtivo.setCellValueFactory(new PropertyValueFactory<>("active"));

        tv.getColumns().addAll(cId, cNome, cEmail, cUser, cRole, cAtivo);

        // Campos de cadastro
        TextField fNome = new TextField(); fNome.setPromptText("Nome completo");
        TextField fCpf = new TextField(); fCpf.setPromptText("CPF (somente dígitos)");
        TextField fEmail = new TextField(); fEmail.setPromptText("email@dominio.com");
        TextField fCargo = new TextField(); fCargo.setPromptText("Cargo");
        TextField fUsername = new TextField(); fUsername.setPromptText("username");
        PasswordField fSenha = new PasswordField(); fSenha.setPromptText("Senha");

        ComboBox<br.com.gpro.model.Role> cbRole = new ComboBox<>();
        cbRole.getItems().setAll(br.com.gpro.model.Role.values());
        cbRole.getSelectionModel().select(br.com.gpro.model.Role.OPER);
  
        // Botões (ligados ao RBAC)
        btnUserCriar = new Button("Adicionar usuário");
        btnUserCriar.setOnAction(e -> {
            try {
                String cpfIn = fCpf.getText().trim();
                if (!br.com.gpro.util.CpfUtils.isValid(cpfIn)) {
                    alertErro("CPF", "CPF inválido. Verifique os dígitos.");
                    return; // não prossegue com o cadastro
                }
                Long id = userCtl.criarUsuario(
                    fNome.getText().trim(),
                    fCpf.getText().trim(),
                    fEmail.getText().trim(),
                    fCargo.getText().trim(),
                    fUsername.getText().trim(),
                    fSenha.getText(),
                    cbRole.getValue()
                );
                alertInfo("Usuário criado", "ID = " + id);
                refreshUsuarios();
                fNome.clear(); fCpf.clear(); fEmail.clear(); fCargo.clear(); fUsername.clear(); fSenha.clear();
                cbRole.getSelectionModel().select(br.com.gpro.model.Role.OPER);
            } catch (Exception ex) { alertErro("Erro", ex.getMessage()); }
        });

        // Atualizar dados básicos do usuário selecionado
        btnUserAtualizar = new Button("Atualizar selecionado");
        btnUserAtualizar.setOnAction(e -> {
            var sel = tv.getSelectionModel().getSelectedItem();
            if (sel == null) { alertErro("Atenção", "Selecione um usuário na tabela."); return; }
            try {
                // usa os campos preenchidos (se vazio, mantém o atual)
                String novoNome = fNome.getText().isBlank() ? sel.getFullName() : fNome.getText().trim();
                String novoEmail = fEmail.getText().isBlank() ? sel.getEmail() : fEmail.getText().trim();
                String novoCargo = fCargo.getText().isBlank() ? sel.getJobTitle() : fCargo.getText().trim();
                userCtl.atualizarBasico(sel.getId(), novoNome, novoEmail, novoCargo);
                alertInfo("OK", "Atualizado.");
                refreshUsuarios();
            } catch (Exception ex) { alertErro("Erro", ex.getMessage()); }
        });

        // Ativar/Desativar
        btnUserAtivar = new Button("Ativar/Desativar");
        btnUserAtivar.setOnAction(e -> {
            var sel = tv.getSelectionModel().getSelectedItem();
            if (sel == null) { alertErro("Atenção", "Selecione um usuário."); return; }
            try {
                userCtl.setAtivo(sel.getId(), !sel.isActive());
                refreshUsuarios();
            } catch (Exception ex) { alertErro("Erro", ex.getMessage()); }
        });

        // Trocar perfil
        btnUserPerfil = new Button("Trocar perfil");
        btnUserPerfil.setOnAction(e -> {
            var sel = tv.getSelectionModel().getSelectedItem();
            if (sel == null) { alertErro("Atenção", "Selecione um usuário."); return; }
            var choices = javafx.collections.FXCollections.observableArrayList(br.com.gpro.model.Role.values());
            ChoiceDialog<br.com.gpro.model.Role> d = new ChoiceDialog<>(sel.getRole(), choices);
            d.setTitle("Perfil"); d.setHeaderText("Selecione o novo perfil"); d.setContentText("Perfil:");
            var res = d.showAndWait();
            if (res.isPresent()) {
                try { userCtl.alterarPerfil(sel.getId(), res.get()); refreshUsuarios(); }
                catch (Exception ex) { alertErro("Erro", ex.getMessage()); }
            }
        });

        // Reset de senha (admin define nova)
        btnUserResetSenha = new Button("Reset de senha");
        btnUserResetSenha.setOnAction(e -> {
            var sel = tv.getSelectionModel().getSelectedItem();
            if (sel == null) { alertErro("Atenção", "Selecione um usuário."); return; }
            TextInputDialog d = new TextInputDialog();
            d.setTitle("Reset de senha");
            d.setHeaderText("Defina a nova senha para " + sel.getUsername());
            d.setContentText("Nova senha:");
            var res = d.showAndWait();
            if (res.isPresent()) {
                try { userCtl.alterarSenha(sel.getId(), null, res.get(), true); alertInfo("OK","Senha redefinida."); }
                catch (Exception ex) { alertErro("Erro", ex.getMessage()); }
            }
        });

        // Layouts
        HBox form1 = new HBox(10, fNome, fCpf, fEmail, fCargo);
        HBox form2 = new HBox(10, fUsername, fSenha, cbRole, btnUserCriar, btnUserAtualizar, btnUserAtivar, btnUserPerfil, btnUserResetSenha);
        form1.setPadding(new Insets(10));
        form2.setPadding(new Insets(10));
        VBox root = new VBox(10, tv, form1, form2);
        root.setPadding(new Insets(10));

        Tab tab = new Tab("Usuários", root);
        tab.setClosable(false);
        tab.setOnSelectionChanged(ev -> {
            if (tab.isSelected()) {
                refreshUsuarios();
                applyRbac();
            }
        });
        return tab;
    }

    private MenuBar buildMenuBar(Stage stage, TabPane tabs) {
        Menu mSistema = new Menu("Sistema");

        MenuItem miLogin = new MenuItem("Login...");
        miLogin.setOnAction(e -> doLogin(stage));

        MenuItem miLogout = new MenuItem("Logout");
        miLogout.setOnAction(e -> {
            auth.logout();
            applyRbac();
            stage.setTitle("Gestão de Projetos e Equipes - JavaFX (não logado)");
            alertInfo("Logout", "Sessão encerrada.");
        });

        MenuItem miSair = new MenuItem("Sair");
        miSair.setOnAction(e -> javafx.application.Platform.exit());

        mSistema.getItems().addAll(miLogin, miLogout, miSair);

        Menu mConta = new Menu("Conta");
        MenuItem miTrocarSenha = new MenuItem("Alterar senha...");
        miTrocarSenha.setOnAction(e -> changeOwnPassword());
        mConta.getItems().add(miTrocarSenha);

        return new MenuBar(mSistema, mConta);
    }

    private void doLogin(Stage stage) {
        Dialog<User> dlg = new Dialog<>();
        dlg.setTitle("Login");
        dlg.setHeaderText("Informe suas credenciais");
        ButtonType btOk = new ButtonType("Entrar", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(btOk, ButtonType.CANCEL);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new Insets(10));

        TextField tfUser = new TextField();
        tfUser.setPromptText("username");
        PasswordField pfPass = new PasswordField();
        pfPass.setPromptText("senha");

        gp.add(new Label("Usuário:"), 0, 0); gp.add(tfUser, 1, 0);
        gp.add(new Label("Senha:"), 0, 1); gp.add(pfPass, 1, 1);

        dlg.getDialogPane().setContent(gp);
        dlg.setResultConverter(bt -> {
            if (bt == btOk) {
                boolean ok = auth.login(tfUser.getText().trim(), pfPass.getText());
                if (!ok) { alertErro("Login", "Credenciais inválidas ou usuário inativo."); return null; }
                return auth.getCurrentUser();
            }
            return null;
        });

        var res = dlg.showAndWait();
        if (res.isEmpty() || res.get() == null) {
            // exige login: se cancelou, pergunta se quer sair
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Sair da aplicação?", ButtonType.YES, ButtonType.NO);
            a.setHeaderText("Login é obrigatório");
            var ar = a.showAndWait();
            if (ar.isPresent() && ar.get() == ButtonType.YES) {
                javafx.application.Platform.exit();
            } else {
                doLogin(stage);
            }
            return;
        }

        User u = res.get();
        stage.setTitle("Gestão de Projetos e Equipes - JavaFX (logado: " + u.getUsername() + " | " + u.getRole() + ")");
        applyRbac();
        alertInfo("Bem-vindo", u.getFullName() + " (" + u.getRole() + ")");
    }

    private void applyRbac() {
        boolean logged = auth.isLogged();
        Role role = logged ? auth.getCurrentUser().getRole() : null;

        boolean isAdmin = logged && role == Role.ADM;
        boolean isManager = logged && role == Role.GER;
        boolean isMember = logged && role == Role.OPER;
        boolean isDeveloper = logged && role == Role.DEV;
        boolean tarefasOk = logged && (isAdmin || isManager || isDeveloper);


        setDisable(btnTaskMover, !tarefasOk);

        setDisable(btnProjExportCsv, !logged);
        setDisable(btnTaskExportCsv, !logged);

        // Projetos (ADMIN/MANAGER)
        setDisable(btnProjetoCriar, !(isAdmin || isManager || isDeveloper));
        setDisable(btnProjetoExcluir, !(isAdmin || isManager || isDeveloper));
        setDisable(btnProjetoProgresso, !(isAdmin || isManager || isDeveloper));

        // Equipes (ADMIN/MANAGER)
        setDisable(btnEquipeCriar, !(isAdmin || isManager || isDeveloper));
        setDisable(btnEquipeExcluir, !(isAdmin || isManager || isDeveloper));
        setDisable(btnAddMember, !(isAdmin || isManager || isDeveloper));
        setDisable(btnAlocarEquipe, !(isAdmin || isManager || isDeveloper));

        // Tarefas (todos logados podem criar/concluir/reatribuir; em negócio real você pode fechar mais)
        setDisable(btnTaskCriar, !tarefasOk);
        setDisable(btnTaskDone, !tarefasOk);
        setDisable(btnTaskReattr, !tarefasOk);

        // Usuários: apenas ADMIN pode gerenciar
        setDisable(btnUserCriar, !(isAdmin || isDeveloper));
        setDisable(btnUserAtualizar, !(isAdmin || isDeveloper));
        setDisable(btnUserAtivar, !(isAdmin || isDeveloper));
        setDisable(btnUserPerfil, !(isAdmin || isDeveloper));
        setDisable(btnUserResetSenha, !(isAdmin || isDeveloper));

    }

    private static void setDisable(Control c, boolean v) {
        if (c != null) c.setDisable(v);
    }

    private void changeOwnPassword() {
        if (!auth.isLogged()) { alertErro("Conta", "Faça login primeiro."); return; }
        Dialog<String[]> dlg = new Dialog<>();
        dlg.setTitle("Alterar senha");
        dlg.setHeaderText("Informe a senha atual e a nova senha");
        ButtonType btOk = new ButtonType("Alterar", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(btOk, ButtonType.CANCEL);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new Insets(10));

        PasswordField oldPass = new PasswordField();
        PasswordField newPass = new PasswordField();
        gp.add(new Label("Atual:"), 0, 0); gp.add(oldPass, 1, 0);
        gp.add(new Label("Nova:"), 0, 1); gp.add(newPass, 1, 1);

        dlg.getDialogPane().setContent(gp);
        dlg.setResultConverter(bt -> bt == btOk ? new String[]{oldPass.getText(), newPass.getText()} : null);
        var res = dlg.showAndWait();
        if (res.isEmpty() || res.get() == null) return;

        try {
            userCtl.alterarSenha(auth.getCurrentUser().getId(), res.get()[0], res.get()[1], false);
            alertInfo("Senha", "Senha alterada com sucesso.");
        } catch (Exception ex) {
            alertErro("Senha", ex.getMessage());
        }
    }

    private void refreshUsuarios() {
        usuarios.setAll(userCtl.listarUsuarios());
    }

    private void refreshProjetos() {
        List<br.com.gpro.model.Project> all = projCtl.listarProjetos();
        // Aplicar filtros (se existirem)
        br.com.gpro.model.ProjectStatus st = pfStatus != null ? pfStatus.getValue() : null;
        String q = pfBuscaNome != null ? pfBuscaNome.getText().trim().toLowerCase() : "";

        var filtered = all.stream()
            .filter(p -> st == null || p.getStatus() == st)
            .filter(p -> q.isEmpty() || (p.getName() != null && p.getName().toLowerCase().contains(q)))
            .toList();

        projetos.setAll(filtered);
    }

    private void exportProjetosVisiveis(TableView<br.com.gpro.model.Project> tv) {
        var rows = new java.util.ArrayList<String[]>();
        rows.add(new String[]{"ID","Nome","Status","Início","Término","GerenteId"});
        for (var p : tv.getItems()) {
            rows.add(new String[]{
                String.valueOf(p.getId()),
                p.getName(),
                String.valueOf(p.getStatus()),
                String.valueOf(p.getStartDate()),
                String.valueOf(p.getDueDate()),
                String.valueOf(p.getManagerId())
            });
        }
        var path = br.com.gpro.app.CsvExporter.write("projetos_visiveis.csv", rows);
        alertInfo("CSV gerado", "Arquivo: " + path.toAbsolutePath());
    }


   private void refreshTarefas(Long projectId) {
        var list = taskCtl.listarPorProjeto(projectId);
        // Aplicar filtros da GUI
        var st = tfStatus != null ? tfStatus.getValue() : null;
        var pr = tfPrio != null ? tfPrio.getValue() : null;
        Long assgId = (tfAssigneeCombo != null && tfAssigneeCombo.getValue() != null)
              ? tfAssigneeCombo.getValue().getId() : null;
        boolean onlyOverdue = tfSomenteAtrasadas != null && tfSomenteAtrasadas.isSelected();

        java.time.LocalDate hoje = java.time.LocalDate.now();

        var filtered = list.stream()
            .filter(t -> st == null || t.getStatus() == st)
            .filter(t -> pr == null || t.getPriority() == pr)
            .filter(t -> assgId == null || (t.getAssigneeId() != null && t.getAssigneeId().equals(assgId)))
            .filter(t -> !onlyOverdue || (t.getDueDate() != null && t.getStatus() != br.com.gpro.model.TaskStatus.CONCLUIDO && t.getDueDate().isBefore(hoje)))
            .toList();

        tarefas.setAll(filtered);
    }

    private void exportTarefasVisiveis(TableView<br.com.gpro.model.Task> tv) {
        var rows = new java.util.ArrayList<String[]>();
        rows.add(new String[]{"ID","Projeto","Título","Prioridade","Status","Estimativa(h)","Gasto(h)","Vencimento","AssigneeId"});
        for (var t : tv.getItems()) {
            rows.add(new String[]{
                String.valueOf(t.getId()),
                String.valueOf(t.getProjectId()),
                t.getTitle(),
                String.valueOf(t.getPriority()),
                String.valueOf(t.getStatus()),
                t.getEstimateH() == null ? "" : String.valueOf(t.getEstimateH()),
                t.getSpentH() == null ? "0" : String.valueOf(t.getSpentH()),
                String.valueOf(t.getDueDate()),
                String.valueOf(t.getAssigneeId())
            });
        }
        var path = br.com.gpro.app.CsvExporter.write("tarefas_visiveis.csv", rows);
        alertInfo("CSV gerado", "Arquivo: " + path.toAbsolutePath());
    }


    private static void alertInfo(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
            a.setTitle(title); a.setHeaderText(null); a.showAndWait();
        });
    }

    private static void alertErro(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            a.setTitle(title); a.setHeaderText(null); a.showAndWait();
        });
    }

    private void refreshDashboard() {
        // Projetos
        var projs = projCtl.listarProjetos();
        int totalProjetos = projs.size();

        var projStatusMap = new java.util.EnumMap<br.com.gpro.model.ProjectStatus, Integer>(br.com.gpro.model.ProjectStatus.class);
        for (var s : br.com.gpro.model.ProjectStatus.values()) projStatusMap.put(s, 0);
        projs.forEach(p -> projStatusMap.computeIfPresent(p.getStatus(), (k,v) -> v + 1));

        // Tarefas (somando todos os projetos)
        int totalTarefas = 0, atrasadas = 0;
        double estTotal = 0.0, spentTotal = 0.0;

        var taskStatusMap = new java.util.EnumMap<br.com.gpro.model.TaskStatus, Integer>(br.com.gpro.model.TaskStatus.class);
        for (var s : br.com.gpro.model.TaskStatus.values()) taskStatusMap.put(s, 0);

        var hoje = java.time.LocalDate.now();

        for (var p : projs) {
            var tasks = taskCtl.listarPorProjeto(p.getId());
            totalTarefas += tasks.size();
            for (var t : tasks) {
                taskStatusMap.computeIfPresent(t.getStatus(), (k,v) -> v + 1);

                boolean concluida = (t.getStatus() == br.com.gpro.model.TaskStatus.CONCLUIDO);
                if (t.getDueDate() != null && !concluida && t.getDueDate().isBefore(hoje)) {
                    atrasadas++;
                }
                if (t.getEstimateH() != null) estTotal += t.getEstimateH();
                if (t.getSpentH() != null)    spentTotal += t.getSpentH();
            }
        }

        // KPIs
        kpiProjetos.setText("Projetos: " + totalProjetos);
        kpiTarefas.setText("Tarefas: " + totalTarefas);
        kpiAtrasadas.setText("Atrasadas: " + atrasadas);
        double pct = estTotal == 0 ? 0 : (spentTotal / estTotal) * 100.0;
        kpiEsforco.setText(String.format(java.util.Locale.US, "Esforço: %.1f/%.1fh (%.1f%%)", spentTotal, estTotal, pct));

       // Pizza: Projetos por status
        chartProjetosStatus.getData().clear();
        var ordemProj = java.util.Arrays.stream(br.com.gpro.model.ProjectStatus.values())
            .sorted(java.util.Comparator.comparingInt(s -> weightProjectStatus(s.name())))
            .toList();
        for (var s : ordemProj) {
            Integer v = projStatusMap.getOrDefault(s, 0);
            if (v != null && v > 0) chartProjetosStatus.getData().add(new PieChart.Data(s.pt(), v));
        }

        // Barras: Tarefas por status
        chartTarefasStatus.getData().clear();
        var serie = new XYChart.Series<String, Number>();
        serie.setName("Status");
        var ordemTask = java.util.Arrays.stream(br.com.gpro.model.TaskStatus.values())
            .sorted(java.util.Comparator.comparingInt(s -> weightTaskStatus(s.name())))
            .toList();
        for (var s : ordemTask) {
            Integer v = taskStatusMap.getOrDefault(s, 0);
            if (v != null && v > 0) serie.getData().add(new XYChart.Data<>(s.pt(), v));
        }
        chartTarefasStatus.getData().add(serie);
    }

    private static int weightProjectStatus(String name) {
        return switch (name) {
            case "PLANEJADO" -> 0;
            case "ANDAMENTO" -> 1;
            case "CONCLUIDO" -> 2;
            case "CANCELADO" -> 3;
            default -> 99;
        };
    }

    private static int weightTaskStatus(String name) {
        return switch (name) {
            case "FAZER"     -> 0;
            case "ANDAMENTO" -> 1;
            case "BLOQUEADO" -> 2;
            case "REVISANDO" -> 3;
            case "CONCLUIDO" -> 4;
            default -> 99;
        };
    }

    private static void setupTeamCombo(ComboBox<br.com.gpro.model.Team> cb) {
        cb.setConverter(new StringConverter<>() {
            @Override public String toString(br.com.gpro.model.Team t) {
                return t == null ? "" : "#" + t.getId() + " • " + t.getName();
            }
            @Override public br.com.gpro.model.Team fromString(String s) { return null; }
        });
        cb.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(br.com.gpro.model.Team t, boolean empty) {
                super.updateItem(t, empty); setText(empty || t==null ? "" : "#" + t.getId() + " • " + t.getName());
            }
        });
    }

    private static void setupUserCombo(ComboBox<br.com.gpro.model.User> cb) {
        cb.setConverter(new StringConverter<>() {
            @Override public String toString(br.com.gpro.model.User u) {
                return u == null ? "" : u.getFullName() + " (" + u.getUsername() + ") — " + u.getRole();
            }
            @Override public br.com.gpro.model.User fromString(String s) { return null; }
        });
        cb.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(br.com.gpro.model.User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u==null ? "" : u.getFullName() + " (" + u.getUsername() + ") — " + u.getRole());
            }
        });
    }

    private static void setupProjectCombo(ComboBox<br.com.gpro.model.Project> cb) {
        cb.setConverter(new StringConverter<>() {
            @Override public String toString(br.com.gpro.model.Project p) {
                return p == null ? "" : "#" + p.getId() + " • " + p.getName() + " [" + p.getStatus().pt() + "]";
            }
            @Override public br.com.gpro.model.Project fromString(String s) { return null; }
        });
        cb.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(br.com.gpro.model.Project p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p==null ? "" : "#" + p.getId() + " • " + p.getName() + " [" + p.getStatus().pt() + "]");
            }
        });
    }


    private static void clearFields(TextField... fs) {
        for (TextField f : fs) f.clear();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

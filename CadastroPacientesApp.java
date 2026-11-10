import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Cadastro local de pacientes. Requer Java 8 ou superior.
 * Os dados são gravados em ~/.cadastro-pacientes/pacientes.tsv.
 */
public final class CadastroPacientesApp extends JFrame {
    private static final Color BACKGROUND = new Color(0xF2F6F4);
    private static final Color SURFACE = Color.WHITE;
    private static final Color SIDEBAR = new Color(0x153D3A);
    private static final Color SIDEBAR_MUTED = new Color(0x9BC8BE);
    private static final Color SIDEBAR_TEXT = new Color(0xD6E7E2);
    private static final Color SIDEBAR_SELECTED = new Color(0x24544F);
    private static final Color ACCENT = new Color(0x08766B);
    private static final Color ACCENT_SOFT = new Color(0xE3F1EE);
    private static final Color TABLE_HEADER = new Color(0xF7F9F8);
    private static final Color TEXT = new Color(0x1D302D);
    private static final Color MUTED = new Color(0x687A76);
    private static final Color BORDER = new Color(0xDCE6E2);
    private static final Color ERROR = new Color(0xA43E3E);
    private static final Color WARNING = new Color(0x8A5A17);
    private static final Font FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 27);
    private static final int PAGE_SIZE = 25;
    private static final int MAX_PATIENTS = 50000;

    private final Storage storage;
    private final List<Paciente> pacientes = new ArrayList<Paciente>();
    private final CardLayout screenLayout = new CardLayout();
    private final JPanel screenCards = new JPanel(screenLayout);
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel formErrorLabel = new JLabel(" ");
    private final JLabel countLabel = new JLabel("0 pacientes");
    private final JLabel pageLabel = new JLabel("Página 1 de 1");
    private final JLabel emptyTitle = new JLabel("Nenhum paciente cadastrado");
    private final JLabel emptyDescription = new JLabel("Preencha o cadastro para incluir o primeiro paciente.");
    private final JTextField nameField = new JTextField(32);
    private final JTextField ageField = new JTextField(8);
    private final JTextField phoneField = new JTextField(20);
    private final JTextField emailField = new JTextField(32);
    private final JTextField searchField = new JTextField(24);
    private final JButton saveButton = new JButton("Salvar paciente");
    private final JButton clearSearchButton = new JButton("Limpar");
    private final JButton previousButton = new JButton("‹  Anterior");
    private final JButton nextButton = new JButton("Próxima  ›");
    private final JButton emptyActionButton = new JButton("Adicionar paciente");
    private final JButton addNavigationButton = new JButton("Adicionar paciente");
    private final JButton listNavigationButton = new JButton("Ver pacientes");
    private final PatientTableModel tableModel = new PatientTableModel();
    private final JTable table = new JTable(tableModel);
    private final JPanel listBody = new JPanel(new BorderLayout());
    private final JScrollPane tableScrollPane = new JScrollPane(table);
    private final JPanel emptyPanel = createEmptyPanel();

    private int pageIndex;
    private int invalidRowsInSource;
    private boolean storageWritable = true;
    private boolean saving;
    private String statusMessage = " ";
    private Color statusColor = MUTED;

    private CadastroPacientesApp(Path dataFile) {
        super("Cadastro de pacientes");
        storage = new Storage(dataFile);
        configureWindow();
        buildInterface();
        loadPatients();
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            SwingUtilities.invokeLater(() -> showStartupMessage(
                    "Ocorreu um erro inesperado. O cadastro continua aberto; confira os dados e tente novamente."));
        });
        SwingUtilities.invokeLater(() -> {
            try {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // Em caso de falha, use o visual padrão do Swing e continue abrindo o cadastro.
                }
                Path dataFile = defaultDataFile();
                CadastroPacientesApp app = new CadastroPacientesApp(dataFile);
                app.setVisible(true);
            } catch (Exception exception) {
                showStartupMessage("Não foi possível abrir a janela. Verifique a instalação do Java e tente novamente.");
            }
        });
    }

    private static Path defaultDataFile() {
        String home = System.getProperty("user.home", ".");
        if (home == null || home.trim().isEmpty()) {
            home = ".";
        }
        return Paths.get(home, ".cadastro-pacientes", "pacientes.tsv");
    }

    private static void showStartupMessage(String message) {
        try {
            javax.swing.JOptionPane.showMessageDialog(null, message, "Cadastro de pacientes",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        } catch (Exception ignored) {
            System.err.println(message);
        }
    }

    private void configureWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));
        setSize(1120, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);
    }

    private void buildInterface() {
        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);

        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(BACKGROUND);
        workspace.setBorder(BorderFactory.createEmptyBorder(28, 34, 18, 34));
        workspace.add(screenCards, BorderLayout.CENTER);
        workspace.add(buildStatusBar(), BorderLayout.SOUTH);

        screenCards.add(buildFormScreen(), "form");
        screenCards.add(buildListScreen(), "list");
        add(workspace, BorderLayout.CENTER);
        showScreen("form");
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(242, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(30, 20, 22, 20));

        JLabel brandOverline = new JLabel("REGISTRO LOCAL");
        brandOverline.setFont(new Font("Segoe UI", Font.BOLD, 11));
        brandOverline.setForeground(SIDEBAR_MUTED);
        brandOverline.setAlignmentX(LEFT_ALIGNMENT);
        JLabel brandTitle = new JLabel("<html>Caderno<br>de pacientes</html>");
        brandTitle.setFont(new Font("Segoe UI", Font.BOLD, 25));
        brandTitle.setForeground(Color.WHITE);
        brandTitle.setAlignmentX(LEFT_ALIGNMENT);

        sidebar.add(brandOverline);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(brandTitle);
        sidebar.add(Box.createVerticalStrut(42));

        JLabel navigationLabel = new JLabel("MENU");
        navigationLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        navigationLabel.setForeground(SIDEBAR_MUTED);
        navigationLabel.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(navigationLabel);
        sidebar.add(Box.createVerticalStrut(12));

        styleNavigationButton(addNavigationButton);
        styleNavigationButton(listNavigationButton);
        addNavigationButton.addActionListener(event -> showScreen("form"));
        listNavigationButton.addActionListener(event -> {
            pageIndex = 0;
            refreshTable();
            showScreen("list");
        });
        sidebar.add(addNavigationButton);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(listNavigationButton);
        sidebar.add(Box.createVerticalGlue());

        JPanel note = new JPanel(new BorderLayout(0, 7));
        note.setOpaque(false);
        note.setAlignmentX(LEFT_ALIGNMENT);
        JLabel noteTitle = new JLabel("ARMAZENAMENTO");
        noteTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        noteTitle.setForeground(SIDEBAR_MUTED);
        JLabel noteText = new JLabel("<html>Os registros ficam em um<br>arquivo local neste computador.</html>");
        noteText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        noteText.setForeground(SIDEBAR_TEXT);
        noteText.setVerticalAlignment(SwingConstants.TOP);
        noteText.setPreferredSize(new Dimension(195, 54));
        note.add(noteTitle, BorderLayout.NORTH);
        note.add(noteText, BorderLayout.CENTER);
        sidebar.add(note);
        return sidebar;
    }

    private void styleNavigationButton(JButton button) {
        button.setFont(FONT_BOLD);
        button.setForeground(SIDEBAR_TEXT);
        button.setBackground(SIDEBAR);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.setPreferredSize(new Dimension(200, 46));
        button.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
        button.setFocusPainted(true);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setOpaque(true);
    }

    private JPanel buildFormScreen() {
        JPanel screen = new JPanel(new BorderLayout(0, 22));
        screen.setBackground(BACKGROUND);
        screen.add(buildScreenHeading("Novo cadastro", "Inclua os dados básicos para registrar um paciente."), BorderLayout.NORTH);

        JPanel card = createCard();
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(26, 28, 26, 28)));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 20, 0);
        JLabel sectionTitle = new JLabel("Dados do paciente");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        sectionTitle.setForeground(TEXT);
        card.add(sectionTitle, constraints);

        addFormField(card, constraints, 1, "Nome completo *", nameField, "Ex.: Ana Maria da Silva");
        addFormField(card, constraints, 2, "Idade *", ageField, "Em anos, de 0 a 130");
        addFormField(card, constraints, 3, "Telefone", phoneField, "DDD + número, somente números ou com formatação");
        addFormField(card, constraints, 4, "E-mail", emailField, "Opcional");

        constraints.gridx = 0;
        constraints.gridy = 13;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(2, 0, 14, 0);
        formErrorLabel.setForeground(ERROR);
        formErrorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formErrorLabel.getAccessibleContext().setAccessibleName("Erro do formulário");
        card.add(formErrorLabel, constraints);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        stylePrimaryButton(saveButton);
        JButton clearButton = new JButton("Limpar campos");
        styleSecondaryButton(clearButton);
        clearButton.addActionListener(event -> clearForm());
        saveButton.addActionListener(event -> savePatient());
        actions.add(saveButton);
        actions.add(clearButton);

        constraints.gridy = 14;
        constraints.insets = new Insets(2, 0, 0, 0);
        card.add(actions, constraints);
        getRootPane().setDefaultButton(saveButton);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints centerConstraints = new GridBagConstraints();
        centerConstraints.gridx = 0;
        centerConstraints.gridy = 0;
        centerConstraints.weightx = 1;
        centerConstraints.fill = GridBagConstraints.HORIZONTAL;
        centerConstraints.anchor = GridBagConstraints.NORTH;
        centerConstraints.insets = new Insets(0, 0, 0, 0);
        center.add(card, centerConstraints);
        screen.add(center, BorderLayout.CENTER);
        return screen;
    }

    private void addFormField(JPanel card, GridBagConstraints base, int row, String labelText,
                              JTextField field, String helperText) {
        GridBagConstraints constraints = (GridBagConstraints) base.clone();
        constraints.gridy = 1 + (row - 1) * 3;
        constraints.gridwidth = 1;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 5, 0);
        JLabel label = new JLabel(labelText);
        label.setFont(FONT_BOLD);
        label.setForeground(TEXT);
        label.setLabelFor(field);
        card.add(label, constraints);

        constraints.gridy = 2 + (row - 1) * 3;
        constraints.insets = new Insets(0, 0, 3, 0);
        styleTextField(field);
        field.setToolTipText(helperText);
        field.getAccessibleContext().setAccessibleName(labelText.replace(" *", ""));
        card.add(field, constraints);

        constraints.gridy = 3 + (row - 1) * 3;
        constraints.insets = new Insets(0, 0, 13, 0);
        JLabel helper = new JLabel(helperText);
        helper.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        helper.setForeground(MUTED);
        card.add(helper, constraints);
    }

    private JPanel buildListScreen() {
        JPanel screen = new JPanel(new BorderLayout(0, 22));
        screen.setBackground(BACKGROUND);

        JPanel heading = buildScreenHeading("Pacientes cadastrados", "Pesquise os registros salvos neste computador.");
        JPanel headingRow = new JPanel(new BorderLayout(12, 0));
        headingRow.setOpaque(false);
        headingRow.add(heading, BorderLayout.CENTER);
        countLabel.setFont(FONT_BOLD);
        countLabel.setForeground(ACCENT);
        countLabel.setOpaque(true);
        countLabel.setBackground(ACCENT_SOFT);
        countLabel.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        headingRow.add(countLabel, BorderLayout.EAST);
        screen.add(headingRow, BorderLayout.NORTH);

        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(20, 20, 16, 20)));
        card.add(buildSearchBar(), BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        table.setRowHeight(42);
        table.setFont(FONT);
        table.setForeground(TEXT);
        table.setBackground(SURFACE);
        table.setGridColor(BORDER);
        table.setShowVerticalLines(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getAccessibleContext().setAccessibleName("Lista de pacientes");
        table.getTableHeader().setFont(FONT_BOLD);
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setBackground(TABLE_HEADER);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.setDefaultRenderer(Object.class, new PlainTextRenderer());
        tableScrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        tableScrollPane.getViewport().setBackground(SURFACE);
        listBody.setOpaque(false);
        listBody.add(tableScrollPane, BorderLayout.CENTER);
        card.add(listBody, BorderLayout.CENTER);
        card.add(buildPagination(), BorderLayout.SOUTH);
        screen.add(card, BorderLayout.CENTER);
        return screen;
    }

    private JPanel buildSearchBar() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        JPanel inputGroup = new JPanel(new BorderLayout(8, 0));
        inputGroup.setOpaque(false);
        JLabel searchLabel = new JLabel("Buscar");
        searchLabel.setFont(FONT_BOLD);
        searchLabel.setForeground(TEXT);
        searchLabel.setLabelFor(searchField);
        styleTextField(searchField);
        searchField.setToolTipText("Pesquise por nome, telefone ou e-mail");
        searchField.getAccessibleContext().setAccessibleName("Buscar por nome, telefone ou e-mail");
        inputGroup.add(searchLabel, BorderLayout.WEST);
        inputGroup.add(searchField, BorderLayout.CENTER);
        panel.add(inputGroup, BorderLayout.CENTER);
        styleSecondaryButton(clearSearchButton);
        clearSearchButton.setEnabled(false);
        clearSearchButton.addActionListener(event -> searchField.setText(""));
        panel.add(clearSearchButton, BorderLayout.EAST);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { searchChanged(); }
            @Override public void removeUpdate(DocumentEvent event) { searchChanged(); }
            @Override public void changedUpdate(DocumentEvent event) { searchChanged(); }
        });
        return panel;
    }

    private JPanel buildPagination() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        pageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pageLabel.setForeground(MUTED);
        panel.add(pageLabel, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        styleSecondaryButton(previousButton);
        styleSecondaryButton(nextButton);
        previousButton.addActionListener(event -> {
            if (pageIndex > 0) {
                pageIndex--;
                refreshTable();
            }
        });
        nextButton.addActionListener(event -> {
            pageIndex++;
            refreshTable();
        });
        buttons.add(previousButton);
        buttons.add(nextButton);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    private JPanel createEmptyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(SURFACE);
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        emptyTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        emptyTitle.setForeground(TEXT);
        emptyTitle.setAlignmentX(CENTER_ALIGNMENT);
        emptyDescription.setFont(FONT);
        emptyDescription.setForeground(MUTED);
        emptyDescription.setAlignmentX(CENTER_ALIGNMENT);
        emptyActionButton.setAlignmentX(CENTER_ALIGNMENT);
        stylePrimaryButton(emptyActionButton);
        emptyActionButton.addActionListener(event -> showScreen("form"));
        content.add(emptyTitle);
        content.add(Box.createVerticalStrut(8));
        content.add(emptyDescription);
        content.add(Box.createVerticalStrut(18));
        content.add(emptyActionButton);
        panel.add(content);
        return panel;
    }

    private JPanel buildScreenHeading(String title, String description) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(TEXT);
        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setFont(FONT);
        descriptionLabel.setForeground(MUTED);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(7));
        panel.add(descriptionLabel);
        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 2, 0, 2));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(MUTED);
        statusLabel.getAccessibleContext().setAccessibleName("Status do cadastro");
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCard() {
        JPanel panel = new JPanel();
        panel.setBackground(SURFACE);
        return panel;
    }

    private void styleTextField(JTextField field) {
        field.setFont(FONT);
        field.setForeground(TEXT);
        field.setBackground(SURFACE);
        field.setCaretColor(ACCENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(9, 11, 9, 11)));
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 42));
    }

    private void stylePrimaryButton(JButton button) {
        button.setFont(FONT_BOLD);
        button.setForeground(Color.WHITE);
        button.setBackground(ACCENT);
        button.setFocusPainted(true);
        button.setBorder(BorderFactory.createEmptyBorder(11, 17, 11, 17));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
    }

    private void styleSecondaryButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(TEXT);
        button.setBackground(SURFACE);
        button.setFocusPainted(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
    }

    private void showScreen(String name) {
        screenLayout.show(screenCards, name);
        boolean form = "form".equals(name);
        getRootPane().setDefaultButton(form ? saveButton : null);
        styleNavigationState(addNavigationButton, form);
        styleNavigationState(listNavigationButton, !form);
        if (form) {
            nameField.requestFocusInWindow();
        } else {
            searchField.requestFocusInWindow();
        }
        refreshStatus();
    }

    private void styleNavigationState(JButton button, boolean selected) {
        button.setBackground(selected ? SIDEBAR_SELECTED : SIDEBAR);
        button.setForeground(selected ? Color.WHITE : SIDEBAR_TEXT);
    }

    private void loadPatients() {
        try {
            LoadResult result = storage.load();
            pacientes.clear();
            pacientes.addAll(result.patients);
            invalidRowsInSource = result.invalidRows;
            if (invalidRowsInSource > 0) {
                setStatus("Algumas linhas inválidas foram ignoradas. Uma cópia será preservada antes da próxima gravação.", WARNING);
            } else {
                setStatus(pacientes.size() + (pacientes.size() == 1 ? " paciente carregado." : " pacientes carregados."), MUTED);
            }
        } catch (Exception exception) {
            storageWritable = false;
            saveButton.setEnabled(false);
            setStatus("Não foi possível ler o arquivo local. Novos cadastros estão desativados para preservar os dados.", ERROR);
        }
        refreshTable();
    }

    private void savePatient() {
        if (saving) {
            return;
        }
        if (!storageWritable) {
            showFormError("O arquivo local não pôde ser lido; não é seguro gravar novos dados.");
            return;
        }
        try {
            Paciente paciente = validateForm();
            if (pacientes.size() >= MAX_PATIENTS) {
                throw new ValidationException("O limite de " + MAX_PATIENTS + " pacientes foi atingido.");
            }
            saving = true;
            saveButton.setEnabled(false);
            List<Paciente> updatedPatients = new ArrayList<Paciente>(pacientes);
            updatedPatients.add(paciente);
            storage.save(updatedPatients, invalidRowsInSource);
            pacientes.clear();
            pacientes.addAll(updatedPatients);
            invalidRowsInSource = 0;
            pageIndex = 0;
            clearForm();
            refreshTable();
            setStatus("Paciente salvo no arquivo local.", ACCENT);
            showScreen("list");
        } catch (ValidationException exception) {
            showFormError(exception.getMessage());
            if (exception.field != null) {
                exception.field.getAccessibleContext().setAccessibleDescription(exception.getMessage());
                exception.field.requestFocusInWindow();
            }
        } catch (IOException exception) {
            showFormError("Não foi possível salvar o arquivo. Confira o espaço disponível e as permissões da pasta.");
            setStatus("O cadastro não foi gravado; os dados preenchidos foram mantidos.", ERROR);
        } catch (Exception exception) {
            showFormError("Não foi possível concluir o cadastro. Confira os campos e tente novamente.");
            setStatus("O cadastro não foi gravado.", ERROR);
        } finally {
            saving = false;
            saveButton.setEnabled(storageWritable);
        }
    }

    private Paciente validateForm() throws ValidationException {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String ageText = ageField.getText() == null ? "" : ageField.getText().trim();
        String phoneText = phoneField.getText() == null ? "" : phoneField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();

        if (name.isEmpty()) {
            throw new ValidationException("Informe o nome completo do paciente.", nameField);
        }
        if (name.codePointCount(0, name.length()) < 2 || name.codePointCount(0, name.length()) > 100 || !containsLetter(name)) {
            throw new ValidationException("O nome deve ter de 2 a 100 caracteres e conter pelo menos uma letra.", nameField);
        }
        if (containsControlCharacter(name)) {
            throw new ValidationException("O nome contém caracteres inválidos.", nameField);
        }
        if (ageText.isEmpty()) {
            throw new ValidationException("Informe a idade do paciente.", ageField);
        }
        if (ageText.length() > 3) {
            throw new ValidationException("A idade deve estar entre 0 e 130 anos.", ageField);
        }
        int age;
        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException exception) {
            throw new ValidationException("A idade deve ser um número inteiro.", ageField);
        }
        if (age < 0 || age > 130) {
            throw new ValidationException("A idade deve estar entre 0 e 130 anos.", ageField);
        }

        String phone = "";
        if (!phoneText.isEmpty()) {
            if (phoneText.length() > 40) {
                throw new ValidationException("O telefone informado é longo demais.", phoneField);
            }
            if (!phoneText.matches("[+0-9() .-]+")) {
                throw new ValidationException("O telefone pode conter apenas números e os símbolos +, espaço, parênteses, ponto ou hífen.", phoneField);
            }
            phone = phoneText.replaceAll("\\D", "");
            if (phone.length() < 10 || phone.length() > 13) {
                throw new ValidationException("O telefone deve conter de 10 a 13 dígitos, incluindo o DDD.", phoneField);
            }
        }

        if (email.length() > 254 || (!email.isEmpty() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$"))) {
            throw new ValidationException("Informe um e-mail válido com até 254 caracteres.", emailField);
        }
        return new Paciente(name, age, phone, email);
    }

    private void clearForm() {
        nameField.setText("");
        ageField.setText("");
        phoneField.setText("");
        emailField.setText("");
        formErrorLabel.setText(" ");
        nameField.getAccessibleContext().setAccessibleDescription(null);
        ageField.getAccessibleContext().setAccessibleDescription(null);
        phoneField.getAccessibleContext().setAccessibleDescription(null);
        emailField.getAccessibleContext().setAccessibleDescription(null);
        nameField.requestFocusInWindow();
    }

    private void showFormError(String message) {
        formErrorLabel.setText("<html><body style='width: 500px'>" + escapeHtml(message) + "</body></html>");
        formErrorLabel.setForeground(ERROR);
        formErrorLabel.setToolTipText(message);
    }

    private void searchChanged() {
        try {
            clearSearchButton.setEnabled(!searchField.getText().isEmpty());
            pageIndex = 0;
            refreshTable();
        } catch (Exception exception) {
            setStatus("Não foi possível atualizar a pesquisa.", ERROR);
        }
    }

    private void refreshTable() {
        try {
            List<Paciente> filtered = filteredPatients();
            int pageCount = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (pageIndex >= pageCount) {
                pageIndex = pageCount - 1;
            }
            int from = Math.min(pageIndex * PAGE_SIZE, filtered.size());
            int to = Math.min(from + PAGE_SIZE, filtered.size());
            tableModel.setRows(filtered.subList(from, to));
            countLabel.setText(filtered.size() + (filtered.size() == 1 ? " paciente" : " pacientes"));
            pageLabel.setText("Página " + (pageIndex + 1) + " de " + pageCount);
            previousButton.setEnabled(pageIndex > 0);
            nextButton.setEnabled(pageIndex + 1 < pageCount);

            if (filtered.isEmpty()) {
                boolean searching = !searchField.getText().trim().isEmpty();
                emptyTitle.setText(searching ? "Nenhum resultado encontrado" : "Nenhum paciente cadastrado");
                emptyDescription.setText(searching
                        ? "Tente outro nome, telefone ou e-mail."
                        : "Preencha o cadastro para incluir o primeiro paciente.");
                emptyActionButton.setText(searching ? "Limpar pesquisa" : "Adicionar paciente");
                for (java.awt.event.ActionListener listener : emptyActionButton.getActionListeners()) {
                    emptyActionButton.removeActionListener(listener);
                }
                if (searching) {
                    emptyActionButton.addActionListener(event -> searchField.setText(""));
                } else {
                    emptyActionButton.addActionListener(event -> showScreen("form"));
                }
                listBody.removeAll();
                listBody.add(emptyPanel, BorderLayout.CENTER);
            } else {
                listBody.removeAll();
                listBody.add(tableScrollPane, BorderLayout.CENTER);
            }
            listBody.revalidate();
            listBody.repaint();
        } catch (Exception exception) {
            setStatus("Não foi possível atualizar a lista de pacientes.", ERROR);
        }
    }

    private List<Paciente> filteredPatients() {
        String query = normalize(searchField.getText().trim());
        if (query.isEmpty()) {
            return new ArrayList<Paciente>(pacientes);
        }
        List<Paciente> result = new ArrayList<Paciente>();
        String digitQuery = query.replaceAll("\\D", "");
        for (Paciente paciente : pacientes) {
            String searchable = normalize(paciente.name + " " + paciente.phone + " " + paciente.email);
            if (searchable.contains(query) || (!digitQuery.isEmpty() && paciente.phone.contains(digitQuery))) {
                result.add(paciente);
            }
        }
        return result;
    }

    private void setStatus(String message, Color color) {
        statusMessage = message == null || message.trim().isEmpty() ? " " : message;
        statusColor = color;
        refreshStatus();
    }

    private void refreshStatus() {
        statusLabel.setText(statusMessage);
        statusLabel.setForeground(statusColor);
    }

    private static String normalize(String text) {
        String decomposed = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    private static boolean containsLetter(String text) {
        for (int index = 0; index < text.length();) {
            int point = text.codePointAt(index);
            if (Character.isLetter(point)) {
                return true;
            }
            index += Character.charCount(point);
        }
        return false;
    }

    private static boolean containsControlCharacter(String text) {
        for (int index = 0; index < text.length(); index++) {
            if (Character.isISOControl(text.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidPatient(Paciente paciente) {
        if (paciente == null || paciente.name == null || paciente.name.trim().isEmpty()
                || paciente.name.codePointCount(0, paciente.name.length()) < 2
                || paciente.name.codePointCount(0, paciente.name.length()) > 100
                || !containsLetter(paciente.name) || containsControlCharacter(paciente.name)
                || paciente.age < 0 || paciente.age > 130) {
            return false;
        }
        boolean validPhone = paciente.phone == null || paciente.phone.isEmpty()
                || (paciente.phone.matches("\\d+") && paciente.phone.length() >= 10 && paciente.phone.length() <= 13);
        boolean validEmail = paciente.email == null || paciente.email.isEmpty()
                || (paciente.email.length() <= 254 && paciente.email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$"));
        return validPhone && validEmail;
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private final class PatientTableModel extends AbstractTableModel {
        private final String[] columns = {"Nome", "Idade", "Telefone", "E-mail"};
        private List<Paciente> rows = Collections.emptyList();

        void setRows(List<Paciente> rows) {
            this.rows = new ArrayList<Paciente>(rows);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Paciente paciente = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return paciente.name;
                case 1: return paciente.age + " anos";
                case 2: return paciente.phone.isEmpty() ? "—" : paciente.phone;
                case 3: return paciente.email.isEmpty() ? "—" : paciente.email;
                default: return "";
            }
        }
    }

    private static final class PlainTextRenderer extends DefaultTableCellRenderer {
        @Override
        protected void setValue(Object value) {
            String text = value == null ? "" : value.toString();
            super.setValue(escapeHtml(text));
        }
    }

    private static final class Paciente {
        final String name;
        final int age;
        final String phone;
        final String email;

        Paciente(String name, int age, String phone, String email) {
            this.name = name;
            this.age = age;
            this.phone = phone;
            this.email = email;
        }
    }

    private static final class LoadResult {
        final List<Paciente> patients;
        final int invalidRows;

        LoadResult(List<Paciente> patients, int invalidRows) {
            this.patients = patients;
            this.invalidRows = invalidRows;
        }
    }

    private static final class ValidationException extends Exception {
        final JTextField field;

        ValidationException(String message) {
            this(message, null);
        }

        ValidationException(String message, JTextField field) {
            super(message);
            this.field = field;
        }
    }

    private static final class Storage {
        private static final String HEADER = "# cadastro-pacientes v1";
        private static final long MAX_FILE_BYTES = 64L * 1024L * 1024L;
        private final Path file;

        Storage(Path file) {
            this.file = file;
        }

        LoadResult load() throws IOException {
            if (!Files.exists(file)) {
                return new LoadResult(new ArrayList<Paciente>(), 0);
            }
            long size = Files.size(file);
            if (size > MAX_FILE_BYTES) {
                throw new IOException("O arquivo ultrapassa o tamanho permitido.");
            }
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.isEmpty() || !HEADER.equals(lines.get(0))) {
                throw new IOException("Formato de arquivo não reconhecido.");
            }
            if (lines.size() > MAX_PATIENTS + 1) {
                throw new IOException("O arquivo contém registros demais.");
            }
            List<Paciente> patients = new ArrayList<Paciente>();
            int invalid = 0;
            for (int index = 1; index < lines.size(); index++) {
                String line = lines.get(index);
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    String[] parts = line.split("\\t", -1);
                    if (parts.length != 4) {
                        invalid++;
                        continue;
                    }
                    Paciente paciente = new Paciente(decode(parts[0]), Integer.parseInt(parts[1]),
                            decode(parts[2]), decode(parts[3]));
                    if (!isValidPatient(paciente)) {
                        invalid++;
                        continue;
                    }
                    patients.add(paciente);
                } catch (Exception exception) {
                    invalid++;
                }
            }
            return new LoadResult(patients, invalid);
        }

        void save(List<Paciente> patients, int invalidRows) throws IOException {
            if (patients.size() > MAX_PATIENTS) {
                throw new IOException("O limite de registros foi ultrapassado.");
            }
            Path absoluteFile = file.toAbsolutePath();
            Path parent = absoluteFile.getParent();
            if (parent == null) {
                throw new IOException("Pasta de destino inválida.");
            }
            Files.createDirectories(parent);

            if (invalidRows > 0 && Files.exists(file)) {
                Files.copy(file, nextBackupPath(), StandardCopyOption.COPY_ATTRIBUTES);
            }

            List<String> lines = new ArrayList<String>();
            lines.add(HEADER);
            for (Paciente paciente : patients) {
                if (!isValidPatient(paciente)) {
                    throw new IOException("Um registro não passou pela validação de segurança.");
                }
                lines.add(encode(paciente.name) + "\t" + paciente.age + "\t"
                        + encode(paciente.phone) + "\t" + encode(paciente.email));
            }

            Path temporaryFile = Files.createTempFile(parent, "pacientes-", ".tmp");
            boolean moved = false;
            try {
                Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
                try {
                    Files.move(temporaryFile, absoluteFile, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporaryFile, absoluteFile, StandardCopyOption.REPLACE_EXISTING);
                }
                moved = true;
            } finally {
                if (!moved) {
                    try {
                        Files.deleteIfExists(temporaryFile);
                    } catch (IOException ignored) {
                        // A gravação original já falhou; não interrompa a interface por limpeza temporária.
                    }
                }
            }
        }

        private Path nextBackupPath() throws IOException {
            Path first = file.resolveSibling(file.getFileName().toString() + ".bak");
            if (!Files.exists(first)) {
                return first;
            }
            for (int index = 2; index <= 100; index++) {
                Path candidate = file.resolveSibling(file.getFileName().toString() + ".bak-" + index);
                if (!Files.exists(candidate)) {
                    return candidate;
                }
            }
            throw new IOException("Não foi possível criar uma cópia de segurança do arquivo original.");
        }
    }
}

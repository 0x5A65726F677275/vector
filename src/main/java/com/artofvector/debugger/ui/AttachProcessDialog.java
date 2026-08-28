package com.artofvector.debugger.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import com.artofvector.ui.theme.UiControls;
import com.artofvector.ui.theme.UiIcons;
import com.artofvector.ui.theme.UiTheme;

/**
 * Lists running processes so Attach can pick a PID instead of guessing.
 * {@link Optional#empty()} means cancelled; {@code 0} means simulated target.
 */
public final class AttachProcessDialog extends JDialog {

    public record Choice(long pid, boolean simulated) {
    }

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"PID", "User", "Program", "Command"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0 ? Long.class : String.class;
        }
    };
    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField filter = new JTextField();
    private Choice result;

    private AttachProcessDialog(Window owner) {
        super(owner, "Attach to process", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(UiTheme.BG_PANEL);

        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(false);
        table.setFont(UiTheme.MONO_SMALL);
        table.setRowHeight(UiTheme.MONO_SMALL.getSize() + 10);
        table.setBackground(UiTheme.BG_PANEL);
        table.setForeground(UiTheme.TEXT);
        table.setSelectionBackground(UiTheme.BG_SELECTED);
        table.setSelectionForeground(UiTheme.TEXT);
        table.setGridColor(UiTheme.BORDER);
        table.getTableHeader().setBackground(UiTheme.BG_ELEVATED);
        table.getTableHeader().setForeground(UiTheme.TEXT_MUTED);
        table.getTableHeader().setFont(UiTheme.UI_FONT);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(420);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    acceptSelected();
                }
            }
        });

        filter.setFont(UiTheme.UI_FONT);
        filter.setBackground(UiTheme.BG_INPUT);
        filter.setForeground(UiTheme.TEXT);
        filter.setCaretColor(UiTheme.TEXT);
        filter.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(UiTheme.BORDER),
                new EmptyBorder(6, 8, 6, 8)));
        filter.putClientProperty("JTextField.placeholderText", "Filter by name, command, or PID");
        filter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }
        });

        JLabel hint = new JLabel("Select a running program, then Attach. Simulated target does not need a PID.");
        hint.setForeground(UiTheme.TEXT_MUTED);
        hint.setFont(UiTheme.UI_FONT);
        hint.setBorder(new EmptyBorder(0, 0, 8, 0));

        JLabel searchLabel = new JLabel("Filter", UiIcons.of(UiIcons.Glyph.OPEN, 14), JLabel.LEFT);
        searchLabel.setForeground(UiTheme.TEXT_MUTED);
        searchLabel.setFont(UiTheme.UI_FONT);

        JPanel search = new JPanel(new BorderLayout(8, 0));
        search.setOpaque(false);
        search.add(searchLabel, BorderLayout.WEST);
        search.add(filter, BorderLayout.CENTER);

        JPanel north = new JPanel(new BorderLayout());
        north.setBackground(UiTheme.BG_PANEL);
        north.setBorder(new EmptyBorder(12, 12, 8, 12));
        north.add(hint, BorderLayout.NORTH);
        north.add(search, BorderLayout.SOUTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(javax.swing.BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.getViewport().setBackground(UiTheme.BG_PANEL);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBackground(UiTheme.BG_ELEVATED);
        buttons.setBorder(new EmptyBorder(8, 12, 8, 12));
        buttons.add(UiControls.toolButton("Refresh", UiIcons.Glyph.RESET, this::reload));
        buttons.add(UiControls.toolButton("Simulated target", UiIcons.Glyph.DEBUGGER, this::acceptSimulated));
        buttons.add(UiControls.primaryButton("Attach", UiIcons.Glyph.ATTACH, this::acceptSelected));
        buttons.add(UiControls.toolButton("Cancel", UiIcons.Glyph.CLEAR, this::dispose));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiTheme.BG_PANEL);
        root.add(north, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        setPreferredSize(new Dimension(780, 520));
        pack();
        setLocationRelativeTo(owner);
        reload();
        SwingUtilities.invokeLater(filter::requestFocusInWindow);
    }

    public static Optional<Choice> show(java.awt.Component parent) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (owner == null && parent instanceof Window window) {
            owner = window;
        }
        AttachProcessDialog dialog = new AttachProcessDialog(owner);
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.result);
    }

    private void reload() {
        model.setRowCount(0);
        long self = ProcessHandle.current().pid();
        List<Object[]> rows = new ArrayList<>();
        ProcessHandle.allProcesses()
                .filter(handle -> handle.pid() != self)
                .forEach(handle -> {
                    String command = commandLine(handle);
                    String program = programName(handle, command);
                    if (program.isBlank() && command.isBlank()) {
                        return;
                    }
                    String user = handle.info().user().orElse("");
                    rows.add(new Object[]{handle.pid(), user, program, command});
                });
        rows.sort(Comparator.comparing(row -> (Long) row[0]));
        for (Object[] row : rows) {
            model.addRow(row);
        }
        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private void applyFilter() {
        String query = filter.getText().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                for (int i = 0; i < entry.getValueCount(); i++) {
                    if (String.valueOf(entry.getValue(i)).toLowerCase(Locale.ROOT).contains(query)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void acceptSelected() {
        int view = table.getSelectedRow();
        if (view < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(view);
        long pid = (Long) model.getValueAt(modelRow, 0);
        result = new Choice(pid, false);
        dispose();
    }

    private void acceptSimulated() {
        result = new Choice(0, true);
        dispose();
    }

    private static String programName(ProcessHandle handle, String commandLine) {
        Optional<String> command = handle.info().command();
        if (command.isPresent()) {
            Path path = Path.of(command.get());
            Path name = path.getFileName();
            return name == null ? command.get() : name.toString();
        }
        if (!commandLine.isBlank()) {
            String first = commandLine.split("\\s+")[0];
            int slash = Math.max(first.lastIndexOf('/'), first.lastIndexOf('\\'));
            return slash >= 0 ? first.substring(slash + 1) : first;
        }
        return "";
    }

    private static String commandLine(ProcessHandle handle) {
        Optional<String> line = handle.info().commandLine();
        if (line.isPresent() && !line.get().isBlank()) {
            return line.get();
        }
        Optional<String> command = handle.info().command();
        Path proc = Path.of("/proc/" + handle.pid() + "/cmdline");
        if (Files.isReadable(proc)) {
            try {
                String raw = Files.readString(proc, StandardCharsets.UTF_8).replace('\0', ' ').trim();
                if (!raw.isBlank()) {
                    return raw;
                }
            } catch (Exception ignored) {
                // Fall through to command path.
            }
        }
        return command.orElse("");
    }
}

package sigelab.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import sigelab.model.Auditoria;
import sigelab.service.AuditoriaService;

public class AuditoriaView extends JPanel {

    private final AuditoriaService auditoriaService;

    private JTable tablaAuditoria;
    private DefaultTableModel modeloTabla;
    private JTextField txtFiltroAccion;
    private JTextField txtFiltroEntidad;
    private JButton btnFiltrar;
    private JButton btnLimpiar;

    public AuditoriaView() {
        this(new AuditoriaService());
    }

    public AuditoriaView(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
        initComponents();
        cargarDatos();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel Superior
        JPanel panelSuperior = new JPanel(new BorderLayout(10, 10));
        JLabel lblTitulo = new JLabel("Bitácora de Auditoría del Sistema (RN-18)");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(18f));
        panelSuperior.add(lblTitulo, BorderLayout.NORTH);

        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelFiltros.add(new JLabel("Acción:"));
        txtFiltroAccion = new JTextField(10);
        panelFiltros.add(txtFiltroAccion);

        panelFiltros.add(new JLabel("Entidad:"));
        txtFiltroEntidad = new JTextField(10);
        panelFiltros.add(txtFiltroEntidad);

        btnFiltrar = new JButton("Buscar");
        btnFiltrar.addActionListener(e -> filtrarDatos());
        panelFiltros.add(btnFiltrar);

        btnLimpiar = new JButton("Mostrar Todos");
        btnLimpiar.addActionListener(e -> {
            txtFiltroAccion.setText("");
            txtFiltroEntidad.setText("");
            cargarDatos();
        });
        panelFiltros.add(btnLimpiar);

        panelSuperior.add(panelFiltros, BorderLayout.SOUTH);
        add(panelSuperior, BorderLayout.NORTH);

        // Tabla Central
        String[] columnas = {"ID", "ID Usuario", "Fecha y Hora", "Acción", "Entidad", "ID Registro", "Descripción"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaAuditoria = new JTable(modeloTabla);
        tablaAuditoria.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaAuditoria.setRowHeight(22);
        JScrollPane scroll = new JScrollPane(tablaAuditoria);
        add(scroll, BorderLayout.CENTER);
    }

    public void cargarDatos() {
        modeloTabla.setRowCount(0);
        try {
            List<Auditoria> lista = auditoriaService.obtenerHistorial();
            for (Auditoria a : lista) {
                modeloTabla.addRow(new Object[]{
                    a.getIdAuditoria(),
                    a.getIdUsuario() != null ? a.getIdUsuario() : "(Sin usuario)",
                    a.getFechaHora(),
                    a.getAccion(),
                    a.getEntidad(),
                    a.getIdRegistro() != null ? a.getIdRegistro() : "-",
                    a.getDescripcion()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al consultar bitácora: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filtrarDatos() {
        modeloTabla.setRowCount(0);
        try {
            String accion = txtFiltroAccion.getText().trim();
            String entidad = txtFiltroEntidad.getText().trim();

            List<Auditoria> lista = auditoriaService.filtrarHistorial(accion, entidad, null);
            for (Auditoria a : lista) {
                modeloTabla.addRow(new Object[]{
                    a.getIdAuditoria(),
                    a.getIdUsuario() != null ? a.getIdUsuario() : "(Sin usuario)",
                    a.getFechaHora(),
                    a.getAccion(),
                    a.getEntidad(),
                    a.getIdRegistro() != null ? a.getIdRegistro() : "-",
                    a.getDescripcion()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al filtrar bitácora: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

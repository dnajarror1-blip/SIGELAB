package sigelab.view;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import sigelab.controller.ReservaController;
import sigelab.enums.EstadoReserva;
import sigelab.repository.ReservaRepository.*;

public class ReservaView extends JPanel {
    private final ReservaController controller;
    private final JComboBox<Solicitante> solicitante = new JComboBox<>();
    private final JComboBox<EquipoReservable> equipo = new JComboBox<>();
    private final JTextField inicio = new JTextField(LocalDate.now().toString(),10);
    private final JTextField fin = new JTextField(LocalDate.now().toString(),10);
    private final JTextField observaciones = new JTextField(20);
    private final JButton crear = new JButton("Crear reserva"), cancelar = new JButton("Cancelar seleccionada"),
            actualizar = new JButton("Actualizar");
    private final JLabel estado = new JLabel("Reserva por fechas; no constituye un préstamo ni cambia el estado del equipo.");
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Reserva","Solicitante","Código de equipo","Equipo","Inicio","Fin","Estado","Observaciones"},0) {
        public boolean isCellEditable(int fila,int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private List<Resumen> reservas = List.of();
    private boolean ocupado;

    private record Carga(List<Resumen> reservas,List<Solicitante> solicitantes,List<EquipoReservable> equipos) { }
    public ReservaView(int actor) { this(new ReservaController(actor)); }
    ReservaView(ReservaController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10,12));
        setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        JPanel arriba = new JPanel(new BorderLayout(8,8));
        JLabel titulo = new JLabel("Reservas de equipos"); titulo.setFont(titulo.getFont().deriveFont(20f));
        arriba.add(titulo,BorderLayout.NORTH);
        JPanel formulario = new JPanel(new GridLayout(0,2,8,6));
        formulario.add(new JLabel("Solicitante activo:")); formulario.add(solicitante);
        formulario.add(new JLabel("Equipo (estado actual informativo):")); formulario.add(equipo);
        formulario.add(new JLabel("Inicio · AAAA-MM-DD:")); formulario.add(inicio);
        formulario.add(new JLabel("Fin · AAAA-MM-DD:")); formulario.add(fin);
        formulario.add(new JLabel("Observaciones · máximo 250 caracteres:")); formulario.add(observaciones);
        arriba.add(formulario,BorderLayout.CENTER); add(arriba,BorderLayout.NORTH);
        solicitante.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f) {
                super.getListCellRendererComponent(l,v,i,s,f); if (v instanceof Solicitante u) setText(u.nombre()); return this;
            }
        });
        equipo.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f) {
                super.getListCellRendererComponent(l,v,i,s,f);
                if (v instanceof EquipoReservable e) setText(e.codigo() + " · " + e.nombre() + " · " + e.estado()); return this;
            }
        });
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); tabla.setRowHeight(26); tabla.setAutoCreateRowSorter(true);
        add(new JScrollPane(tabla),BorderLayout.CENTER);
        JPanel abajo = new JPanel(new BorderLayout(8,8)), acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.add(crear); acciones.add(cancelar); acciones.add(actualizar);
        abajo.add(acciones,BorderLayout.NORTH); abajo.add(estado,BorderLayout.SOUTH); add(abajo,BorderLayout.SOUTH);
        tabla.getSelectionModel().addListSelectionListener(e -> actualizarAcciones());
        crear.addActionListener(e -> crear()); cancelar.addActionListener(e -> cancelar());
        actualizar.addActionListener(e -> cargar()); cargar();
    }

    private void ocupado(boolean valor) {
        ocupado = valor;
        solicitante.setEnabled(!valor); equipo.setEnabled(!valor); inicio.setEnabled(!valor); fin.setEnabled(!valor);
        observaciones.setEnabled(!valor); actualizar.setEnabled(!valor); tabla.setEnabled(!valor); actualizarAcciones();
    }
    private void actualizarAcciones() {
        crear.setEnabled(!ocupado && solicitante.getSelectedItem()!=null && equipo.getSelectedItem()!=null);
        int fila = tabla.getSelectedRow();
        cancelar.setEnabled(!ocupado && fila>=0 && reservas.get(tabla.convertRowIndexToModel(fila)).reserva().getEstado()==EstadoReserva.ACTIVA);
    }
    private void cargar() {
        if (ocupado) return;
        ocupado(true);
        OperacionModulo.ejecutar(this, () -> new Carga(controller.listar(),controller.listarSolicitantes(),controller.listarEquipos()),datos -> {
            tabla.clearSelection(); modelo.setRowCount(0); reservas = List.copyOf(datos.reservas());
            solicitante.removeAllItems(); for (Solicitante u : datos.solicitantes()) solicitante.addItem(u);
            equipo.removeAllItems(); for (EquipoReservable e : datos.equipos()) equipo.addItem(e);
            for (Resumen r : reservas) modelo.addRow(new Object[]{r.reserva().getIdReserva(),r.solicitante(),r.codigoEquipo(),r.nombreEquipo(),
                    r.reserva().getFechaInicio(),r.reserva().getFechaFin(),r.reserva().getEstado(),r.reserva().getObservaciones()});
        }, () -> ocupado(false));
    }
    private void crear() {
        if (ocupado) return;
        Solicitante u = (Solicitante)solicitante.getSelectedItem(); EquipoReservable e = (EquipoReservable)equipo.getSelectedItem();
        if (u==null || e==null) return;
        try {
            LocalDate desde = LocalDate.parse(inicio.getText().trim()), hasta = LocalDate.parse(fin.getText().trim());
            if (JOptionPane.showConfirmDialog(this,"¿Crear la reserva para " + u.nombre() + "?","Crear reserva",
                    JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return;
            String obs = observaciones.getText(); ocupado(true);
            OperacionModulo.ejecutar(this, () -> controller.crear(u.idUsuario(),e.idEquipo(),desde,hasta,obs),r -> {
                observaciones.setText(""); estado.setText("Reserva " + r.getIdReserva() + " creada. El equipo conserva su estado físico.");
            }, () -> { ocupado(false); cargar(); });
        } catch (DateTimeParseException error) {
            JOptionPane.showMessageDialog(this,"Indique fechas válidas con formato AAAA-MM-DD.","Revisar fechas",JOptionPane.WARNING_MESSAGE);
        }
    }
    private void cancelar() {
        if (ocupado || tabla.getSelectedRow()<0) return;
        Resumen r = reservas.get(tabla.convertRowIndexToModel(tabla.getSelectedRow()));
        if (JOptionPane.showConfirmDialog(this,"¿Cancelar la reserva seleccionada?","Cancelar reserva",
                JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return;
        ocupado(true);
        OperacionModulo.ejecutar(this, () -> { controller.cancelar(r.reserva().getIdReserva()); return true; },
                resultado -> estado.setText("Reserva cancelada."), () -> { ocupado(false); cargar(); });
    }
}

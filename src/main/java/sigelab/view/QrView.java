package sigelab.view;

import sigelab.controller.*;
import sigelab.model.*;
import sigelab.enums.EstadoEquipo;
import sigelab.service.Modulo;
import sigelab.service.CatalogoSeleccionService.EquipoOpcion;
import sigelab.view.ui.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class QrView extends JPanel {
    private final QrController controller;
    private final CatalogoSeleccionController catalogos;
    private final SelectorBuscable<EquipoOpcion> equipo = new SelectorBuscable<>(
            "Buscar equipo por código, nombre o marca", EquipoOpcion::toString, EquipoOpcion::busqueda);
    private final JButton generar = new JButton("Generar QR"), leer = new JButton("Leer QR"), actualizar = new JButton("Actualizar equipos");
    private final JLabel codigo = new JLabel("—"), nombre = new JLabel("—"), laboratorio = new JLabel("—"), estado = new JLabel("—"), disponibilidad = new JLabel("Selecciona un equipo o una imagen QR.");
    private final JLabel mensaje = new JLabel(" ");
    private boolean ocupado;
    private boolean catalogoDisponible;

    public QrView(Usuario usuario) {
        this(new QrController(usuario), new CatalogoSeleccionController(usuario.getIdUsuario(), Modulo.REPORTES_QR));
    }
    QrView(QrController controller, CatalogoSeleccionController catalogos) {
        super(new BorderLayout(10, 10));
        this.controller = controller; this.catalogos = catalogos;
        JPanel acciones = new JPanel(new BorderLayout(8, 8));
        acciones.add(new JLabel("Equipo"), BorderLayout.NORTH); acciones.add(equipo);
        JPanel botones = new JPanel(); botones.add(generar); botones.add(leer); botones.add(actualizar);
        UiTheme.botonPrimario(generar); UiTheme.botonSecundario(leer); UiTheme.botonSecundario(actualizar);
        acciones.add(botones, BorderLayout.SOUTH); add(acciones, BorderLayout.NORTH);
        JPanel tarjeta = new JPanel(new GridLayout(0, 2, 12, 16)); tarjeta.setBorder(UiTheme.bordeTarjeta());
        tarjeta.add(new JLabel("Código")); tarjeta.add(codigo);
        tarjeta.add(new JLabel("Nombre")); tarjeta.add(nombre);
        tarjeta.add(new JLabel("Laboratorio")); tarjeta.add(laboratorio);
        tarjeta.add(new JLabel("Estado")); tarjeta.add(estado);
        tarjeta.add(new JLabel("Disponibilidad")); tarjeta.add(disponibilidad);
        add(tarjeta); add(mensaje, BorderLayout.SOUTH);
        generar.addActionListener(e -> generar()); leer.addActionListener(e -> leer());
        actualizar.addActionListener(e -> cargar()); cargar();
    }
    private void ocupado(boolean valor) {
        ocupado = valor; generar.setEnabled(!valor && catalogoDisponible); leer.setEnabled(!valor);
        actualizar.setEnabled(!valor); equipo.setEnabled(!valor && catalogoDisponible);
    }
    private void cargar() {
        if (ocupado) return;
        ocupado(true); mensaje.setText("Cargando...");
        OperacionModulo.ejecutar(this, () -> catalogos.listarEquipos(false), lista -> {
            equipo.valores(lista); catalogoDisponible = true;
            mensaje.setText(lista.isEmpty() ? "No se encontraron equipos." : "Selecciona un equipo para generar su QR.");
        }, () -> { ocupado(false); if ("Cargando...".equals(mensaje.getText())) mensaje.setText("No se pudo cargar. Pulsa Actualizar equipos."); });
    }
    private void generar() {
        EquipoOpcion seleccionado = equipo.seleccionado();
        if (seleccionado == null) { mensaje.setText("Selecciona un equipo de la lista."); return; }
        JFileChooser selector = new JFileChooser();
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File archivo = selector.getSelectedFile();
        if (!archivo.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".png")) archivo = new File(archivo.getParentFile(), archivo.getName() + ".png");
        if (archivo.exists() && JOptionPane.showConfirmDialog(this, "El archivo ya existe. ¿Reemplazarlo?", "Generar QR", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        generarSeleccionado(archivo);
    }
    void generarSeleccionado(File destino) {
        EquipoOpcion seleccionado = equipo.seleccionado();
        if (ocupado || seleccionado == null) { mensaje.setText("Selecciona un equipo de la lista."); return; }
        ocupado(true); mensaje.setText("Generando QR...");
        OperacionModulo.ejecutar(this, () -> { controller.generar(seleccionado.equipo().getCodigoInventario(), destino); return seleccionado; }, resultado -> {
            mostrarEquipo(resultado.equipo(), resultado.laboratorio()); mensaje.setText("QR generado.");
        }, () -> { ocupado(false); if ("Generando QR...".equals(mensaje.getText())) mensaje.setText("No se pudo generar el QR."); });
    }
    private void leer() {
        JFileChooser selector = new JFileChooser();
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        identificar(selector.getSelectedFile());
    }
    void identificar(File imagen) {
        if (ocupado) return;
        ocupado(true); mensaje.setText("Leyendo QR...");
        codigo.setText("—"); nombre.setText("—"); laboratorio.setText("—"); estado.setText("—"); disponibilidad.setText("Consultando...");
        OperacionModulo.ejecutar(this, () -> {
            Equipo encontrado = controller.identificar(imagen).equipo();
            String lab = catalogos.listarEquipos(false).stream().filter(e -> e.equipo().getIdEquipo().equals(encontrado.getIdEquipo()))
                    .map(EquipoOpcion::laboratorio).findFirst().orElse("Sin laboratorio disponible");
            return new EquipoOpcion(encontrado, "", lab);
        }, resultado -> { mostrarEquipo(resultado.equipo(), resultado.laboratorio()); mensaje.setText("Equipo identificado."); },
                () -> { ocupado(false); if ("Leyendo QR...".equals(mensaje.getText())) { mensaje.setText("No se pudo identificar el equipo."); disponibilidad.setText("Sin equipo identificado."); } });
    }
    private void mostrarEquipo(Equipo e, String lab) {
        codigo.setText(e.getCodigoInventario()); nombre.setText(e.getNombre()); laboratorio.setText(lab); estado.setText(String.valueOf(e.getEstado()));
        disponibilidad.setText(e.getEstado() == EstadoEquipo.DISPONIBLE ? "Disponible para préstamo" : "No disponible para préstamo");
    }
}

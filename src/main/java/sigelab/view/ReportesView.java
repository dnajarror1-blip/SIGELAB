package sigelab.view;
import sigelab.controller.ReporteController;
import sigelab.enums.EstadoEquipo;
import sigelab.enums.EstadoMantenimiento;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class ReportesView extends JPanel{
 private final ReporteController controller;private final JComboBox<String> tipo=new JComboBox<>(new String[]{"Dashboard","Equipos por estado","Préstamos","Préstamos por fechas","Historial de equipos","Historial de mantenimientos"});
 private final JComboBox<EstadoEquipo> eq=new JComboBox<>(EstadoEquipo.values());private final JComboBox<EstadoMantenimiento> mant=new JComboBox<>(EstadoMantenimiento.values());private final JTextField desde=new JTextField(10),hasta=new JTextField(10);private final JLabel resumen=new JLabel(" ");
 private final DefaultTableModel model=new DefaultTableModel(){public boolean isCellEditable(int r,int c){return false;}};
 public ReportesView(){this(new ReporteController());}
 public ReportesView(ReporteController c){if(c==null)throw new IllegalArgumentException("Controlador de reportes obligatorio.");controller=c;setLayout(new BorderLayout());JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT));p.add(tipo);p.add(eq);p.add(mant);p.add(new JLabel("Desde AAAA-MM-DD"));p.add(desde);p.add(new JLabel("Hasta"));p.add(hasta);JButton go=new JButton("Consultar");go.addActionListener(e->cargar());p.add(go);add(p,BorderLayout.NORTH);add(new JScrollPane(new JTable(model)),BorderLayout.CENTER);add(resumen,BorderLayout.SOUTH);tipo.addActionListener(e->filtros());filtros();cargar();}
 private void filtros(){String s=(String)tipo.getSelectedItem();eq.setVisible("Equipos por estado".equals(s));mant.setVisible("Historial de mantenimientos".equals(s));boolean b="Préstamos por fechas".equals(s);desde.setVisible(b);hasta.setVisible(b);}
 private void cargar(){try{String s=(String)tipo.getSelectedItem();List<Map<String,Object>> rows;if("Dashboard".equals(s)){Map<String,Object> m=controller.dashboard();resumen.setText(m.toString());rows=List.of(m);}else{resumen.setText(" ");rows=switch(s){case "Equipos por estado"->controller.equiposPorEstado((EstadoEquipo)eq.getSelectedItem());case "Préstamos"->controller.prestamos();case "Préstamos por fechas"->controller.prestamosEntreFechas(LocalDate.parse(desde.getText().trim()),LocalDate.parse(hasta.getText().trim()));case "Historial de equipos"->controller.historialEquipos();case "Historial de mantenimientos"->controller.historialMantenimientos((EstadoMantenimiento)mant.getSelectedItem());default->List.of();};}mostrar(rows);}catch(RuntimeException e){JOptionPane.showMessageDialog(this,e.getMessage(),"Reportes",JOptionPane.ERROR_MESSAGE);}}
 private void mostrar(List<Map<String,Object>> rows){model.setRowCount(0);model.setColumnCount(0);if(rows==null||rows.isEmpty()){model.addColumn("Resultado");return;}List<String> cols=new ArrayList<>(rows.get(0).keySet());cols.forEach(model::addColumn);for(Map<String,Object> row:rows)model.addRow(cols.stream().map(row::get).toArray());}
}

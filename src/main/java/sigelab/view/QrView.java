package sigelab.view;
import sigelab.controller.QrController;
import sigelab.enums.EstadoEquipo;
import sigelab.service.QrEquipoService;
import javax.swing.*;
import java.awt.*;
import java.io.File;
public class QrView extends JPanel{
 private final QrController controller;private final JTextField codigo=new JTextField(15);private final JTextArea salida=new JTextArea(8,40);
 public QrView(){this(new QrController());}
 public QrView(QrController c){if(c==null)throw new IllegalArgumentException("Controlador QR obligatorio.");controller=c;setLayout(new BorderLayout());JPanel p=new JPanel();p.add(new JLabel("Código:"));p.add(codigo);JButton gen=new JButton("Generar QR");gen.addActionListener(e->generar());p.add(gen);JButton read=new JButton("Leer QR");read.addActionListener(e->leer());p.add(read);add(p,BorderLayout.NORTH);salida.setEditable(false);add(new JScrollPane(salida),BorderLayout.CENTER);}
 private void generar(){JFileChooser c=new JFileChooser();if(c.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;try{File f=c.getSelectedFile();if(!f.getName().toLowerCase().endsWith(".png"))f=new File(f.getParentFile(),f.getName()+".png");controller.generar(codigo.getText(),f);JOptionPane.showMessageDialog(this,"QR generado.");}catch(RuntimeException e){error(e);}}
 private void leer(){JFileChooser c=new JFileChooser();if(c.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;try{QrEquipoService.Resultado r=controller.identificar(c.getSelectedFile());EstadoEquipo s=r.equipo().getEstado();String uso=s==EstadoEquipo.DISPONIBLE?"Disponible para uso.":s==EstadoEquipo.PRESTADO?"Identificado, pero bloqueado porque está prestado.":"Identificado, pero no disponible.";salida.setText("Código: "+r.codigoInventario()+"\nEquipo: "+r.equipo().getNombre()+"\nMarca: "+r.equipo().getMarca()+"\nModelo: "+r.equipo().getModelo()+"\nEstado: "+s+"\n"+uso);}catch(RuntimeException e){error(e);}}
 private void error(RuntimeException e){JOptionPane.showMessageDialog(this,e.getMessage(),"QR",JOptionPane.ERROR_MESSAGE);}
}

package sigelab.controller;
import sigelab.service.QrEquipoService;
import java.io.File;
public class QrController{
 private final QrEquipoService service;
 public QrController(){this(new QrEquipoService());}
 public QrController(QrEquipoService s){if(s==null)throw new IllegalArgumentException("Servicio QR obligatorio.");service=s;}
 public QrEquipoService.Resultado identificar(File f){return service.identificar(f);}
 public void generar(String c,File f){service.generar(c,f);}
}

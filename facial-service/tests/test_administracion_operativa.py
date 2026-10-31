"""Autorización real por nombre de rol, transacciones y coordinación sin DB/cámara."""
import struct
import unittest
from unittest.mock import MagicMock, patch
from app import administracion_repository as repo, auth_service as service


class TestAutorizacionAdministrativa(unittest.TestCase):
    def test_admin_activo_permitido_otros_rechazados(self):
        for row, permitido in [(('ACTIVO','ADMIN'),True), (('ACTIVO','ENCARGADO'),False),
                                (('INACTIVO','ADMIN'),False), (None,False)]:
            conexion = MagicMock()
            cursor = conexion.cursor.return_value
            cursor.execute.return_value.fetchone.return_value = row
            if permitido:
                repo.exigir_admin(conexion,42)
            else:
                with self.assertRaises(repo.RechazoFacial):
                    repo.exigir_admin(conexion,42)
            sql, actor = cursor.execute.call_args.args
            self.assertIn('JOIN dbo.Rol', sql)
            self.assertIn('r.id_rol = u.id_rol', sql)
            self.assertEqual(actor,42)
            cursor.close.assert_called_once()
            conexion.commit.assert_not_called()

    def test_rechazo_administrativo_no_abre_camara(self):
        with (patch.object(repo,'comprobar_registro',side_effect=repo.RechazoFacial(403,'ADMIN requerido')),
              patch.object(service,'ejecutar_captura') as capture,
              patch.object(repo,'guardar_registro') as save):
            with self.assertRaises(repo.RechazoFacial):
                service.registrar(7,42,False)
            capture.assert_not_called()
            save.assert_not_called()

    def test_perfil_existente_exige_replace(self):
        conexion = MagicMock()
        conexion.cursor.return_value.execute.return_value.fetchone.return_value = (1,)
        with self.assertRaises(repo.RechazoFacial):
            repo.exigir_perfil_disponible(conexion,7,False)
        repo.exigir_perfil_disponible(conexion,7,True)

    def test_enroll_valido_persiste_actor_y_replace(self):
        perfil = {'embedding':struct.pack('<512f',*([1.]*512)), 'cantidad_muestras':6,
                  'confianza_deteccion_media':.95,'liveness_promedio':.99}
        with (patch.object(repo,'comprobar_registro') as authorize,
              patch.object(service,'obtener_clave'),
              patch.object(repo,'registrar_evento'),
              patch.object(service,'ejecutar_captura',return_value=perfil),
              patch.object(repo,'guardar_registro') as save):
            self.assertTrue(service.registrar(7,42,True)['registered'])
            authorize.assert_called_once_with(7,42,True)
            save.assert_called_once_with(7,42,True,perfil)

    def test_eliminar_revalida_actor_y_audita_en_misma_transaccion(self):
        conexion = MagicMock()
        conexion.cursor.return_value.execute.return_value.fetchone.return_value = (1,)
        with (patch.object(repo,'get_connection',return_value=conexion),
              patch.object(repo,'verificar_base_configurada'),
              patch.object(repo,'exigir_admin') as admin,
              patch.object(repo,'exigir_objetivo'),
              patch.object(repo,'auditar') as audit):
            self.assertTrue(repo.eliminar_registro(7,42)['deleted'])
            admin.assert_called_once_with(conexion,42)
            audit.assert_called_once()
            conexion.commit.assert_called_once()
            conexion.rollback.assert_not_called()
            conexion.close.assert_called_once()

    def test_fallo_auditoria_revierte_eliminacion(self):
        conexion = MagicMock()
        conexion.cursor.return_value.execute.return_value.fetchone.return_value = (1,)
        with (patch.object(repo,'get_connection',return_value=conexion),
              patch.object(repo,'verificar_base_configurada'),
              patch.object(repo,'exigir_admin'),patch.object(repo,'exigir_objetivo'),
              patch.object(repo,'auditar',side_effect=RuntimeError('Fallo simulado'))):
            with self.assertRaises(RuntimeError):
                repo.eliminar_registro(7,42)
            conexion.rollback.assert_called_once()
            conexion.commit.assert_not_called()

    def test_enroll_error_auditoria_revierte_guardado(self):
        conexion = MagicMock()
        with (patch.object(repo,'get_connection',return_value=conexion),
              patch.object(repo,'verificar_base_configurada'),
              patch.object(repo,'exigir_admin'),patch.object(repo,'exigir_objetivo'),
              patch.object(repo,'exigir_perfil_disponible'),
              patch.object(repo,'guardar_perfil_facial') as save,
              patch.object(repo,'auditar',side_effect=RuntimeError('Fallo simulado'))):
            with self.assertRaises(RuntimeError):
                repo.guardar_registro(7,42,False,{'embedding':b'simulado','cantidad_muestras':6,
                                                'confianza_deteccion_media':.95})
            self.assertFalse(save.call_args.kwargs['registrar_auditoria'])
            conexion.rollback.assert_called_once()
            conexion.commit.assert_not_called()

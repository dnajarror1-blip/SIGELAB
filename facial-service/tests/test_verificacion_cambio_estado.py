"""Cambios de perfil/estado y decisión facial con fronteras simuladas."""
import struct
import unittest
from unittest.mock import patch
from app import auth_service as service
from app.administracion_repository import RechazoFacial
from app.camara_control import BLOQUEO_CAMARA


class TestCambioEstadoFacial(unittest.TestCase):
    def setUp(self):
        self.patches = []
        def mock(name, **kwargs):
            patcher = patch("app.auth_service." + name, **kwargs)
            self.patches.append(patcher)
            return patcher.start()
        mock("facial_threshold", return_value=.6)
        mock("liveness_threshold", return_value=.8)
        mock("obtener_clave")
        mock("obtener_embedding_facial", return_value=struct.pack("<512f", *([1.] * 512)))
        self.version = mock("repository.version_perfil", return_value=(1, b"cifrado"))
        self.audit = mock("repository.registrar_evento")
        self.capture = mock("ejecutar_captura", return_value={"score": .9,"livenessPassed": True,"livenessScore": .99})

    def tearDown(self):
        for patcher in reversed(self.patches):
            patcher.stop()
        self.assertFalse(BLOQUEO_CAMARA.locked())

    def test_exito_y_fallo_con_umbral(self):
        self.assertTrue(service.verificar(7)["matched"])
        self.capture.return_value["score"] = .2
        self.assertFalse(service.verificar(7)["matched"])

    def test_liveness_rechazado_deniega_aunque_score_alto(self):
        self.capture.return_value["livenessPassed"] = False
        self.assertFalse(service.verificar(7)["matched"])
        self.capture.return_value["livenessPassed"] = True
        self.capture.return_value["livenessScore"] = .1
        self.assertFalse(service.verificar(7)["matched"])

    def test_reemplazo_durante_captura_denegado(self):
        self.version.side_effect = [(1,b"a"),(1,b"a"),(1,b"b")]
        with self.assertRaises(RechazoFacial):
            service.verificar(7)
        self.assertFalse(any(call.args[2] == "EXITO" for call in self.audit.call_args_list))

    def test_inactivo_durante_captura_denegado(self):
        self.version.side_effect = [(1,b"a"),(1,b"a"),RechazoFacial(403,"Inactivo")]
        with self.assertRaises(RechazoFacial):
            service.verificar(7)

    def test_error_camara_y_cancelacion_auditados(self):
        self.capture.side_effect = RuntimeError("Cámara")
        with self.assertRaises(RuntimeError):
            service.verificar(7)
        self.assertEqual(self.audit.call_args.args[2], "FALLO")
        self.capture.side_effect = None
        self.capture.return_value = {"cancelled": True}
        with self.assertRaises(RechazoFacial):
            service.verificar(7)

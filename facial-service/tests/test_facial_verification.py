"""Verificación automática sin cámara, motor real, SQL ni perfiles persistidos."""
import base64
from concurrent.futures import Future
from contextlib import ExitStack
import io
import json
import struct
import unittest
from unittest.mock import MagicMock, patch

import numpy as np

from app import facial_verification as verification, capture_worker, facial_enrollment, facial_pipeline

EMBEDDING = struct.pack('<512f', 1., *([0.] * 511))


def sample(score=.9, live=.95, passed=True, status='ok'):
    return {'livenessStatus':status, 'livenessPassed':passed,
            'livenessScore':live, 'similarity':score}


class Clock:
    def __init__(self):
        self.now = 0.
    def __call__(self):
        self.now += .1
        return self.now


class TestAutomaticVerification(unittest.TestCase):
    def setUp(self):
        self.stack = ExitStack()
        self.addCleanup(self.stack.close)
        def mock(target, **kwargs):
            return self.stack.enter_context(patch(target, **kwargs))
        self.camera = MagicMock()
        self.camera.read.return_value = (True, np.zeros((100,100,3), dtype=np.uint8))
        self.open_camera = mock('app.facial_verification.abrir_camara', return_value=self.camera)
        self.clock = Clock()
        mock('app.facial_verification.perf_counter', side_effect=self.clock)
        mock('app.facial_verification.capture_timeout', return_value=120)
        mock('app.facial_verification.liveness_threshold', return_value=.8)
        mock('app.facial_verification.cv2.namedWindow')
        mock('app.facial_verification.cv2.putText')
        mock('app.facial_verification.cv2.imshow')
        self.visible = mock('app.facial_verification.cv2.getWindowProperty', return_value=1)
        self.key = mock('app.facial_verification.cv2.waitKey', return_value=-1)
        self.close_windows = mock('app.facial_verification.cv2.destroyAllWindows')
        self.pipeline = mock('app.facial_verification.analizar_captura_facial', return_value=sample())
        self.executor = MagicMock()
        mock('app.facial_verification.ThreadPoolExecutor', return_value=self.executor)
        self.submitted = []
        def submit(function, *args, **kwargs):
            self.submitted.append(self.clock.now)
            future = Future()
            try:
                future.set_result(function(*args, **kwargs))
            except Exception as error:
                future.set_exception(error)
            return future
        self.executor.submit.side_effect = submit

    def assert_cleanup(self):
        self.camera.release.assert_called_once()
        self.close_windows.assert_called_once()
        self.executor.shutdown.assert_called_once_with(wait=False, cancel_futures=True)

    def test_tres_automaticas_mediana_y_promedio_sin_decidir_match(self):
        self.pipeline.side_effect = [sample(.90,.9),sample(.10,.95),sample(.85,1.)]
        result = verification.capturar_verificacion(EMBEDDING)
        self.assertEqual(set(result), {'cancelled','livenessPassed','livenessScore','score'})
        self.assertFalse(result['cancelled'])
        self.assertTrue(result['livenessPassed'])
        self.assertAlmostEqual(result['score'],.85)
        self.assertAlmostEqual(result['livenessScore'],.95)
        self.assertEqual(self.pipeline.call_count,3)
        for call in self.pipeline.call_args_list:
            self.assertEqual(call.args[1],EMBEDDING)
            self.assertEqual(call.kwargs,{'exigir_calidad':True})
        self.assertTrue(all(b-a >= .6 for a,b in zip(self.submitted,self.submitted[1:])))
        self.assert_cleanup()

    def test_embedding_invalido_no_abre_camara(self):
        with self.assertRaises(ValueError):
            verification.capturar_verificacion(b'no es un embedding')
        self.open_camera.assert_not_called()

    def test_q_y_escape_cancelan_sin_analizar(self):
        for key in (ord('q'),ord('Q'),27):
            self.key.return_value=key
            self.assertEqual(verification.capturar_verificacion(EMBEDDING), {'cancelled':True})
        self.pipeline.assert_not_called()
        self.assertEqual(self.camera.release.call_count,3)

    def test_cerrar_ventana_cancela_sin_reabrirla(self):
        self.visible.return_value=0
        self.assertEqual(verification.capturar_verificacion(EMBEDDING), {'cancelled':True})
        self.pipeline.assert_not_called()
        self.assert_cleanup()

    def test_timeout_sin_muestras_libera_recursos(self):
        self.pipeline.return_value=sample(None,None,None,'input_rejected')
        with patch.object(verification,'capture_timeout', return_value=2):
            with self.assertRaises(TimeoutError):
                verification.capturar_verificacion(EMBEDDING)
        self.assert_cleanup()

    def test_sin_rostro_varios_baja_calidad_e_input_rejected_permiten_reintentar(self):
        self.pipeline.side_effect=[ValueError('cero rostros'), ValueError('varios rostros'),
                                   ValueError('baja calidad'),sample(None,None,None,'input_rejected'),
                                   sample(),sample(),sample()]
        self.assertTrue(verification.capturar_verificacion(EMBEDDING)['livenessPassed'])
        self.assertEqual(self.pipeline.call_count,7)
        self.assert_cleanup()

    def test_tres_spoofs_intercalados_impiden_aprobar(self):
        self.pipeline.side_effect=[sample(passed=False,live=.1),sample(),
                                   sample(passed=False,live=.2),sample(),
                                   sample(passed=False,live=.3)]
        result=verification.capturar_verificacion(EMBEDDING)
        self.assertFalse(result['livenessPassed'])
        self.assertEqual(result['score'],-1.)
        self.assertAlmostEqual(result['livenessScore'],.2)
        self.assert_cleanup()

    def test_live_score_bajo_aunque_flag_true_no_se_acepta(self):
        self.pipeline.return_value=sample(live=.2)
        self.assertFalse(verification.capturar_verificacion(EMBEDDING)['livenessPassed'])
        self.assert_cleanup()

    def test_resultado_no_finito_falla_controladamente(self):
        self.pipeline.return_value=sample(float('nan'))
        with self.assertRaises(RuntimeError):
            verification.capturar_verificacion(EMBEDDING)
        self.assert_cleanup()

    def test_error_camara_libera_recursos(self):
        self.camera.read.return_value=(False,None)
        with self.assertRaises(verification.ErrorCamara):
            verification.capturar_verificacion(EMBEDDING)
        self.assert_cleanup()

    def test_error_motor_no_se_oculta_como_ausencia_de_rostro(self):
        self.pipeline.side_effect=RuntimeError('Motor no disponible')
        with self.assertRaises(RuntimeError):
            verification.capturar_verificacion(EMBEDDING)
        self.assert_cleanup()

    def test_cancela_y_hace_timeout_incluso_con_motor_pendiente(self):
        self.executor.submit.side_effect=None
        self.executor.submit.return_value=Future()  # Nunca completa: preview sigue respondiendo.
        self.key.side_effect=[-1,ord('q')]
        self.assertEqual(verification.capturar_verificacion(EMBEDDING), {'cancelled':True})
        self.key.side_effect=None
        self.key.return_value=-1
        with patch.object(verification,'capture_timeout',return_value=1):
            with self.assertRaises(TimeoutError):
                verification.capturar_verificacion(EMBEDDING)
        self.assertEqual(self.camera.release.call_count,2)


class TestCaptureWorkerRouting(unittest.TestCase):
    def run_worker(self, request):
        with patch('sys.stdin',io.StringIO(json.dumps(request))):
            return capture_worker.main()

    def test_enroll_conserva_preparacion_y_embedding_base64(self):
        perfil={'embedding':EMBEDDING,'cantidad_muestras':6,'liveness_promedio':.95}
        with (patch.object(facial_enrollment,'preparar_perfil_preliminar',return_value=perfil) as enroll,
              patch.object(verification,'capturar_verificacion') as verify):
            response=self.run_worker({'operation':'enroll'})
        enroll.assert_called_once_with()
        verify.assert_not_called()
        self.assertEqual(base64.b64decode(response['embedding']),EMBEDDING)
        self.assertEqual(response['cantidad_muestras'],6)

    def test_enroll_cancelado_conserva_contrato(self):
        with patch.object(facial_enrollment,'preparar_perfil_preliminar',return_value=None):
            self.assertEqual(self.run_worker({'operation':'enroll'}),{'cancelled':True})

    def test_verify_no_invoca_enrollment_y_preserva_json(self):
        expected={'cancelled':False,'livenessPassed':True,'livenessScore':.95,'score':.91}
        with (patch.object(facial_enrollment,'preparar_perfil_preliminar') as enroll,
              patch.object(verification,'capturar_verificacion',return_value=expected) as verify):
            response=self.run_worker({'operation':'verify','embedding':base64.b64encode(EMBEDDING).decode()})
        enroll.assert_not_called()
        verify.assert_called_once_with(EMBEDDING)
        self.assertEqual(json.loads(json.dumps(response,allow_nan=False)),expected)

    def test_verify_cancelado_y_timeout_no_caen_en_enrollment(self):
        request={'operation':'verify','embedding':base64.b64encode(EMBEDDING).decode()}
        with (patch.object(facial_enrollment,'preparar_perfil_preliminar') as enroll,
              patch.object(verification,'capturar_verificacion',return_value={'cancelled':True}) as verify):
            self.assertEqual(self.run_worker(request),{'cancelled':True})
            verify.side_effect=TimeoutError()
            with self.assertRaises(TimeoutError):
                self.run_worker(request)
        enroll.assert_not_called()

    def test_worker_cancelado_no_espera_a_hilo_del_motor_y_emite_json(self):
        import subprocess
        import sys
        from pathlib import Path
        script = """
import sys, types, threading, runpy
fake = types.ModuleType('app.facial_verification')
def capture(embedding):
    threading.Thread(target=lambda: threading.Event().wait(30), daemon=False).start()
    return {'cancelled': True}
fake.capturar_verificacion = capture
sys.modules['app.facial_verification'] = fake
runpy.run_module('app.capture_worker', run_name='__main__')
"""
        result = subprocess.run([sys.executable,'-c',script],
            cwd=Path(__file__).resolve().parents[1],
            input=json.dumps({'operation':'verify','embedding':base64.b64encode(EMBEDDING).decode()}),
            text=True,capture_output=True,timeout=5,check=True)
        self.assertEqual(json.loads(result.stdout), {'cancelled':True})

    def test_operacion_invalida_no_abre_captura(self):
        with (patch.object(facial_enrollment,'preparar_perfil_preliminar') as enroll,
              patch.object(verification,'capturar_verificacion') as verify):
            with self.assertRaises(ValueError):
                self.run_worker({'operation':'otra'})
        enroll.assert_not_called()
        verify.assert_not_called()


class TestVerificationQuality(unittest.TestCase):
    def test_calidad_insuficiente_se_rechaza_en_motor_reutilizado(self):
        from types import SimpleNamespace
        face=SimpleNamespace(liveness=SimpleNamespace(status='ok',is_live=True,live_score=.95),
                             det_score=.4)
        analyzer=MagicMock()
        analyzer.get.return_value=[face]
        with (patch.object(facial_pipeline,'_obtener_analizador',return_value=analyzer),
              patch.object(facial_pipeline,'calcular_similitud') as compare):
            with self.assertRaises(ValueError):
                facial_pipeline.analizar_captura_facial(np.zeros((480,640,3),dtype=np.uint8),
                                                        EMBEDDING,exigir_calidad=True)
        compare.assert_not_called()

    def test_calidad_posicion_brillo_y_nitidez_se_pueden_corregir(self):
        from types import SimpleNamespace
        face=SimpleNamespace(liveness=SimpleNamespace(status='ok',is_live=True,live_score=.95),
                             det_score=.95,bbox=[100,100,300,300],
                             embedding=np.frombuffer(EMBEDDING,dtype='<f4'))
        analyzer=MagicMock()
        analyzer.get.return_value=[face]
        image=np.full((480,640,3),120,dtype=np.uint8)
        with (patch.object(facial_pipeline,'_obtener_analizador',return_value=analyzer),
              patch.object(facial_pipeline,'calcular_similitud',return_value=.9)):
            # Imagen uniforme: no supera nitidez, aunque haya liveness positivo.
            with self.assertRaises(ValueError):
                facial_pipeline.analizar_captura_facial(image,EMBEDDING,exigir_calidad=True)
            with patch('cv2.Laplacian',return_value=MagicMock(var=lambda:100.)):
                result=facial_pipeline.analizar_captura_facial(image,EMBEDDING,exigir_calidad=True)
                self.assertEqual(result['similarity'],.9)
                face.bbox=[100,100,110,110]
                with self.assertRaises(ValueError):
                    facial_pipeline.analizar_captura_facial(image,EMBEDDING,exigir_calidad=True)
                face.bbox=[100,100,300,300]
                with self.assertRaises(ValueError):
                    facial_pipeline.analizar_captura_facial(np.zeros_like(image),EMBEDDING,exigir_calidad=True)

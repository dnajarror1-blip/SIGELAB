import os
import subprocess
import unittest
from unittest.mock import patch
from app.config import facial_threshold, get_database_config
from app.capture_client import ejecutar_captura


class TestConfiguracionOperativa(unittest.TestCase):
    def test_threshold_es_explicito_finito_y_positivo(self):
        for valor in ('', 'nan', 'inf', '0', '-1', '1.1'):
            with patch.dict(os.environ, {'FACIAL_MATCH_THRESHOLD':valor}):
                with self.assertRaises(RuntimeError):
                    facial_threshold()
        with patch.dict(os.environ, {'FACIAL_MATCH_THRESHOLD':'.6'}):
            self.assertEqual(facial_threshold(),.6)

    def test_base_predeterminada_y_driver_configurables(self):
        with patch.dict(os.environ, {'DB_USER':'prueba','DB_PASSWORD':'solo-fixture',
                                    'DB_DRIVER':'driver-fixture'}, clear=True):
            config = get_database_config()
            self.assertEqual(config['database'],'SIGELAB')
            self.assertEqual(config['driver'],'driver-fixture')

    def test_timeout_proceso_se_controla(self):
        with patch('app.capture_client.subprocess.run', side_effect=subprocess.TimeoutExpired('worker',1)):
            with self.assertRaises(RuntimeError):
                ejecutar_captura('verify',b'simulado')

    def test_embedding_viaja_en_pipe_no_en_argumentos(self):
        result = subprocess.CompletedProcess([],0,stdout='{"cancelled":true}',stderr='')
        with (patch('app.capture_client.capture_timeout',return_value=120),
              patch('app.capture_client.subprocess.run',return_value=result) as run):
            self.assertTrue(ejecutar_captura('verify',b'simulado')['cancelled'])
            self.assertNotIn('embedding',' '.join(run.call_args.args[0]))
            self.assertIn('embedding',run.call_args.kwargs['input'])

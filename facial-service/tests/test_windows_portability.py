"""Diagnóstico/configuración portables, sin SQL, cámara ni claves reales."""
from contextlib import redirect_stderr, redirect_stdout
import io
import os
from pathlib import Path
import stat
import sys
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import Mock, patch

from scripts import configure_local, doctor


class PortableConfigurationTest(unittest.TestCase):
    def test_posix_private_mode_and_atomic_failure_preserve_original(self):
        if os.name == 'nt':
            self.skipTest('Permisos POSIX solo en POSIX')
        with tempfile.TemporaryDirectory(prefix='sigelab with spaces ') as folder:
            path = Path(folder) / '.env'
            configure_local.save_env(path, {'DB_PASSWORD': 'fixture only'})
            self.assertEqual(stat.S_IMODE(path.stat().st_mode), 0o600)
            original = path.read_bytes()
            with patch.object(configure_local.os, 'replace', side_effect=OSError('fixture')):
                with self.assertRaises(OSError):
                    configure_local.save_env(path, {'DB_PASSWORD': 'changed fixture'})
            self.assertEqual(path.read_bytes(), original)
            self.assertEqual(list(Path(folder).iterdir()), [path])

    def test_windows_does_not_claim_or_apply_posix_permissions(self):
        with tempfile.TemporaryDirectory(prefix='sigelab with spaces ') as folder:
            path = Path(folder) / '.env'
            fake_os = SimpleNamespace(name='nt', fdopen=os.fdopen, replace=os.replace,
                                      path=os.path, unlink=os.unlink, chmod=Mock())
            with patch.object(configure_local, 'os', fake_os):
                configure_local.save_env(path, {'DB_PASSWORD': 'fixture only'})
            fake_os.chmod.assert_not_called()
            self.assertEqual(configure_local.read_env(path)['DB_PASSWORD'], 'fixture only')


class PortableDoctorTest(unittest.TestCase):
    def run_camera(self, read_result):
        camera = Mock()
        camera.getBackendName.return_value = 'DSHOW'
        camera.read.return_value = read_result
        output, errors = io.StringIO(), io.StringIO()
        with (patch.object(sys, 'argv', ['doctor.py', '--camera']),
              patch.object(doctor, 'dependencies'), patch.object(doctor, 'configuration'),
              patch.object(doctor.platform, 'system', return_value='Windows'),
              patch.dict(sys.modules, {'app.camara': SimpleNamespace(abrir_camara=lambda: camera)}),
              redirect_stdout(output), redirect_stderr(errors)):
            code = doctor.main()
        camera.release.assert_called_once()
        return code, output.getvalue(), errors.getvalue()

    def test_reports_backend_and_releases_camera(self):
        code, output, errors = self.run_camera((True, object()))
        self.assertEqual(code, 0)
        self.assertIn('DSHOW', output)
        self.assertEqual(errors, '')

    def test_missing_frames_gives_windows_guidance_and_releases_camera(self):
        code, _, errors = self.run_camera((False, None))
        self.assertEqual(code, 1)
        self.assertIn('Teams/Zoom/Camera', errors)
        self.assertIn('aplicaciones de escritorio', errors)
        self.assertIn('CAP_ANY', errors)

import struct
import unittest
from app.facial_matching import calcular_similitud


class TestSimilitudOperativa(unittest.TestCase):
    def test_identico_opuesto_y_ortogonal(self):
        a = struct.pack('<512f', 1., *([0.] * 511))
        opuesto = struct.pack('<512f', -1., *([0.] * 511))
        ortogonal = struct.pack('<512f', 0., 1., *([0.] * 510))
        self.assertAlmostEqual(calcular_similitud(a,a),1.)
        self.assertAlmostEqual(calcular_similitud(a,opuesto),-1.)
        self.assertAlmostEqual(calcular_similitud(a,ortogonal),0.)

    def test_no_acepta_vectores_vacios_o_no_finitos(self):
        a = struct.pack('<512f', *([1.] * 512))
        for invalido in (b'', struct.pack('<512f', *([0.] * 512)),
                         struct.pack('<512f', float('nan'), *([0.] * 511))):
            with self.assertRaises(ValueError):
                calcular_similitud(a,invalido)

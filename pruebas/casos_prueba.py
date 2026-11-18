"""Ejecuta la secuencia de casosPrueba.pdf contra el servidor real."""
from pathlib import Path
import socket
import subprocess
import time
import uuid

RAIZ = Path(__file__).resolve().parents[1]
TEMPORAL = RAIZ / 'tmp' / ('casos-prueba-' + uuid.uuid4().hex)
TEMPORAL.mkdir(parents=True)
JAR = RAIZ / 'conecta2-servidor/Conecta2-Servidor.jar'


class Cliente:
    def __init__(self, puerto, alias):
        self.socket = socket.create_connection(('127.0.0.1', puerto), timeout=3)
        self.buffer = b''
        self.eventos = []
        self.enviar('INGRESAR\t' + alias)
        self.esperar('LISTO')

    def enviar(self, linea):
        self.socket.sendall((linea + '\n').encode('utf-8'))

    def recibir(self, espera=3):
        self.socket.settimeout(espera)
        while b'\n' not in self.buffer:
            bloque = self.socket.recv(65536)
            if not bloque:
                raise EOFError('Conexión cerrada')
            self.buffer += bloque
        linea, self.buffer = self.buffer.split(b'\n', 1)
        campos = linea.rstrip(b'\r').decode('utf-8').split('\t', 6)
        self.eventos.append(campos)
        return campos

    def esperar(self, orden, condicion=lambda campos: True):
        limite = time.monotonic() + 4
        while time.monotonic() < limite:
            campos = self.recibir(limite - time.monotonic())
            if campos[0] == orden and condicion(campos):
                return campos
        raise AssertionError('No llegó ' + orden)

    def recoger(self):
        try:
            while True:
                self.recibir(.15)
        except (socket.timeout, EOFError):
            pass

    def mensajes(self):
        return [p for p in self.eventos if p[0] == 'MENSAJE']

    def cerrar(self):
        self.socket.close()


def puerto_libre():
    with socket.socket() as reserva:
        reserva.bind(('127.0.0.1', 0))
        return reserva.getsockname()[1]


def iniciar(puerto):
    log = (TEMPORAL / ('servidor-' + uuid.uuid4().hex + '.log')).open('w', encoding='utf-8')
    proceso = subprocess.Popen(['java', '-jar', str(JAR), str(puerto), str(TEMPORAL / 'datos')],
                               stdout=log, stderr=log)
    for _ in range(80):
        try:
            with socket.create_connection(('127.0.0.1', puerto), timeout=.2):
                return proceso, log
        except OSError:
            if proceso.poll() is not None:
                raise AssertionError('El servidor no inició')
            time.sleep(.05)
    proceso.terminate()
    raise AssertionError('El servidor no responde')


def desconectado(campos, alias):
    # CLIENTES contiene pares alias/presencia y puede superar siete columnas.
    valores = '\t'.join(campos).split('\t')
    return any(valores[i] == alias and valores[i + 1] == 'false'
               for i in range(1, len(valores) - 1, 2))


def leer(cliente, mensaje):
    cliente.enviar('LEER\t' + mensaje[1])
    cliente.esperar('LEIDO', lambda p: p[1] == mensaje[1])


def main():
    puerto = puerto_libre()
    proceso, log = iniciar(puerto)
    clientes = []
    try:
        jose, raul, pedro = [Cliente(puerto, alias) for alias in ('JOSE', 'RAUL', 'PEDRO')]
        clientes += [jose, raul, pedro]
        assert not any(c.mensajes() for c in clientes)
        print('OK 1-2: ingreso de tres clientes con historial vacío')

        jose.enviar('ENVIAR\t\tHola')
        jose.esperar('ENVIADO')
        hola_raul = raul.esperar('MENSAJE')
        hola_pedro = pedro.esperar('MENSAJE')
        assert hola_raul[3:7] == ['jose', 'raul', 'false', 'Hola']
        assert hola_pedro[3:7] == ['jose', 'pedro', 'false', 'Hola']
        assert len(jose.mensajes()) == 2
        print('OK 3: JOSE envía a todos y cada destinatario conserva su pendiente')

        pedro.enviar('ENVIAR\t\tSaludos')
        pedro.esperar('ENVIADO')
        saludo_jose = jose.esperar('MENSAJE', lambda p: p[3] == 'pedro')
        saludo_raul = raul.esperar('MENSAJE')
        assert saludo_raul[6] == 'Saludos'
        assert {p[3] for p in raul.mensajes()} == {'jose', 'pedro'}
        print('OK 4: PEDRO envía a todos sin seleccionar un contacto')

        jose.enviar('ABRIR\tpedro')
        leer(jose, saludo_jose)
        jose.enviar('ENVIAR\tpedro\tReunión')
        jose.esperar('ENVIADO')
        reunion = pedro.esperar('MENSAJE', lambda p: p[6] == 'Reunión')
        raul.recoger()
        assert all(p[6] != 'Reunión' for p in raul.mensajes())
        print('OK 5: conversación privada y separación de los otros clientes')

        pedro.enviar('ABRIR\tjose')
        leer(pedro, hola_pedro)
        leer(pedro, reunion)
        pedro.enviar('ENVIAR\tjose\tCuándo')
        pedro.esperar('ENVIADO')
        respuesta = jose.esperar('MENSAJE', lambda p: p[6] == 'Cuándo')
        leer(jose, respuesta)
        print('OK 6: respuesta privada y lectura del historial anterior')

        anteriores = len(jose.mensajes())
        raul.enviar('bye')
        jose.esperar('CLIENTES', lambda p: desconectado(p, 'raul'))
        assert len(jose.mensajes()) == anteriores
        pedro.recoger()
        assert not any('raul salió' in p[6] for p in pedro.mensajes())
        print('OK 7: RAUL sale sin generar avisos de conversación')

        pedro.enviar('bye')
        aviso = jose.esperar('MENSAJE', lambda p: p[6] == 'pedro salió del chat.')
        jose.esperar('CLIENTES', lambda p: desconectado(p, 'pedro'))
        leer(jose, aviso)
        print('OK 8: la salida de PEDRO se avisa solo a su contacto activo')

        for texto in ('uno uno', 'dos dos'):
            jose.enviar('ENVIAR\tpedro\t' + texto)
            jose.esperar('ENVIADO')
        print('OK 9: se guardan los mensajes enviados a PEDRO desconectado')

        pedro = Cliente(puerto, 'PEDRO')
        clientes.append(pedro)
        recibidos = [p for p in pedro.mensajes() if p[4] == 'pedro']
        assert [p[6] for p in recibidos] == ['Hola', 'Reunión', 'uno uno', 'dos dos']
        assert [p[5] for p in recibidos] == ['true', 'true', 'false', 'false']
        pedro.enviar('ABRIR\tjose')
        for mensaje in recibidos[-2:]:
            leer(pedro, mensaje)
        print('OK 10: al volver, PEDRO recupera dos no leídos y sus mensajes anteriores')

        # Volver a Todos limpia el contacto activo, incluso si se cierra inmediatamente.
        jose.recoger()
        anteriores = len(jose.mensajes())
        pedro.enviar('ABRIR\t')
        pedro.enviar('SALIR')
        jose.esperar('CLIENTES', lambda p: desconectado(p, 'pedro'))
        assert len(jose.mensajes()) == anteriores
        jose.enviar('ENVIAR\t\tSin destinatarios')
        jose.esperar('ERROR')
        jose.enviar('ENVIAR\tjose\tAutoenvío')
        jose.esperar('ERROR')
        print('OK: volver a Todos, salida ordenada y validaciones de destino')
    finally:
        for cliente in clientes:
            cliente.cerrar()
        proceso.terminate()
        proceso.wait(timeout=5)
        log.close()

    proceso, log = iniciar(puerto)
    try:
        pedro = Cliente(puerto, 'PEDRO')
        try:
            recibidos = [p for p in pedro.mensajes() if p[4] == 'pedro']
            assert [p[6] for p in recibidos] == ['Hola', 'Reunión', 'uno uno', 'dos dos']
            assert all(p[5] == 'true' for p in recibidos)
        finally:
            pedro.cerrar()
        print('OK: historial y lecturas conservados al reiniciar el servidor')
    finally:
        proceso.terminate()
        proceso.wait(timeout=5)
        log.close()


if __name__ == '__main__':
    main()

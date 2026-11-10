//
//  ViewModelsTests.swift
//  appcmoTests
//
//  Pruebas de los ViewModels de login, registro y solicitud de atención (HU-84).
//  Corren con: xcodebuild test -scheme appcmo -destination 'platform=iOS Simulator,name=iPhone 17'
//

import XCTest
@testable import appcmo

@MainActor
class PruebaConAPIFalsa: XCTestCase {
    /// Keychain propio de las pruebas: no toca la sesión real de la app en el simulador.
    let keychain = KeychainManager(servicio: "appcmoTests")
    var cliente: NetworkClient!
    var store: SessionStore!

    override func setUp() async throws {
        APIFalsa.reiniciar()
        keychain.borrarToken()
        cliente = APIFalsa.cliente(keychain: keychain)
        store = SessionStore(auth: AuthRepository(cliente: cliente), keychain: keychain)
    }

    override func tearDown() async throws {
        keychain.borrarToken()
    }
}

final class LoginViewModelTests: PruebaConAPIFalsa {
    private func login(_ usuario: String = "medico.general", _ password: String = "clave") -> LoginViewModel {
        let vm = LoginViewModel(store: store)
        vm.usuario = "  \(usuario) "
        vm.password = password
        return vm
    }

    func testSinDatosNoSePuedeEnviar() {
        let vm = LoginViewModel(store: store)
        XCTAssertFalse(vm.puedeEnviar)
        vm.usuario = "   "
        vm.password = "x"
        XCTAssertFalse(vm.puedeEnviar)
    }

    func testMedicoEntraYElTokenQuedaEnElKeychain() async {
        APIFalsa.responder("POST auth/login", 200, JSONPrueba.login(rol: "MEDICO", token: "t-medico"))
        let vm = login()

        await vm.enviar()

        XCTAssertEqual(APIFalsa.peticion("POST auth/login")?.cuerpo["usuario"] as? String, "medico.general", "el usuario se envía sin espacios")
        XCTAssertEqual(store.sesion?.usuario.rol, .medico)
        XCTAssertEqual(keychain.leerToken(), "t-medico")
        XCTAssertEqual(vm.password, "")
        XCTAssertNil(vm.aviso)
        XCTAssertFalse(vm.estaCargando)
    }

    func testCredencialesIncorrectasMuestranElMensajeYNoGuardanSesion() async {
        APIFalsa.responder("POST auth/login", 401, #"{"success":false,"message":"Credenciales incorrectas."}"#)
        let vm = login()

        await vm.enviar()

        XCTAssertEqual(vm.aviso, .error("Credenciales incorrectas."))
        XCTAssertEqual(vm.password, "clave", "no se pierde lo escrito")
        XCTAssertNil(store.sesion)
        XCTAssertNil(keychain.leerToken())
    }

    func testRecepcionRecibeCredencialesIncorrectasYSuTokenSeRevoca() async {
        APIFalsa.responder("POST auth/login", 200, JSONPrueba.login(rol: "RECEPCIONISTA", token: "t-recepcion"))
        APIFalsa.responder("POST auth/logout", 200, JSONPrueba.ok)
        let vm = login("recepcion")

        await vm.enviar()

        XCTAssertEqual(vm.aviso, .error("Credenciales incorrectas."))
        XCTAssertNil(store.sesion)
        XCTAssertNil(keychain.leerToken(), "el token de otro rol nunca se guarda")
        XCTAssertEqual(APIFalsa.peticion("POST auth/logout")?.token, "t-recepcion")
    }

    func testDemasiadosIntentosBloqueanElBoton() async {
        APIFalsa.responder("POST auth/login", 429, #"{"success":false,"message":"Demasiados intentos."}"#, cabeceras: ["Retry-After": "60"])
        let vm = login()

        await vm.enviar()

        XCTAssertTrue(vm.enEspera)
        XCTAssertFalse(vm.puedeEnviar)
        if case .error = vm.aviso {} else { XCTFail("se esperaba un aviso de error") }
    }

    func testErroresDeValidacionVanJuntoAlCampo() async {
        APIFalsa.responder("POST auth/login", 422, JSONPrueba.validacion(campos: ["usuario": "El usuario es obligatorio."]))
        let vm = login()

        await vm.enviar()

        XCTAssertEqual(vm.errorUsuario, "El usuario es obligatorio.")
        XCTAssertNil(vm.errorPassword)
        XCTAssertNil(vm.aviso)
    }
}

final class RegistroViewModelTests: PruebaConAPIFalsa {
    private func registroValido() -> RegistroViewModel {
        let vm = RegistroViewModel(store: store)
        vm.nombreCompleto = "Ana Pérez"
        vm.usuario = "ana.perez"
        vm.dui = "01234567-8"
        vm.password = "Segura2026"
        vm.confirmacion = "Segura2026"
        return vm
    }

    func testValidaAntesDeEnviar() async {
        let vm = registroValido()
        vm.password = "corta"
        vm.confirmacion = "distinta"
        vm.codigoActivacion = "12a"

        await vm.registrar()

        XCTAssertNotNil(vm.errores[.password])
        XCTAssertNotNil(vm.errores[.confirmacion])
        XCTAssertNotNil(vm.errores[.codigoActivacion])
        XCTAssertTrue(APIFalsa.peticiones.isEmpty, "con errores locales no se llama a la API")
    }

    func testCuentaNuevaQuedaConSesionIniciada() async {
        APIFalsa.responder("POST auth/registro", 201, JSONPrueba.login(rol: "PACIENTE", token: "t-paciente", idPaciente: "p-1"))
        let vm = registroValido()
        vm.telefono = "   "

        await vm.registrar()

        let cuerpo = APIFalsa.peticion("POST auth/registro")?.cuerpo
        XCTAssertEqual(cuerpo?["password_confirmation"] as? String, "Segura2026")
        XCTAssertNil(cuerpo?["telefono"], "los campos opcionales vacíos no se envían")
        XCTAssertEqual(store.sesion?.idPaciente, "p-1")
        XCTAssertEqual(keychain.leerToken(), "t-paciente")
        XCTAssertTrue(vm.errores.isEmpty)
    }

    func testDUIConExpedienteSinCodigoMuestraElErrorEnElCodigo() async {
        let mensaje = "Este DUI ya tiene expediente. Pide tu código de activación en recepción."
        APIFalsa.responder("POST auth/registro", 422, JSONPrueba.validacion(mensaje, campos: ["codigo_activacion": mensaje]))
        let vm = registroValido()

        await vm.registrar()

        XCTAssertEqual(vm.errores[.codigoActivacion], mensaje)
        XCTAssertEqual(vm.aviso, .error(mensaje))
        XCTAssertNil(store.sesion)
        XCTAssertFalse(vm.enviando)
    }
}

final class SolicitudAtencionViewModelTests: PruebaConAPIFalsa {
    private var vm: SolicitudAtencionViewModel!

    override func setUp() async throws {
        try await super.setUp()
        vm = SolicitudAtencionViewModel(repositorio: CitasRepository(cliente: cliente))
    }

    func testSinMotivoNoSePuedeEnviar() {
        vm.motivo = "  \n "
        XCTAssertFalse(vm.puedeEnviar)
    }

    func testElMotivoSeRecortaAlLimite() {
        vm.motivo = String(repeating: "a", count: SolicitudAtencionViewModel.limite + 50)
        XCTAssertEqual(vm.motivo.count, SolicitudAtencionViewModel.limite)
    }

    func testSolicitudEnviadaQuedaPendiente() async {
        APIFalsa.responder("POST solicitudes-atencion", 201, """
        {"success":true,"message":"Solicitud creada.","data":{"id_cita":"c-1","id_paciente":"p-1","motivo":"Dolor de garganta","estado":"SOLICITADA"}}
        """)
        vm.motivo = "  Dolor de garganta  "

        let enviada = await vm.enviar()

        XCTAssertTrue(enviada)
        XCTAssertEqual(APIFalsa.peticion("POST solicitudes-atencion")?.cuerpo["motivo"] as? String, "Dolor de garganta")
        XCTAssertEqual(vm.motivo, "")
        if case .exito = vm.aviso {} else { XCTFail("se esperaba un aviso de éxito") }
        XCTAssertFalse(vm.enviando)
    }

    func testErrorDeLaAPISeMuestraYConservaElMotivo() async {
        APIFalsa.responder("POST solicitudes-atencion", 422, JSONPrueba.validacion("El motivo es obligatorio.", campos: ["motivo": "El motivo es obligatorio."]))
        vm.motivo = "Fiebre"

        let enviada = await vm.enviar()

        XCTAssertFalse(enviada)
        XCTAssertEqual(vm.aviso, .error("El motivo es obligatorio."))
        XCTAssertEqual(vm.motivo, "Fiebre")
    }
}

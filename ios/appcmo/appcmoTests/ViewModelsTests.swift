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

    func testSinMotivoNoSePuedeEnviarPeroFechasEsOpcional() {
        vm.motivo = "  \n "
        vm.rangoFechas = "Mañana a las 10"
        XCTAssertFalse(vm.puedeEnviar, "sin motivo válido no se puede enviar")

        vm.motivo = "Fiebre y dolor"
        vm.rangoFechas = ""
        XCTAssertTrue(vm.puedeEnviar, "el rango de fechas es opcional, con solo motivo debe poderse enviar")
    }

    func testElMotivoYFechasSeRecortanAlLimite() {
        vm.motivo = String(repeating: "a", count: SolicitudAtencionViewModel.limite + 50)
        XCTAssertEqual(vm.motivo.count, SolicitudAtencionViewModel.limite)

        vm.rangoFechas = String(repeating: "b", count: SolicitudAtencionViewModel.limiteFechas + 20)
        XCTAssertEqual(vm.rangoFechas.count, SolicitudAtencionViewModel.limiteFechas)
    }

    func testTextoConcatenadoFormateaDisponibilidadYMotivo() {
        vm.motivo = "Dolor de espalda"
        vm.rangoFechas = ""
        XCTAssertEqual(vm.textoConcatenado, "Dolor de espalda")

        vm.rangoFechas = "Del 18 al 22 de noviembre"
        XCTAssertEqual(vm.textoConcatenado, "[Disponibilidad: Del 18 al 22 de noviembre] Dolor de espalda")
    }

    func testSolicitudEnviadaConDisponibilidadQuedaPendiente() async {
        APIFalsa.responder("POST solicitudes-atencion", 201, """
        {"success":true,"message":"Solicitud creada.","data":{"id_cita":"c-1","id_paciente":"p-1","motivo":"[Disponibilidad: Tardes] Dolor de muelas","estado":"SOLICITADA"}}
        """)
        vm.rangoFechas = "  Tardes  "
        vm.motivo = "  Dolor de muelas  "

        let enviada = await vm.enviar()

        XCTAssertTrue(enviada)
        XCTAssertEqual(APIFalsa.peticion("POST solicitudes-atencion")?.cuerpo["motivo"] as? String, "[Disponibilidad: Tardes] Dolor de muelas")
        XCTAssertEqual(vm.motivo, "")
        XCTAssertEqual(vm.rangoFechas, "")
        if case .exito = vm.aviso {} else { XCTFail("se esperaba un aviso de éxito") }
        XCTAssertFalse(vm.enviando)
    }

    func testSolicitudEnviadaSinFechasEnviaSoloMotivo() async {
        APIFalsa.responder("POST solicitudes-atencion", 201, """
        {"success":true,"message":"Solicitud creada.","data":{"id_cita":"c-1","id_paciente":"p-1","motivo":"Dolor de garganta","estado":"SOLICITADA"}}
        """)
        vm.rangoFechas = ""
        vm.motivo = "  Dolor de garganta  "

        let enviada = await vm.enviar()

        XCTAssertTrue(enviada)
        XCTAssertEqual(APIFalsa.peticion("POST solicitudes-atencion")?.cuerpo["motivo"] as? String, "Dolor de garganta")
        XCTAssertEqual(vm.motivo, "")
        XCTAssertEqual(vm.rangoFechas, "")
        XCTAssertFalse(vm.enviando)
    }

    func testErrorDeLaAPISeMuestraYConservaCampos() async {
        APIFalsa.responder("POST solicitudes-atencion", 422, JSONPrueba.validacion("El motivo es obligatorio.", campos: ["motivo": "El motivo es obligatorio."]))
        vm.rangoFechas = "Próxima semana"
        vm.motivo = "Fiebre"

        let enviada = await vm.enviar()

        XCTAssertFalse(enviada)
        XCTAssertEqual(vm.aviso, .error("El motivo es obligatorio."))
        XCTAssertEqual(vm.motivo, "Fiebre")
        XCTAssertEqual(vm.rangoFechas, "Próxima semana")
    }
}

final class VincularExpedienteViewModelTests: PruebaConAPIFalsa {
    private var vm: VincularExpedienteViewModel!

    override func setUp() async throws {
        try await super.setUp()
        try? keychain.guardarToken("token-test")
        APIFalsa.responder("GET auth/me", 200, """
        {"success":true,"data":{"usuario":{"id":"u-1","nombre_completo":"Paciente Prueba","usuario":"paciente.prueba","email":"paciente@correo.com","rol":"PACIENTE","cargo":null,"estado":"ACTIVO"},"id_paciente":null}}
        """)
        await store.restaurarSesion()
        let repo = PacientesRepository(cliente: cliente)
        vm = VincularExpedienteViewModel(repositorio: repo, store: store)
    }

    func testSinDuiNoSePuedeSolicitarOtp() async {
        vm.dui = "   "
        XCTAssertFalse(vm.puedeSolicitarOtp)
        let ok = await vm.solicitarOtp()
        XCTAssertFalse(ok)
        XCTAssertNotNil(vm.errorCampo)
    }

    func testSolicitarOtpExitosoAvanzaAVerificarOtp() async {
        APIFalsa.responder("POST mi/expediente/solicitar-otp", 200, """
        {"success":true,"message":"Código de verificación enviado.","data":{"email_enmascarado":"p***a@correo.com","vence_en_minutos":15}}
        """)
        vm.dui = "01234567-8"
        XCTAssertTrue(vm.puedeSolicitarOtp)

        let ok = await vm.solicitarOtp()

        XCTAssertTrue(ok)
        if case .verificarOtp = vm.paso {
            XCTAssertEqual(vm.emailEnmascarado, "p***a@correo.com")
        } else {
            XCTFail("Se esperaba avanzar al paso de verificar OTP")
        }
    }

    func testConfirmarOtpValidaSeisDigitos() async {
        vm.otp = "123"
        XCTAssertFalse(vm.puedeConfirmarOtp)
        let ok = await vm.confirmarOtp()
        XCTAssertFalse(ok)
        XCTAssertNotNil(vm.errorCampo)
    }

    func testConfirmarOtpExitosoActualizaIdPacienteYMarcaExito() async {
        APIFalsa.responder("POST mi/expediente/confirmar-otp", 200, """
        {"success":true,"message":"Expediente vinculado exitosamente.","data":{"id_paciente":"p-100","numero_expediente":"EXP-2026-000501","nombre_completo":"Paciente Prueba"}}
        """)
        vm.dui = "01234567-8"
        vm.otp = "123456"
        XCTAssertTrue(vm.puedeConfirmarOtp)

        let ok = await vm.confirmarOtp()

        XCTAssertTrue(ok)
        XCTAssertEqual(store.sesion?.idPaciente, "p-100")
        if case .exito(let numero) = vm.paso {
            XCTAssertEqual(numero, "EXP-2026-000501")
        } else {
            XCTFail("Se esperaba estado de éxito")
        }
    }

    func testOtpInvalidoMuestraMensajeDeError() async {
        APIFalsa.responder("POST mi/expediente/confirmar-otp", 422, JSONPrueba.validacion("El código es incorrecto.", campos: ["otp": "El código es incorrecto."]))
        vm.dui = "01234567-8"
        vm.otp = "999999"

        let ok = await vm.confirmarOtp()

        XCTAssertFalse(ok)
        XCTAssertEqual(vm.errorCampo, "El código es incorrecto.")
        XCTAssertNil(store.sesion?.idPaciente)
    }
}

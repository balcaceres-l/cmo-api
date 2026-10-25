//
//  PerfilMedicoView.swift
//  appcmo
//

import SwiftUI

/// Perfil del médico: sus datos, accesos a Mis turnos y Laboratorio, cambio de contraseña y cierre de sesión.
struct PerfilMedicoView: View {
    private enum Destino: Hashable {
        case turnos, laboratorio, ingresados, ambulancia, password
    }

    @Environment(SessionStore.self) private var store
    @State private var confirmando = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    EncabezadoPantalla("Perfil") { CampanaAvisos() }

                    if let usuario = store.sesion?.usuario {
                        VStack(spacing: 10) {
                            AvatarIniciales(nombre: usuario.nombreCompleto, tono: .neutro, tamano: 84)
                            Text(usuario.nombreCompleto)
                                .font(.title3.bold())
                                .foregroundStyle(Color.cmoTinta)
                                .accessibilityIdentifier("perfil.nombre")
                            EstadoChip(texto: usuario.rol.nombre, tono: .neutro)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)

                        VStack(spacing: 0) {
                            dato("Usuario", usuario.usuario)
                            Divider()
                            dato("Correo", usuario.email)
                            if let cargo = usuario.cargo {
                                Divider()
                                dato("Cargo", cargo)
                            }
                        }
                        .tarjeta(relleno: 0)
                    }

                    Text("Herramientas")
                        .font(.headline)
                        .foregroundStyle(Color.cmoTinta)
                        .padding(.top, 4)
                    VStack(spacing: 0) {
                        acceso("clock", "Mis turnos", .turnos, id: "perfil.turnos")
                        Divider().padding(.leading, 60)
                        acceso("testtube.2", "Laboratorio", .laboratorio, id: "perfil.laboratorio")
                        Divider().padding(.leading, 60)
                        acceso("bed.double", "Pacientes ingresados", .ingresados, id: "perfil.ingresados")
                        Divider().padding(.leading, 60)
                        acceso("cross.case", "Ambulancia", .ambulancia, id: "perfil.ambulancia")
                    }
                    .tarjeta(relleno: 0)

                    Text("Seguridad")
                        .font(.headline)
                        .foregroundStyle(Color.cmoTinta)
                        .padding(.top, 4)
                    VStack(spacing: 0) {
                        acceso("lock", "Cambiar contraseña", .password, id: "perfil.cambiarPassword")
                    }
                    .tarjeta(relleno: 0)

                    Button {
                        confirmando = true
                    } label: {
                        Label("Cerrar sesión", systemImage: "rectangle.portrait.and.arrow.right")
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity, minHeight: 48)
                            .foregroundStyle(Color.cmoError)
                            .background(Color.cmoErrorSuave, in: .rect(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                    .padding(.top, 6)
                    .accessibilityIdentifier("cuenta.cerrarSesion")
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 24)
            }
            .background(Color.cmoFondo)
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: Destino.self) { destino in
                switch destino {
                case .turnos: TurnosView()
                case .laboratorio: LaboratorioView()
                case .ingresados: IngresadosView()
                case .ambulancia: AmbulanciaView()
                case .password: CambiarPasswordView(store: store)
                }
            }
            .confirmationDialog("¿Cerrar sesión?", isPresented: $confirmando, titleVisibility: .visible) {
                Button("Cerrar sesión", role: .destructive) {
                    Task { await store.cerrarSesion() }
                }
                Button("Cancelar", role: .cancel) {}
            }
        }
    }

    private func dato(_ titulo: String, _ valor: String?) -> some View {
        HStack(alignment: .top) {
            Text(titulo).foregroundStyle(Color.cmoTintaSuave)
            Spacer(minLength: 16)
            Text(valor ?? "—").foregroundStyle(Color.cmoTinta).multilineTextAlignment(.trailing)
        }
        .font(.subheadline)
        .padding(14)
    }

    private func acceso(_ sistema: String, _ titulo: String, _ destino: Destino, id: String) -> some View {
        NavigationLink(value: destino) {
            HStack(spacing: 12) {
                IconoCirculo(sistema: sistema, tono: .neutro, tamano: 36)
                Text(titulo)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color.cmoTinta)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Color.cmoTintaSuave)
            }
            .padding(12)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(id)
    }
}

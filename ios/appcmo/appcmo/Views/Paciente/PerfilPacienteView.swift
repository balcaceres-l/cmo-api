//
//  PerfilPacienteView.swift
//  appcmo
//

import SwiftUI

/// Perfil del paciente (pantalla 10): sus datos, cambio de contraseña y cierre de sesión (HU-23).
struct PerfilPacienteView: View {
    @Environment(SessionStore.self) private var store
    @Environment(PacienteDatos.self) private var datos
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
                            FilaDato("Usuario", usuario.usuario)
                            Divider()
                            FilaDato("Correo", usuario.email)
                            Divider()
                            FilaDato("DUI", datos.expediente.valor?.dui)
                            Divider()
                            FilaDato("Expediente", datos.expediente.valor?.numeroExpediente)
                        }
                        .tarjeta(relleno: 0)
                    }

                    NavigationLink {
                        CambiarPasswordView(store: store)
                    } label: {
                        HStack(spacing: 12) {
                            IconoCirculo(sistema: "lock", tono: .info, tamano: 40)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Cambiar contraseña").font(.subheadline.weight(.semibold)).foregroundStyle(Color.cmoTinta)
                                Text("Se cerrará tu sesión en todos tus dispositivos").font(.caption).foregroundStyle(Color.cmoTintaSuave)
                            }
                            Spacer(minLength: 8)
                            Image(systemName: "chevron.right")
                                .font(.footnote.weight(.semibold))
                                .foregroundStyle(Color.cmoTintaSuave)
                        }
                        .tarjeta(relleno: 12)
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("perfil.cambiarPassword")

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
            .onAppear { Task { await datos.cargarExpediente() } }
            .confirmationDialog("¿Cerrar sesión?", isPresented: $confirmando, titleVisibility: .visible) {
                Button("Cerrar sesión", role: .destructive) {
                    Task { await store.cerrarSesion() }
                }
                Button("Cancelar", role: .cancel) {}
            }
        }
    }
}

//
//  Theme.swift
//  appcmo
//

import SwiftUI
import UIKit

/// Paleta del Figma Hospital-CMO (azul petróleo), con variante para modo oscuro.
extension Color {
    init(claro: UInt32, oscuro: UInt32) {
        self.init(uiColor: UIColor { rasgos in
            UIColor(hex: rasgos.userInterfaceStyle == .dark ? oscuro : claro)
        })
    }

    static let cmoFondo = Color(claro: 0xF1F7FB, oscuro: 0x0C1620)
    static let cmoSuperficie = Color(claro: 0xFFFFFF, oscuro: 0x14212C)
    static let cmoTinta = Color(claro: 0x0F1B26, oscuro: 0xE6EEF4)
    static let cmoTintaSuave = Color(claro: 0x5B6B78, oscuro: 0xA0B2C0)
    static let cmoLinea = Color(claro: 0xDCE6EE, oscuro: 0x263847)
    static let cmoMarca = Color(claro: 0x0E5C80, oscuro: 0x6CB8DE)
    static let cmoSobreMarca = Color(claro: 0xFFFFFF, oscuro: 0x06202D)
    static let cmoMarcaSuave = Color(claro: 0xDBEBF2, oscuro: 0x163241)
    static let cmoError = Color(claro: 0xB3261E, oscuro: 0xF08A8C)
    static let cmoErrorSuave = Color(claro: 0xFBE4E2, oscuro: 0x3D1C1D)
    static let cmoInfo = Color(claro: 0x1F5FA8, oscuro: 0x8DB8F0)
    static let cmoInfoSuave = Color(claro: 0xE0EBFE, oscuro: 0x16283F)
    static let cmoAdvertencia = Color(claro: 0x8A5A00, oscuro: 0xE6B455)
    static let cmoAdvertenciaSuave = Color(claro: 0xFCEBC8, oscuro: 0x3A2D12)
    static let cmoExito = Color(claro: 0x1E7A4C, oscuro: 0x68D196)
    static let cmoExitoSuave = Color(claro: 0xDEF3E8, oscuro: 0x16361F)
}

private extension UIColor {
    convenience init(hex: UInt32) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1
        )
    }
}

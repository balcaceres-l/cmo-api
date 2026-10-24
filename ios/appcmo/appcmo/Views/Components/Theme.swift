//
//  Theme.swift
//  appcmo
//

import SwiftUI
import UIKit

/// Paleta del plan de trabajo (verde CMO), con variante para modo oscuro.
extension Color {
    init(claro: UInt32, oscuro: UInt32) {
        self.init(uiColor: UIColor { rasgos in
            UIColor(hex: rasgos.userInterfaceStyle == .dark ? oscuro : claro)
        })
    }

    static let cmoFondo = Color(claro: 0xF3F6F4, oscuro: 0x0D1612)
    static let cmoSuperficie = Color(claro: 0xFFFFFF, oscuro: 0x15221D)
    static let cmoTinta = Color(claro: 0x13221C, oscuro: 0xE5EFEA)
    static let cmoTintaSuave = Color(claro: 0x4A5C53, oscuro: 0xA2B7AD)
    static let cmoLinea = Color(claro: 0xD1DCD6, oscuro: 0x2B4036)
    static let cmoMarca = Color(claro: 0x0B5D4B, oscuro: 0x4FC4A2)
    static let cmoSobreMarca = Color(claro: 0xFFFFFF, oscuro: 0x06201A)
    static let cmoMarcaSuave = Color(claro: 0xD6EBE3, oscuro: 0x17382F)
    static let cmoError = Color(claro: 0xA3262A, oscuro: 0xF08A8C)
    static let cmoErrorSuave = Color(claro: 0xF7DADA, oscuro: 0x3D1C1D)
    static let cmoInfo = Color(claro: 0x0E5C86, oscuro: 0x6CB7E3)
    static let cmoInfoSuave = Color(claro: 0xDBEBF4, oscuro: 0x14313F)
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

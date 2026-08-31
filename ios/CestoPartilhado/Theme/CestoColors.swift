import SwiftUI
import UIKit

// Paleta Cesto Partilhado — gerada a partir dos tokens de design (oklch -> sRGB).
// Ver export/tokens/colors.json para os valores oklch de origem.

extension UIColor {
    static let sslBg          = UIColor(hex: 0xFDF8ED)
    static let sslSurface     = UIColor(hex: 0xFFFDF9)
    static let sslSurface2    = UIColor(hex: 0xF6EFE3)
    static let sslBorder      = UIColor(hex: 0xE2DDD4)
    static let sslText        = UIColor(hex: 0x1F1A10)
    static let sslText2       = UIColor(hex: 0x635D51)
    static let sslText3       = UIColor(hex: 0x918C80)
    static let sslGreen       = UIColor(hex: 0x409D48)
    static let sslGreenDark   = UIColor(hex: 0x146720)
    static let sslGreenTint   = UIColor(hex: 0xD7F5D7)
    static let sslOrange      = UIColor(hex: 0xD67523)
    static let sslOrangeDark  = UIColor(hex: 0x8C3F00)
    static let sslOrangeTint  = UIColor(hex: 0xFFE1C6)

    convenience init(hex: UInt32) {
        let r = CGFloat((hex >> 16) & 0xFF) / 255
        let g = CGFloat((hex >> 8) & 0xFF) / 255
        let b = CGFloat(hex & 0xFF) / 255
        self.init(red: r, green: g, blue: b, alpha: 1)
    }
}

extension Color {
    static let sslBg          = Color(UIColor.sslBg)
    static let sslSurface     = Color(UIColor.sslSurface)
    static let sslSurface2    = Color(UIColor.sslSurface2)
    static let sslBorder      = Color(UIColor.sslBorder)
    static let sslText        = Color(UIColor.sslText)
    static let sslText2       = Color(UIColor.sslText2)
    static let sslText3       = Color(UIColor.sslText3)
    static let sslGreen       = Color(UIColor.sslGreen)
    static let sslGreenDark   = Color(UIColor.sslGreenDark)
    static let sslGreenTint   = Color(UIColor.sslGreenTint)
    static let sslOrange      = Color(UIColor.sslOrange)
    static let sslOrangeDark  = Color(UIColor.sslOrangeDark)
    static let sslOrangeTint  = Color(UIColor.sslOrangeTint)
}

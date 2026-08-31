import SwiftUI

struct AvatarCircle: View {
    let label: String
    var size: CGFloat = 32
    var background: Color = .sslGreen
    var bordered: Bool = false

    var body: some View {
        Circle()
            .fill(background)
            .frame(width: size, height: size)
            .overlay(
                Text(label.prefix(1).uppercased())
                    .font(.system(size: size * 0.4, weight: .semibold))
                    .foregroundColor(.white)
            )
            .overlay(bordered ? Circle().stroke(Color.sslSurface, lineWidth: 2) : nil)
    }
}

/// Avatar do utilizador atual + um badge "+N" se houver mais membros na lista.
struct MemberAvatarStack: View {
    let selfLabel: String
    let extraMembers: Int

    var body: some View {
        HStack(spacing: -10) {
            AvatarCircle(label: selfLabel, size: 32, background: .sslGreen, bordered: true)
            if extraMembers > 0 {
                Circle()
                    .fill(Color.sslSurface2)
                    .frame(width: 32, height: 32)
                    .overlay(Text("+\(extraMembers)").font(.caption2.weight(.semibold)).foregroundColor(.sslText2))
                    .overlay(Circle().stroke(Color.sslSurface, lineWidth: 2))
            }
        }
    }
}

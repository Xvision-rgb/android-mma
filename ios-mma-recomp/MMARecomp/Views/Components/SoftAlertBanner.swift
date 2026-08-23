import SwiftUI

enum SoftAlertTone {
    case positive, neutral

    var color: Color {
        switch self {
        case .positive: return .green
        case .neutral: return .blue
        }
    }
}

/// Bandeau non culpabilisant : jamais de rouge, jamais de ton alarmiste —
/// utilisé pour les messages de recomposition, suggestions de charge, etc.
struct SoftAlertBanner: View {
    let icon: String
    let message: String
    let tone: SoftAlertTone

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: icon)
                .foregroundStyle(tone.color)
            Text(message)
                .font(.footnote)
                .foregroundStyle(.primary)
        }
        .padding(10)
        .background(tone.color.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))
    }
}

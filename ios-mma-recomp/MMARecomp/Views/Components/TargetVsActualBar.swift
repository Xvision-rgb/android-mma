import SwiftUI

struct TargetVsActualBar: View {
    let label: String
    let actual: Double
    let target: Double
    let unit: String

    private var ratio: Double {
        guard target > 0 else { return 0 }
        return min(actual / target, 1.2)
    }

    private var barColor: Color {
        // Jamais rouge / culpabilisant : au pire un orange doux si largement dépassé.
        ratio > 1.15 ? .orange : .accentColor
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(label).font(.subheadline)
                Spacer()
                Text("\(Int(actual))/\(Int(target)) \(unit)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 6)
                        .fill(Color.secondary.opacity(0.15))
                    RoundedRectangle(cornerRadius: 6)
                        .fill(barColor)
                        .frame(width: geo.size.width * min(ratio, 1.0))
                }
            }
            .frame(height: 8)
        }
    }
}

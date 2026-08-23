import Foundation

@MainActor
final class WeighInViewModel: ObservableObject {
    @Published var date: Date = Date()
    @Published var heure: Date = Date()
    @Published var type: WeighInType = .matinJeun
    @Published var poidsKg: String = ""
    @Published var bfPct: String = ""
    @Published var creatineRecente = false
    @Published var alcoolRecent = false
    @Published var postTraining = false
    @Published var isSaving = false
    @Published var errorMessage: String?

    @Published var history: [WeighIn] = []

    private let service = WeighInService()

    /// Distinction stricte matin/soir : le graphique de tendance ne prend
    /// jamais les pesées du soir, uniquement le matin à jeun.
    var trend7Day: [(date: Date, value: Double)] {
        let points = history
            .filter { $0.type == .matinJeun }
            .compactMap { weighIn -> (date: Date, value: Double)? in
                guard let d = DateUtils.date(from: weighIn.date) else { return nil }
                return (d, weighIn.poidsKg)
            }
        return MovingAverage.sevenDay(points: points)
    }

    var eveningWeighIns: [WeighIn] {
        history.filter { $0.type == .soir }
    }

    func loadHistory(days: Int = 60) async {
        do {
            history = try await service.fetch(since: DateUtils.daysAgo(days))
        } catch {
            errorMessage = "Impossible de charger l'historique des pesées."
        }
    }

    func save() async -> Bool {
        guard let poids = Double(poidsKg.replacingOccurrences(of: ",", with: ".")) else {
            errorMessage = "Poids invalide."
            return false
        }
        errorMessage = nil
        isSaving = true
        defer { isSaving = false }

        let timeFormatter = DateFormatter()
        timeFormatter.dateFormat = "HH:mm:ss"

        let newWeighIn = NewWeighIn(
            date: DateUtils.string(from: date),
            heure: timeFormatter.string(from: heure),
            type: type,
            poidsKg: poids,
            bfPct: Double(bfPct.replacingOccurrences(of: ",", with: ".")),
            contexte: WeighInContext(
                creatineRecente: creatineRecente,
                alcoolRecent: alcoolRecent,
                postTraining: postTraining
            )
        )

        do {
            let saved = try await service.log(newWeighIn)
            history.removeAll { $0.date == saved.date && $0.type == saved.type }
            history.append(saved)
            return true
        } catch {
            errorMessage = "Impossible d'enregistrer la pesée."
            return false
        }
    }

    var plateauStatus: PlateauStatus {
        let points = history
            .filter { $0.type == .matinJeun }
            .compactMap { weighIn -> (date: Date, poidsKg: Double)? in
                guard let d = DateUtils.date(from: weighIn.date) else { return nil }
                return (d, weighIn.poidsKg)
            }
        return PlateauDetector.detect(morningWeighIns: points, performanceTrendUp: true)
    }
}

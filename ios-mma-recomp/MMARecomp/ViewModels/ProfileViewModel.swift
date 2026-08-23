import Foundation

@MainActor
final class ProfileViewModel: ObservableObject {
    @Published var profile: Profile?
    @Published var poidsObjectifKg: String = ""
    @Published var bfObjectifPct: String = ""
    @Published var phase: Phase = .ete
    @Published var coachNotes: String = ""
    @Published var isSaving = false
    @Published var errorMessage: String?

    private let service = ProfileService()
    private let userId: UUID

    init(userId: UUID) {
        self.userId = userId
    }

    func load() async {
        do {
            let fetched = try await service.fetch(userId: userId)
            profile = fetched
            poidsObjectifKg = String(fetched.poidsObjectifKg)
            bfObjectifPct = String(fetched.bfObjectifPct)
            phase = fetched.phase
            coachNotes = fetched.coachNotes ?? ""
        } catch {
            errorMessage = "Impossible de charger le profil."
        }
    }

    func save() async {
        isSaving = true
        defer { isSaving = false }
        let patch = ProfileUpdate(
            poidsObjectifKg: Double(poidsObjectifKg.replacingOccurrences(of: ",", with: ".")),
            bfObjectifPct: Double(bfObjectifPct.replacingOccurrences(of: ",", with: ".")),
            phase: phase,
            coachNotes: coachNotes.isEmpty ? nil : coachNotes
        )
        do {
            try await service.update(userId: userId, patch: patch)
        } catch {
            errorMessage = "Impossible d'enregistrer le profil."
        }
    }
}

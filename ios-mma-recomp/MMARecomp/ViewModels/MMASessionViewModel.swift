import Foundation

@MainActor
final class MMASessionViewModel: ObservableObject {
    @Published var date: Date = Date()
    @Published var wodContent: String = ""
    @Published var roundsSets: String = ""
    @Published var ressenti: Int = 3
    @Published var notesTechnique: String = ""
    @Published var isSaving = false
    @Published var errorMessage: String?

    private let service = MMASessionService()

    var parsedMovements: [ParsedWodMovement] {
        WodParser.parse(wodContent)
    }

    func save() async -> Bool {
        errorMessage = nil
        isSaving = true
        defer { isSaving = false }

        let session = NewMMASession(
            date: DateUtils.string(from: date),
            wodContent: wodContent,
            roundsSets: roundsSets.isEmpty ? nil : roundsSets,
            ressenti: ressenti,
            notesTechnique: notesTechnique.isEmpty ? nil : notesTechnique
        )

        do {
            _ = try await service.log(session)
            return true
        } catch {
            errorMessage = "Impossible d'enregistrer la séance MMA."
            return false
        }
    }
}

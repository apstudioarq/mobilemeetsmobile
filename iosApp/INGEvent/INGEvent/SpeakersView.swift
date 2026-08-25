import SwiftUI
import shared

struct SpeakersView: View {
    @StateObject private var wrapper = SpeakersViewModelWrapper()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let error = wrapper.state.error, wrapper.state.speakers.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(error)
                            .font(.callout)
                            .foregroundStyle(Color.vibrantMuted)
                        Button("Retry") {
                            wrapper.viewModel.loadSpeakers()
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(Color.ingOrange)
                    }
                }
                ForEach(wrapper.state.speakers, id: \.id) { speaker in
                    SpeakerRow(speaker: speaker)
                }
            }
            .padding(24)
        }
        .background(Color.vibrantBackground)
    }
}

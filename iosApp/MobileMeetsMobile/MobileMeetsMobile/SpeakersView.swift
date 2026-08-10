import SwiftUI
import shared

struct SpeakersView: View {
    @StateObject private var wrapper = SpeakersViewModelWrapper()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                ForEach(wrapper.state.speakers, id: \.id) { speaker in
                    SpeakerRow(speaker: speaker)
                }
            }
            .padding(24)
        }
        .background(Color.vibrantBackground)
    }
}

import SwiftUI
import UIKit
import shared

struct MapView: View {
    @StateObject private var schedule = ScheduleViewModelWrapper()

    var body: some View {
        VStack(spacing: 0) {
            EventTopBar(section: "Map")

            Group {
                if schedule.state.isMapLoading {
                    ProgressView()
                        .tint(.ingOrange)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let image = decodedImage {
                    ScrollView([.vertical, .horizontal]) {
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFit()
                            .frame(maxWidth: .infinity)
                            .padding(16)
                    }
                } else if let url = remoteImageUrl {
                    ScrollView {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .scaledToFit()
                            case .failure:
                                mapMessage("Unable to display the event map.", showsRetry: true)
                            default:
                                ProgressView().tint(.ingOrange)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding(16)
                    }
                } else if let error = schedule.state.mapError {
                    mapMessage(error, showsRetry: true)
                } else {
                    mapMessage("The event map is not available yet.")
                }
            }
            .background(Color.vibrantBackground)
        }
        .onAppear {
            schedule.viewModel.loadMapContent()
        }
    }

    private var decodedImage: UIImage? {
        let cleanBase64 = schedule.state.mapContent.imageBase64
            .components(separatedBy: "base64,")
            .last?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !cleanBase64.isEmpty, let data = Data(base64Encoded: cleanBase64) else { return nil }
        return UIImage(data: data)
    }

    private var remoteImageUrl: URL? {
        let imageUrl = schedule.state.mapContent.imageUrl
        return imageUrl.isEmpty ? nil : URL(string: imageUrl)
    }

    @ViewBuilder
    private func mapMessage(_ message: String, showsRetry: Bool = false) -> some View {
        VStack(spacing: 16) {
            Text(message)
                .font(.body)
                .foregroundStyle(Color.vibrantMuted)
                .multilineTextAlignment(.center)
            if showsRetry {
                Button("Try again") {
                    schedule.viewModel.loadMapContent()
                }
                .buttonStyle(.borderedProminent)
                .tint(.ingOrange)
            }
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
